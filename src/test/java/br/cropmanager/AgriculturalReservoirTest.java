package br.cropmanager;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.Assert.*;

/** Focused regression checks for the reservoir's deterministic contracts. */
public final class AgriculturalReservoirTest {
    @BeforeClass public static void boot() {
        Bootstrap.register();
        TileEntity.register("cropmanager:agricultural_reservoir", AgriculturalReservoirTile.class);
    }

    @Test public void capacityScalesForEverySupportedHeight() {
        for (int height = 4; height <= 11; height++)
            assertEquals(height * 8000, AgriculturalReservoirTile.capacityForHeight(height));
        assertEquals(32000, AgriculturalReservoirTile.capacityForHeight(1));
        assertEquals(88000, AgriculturalReservoirTile.capacityForHeight(99));
    }

    @Test public void shellFormsWithOneControllerAndRejectsAdditionalControllers() {
        World world = mock(World.class);
        BlockPos floorCenter = new BlockPos(0, 64, 0);
        BlockPos controllerPos = floorCenter.north().up();
        IBlockState controller = CropManager.AGRICULTURAL_RESERVOIR.getDefaultState()
                .withProperty(AgriculturalReservoirBlock.FACING, EnumFacing.NORTH);
        IBlockState casing = CropManager.RESERVOIR_CASING.getDefaultState();
        when(world.getHeight()).thenReturn(256);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
        when(world.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
            BlockPos pos = call.getArgument(0);
            int y = pos.getY() - floorCenter.getY();
            if (pos.equals(controllerPos)) return controller;
            if (y < 0 || y >= 5 || (y > 0 && y < 4 && pos.getX() == 0 && pos.getZ() == 0))
                return Blocks.AIR.getDefaultState();
            return casing;
        });
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        tile.setWorld(world);
        tile.setPos(controllerPos);
        tile.tryForm();
        assertTrue(tile.formed);
        assertEquals(5, tile.height);

        BlockPos cap = floorCenter.up(4);
        ReservoirCasingTile shell = new ReservoirCasingTile();
        shell.setWorld(world);
        shell.setPos(cap);
        shell.setController(controllerPos);
        when(world.getTileEntity(cap)).thenReturn(shell);
        when(world.getTileEntity(controllerPos)).thenReturn(tile);
        EntityPlayer visitor = mock(EntityPlayer.class);
        visitor.world = world;
        when(visitor.getDistanceSq(any(BlockPos.class))).thenReturn(100.0);
        when(visitor.getDistanceSq(cap)).thenReturn(16.0);
        assertTrue(tile.usable(visitor));
        for (EnumFacing face : EnumFacing.values()) {
            assertTrue(CropManager.RESERVOIR_CASING.onBlockActivated(
                    world, cap, casing, visitor, EnumHand.MAIN_HAND, face, .5F, .5F, .5F));
        }
        verify(visitor, times(6)).openGui(CropManager.instance, 15, world,
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        when(visitor.getDistanceSq(cap)).thenReturn(100.0);
        assertFalse(tile.usable(visitor));

        when(world.getBlockState(controllerPos.up())).thenReturn(controller);
        tile.tryForm();
        assertFalse(tile.formed);

        when(world.getTileEntity(controllerPos)).thenReturn(tile);
        EntityPlayer player = mock(EntityPlayer.class);
        assertTrue(CropManager.AGRICULTURAL_RESERVOIR.onBlockActivated(
                world, controllerPos, controller, player, EnumHand.MAIN_HAND, EnumFacing.NORTH, .5F, .5F, .5F));
        verify(player).sendStatusMessage(new TextComponentTranslation("cropmanager.reservoir_unformed"), true);
    }

    @Test public void casingPacketInvalidatesRenderWhenControllerLinkChanges() {
        World world = mock(World.class);
        BlockPos casingPos = new BlockPos(10, 64, 20);
        ReservoirCasingTile casing = new ReservoirCasingTile();
        casing.setWorld(world);
        casing.setPos(casingPos);
        NBTTagCompound linked = new NBTTagCompound();
        linked.setInteger("x", casingPos.getX());
        linked.setInteger("y", casingPos.getY());
        linked.setInteger("z", casingPos.getZ());
        linked.setBoolean("ReservoirLinked", true);
        linked.setInteger("ControllerDz", 2);

        SPacketUpdateTileEntity linkPacket = new SPacketUpdateTileEntity(casingPos, 1, linked);
        casing.onDataPacket(null, linkPacket);
        casing.onDataPacket(null, linkPacket);
        verify(world).markBlockRangeForRenderUpdate(casingPos, casingPos);

        NBTTagCompound unlinked = linked.copy();
        unlinked.setBoolean("ReservoirLinked", false);
        casing.onDataPacket(null, new SPacketUpdateTileEntity(casingPos, 1, unlinked));
        verify(world, times(2)).markBlockRangeForRenderUpdate(casingPos, casingPos);
    }

