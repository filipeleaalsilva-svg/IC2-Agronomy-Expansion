package br.cropmanager;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ManagerBlock extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyEnum<MachineHalf> HALF = PropertyEnum.create("half", MachineHalf.class);
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");
    public enum MachineHalf implements IStringSerializable { LOWER, UPPER; @Override public String getName() { return name().toLowerCase(java.util.Locale.ROOT); } }

    private final int kind;
    public ManagerBlock(String name, int kind) {
        super(Material.IRON); this.kind = kind;
        setRegistryName(CropManager.ID, name);
        setTranslationKey("cropmanager." + name);
        if (kind == 1 || kind == 3 || kind >= 4 && kind != 5) setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        IBlockState state = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ACTIVE, false)
                .withProperty(HALF, MachineHalf.LOWER)
                .withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
                .withProperty(WEST, false).withProperty(UP, false).withProperty(DOWN, false);
        setDefaultState(state);
    }

    private boolean isTwoHigh() { return false; }
    private boolean isUpper(IBlockState state) { return isTwoHigh() && state.getValue(HALF) == MachineHalf.UPPER; }
    private BlockPos basePos(BlockPos pos, IBlockState state) { return isUpper(state) ? pos.down() : pos; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, ACTIVE, HALF, NORTH, EAST, SOUTH, WEST, UP, DOWN); }
    @Override public IBlockState getActualState(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        if (kind != 12) return state;
        return state
                .withProperty(NORTH, canConnectPipe(world, pos, EnumFacing.NORTH))
                .withProperty(EAST, canConnectPipe(world, pos, EnumFacing.EAST))
                .withProperty(SOUTH, canConnectPipe(world, pos, EnumFacing.SOUTH))
                .withProperty(WEST, canConnectPipe(world, pos, EnumFacing.WEST))
                .withProperty(UP, canConnectPipe(world, pos, EnumFacing.UP))
                .withProperty(DOWN, canConnectPipe(world, pos, EnumFacing.DOWN));
    }
    private static boolean canConnectPipe(net.minecraft.world.IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity neighbor = world.getTileEntity(pos.offset(side));
        return neighbor instanceof ThermalPipeTile || neighbor instanceof GrinExtractorTile || neighbor instanceof ic2.api.energy.tile.IHeatSource;
    }
    @Override public boolean hasTileEntity(IBlockState state) { return !isUpper(state); }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { if (isUpper(state)) return null; return kind == 1 ? new AdvancedManagerTile() : kind == 3 ? new MonitorTile() : kind == 4 ? new ComposterTile() : kind == 8 ? new WeederTile() : kind == 9 ? new WeedExFabricatorTile() : kind == 10 ? new GrinGrinderTile() : kind == 11 ? new GrinExtractorTile() : kind == 12 ? new ThermalPipeTile() : kind == 13 ? new SeedAnalyzerTile() : new ManagerTile(); }
    @Override public IBlockState getStateFromMeta(int meta) {
        IBlockState state = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(ACTIVE, (meta & 4) != 0);
        return isTwoHigh() ? state.withProperty(HALF, (meta & 8) == 0 ? MachineHalf.LOWER : MachineHalf.UPPER) : state;
    }
    @Override public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(ACTIVE) ? 4 : 0) | (isUpper(state) ? 8 : 0);
    }
    @Override public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (!super.canPlaceBlockAt(world, pos)) return false;
        return !isTwoHigh() || hasRoomForUpper(world, pos);
    }
    private boolean hasRoomForUpper(World world, BlockPos pos) { BlockPos top = pos.up(); return world.isBlockLoaded(top) && world.getBlockState(top).getBlock().isReplaceable(world, top); }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float x, float y, float z, int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState state = getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        return isTwoHigh() ? state.withProperty(HALF, MachineHalf.LOWER) : state;
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, net.minecraft.item.ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote && isTwoHigh() && state.getValue(HALF) == MachineHalf.LOWER && hasRoomForUpper(world, pos))
            world.setBlockState(pos.up(), state.withProperty(HALF, MachineHalf.UPPER), 3);
    }
    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (kind >= 10 && kind <= 13 && isWrench(player.getHeldItem(hand))) {
            if (world.isRemote) return true;
            if (kind == 12 && !player.isSneaking()) return true;
            if (player.isSneaking() && !player.canPlayerEdit(pos, side, player.getHeldItem(hand))) return false;
            if (isElectricWrench(player.getHeldItem(hand)) && !ic2.api.item.ElectricItem.manager.use(player.getHeldItem(hand), 100, player)) return true;
            BlockPos base = basePos(pos, state);
            IBlockState baseState = world.getBlockState(base);
            if (player.isSneaking()) {
                world.setBlockToAir(base);
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world, base.getX(), base.getY(), base.getZ(), new net.minecraft.item.ItemStack(this));
            } else {
                EnumFacing facing = side.getAxis().isHorizontal() ? baseState.getValue(FACING).rotateY() : player.getHorizontalFacing().getOpposite();
                world.setBlockState(base, baseState.withProperty(FACING, facing), 3);
                if (isTwoHigh()) {
                    BlockPos upper = base.up(); IBlockState upperState = world.getBlockState(upper);
                    if (upperState.getBlock() == this && isUpper(upperState)) world.setBlockState(upper, upperState.withProperty(FACING, facing), 3);
                }
                world.notifyBlockUpdate(base, baseState, world.getBlockState(base), 3);
            }
            if (!isElectricWrench(player.getHeldItem(hand))) player.getHeldItem(hand).damageItem(1, player);
            return true;
        }
        if (kind == 12) return true;
        BlockPos base = basePos(pos, state);
        if (!world.isRemote) player.openGui(CropManager.instance, kind == 2 ? 1 : kind == 3 ? 2 : kind == 4 ? 3 : kind == 5 ? 4 : kind == 6 ? 5 : kind == 7 ? 6 : kind == 8 ? 7 : kind == 9 ? 8 : kind == 10 ? 9 : kind == 11 ? 10 : kind == 13 ? 14 : 0, world, base.getX(), base.getY(), base.getZ());
        return true;
    }
    private boolean isWrench(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.item.ItemStack wrench = ic2.api.item.IC2Items.getItem("wrench");
        if (wrench == null || wrench.isEmpty()) wrench = ic2.api.item.IC2Items.getItem("tool", "wrench");
        return (wrench != null && !wrench.isEmpty() && stack.getItem() == wrench.getItem()) || isElectricWrench(stack);
    }
    private boolean isElectricWrench(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.item.ItemStack electric = ic2.api.item.IC2Items.getItem("electric_wrench");
        if (electric == null || electric.isEmpty()) electric = ic2.api.item.IC2Items.getItem("tool", "electric_wrench");
        return electric != null && !electric.isEmpty() && stack.getItem() == electric.getItem();
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && isTwoHigh()) {
            BlockPos other = isUpper(state) ? pos.down() : pos.up(); IBlockState otherState = world.getBlockState(other);
            if (otherState.getBlock() == this && isUpper(otherState) != isUpper(state)) world.setBlockToAir(other);
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof FarmTile && !(tile instanceof ManagerTile && ((ManagerTile) tile).migrating))
            ((FarmTile) tile).dropContents();
        super.breakBlock(world, pos, state);
    }
    @Override public boolean canProvidePower(IBlockState state) { return kind == 3; }
    @Override public int getWeakPower(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        return kind == 3 && tile instanceof MonitorTile && ((MonitorTile)tile).signal > 0 ? ((MonitorTile)tile).signal : 0;
    }
    @Override public boolean isFullCube(IBlockState state) { return kind != 8 && kind != 12; }
    @Override public boolean isOpaqueCube(IBlockState state) { return kind != 8 && kind != 12; }
    @Override public net.minecraft.util.math.AxisAlignedBB getBoundingBox(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        if (kind != 12) return FULL_BLOCK_AABB;
        IBlockState actual = getActualState(state, world, pos);
        double minX = actual.getValue(WEST) ? 0 : 0.3125, maxX = actual.getValue(EAST) ? 1 : 0.6875;
        double minY = actual.getValue(DOWN) ? 0 : 0.3125, maxY = actual.getValue(UP) ? 1 : 0.6875;
        double minZ = actual.getValue(NORTH) ? 0 : 0.3125, maxZ = actual.getValue(SOUTH) ? 1 : 0.6875;
        return new net.minecraft.util.math.AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    @Override public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, net.minecraft.util.math.AxisAlignedBB entityBox, java.util.List<net.minecraft.util.math.AxisAlignedBB> boxes, net.minecraft.entity.Entity entity, boolean actual) {
        if (kind == 12) { addCollisionBoxToList(pos, entityBox, boxes, getBoundingBox(state, world, pos)); return; }
        super.addCollisionBoxToList(state, world, pos, entityBox, boxes, entity, actual);
    }
    @Override public int damageDropped(IBlockState state) { return 0; }
}
