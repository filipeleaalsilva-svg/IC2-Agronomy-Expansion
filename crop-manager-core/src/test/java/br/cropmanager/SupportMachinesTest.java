package br.cropmanager;

import br.cropmanager.chest.SeedFixtures;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SupportMachinesTest {
    @BeforeClass
    public static void bootstrap() {
        SeedFixtures.bootstrap();
        if (Crops.weed == null) {
            Crops.weed = mock(CropCard.class);
        }
    }

    @Test
    public void weederHarvestsWeedItemAndResetsCropStickWithoutDeletingValuableCrops() {
        WeederTile weeder = new WeederTile();
        World world = mock(World.class);
        weeder.setWorld(world);
        weeder.setPos(new BlockPos(0, 64, 0));
        weeder.radius = 0;
        weeder.layers = 2; // same level only: (0, 64, 0)
        weeder.energy.setEnergyStored(1000);

        when(world.getHeight()).thenReturn(256);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);

        // 1. Non-weed crop must NEVER be reset or harvested
        TileEntityCrop valuableCrop = mock(TileEntityCrop.class);
        CropCard aurelia = mock(CropCard.class);
        when(valuableCrop.getCrop()).thenReturn(aurelia);
        when(world.getTileEntity(any(BlockPos.class))).thenReturn(valuableCrop);

        assertFalse(weeder.scanOne());
        verify(valuableCrop, never()).reset();
        assertTrue(weeder.inventory.getStackInSlot(0).isEmpty());
        assertEquals(0, weeder.removed);

        // 2. Actual IC2 weed is harvested into slot 0 and crop stick is reset
        TileEntityCrop weedCrop = mock(TileEntityCrop.class);
        when(weedCrop.getCrop()).thenReturn(Crops.weed);
        when(world.getTileEntity(any(BlockPos.class))).thenReturn(weedCrop);

        assertTrue(weeder.scanOne());
        verify(weedCrop, times(1)).reset();
        verify(weedCrop, times(1)).updateState();
        assertEquals(1, weeder.removed);
        assertEquals(1, weeder.inventory.getStackInSlot(0).getCount());
        assertTrue(ComposterTile.isIc2Weed(weeder.inventory.getStackInSlot(0)));
    }

    @Test
    public void composterGivesDoubleEfficiencyForIc2WeedAndStandardForOrganics() {
        ComposterTile composter = new ComposterTile();
        composter.setWorld(mock(World.class));
        composter.energy.setEnergyStored(5000);
        composter.water.fillInternal(new FluidStack(FluidRegistry.WATER, 2000), true);

        // 1. IC2 Weed: 2 items + 500 mB water -> 1 fertilizer
        ItemStack weedInput = WeederTile.weedItem();
        weedInput.setCount(2);
        composter.inventory.setStackInSlot(0, weedInput);

        for (int t = 0; t < composter.workTicks; t++) {
            assertTrue(composter.processTick());
        }
        assertTrue(composter.inventory.getStackInSlot(0).isEmpty());
        assertEquals(1500, composter.water.getFluidAmount());
        assertEquals(1, composter.inventory.getStackInSlot(1).getCount());

        // 2. Common organic (Wheat): 4 items + 1000 mB water -> +1 fertilizer
        composter.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, 4));
        for (int t = 0; t < composter.workTicks; t++) {
            assertTrue(composter.processTick());
        }
        assertTrue(composter.inventory.getStackInSlot(0).isEmpty());
        assertEquals(500, composter.water.getFluidAmount());
        assertEquals(2, composter.inventory.getStackInSlot(1).getCount());
    }

    @Test
    public void composterDiscardsProgressWhenItsInputIsRemoved() {
        ComposterTile composter = new ComposterTile();
        composter.setWorld(mock(World.class));
        composter.energy.setEnergyStored(5000);
        composter.water.fillInternal(new FluidStack(FluidRegistry.WATER, 2000), true);
        composter.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, ManagerConfig.composterRequiredItems));
        for (int tick = 1; tick < composter.workTicks; tick++) assertTrue(composter.processTick());
        double remainingEnergy = composter.getEnergy();

        composter.inventory.setStackInSlot(0, ItemStack.EMPTY);
        assertFalse(composter.processTick());
        assertEquals(0, composter.progress);
        assertEquals("compost_input", composter.condition);
        assertEquals(remainingEnergy, composter.getEnergy(), 0.0);

        composter.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, ManagerConfig.composterRequiredItems));
        assertTrue(composter.processTick());
        assertEquals(1, composter.progress);
        assertEquals(2000, composter.water.getFluidAmount());
        assertTrue(composter.inventory.getStackInSlot(1).isEmpty());
    }

    @Test
    public void composterKeepsProgressAndMaterialsWhileBlocked() {
        ComposterTile composter = new ComposterTile();
        composter.setWorld(mock(World.class));
        composter.energy.setEnergyStored(5000);
        composter.water.fillInternal(new FluidStack(FluidRegistry.WATER, 2000), true);
        composter.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, ManagerConfig.composterRequiredItems));
        assertTrue(composter.processTick());

        ItemStack fullOutput = ComposterTile.fertilizerItem();
        fullOutput.setCount(fullOutput.getMaxStackSize());
        composter.inventory.setStackInSlot(1, fullOutput);
        double remainingEnergy = composter.getEnergy();
        assertFalse(composter.processTick());
        assertEquals("no_space", composter.condition);
        assertEquals(remainingEnergy, composter.getEnergy(), 0.0);

        composter.inventory.setStackInSlot(1, ItemStack.EMPTY);
        composter.energy.setEnergyStored(0);
        assertFalse(composter.processTick());
        assertEquals("no_energy", composter.condition);
        assertEquals(1, composter.progress);
        assertEquals(ManagerConfig.composterRequiredItems, composter.inventory.getStackInSlot(0).getCount());
        assertEquals(2000, composter.water.getFluidAmount());
    }

    @Test
    public void weedExMixerConvertsBothLateralTanksAndMixesCentralWeedEx() {
        WeedExFabricatorTile mixer = new WeedExFabricatorTile();
        mixer.setWorld(mock(World.class));
        mixer.energy.setEnergyStored(5000);

        // Pump 2000 mB of water: distributes 1000 mB to Tank 1 (blue water) and 1000 mB to Tank 2 (blue water)
        assertEquals(2000, mixer.fillWater(2000, true));
        assertEquals(1000, mixer.spiderTank.getFluidAmount());
        assertEquals(1, mixer.spiderType()); // 1 = Water
        assertEquals(1000, mixer.redstoneTank.getFluidAmount());
        assertEquals(1, mixer.redstoneType()); // 1 = Water

        // Insert 1 Spider Eye in Tank 1 slot and 1 Redstone in Tank 2 slot
        mixer.inventory.setStackInSlot(WeedExFabricatorTile.SPIDER_EYE, new ItemStack(Items.SPIDER_EYE, 1));
        mixer.inventory.setStackInSlot(WeedExFabricatorTile.REDSTONE, new ItemStack(Items.REDSTONE, 1));
        mixer.updateSideConversions();

        // Both lateral tanks now hold 1000 mB of converted extracts
        assertTrue(mixer.inventory.getStackInSlot(WeedExFabricatorTile.SPIDER_EYE).isEmpty());
        assertTrue(mixer.inventory.getStackInSlot(WeedExFabricatorTile.REDSTONE).isEmpty());
        assertEquals(2, mixer.spiderType()); // 2 = Spider Extract
        assertEquals(1000, mixer.spiderTank.getFluidAmount());
        assertEquals(2, mixer.redstoneType()); // 2 = Liquid Redstone
        assertEquals(1000, mixer.redstoneTank.getFluidAmount());

        // Run central mixer for 1 recipe cycle: drains 500 mB from each side tank -> produces 1000 mB Weed-Ex in central tank
        for (int t = 0; t < mixer.workTicks; t++) {
            assertTrue(mixer.processRecipe());
        }
        assertEquals(500, mixer.spiderTank.getFluidAmount());
        assertEquals(500, mixer.redstoneTank.getFluidAmount());
        assertEquals(1000, mixer.weed.getFluidAmount());
    }
}
