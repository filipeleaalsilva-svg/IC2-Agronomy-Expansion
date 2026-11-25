package br.cropmanager;

import ic2.core.ref.FluidName;
import java.lang.reflect.Field;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BiogasGeneratorTest {
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
}