    @Test public void eachTankLocksToFirstFluidUntilEmpty() {
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        FluidStack lava = new FluidStack(FluidRegistry.LAVA, 1000);
        assertEquals(1000, tile.water.fill(water, true));
        assertEquals(0, tile.water.fill(lava, true));
        assertEquals(0, tile.weed.getFluidAmount());
        assertEquals(1000, tile.water.drain(1000, true).amount);
        assertEquals(1000, tile.water.fill(lava, true));
        assertEquals("Tanks are independent", 0, tile.weed.getFluidAmount());
    }

    @Test public void equalizationLeavesUnfilteredEmptyTanksAvailable() {
        AgriculturalReservoirTile first = new AgriculturalReservoirTile();
        AgriculturalReservoirTile second = new AgriculturalReservoirTile();
        first.water.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        first.equalizeWith(second);
        assertEquals(1000, first.water.getFluidAmount());
        assertEquals(0, second.water.getFluidAmount());
        assertEquals(0, second.weed.getFluidAmount());

        second.water.fill(new FluidStack(FluidRegistry.WATER, 200), true);
        first.equalizeWith(second);
        assertEquals(600, first.water.getFluidAmount());
        assertEquals(600, second.water.getFluidAmount());
        assertEquals(0, first.weed.getFluidAmount());
        assertEquals(0, second.weed.getFluidAmount());
    }

    @Test public void transferEuRatioCarriesIntegerRemainder() {
        assertEquals(0, AgriculturalReservoirTile.energyCostFor(0, 49));
        assertEquals(1, AgriculturalReservoirTile.energyCostFor(49, 1));
        assertEquals(20, AgriculturalReservoirTile.energyCostFor(0, 1000));
        assertEquals(0, AgriculturalReservoirTile.energyCostFor(49, 0));
    }

    @Test public void filterSlotRejectsOrdinaryItems() {
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        assertFalse("Ordinary items cannot be used as fluid filters",
                tile.inventory.isItemValid(0, new net.minecraft.item.ItemStack(Items.DIAMOND)));
    }

    @Test public void serverSlotActionsFollowTabsWithoutCallingClientSlotMethod() {
        World world = mock(World.class);
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        tile.setWorld(world);
        tile.setPos(BlockPos.ORIGIN);
        tile.formed = true;
        when(world.isBlockLoaded(BlockPos.ORIGIN, false)).thenReturn(true);
        when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(tile);
        when(world.getBlockState(BlockPos.ORIGIN)).thenReturn(CropManager.AGRICULTURAL_RESERVOIR.getDefaultState());

        EntityPlayer player = mock(EntityPlayer.class);
        player.world = world;
        InventoryPlayer inventory = new InventoryPlayer(player);
        ReservoirContainer container = new ReservoirContainer(inventory, tile);
        int capacitySlot = AgriculturalReservoirTile.CAPACITY_SLOT;
        Slot upgrade = spy(container.getSlot(capacitySlot));
        doThrow(new NoSuchMethodError("Slot.isEnabled")).when(upgrade).isEnabled();
        container.inventorySlots.set(capacitySlot, upgrade);
        ItemStack capacity = new ItemStack(CropManager.RESERVOIR_CAPACITY);
        tile.inventory.setStackInSlot(capacitySlot, capacity);

        assertFalse(upgrade.canTakeStack(player));
        assertFalse(upgrade.isItemValid(capacity));
        assertTrue(container.slotClick(capacitySlot, 0, ClickType.PICKUP, player).isEmpty());
        assertTrue(container.transferStackInSlot(player, capacitySlot).isEmpty());
        assertSame(capacity, tile.inventory.getStackInSlot(capacitySlot));

        assertTrue(container.enchantItem(player, 1));
        assertTrue(upgrade.canTakeStack(player));
        assertTrue(upgrade.isItemValid(capacity));
        assertFalse(container.getSlot(0).canTakeStack(player));
        assertEquals(CropManager.RESERVOIR_CAPACITY, container.transferStackInSlot(player, capacitySlot).getItem());
        assertTrue(tile.inventory.getStackInSlot(capacitySlot).isEmpty());
        assertEquals(CropManager.RESERVOIR_CAPACITY, inventory.getStackInSlot(8).getItem());
    }

