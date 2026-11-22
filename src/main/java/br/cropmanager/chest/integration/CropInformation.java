package br.cropmanager.chest.integration;

import ic2.api.crops.CropCard;
import ic2.api.crops.ICropSeed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/** Read-only snapshot. Never writes analysis or statistics back to the bag. */
public final class CropInformation {
    public final String nameKey;
    public final String discoveredBy;
    public final int tier, growth, gain, resistance, maxSize, scanLevel;
    public final List<String> attributes;
    public final ProductionInformation production;

    private CropInformation(String nameKey, String discoveredBy, int tier, int growth, int gain, int resistance,
                            int maxSize, int scanLevel, List<String> attributes, ProductionInformation production) {
        this.nameKey = nameKey;
        this.discoveredBy = discoveredBy;
        this.tier = tier;
        this.growth = growth;
        this.gain = gain;
        this.resistance = resistance;
        this.maxSize = maxSize;
        this.scanLevel = scanLevel;
        this.attributes = attributes;
        this.production = production;
    }

    public static CropInformation read(ItemStack bag) {
        if (bag.isEmpty() || !(bag.getItem() instanceof ICropSeed)) return unknown();
        // Work on a copy: third-party API implementations must not change the stored bag.
        ItemStack copy = bag.copy();
        ICropSeed seed = (ICropSeed) copy.getItem();
        int scan = Math.max(0, Math.min(4, seed.getScannedFromStack(copy)));
        if (scan < 1) return unknown();
        CropCard crop = seed.getCropFromStack(copy);
        if (crop == null) return unknown();
        String author = crop.getDiscoveredBy();
        List<String> attrs = Collections.emptyList();
        if (scan >= 3 && crop.getAttributes() != null) {
            List<String> list = new ArrayList<>();
            for (String attr : crop.getAttributes()) {
                if (attr != null && !attr.trim().isEmpty()) list.add(attr.trim());
            }
            attrs = Collections.unmodifiableList(list);
        }
        return new CropInformation(
                crop.getUnlocalizedName(),
                author == null ? "" : author,
                scan >= 2 ? crop.getProperties().getTier() : -1,
                scan >= 4 ? seed.getGrowthFromStack(copy) : -1,
                scan >= 4 ? seed.getGainFromStack(copy) : -1,
                scan >= 4 ? seed.getResistanceFromStack(copy) : -1,
                scan >= 2 ? crop.getMaxSize() : -1,
                scan,
                attrs,
                ProductionInformation.query(crop));
    }

    private static CropInformation unknown() {
        return new CropInformation("cropdatabasechest.unknown", "", -1, -1, -1, -1, -1, 0,
                Collections.emptyList(), ProductionInformation.hidden());
    }
}
