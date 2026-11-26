package br.cropmanager;

import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
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

    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof BiogasGeneratorTile) ((BiogasGeneratorTile) tile).validateShell();
        }
    }

    @Override public boolean hasComparatorInputOverride(IBlockState state) {
        return this == CropManager.BIOGAS_GENERATOR;
    }

    @Override public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        if (this != CropManager.BIOGAS_GENERATOR) return 0;
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof BiogasGeneratorTile ? ((BiogasGeneratorTile) tile).comparatorSignal() : 0;
    }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess access, BlockPos pos,
            IBlockState state, int fortune) {
        TileEntity tile = access.getTileEntity(pos);
        if (tile instanceof BiogasGeneratorTile) {
            ItemStack stack = new ItemStack(this);
            stack.setTagInfo("BlockEntityTag", ((BiogasGeneratorTile) tile).writeItemData());
            drops.add(stack);
            return;
        }
        super.getDrops(drops, access, pos, state, fortune);
    }

    @Override public boolean removedByPlayer(IBlockState state, World world, BlockPos pos,
            EntityPlayer player, boolean willHarvest) {
        if (this == CropManager.BIOGAS_GENERATOR) {
            return willHarvest || super.removedByPlayer(state, world, pos, player, false);
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override protected boolean canSilkHarvest() {
        return this != CropManager.BIOGAS_GENERATOR && super.canSilkHarvest();
    }

    @Override public void harvestBlock(World world, EntityPlayer player, BlockPos pos,
            IBlockState state, @Nullable TileEntity tile, ItemStack tool) {
        try {
            super.harvestBlock(world, player, pos, state, tile, tool);
        } finally {
            if (this == CropManager.BIOGAS_GENERATOR
                    && world.getBlockState(pos).getBlock() == this && world.getTileEntity(pos) == tile) {
                world.setBlockToAir(pos);
            }
        }
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
