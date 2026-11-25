package br.cropmanager;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DiagonalSprinklerTest {
    @Test public void everyQuadrantHas81DistinctColumnsAndLeavesTheAxesDry() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> covered = new HashSet<>();
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int i = 0; i < 81; i++) {
                BlockPos column = DiagonalSprinklerTile.column(origin, quadrant, i);
                assertTrue(covered.add(column));
                assertTrue(Math.abs(column.getX()) >= 1 && Math.abs(column.getX()) <= 9);
                assertTrue(Math.abs(column.getZ()) >= 1 && Math.abs(column.getZ()) <= 9);
                assertEquals((quadrant & 1) != 0, column.getX() > 0);
                assertEquals((quadrant & 2) != 0, column.getZ() > 0);
            }
        }
        assertEquals(324, covered.size());
    }

    @Test public void paidQuadrantResumesAfterLoadAndChecksAtMost16ColumnsPerTick() {
        Bootstrap.register();
        if (TileEntity.getKey(DiagonalSprinklerTile.class) == null) {
            TileEntity.register("cropmanager:sprinkler_diagonal", DiagonalSprinklerTile.class);
        }
        BlockPos origin = new BlockPos(0, 64, 0);
        World world = mock(World.class);
        when(world.getHeight()).thenReturn(256);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
        when(world.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.getDefaultState());
        DiagonalSprinklerTile tile = new DiagonalSprinklerTile();
        tile.setWorld(world);
        tile.setPos(origin);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 20), true);
        for (int quadrant : new int[]{0, 2, 3}) assertTrue(tile.control(quadrant, null));
        tile.update();
        assertEquals(10, tile.water.getFluidAmount());
        verify(world, times(48)).getTileEntity(any(BlockPos.class));
        verify(world, never()).getTileEntity(DiagonalSprinklerTile.column(origin, 0, 0));

        DiagonalSprinklerTile restored = new DiagonalSprinklerTile();
        restored.readFromNBT(tile.writeToNBT(new NBTTagCompound()));
        restored.setWorld(world);
        assertTrue(restored.enabled(1));
        assertFalse(restored.enabled(0));
        for (int i = 0; i < 5; i++) restored.update();
        assertEquals(10, restored.water.getFluidAmount());
        verify(world, times(243)).getTileEntity(any(BlockPos.class));
        verify(world).getTileEntity(DiagonalSprinklerTile.column(origin, 1, 80));
    }
}
