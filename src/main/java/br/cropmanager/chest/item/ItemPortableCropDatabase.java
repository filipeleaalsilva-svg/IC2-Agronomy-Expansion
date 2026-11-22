package br.cropmanager.chest.item;

import br.cropmanager.CropManager;
import br.cropmanager.chest.inventory.PortableCropInventory;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPortableCropDatabase extends Item {
    public ItemPortableCropDatabase() {
        setRegistryName(CropManager.ID, "portable_crop_database");
        setTranslationKey(CropManager.ID + ".portable_crop_database");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    /**
     * Resolves the linked {@link TileCropDatabaseChest} from the item stack's NBT.
     *
     * <p><b>Wireless Connection Rules:</b></p>
     * <ul>
     *   <li><b>Dimension:</b> Stored in NBT tag {@code Dim}. Resolves across different dimensions
     *       using {@link MinecraftServer#getWorld(int)}. Cross-dimensional access is supported.</li>
     *   <li><b>Range:</b> Unlimited distance across dimensions and worlds.</li>
     *   <li><b>Chunk Loading:</b> Requires the target chunk to be currently loaded ({@code targetWorld.isBlockLoaded(pos)}).
     *       If unloaded, returns {@code null} without force-loading chunks.</li>
     *   <li><b>Target & Broken Block:</b> Must be an intact, valid {@link TileCropDatabaseChest}.
     *       If broken, invalid, or missing, returns {@code null} and notifies the player gracefully.</li>
     * </ul>
     */
    public static TileCropDatabaseChest resolveLinkedChest(EntityPlayer player, ItemStack stack) {
        NBTTagCompound link = ItemUpgradeLinker.getLink(stack);
        if (link == null || player == null || player.world == null || player.world.isRemote) return null;
        MinecraftServer server = player.world.getMinecraftServer();
        if (server == null) return null;
        WorldServer targetWorld = server.getWorld(link.getInteger("Dim"));
        if (targetWorld == null) return null;
        BlockPos pos = new BlockPos(link.getInteger("X"), link.getInteger("Y"), link.getInteger("Z"));
        if (!targetWorld.isBlockLoaded(pos)) return null;
        TileEntity tile = targetWorld.getTileEntity(pos);
        return tile instanceof TileCropDatabaseChest && !tile.isInvalid() ? (TileCropDatabaseChest) tile : null;
    }

    @Override
    public net.minecraft.util.EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos,
            net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!player.isSneaking()) return EnumActionResult.PASS;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileCropDatabaseChest)) return EnumActionResult.PASS;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack held = player.getHeldItem(hand);
        ItemUpgradeLinker.setLink(held, world.provider.getDimension(), pos);
        player.sendStatusMessage(new TextComponentTranslation("cropmanager.linker.linked",
                pos.getX(), pos.getY(), pos.getZ()), true);
        return EnumActionResult.SUCCESS;
    }

    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!player.inventory.getItemStack().isEmpty()) return new ActionResult<>(EnumActionResult.FAIL, held);
        if (!world.isRemote) {
            int slot = hand == EnumHand.MAIN_HAND ? player.inventory.currentItem : 40;
            if (ItemUpgradeLinker.hasLink(held)) {
                if (resolveLinkedChest(player, held) != null) {
                    player.openGui(CropManager.instance, 13, world, slot, 0, 0);
                } else {
                    player.sendStatusMessage(new TextComponentTranslation("cropmanager.linker.unavailable"), true);
                    return new ActionResult<>(EnumActionResult.FAIL, held);
                }
            } else {
                player.openGui(CropManager.instance, 12, world, slot, 0, 0);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    @Override @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound link = ItemUpgradeLinker.getLink(stack);
        if (link != null) {
            tooltip.add(I18n.format("cropmanager.linker.linked_to",
                    link.getInteger("X"), link.getInteger("Y"), link.getInteger("Z"), link.getInteger("Dim")));
        } else {
            int occupied = stack.hasTagCompound() ? stack.getTagCompound().getCompoundTag(PortableCropInventory.DATA)
                    .getCompoundTag("Seeds").getTagList("Items", 10).tagCount() : 0;
            tooltip.add(I18n.format("cropdatabasechest.portable.capacity", occupied));
        }
        tooltip.add(I18n.format(PortableCropInventory.isPickupEnabled(stack) ? "cropdatabasechest.pickup.tooltip_on" : "cropdatabasechest.pickup.tooltip_off"));
    }
}
