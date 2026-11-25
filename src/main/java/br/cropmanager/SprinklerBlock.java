package br.cropmanager;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 3D tiered agricultural sprinkler (Mk1: 9x9 1x, Mk2: 9x9 4x, Mk3: 17x17 4x). */
public final class SprinklerBlock extends Block {
    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(0.1875, 0.0, 0.1875, 0.8125, 0.6875, 0.8125);
    public final int tier;

    public SprinklerBlock(String name, int tier) {
        super(Material.IRON);
        this.tier = tier;
        setRegistryName(CropManager.ID, name);
        setTranslationKey("cropmanager." + name);
        setCreativeTab(CropManager.TAB);
        setHardness(2.5F);
        setResistance(8.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setDefaultState(blockState.getBaseState().withProperty(ACTIVE, false));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, ACTIVE); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(ACTIVE, (meta & 1) != 0); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(ACTIVE) ? 1 : 0; }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new SprinklerTile(tier); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return BOUNDS; }
    @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        int diameter = SprinklerTile.radiusForTier(tier) * 2 + 1;
        int speed = SprinklerTile.speedMultiplierForTier(tier);
        tooltip.add(I18n.format("cropmanager.sprinkler_tip_area", diameter, diameter, speed));
        tooltip.add(I18n.format("cropmanager.sprinkler_tip_water"));
        tooltip.add(I18n.format("cropmanager.sprinkler_tip_redstone"));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof SprinklerTile) ((SprinklerTile) te).refreshActiveState();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof SprinklerTile)) return false;
        SprinklerTile sprinkler = (SprinklerTile) te;
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && player.canPlayerEdit(pos, facing, held) && world.isBlockModifiable(player, pos)) {
            FluidActionResult res = FluidUtil.tryEmptyContainer(held, sprinkler.water, sprinkler.water.getCapacity(), player, true);
            if (res.isSuccess()) {
                if (!player.capabilities.isCreativeMode) player.setHeldItem(hand, res.getResult());
                sprinkler.refreshActiveState();
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7F, 1.1F);
                return true;
            }
        }
        if (player.isSneaking()) {
            if (!player.canPlayerEdit(pos, facing, held) || !world.isBlockModifiable(player, pos)) return false;
            sprinkler.redstoneMode = (sprinkler.redstoneMode + 1) % 3;
            sprinkler.markDirty();
            sprinkler.refreshActiveState();
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5F,
                    sprinkler.redstoneMode == 1 ? 0.6F : 0.5F);
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.redstone_" + sprinkler.redstoneMode), true);
            return true;
        }
        int diameter = sprinkler.radius() * 2 + 1;
        if (!sprinkler.isRedstoneAllowed()) {
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.sprinkler_status_paused",
                    diameter, diameter, sprinkler.speedMultiplier()), true);
        } else if (sprinkler.hasWaterSourceBelow()) {
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.sprinkler_status_source",
                    diameter, diameter, sprinkler.speedMultiplier()), true);
        } else {
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.sprinkler_status_tank",
                    diameter, diameter, sprinkler.speedMultiplier(),
                    sprinkler.water.getFluidAmount(), sprinkler.water.getCapacity()), true);
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (!state.getValue(ACTIVE)) return;
        int droplets = tier == 1 ? 4 : tier == 2 ? 8 : 12;
        double spread = Math.min(3.5, SprinklerTile.radiusForTier(tier) * 0.55);
        for (int i = 0; i < droplets; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double dist = 0.4 + rand.nextDouble() * spread;
            double x = pos.getX() + 0.5 + Math.cos(angle) * dist;
            double y = pos.getY() + 0.65 + rand.nextDouble() * 0.25;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * dist;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, x, y, z, 0.0, 0.05, 0.0);
        }
    }
}
