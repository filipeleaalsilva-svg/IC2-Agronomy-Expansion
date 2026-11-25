package br.cropmanager;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class GrindingBlade extends Item {
    public GrindingBlade() {
        setRegistryName(CropManager.ID, "grinding_blade");
        setTranslationKey("cropmanager.grinding_blade");
        setCreativeTab(CropManager.TAB);
        setMaxStackSize(1);
        setMaxDamage(256);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ManagerConfig.grinderBladeDurability;
    }
}