    @Test public void automaticTransferChargesOnlyWholeEuAndCarriesRemainder() {
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        FluidTank destination = new FluidTank(5000);
        tile.setWorld(mock(net.minecraft.world.World.class));
        tile.setPos(net.minecraft.util.math.BlockPos.ORIGIN);
        tile.energy.setEnergyStored(1);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 49), true);
        tile.transferToHandler(0, destination);
        assertEquals(49, destination.getFluidAmount());
        assertEquals(49, tile.euRemainder[0]);
        assertEquals(1.0D, tile.getEnergy(), 0.001D);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 1), true);
        tile.transferToHandler(0, destination);
        assertEquals(50, destination.getFluidAmount());
        assertEquals(0, tile.euRemainder[0]);
        assertEquals(0.0D, tile.getEnergy(), 0.001D);
    }

    @Test public void shrinkingTankRetainsOverCapacityFluidAndAllowsDrain() {
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        tile.water.setCapacity(AgriculturalReservoirTile.capacityForHeight(5));
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 40000), true);
        tile.water.setCapacity(AgriculturalReservoirTile.capacityForHeight(4));
        assertEquals(40000, tile.water.getFluidAmount());
        assertEquals(0, tile.water.fill(new FluidStack(FluidRegistry.WATER, 1), true));
        assertEquals(1000, tile.water.drain(1000, true).amount);
    }

    @Test public void partialTransferRestoresFluidEvenAboveCapacity() {
        AgriculturalReservoirTile tile = new AgriculturalReservoirTile();
        tile.setWorld(mock(World.class));
        tile.water.setCapacity(40000);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 40000), true);
        tile.water.setCapacity(32000);
        tile.energy.setEnergyStored(20);
        FluidTank destination = new FluidTank(1000) {
            @Override public int fill(FluidStack resource, boolean execute) {
                return super.fill(execute ? new FluidStack(resource, 250) : resource, execute);
            }
        };
        tile.transferToHandler(0, destination);
        assertEquals(39750, tile.water.getFluidAmount());
        assertEquals(250, destination.getFluidAmount());
        assertEquals(15.0, tile.getEnergy(), 0.001);
    }

    @Test public void modernInventoriesKeepTheirFixedSizesWhenSavedSizesAreCorrupt() {
        AgriculturalReservoirTile original = new AgriculturalReservoirTile();
        original.inventory.setStackInSlot(3, new ItemStack(Items.BUCKET));
        NBTTagCompound tag = original.writeToNBT(new NBTTagCompound());
        NBTTagCompound inventory = tag.getCompoundTag("Inventory");
        inventory.setInteger("Size", 1);
        ItemStackHandler returns = new ItemStackHandler(2);
        returns.setStackInSlot(1, new ItemStack(Items.WATER_BUCKET));
        NBTTagCompound pending = returns.serializeNBT();
        pending.setInteger("Size", -1);
        tag.setTag("PendingReturns", pending);

        AgriculturalReservoirTile restored = new AgriculturalReservoirTile();
        restored.readFromNBT(tag);
        assertEquals(AgriculturalReservoirTile.INVENTORY_SLOTS, restored.inventory.getSlots());
        assertEquals(Items.BUCKET, restored.inventory.getStackInSlot(3).getItem());
        returns.deserializeNBT(restored.writeToNBT(new NBTTagCompound()).getCompoundTag("PendingReturns"));
        assertEquals(2, returns.getSlots());
        assertEquals(Items.WATER_BUCKET, returns.getStackInSlot(1).getItem());
        assertEquals(1, inventory.getInteger("Size"));
        assertEquals(-1, pending.getInteger("Size"));
    }

    @Test public void legacyInventoryMigratesValidSlotsDespiteCorruptSize() {
        ItemStackHandler old = new ItemStackHandler(4);
        old.setStackInSlot(3, new ItemStack(Items.BUCKET));
        NBTTagCompound inventory = old.serializeNBT();
        inventory.setInteger("Size", 0);
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setTag("Inventory", inventory);

        AgriculturalReservoirTile restored = new AgriculturalReservoirTile();
        restored.readFromNBT(legacy);
        assertEquals(AgriculturalReservoirTile.INVENTORY_SLOTS, restored.inventory.getSlots());
        assertEquals(Items.BUCKET, restored.inventory.getStackInSlot(1).getItem());
        assertEquals(0, inventory.getInteger("Size"));
    }

    @Test public void legacyWaterWeedAndIc2EnergySurviveLoadAndAbsentEnergyDefaultsToZero() {
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setTag("Water", new FluidStack(FluidRegistry.WATER, 750).writeToNBT(new NBTTagCompound()));
        legacy.setTag("Weed", new FluidStack(FluidRegistry.LAVA, 1250).writeToNBT(new NBTTagCompound()));
        NBTTagCompound oldSink = new NBTTagCompound();
        oldSink.setDouble("energy", 4321.0D);
        legacy.setTag("IC2BasicSink", oldSink);
        AgriculturalReservoirTile restored = new AgriculturalReservoirTile();
        restored.readFromNBT(legacy);
        assertEquals(750, restored.water.getFluidAmount());
        assertEquals(1250, restored.weed.getFluidAmount());
        assertEquals(4321.0D, restored.getEnergy(), 0.001D);
        assertFalse("Legacy blocks wait for shell construction", restored.formed);

        NBTTagCompound noEnergy = legacy.copy();
        noEnergy.removeTag("IC2BasicSink");
        AgriculturalReservoirTile withoutEnergy = new AgriculturalReservoirTile();
        withoutEnergy.readFromNBT(noEnergy);
        assertEquals(0.0D, withoutEnergy.getEnergy(), 0.001D);
    }
}
