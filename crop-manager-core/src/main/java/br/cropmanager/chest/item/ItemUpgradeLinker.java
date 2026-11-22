package br.cropmanager.chest.item;

import br.cropmanager.CropManager;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Upgrade item that binds to a placed Crop Database Chest and links it to a Portable Crop Database.
 */
public final class ItemUpgradeLinker extends Item {
    public static final String LINK_TAG = "LinkedChest";

    public ItemUpgradeLinker() {
        setRegistryName(CropManager.ID, "upgrade_linker");
        setTranslationKey(CropManager.ID + ".upgrade_linker");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    public static boolean hasLink(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey(LINK_TAG, 10);
    }

    public static NBTTagCompound getLink(ItemStack stack) {
        return hasLink(stack) ? stack.getTagCompound().getCompoundTag(LINK_TAG) : null;
    }

    public static void setLink(ItemStack stack, int dim, BlockPos pos) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound link = new NBTTagCompound();
        link.setInteger("Dim", dim);
        link.setInteger("X", pos.getX());
        link.setInteger("Y", pos.getY());
        link.setInteger("Z", pos.getZ());
        stack.getTagCompound().setTag(LINK_TAG, link);
    }

    public static void copyLink(ItemStack from, ItemStack to) {
        NBTTagCompound link = getLink(from);
        if (link == null) return;
        if (!to.hasTagCompound()) to.setTagCompound(new NBTTagCompound());
        to.getTagCompound().setTag(LINK_TAG, link.copy());
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return EnumActionResult.PASS;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileCropDatabaseChest)) return EnumActionResult.PASS;
        if (!world.isRemote) {
            ItemStack held = player.getHeldItem(hand);
            setLink(held, world.provider.getDimension(), pos);
            player.sendStatusMessage(new TextComponentTranslation(
                    "cropmanager.linker.bound", pos.getX(), pos.getY(), pos.getZ(), world.provider.getDimension()), true);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        EnumHand otherHand = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack other = player.getHeldItem(otherHand);
        if (hasLink(held) && !other.isEmpty() && other.getItem() == CropManager.PORTABLE_DATABASE) {
            if (!world.isRemote) {
                copyLink(held, other);
                if (!player.capabilities.isCreativeMode) held.shrink(1);
                player.sendStatusMessage(new TextComponentTranslation("cropmanager.linker.applied"), true);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, held);
        }
        if (player.isSneaking() && hasLink(held)) {
            if (!world.isRemote) {
                held.getTagCompound().removeTag(LINK_TAG);
                if (held.getTagCompound().isEmpty()) held.setTagCompound(null);
                player.sendStatusMessage(new TextComponentTranslation("cropmanager.linker.cleared"), true);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, held);
        }
        return new ActionResult<>(EnumActionResult.PASS, held);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound link = getLink(stack);
        if (link != null) {
            tooltip.add(I18n.format("cropmanager.linker.target",
                    link.getInteger("X"), link.getInteger("Y"), link.getInteger("Z"), link.getInteger("Dim")));
            tooltip.add(I18n.format("cropmanager.linker.apply_help"));
        } else {
            tooltip.add(I18n.format("cropmanager.linker.bind_help"));
        }
    }
}
