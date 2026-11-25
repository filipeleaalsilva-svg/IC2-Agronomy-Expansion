package br.cropmanager;

import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.RadbloomCropCard;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropSeed;
import ic2.api.crops.ICropTile;
import ic2.api.energy.tile.IHeatSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Headless Archetype C unit tests for {@link HydroponicChamberTile} and {@link VirtualCropTile}.
 */
public final class HydroponicChamberTest {
    private static final BlockPos POS = new BlockPos(10, 64, 10);

    private static final class DummySeedItem extends Item implements ICropSeed {
        private final CropCard card;
        DummySeedItem(CropCard card) { this.card = card; }
        @Override public CropCard getCropFromStack(ItemStack stack) { return card; }
        @Override public void setCropFromStack(ItemStack stack, CropCard crop) {}
        @Override public int getGrowthFromStack(ItemStack stack) { return 10; }
        @Override public void setGrowthFromStack(ItemStack stack, int growth) {}
        @Override public int getGainFromStack(ItemStack stack) { return 10; }
        @Override public void setGainFromStack(ItemStack stack, int gain) {}
        @Override public int getResistanceFromStack(ItemStack stack) { return 10; }
        @Override public void setResistanceFromStack(ItemStack stack, int resistance) {}
        @Override public int getScannedFromStack(ItemStack stack) { return 4; }
        @Override public void setScannedFromStack(ItemStack stack, int scan) {}
        @Override public void incrementScannedFromStack(ItemStack stack) {}
    }

    private static final class FakeHeatTile extends TileEntity implements IHeatSource {
        int available;
        int drawn;
        FakeHeatTile(int available) { this.available = available; }
        @Override public int maxrequestHeatTick(EnumFacing side) { return available; }
        @Override public int requestHeat(EnumFacing side, int request) { return drawHeat(side, request, false); }
        @Override public int drawHeat(EnumFacing side, int request, boolean simulate) {
            int amt = Math.min(available, request);
            if (!simulate) drawn += amt;
            return amt;
        }
    }

