package br.cropmanager;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AgriculturalMachineBlock extends BlockHorizontal {
    private final java.util.function.Supplier<TileEntity> tileFactory;

    AgriculturalMachineBlock(String id, java.util.function.Supplier<TileEntity> tileFactory) {
        super(Material.IRON);
        this.tileFactory = tileFactory;
        setRegistryName(CropManager.ID, id);
        setTranslationKey("cropmanager." + id);
        setCreativeTab(CropManager.TAB);
        setHardness(3.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return tileFactory.get(); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float x, float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            net.minecraft.entity.player.EntityPlayer player, net.minecraft.util.EnumHand hand, EnumFacing side,
            float x, float y, float z) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof DiagonalSprinklerTile) {
            if (!world.isRemote) player.openGui(CropManager.instance, DiagonalSprinklerTile.GUI, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        if (tile instanceof BiogasSprayerTile) {
            if (!world.isRemote) player.openGui(CropManager.instance, BiogasSprayerTile.GUI, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        if (!(tile instanceof BiogasGeneratorTile)) return false;
        if (!world.isRemote) {
            BiogasGeneratorTile generator = (BiogasGeneratorTile) tile;
            generator.validateShell();
            generator.openGui(player);
        }
        return true;
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof BiogasSprayerTile) ((BiogasSprayerTile) tile).dropFertilizer();
        if (!world.isRemote && tile instanceof BiogasGeneratorTile) ((BiogasGeneratorTile) tile).unformOnBreak();
        super.breakBlock(world, pos, state);
    }

    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }
}
