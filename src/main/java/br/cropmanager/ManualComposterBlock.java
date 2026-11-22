package br.cropmanager;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Wooden + IC2-steel framed manual composter with direct right-click world interaction. */
public final class ManualComposterBlock extends Block {
    /** 0=empty, 1..3=raw filling, 4=fermenting, 5=ready compost. */
    public static final PropertyInteger FILL = PropertyInteger.create("fill", 0, 5);
    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.9375, 0.9375);

    public ManualComposterBlock() {
        super(Material.WOOD);
        setRegistryName(CropManager.ID, "manual_composter");
        setTranslationKey("cropmanager.manual_composter");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(2.0F);
        setResistance(5.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
        setDefaultState(blockState.getBaseState().withProperty(FILL, 0));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FILL); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FILL, Math.max(0, Math.min(5, meta))); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FILL); }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new ManualComposterTile(); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return BOUNDS; }
    @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof ManualComposterTile)) return false;
        ManualComposterTile composter = (ManualComposterTile) te;
        ItemStack held = player.getHeldItem(hand);

        if (world.isRemote) return true;
        if (!player.canPlayerEdit(pos, facing, held) || !world.isBlockModifiable(player, pos)) return false;

        // 1. Collect ready Organic Compost
        if (composter.isReady()) {
            ItemStack out = composter.collectCompost();
            if (!out.isEmpty()) {
                if (!player.inventory.addItemStackToInventory(out)) {
                    InventoryHelper.spawnItemStack(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, out);
                }
                world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 0.7F, 1.1F);
                return true;
            }
        }

        // 2. Hydrate with water bucket or water bottle to speed up fermentation
        if (!held.isEmpty() && isWaterContainer(held) && composter.hydrate()) {
            if (!player.capabilities.isCreativeMode) {
                ItemStack empty = held.getItem() == Items.WATER_BUCKET ? new ItemStack(Items.BUCKET) : new ItemStack(Items.GLASS_BOTTLE);
                held.shrink(1);
                if (held.isEmpty()) player.setHeldItem(hand, empty);
                else if (!player.inventory.addItemStackToInventory(empty)) {
                    InventoryHelper.spawnItemStack(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, empty);
                }
            }
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7F, 1.0F);
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.manual_composter_hydrated"), true);
            return true;
        }

        // 3. Insert organic items (Shift inserts up to full capacity)
        if (!held.isEmpty() && ComposterTile.organic(held)) {
            int inserted = 0;
            int max = player.isSneaking() ? held.getCount() : 1;
            while (inserted < max && !held.isEmpty() && composter.addOrganic(held)) {
                if (!player.capabilities.isCreativeMode) held.shrink(1);
                inserted++;
            }
            if (inserted > 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS, 0.6F, 0.9F + 0.05F * composter.visualFill());
                if (composter.isFermenting()) {
                    player.sendStatusMessage(new TextComponentTranslation("cropmanager.manual_composter_started"), true);
                }
                return true;
            }
        }

        // 4. Status readout on action bar
        if (composter.isFermenting()) {
            player.sendStatusMessage(new TextComponentTranslation(
                    composter.hydrated ? "cropmanager.manual_composter_progress_hydrated" : "cropmanager.manual_composter_progress",
                    composter.progressPercent()), true);
        } else {
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.manual_composter_fill",
                    composter.itemCount(), ManagerConfig.manualComposterRequiredItems), true);
        }
        return true;
    }

    private static boolean isWaterContainer(ItemStack stack) {
        if (stack.getItem() == Items.WATER_BUCKET) return true;
        return stack.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(stack) == PotionTypes.WATER;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof ManualComposterTile) {
            ((ManualComposterTile) tile).dropContents();
        }
        super.breakBlock(world, pos, state);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        int fill = state.getValue(FILL);
        if (fill == 4 && rand.nextInt(3) == 0) {
            double x = pos.getX() + 0.25 + rand.nextDouble() * 0.5;
            double y = pos.getY() + 0.92;
            double z = pos.getZ() + 0.25 + rand.nextDouble() * 0.5;
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0, 0.015, 0.0);
        } else if (fill == 5 && rand.nextInt(4) == 0) {
            double x = pos.getX() + 0.25 + rand.nextDouble() * 0.5;
            double y = pos.getY() + 0.95;
            double z = pos.getZ() + 0.25 + rand.nextDouble() * 0.5;
            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
