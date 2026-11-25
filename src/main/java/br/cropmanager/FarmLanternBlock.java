package br.cropmanager;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tickless decorative Farm Lantern supporting floor, ceiling (hanging), and wall-bracket mounting.
 */
public final class FarmLanternBlock extends Block {
    /** Points toward the supporting surface (DOWN = floor, UP = ceiling, HORIZONTAL = wall). */
    public static final PropertyDirection FACING = BlockDirectional.FACING;

    private static final AxisAlignedBB FLOOR_BOX = new AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.875, 0.75);
    private static final AxisAlignedBB CEILING_BOX = new AxisAlignedBB(0.25, 0.125, 0.25, 0.75, 1.0, 0.75);
    private static final AxisAlignedBB WALL_NORTH = new AxisAlignedBB(0.25, 0.0625, 0.0, 0.75, 0.9375, 0.75);
    private static final AxisAlignedBB WALL_SOUTH = new AxisAlignedBB(0.25, 0.0625, 0.25, 0.75, 0.9375, 1.0);
    private static final AxisAlignedBB WALL_WEST = new AxisAlignedBB(0.0, 0.0625, 0.25, 0.75, 0.9375, 0.75);
    private static final AxisAlignedBB WALL_EAST = new AxisAlignedBB(0.25, 0.0625, 0.25, 1.0, 0.9375, 0.75);

    public FarmLanternBlock(String name, Material material, SoundType sound) {
        super(material);
        setRegistryName(CropManager.ID, name);
        setTranslationKey("cropmanager." + name);
        setCreativeTab(CropManager.TAB);
        setHardness(1.5F);
        setResistance(5.0F);
        setSoundType(sound);
        setLightLevel(1.0F);
        this.fullBlock = false;
        this.lightOpacity = 0;
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing clickedFace,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        // Support direction is opposite the clicked face (e.g. clicking north side of a wall -> support is south).
        return getDefaultState().withProperty(FACING, clickedFace.getOpposite());
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (!super.canPlaceBlockAt(world, pos)) return false;
        for (EnumFacing direction : EnumFacing.values()) {
            if (hasSupport(world, pos, direction)) return true;
        }
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return super.canPlaceBlockAt(world, pos) && hasSupport(world, pos, side.getOpposite());
    }

    private static boolean hasSupport(World world, BlockPos pos, EnumFacing direction) {
        BlockPos support = pos.offset(direction);
        if (!world.isBlockLoaded(support, false)) return false;
        IBlockState state = world.getBlockState(support);
        Block block = state.getBlock();
        return block == CropManager.GREENHOUSE_GLASS || block == CropManager.GREENHOUSE_FRAME
                || state.isSideSolid(world, support, direction.getOpposite());
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block changed, BlockPos from) {
        EnumFacing direction = state.getValue(FACING);
        if (!world.isRemote && world.isBlockLoaded(pos.offset(direction), false)
                && !hasSupport(world, pos, direction) && world.setBlockToAir(pos)) {
            dropBlockAsItem(world, pos, state, 0);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case UP: return CEILING_BOX;
            case NORTH: return WALL_NORTH;
            case SOUTH: return WALL_SOUTH;
            case WEST: return WALL_WEST;
            case EAST: return WALL_EAST;
            default: return FLOOR_BOX;
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager." + getRegistryName().getPath() + "_help"));
    }
}
