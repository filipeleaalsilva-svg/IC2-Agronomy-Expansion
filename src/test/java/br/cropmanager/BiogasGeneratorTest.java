package br.cropmanager;

import ic2.core.ref.FluidName;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BiogasGeneratorTest {
    private static Fluid testBiogas;

    @BeforeClass
    public static void boot() {
        Bootstrap.register();
        if (TileEntity.getKey(BiogasGeneratorTile.class) == null) {
            TileEntity.register("cropmanager:biogas_generator", BiogasGeneratorTile.class);
        }
        if (TileEntity.getKey(StorageCasingTile.class) == null) {
            TileEntity.register("cropmanager:storage_casing", StorageCasingTile.class);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.BIOGAS_GENERATOR.getRegistryName())) {
            ForgeRegistries.ITEMS.register(new ItemBlock(CropManager.BIOGAS_GENERATOR)
                    .setRegistryName(CropManager.BIOGAS_GENERATOR.getRegistryName()));
        }
        testBiogas = FluidRegistry.getFluid("cropmanager_test_generator_gas_v2");
        if (testBiogas == null) {
            testBiogas = new Fluid("cropmanager_test_generator_gas_v2",
                    new ResourceLocation("minecraft:blocks/water_still"),
                    new ResourceLocation("minecraft:blocks/water_flow"));
            FluidRegistry.registerFluid(testBiogas);
        }
    }

    private static Object installTestBiogas() throws ReflectiveOperationException {
        Field instance = FluidName.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object old = instance.get(FluidName.biogas);
        instance.set(FluidName.biogas, testBiogas);
        return old;
    }

    private static void restoreBiogas(Object old) throws ReflectiveOperationException {
        Field instance = FluidName.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(FluidName.biogas, old);
    }

    private static final class MockGeneratorStructure {
        final World world = mock(World.class);
        final BlockPos center;
        final BlockPos controllerPos;
        final IBlockState controllerState = CropManager.BIOGAS_GENERATOR.getDefaultState()
                .withProperty(AgriculturalMachineBlock.FACING, EnumFacing.NORTH);
        final IBlockState casingState = CropManager.BIOGAS_GENERATOR_CASING.getDefaultState();
        final BiogasGeneratorTile controller = new BiogasGeneratorTile();
        final Map<BlockPos, StorageCasingTile> casings = new HashMap<>();
        final Map<BlockPos, IBlockState> overrides = new HashMap<>();

        MockGeneratorStructure(BlockPos center) throws ReflectiveOperationException {
            this.center = center;
            this.controllerPos = center.north();
            Field randField = World.class.getDeclaredField("rand");
            randField.setAccessible(true);
            randField.set(world, new Random(0L));
            Field providerField = World.class.getDeclaredField("provider");
            providerField.setAccessible(true);
            providerField.set(world, new WorldProviderSurface());
            GameRules gameRules = new GameRules();
            MinecraftServer server = mock(MinecraftServer.class);
            when(world.getHeight()).thenReturn(256);
            when(world.getGameRules()).thenReturn(gameRules);
            when(world.getMinecraftServer()).thenReturn(server);
            when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
            controller.setWorld(world);
            controller.setPos(controllerPos);
            for (int y = 0; y < BiogasGeneratorTile.HEIGHT; y++) {
                int wy = controllerPos.getY() - 1 + y;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (y == 1 && dx == 0 && dz == 0) continue;
                        BlockPos p = new BlockPos(center.getX() + dx, wy, center.getZ() + dz);
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
                BlockPos at = call.getArgument(0);
                if (overrides.containsKey(at)) return overrides.get(at);
                if (at.equals(controllerPos)) return controllerState;
                if (at.equals(center)) return Blocks.AIR.getDefaultState();
                if (casings.containsKey(at)) return casingState;
                return Blocks.AIR.getDefaultState();
            });
            when(world.getTileEntity(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos at = call.getArgument(0);
                if (overrides.containsKey(at) && overrides.get(at).getBlock() == Blocks.AIR) return null;
                if (at.equals(controllerPos)) return controller;
                return casings.get(at);
            });
            when(world.setBlockToAir(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos at = call.getArgument(0);
                IBlockState current = world.getBlockState(at);
                current.getBlock().breakBlock(world, at, current);
                overrides.put(at, Blocks.AIR.getDefaultState());
                return true;
            });
        }
    }
    @Test public void formedGeneratorBurnsOneUnitAndStopsOnBreakOrFullBuffer() throws Exception {
        Bootstrap.register();
        if (TileEntity.getKey(BiogasGeneratorTile.class) == null) {
            TileEntity.register("cropmanager:biogas_generator", BiogasGeneratorTile.class);
        }
        Fluid biogas = new Fluid("cropmanager_test_generator_gas",
                new ResourceLocation("minecraft:blocks/water_still"), new ResourceLocation("minecraft:blocks/water_flow"));
        FluidRegistry.registerFluid(biogas);
        Field instance = FluidName.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object old = instance.get(FluidName.biogas);
        instance.set(FluidName.biogas, biogas);
        try {
            World world = mock(World.class);
            BlockPos center = new BlockPos(0, 64, 0), pos = center.north();
            BlockPos cap = center.up();
            BiogasGeneratorTile tile = new BiogasGeneratorTile();
            tile.setWorld(world);
            tile.setPos(pos);
            when(world.getHeight()).thenReturn(256);
            when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
            when(world.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos at = call.getArgument(0);
                if (at.equals(pos)) return CropManager.BIOGAS_GENERATOR.getDefaultState();
                if (at.equals(center)) return Blocks.AIR.getDefaultState();
                return CropManager.BIOGAS_GENERATOR_CASING.getDefaultState();
            });
            assertEquals(0, tile.gas.fill(new FluidStack(FluidRegistry.WATER, 2), true));
            tile.gas.fill(new FluidStack(biogas, 40), true);
            assertTrue(tile.emitsEnergyTo(null, EnumFacing.NORTH));
            assertFalse(tile.emitsEnergyTo(null, EnumFacing.SOUTH));
            tile.update();
            assertTrue(tile.isFormed());
            assertEquals(39, tile.gas.getFluidAmount());
            assertEquals(32, tile.getOfferedEnergy(), 0);
            for (int i = 0; i < 40; i++) tile.update();
            assertEquals(8, tile.gas.getFluidAmount());
            tile.casingBroken(cap);
            when(world.getBlockState(cap)).thenReturn(Blocks.AIR.getDefaultState());
            tile.update();
            assertFalse(tile.isFormed());
            assertEquals(0, tile.getOfferedEnergy(), 0);
            assertEquals(8, tile.gas.getFluidAmount());
            BiogasGeneratorTile restored = new BiogasGeneratorTile();
            restored.readFromNBT(tile.writeToNBT(new NBTTagCompound()));
            restored.setWorld(world);
            restored.setPos(pos);
            assertEquals(8, restored.gas.getFluidAmount());
            assertEquals(0, restored.getOfferedEnergy(), 0);
            when(world.getBlockState(cap)).thenReturn(CropManager.BIOGAS_GENERATOR_CASING.getDefaultState());
            restored.update();
            assertEquals(32, restored.getOfferedEnergy(), 0);
            assertEquals(8, restored.gas.getFluidAmount());
            restored.drawEnergy(32);
            restored.update();
            assertEquals(7, restored.gas.getFluidAmount());
        } finally {
            instance.set(FluidName.biogas, old);
        }
    }

    @Test public void breakCasingOrControllerUnlinksAllCasingsAndUnloadedDefers() {
        Bootstrap.register();
        if (TileEntity.getKey(BiogasGeneratorTile.class) == null) {
            TileEntity.register("cropmanager:biogas_generator", BiogasGeneratorTile.class);
        }
        if (TileEntity.getKey(StorageCasingTile.class) == null) {
            TileEntity.register("cropmanager:storage_casing", StorageCasingTile.class);
        }
        World world = mock(World.class);
        BlockPos center = new BlockPos(0, 64, 0), pos = center.north();
        BlockPos cap = center.up();
        BiogasGeneratorTile tile = new BiogasGeneratorTile();
        tile.setWorld(world);
        tile.setPos(pos);

        java.util.Map<BlockPos, StorageCasingTile> casings = new java.util.HashMap<>();
        for (int y = 0; y < BiogasGeneratorTile.HEIGHT; y++) {
            int wy = pos.getY() - 1 + y;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (y == 1 && dx == 0 && dz == 0) continue;
                    BlockPos p = new BlockPos(center.getX() + dx, wy, center.getZ() + dz);
                    if (!p.equals(pos)) {
                        StorageCasingTile ct = new StorageCasingTile();
                        ct.setWorld(world);
                        ct.setPos(p);
                        casings.put(p, ct);
                    }
                }
            }
        }
        java.util.Map<BlockPos, net.minecraft.block.state.IBlockState> overrides = new java.util.HashMap<>();
        when(world.getHeight()).thenReturn(256);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
        when(world.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
            BlockPos at = call.getArgument(0);
            if (overrides.containsKey(at)) return overrides.get(at);
            if (at.equals(pos)) return CropManager.BIOGAS_GENERATOR.getDefaultState();
            if (at.equals(center)) return Blocks.AIR.getDefaultState();
            return CropManager.BIOGAS_GENERATOR_CASING.getDefaultState();
        });
        when(world.getTileEntity(any(BlockPos.class))).thenAnswer(call -> {
            BlockPos at = call.getArgument(0);
            if (at.equals(pos)) return tile;
            return casings.get(at);
        });

        tile.validateShell();
        assertTrue(tile.isFormed());
        assertEquals(25, casings.size());
        for (StorageCasingTile ct : casings.values()) {
            assertSame(tile, ct.controller());
        }

        // Unloaded shell block must defer without unforming
        when(world.isBlockLoaded(eq(cap), eq(false))).thenReturn(false);
        tile.validateShell();
        assertTrue("Unloaded shell block must defer", tile.isFormed());
        when(world.isBlockLoaded(eq(cap), eq(false))).thenReturn(true);

        // Breaking one casing immediately unlinks all surviving casings
        casings.get(cap).notifyControllerBroken();
        assertFalse(tile.isFormed());
        for (StorageCasingTile ct : casings.values()) {
            assertNull(ct.controller());
        }

        // Re-form, then break controller block -> unlinks all casings
        tile.validateShell();
        assertTrue(tile.isFormed());
        CropManager.BIOGAS_GENERATOR.breakBlock(world, pos, CropManager.BIOGAS_GENERATOR.getDefaultState());
        assertFalse(tile.isFormed());
        for (StorageCasingTile ct : casings.values()) {
            assertNull(ct.controller());
        }

        // Replaced controller block must not throw on emitsEnergyTo
        overrides.put(pos, Blocks.AIR.getDefaultState());
        assertFalse(tile.emitsEnergyTo(null, EnumFacing.NORTH));
    }

    @Test
    public void formsOnlyWithValid3x3x3ShellRejectsObstructionAndDuplicateControllerAndDefersUnloaded() throws Exception {
        MockGeneratorStructure s = new MockGeneratorStructure(new BlockPos(0, 64, 0));
        EntityPlayer player = mock(EntityPlayer.class);

        CropManager.BIOGAS_GENERATOR.onBlockPlacedBy(s.world, s.controllerPos, s.controllerState, player, ItemStack.EMPTY);
        assertTrue(s.controller.isFormed());
        assertEquals(25, s.casings.size());
        for (StorageCasingTile ct : s.casings.values()) {
            assertSame(s.controller, ct.controller());
        }

        // Core obstruction unforms and unlinks all 25 casings
        s.overrides.put(s.center, Blocks.STONE.getDefaultState());
        s.controller.validateShell();
        assertFalse(s.controller.isFormed());
        for (StorageCasingTile ct : s.casings.values()) {
            assertNull(ct.controller());
        }

        // Duplicate controller in shell rejects formation
        s.overrides.remove(s.center);
        s.overrides.put(s.controllerPos.up(), s.controllerState);
        s.controller.validateShell();
        assertFalse(s.controller.isFormed());

        // Restore casing, form, then defer on unloaded shell block
        s.overrides.remove(s.controllerPos.up());
        s.controller.validateShell();
        assertTrue(s.controller.isFormed());
        BlockPos cap = s.center.up();
        when(s.world.isBlockLoaded(eq(cap), eq(false))).thenReturn(false);
        s.controller.validateShell();
        assertTrue("Unloaded shell block must defer while formed", s.controller.isFormed());

        // Unformed generator must also defer without forming when a shell block is unloaded
        MockGeneratorStructure unloaded = new MockGeneratorStructure(new BlockPos(32, 64, 32));
        when(unloaded.world.isBlockLoaded(eq(unloaded.center.up()), eq(false))).thenReturn(false);
        unloaded.controller.validateShell();
        assertFalse("Unloaded shell block must defer without forming", unloaded.controller.isFormed());
    }

    @Test
    public void cleanBreakAndReplaceRoundTripPreservesGasAndStoredEuWithoutStaleCoords() throws Exception {
        Object old = installTestBiogas();
        try {
            MockGeneratorStructure s = new MockGeneratorStructure(new BlockPos(0, 64, 0));
            EntityPlayer player = mock(EntityPlayer.class);
            CropManager.BIOGAS_GENERATOR.onBlockPlacedBy(s.world, s.controllerPos, s.controllerState, player, ItemStack.EMPTY);
            assertTrue(s.controller.isFormed());
            assertEquals(8000, s.controller.gas.fill(new FluidStack(testBiogas, 8000), true));
            for (int i = 0; i < 17; i++) {
                s.controller.update();
            }
            assertEquals(7983, s.controller.gas.getFluidAmount());
            assertEquals(544.0, s.controller.writeToNBT(new NBTTagCompound()).getDouble("StoredEU"), 1e-6);

            assertTrue(CropManager.BIOGAS_GENERATOR.removedByPlayer(s.controllerState, s.world, s.controllerPos, player, true));
            assertSame(s.controller, s.world.getTileEntity(s.controllerPos));

            CropManager.BIOGAS_GENERATOR.harvestBlock(
                    s.world, player, s.controllerPos, s.controllerState, s.controller, new ItemStack(Items.IRON_PICKAXE));
            ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
            verify(s.world).spawnEntity(captor.capture());
            assertTrue(captor.getValue() instanceof EntityItem);
            ItemStack drop = ((EntityItem) captor.getValue()).getItem();
            assertEquals(Item.getItemFromBlock(CropManager.BIOGAS_GENERATOR), drop.getItem());
            NBTTagCompound tag = drop.getSubCompound("BlockEntityTag");
            assertNotNull(tag);
            assertFalse(tag.hasKey("x"));
            assertFalse(tag.hasKey("y"));
            assertFalse(tag.hasKey("z"));
            assertFalse(tag.hasKey("Formed"));
            assertFalse(tag.hasKey("CenterX"));
            assertFalse(tag.hasKey("CenterY"));
            assertFalse(tag.hasKey("CenterZ"));

            assertEquals(Blocks.AIR, s.world.getBlockState(s.controllerPos).getBlock());
            assertFalse(s.controller.isFormed());
            for (StorageCasingTile ct : s.casings.values()) {
                assertNull(ct.controller());
            }

            // Re-place at a different position (16, 70, 16)
            BlockPos newPos = new BlockPos(16, 70, 16);
            MockGeneratorStructure s2 = new MockGeneratorStructure(newPos.south());
            assertTrue(ItemBlock.setTileEntityNBT(s2.world, player, s2.controllerPos, drop));
            CropManager.BIOGAS_GENERATOR.onBlockPlacedBy(s2.world, s2.controllerPos, s2.controllerState, player, drop);

            assertTrue(s2.controller.isFormed());
            assertEquals(newPos, s2.controller.getPos());
            assertEquals(7983, s2.controller.gas.getFluidAmount());
            assertEquals(544.0, s2.controller.writeToNBT(new NBTTagCompound()).getDouble("StoredEU"), 1e-6);
            assertEquals(32.0, s2.controller.getOfferedEnergy(), 1e-6);
            for (StorageCasingTile ct : s2.casings.values()) {
                assertSame(s2.controller, ct.controller());
            }

            // shouldRefresh contract
            IBlockState rotated = s2.controllerState.withProperty(AgriculturalMachineBlock.FACING, EnumFacing.SOUTH);
            assertFalse(s2.controller.shouldRefresh(s2.world, s2.controllerPos, s2.controllerState, rotated));
            assertTrue(s2.controller.shouldRefresh(s2.world, s2.controllerPos, s2.controllerState, Blocks.AIR.getDefaultState()));

            // AgriculturalMachineBlock sibling safety
            for (AgriculturalMachineBlock sibling : new AgriculturalMachineBlock[] {
                    CropManager.SPRINKLER_DIAGONAL, CropManager.BIOGAS_SPRAYER, CropManager.BIOGAS_HEAT_BURNER
            }) {
                NonNullList<ItemStack> siblingDrops = NonNullList.create();
                World siblingWorld = mock(World.class);
                TileEntity siblingTile = sibling.createTileEntity(siblingWorld, sibling.getDefaultState());
                when(siblingWorld.getTileEntity(BlockPos.ORIGIN)).thenReturn(siblingTile);
                assertFalse(sibling.hasComparatorInputOverride(sibling.getDefaultState()));
                assertEquals(0, sibling.getComparatorInputOverride(sibling.getDefaultState(), siblingWorld, BlockPos.ORIGIN));
                sibling.getDrops(siblingDrops, siblingWorld, BlockPos.ORIGIN, sibling.getDefaultState(), 0);
                for (ItemStack dropped : siblingDrops) {
                    assertNull(dropped.getSubCompound("BlockEntityTag"));
                }
            }
        } finally {
            restoreBiogas(old);
        }
    }

    @Test
    public void breakingCasingUnformsAndResetsComparatorsWhilePreservingControllerContents() throws Exception {
        Object old = installTestBiogas();
        try {
            MockGeneratorStructure s = new MockGeneratorStructure(new BlockPos(0, 64, 0));
            s.controller.validateShell();
            assertTrue(s.controller.isFormed());
            assertEquals(8008, s.controller.gas.fill(new FluidStack(testBiogas, 8008), true));
            for (int i = 0; i < 8; i++) {
                s.controller.update();
            }
            assertEquals(8000, s.controller.gas.getFluidAmount());
            assertEquals(256.0, s.controller.writeToNBT(new NBTTagCompound()).getDouble("StoredEU"), 1e-6);

            BlockPos cap = s.center.up();
            BlockPos sideCasing = s.center.east();
            assertEquals(8, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(8, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));

            clearInvocations(s.world);
            CropManager.BIOGAS_GENERATOR_CASING.breakBlock(s.world, cap, s.casingState);

            assertFalse(s.controller.isFormed());
            for (StorageCasingTile ct : s.casings.values()) {
                assertNull(ct.controller());
            }
            assertEquals(8000, s.controller.gas.getFluidAmount());
            assertEquals(256.0, s.controller.writeToNBT(new NBTTagCompound()).getDouble("StoredEU"), 1e-6);
            assertEquals(0, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(0, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));
            verify(s.world, atLeastOnce()).updateComparatorOutputLevel(s.controllerPos, CropManager.BIOGAS_GENERATOR);
            verify(s.world, atLeastOnce()).updateComparatorOutputLevel(sideCasing, CropManager.BIOGAS_GENERATOR_CASING);
        } finally {
            restoreBiogas(old);
        }
    }

    @Test
    public void comparatorSignalTracksFormedAndGasLevelOnControllerAndCasingsWithoutRedundantUpdates() throws Exception {
        Object old = installTestBiogas();
        try {
            MockGeneratorStructure s = new MockGeneratorStructure(new BlockPos(0, 64, 0));
            BlockPos cap = s.center.up();
            BlockPos sideCasing = s.center.east();
            s.overrides.put(cap, Blocks.AIR.getDefaultState());
            s.controller.validateShell();
            assertFalse(s.controller.isFormed());

            assertEquals(8000, s.controller.gas.fill(new FluidStack(testBiogas, 8000), true));
            assertEquals(0, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(0, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));

            // Empty formed -> 0
            s.controller.gas.drainInternal(8000, true);
            s.overrides.remove(cap);
            s.controller.validateShell();
            assertTrue(s.controller.isFormed());
            assertEquals(0, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(0, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));

            // 1 mB -> 1, triggers controller + casing notifications
            clearInvocations(s.world);
            s.controller.gas.fill(new FluidStack(testBiogas, 1), true);
            assertEquals(1, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(1, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));
            verify(s.world, atLeastOnce()).updateComparatorOutputLevel(s.controllerPos, CropManager.BIOGAS_GENERATOR);
            verify(s.world, atLeastOnce()).updateComparatorOutputLevel(sideCasing, CropManager.BIOGAS_GENERATOR_CASING);

            // 8000 mB -> 8
            s.controller.gas.fill(new FluidStack(testBiogas, 7999), true);
            assertEquals(8, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(8, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));

            // 16000 mB -> 15
            s.controller.gas.fill(new FluidStack(testBiogas, 8000), true);
            assertEquals(15, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(15, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));

            // 16000 -> 15999 drops signal 15 -> 14 and notifies controller + casings
            s.controller.drawEnergy(1024);
            clearInvocations(s.world);
            s.controller.update();
            assertEquals(15999, s.controller.gas.getFluidAmount());
            assertEquals(14, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            assertEquals(14, CropManager.BIOGAS_GENERATOR_CASING.getComparatorInputOverride(s.casingState, s.world, sideCasing));
            verify(s.world, atLeastOnce()).updateComparatorOutputLevel(s.controllerPos, CropManager.BIOGAS_GENERATOR);
            verify(s.world, atLeastOnce()).updateComparatorOutputLevel(sideCasing, CropManager.BIOGAS_GENERATOR_CASING);

            // 15999 -> 15998 keeps signal at 14 with zero redundant comparator updates
            clearInvocations(s.world);
            s.controller.update();
            assertEquals(15998, s.controller.gas.getFluidAmount());
            assertEquals(14, CropManager.BIOGAS_GENERATOR.getComparatorInputOverride(s.controllerState, s.world, s.controllerPos));
            verify(s.world, never()).updateComparatorOutputLevel(any(BlockPos.class), any());
        } finally {
            restoreBiogas(old);
        }
    }
}
