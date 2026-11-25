package br.cropmanager;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Shared block for the Flux-style Wi-Fi EU transmitter ({@code eu_wifi_plug})
 * and receiver ({@code eu_wifi_point}).
 */
public final class EuWifiBlock extends Block {
    public enum Kind {
        PLUG("eu_wifi_plug", 18),
        POINT("eu_wifi_point", 19);

        final String registryName;
        final int guiId;

        Kind(String registryName, int guiId) {
            this.registryName = registryName;
            this.guiId = guiId;
        }
    }

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private final Kind kind;

    public EuWifiBlock(Kind kind) {
        super(Material.IRON);
        this.kind = kind;
        setRegistryName(CropManager.ID, kind.registryName);
        setTranslationKey("cropmanager." + kind.registryName);
        setCreativeTab(CropManager.TAB);
        setHardness(2.5F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return kind == Kind.PLUG ? new EuWifiPlugTile() : new EuWifiPointTile();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote || !(placer instanceof EntityPlayer)) return;
        TileEntity tile = world.getTileEntity(pos);
        EntityPlayer player = (EntityPlayer) placer;
        if (tile instanceof EuWifiPlugTile) {
            ((EuWifiPlugTile) tile).setOwner(player);
        } else if (tile instanceof EuWifiPointTile) {
            ((EuWifiPointTile) tile).setOwner(player);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof FarmTile && ((FarmTile) tile).usable(player)) {
            player.openGui(CropManager.instance, kind.guiId, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager." + kind.registryName + "_help"));
    }
}
