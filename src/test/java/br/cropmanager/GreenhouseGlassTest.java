package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import java.lang.reflect.Field;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

/** Regression gates for Greenhouse Glass and its passive IC2 crop growth effect. */
public final class GreenhouseGlassTest {
    private static final BlockPos GLASS_POS = new BlockPos(12, 64, 12);
    private CropCard previousWeed;
    private CropCard weed;
    private int previousGrowthBonus;

    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
    }

    @Before public void setUp() {
        previousWeed = Crops.weed;
        previousGrowthBonus = ManagerConfig.greenhouseGrowthBonus;
        ManagerConfig.greenhouseGrowthBonus = 20;
        weed = Mockito.mock(CropCard.class);
        Crops.weed = weed;
    }

    @After public void tearDown() {
        Crops.weed = previousWeed;
        ManagerConfig.greenhouseGrowthBonus = previousGrowthBonus;
    }

    @Test public void brightGlassBoostsOnlyTheCropDirectlyBelow() throws Exception {
        World world = mockWorld(false, 12);
        TileEntityCrop crop = crop(1, 40, mockCard(4));
        Mockito.when(world.getTileEntity(GLASS_POS.down())).thenReturn(crop);
        Mockito.when(world.getTileEntity(GLASS_POS.down(2))).thenReturn(crop);

        CropManager.GREENHOUSE_GLASS.randomTick(world, GLASS_POS,
                CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(1L));

        Mockito.verify(crop).setGrowthPoints(60);
        Mockito.verify(world).markChunkDirty(GLASS_POS.down(), crop);
        Mockito.verify(world, Mockito.never()).getTileEntity(GLASS_POS.down(2));
    }

    @Test public void saturatesGrowthPointsWithoutIntegerOverflow() throws Exception {
        World world = mockWorld(false, 12);
        TileEntityCrop crop = crop(1, Integer.MAX_VALUE - 5, mockCard(4));
        Mockito.when(world.getTileEntity(GLASS_POS.down())).thenReturn(crop);

        CropManager.GREENHOUSE_GLASS.randomTick(world, GLASS_POS,
                CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(2L));

        Mockito.verify(crop).setGrowthPoints(Integer.MAX_VALUE);
    }

    @Test public void ignoresClientDimMatureWeedAndMissingCropCards() throws Exception {
        World client = mockWorld(true, 12);
        CropManager.GREENHOUSE_GLASS.randomTick(client, GLASS_POS,
                CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(3L));
        Mockito.verify(client, Mockito.never()).getTileEntity(Mockito.any(BlockPos.class));

        World dim = mockWorld(false, 8);
        TileEntityCrop immature = crop(1, 20, mockCard(4));
        Mockito.when(dim.getTileEntity(GLASS_POS.down())).thenReturn(immature);
        CropManager.GREENHOUSE_GLASS.randomTick(dim, GLASS_POS,
                CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(4L));
        Mockito.verify(immature, Mockito.never()).setGrowthPoints(Mockito.anyInt());

        World world = mockWorld(false, 12);
        TileEntityCrop mature = crop(4, 20, mockCard(4));
        TileEntityCrop weedCrop = crop(1, 20, weed);
        TileEntityCrop noCard = crop(1, 20, null);
        Mockito.when(world.getTileEntity(GLASS_POS.down())).thenReturn(mature, weedCrop, noCard);
        for (int i = 0; i < 3; i++) {
            CropManager.GREENHOUSE_GLASS.randomTick(world, GLASS_POS,
                    CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(i));
        }
        Mockito.verify(mature, Mockito.never()).setGrowthPoints(Mockito.anyInt());
        Mockito.verify(weedCrop, Mockito.never()).setGrowthPoints(Mockito.anyInt());
        Mockito.verify(noCard, Mockito.never()).setGrowthPoints(Mockito.anyInt());
    }

    @Test public void rejectsUnloadedTargetAndZeroConfiguredBonus() throws Exception {
        World world = mockWorld(false, 12);
        Mockito.when(world.isBlockLoaded(GLASS_POS.down(), false)).thenReturn(false);
        CropManager.GREENHOUSE_GLASS.randomTick(world, GLASS_POS,
                CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(5L));
        Mockito.verify(world, Mockito.never()).getTileEntity(GLASS_POS.down());

        ManagerConfig.greenhouseGrowthBonus = 0;
        TileEntityCrop crop = crop(1, 20, mockCard(4));
        Mockito.when(world.isBlockLoaded(GLASS_POS.down(), false)).thenReturn(true);
        Mockito.when(world.getTileEntity(GLASS_POS.down())).thenReturn(crop);
        CropManager.GREENHOUSE_GLASS.randomTick(world, GLASS_POS,
                CropManager.GREENHOUSE_GLASS.getDefaultState(), new Random(6L));
        Mockito.verify(crop, Mockito.never()).setGrowthPoints(Mockito.anyInt());
    }

    @Test public void isCutoutTransparentDropsOneUnitAndHasNoTileEntity() throws Exception {
        GreenhouseGlassBlock block = CropManager.GREENHOUSE_GLASS;
        IBlockState state = block.getDefaultState();
        World world = mockWorld(false, 12);
        assertEquals(BlockRenderLayer.CUTOUT, block.getRenderLayer());
        assertEquals(0, block.getLightOpacity(state, world, GLASS_POS));
        assertFalse(block.isOpaqueCube(state));
        assertFalse(block.isFullCube(state));
        assertEquals(1, block.quantityDropped(new Random(7L)));
        assertFalse(block.hasTileEntity(state));
    }

    @Test public void hidesInternalFacesBetweenAdjacentGreenhousePanes() {
        GreenhouseGlassBlock block = CropManager.GREENHOUSE_GLASS;
        IBlockState state = block.getDefaultState();
        IBlockAccess access = Mockito.mock(IBlockAccess.class);
        Mockito.when(access.getBlockState(GLASS_POS.east())).thenReturn(state);
        assertFalse(block.shouldSideBeRendered(state, access, GLASS_POS, EnumFacing.EAST));
    }

    private static CropCard mockCard(int maxSize) {
        CropCard card = Mockito.mock(CropCard.class);
        Mockito.when(card.getMaxSize()).thenReturn(maxSize);
        return card;
    }

    private static TileEntityCrop crop(int size, int growth, CropCard card) {
        TileEntityCrop crop = Mockito.mock(TileEntityCrop.class);
        Mockito.when(crop.getCurrentSize()).thenReturn(size);
        Mockito.when(crop.getGrowthPoints()).thenReturn(growth);
        Mockito.when(crop.getCrop()).thenReturn(card);
        return crop;
    }

    private static World mockWorld(boolean remote, int light) throws Exception {
        World world = Mockito.mock(World.class);
        Field remoteField = World.class.getDeclaredField("isRemote");
        remoteField.setAccessible(true);
        remoteField.set(world, remote);
        Mockito.when(world.getHeight()).thenReturn(256);
        Mockito.when(world.getLightFromNeighbors(GLASS_POS.up())).thenReturn(light);
        Mockito.when(world.isBlockLoaded(GLASS_POS.down(), false)).thenReturn(true);
        return world;
    }
}