    private static World mockWorld(Map<BlockPos, IBlockState> blocks, Map<BlockPos, TileEntity> tiles) throws Exception {
        World world = mock(World.class);
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, false);
        when(world.getHeight()).thenReturn(256);
        when(world.isBlockLoaded(any(BlockPos.class), anyBoolean())).thenReturn(true);
        when(world.getBlockState(any(BlockPos.class))).thenAnswer(inv -> {
            BlockPos p = inv.getArgument(0);
            return blocks.getOrDefault(p, Blocks.AIR.getDefaultState());
        });
        when(world.getTileEntity(any(BlockPos.class))).thenAnswer(inv -> tiles.get(inv.getArgument(0)));
        return world;
    }

    private static HydroponicChamberTile createChamber(World world) {
        HydroponicChamberTile tile = new HydroponicChamberTile();
        tile.setWorld(world);
        tile.setPos(POS);
        return tile;
    }

    private static BaseCropCard simpleCrop(boolean catalyzed) {
        return new BaseCropCard(
                catalyzed ? "test_cat" : "test_plain",
                new CropProperties(2, 0, 2, 0, 2, 0),
                new String[]{"Test"},
                () -> new ItemStack(Items.WHEAT),
                catalyzed);
    }

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        TileEntity.register("cropmanager:hydroponic_chamber", HydroponicChamberTile.class);
    }

    @Test
    public void c1_fullCycleAndDomeAndHeatSpeedScaling() throws Exception {
        Map<BlockPos, IBlockState> blocks = new HashMap<>();
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        blocks.put(POS.up(), CropManager.GREENHOUSE_GLASS.getDefaultState());
        blocks.put(POS.down(), Blocks.FARMLAND.getDefaultState());
        World world = mockWorld(blocks, tiles);

        HydroponicChamberTile tile = createChamber(world);
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, new ItemStack(new DummySeedItem(simpleCrop(false))));
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 4000), true);
        tile.energy.setEnergyStored(20000);

        // Base speed: step = 4 per tick -> 200 ticks = 800 scaled units
        for (int i = 0; i < 200; i++) {
            assertTrue("Expected active on tick " + i, tile.processTick());
        }
        assertEquals(Items.WHEAT, tile.inventory.getStackInSlot(HydroponicChamberTile.SLOT_OUT_START).getItem());
        assertEquals(1, tile.inventory.getStackInSlot(HydroponicChamberTile.SLOT_OUT_START).getCount());
        assertEquals(3500, tile.water.getFluidAmount());
        assertEquals(20000 - 200 * 16, tile.energy.getEnergyStored(), 0.01);
        assertFalse(tile.domeBonus);
        assertFalse(tile.heatBonus);

        // Add full 3x3 greenhouse dome (+25% -> +1) and adjacent heat (+50% -> +2) => step = 7
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                blocks.put(POS.up().add(dx, 0, dz), CropManager.GREENHOUSE_GLASS.getDefaultState());
            }
        }
        FakeHeatTile heater = new FakeHeatTile(20);
        tiles.put(POS.east(), heater);
        tile.refreshStructure(simpleCrop(false));
        assertTrue(tile.domeBonus);
        assertTrue(tile.heatBonus);

        // 800 / 7 = 114.28 -> completes on tick 115
        for (int i = 0; i < 115; i++) {
            assertTrue(tile.processTick());
        }
        assertEquals(2, tile.inventory.getStackInSlot(HydroponicChamberTile.SLOT_OUT_START).getCount());
        assertEquals(115 * 20, heater.drawn);
    }

    @Test
    public void c1_catalyzedCropConsumesCatalyticFertilizerByCharges() throws Exception {
        Map<BlockPos, IBlockState> blocks = new HashMap<>();
        blocks.put(POS.up(), CropManager.GREENHOUSE_GLASS.getDefaultState());
        blocks.put(POS.down(), Blocks.DIRT.getDefaultState());
        World world = mockWorld(blocks, new HashMap<>());

        HydroponicChamberTile tile = createChamber(world);
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, new ItemStack(new DummySeedItem(simpleCrop(true))));
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 4000), true);
        tile.energy.setEnergyStored(50000);

        // Without catalytic fertilizer: stalls with 0 EU spent
        assertFalse(tile.processTick());
        assertEquals("hydroponic_no_catalyst", tile.condition);
        assertEquals(50000, tile.energy.getEnergyStored(), 0.01);

        // Insert 1 Catalytic Fertilizer (3 charges by default) -> runs 1 full cycle and leaves 2 charges
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_CATALYST, new ItemStack(CropManager.CATALYTIC_FERTILIZER, 1));
        for (int i = 0; i < 200; i++) {
            assertTrue(tile.processTick());
        }
        assertTrue(tile.inventory.getStackInSlot(HydroponicChamberTile.SLOT_CATALYST).isEmpty());
        assertEquals(ManagerConfig.catalyticCharges - 1, tile.catalystCharges);
        assertEquals(1, tile.inventory.getStackInSlot(HydroponicChamberTile.SLOT_OUT_START).getCount());
    }

    @Test
    public void c2_structuralAndResourceGatesSpendZeroEuAndResetOnSeedChange() throws Exception {
        Map<BlockPos, IBlockState> blocks = new HashMap<>();
        World world = mockWorld(blocks, new HashMap<>());
        HydroponicChamberTile tile = createChamber(world);
        tile.energy.setEnergyStored(10000);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 4000), true);

        // No seed
        assertFalse(tile.processTick());
        assertEquals("hydroponic_no_seed", tile.condition);

        // Missing glass above
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, new ItemStack(new DummySeedItem(simpleCrop(false))));
        assertFalse(tile.processTick());
        assertEquals("hydroponic_no_glass", tile.condition);
        assertEquals(10000, tile.energy.getEnergyStored(), 0.01);

        // Glass present, missing subsoil
        blocks.put(POS.up(), CropManager.GREENHOUSE_GLASS.getDefaultState());
        tile.refreshStructure(simpleCrop(false));
        assertFalse(tile.processTick());
        assertEquals("hydroponic_no_subsoil", tile.condition);
        assertEquals(10000, tile.energy.getEnergyStored(), 0.01);

        // Radbloom requires radioactive_soil specifically (dirt is rejected)
        RadbloomCropCard rad = new RadbloomCropCard(() -> new ItemStack(Items.EMERALD));
        blocks.put(POS.down(), Blocks.DIRT.getDefaultState());
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, new ItemStack(new DummySeedItem(rad)));
        tile.refreshStructure(rad);
        assertFalse(tile.hasSubsoil);
        assertFalse(tile.processTick());
        assertEquals("hydroponic_no_subsoil", tile.condition);

        blocks.put(POS.down(), CropManager.RADIOACTIVE_SOIL.getDefaultState());
        tile.refreshStructure(rad);
        assertTrue(tile.hasSubsoil);

        // Switch back to plain crop, test low water, low EU, full output, and progress reset
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, new ItemStack(new DummySeedItem(simpleCrop(false))));
        tile.water.setFluid(null);
        assertFalse(tile.processTick());
        assertEquals("hydroponic_no_water", tile.condition);
        assertEquals(10000, tile.energy.getEnergyStored(), 0.01);

        tile.water.fill(new FluidStack(FluidRegistry.WATER, 2000), true);
        tile.energy.setEnergyStored(5);
        assertFalse(tile.processTick());
        assertEquals("no_energy", tile.condition);

        // Full output slots -> no_space with 0 EU spent
        tile.energy.setEnergyStored(10000);
        for (int s = HydroponicChamberTile.SLOT_OUT_START; s < HydroponicChamberTile.SLOT_OUT_END; s++) {
            tile.inventory.setStackInSlot(s, new ItemStack(Blocks.COBBLESTONE, 64));
        }
        assertFalse(tile.processTick());
        assertEquals("no_space", tile.condition);
        assertEquals(10000, tile.energy.getEnergyStored(), 0.01);

        // Clear output, advance progress, then swap seed -> progress resets
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_OUT_START, ItemStack.EMPTY);
        assertTrue(tile.processTick());
        assertTrue(tile.progress > 0);
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, ItemStack.EMPTY);
        assertEquals(0, tile.progress);
    }

    @Test
    public void c3_c4_c5_supportMachineAutomationNbtAndGuiRoundTrip() throws Exception {
        Map<BlockPos, IBlockState> blocks = new HashMap<>();
        blocks.put(POS.up(), CropManager.GREENHOUSE_GLASS.getDefaultState());
        blocks.put(POS.down(), Blocks.DIRT.getDefaultState());
        World world = mockWorld(blocks, new HashMap<>());

        HydroponicChamberTile tile = createChamber(world);
        assertTrue(tile.isSupportMachine());
        assertNotNull(tile.energySink());
        assertFalse(tile.inventory.isItemValid(HydroponicChamberTile.SLOT_UPG_START, new ItemStack(Blocks.DIRT)));
        assertFalse(tile.inventory.isItemValid(HydroponicChamberTile.SLOT_BATTERY, new ItemStack(Blocks.DIRT)));

        // C4: 6 faces + null automation rules
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED, new ItemStack(new DummySeedItem(simpleCrop(false))));
        tile.inventory.setStackInSlot(HydroponicChamberTile.SLOT_OUT_START, new ItemStack(Items.WHEAT, 3));
        EnumFacing[] faces = {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null};
        for (EnumFacing face : faces) {
            IItemHandler h = tile.itemsForFace(face);
            // Cannot extract seed bag
            assertTrue(h.extractItem(HydroponicChamberTile.SLOT_SEED, 1, true).isEmpty());
            // Cannot insert arbitrary items into seed or output slots
            assertFalse(h.insertItem(HydroponicChamberTile.SLOT_SEED, new ItemStack(Items.WHEAT), true).isEmpty());
            assertFalse(h.insertItem(HydroponicChamberTile.SLOT_OUT_START, new ItemStack(Items.WHEAT), true).isEmpty());
            // Can insert Catalytic Fertilizer into slot 1
            assertTrue(h.insertItem(HydroponicChamberTile.SLOT_CATALYST, new ItemStack(CropManager.CATALYTIC_FERTILIZER, 1), true).isEmpty());
            // Can extract from output slot
            assertEquals(1, h.extractItem(HydroponicChamberTile.SLOT_OUT_START, 1, true).getCount());
        }

        // Fluid tank filters null & non-water, blocks drain
        assertFalse(tile.water.canFillFluidType(null));
        assertFalse(tile.water.canFillFluidType(new FluidStack(FluidRegistry.LAVA, 1000)));
        assertTrue(tile.water.canFillFluidType(new FluidStack(FluidRegistry.WATER, 1000)));
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 1500), true);
        assertNull(tile.water.drain(500, true));

        // C5: NBT round-trip + NaN EU recovery
        tile.progress = 123;
        tile.catalystCharges = 2;
        tile.energy.setEnergyStored(Double.NaN);
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        HydroponicChamberTile loaded = createChamber(world);
        loaded.readFromNBT(saved);
        assertEquals(0.0, loaded.energy.getEnergyStored(), 0.0);
        assertEquals(123, loaded.progress);
        assertEquals(2, loaded.catalystCharges);
        assertEquals(1500, loaded.water.getFluidAmount());

        // GUI state round-trip
        tile.energy.setEnergyStored(12345);
        tile.hasGlass = true;
        tile.hasSubsoil = true;
        tile.domeBonus = true;
        tile.heatBonus = false;
        NBTTagCompound guiTag = tile.guiState();
        HydroponicChamberTile client = new HydroponicChamberTile();
        client.acceptGuiState(guiTag);
        assertEquals(12345, client.energy.getEnergyStored(), 0.01);
        assertTrue(client.hasGlass);
        assertTrue(client.hasSubsoil);
        assertTrue(client.domeBonus);
        assertFalse(client.heatBonus);
        assertEquals(1500, client.water.getFluidAmount());
        assertTrue(client.clientDuration >= 1);
    }

    @Test
    public void virtualCropTileProvidesSafeMetricsAndSubsoilChecks() throws Exception {
        BaseCropCard card = simpleCrop(false);
        VirtualCropTile virtual = new VirtualCropTile(null, POS, card, 12, 15, 8, CropManager.RADIOACTIVE_SOIL);
        assertEquals(card, virtual.getCrop());
        assertEquals(card.getMaxSize(), virtual.getCurrentSize());
        assertEquals(12, virtual.getStatGrowth());
        assertEquals(15, virtual.getStatGain());
        assertEquals(8, virtual.getStatResistance());
        assertEquals(15, virtual.getLightLevel());
        assertTrue(virtual.isBlockBelow(CropManager.RADIOACTIVE_SOIL));
        assertFalse(virtual.isBlockBelow(Blocks.DIRT));
        assertEquals(Items.WHEAT, card.getGain(virtual).getItem());
    }
}
