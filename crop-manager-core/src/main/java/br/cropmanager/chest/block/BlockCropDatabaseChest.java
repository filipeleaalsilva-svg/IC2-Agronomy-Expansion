package br.cropmanager.chest.block;

import br.cropmanager.CropManager;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.EnumBlockRenderType;

public final class BlockCropDatabaseChest extends BlockHorizontal {
    public BlockCropDatabaseChest() {
        super(Material.IRON);
        setRegistryName(CropManager.ID, "crop_database_chest");
        setTranslationKey(CropManager.ID + ".crop_database_chest");
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(3.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int value) {
        TileEntity tile = world.getTileEntity(pos);
        return tile != null && tile.receiveClientEvent(id, value);
    }
    @Override public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCropDatabaseChest();
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) { return state.withProperty(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) { return withRotation(state, mirror.toRotation(state.getValue(FACING))); }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float x,
                                           float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!world.isRemote) {
            player.openGui(CropManager.instance, 11, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileCropDatabaseChest) {
            InventoryHelper.dropInventoryItems(world, pos, (TileCropDatabaseChest) tile);
            ((TileCropDatabaseChest) tile).clear();
        }
        super.breakBlock(world, pos, state);
    }
}
