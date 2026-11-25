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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

/** Controller for the EU-free 3x5x3 Agricultural Grain Silo bulk storage multiblock. */
public final class AgriculturalSiloBlock extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool FORMED = PropertyBool.create("formed");

    public AgriculturalSiloBlock() {
        super(Material.IRON);
        setRegistryName(CropManager.ID, "agricultural_silo");
        setTranslationKey("cropmanager.agricultural_silo");
        setCreativeTab(CropManager.TAB);
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(FORMED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, FORMED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        boolean formed = tile instanceof AgriculturalSiloTile && ((AgriculturalSiloTile) tile).formed;
        return state.withProperty(FORMED, Boolean.valueOf(formed));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new AgriculturalSiloTile();
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof AgriculturalSiloTile ? ((AgriculturalSiloTile) tile).comparatorSignal() : 0;
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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof AgriculturalSiloTile) ((AgriculturalSiloTile) tile).tryForm();
        }
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof AgriculturalSiloTile)) return false;
        if (!world.isRemote) {
            AgriculturalSiloTile silo = (AgriculturalSiloTile) tile;
            silo.tryForm();
            if (silo.formed) {
                player.openGui(CropManager.instance, 17, world, pos.getX(), pos.getY(), pos.getZ());
            } else {
                player.sendStatusMessage(new TextComponentTranslation("cropmanager.silo_unformed"), true);
            }
        }
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess access, BlockPos pos, IBlockState state, int fortune) {
        TileEntity tile = access.getTileEntity(pos);
        ItemStack stack = new ItemStack(this);
        if (tile instanceof AgriculturalSiloTile) {
            stack.setTagInfo("BlockEntityTag", ((AgriculturalSiloTile) tile).writeItemData());
        }
        drops.add(stack);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof AgriculturalSiloTile) ((AgriculturalSiloTile) tile).unformOnBreak();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos,
            IBlockState state, @Nullable TileEntity tile, ItemStack tool) {
        try {
            super.harvestBlock(world, player, pos, state, tile, tool);
        } finally {
            if (world.getBlockState(pos).getBlock() == this && world.getTileEntity(pos) == tile) {
                world.setBlockToAir(pos);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager.silo_help"));
        NBTTagCompound tag = stack.getSubCompound("BlockEntityTag");
        if (tag == null) return;
        ItemStack stored = tag.hasKey("StoredItem", 10)
                ? new ItemStack(tag.getCompoundTag("StoredItem"))
                : ItemStack.EMPTY;
        int count = Math.max(0, tag.getInteger("StoredCount"));
        if (!stored.isEmpty() && count > 0) {
            tooltip.add(I18n.format("cropmanager.silo_item_stored",
                    stored.getDisplayName(), UiNumbers.exact(count)));
        }
        ItemStackHandler upg = new ItemStackHandler(1);
        if (tag.hasKey("Upgrades", 10)) {
            NBTTagCompound upgTag = tag.getCompoundTag("Upgrades").copy();
            upgTag.setInteger("Size", 1);
            upg.deserializeNBT(upgTag);
            int countUpg = Math.min(AgriculturalSiloTile.MAX_CAPACITY_UPGRADES, upg.getStackInSlot(0).getCount());
            if (countUpg > 0) {
                tooltip.add(I18n.format("cropmanager.fluid_tank_item_upgrades",
                        Integer.valueOf(countUpg), Integer.valueOf(AgriculturalSiloTile.MAX_CAPACITY_UPGRADES)));
            }
        }
    }
}
