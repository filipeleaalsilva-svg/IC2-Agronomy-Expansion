package br.cropmanager;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FarmLanternTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Test public void eachVariantMountsInSixDirectionsAndLosesSupportOnlyWhenItsOwnFaceIsRemoved() {
        BlockPos pos = new BlockPos(8, 64, 8);
        for (FarmLanternBlock lamp : CropManager.LANTERNS) {
            assertEquals(15, lamp.getLightValue(lamp.getDefaultState()));
            assertFalse(lamp.getTickRandomly());
            assertFalse(lamp.hasTileEntity(lamp.getDefaultState()));
            for (EnumFacing clickedFace : EnumFacing.values()) {
                World world = mock(World.class);
                EnumFacing direction = clickedFace.getOpposite();
                BlockPos support = pos.offset(direction);
                when(world.isBlockLoaded(support, false)).thenReturn(true);
                when(world.getBlockState(pos)).thenReturn(Blocks.AIR.getDefaultState());
                when(world.getBlockState(support)).thenReturn(Blocks.AIR.getDefaultState());
                assertFalse(lamp.canPlaceBlockOnSide(world, pos, clickedFace));
                when(world.getBlockState(support)).thenReturn(Blocks.STONE.getDefaultState());
                assertTrue(lamp.canPlaceBlockOnSide(world, pos, clickedFace));
                assertTrue(lamp.canPlaceBlockAt(world, pos));
                for (EnumFacing other : EnumFacing.values()) {
                    if (other != clickedFace) assertFalse(lamp.canPlaceBlockOnSide(world, pos, other));
                }
                IBlockState placed = lamp.getStateForPlacement(world, pos, clickedFace, 0, 0, 0, 0, null);
                assertEquals(direction, placed.getValue(FarmLanternBlock.FACING));
                assertEquals(placed, lamp.getStateFromMeta(lamp.getMetaFromState(placed)));

                lamp.neighborChanged(placed, world, pos, Blocks.STONE, pos.offset(clickedFace));
                verify(world, never()).setBlockToAir(pos);
                when(world.getBlockState(support)).thenReturn(Blocks.AIR.getDefaultState());
                lamp.neighborChanged(placed, world, pos, Blocks.AIR, support);
                verify(world).setBlockToAir(pos);
            }
        }
    }

    @Test public void greenhouseGlassAndFrameCanSupportAWallOrCeilingLamp() {
        World world = mock(World.class);
        BlockPos pos = new BlockPos(8, 64, 8);
        when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
        when(world.getBlockState(pos)).thenReturn(Blocks.AIR.getDefaultState());
        when(world.getBlockState(pos.up())).thenReturn(CropManager.GREENHOUSE_GLASS.getDefaultState());
        when(world.getBlockState(pos.north())).thenReturn(CropManager.GREENHOUSE_FRAME.getDefaultState());
        assertTrue(CropManager.GLASS_LAMP.canPlaceBlockOnSide(world, pos, EnumFacing.DOWN));
        assertTrue(CropManager.WOODEN_SCONCE.canPlaceBlockOnSide(world, pos, EnumFacing.SOUTH));
    }
}
