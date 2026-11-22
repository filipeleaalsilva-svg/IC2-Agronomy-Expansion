package br.cropmanager.chest.integration;

import br.cropmanager.chest.inventory.SeedInventory;
import ic2.api.crops.CropCard;
import ic2.api.crops.ICropSeed;
import java.util.Locale;
import java.util.Set;
import net.minecraft.item.ItemStack;

public final class SeedSearch {
    private SeedSearch() { }

    public static CropCard identifiedCrop(ItemStack stack) {
        if (!SeedInventory.accepts(stack)) return null;
        ItemStack copy = stack.copy();
        ICropSeed seed = (ICropSeed) copy.getItem();
        return seed.getScannedFromStack(copy) >= 1 ? seed.getCropFromStack(copy) : null;
    }

    public static String identity(CropCard crop) { return crop.getOwner() + ":" + crop.getId(); }

    public static boolean matches(ItemStack stack, String query, Set<String> translatedMatches) {
        if (query.isEmpty()) return true;
        CropCard crop = identifiedCrop(stack);
        return crop != null && (identity(crop).toLowerCase(Locale.ROOT).contains(query)
                || translatedMatches.contains(identity(crop)));
    }
}
