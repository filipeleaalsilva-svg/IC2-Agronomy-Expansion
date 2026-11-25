package br.cropmanager;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Simple named item with a localized client-only tooltip. */
public final class TooltipItem extends Item {
    private final String tipKey;

    public TooltipItem(String name, CreativeTabs tab, int maxStack, String tipKey) {
        this.tipKey = tipKey;
        setRegistryName(CropManager.ID, name);
        setTranslationKey("cropmanager." + name);
        setCreativeTab(tab);
        setMaxStackSize(maxStack);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format(tipKey));
    }
}
