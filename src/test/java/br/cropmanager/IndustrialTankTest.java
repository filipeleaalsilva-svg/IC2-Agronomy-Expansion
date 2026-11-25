package br.cropmanager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Archetype B test suite for the EU-free 3x5x3 Industrial Fluid Reservoir. */
public final class IndustrialTankTest {
    private static ForgeModContainer previousForgeMod;
    private static Field forgeInstanceField;

    @BeforeClass
    public static void boot() throws ReflectiveOperationException {
        Bootstrap.register();
        TestCapabilityHelper.ensureFluidCapabilities();
        if (TileEntity.getKey(IndustrialTankTile.class) == null) {
            TileEntity.register("cropmanager:fluid_tank", IndustrialTankTile.class);
        }
        if (TileEntity.getKey(StorageCasingTile.class) == null) {
            TileEntity.register("cropmanager:storage_casing", StorageCasingTile.class);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.RESERVOIR_CAPACITY.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.RESERVOIR_CAPACITY);
        }
        forgeInstanceField = ForgeModContainer.class.getDeclaredField("INSTANCE");
        forgeInstanceField.setAccessible(true);
        previousForgeMod = (ForgeModContainer) forgeInstanceField.get(null);
        if (previousForgeMod == null) {
            forgeInstanceField.set(null, mock(ForgeModContainer.class));
        }
    }

    @AfterClass
    public static void restoreForge() throws ReflectiveOperationException {
        if (forgeInstanceField != null) {
            forgeInstanceField.set(null, previousForgeMod);
        }
    }

    private static final class MockStructure {
        final World world = mock(World.class);
        final BlockPos center = new BlockPos(0, 64, 0);
        final BlockPos controllerPos = center.north().up();
        final IBlockState controllerState = CropManager.FLUID_TANK.getDefaultState()
                .withProperty(IndustrialTankBlock.FACING, EnumFacing.NORTH);
        final IBlockState casingState = CropManager.FLUID_TANK_CASING.getDefaultState();
        final IndustrialTankTile controller = new IndustrialTankTile();
        final Map<BlockPos, StorageCasingTile> casings = new HashMap<>();
        final Map<BlockPos, IBlockState> overrides = new HashMap<>();
        final Map<BlockPos, TileEntity> externalTiles = new HashMap<>();
        boolean powered = false;

        MockStructure() {
            when(world.getHeight()).thenReturn(256);
            when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
            when(world.isBlockPowered(any(BlockPos.class))).thenAnswer(call -> powered);
            controller.setWorld(world);
            controller.setPos(controllerPos);
            for (int y = 0; y < IndustrialTankTile.FIXED_HEIGHT; y++) {
                boolean cap = y == 0 || y == IndustrialTankTile.FIXED_HEIGHT - 1;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (!cap && Math.abs(dx) == 0 && Math.abs(dz) == 0) continue;
                        BlockPos p = new BlockPos(center.getX() + dx, center.getY() + y, center.getZ() + dz);
                        if (!p.equals(controllerPos)) {
                            StorageCasingTile ct = new StorageCasingTile();
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
                if (pos.equals(controllerPos)) return controller;
                if (casings.containsKey(pos)) return casings.get(pos);
                return externalTiles.get(pos);
            });
        }
    }

    @Test
    public void formsWith41CasingsRejectsInvalidCenterAndDefersUnloaded() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        assertEquals(41, s.casings.size());
        for (StorageCasingTile casing : s.casings.values()) {
            assertSame(s.controller, casing.controller());
        }

        // Unloaded shell block must defer without unforming
        when(s.world.isBlockLoaded(eq(s.center.up(4)), eq(false))).thenReturn(false);
        when(s.world.getTotalWorldTime()).thenReturn(20L);
        s.controller.update();
        assertTrue("Unloaded block must defer", s.controller.formed);

        // Restoring loaded + putting non-air inside core unforms cleanly
        when(s.world.isBlockLoaded(eq(s.center.up(4)), eq(false))).thenReturn(true);
        s.overrides.put(s.center.up(2), Blocks.STONE.getDefaultState());
        s.controller.update();
        assertFalse("Core obstruction must unform", s.controller.formed);

        // Clear core, form again, then break a casing -> unforms
        s.overrides.remove(s.center.up(2));
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        BlockPos corner = new BlockPos(1, 68, 1);
        s.casings.get(corner).notifyControllerBroken();
        assertFalse(s.controller.formed);
        assertNull(s.casings.get(new BlockPos(-1, 64, -1)).controller());
    }

    @Test
    public void rejectsDuplicateControllerInShell() {
        MockStructure s = new MockStructure();
        s.overrides.put(s.controllerPos.up(), s.controllerState);
        s.controller.tryForm();
        assertFalse(s.controller.formed);
    }

    @Test
    public void hasZeroEuDependencyAndSupportsUpTo8CapacityUpgrades() {
        IndustrialTankTile tile = new IndustrialTankTile();
        assertFalse(tile.isSupportMachine());
        assertNull(tile.energySink());
        assertFalse(ic2.api.energy.tile.IEnergySink.class.isAssignableFrom(StorageCasingTile.class));
        assertFalse(ic2.api.energy.tile.IEnergySink.class.isAssignableFrom(IndustrialTankTile.class));

        assertEquals(128000, tile.tank.getCapacity());
        ItemStack upg = new ItemStack(CropManager.RESERVOIR_CAPACITY, 8);
        assertTrue(tile.inventory.isItemValid(IndustrialTankTile.CAPACITY_SLOT, upg));
        tile.inventory.setStackInSlot(IndustrialTankTile.CAPACITY_SLOT, upg);
        assertEquals(384000, tile.tank.getCapacity());

        // Fill near max, then remove upgrades -> fluid above new capacity must be preserved safely
        tile.tank.fill(new FluidStack(FluidRegistry.WATER, 350000), true);
        tile.inventory.setStackInSlot(IndustrialTankTile.CAPACITY_SLOT, ItemStack.EMPTY);
        assertEquals(128000, tile.tank.getCapacity());
        assertEquals(350000, tile.tank.getFluidAmount());
        assertEquals(0, tile.tank.fill(new FluidStack(FluidRegistry.WATER, 100), true));
        assertEquals(1000, tile.tank.drain(1000, true).amount);
        assertEquals(349000, tile.tank.getFluidAmount());
    }

    @Test
    public void filterAndBucketSlotsRespectSingleFluidLocking() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();

        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        player.world = s.world;

        // Invalid filter cursor rejected
        player.inventory.setItemStack(new ItemStack(Items.IRON_INGOT));
        assertFalse(s.controller.control(30, player));
        assertNull(s.controller.filterFluid());

        // Set water filter via cursor
        player.inventory.setItemStack(new ItemStack(Items.WATER_BUCKET));
        assertTrue(s.controller.control(30, player));
        assertNotNull(s.controller.filterFluid());
        assertEquals(FluidRegistry.WATER, s.controller.filterFluid().getFluid());

        // Lava bucket in input slot is rejected when water filter is set
        s.controller.inventory.setStackInSlot(IndustrialTankTile.CONTAINER_IN_SLOT, new ItemStack(Items.LAVA_BUCKET));
        s.controller.processContainers();
        assertEquals(0, s.controller.tank.getFluidAmount());
        assertEquals(Items.LAVA_BUCKET, s.controller.inventory.getStackInSlot(IndustrialTankTile.CONTAINER_IN_SLOT).getItem());

        // Clear filter with empty cursor
        player.inventory.setItemStack(ItemStack.EMPTY);
        assertTrue(s.controller.control(31, player));
        assertNull(s.controller.filterFluid());

        // Water bucket fills tank and returns empty bucket
        s.controller.inventory.setStackInSlot(IndustrialTankTile.CONTAINER_IN_SLOT, new ItemStack(Items.WATER_BUCKET));
        s.controller.processContainers();
        assertEquals(1000, s.controller.tank.getFluidAmount());
        assertTrue(s.controller.inventory.getStackInSlot(IndustrialTankTile.CONTAINER_IN_SLOT).isEmpty());
        assertEquals(Items.BUCKET, s.controller.inventory.getStackInSlot(IndustrialTankTile.CONTAINER_OUT_SLOT).getItem());

        // Empty bucket drains tank and returns water bucket
        s.controller.inventory.setStackInSlot(IndustrialTankTile.CONTAINER_OUT_SLOT, ItemStack.EMPTY);
        s.controller.inventory.setStackInSlot(IndustrialTankTile.CONTAINER_IN_SLOT, new ItemStack(Items.BUCKET));
        s.controller.processContainers();
        assertEquals(0, s.controller.tank.getFluidAmount());
        assertEquals(Items.WATER_BUCKET, s.controller.inventory.getStackInSlot(IndustrialTankTile.CONTAINER_OUT_SLOT).getItem());
    }

    @Test
    public void sixFacesNullCapabilityAndRedstoneValveControlDrainAndAutoEject() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);

        // Default ports: UP=INPUT, DOWN=OUTPUT, horizontals=CLOSED
        BlockPos topPos = new BlockPos(0, 68, 0);
        BlockPos botPos = new BlockPos(0, 64, 0);
        StorageCasingTile topCasing = s.casings.get(topPos);
        StorageCasingTile botCasing = s.casings.get(botPos);

        // Interior face of top casing is NOT exterior
        assertFalse(topCasing.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN));
        assertTrue(topCasing.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP));
        assertTrue(topCasing.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null));

        // Fill through UP input face
        IFluidHandler upHandler = topCasing.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
        assertNotNull(upHandler);
        assertEquals(5000, upHandler.fill(new FluidStack(FluidRegistry.WATER, 5000), true));
        assertNull(upHandler.drain(1000, true));

        // Null-safe property checks on all faces + null
        for (EnumFacing face : EnumFacing.values()) {
            IFluidHandler h = s.controller.getPortCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face);
            assertNotNull(h);
            for (IFluidTankProperties prop : h.getTankProperties()) {
                assertFalse(prop.canFillFluidType(null));
                assertFalse(prop.canDrainFluidType(null));
            }
        }
        IFluidHandler nullFaceHandler = topCasing.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        assertNotNull(nullFaceHandler);
        assertEquals(0, nullFaceHandler.fill(null, true));

        // Set Redstone mode = HIGH (1) via GUI action
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        player.world = s.world;
        assertTrue(s.controller.control(1, player));
        assertEquals(1, s.controller.redstoneMode());

        // Attach destination tank under botPos
        final FluidTank destTank = new FluidTank(10000);
        TileEntity destTile = mock(TileEntity.class);
        when(destTile.isInvalid()).thenReturn(false);
        when(destTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP)).thenReturn(destTank);
        s.externalTiles.put(botPos.down(), destTile);

        IFluidHandler downHandler = botCasing.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN);
        assertNotNull(downHandler);

        // Redstone is LOW -> drain blocked, auto-eject blocked, but UP fill still allowed!
        assertNull(downHandler.drain(1000, true));
        s.controller.autoEjectFluids();
        assertEquals(0, destTank.getFluidAmount());
        assertEquals(500, upHandler.fill(new FluidStack(FluidRegistry.WATER, 500), true));

        // Turn Redstone HIGH -> drain and passive auto-eject work (0 EU)
        s.powered = true;
        assertEquals(500, downHandler.drain(500, true).amount);
        s.controller.autoEjectFluids();
        assertEquals(2000, destTank.getFluidAmount());
        assertEquals(3000, s.controller.tank.getFluidAmount());

        // Internal shell neighbor is never ejected into even if configured OUTPUT
        // Configure BACK (relative NORTH, facing=NORTH -> world SOUTH) to OUTPUT and verify self-casings skipped
        s.controller.control(10 + EnumFacing.NORTH.getIndex(), player); // CLOSED->INPUT
        s.controller.control(10 + EnumFacing.NORTH.getIndex(), player); // INPUT->OUTPUT
        s.controller.autoEjectFluids();
        assertEquals(4000, destTank.getFluidAmount());

        // Client update() is strictly a no-op
        try {
            java.lang.reflect.Field remoteField = World.class.getDeclaredField("isRemote");
            remoteField.setAccessible(true);
            remoteField.setBoolean(s.world, true);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        when(s.world.getTotalWorldTime()).thenReturn(40L);
        s.controller.update();
        assertEquals(4000, destTank.getFluidAmount());
    }

    @Test
    public void comparatorSignalAndNbtClampingRoundTrip() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();
        assertEquals(0, s.controller.comparatorSignal());
        StorageCasingTile someCasing = s.casings.get(new BlockPos(1, 65, 1));
        assertEquals(0, CropManager.FLUID_TANK_CASING.getComparatorInputOverride(
                s.casingState, s.world, new BlockPos(1, 65, 1)));

        s.controller.tank.fill(new FluidStack(FluidRegistry.WATER, 128000), true);
        assertEquals(15, s.controller.comparatorSignal());
        assertEquals(15, CropManager.FLUID_TANK_CASING.getComparatorInputOverride(
                s.casingState, s.world, new BlockPos(1, 65, 1)));
        assertSame(s.controller, someCasing.controller());

        // Corrupt NBT clamping + writeItemData resets formation while preserving fluid/filter/upgrades/settings
        s.controller.inventory.setStackInSlot(IndustrialTankTile.CAPACITY_SLOT,
                new ItemStack(CropManager.RESERVOIR_CAPACITY, 3));
        NBTTagCompound itemTag = s.controller.writeItemData();
        assertFalse(itemTag.hasKey("x"));
        assertFalse(itemTag.getBoolean("TankFormed"));
        itemTag.setInteger("TankRedstone", 99);
        itemTag.setIntArray("TankPorts", new int[] {-5, 8, 2, 1, 0, 99});
        itemTag.getCompoundTag("Inventory").setInteger("Size", 1);

        IndustrialTankTile restored = new IndustrialTankTile();
        restored.readFromNBT(itemTag);
        assertFalse(restored.formed);
        assertEquals(IndustrialTankTile.INVENTORY_SLOTS, restored.inventory.getSlots());
        assertEquals(3, restored.upgradeCount());
        assertEquals(128000 + 3 * 32000, restored.tank.getCapacity());
        assertEquals(128000, restored.tank.getFluidAmount());
        assertEquals(2, restored.redstoneMode());
        assertEquals(0, restored.portMode(0));
        assertEquals(2, restored.portMode(1));
        assertEquals(2, restored.portMode(5));

        // shouldRefresh contract
        IBlockState same = CropManager.FLUID_TANK.getDefaultState();
        IBlockState rotated = same.withProperty(IndustrialTankBlock.FACING, EnumFacing.SOUTH);
        assertFalse(restored.shouldRefresh(s.world, s.controllerPos, same, rotated));
        assertTrue(restored.shouldRefresh(s.world, s.controllerPos, same, Blocks.AIR.getDefaultState()));
        assertFalse(someCasing.shouldRefresh(s.world, someCasing.getPos(), s.casingState,
                s.casingState.withProperty(StorageCasingBlock.FORMED, Boolean.TRUE)));
        assertTrue(someCasing.shouldRefresh(s.world, someCasing.getPos(), s.casingState, Blocks.AIR.getDefaultState()));
    }

    @Test
    public void casingInteractionOpensControllerWhenFormedAndIgnoresWhenUnformed() {
        MockStructure s = new MockStructure();
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        player.world = s.world;
        BlockPos casingPos = new BlockPos(1, 66, 1);

        assertFalse(CropManager.FLUID_TANK_CASING.onBlockActivated(
                s.world, casingPos, s.casingState, player, EnumHand.MAIN_HAND, EnumFacing.EAST, .5F, .5F, .5F));
        verify(player, never()).openGui(any(), eq(16), any(), eq(s.controllerPos.getX()),
                eq(s.controllerPos.getY()), eq(s.controllerPos.getZ()));

        s.controller.tryForm();
        assertTrue(CropManager.FLUID_TANK_CASING.onBlockActivated(
                s.world, casingPos, s.casingState, player, EnumHand.MAIN_HAND, EnumFacing.EAST, .5F, .5F, .5F));
        verify(player).openGui(CropManager.instance, 16, s.world,
                s.controllerPos.getX(), s.controllerPos.getY(), s.controllerPos.getZ());
    }
}
