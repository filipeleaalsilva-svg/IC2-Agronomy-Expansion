package br.cropmanager;

import br.cropmanager.chest.SeedFixtures;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EndToEndIntegrationTest {
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void bootstrap() throws ReflectiveOperationException {
        SeedFixtures.bootstrap();
        if (Crops.weed == null) {
            Crops.weed = mock(CropCard.class);
        }
        Field providers = CapabilityManager.class.getDeclaredField("providers");
        providers.setAccessible(true);
        Map<String, Capability<?>> capabilities = (Map<String, Capability<?>>) providers.get(CapabilityManager.INSTANCE);
        if (!capabilities.containsKey(IFluidHandler.class.getName().intern())) {
            CapabilityFluidHandler.register();
        }
        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY =
                (Capability<IFluidHandler>) capabilities.get(IFluidHandler.class.getName().intern());
        CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY =
                (Capability<net.minecraftforge.fluids.capability.IFluidHandlerItem>) capabilities.get(net.minecraftforge.fluids.capability.IFluidHandlerItem.class.getName().intern());
        if (!capabilities.containsKey(IItemHandler.class.getName().intern())) {
            CapabilityItemHandler.register();
        }
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY =
                (Capability<IItemHandler>) capabilities.get(IItemHandler.class.getName().intern());
    }

    @Test
    public void endToEndWeedToFertilizerToManagerPipelineViaCapabilities() {
        World world = mock(World.class);
        when(world.getHeight()).thenReturn(256);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);

        // Stage 1: WeederTile harvests 2x IC2 Weed from crop sticks
        WeederTile weeder = new WeederTile();
        weeder.setWorld(world);
        weeder.setPos(new BlockPos(0, 64, 0));
        weeder.radius = 0;
        weeder.layers = 2;
        weeder.energy.setEnergyStored(5000);

        TileEntityCrop weedCrop = mock(TileEntityCrop.class);
        when(weedCrop.getCrop()).thenReturn(Crops.weed);
        when(world.getTileEntity(any(BlockPos.class))).thenReturn(weedCrop);

        assertTrue(weeder.scanOne());
        assertTrue(weeder.scanOne());
        assertEquals(2, weeder.removed);

        // Stage 2: Extract harvested weed via CapabilityItemHandler and pipe into ComposterTile
        IItemHandler weederPipe = weeder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertNotNull(weederPipe);
        // Automation must never be able to insert into Weeder output slots 0..3
        assertFalse(weederPipe.insertItem(0, WeederTile.weedItem(), false).isEmpty());

        ItemStack extractedWeeds = weederPipe.extractItem(0, 2, false);
        assertEquals(2, extractedWeeds.getCount());
        assertTrue(ComposterTile.isIc2Weed(extractedWeeds));

        ComposterTile composter = new ComposterTile();
        composter.setWorld(world);
        composter.energy.setEnergyStored(5000);

        IFluidHandler composterFluidPipe = composter.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
        assertNotNull(composterFluidPipe);
        assertEquals(1000, composterFluidPipe.fill(new FluidStack(FluidRegistry.WATER, 1000), true));

        IItemHandler composterItemPipe = composter.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.WEST);
        assertNotNull(composterItemPipe);
        assertTrue(composterItemPipe.insertItem(0, extractedWeeds, false).isEmpty());

        for (int t = 0; t < composter.workTicks; t++) {
            assertTrue(composter.processTick());
        }
        ItemStack producedFertilizer = composterItemPipe.extractItem(1, 1, false);
        assertEquals(1, producedFertilizer.getCount());
        assertEquals(500, composter.water.getFluidAmount()); // Only 500 mB consumed for 2x IC2 Weed!

        // Stage 3: Verify AdvancedManagerTile Ender IO DOWN-face guard invariant
        AdvancedManagerTile manager = new AdvancedManagerTile();
        manager.setWorld(world);
        assertFalse("DOWN face must never expose ITEM_HANDLER_CAPABILITY (Ender IO crash guard)",
                manager.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN));
        assertNull(manager.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN));
        assertTrue(manager.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP));
    }

    @Test
    public void endToEndMixerCapabilitiesAndMonitorWirelessEuPipeline() {
        World world = mock(World.class);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);

        WeedExFabricatorTile mixer = new WeedExFabricatorTile();
        mixer.setWorld(world);
        mixer.setPos(new BlockPos(2, 64, 0));
        mixer.energy.setEnergyStored(5000);

        // Pipe water and items into Mixer via Capabilities
        IFluidHandler mixerFluids = mixer.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.NORTH);
        IItemHandler mixerItems = mixer.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        assertNotNull(mixerFluids);
        assertNotNull(mixerItems);

        assertEquals(2000, mixerFluids.fill(new FluidStack(FluidRegistry.WATER, 2000), true));
        assertTrue(mixerItems.insertItem(WeedExFabricatorTile.SPIDER_EYE, new ItemStack(Items.SPIDER_EYE, 1), false).isEmpty());
        assertTrue(mixerItems.insertItem(WeedExFabricatorTile.REDSTONE, new ItemStack(Items.REDSTONE, 1), false).isEmpty());

        for (int t = 0; t < mixer.workTicks; t++) {
            assertTrue(mixer.processRecipe());
        }

        // Verify fluid pipe drains ONLY Weed-Ex from the central tank, never lateral extracts
        FluidStack drainedWeedEx = mixerFluids.drain(1000, true);
        assertNotNull(drainedWeedEx);
        assertEquals(1000, drainedWeedEx.amount);
        assertEquals(WeedExFabricatorTile.weedExFluid(), drainedWeedEx.getFluid());
        assertEquals(0, mixer.weed.getFluidAmount());
        assertEquals(500, mixer.spiderTank.getFluidAmount());
        assertEquals(500, mixer.redstoneTank.getFluidAmount());

        // Verify Monitor wireless energy transfer to linked WeederTile
        MonitorTile monitor = new MonitorTile();
        monitor.setWorld(world);
        monitor.setPos(new BlockPos(0, 64, 0));
        WeederTile weeder = new WeederTile();
        weeder.setWorld(world);
        weeder.setPos(new BlockPos(5, 64, 0));
        weeder.energy.setEnergyStored(0);

        double transferred = monitor.transferWirelessEnergyTo(weeder.energy, 256);
        assertEquals(0, transferred, 0.001); // 0 without Wireless EU upgrade installed
    }

    @Test
    public void nbtAndGuiStateRoundTripPreservesAllThreeMachinesState() {
        World world = mock(World.class);

        // 1. WeedExFabricatorTile NBT + GuiState round-trip
        WeedExFabricatorTile mixer = new WeedExFabricatorTile();
        mixer.setWorld(world);
        mixer.energy.setEnergyStored(4321);
        mixer.fillWater(3000, true); // 2000 mB Side 1, 1000 mB Side 2
        mixer.inventory.setStackInSlot(WeedExFabricatorTile.SPIDER_EYE, new ItemStack(Items.SPIDER_EYE, 1));
        mixer.updateSideConversions(); // Side 1: 1000 mB Spider Extract + 1000 mB in waterBuffer1
        mixer.weed.fill(new FluidStack(WeedExFabricatorTile.weedExFluid(), 1500), true);
        mixer.progress = 42;

        NBTTagCompound savedMixer = mixer.writeToNBT(new NBTTagCompound());
        WeedExFabricatorTile loadedMixer = new WeedExFabricatorTile();
        loadedMixer.setWorld(world);
        loadedMixer.readFromNBT(savedMixer);

        assertEquals(4321, loadedMixer.getEnergy(), 0.01);
        assertEquals(1000, loadedMixer.spiderTank.getFluidAmount());
        assertEquals(2, loadedMixer.spiderType());
        assertEquals(1000, loadedMixer.waterBuffer1);
        assertEquals(1000, loadedMixer.redstoneTank.getFluidAmount());
        assertEquals(1, loadedMixer.redstoneType());
        assertEquals(1500, loadedMixer.weed.getFluidAmount());
        assertEquals(42, loadedMixer.progress);

        WeedExFabricatorTile clientMixer = new WeedExFabricatorTile();
        clientMixer.acceptGuiState(loadedMixer.guiState());
        assertEquals(1000, clientMixer.spiderTank.getFluidAmount());
        assertEquals(2, clientMixer.spiderType());
        assertEquals(1000, clientMixer.redstoneTank.getFluidAmount());
        assertEquals(1, clientMixer.redstoneType());
        assertEquals(1500, clientMixer.weed.getFluidAmount());

        // 2. WeederTile NBT + GuiState round-trip
        WeederTile weeder = new WeederTile();
        weeder.setWorld(world);
        weeder.energy.setEnergyStored(2500);
        weeder.removed = 17;
        weeder.radius = 6;
        weeder.layers = 5;
        weeder.inventory.setStackInSlot(0, WeederTile.weedItem());

        WeederTile loadedWeeder = new WeederTile();
        loadedWeeder.setWorld(world);
        loadedWeeder.readFromNBT(weeder.writeToNBT(new NBTTagCompound()));
        assertEquals(2500, loadedWeeder.getEnergy(), 0.01);
        assertEquals(17, loadedWeeder.removed);
        assertEquals(6, loadedWeeder.radius);
        assertEquals(5, loadedWeeder.layers);
        assertEquals(1, loadedWeeder.inventory.getStackInSlot(0).getCount());
    }

    @Test
    public void containerSlotCoordinatesMatchPixelAlignedTextures() {
        EntityPlayer player = mock(EntityPlayer.class);
        InventoryPlayer inv = new InventoryPlayer(player);

        // 1. ManagerContainer: verify fluid output slots 24 and 25 are at y = 48 (fixed from y = 42)
        AdvancedManagerTile manager = new AdvancedManagerTile();
        ManagerContainer managerContainer = new ManagerContainer(inv, manager);
        Slot slot22 = managerContainer.getSlot(22);
        Slot slot23 = managerContainer.getSlot(23);
        Slot slot24 = managerContainer.getSlot(24);
        Slot slot25 = managerContainer.getSlot(25);
        assertEquals(33, slot22.xPos); assertEquals(24, slot22.yPos);
        assertEquals(51, slot23.xPos); assertEquals(24, slot23.yPos);
        assertEquals(33, slot24.xPos); assertEquals(48, slot24.yPos);
        assertEquals(51, slot25.xPos); assertEquals(48, slot25.yPos);

        // 2. EquipmentContainer for 3-Tank Weed-Ex Mixer: verify all 10 machine slots + shift-click
        WeedExFabricatorTile mixer = new WeedExFabricatorTile();
        EquipmentContainer mixerContainer = new EquipmentContainer(inv, mixer);
        assertEquals(10, mixerContainer.machineSlots);
        assertEquals(27, mixerContainer.getSlot(0).xPos); assertEquals(20, mixerContainer.getSlot(0).yPos);
        assertEquals(117, mixerContainer.getSlot(1).xPos); assertEquals(20, mixerContainer.getSlot(1).yPos);
        assertEquals(27, mixerContainer.getSlot(2).xPos); assertEquals(86, mixerContainer.getSlot(2).yPos);
        assertEquals(72, mixerContainer.getSlot(3).xPos); assertEquals(86, mixerContainer.getSlot(3).yPos);
        assertEquals(117, mixerContainer.getSlot(4).xPos); assertEquals(86, mixerContainer.getSlot(4).yPos);

        // Shift-click Spider Eye and Redstone from player inventory into Mixer slots 0 and 1
        inv.setInventorySlotContents(9, new ItemStack(Items.SPIDER_EYE, 3));
        inv.setInventorySlotContents(10, new ItemStack(Items.REDSTONE, 5));
        mixerContainer.transferStackInSlot(player, 10); // player slot 9 is index 10 in container
        mixerContainer.transferStackInSlot(player, 11); // player slot 10 is index 11 in container
        assertEquals(3, mixer.inventory.getStackInSlot(WeedExFabricatorTile.SPIDER_EYE).getCount());
        assertEquals(5, mixer.inventory.getStackInSlot(WeedExFabricatorTile.REDSTONE).getCount());
    }
}
