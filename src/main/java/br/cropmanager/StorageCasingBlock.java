package br.cropmanager;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Structural shell block for EU-free 3x5x3 storage multiblocks (fluid tank and agricultural silo). */
public final class StorageCasingBlock extends Block {
    public static final PropertyBool FORMED = PropertyBool.create("formed");

    public StorageCasingBlock(String id) {
        super(Material.IRON);
        setRegistryName(CropManager.ID, id);
        setTranslationKey(CropManager.ID + "." + id);
        setCreativeTab(CropManager.TAB);
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FORMED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FORMED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        boolean formed = tile instanceof StorageCasingTile && ((StorageCasingTile) tile).isFormed();
        return state.withProperty(FORMED, Boolean.valueOf(formed));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new StorageCasingTile();
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof StorageCasingTile ? ((StorageCasingTile) tile).comparatorSignal() : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof StorageCasingTile)) return false;
        StorageMultiblock controller = ((StorageCasingTile) tile).controller();
        if (controller == null || !controller.isFormed()) return false;
        return world.isRemote || controller.openFromCasing(player);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof StorageCasingTile) ((StorageCasingTile) tile).notifyControllerBroken();
        }
        super.breakBlock(world, pos, state);
    }
}
