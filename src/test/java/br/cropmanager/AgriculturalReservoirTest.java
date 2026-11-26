package br.cropmanager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
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
    private static ForgeModContainer previousForgeMod;
    private static Field forgeInstanceField;

    @BeforeClass public static void boot() throws ReflectiveOperationException {
        Bootstrap.register();
        TestCapabilityHelper.ensureFluidCapabilities();
        if (TileEntity.getKey(AgriculturalReservoirTile.class) == null) {
            TileEntity.register("cropmanager:agricultural_reservoir", AgriculturalReservoirTile.class);
        }
        if (TileEntity.getKey(ReservoirCasingTile.class) == null) {
            TileEntity.register("cropmanager:reservoir_casing", ReservoirCasingTile.class);
        }
        if (TileEntity.getKey(FluidPipeTile.class) == null) {
            TileEntity.register("cropmanager:fluid_pipe", FluidPipeTile.class);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.RESERVOIR_CAPACITY.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.RESERVOIR_CAPACITY);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.RESERVOIR_RESERVE.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.RESERVOIR_RESERVE);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.RESERVOIR_PUMP.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.RESERVOIR_PUMP);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.AGRICULTURAL_RESERVOIR.getRegistryName())) {
            ForgeRegistries.ITEMS.register(new ItemBlock(CropManager.AGRICULTURAL_RESERVOIR)
                    .setRegistryName(CropManager.AGRICULTURAL_RESERVOIR.getRegistryName()));
        }
        forgeInstanceField = ForgeModContainer.class.getDeclaredField("INSTANCE");
        forgeInstanceField.setAccessible(true);
        previousForgeMod = (ForgeModContainer) forgeInstanceField.get(null);
        if (previousForgeMod == null) {
            forgeInstanceField.set(null, mock(ForgeModContainer.class));
        }
    }

    @AfterClass public static void restoreForge() throws ReflectiveOperationException {
        if (forgeInstanceField != null) {
            forgeInstanceField.set(null, previousForgeMod);
        }
    }

    private static final class MockReservoirStructure {
        final World world = mock(World.class);
        final BlockPos floorCenter;
        final BlockPos controllerPos;
        final IBlockState controllerState = CropManager.AGRICULTURAL_RESERVOIR.getDefaultState()
                .withProperty(AgriculturalReservoirBlock.FACING, EnumFacing.NORTH);
        final IBlockState casingState = CropManager.RESERVOIR_CASING.getDefaultState();
        final AgriculturalReservoirTile controller = new AgriculturalReservoirTile();
        final Map<BlockPos, ReservoirCasingTile> casings = new HashMap<>();
        final Map<BlockPos, IBlockState> overrides = new HashMap<>();
        final Map<BlockPos, TileEntity> externalTiles = new HashMap<>();
        boolean powered = false;

        MockReservoirStructure() throws ReflectiveOperationException {
            this(new BlockPos(0, 64, 0));
        }

        MockReservoirStructure(BlockPos floorCenter) throws ReflectiveOperationException {
            this.floorCenter = floorCenter;
            this.controllerPos = floorCenter.north().up();
            Field randField = World.class.getDeclaredField("rand");
            randField.setAccessible(true);
            randField.set(world, new Random(0L));
            Field providerField = World.class.getDeclaredField("provider");
            providerField.setAccessible(true);
            providerField.set(world, new WorldProviderSurface());
            Field addedToEnet = ic2.api.energy.prefab.BasicSink.class.getSuperclass().getDeclaredField("addedToEnet");
            addedToEnet.setAccessible(true);
            addedToEnet.setBoolean(controller.energy, true);
            GameRules gameRules = new GameRules();
            MinecraftServer server = mock(MinecraftServer.class);
            when(world.getHeight()).thenReturn(256);
            when(world.getGameRules()).thenReturn(gameRules);
            when(world.getMinecraftServer()).thenReturn(server);
            when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
            when(world.isBlockPowered(any(BlockPos.class))).thenAnswer(call -> powered);
            controller.setWorld(world);
            controller.setPos(controllerPos);
            for (int y = 0; y < AgriculturalReservoirTile.FIXED_HEIGHT; y++) {
                boolean cap = y == 0 || y == AgriculturalReservoirTile.FIXED_HEIGHT - 1;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (!cap && dx == 0 && dz == 0) continue;
                        BlockPos p = new BlockPos(floorCenter.getX() + dx, floorCenter.getY() + y, floorCenter.getZ() + dz);
                        if (!p.equals(controllerPos)) {
                            ReservoirCasingTile ct = new ReservoirCasingTile();
                            ct.setWorld(world);
                            ct.setPos(p);
                            casings.put(p, ct);
                        }
                    }
                }
            }
            when(world.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos pos = call.getArgument(0);
                if (overrides.containsKey(pos)) return overrides.get(pos);
                if (pos.equals(controllerPos)) return controllerState;
                if (casings.containsKey(pos)) return casingState;
                return Blocks.AIR.getDefaultState();
            });
            when(world.getTileEntity(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos pos = call.getArgument(0);
                if (overrides.containsKey(pos) && overrides.get(pos).getBlock() == Blocks.AIR) return null;
                if (pos.equals(controllerPos)) return controller;
                if (casings.containsKey(pos)) return casings.get(pos);
                return externalTiles.get(pos);
            });
            when(world.setBlockToAir(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos pos = call.getArgument(0);
                IBlockState current = world.getBlockState(pos);
                current.getBlock().breakBlock(world, pos, current);
                overrides.put(pos, Blocks.AIR.getDefaultState());
                return true;
            });
        }
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

    @Test public void formsWith41CasingsRejectsInvalidStructureAndDefersUnloaded() throws Exception {
        MockReservoirStructure s = new MockReservoirStructure();
        BlockPos topCenter = s.floorCenter.up(4);

        // Unloaded shell block during initial tryForm() defers without forming
        when(s.world.isBlockLoaded(eq(topCenter), eq(false))).thenReturn(false);
        s.controller.tryForm();
        assertFalse("Unloaded chunk must defer initial formation", s.controller.formed);

        // Once loaded, all 41 casings link cleanly
        when(s.world.isBlockLoaded(eq(topCenter), eq(false))).thenReturn(true);
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        assertEquals(41, s.casings.size());
        for (ReservoirCasingTile casing : s.casings.values()) {
            assertSame(s.controller, casing.controller());
            assertTrue(casing.isFormed());
        }

        // Unloaded shell block during update() defers without unforming or unlinking
        when(s.world.isBlockLoaded(eq(topCenter), eq(false))).thenReturn(false);
        when(s.world.getTotalWorldTime()).thenReturn(20L);
        s.controller.update();
        assertTrue("Unloaded chunk must defer validation in update()", s.controller.formed);
        assertSame(s.controller, s.casings.get(s.floorCenter).controller());

        // Core obstruction unforms and unlinks all casings
        when(s.world.isBlockLoaded(eq(topCenter), eq(false))).thenReturn(true);
        s.overrides.put(s.floorCenter.up(2), Blocks.STONE.getDefaultState());
        s.controller.update();
        assertFalse(s.controller.formed);
        for (ReservoirCasingTile casing : s.casings.values()) {
            assertNull(casing.controller());
            assertFalse(casing.isFormed());
        }

        // Wrong height (4-high shell with top cap removed) cannot form
        s.overrides.remove(s.floorCenter.up(2));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                s.overrides.put(topCenter.add(dx, 0, dz), Blocks.AIR.getDefaultState());
            }
        }
        s.controller.tryForm();
        assertFalse("Height != 5 without LegacyShape must not form", s.controller.formed);
    }

    @Test public void cleanBreakHarvestRoundTripAndCasingBreakPreserveContents() throws Exception {
        MockReservoirStructure s = new MockReservoirStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);

        EntityPlayer player = mock(EntityPlayer.class);
        player.world = s.world;
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);

        s.controller.inventory.setStackInSlot(AgriculturalReservoirTile.CAPACITY_SLOT,
                new ItemStack(CropManager.RESERVOIR_CAPACITY, 1));
        s.controller.inventory.setStackInSlot(AgriculturalReservoirTile.FIRST_MODULE_SLOT,
                new ItemStack(CropManager.RESERVOIR_RESERVE, 1));
        assertEquals(48000, s.controller.water.getCapacity());
        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 18000), true);
        s.controller.weed.fill(new FluidStack(FluidRegistry.LAVA, 12000), true);
        s.controller.energy.setEnergyStored(6400.0);
        assertTrue(s.controller.control(10, player)); // port 0: CLOSED -> INPUT
        assertTrue(s.controller.control(40, player)); // reserveStep[0] -> 1
        assertTrue(s.controller.control(40, player)); // reserveStep[0] -> 2 (50%)
        assertTrue(s.controller.control(2, player));  // redstoneModes[0] -> 1 (HIGH)
        assertTrue(s.controller.control(4, player));  // comparatorTank: BOTH -> 0

        // Breaking one casing unforms and unlinks all casings while keeping controller contents & settings
        BlockPos cap = s.floorCenter.up(4);
        CropManager.RESERVOIR_CASING.breakBlock(s.world, cap, s.casingState);
        assertFalse(s.controller.formed);
        for (ReservoirCasingTile casing : s.casings.values()) {
            assertNull(casing.controller());
        }
        assertEquals(18000, s.controller.water.getFluidAmount());
        assertEquals(12000, s.controller.weed.getFluidAmount());
        assertEquals(6400.0, s.controller.getEnergy(), 1e-6);
        assertEquals(1, s.controller.portMode(0));
        assertEquals(2, s.controller.reserveStep(0));
        assertEquals(1, s.controller.redstoneMode(0));
        assertEquals(0, s.controller.comparatorTank());

        // Re-form, then clean break controller with removedByPlayer + harvestBlock
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        assertTrue(CropManager.AGRICULTURAL_RESERVOIR.removedByPlayer(
                s.controllerState, s.world, s.controllerPos, player, true));
        assertSame(s.controller, s.world.getTileEntity(s.controllerPos));

        clearInvocations(s.world);
        CropManager.AGRICULTURAL_RESERVOIR.harvestBlock(
                s.world, player, s.controllerPos, s.controllerState, s.controller, new ItemStack(Items.IRON_PICKAXE));
        ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
        verify(s.world).spawnEntity(captor.capture());
        ItemStack drop = ((EntityItem) captor.getValue()).getItem();
        assertEquals(Item.getItemFromBlock(CropManager.AGRICULTURAL_RESERVOIR), drop.getItem());
        NBTTagCompound tag = drop.getSubCompound("BlockEntityTag");
        assertNotNull(tag);
        assertFalse(tag.getBoolean("ReservoirFormed"));
        assertFalse(tag.hasKey("x"));
        assertFalse(tag.hasKey("y"));
        assertFalse(tag.hasKey("z"));
        assertFalse(tag.hasKey("StructureCenter"));
        assertFalse(tag.hasKey("StructureBaseY"));
        assertFalse(s.controller.formed);
        for (ReservoirCasingTile casing : s.casings.values()) {
            assertNull(casing.controller());
        }
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(cap, CropManager.RESERVOIR_CASING);

        // Re-place at a different world coordinate
        BlockPos newFloorCenter = new BlockPos(32, 70, 32);
        MockReservoirStructure s2 = new MockReservoirStructure(newFloorCenter);
        assertTrue(ItemBlock.setTileEntityNBT(s2.world, player, s2.controllerPos, drop));
        CropManager.AGRICULTURAL_RESERVOIR.onBlockPlacedBy(
                s2.world, s2.controllerPos, s2.controllerState, player, drop);

        assertTrue(s2.controller.formed);
        assertEquals(s2.controllerPos, s2.controller.getPos());
        assertEquals(18000, s2.controller.water.getFluidAmount());
        assertEquals(12000, s2.controller.weed.getFluidAmount());
        assertEquals(6400.0, s2.controller.getEnergy(), 1e-6);
        assertEquals(48000, s2.controller.water.getCapacity());
        assertTrue(s2.controller.hasModule(2));
        assertEquals(1, s2.controller.portMode(0));
        assertEquals(2, s2.controller.reserveStep(0));
        assertEquals(1, s2.controller.redstoneMode(0));
        assertEquals(0, s2.controller.comparatorTank());
    }

    @Test public void comparatorSignalCoversUnformedEmptyPartialFullAndShellUpdates() throws Exception {
        assertTrue(CropManager.AGRICULTURAL_RESERVOIR.hasComparatorInputOverride(CropManager.AGRICULTURAL_RESERVOIR.getDefaultState()));
        assertTrue(CropManager.RESERVOIR_CASING.hasComparatorInputOverride(CropManager.RESERVOIR_CASING.getDefaultState()));

        MockReservoirStructure s = new MockReservoirStructure();
        BlockPos eastCasing = s.floorCenter.east();
        BlockPos topCasing = s.floorCenter.up(4);

        // Unformed with fluid -> 0 on controller and casings
        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 16000), true);
        assertEquals(0, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(0, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));

        // Formed empty -> 0
        s.controller.water.drain(16000, true);
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        assertEquals(0, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(0, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));

        // Switch comparatorTank from BOTH(2) -> Tank 0(0)
        EntityPlayer player = mock(EntityPlayer.class);
        player.world = s.world;
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        assertTrue(s.controller.control(4, player));
        assertEquals(0, s.controller.comparatorTank());

        clearInvocations(s.world);
        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 1), true);
        assertEquals(1, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(1, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));
        assertEquals(1, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, topCasing));
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(s.controllerPos, CropManager.AGRICULTURAL_RESERVOIR);
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(eastCasing, CropManager.RESERVOIR_CASING);
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(topCasing, CropManager.RESERVOIR_CASING);

        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 15999), true);
        assertEquals(8, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(8, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));

        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 15999), true);
        assertEquals(14, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(14, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));

        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 1), true);
        assertEquals(15, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(15, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));

        // Toggle comparatorTank -> Tank 1(1) (empty -> 0) and notify controller + casings
        clearInvocations(s.world);
        assertTrue(s.controller.control(4, player));
        assertEquals(1, s.controller.comparatorTank());
        assertEquals(0, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(0, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));
        verify(s.world).updateComparatorOutputLevel(s.controllerPos, CropManager.AGRICULTURAL_RESERVOIR);
        verify(s.world).updateComparatorOutputLevel(eastCasing, CropManager.RESERVOIR_CASING);

        // Fill Tank 1 to 50% -> 8; toggle back to BOTH(2) -> 48000/64000 -> 11
        s.controller.weed.fill(new FluidStack(FluidRegistry.LAVA, 16000), true);
        assertEquals(8, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));
        assertTrue(s.controller.control(4, player));
        assertEquals(2, s.controller.comparatorTank());
        assertEquals(11, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(11, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));

        // Breaking a casing resets controller + casing comparator to 0 and notifies casings
        clearInvocations(s.world);
        CropManager.RESERVOIR_CASING.breakBlock(s.world, topCasing, s.casingState);
        assertEquals(0, CropManager.AGRICULTURAL_RESERVOIR.getComparatorInputOverride(
                s.controllerState, s.world, s.controllerPos));
        assertEquals(0, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(s.controllerPos, CropManager.AGRICULTURAL_RESERVOIR);
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(eastCasing, CropManager.RESERVOIR_CASING);

        // Re-form, then break controller -> casing comparator resets to 0 and notifies casings
        s.controller.tryForm();
        assertEquals(11, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));
        clearInvocations(s.world);
        CropManager.AGRICULTURAL_RESERVOIR.breakBlock(s.world, s.controllerPos, s.controllerState);
        assertEquals(0, CropManager.RESERVOIR_CASING.getComparatorInputOverride(
                s.casingState, s.world, eastCasing));
        verify(s.world, atLeastOnce()).updateComparatorOutputLevel(eastCasing, CropManager.RESERVOIR_CASING);
    }

    @Test public void reserveValveStopsDrainPipePullAndAutoTransferAt25_50_75Percent() throws Exception {
        MockReservoirStructure s = new MockReservoirStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);

        EntityPlayer player = mock(EntityPlayer.class);
        player.world = s.world;
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        s.controller.inventory.setStackInSlot(AgriculturalReservoirTile.FIRST_MODULE_SLOT,
                new ItemStack(CropManager.RESERVOIR_RESERVE, 1));

        // Controller facing NORTH: world SOUTH casing -> OUTPUT; world NORTH casing -> INPUT
        BlockPos outCasingPos = s.floorCenter.south().up();
        BlockPos inCasingPos = s.floorCenter.north().up(2);
        ReservoirCasingTile outCasing = s.casings.get(outCasingPos);
        ReservoirCasingTile inCasing = s.casings.get(inCasingPos);
        IFluidHandler outPort = outCasing.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.SOUTH);
        IFluidHandler inPort = inCasing.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertNotNull(outPort);
        assertNotNull(inPort);

        // Attach a FluidPipe pulling from outCasingPos into an external destination tank
        BlockPos pipePos = outCasingPos.south();
        BlockPos destPos = pipePos.south();
        FluidTank pipeDest = new FluidTank(64000);
        pipeDest.setCanDrain(false);
        FluidPipeTile pipe = new FluidPipeTile(FluidPipeBlock.Kind.PIPE);
        pipe.setWorld(s.world);
        pipe.setPos(pipePos);
        s.overrides.put(pipePos, CropManager.FLUID_PIPE.getDefaultState());
        s.externalTiles.put(pipePos, pipe);
        TileEntity destTile = new TileEntity() {
            @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, EnumFacing facing) {
                return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(cap, facing);
            }
            @Override public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, EnumFacing facing) {
                return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                        ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(pipeDest)
                        : super.getCapability(cap, facing);
            }
        };
        s.externalTiles.put(destPos, destTile);

        s.controller.energy.setEnergyStored(20000.0);
        FluidTank autoDest = new FluidTank(64000);

        int[] expectedReserves = {8000, 16000, 24000};
        for (int step = 1; step <= 3; step++) {
            assertTrue(s.controller.control(40, player));
            assertEquals(step, s.controller.reserveStep(0));
            int reserve = expectedReserves[step - 1];

            s.controller.water.setFluid(new FluidStack(FluidRegistry.WATER, 30000));
            pipeDest.setFluid(null);
            for (int t = 0; t < 500; t++) {
                pipe.update();
            }
            assertEquals("FluidPipe must stop pulling at reserve " + reserve,
                    reserve, s.controller.water.getFluidAmount());
            assertEquals(30000 - reserve, pipeDest.getFluidAmount());
            assertNull(outPort.drain(1000, true));
            assertFalse(outPort.getTankProperties()[0].canDrain());
            int beforeAuto = autoDest.getFluidAmount();
            s.controller.transferToHandler(0, autoDest);
            assertEquals(beforeAuto, autoDest.getFluidAmount());

            // INPUT port remains unrestricted up to capacity (32000 mB)
            assertEquals(32000 - reserve, inPort.fill(new FluidStack(FluidRegistry.WATER, 40000), true));
            assertEquals(32000, s.controller.water.getFluidAmount());
        }

        // Also check tank 1 (weed) 25% reserve independently
        assertTrue(s.controller.control(41, player));
        assertEquals(1, s.controller.reserveStep(1));
        s.controller.water.setFluid(null);
        s.controller.weed.setFluid(new FluidStack(FluidRegistry.LAVA, 12000));
        pipeDest.setFluid(null);
        for (int t = 0; t < 100; t++) {
            pipe.update();
        }
        assertEquals(8000, s.controller.weed.getFluidAmount());
        assertEquals(4000, pipeDest.getFluidAmount());
        assertNull(outPort.drain(100, true));
    }

    @Test public void redstoneModesGateAutoTransferToFarmTargetsWhileKeepingInputOpen() throws Exception {
        MockReservoirStructure s = new MockReservoirStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);

        EntityPlayer player = mock(EntityPlayer.class);
        player.world = s.world;
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);

        s.controller.water.fill(new FluidStack(FluidRegistry.WATER, 10000), true);
        s.controller.energy.setEnergyStored(5000.0);

        // Attach a real ManagerTile adjacent to the SOUTH OUTPUT casing
        BlockPos targetPos = s.floorCenter.south().up().south();
        ManagerTile manager = new ManagerTile();
        manager.setWorld(s.world);
        manager.setPos(targetPos);
        s.externalTiles.put(targetPos, manager);

        BlockPos inCasingPos = s.floorCenter.north().up(2);
        IFluidHandler inPort = s.casings.get(inCasingPos)
                .getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.NORTH);
        assertNotNull(inPort);

        // Mode 1: HIGH — blocked when unpowered, transfers when powered, INPUT stays open
        assertTrue(s.controller.control(2, player));
        assertEquals(1, s.controller.redstoneMode(0));
        s.powered = false;
        when(s.world.getTotalWorldTime()).thenReturn(20L);
        s.controller.update();
        assertEquals(0, manager.water.getFluidAmount());
        assertEquals(500, inPort.fill(new FluidStack(FluidRegistry.WATER, 500), true));

        s.powered = true;
        when(s.world.getTotalWorldTime()).thenReturn(40L);
        s.controller.update();
        assertEquals(1000, manager.water.getFluidAmount());

        // Mode 2: LOW — blocked when powered, transfers when unpowered, INPUT stays open
        assertTrue(s.controller.control(2, player));
        assertEquals(2, s.controller.redstoneMode(0));
        s.powered = true;
        when(s.world.getTotalWorldTime()).thenReturn(60L);
        s.controller.update();
        assertEquals(1000, manager.water.getFluidAmount());
        assertEquals(500, inPort.fill(new FluidStack(FluidRegistry.WATER, 500), true));

        s.powered = false;
        when(s.world.getTotalWorldTime()).thenReturn(80L);
        s.controller.update();
        assertEquals(2000, manager.water.getFluidAmount());
    }
}
