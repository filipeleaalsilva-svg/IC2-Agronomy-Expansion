package br.cropmanager;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Non-EU fluid pipe, redstone restrictor valve, and 6-way directional fluid router. */
public abstract class FluidPipeBlock extends Block {
    public enum Kind { PIPE, RESTRICTOR, ROUTER }

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public final Kind kind;
    private final String tipKey;

    private FluidPipeBlock(String name, Kind kind, String tipKey) {
        super(Material.IRON);
        this.kind = kind;
        this.tipKey = tipKey;
        setRegistryName(CropManager.ID, name);
        setTranslationKey("cropmanager." + name);
        setCreativeTab(CropManager.TAB);
        setHardness(2.5F);
        setResistance(8.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
    }

    public static FluidPipeBlock pipe() {
        return new FluidPipeBlock("fluid_pipe", Kind.PIPE, "cropmanager.fluid_pipe_help") {
            { setDefaultState(baseConnections(blockState.getBaseState())); }
            @Override protected BlockStateContainer createBlockState() {
                return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN);
            }
            @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState(); }
            @Override public int getMetaFromState(IBlockState state) { return 0; }
        };
    }

    public static FluidPipeBlock restrictor() {
        return new FluidPipeBlock("fluid_restrictor", Kind.RESTRICTOR, "cropmanager.fluid_restrictor_help") {
            { setDefaultState(baseConnections(blockState.getBaseState()).withProperty(POWERED, false)); }
            @Override protected BlockStateContainer createBlockState() {
                return new BlockStateContainer(this, POWERED, NORTH, EAST, SOUTH, WEST, UP, DOWN);
            }
            @Override public IBlockState getStateFromMeta(int meta) {
                return getDefaultState().withProperty(POWERED, (meta & 1) != 0);
            }
            @Override public int getMetaFromState(IBlockState state) {
                return state.getValue(POWERED) ? 1 : 0;
            }
        };
    }

    public static FluidPipeBlock router() {
        return new FluidPipeBlock("fluid_router", Kind.ROUTER, "cropmanager.fluid_router_help") {
            { setDefaultState(baseConnections(blockState.getBaseState()).withProperty(FACING, EnumFacing.NORTH)); }
            @Override protected BlockStateContainer createBlockState() {
                return new BlockStateContainer(this, FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN);
            }
            @Override public IBlockState getStateFromMeta(int meta) {
                return getDefaultState().withProperty(FACING, EnumFacing.byIndex(Math.max(0, meta) % 6));
            }
            @Override public int getMetaFromState(IBlockState state) {
                return state.getValue(FACING).getIndex();
            }
            @Override public IBlockState withRotation(IBlockState state, Rotation rot) {
                return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
            }
            @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
                return state.withRotation(mirror.toRotation(state.getValue(FACING)));
            }
        };
    }

    protected static IBlockState baseConnections(IBlockState base) {
        return base.withProperty(NORTH, false).withProperty(EAST, false)
                .withProperty(SOUTH, false).withProperty(WEST, false)
                .withProperty(UP, false).withProperty(DOWN, false);
    }

    @Override public boolean hasTileEntity(IBlockState state) { return true; }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        FluidPipeTile tile = new FluidPipeTile(kind);
        if (kind == Kind.RESTRICTOR) tile.powered = state.getValue(POWERED);
        if (kind == Kind.ROUTER) tile.outputSide = state.getValue(FACING);
        return tile;
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing routerOut = kind == Kind.ROUTER ? state.getValue(FACING) : null;
        return state
                .withProperty(NORTH, routerOut != EnumFacing.NORTH && canConnectFluid(world, pos, EnumFacing.NORTH))
                .withProperty(EAST, routerOut != EnumFacing.EAST && canConnectFluid(world, pos, EnumFacing.EAST))
                .withProperty(SOUTH, routerOut != EnumFacing.SOUTH && canConnectFluid(world, pos, EnumFacing.SOUTH))
                .withProperty(WEST, routerOut != EnumFacing.WEST && canConnectFluid(world, pos, EnumFacing.WEST))
                .withProperty(UP, routerOut != EnumFacing.UP && canConnectFluid(world, pos, EnumFacing.UP))
                .withProperty(DOWN, routerOut != EnumFacing.DOWN && canConnectFluid(world, pos, EnumFacing.DOWN));
    }

    public static boolean canConnectFluid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity neighbor = world.getTileEntity(pos.offset(side));
        if (neighbor == null || neighbor.isInvalid()) return false;
        if (neighbor instanceof FluidPipeTile) return true;
        return neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState actual = getActualState(state, world, pos);
        double coreMin = kind == Kind.ROUTER ? 0.25 : 0.3125;
        double coreMax = kind == Kind.ROUTER ? 0.75 : 0.6875;
        EnumFacing out = kind == Kind.ROUTER ? actual.getValue(FACING) : null;
        double minX = (actual.getValue(WEST) || out == EnumFacing.WEST) ? 0.0 : coreMin;
        double maxX = (actual.getValue(EAST) || out == EnumFacing.EAST) ? 1.0 : coreMax;
        double minY = (actual.getValue(DOWN) || out == EnumFacing.DOWN) ? 0.0 : coreMin;
        double maxY = (actual.getValue(UP) || out == EnumFacing.UP) ? 1.0 : coreMax;
        double minZ = (actual.getValue(NORTH) || out == EnumFacing.NORTH) ? 0.0 : coreMin;
        double maxZ = (actual.getValue(SOUTH) || out == EnumFacing.SOUTH) ? 1.0 : coreMax;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
            List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, getBoundingBox(state, world, pos));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (kind == Kind.RESTRICTOR) {
            return getDefaultState().withProperty(POWERED, world.isBlockPowered(pos));
        }
        if (kind == Kind.ROUTER) {
            EnumFacing out = placer.isSneaking()
                    ? facing
                    : EnumFacing.getDirectionFromEntityLiving(pos, placer).getOpposite();
            return getDefaultState().withProperty(FACING, out);
        }
        return getDefaultState();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isRemote || kind != Kind.RESTRICTOR) return;
        boolean nowPowered = world.isBlockPowered(pos);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof FluidPipeTile) {
            ((FluidPipeTile) te).powered = nowPowered;
            te.markDirty();
        }
        if (state.getValue(POWERED) != nowPowered) {
            world.setBlockState(pos, state.withProperty(POWERED, nowPowered), 3);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (isWrench(held)) {
            if (world.isRemote) return true;
            if (player.isSneaking()) {
                if (!player.canPlayerEdit(pos, facing, held)) return false;
                if (isElectricWrench(held) && !ic2.api.item.ElectricItem.manager.use(held, 100, player)) return true;
                world.setBlockToAir(pos);
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(this));
                if (!isElectricWrench(held)) held.damageItem(1, player);
                return true;
            }
            if (kind != Kind.ROUTER) return true;
        }
        if (kind != Kind.ROUTER) return false;
        if (world.isRemote) return true;
        if (!player.canPlayerEdit(pos, facing, held)) return false;

        EnumFacing current = state.getValue(FACING);
        EnumFacing next = player.isSneaking() && !isWrench(held)
                ? facing
                : EnumFacing.byIndex((current.getIndex() + 1) % 6);
        if (next == current) next = EnumFacing.byIndex((current.getIndex() + 1) % 6);

        IBlockState updated = state.withProperty(FACING, next);
        world.setBlockState(pos, updated, 3);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof FluidPipeTile) {
            ((FluidPipeTile) te).outputSide = next;
            te.markDirty();
        }
        if (isWrench(held) && !isElectricWrench(held)) {
            held.damageItem(1, player);
        } else if (isElectricWrench(held)) {
            ic2.api.item.ElectricItem.manager.use(held, 50, player);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 0.5F, 1.2F);
        player.sendStatusMessage(new TextComponentTranslation("cropmanager.fluid_router_facing",
                new TextComponentTranslation("cropmanager.face_" + next.getName())), true);
        return true;
    }

    private static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack wrench = ic2.api.item.IC2Items.getItem("wrench");
        if (wrench == null || wrench.isEmpty()) wrench = ic2.api.item.IC2Items.getItem("tool", "wrench");
        return (wrench != null && !wrench.isEmpty() && stack.getItem() == wrench.getItem()) || isElectricWrench(stack);
    }

    private static boolean isElectricWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack electric = ic2.api.item.IC2Items.getItem("electric_wrench");
        if (electric == null || electric.isEmpty()) electric = ic2.api.item.IC2Items.getItem("tool", "electric_wrench");
        return electric != null && !electric.isEmpty() && stack.getItem() == electric.getItem();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format(tipKey));
    }
}
