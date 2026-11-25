package br.cropmanager;

import ic2.api.energy.tile.IHeatSource;
import ic2.api.info.Info;
import ic2.api.item.ElectricItem;
import ic2.api.item.IC2Items;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.IItemAPI;
import ic2.api.upgrade.IEnergyStorageUpgrade;
import ic2.api.upgrade.IProcessingUpgrade;
import ic2.api.upgrade.ITransformerUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ref.FluidName;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários headless (Archetype C + cadeia completa sem calor) para
 * {@link GrinGrinderTile}, {@link GrinExtractorTile} e {@link WeedExFabricatorTile}.
 */
public final class GrinChainTest {
    private static final EnumFacing[] ALL_FACES = {
            EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH,
            EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null
    };

    private static Object previousIc2Items;
    private static Field ic2AvailableField;
    private static Object previousIc2Available;
    private static Capability<IItemHandler> previousItemCap;
    private static Capability<IFluidHandler> previousFluidCap;
    private static Field weedExInstanceField;
    private static Fluid previousWeedExFluid;

    private static Item grinPowderItem;
    private static Item overclockerItem;
    private static Item storageUpgradeItem;
    private static Item transformerUpgradeItem;
    private static Item batteryItem;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void bootstrap() throws ReflectiveOperationException {
        Bootstrap.register();

        ic2AvailableField = Info.class.getDeclaredField("ic2Available");
        ic2AvailableField.setAccessible(true);
        previousIc2Available = ic2AvailableField.get(null);
        ic2AvailableField.set(null, Boolean.FALSE);

        registerTile("cropmanager:grin_grinder", GrinGrinderTile.class);
        registerTile("cropmanager:grin_extractor", GrinExtractorTile.class);
        registerTile("cropmanager:weed_ex_fabricator", WeedExFabricatorTile.class);
        registerTile("cropmanager:thermal_pipe", ThermalPipeTile.class);

        registerItemIfMissing(CropManager.GRINDING_BLADE);
        registerItemIfMissing(CropManager.WIRELESS_EU);

        grinPowderItem = registerTestItem("grin_powder", new Item().setMaxStackSize(64));
        overclockerItem = registerTestItem("overclocker", new TestOverclockerUpgrade());
        storageUpgradeItem = registerTestItem("energy_storage", new TestStorageUpgrade());
        transformerUpgradeItem = registerTestItem("transformer", new TestTransformerUpgrade());
        batteryItem = registerTestItem("battery", new TestBatteryItem());

        Field apiField = IC2Items.class.getDeclaredField("instance");
        apiField.setAccessible(true);
        previousIc2Items = apiField.get(null);
        IItemAPI testApi = mock(IItemAPI.class);
        when(testApi.getItemStack("crop_res", "grin_powder")).thenAnswer(inv -> new ItemStack(grinPowderItem));
        when(testApi.getItemStack("upgrade", "overclocker")).thenAnswer(inv -> new ItemStack(overclockerItem));
        when(testApi.getItemStack("upgrade", "energy_storage")).thenAnswer(inv -> new ItemStack(storageUpgradeItem));
        when(testApi.getItemStack("upgrade", "transformer")).thenAnswer(inv -> new ItemStack(transformerUpgradeItem));
        apiField.set(null, testApi);

        if (!FluidRegistry.isFluidRegistered(CropManager.GRIN_EXTRACT.getName())) {
            FluidRegistry.registerFluid(CropManager.GRIN_EXTRACT);
        }

        weedExInstanceField = FluidName.class.getDeclaredField("instance");
        weedExInstanceField.setAccessible(true);
        previousWeedExFluid = (Fluid) weedExInstanceField.get(FluidName.weed_ex);
        if (!FluidName.weed_ex.hasInstance()) {
            Fluid weedEx = FluidRegistry.getFluid("ic2weed_ex");
            if (weedEx == null) {
                weedEx = new Fluid("ic2weed_ex",
                        new ResourceLocation("ic2", "blocks/fluid/weed_ex_still"),
                        new ResourceLocation("ic2", "blocks/fluid/weed_ex_flow"));
                FluidRegistry.registerFluid(weedEx);
            }
            FluidName.weed_ex.setInstance(weedEx);
        }

        Field itemField = CapabilityItemHandler.class.getDeclaredField("ITEM_HANDLER_CAPABILITY");
        itemField.setAccessible(true);
        previousItemCap = (Capability<IItemHandler>) itemField.get(null);
        Capability<IItemHandler> itemCap = mock(Capability.class);
        when(itemCap.cast(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        itemField.set(null, itemCap);

        TestCapabilityHelper.ensureFluidCapabilities();

        Field fluidField = CapabilityFluidHandler.class.getDeclaredField("FLUID_HANDLER_CAPABILITY");
        fluidField.setAccessible(true);
        previousFluidCap = (Capability<IFluidHandler>) fluidField.get(null);
        Capability<IFluidHandler> fluidCap = mock(Capability.class);
        when(fluidCap.cast(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        fluidField.set(null, fluidCap);
    }

    @AfterClass
    public static void restoreStatics() throws ReflectiveOperationException {
        if (ic2AvailableField != null) {
            ic2AvailableField.set(null, previousIc2Available);
        }

        Field apiField = IC2Items.class.getDeclaredField("instance");
        apiField.setAccessible(true);
        apiField.set(null, previousIc2Items);

        if (weedExInstanceField != null) {
            weedExInstanceField.set(FluidName.weed_ex, previousWeedExFluid);
        }

        Field itemField = CapabilityItemHandler.class.getDeclaredField("ITEM_HANDLER_CAPABILITY");
        itemField.setAccessible(true);
        itemField.set(null, previousItemCap);

        Field fluidField = CapabilityFluidHandler.class.getDeclaredField("FLUID_HANDLER_CAPABILITY");
        fluidField.setAccessible(true);
        if (previousFluidCap != null) {
            fluidField.set(null, previousFluidCap);
        } else {
            TestCapabilityHelper.ensureFluidCapabilities();
        }
    }

    private static void registerTile(String id, Class<? extends TileEntity> cls) {
        if (TileEntity.getKey(cls) == null) TileEntity.register(id, cls);
    }

    private static void registerItemIfMissing(Item item) {
        if (!ForgeRegistries.ITEMS.containsKey(item.getRegistryName())) {
            ForgeRegistries.ITEMS.register(item);
        }
    }

    private static Item registerTestItem(String name, Item item) {
        ResourceLocation key = new ResourceLocation("cropmanager_test", name);
        Item existing = ForgeRegistries.ITEMS.getValue(key);
        if (existing != null) return existing;
        item.setRegistryName(key);
        ForgeRegistries.ITEMS.register(item);
        return item;
    }

    private static final class SimulatedWorld {
        final World world;
        final Map<BlockPos, IBlockState> states = new HashMap<>();
        final Map<BlockPos, TileEntity> tiles = new HashMap<>();

        SimulatedWorld() throws ReflectiveOperationException {
            world = mock(World.class);
            Field remote = World.class.getDeclaredField("isRemote");
            remote.setAccessible(true);
            remote.setBoolean(world, false);
            Field provider = World.class.getDeclaredField("provider");
            provider.setAccessible(true);
            provider.set(world, new WorldProviderSurface());
            when(world.getHeight()).thenReturn(256);
            when(world.isBlockLoaded(any(BlockPos.class), anyBoolean())).thenReturn(true);
            when(world.getBlockState(any(BlockPos.class))).thenAnswer(inv ->
                    states.getOrDefault(inv.getArgument(0), Blocks.AIR.getDefaultState()));
            when(world.setBlockState(any(BlockPos.class), any(IBlockState.class), anyInt())).thenAnswer(inv -> {
                BlockPos pos = inv.getArgument(0);
                IBlockState next = inv.getArgument(1);
                IBlockState prev = states.getOrDefault(pos, Blocks.AIR.getDefaultState());
                states.put(pos, next);
                TileEntity existing = tiles.get(pos);
                if (existing != null && existing.shouldRefresh(world, pos, prev, next)) {
                    existing.invalidate();
                    tiles.remove(pos);
                }
                return true;
            });
            when(world.getTileEntity(any(BlockPos.class))).thenAnswer(inv -> tiles.get(inv.getArgument(0)));
        }

        <T extends TileEntity> T place(BlockPos pos, Block block, T tile) {
            states.put(pos, block.getDefaultState());
            tile.setWorld(world);
            tile.setPos(pos);
            tiles.put(pos, tile);
            tile.onLoad();
            return tile;
        }
    }

    private static EntityPlayer mockPlayer(World world) {
        EntityPlayer player = mock(EntityPlayer.class);
        player.world = world;
        when(player.getDistanceSq(any(BlockPos.class))).thenReturn(1.0);
        return player;
    }

    private static void updateWithMockBattery(Runnable action, double dischargeEu) {
        IElectricItemManager prevManager = ElectricItem.manager;
        IElectricItemManager mockManager = mock(IElectricItemManager.class);
        when(mockManager.discharge(any(ItemStack.class), any(Double.class), anyInt(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(dischargeEu);
        try {
            ElectricItem.manager = mockManager;
            action.run();
        } finally {
            ElectricItem.manager = prevManager;
        }
    }

    private static abstract class TestUpgradeBase extends Item implements IUpgradeItem {
        @Override public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> types) {
            return types != null && !types.isEmpty();
        }
        @Override public boolean onTick(ItemStack stack, IUpgradableBlock parent) { return false; }
        @Override public Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock parent, Collection<ItemStack> out) {
            return Collections.emptyList();
        }
    }

    private static final class TestOverclockerUpgrade extends TestUpgradeBase implements IProcessingUpgrade {
        @Override public double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock parent) { return 0.7; }
        @Override public double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock parent) { return 1.6; }
        @Override public int getExtraProcessTime(ItemStack stack, IUpgradableBlock parent) { return 0; }
        @Override public int getExtraEnergyDemand(ItemStack stack, IUpgradableBlock parent) { return 0; }
    }

    private static final class TestStorageUpgrade extends TestUpgradeBase implements IEnergyStorageUpgrade {
        @Override public int getExtraEnergyStorage(ItemStack stack, IUpgradableBlock parent) { return 10000; }
        @Override public double getEnergyStorageMultiplier(ItemStack stack, IUpgradableBlock parent) { return 1.0; }
    }

    private static final class TestTransformerUpgrade extends TestUpgradeBase implements ITransformerUpgrade {
        @Override public int getExtraTier(ItemStack stack, IUpgradableBlock parent) { return 1; }
    }

    private static final class TestBatteryItem extends Item implements IElectricItem {
        @Override public boolean canProvideEnergy(ItemStack stack) { return true; }
        @Override public double getMaxCharge(ItemStack stack) { return 10000; }
        @Override public int getTier(ItemStack stack) { return 1; }
        @Override public double getTransferLimit(ItemStack stack) { return 128; }
    }

    private static final class StubHeatSource extends TileEntity implements IHeatSource {
        @Override public int maxrequestHeatTick(EnumFacing side) { return 40; }
        @Override public int requestHeat(EnumFacing side, int request) { return Math.min(40, request); }
        @Override public int drawHeat(EnumFacing side, int request, boolean simulate) { return Math.min(40, request); }
    }

    @Test
    public void grinderShouldRefreshAndContainerSurviveActiveTransitions() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(0, 64, 0);
        GrinGrinderTile grinder = sim.place(pos, CropManager.GRIN_GRINDER, new GrinGrinderTile());

        IBlockState inactive = CropManager.GRIN_GRINDER.getDefaultState().withProperty(ManagerBlock.ACTIVE, false);
        IBlockState active = CropManager.GRIN_GRINDER.getDefaultState().withProperty(ManagerBlock.ACTIVE, true);
        assertFalse(grinder.shouldRefresh(sim.world, pos, inactive, active));
        assertFalse(grinder.shouldRefresh(sim.world, pos, active, inactive));
        assertTrue(grinder.shouldRefresh(sim.world, pos, inactive, CropManager.GRIN_EXTRACTOR.getDefaultState()));
        assertTrue(grinder.shouldRefresh(sim.world, pos, active, Blocks.AIR.getDefaultState()));

        EntityPlayer player = mockPlayer(sim.world);
        EquipmentContainer container = new EquipmentContainer(new InventoryPlayer(player), grinder);
        assertFalse(container.getSlot(2).isItemValid(new ItemStack(grinPowderItem)));
        assertTrue(grinder.usable(player));
        assertTrue(container.canInteractWith(player));
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));

        container.getSlot(0).putStack(new ItemStack(Items.SPIDER_EYE));
        container.getSlot(1).putStack(new ItemStack(CropManager.GRINDING_BLADE));
        grinder.energy.setEnergyStored(grinder.euPerTick * grinder.workTicks);

        grinder.update();
        assertTrue(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
        assertTrue(grinder.usable(player));
        assertTrue(container.canInteractWith(player));
        verify(sim.world, atLeastOnce()).observedNeighborChanged(any(BlockPos.class), eq(CropManager.GRIN_GRINDER), eq(pos));

        for (int i = 1; i < grinder.workTicks; i++) {
            grinder.update();
            assertTrue(grinder.usable(player));
            assertTrue(container.canInteractWith(player));
        }

        grinder.update();
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
        assertEquals(grinder, sim.world.getTileEntity(pos));
        assertTrue(grinder.usable(player));
        assertTrue(container.canInteractWith(player));

        EquipmentContainer reopened = new EquipmentContainer(
                new InventoryPlayer(player), (FarmTile) sim.world.getTileEntity(pos));
        assertTrue(reopened.canInteractWith(player));
        assertEquals(grinPowderItem, reopened.getSlot(2).getStack().getItem());
        assertEquals(1, reopened.getSlot(2).getStack().getCount());
        assertEquals(1, reopened.getSlot(1).getStack().getItemDamage());
        assertTrue(reopened.getSlot(0).getStack().isEmpty());
    }

    @Test
    public void grinderCompletesFullCycleConsumesExactEuAndWearsOrBreaksBlade() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(1, 64, 0);
        GrinGrinderTile grinder = sim.place(pos, CropManager.GRIN_GRINDER, new GrinGrinderTile());

        double expectedCost = grinder.euPerTick * grinder.workTicks;
        grinder.inventory.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE));
        grinder.inventory.setStackInSlot(1, new ItemStack(CropManager.GRINDING_BLADE));
        grinder.energy.setEnergyStored(expectedCost);

        for (int i = 0; i < grinder.workTicks; i++) {
            grinder.update();
        }
        assertEquals(0, grinder.progress);
        assertEquals("grinder_ready", grinder.condition);
        assertTrue(grinder.inventory.getStackInSlot(0).isEmpty());
        assertEquals(1, grinder.inventory.getStackInSlot(1).getItemDamage());
        assertEquals(1, grinder.inventory.getStackInSlot(2).getCount());
        assertEquals(grinPowderItem, grinder.inventory.getStackInSlot(2).getItem());
        assertEquals(0.0, grinder.getEnergy(), 0.0);

        // Second cycle with 1 durability remaining breaks the blade cleanly.
        ItemStack almostBroken = new ItemStack(CropManager.GRINDING_BLADE);
        almostBroken.setItemDamage(almostBroken.getMaxDamage() - 1);
        grinder.inventory.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE));
        grinder.inventory.setStackInSlot(1, almostBroken);
        grinder.energy.setEnergyStored(expectedCost);

        for (int i = 0; i < grinder.workTicks; i++) {
            grinder.update();
        }
        assertTrue(grinder.inventory.getStackInSlot(0).isEmpty());
        assertTrue("Blade must break when damage + 1 >= maxDamage", grinder.inventory.getStackInSlot(1).isEmpty());
        assertEquals(2, grinder.inventory.getStackInSlot(2).getCount());
        assertEquals(0.0, grinder.getEnergy(), 0.0);
    }

    @Test
    public void grinderEnforcesStallAndGuardConditions() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(2, 64, 0);
        GrinGrinderTile grinder = sim.place(pos, CropManager.GRIN_GRINDER, new GrinGrinderTile());

        // Missing spider eye.
        grinder.inventory.setStackInSlot(1, new ItemStack(CropManager.GRINDING_BLADE));
        grinder.energy.setEnergyStored(500.0);
        grinder.update();
        assertEquals("grinder_ingredients", grinder.condition);
        assertEquals(0, grinder.progress);
        assertEquals(500.0, grinder.getEnergy(), 0.0);
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));

        // Missing blade.
        grinder.inventory.setStackInSlot(1, ItemStack.EMPTY);
        grinder.inventory.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE));
        grinder.update();
        assertEquals("grinder_ingredients", grinder.condition);
        assertEquals(0, grinder.progress);
        assertEquals(500.0, grinder.getEnergy(), 0.0);

        // Exhausted blade (damage >= maxDamage).
        ItemStack broken = new ItemStack(CropManager.GRINDING_BLADE);
        broken.setItemDamage(broken.getMaxDamage());
        grinder.inventory.setStackInSlot(1, broken);
        grinder.update();
        assertEquals("grinder_ingredients", grinder.condition);
        assertEquals(0, grinder.progress);
        assertEquals(500.0, grinder.getEnergy(), 0.0);

        // Output blocked / full (incompatible item and full stack).
        grinder.inventory.setStackInSlot(1, new ItemStack(CropManager.GRINDING_BLADE));
        grinder.inventory.setStackInSlot(2, new ItemStack(Items.REDSTONE, 1));
        grinder.update();
        assertEquals("grinder_output", grinder.condition);
        assertEquals(0, grinder.progress);
        assertEquals(500.0, grinder.getEnergy(), 0.0);
        assertEquals(1, grinder.inventory.getStackInSlot(0).getCount());

        grinder.inventory.setStackInSlot(2, new ItemStack(grinPowderItem, 64));
        grinder.update();
        assertEquals("grinder_output", grinder.condition);
        assertEquals(0, grinder.progress);
        assertEquals(500.0, grinder.getEnergy(), 0.0);
        assertEquals(1, grinder.inventory.getStackInSlot(0).getCount());

        // Low EU.
        grinder.inventory.setStackInSlot(2, ItemStack.EMPTY);
        grinder.energy.setEnergyStored(grinder.euPerTick - 0.5);
        grinder.update();
        assertEquals("grinder_energy", grinder.condition);
        assertEquals(0, grinder.progress);
        assertEquals(1, grinder.inventory.getStackInSlot(0).getCount());
        assertEquals(grinder.euPerTick - 0.5, grinder.getEnergy(), 0.0);
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
    }

    @Test
    public void grinderEnforcesSixFaceAndNullAutomationUpgradesAndBattery() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(3, 64, 0);
        GrinGrinderTile grinder = sim.place(pos, CropManager.GRIN_GRINDER, new GrinGrinderTile());
        assertTrue(grinder.isSupportMachine());
        assertNotNull(grinder.energySink());

        for (EnumFacing face : ALL_FACES) {
            assertTrue(grinder.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face));
            IItemHandler handler = grinder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
            assertNotNull(handler);

            assertEquals(1, handler.insertItem(0, new ItemStack(Items.REDSTONE), true).getCount());
            assertTrue(handler.insertItem(0, new ItemStack(Items.SPIDER_EYE), true).isEmpty());
            assertTrue(handler.insertItem(1, new ItemStack(CropManager.GRINDING_BLADE), true).isEmpty());
            assertEquals(1, handler.insertItem(2, new ItemStack(grinPowderItem), true).getCount());
            assertEquals(1, handler.insertItem(3, new ItemStack(overclockerItem), true).getCount());
            assertEquals(1, handler.insertItem(7, new ItemStack(batteryItem), true).getCount());
        }

        IItemHandler north = grinder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertTrue(north.insertItem(0, new ItemStack(Items.SPIDER_EYE), false).isEmpty());
        assertTrue(north.insertItem(1, new ItemStack(CropManager.GRINDING_BLADE), false).isEmpty());
        grinder.inventory.setStackInSlot(2, new ItemStack(grinPowderItem, 2));
        grinder.inventory.setStackInSlot(3, new ItemStack(CropManager.WIRELESS_EU));
        grinder.inventory.setStackInSlot(7, new ItemStack(batteryItem));

        for (EnumFacing face : ALL_FACES) {
            IItemHandler handler = grinder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
            assertTrue(handler.extractItem(0, 1, true).isEmpty());
            assertTrue(handler.extractItem(1, 1, true).isEmpty());
            assertTrue(handler.extractItem(3, 1, true).isEmpty());
            assertTrue(handler.extractItem(7, 1, true).isEmpty());
            assertEquals(1, handler.extractItem(2, 1, true).getCount());
        }
        assertEquals(1, north.extractItem(2, 1, false).getCount());
        assertEquals(1, grinder.inventory.getStackInSlot(2).getCount());

        // Upgrades & battery slot guards.
        assertFalse(grinder.inventory.isItemValid(3, new ItemStack(CropManager.WEED_KILLER)));
        assertFalse(grinder.inventory.isItemValid(3, new ItemStack(Items.REDSTONE)));
        assertTrue(grinder.inventory.isItemValid(3, new ItemStack(overclockerItem)));
        assertTrue(grinder.inventory.isItemValid(4, new ItemStack(storageUpgradeItem)));
        assertTrue(grinder.inventory.isItemValid(5, new ItemStack(transformerUpgradeItem)));
        assertFalse(grinder.inventory.isItemValid(7, new ItemStack(Items.REDSTONE)));
        assertTrue(grinder.inventory.isItemValid(7, new ItemStack(batteryItem)));

        grinder.inventory.setStackInSlot(3, new ItemStack(overclockerItem));
        grinder.inventory.setStackInSlot(4, new ItemStack(storageUpgradeItem));
        grinder.inventory.setStackInSlot(5, new ItemStack(transformerUpgradeItem));
        updateWithMockBattery(grinder::update, 250.0);
        assertEquals(70, grinder.workTicks);
        assertEquals(8.0, grinder.euPerTick, 0.0);
        assertEquals(ManagerConfig.grinderEnergyCapacity + 10000.0, grinder.energy.getCapacity(), 0.0);
        assertEquals(ManagerConfig.grinderSinkTier + 1, grinder.energy.getSinkTier());
        assertTrue(grinder.getEnergy() > 0.0);
    }

    @Test
    public void grinderRoundTripsNbtGuiStateAndRecoversNonFiniteEu() {
        GrinGrinderTile source = new GrinGrinderTile();
        ItemStack blade = new ItemStack(CropManager.GRINDING_BLADE);
        blade.setItemDamage(17);
        source.inventory.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE, 5));
        source.inventory.setStackInSlot(1, blade);
        source.inventory.setStackInSlot(2, new ItemStack(grinPowderItem, 3));
        source.inventory.setStackInSlot(3, new ItemStack(CropManager.WIRELESS_EU));
        source.energy.setEnergyStored(4321.5);
        source.progress = 42;
        source.workTicks = 100;
        source.condition = "grinder_working";

        NBTTagCompound saved = source.writeToNBT(new NBTTagCompound());
        GrinGrinderTile restored = new GrinGrinderTile();
        restored.readFromNBT(saved);
        assertEquals(5, restored.inventory.getStackInSlot(0).getCount());
        assertEquals(17, restored.inventory.getStackInSlot(1).getItemDamage());
        assertEquals(3, restored.inventory.getStackInSlot(2).getCount());
        assertEquals(CropManager.WIRELESS_EU, restored.inventory.getStackInSlot(3).getItem());
        assertEquals(4321.5, restored.getEnergy(), 0.0);
        assertEquals(42, restored.progress);
        assertEquals(100, restored.workTicks);

        GrinGrinderTile client = new GrinGrinderTile();
        client.acceptGuiState(source.guiState());
        assertEquals(42, client.progress);
        assertEquals(100, client.workTicks);
        assertEquals(100, client.clientDuration);
        assertEquals("grinder_working", client.condition);
        assertEquals(4321.5, client.getEnergy(), 0.0);
        assertEquals(source.energy.getCapacity(), client.energy.getCapacity(), 0.0);
        assertEquals(source.energy.getSinkTier(), client.energy.getSinkTier());

        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            NBTTagCompound corrupt = source.writeToNBT(new NBTTagCompound());
            corrupt.getCompoundTag("IC2BasicSink").setDouble("energy", invalid);
            GrinGrinderTile fixed = new GrinGrinderTile();
            fixed.readFromNBT(corrupt);
            assertEquals(0.0, fixed.getEnergy(), 0.0);
        }
    }

    @Test
    public void extractorShouldRefreshAndContainerSurviveActiveTransitions() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(4, 64, 0);
        GrinExtractorTile extractor = sim.place(pos, CropManager.GRIN_EXTRACTOR, new GrinExtractorTile());

        IBlockState inactive = CropManager.GRIN_EXTRACTOR.getDefaultState().withProperty(ManagerBlock.ACTIVE, false);
        IBlockState active = CropManager.GRIN_EXTRACTOR.getDefaultState().withProperty(ManagerBlock.ACTIVE, true);
        assertFalse(extractor.shouldRefresh(sim.world, pos, inactive, active));
        assertFalse(extractor.shouldRefresh(sim.world, pos, active, inactive));
        assertTrue(extractor.shouldRefresh(sim.world, pos, inactive, CropManager.GRIN_GRINDER.getDefaultState()));
        assertTrue(extractor.shouldRefresh(sim.world, pos, active, Blocks.AIR.getDefaultState()));

        EntityPlayer player = mockPlayer(sim.world);
        EquipmentContainer container = new EquipmentContainer(new InventoryPlayer(player), extractor);
        assertFalse(container.getSlot(3).isItemValid(new ItemStack(grinPowderItem)));
        assertTrue(extractor.usable(player));
        assertTrue(container.canInteractWith(player));

        container.getSlot(0).putStack(new ItemStack(grinPowderItem));
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, ManagerConfig.extractorWaterPerRecipe), true);
        extractor.energy.setEnergyStored(extractor.euPerTick * extractor.workTicks);

        extractor.update();
        assertTrue(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
        assertTrue(extractor.usable(player));
        assertTrue(container.canInteractWith(player));
        verify(sim.world, atLeastOnce()).observedNeighborChanged(any(BlockPos.class), eq(CropManager.GRIN_EXTRACTOR), eq(pos));

        for (int i = 1; i < extractor.workTicks; i++) {
            extractor.update();
            assertTrue(extractor.usable(player));
            assertTrue(container.canInteractWith(player));
        }

        extractor.update();
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
        assertEquals(extractor, sim.world.getTileEntity(pos));
        assertTrue(extractor.usable(player));
        assertTrue(container.canInteractWith(player));

        EquipmentContainer reopened = new EquipmentContainer(
                new InventoryPlayer(player), (FarmTile) sim.world.getTileEntity(pos));
        assertTrue(reopened.canInteractWith(player));
        assertTrue(reopened.getSlot(0).getStack().isEmpty());
        assertEquals(ManagerConfig.extractorExtractPerRecipe, extractor.extract.getFluidAmount());
    }

    @Test
    public void extractorCompletesCycleWithoutHeatSourceOrThermalPipe() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(5, 64, 0);
        GrinExtractorTile extractor = sim.place(pos, CropManager.GRIN_EXTRACTOR, new GrinExtractorTile());

        for (EnumFacing face : EnumFacing.values()) {
            assertNull(sim.world.getTileEntity(pos.offset(face)));
        }

        double expectedCost = extractor.euPerTick * extractor.workTicks;
        extractor.inventory.setStackInSlot(0, new ItemStack(grinPowderItem));
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, ManagerConfig.extractorWaterPerRecipe), true);
        extractor.energy.setEnergyStored(expectedCost);

        for (int i = 0; i < extractor.workTicks; i++) {
            extractor.update();
        }
        assertEquals(0, extractor.progress);
        assertEquals("extractor_ready", extractor.condition);
        assertTrue(extractor.inventory.getStackInSlot(0).isEmpty());
        assertEquals(0, extractor.water.getFluidAmount());
        assertEquals(ManagerConfig.extractorExtractPerRecipe, extractor.extract.getFluidAmount());
        assertEquals(CropManager.GRIN_EXTRACT, extractor.extract.getFluid().getFluid());
        assertEquals(0.0, extractor.getEnergy(), 0.0);

        extractor.update();
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
        assertEquals("extractor_ingredients", extractor.condition);
    }

    @Test
    public void extractorEnforcesStallAndGuardConditions() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(6, 64, 0);
        GrinExtractorTile extractor = sim.place(pos, CropManager.GRIN_EXTRACTOR, new GrinExtractorTile());

        // Missing grin powder.
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, ManagerConfig.extractorWaterPerRecipe), true);
        extractor.energy.setEnergyStored(500.0);
        extractor.update();
        assertEquals("extractor_ingredients", extractor.condition);
        assertEquals(0, extractor.progress);
        assertEquals(500.0, extractor.getEnergy(), 0.0);
        assertEquals(ManagerConfig.extractorWaterPerRecipe, extractor.water.getFluidAmount());

        // Insufficient water.
        extractor.water.drain(extractor.water.getFluidAmount(), true);
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, ManagerConfig.extractorWaterPerRecipe - 1), true);
        extractor.inventory.setStackInSlot(0, new ItemStack(grinPowderItem));
        extractor.update();
        assertEquals("extractor_water", extractor.condition);
        assertEquals(0, extractor.progress);
        assertEquals(500.0, extractor.getEnergy(), 0.0);
        assertEquals(1, extractor.inventory.getStackInSlot(0).getCount());

        // Output tank full.
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, 1), true);
        extractor.extract.fill(new FluidStack(CropManager.GRIN_EXTRACT, extractor.extract.getCapacity()), true);
        extractor.update();
        assertEquals("extractor_output", extractor.condition);
        assertEquals(0, extractor.progress);
        assertEquals(500.0, extractor.getEnergy(), 0.0);
        assertEquals(1, extractor.inventory.getStackInSlot(0).getCount());
        assertEquals(ManagerConfig.extractorWaterPerRecipe, extractor.water.getFluidAmount());

        // Low EU.
        extractor.extract.drain(extractor.extract.getFluidAmount(), true);
        extractor.energy.setEnergyStored(extractor.euPerTick - 0.5);
        extractor.update();
        assertEquals("extractor_energy", extractor.condition);
        assertEquals(0, extractor.progress);
        assertEquals(1, extractor.inventory.getStackInSlot(0).getCount());
        assertEquals(ManagerConfig.extractorWaterPerRecipe, extractor.water.getFluidAmount());
        assertEquals(extractor.euPerTick - 0.5, extractor.getEnergy(), 0.0);
        assertFalse(sim.world.getBlockState(pos).getValue(ManagerBlock.ACTIVE));
    }

    @Test
    public void extractorEnforcesSixFaceAndNullCapabilitiesUpgradesNbtGuiAndNaNRecovery() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pos = new BlockPos(7, 64, 0);
        GrinExtractorTile extractor = sim.place(pos, CropManager.GRIN_EXTRACTOR, new GrinExtractorTile());
        assertTrue(extractor.isSupportMachine());
        assertNotNull(extractor.energySink());

        for (EnumFacing face : ALL_FACES) {
            assertTrue(extractor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face));
            assertTrue(extractor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face));

            IItemHandler items = extractor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
            assertNotNull(items);
            assertEquals(1, items.insertItem(0, new ItemStack(Items.SPIDER_EYE), true).getCount());
            assertTrue(items.insertItem(0, new ItemStack(grinPowderItem), true).isEmpty());
            assertEquals(1, items.insertItem(3, new ItemStack(Items.BUCKET), true).getCount());
            assertEquals(1, items.insertItem(4, new ItemStack(overclockerItem), true).getCount());
            assertEquals(1, items.insertItem(8, new ItemStack(batteryItem), true).getCount());

            IFluidHandler fluids = extractor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face);
            assertNotNull(fluids);
            IFluidTankProperties[] props = fluids.getTankProperties();
            assertEquals(2, props.length);
            assertTrue(props[0].canFillFluidType(new FluidStack(FluidRegistry.WATER, 1000)));
            assertFalse(props[0].canFillFluidType(null));
            assertFalse(props[0].canDrain());
            assertFalse(props[1].canFill());
            assertTrue(props[1].canDrain());
            assertFalse(props[1].canDrainFluidType(new FluidStack(CropManager.GRIN_EXTRACT, 1000)));
            assertFalse(props[1].canDrainFluidType(new FluidStack(FluidRegistry.WATER, 1000)));

            assertEquals(0, fluids.fill(null, true));
            assertEquals(0, fluids.fill(new FluidStack(FluidRegistry.LAVA, 500), true));
            assertEquals(0, fluids.fill(new FluidStack(CropManager.GRIN_EXTRACT, 500), true));
        }

        IItemHandler itemNorth = extractor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertTrue(itemNorth.insertItem(0, new ItemStack(grinPowderItem), false).isEmpty());
        extractor.inventory.setStackInSlot(3, new ItemStack(Items.BUCKET, 2));
        extractor.inventory.setStackInSlot(4, new ItemStack(CropManager.WIRELESS_EU));
        extractor.inventory.setStackInSlot(8, new ItemStack(batteryItem));
        for (EnumFacing face : ALL_FACES) {
            IItemHandler items = extractor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
            assertTrue(items.extractItem(0, 1, true).isEmpty());
            assertTrue(items.extractItem(4, 1, true).isEmpty());
            assertTrue(items.extractItem(8, 1, true).isEmpty());
            assertEquals(1, items.extractItem(3, 1, true).getCount());
        }
        assertEquals(1, itemNorth.extractItem(3, 1, false).getCount());
        assertEquals(1, extractor.inventory.getStackInSlot(3).getCount());

        IFluidHandler fluidUp = extractor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
        assertEquals(1000, fluidUp.fill(new FluidStack(FluidRegistry.WATER, 1000), true));
        assertNull("drain(int) must never drain the input water tank", fluidUp.drain(500, true));
        assertNull(fluidUp.drain(new FluidStack(FluidRegistry.WATER, 500), true));
        extractor.extract.fill(new FluidStack(CropManager.GRIN_EXTRACT, 750), true);
        IFluidTankProperties extractProps = fluidUp.getTankProperties()[1];
        assertTrue(extractProps.canDrainFluidType(new FluidStack(CropManager.GRIN_EXTRACT, 1000)));
        assertFalse(extractProps.canDrainFluidType(new FluidStack(FluidRegistry.WATER, 1000)));
        assertFalse(extractProps.canDrainFluidType(null));
        FluidStack drained = fluidUp.drain(250, true);
        assertNotNull(drained);
        assertEquals(CropManager.GRIN_EXTRACT, drained.getFluid());
        assertEquals(250, drained.amount);

        // Upgrades & battery slot validation.
        assertFalse(extractor.inventory.isItemValid(4, new ItemStack(CropManager.WEED_KILLER)));
        assertTrue(extractor.inventory.isItemValid(4, new ItemStack(overclockerItem)));
        assertTrue(extractor.inventory.isItemValid(5, new ItemStack(storageUpgradeItem)));
        assertTrue(extractor.inventory.isItemValid(6, new ItemStack(transformerUpgradeItem)));
        assertFalse(extractor.inventory.isItemValid(8, new ItemStack(Items.REDSTONE)));
        assertTrue(extractor.inventory.isItemValid(8, new ItemStack(batteryItem)));

        extractor.inventory.setStackInSlot(4, new ItemStack(overclockerItem));
        extractor.inventory.setStackInSlot(5, new ItemStack(storageUpgradeItem));
        extractor.inventory.setStackInSlot(6, new ItemStack(transformerUpgradeItem));
        updateWithMockBattery(extractor::update, 250.0);
        assertEquals(70, extractor.workTicks);
        assertEquals(8.0, extractor.euPerTick, 0.0);
        assertEquals(ManagerConfig.extractorEnergyCapacity + 10000.0, extractor.energy.getCapacity(), 0.0);
        assertEquals(ManagerConfig.extractorSinkTier + 1, extractor.energy.getSinkTier());
        assertTrue(extractor.getEnergy() > 0.0);

        // NBT + GUI sync + NaN recovery.
        extractor.energy.setEnergyStored(2345.0);
        extractor.progress = 33;
        extractor.condition = "extractor_working";
        NBTTagCompound saved = extractor.writeToNBT(new NBTTagCompound());
        GrinExtractorTile restored = new GrinExtractorTile();
        restored.readFromNBT(saved);
        assertEquals(1000, restored.water.getFluidAmount());
        assertEquals(500, restored.extract.getFluidAmount());
        assertEquals(2345.0, restored.getEnergy(), 0.0);
        assertEquals(33, restored.progress);
        assertEquals(70, restored.workTicks);
        assertEquals(1, restored.inventory.getStackInSlot(0).getCount());

        GrinExtractorTile client = new GrinExtractorTile();
        client.acceptGuiState(extractor.guiState());
        assertEquals(1000, client.water.getFluidAmount());
        assertEquals(500, client.extract.getFluidAmount());
        assertEquals(33, client.progress);
        assertEquals(70, client.workTicks);
        assertEquals("extractor_working", client.condition);
        assertEquals(2345.0, client.getEnergy(), 0.0);
        assertEquals(extractor.energy.getSinkTier(), client.energy.getSinkTier());

        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            NBTTagCompound corrupt = extractor.writeToNBT(new NBTTagCompound());
            corrupt.getCompoundTag("IC2BasicSink").setDouble("energy", invalid);
            GrinExtractorTile fixed = new GrinExtractorTile();
            fixed.readFromNBT(corrupt);
            assertEquals(0.0, fixed.getEnergy(), 0.0);
        }
    }

    @Test
    public void thermalPipeDisconnectsFromGrinExtractorButKeepsPipesAndHeatSources() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos pipePos = new BlockPos(8, 64, 0);
        sim.place(pipePos, CropManager.THERMAL_PIPE, new ThermalPipeTile());
        sim.place(pipePos.north(), CropManager.GRIN_EXTRACTOR, new GrinExtractorTile());
        sim.place(pipePos.south(), CropManager.THERMAL_PIPE, new ThermalPipeTile());
        sim.place(pipePos.east(), Blocks.FURNACE, new StubHeatSource());
        sim.place(pipePos.west(), CropManager.GRIN_GRINDER, new GrinGrinderTile());
        sim.place(pipePos.up(), CropManager.WEED_EX_FABRICATOR, new WeedExFabricatorTile());

        IBlockState actual = CropManager.THERMAL_PIPE.getActualState(
                CropManager.THERMAL_PIPE.getDefaultState(), sim.world, pipePos);
        assertFalse("Thermal Pipe must not connect to GrinExtractorTile", actual.getValue(ManagerBlock.NORTH));
        assertTrue("Thermal Pipe must connect to ThermalPipeTile", actual.getValue(ManagerBlock.SOUTH));
        assertTrue("Thermal Pipe must connect to IHeatSource", actual.getValue(ManagerBlock.EAST));
        assertFalse(actual.getValue(ManagerBlock.WEST));
        assertFalse(actual.getValue(ManagerBlock.UP));
        assertFalse(actual.getValue(ManagerBlock.DOWN));
    }

    @Test
    public void fullGrinChainProducesWeedExWithoutHeat() throws Exception {
        SimulatedWorld sim = new SimulatedWorld();
        BlockPos grinderPos = new BlockPos(10, 64, 0);
        BlockPos extractorPos = new BlockPos(12, 64, 0);
        BlockPos mixerPos = new BlockPos(14, 64, 0);

        GrinGrinderTile grinder = sim.place(grinderPos, CropManager.GRIN_GRINDER, new GrinGrinderTile());
        GrinExtractorTile extractor = sim.place(extractorPos, CropManager.GRIN_EXTRACTOR, new GrinExtractorTile());
        WeedExFabricatorTile mixer = sim.place(mixerPos, CropManager.WEED_EX_FABRICATOR, new WeedExFabricatorTile());

        // Stage 1: Grin Grinder turns 1 Spider Eye into 1 Grin Powder and wears Grinding Blade by +1.
        IItemHandler grinderItems = grinder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        assertTrue(grinderItems.insertItem(0, new ItemStack(Items.SPIDER_EYE), false).isEmpty());
        assertTrue(grinderItems.insertItem(1, new ItemStack(CropManager.GRINDING_BLADE), false).isEmpty());
        grinder.energy.setEnergyStored(grinder.euPerTick * grinder.workTicks);

        for (int i = 0; i < grinder.workTicks; i++) {
            grinder.update();
        }
        assertTrue(sim.world.getBlockState(grinderPos).getValue(ManagerBlock.ACTIVE));
        grinder.update();
        assertFalse(sim.world.getBlockState(grinderPos).getValue(ManagerBlock.ACTIVE));

        ItemStack powder = grinder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN)
                .extractItem(2, 1, false);
        assertEquals(1, powder.getCount());
        assertEquals(grinPowderItem, powder.getItem());
        assertTrue(grinder.inventory.getStackInSlot(0).isEmpty());
        assertEquals(1, grinder.inventory.getStackInSlot(1).getItemDamage());
        assertTrue(grinder.inventory.getStackInSlot(2).isEmpty());
        assertEquals(0.0, grinder.getEnergy(), 0.0);

        // Stage 2: Grin Extractor turns Grin Powder + Water into Grin Extract with zero heat sources.
        IItemHandler extractorItems = extractor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        IFluidHandler extractorFluids = extractor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertTrue(extractorItems.insertItem(0, powder, false).isEmpty());
        assertEquals(ManagerConfig.extractorWaterPerRecipe,
                extractorFluids.fill(new FluidStack(FluidRegistry.WATER, ManagerConfig.extractorWaterPerRecipe), true));
        extractor.energy.setEnergyStored(extractor.euPerTick * extractor.workTicks);

        for (int i = 0; i < extractor.workTicks; i++) {
            extractor.update();
        }
        assertTrue(sim.world.getBlockState(extractorPos).getValue(ManagerBlock.ACTIVE));
        extractor.update();
        assertFalse(sim.world.getBlockState(extractorPos).getValue(ManagerBlock.ACTIVE));

        assertTrue(extractor.inventory.getStackInSlot(0).isEmpty());
        assertEquals(0, extractor.water.getFluidAmount());
        assertEquals(0.0, extractor.getEnergy(), 0.0);

        FluidStack grinExtract = extractor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN)
                .drain(ManagerConfig.extractorExtractPerRecipe, true);
        assertNotNull(grinExtract);
        assertEquals(CropManager.GRIN_EXTRACT, grinExtract.getFluid());
        assertEquals(ManagerConfig.extractorExtractPerRecipe, grinExtract.amount);
        assertEquals(0, extractor.extract.getFluidAmount());

        // Stage 3: Weed-Ex Mixer turns Grin Extract + Redstone into Weed-Ex with zero heat sources.
        EntityPlayer player = mockPlayer(sim.world);
        EquipmentContainer mixerContainer = new EquipmentContainer(new InventoryPlayer(player), mixer);
        assertFalse(mixer.shouldRefresh(sim.world, mixerPos,
                CropManager.WEED_EX_FABRICATOR.getDefaultState().withProperty(ManagerBlock.ACTIVE, false),
                CropManager.WEED_EX_FABRICATOR.getDefaultState().withProperty(ManagerBlock.ACTIVE, true)));
        assertTrue(mixer.usable(player));
        assertTrue(mixerContainer.canInteractWith(player));

        IFluidHandler mixerFluidsIn = mixer.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
        IItemHandler mixerItems = mixer.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertEquals(ManagerConfig.fabricatorExtractPerRecipe, mixerFluidsIn.fill(grinExtract, true));
        assertTrue(mixerItems.insertItem(WeedExFabricatorTile.REDSTONE, new ItemStack(Items.REDSTONE), false).isEmpty());
        mixer.energy.setEnergyStored(mixer.euPerTick * mixer.workTicks);

        for (int i = 0; i < mixer.workTicks; i++) {
            mixer.update();
            assertTrue(mixerContainer.canInteractWith(player));
        }
        assertTrue(sim.world.getBlockState(mixerPos).getValue(ManagerBlock.ACTIVE));
        mixer.update();
        assertFalse(sim.world.getBlockState(mixerPos).getValue(ManagerBlock.ACTIVE));
        assertTrue(mixerContainer.canInteractWith(player));

        assertTrue(mixer.inventory.getStackInSlot(WeedExFabricatorTile.REDSTONE).isEmpty());
        assertEquals(0, mixer.extract.getFluidAmount());
        assertEquals(0.0, mixer.getEnergy(), 0.0);
        assertEquals(ManagerConfig.fabricatorWeedPerRecipe, mixer.weed.getFluidAmount());

        FluidStack weedExOut = mixer.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN)
                .drain(ManagerConfig.fabricatorWeedPerRecipe, true);
        assertNotNull(weedExOut);
        assertEquals(FluidName.weed_ex.getInstance(), weedExOut.getFluid());
        assertEquals(ManagerConfig.fabricatorWeedPerRecipe, weedExOut.amount);
        assertEquals(0, mixer.weed.getFluidAmount());
    }
}
