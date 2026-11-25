package br.cropmanager.guide;

import br.cropmanager.CropManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/**
 * In-game illustrated guide book item that opens {@link GuiAgriculturalManual} on the client.
 */
public class ItemAgriculturalManual extends Item {
    public ItemAgriculturalManual() {
        setRegistryName(CropManager.ID, "agricultural_manual");
        setTranslationKey("cropmanager.agricultural_manual");
        setCreativeTab(CropManager.TAB);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            CropManager.proxy.openGuideBook();
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
