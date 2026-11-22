package br.cropmanager.crop;

import br.cropmanager.CropManager;
import br.cropmanager.FertilizerItem;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Shared IC2 CropCard base for Crop Manager species, with optional Catalytic Soil requirement. */
public class BaseCropCard extends CropCard {
    private final String id;
    private final CropProperties properties;
    private final String[] attributes;
    private final Supplier<ItemStack> gainSupplier;
    private final boolean catalyzed;
    private final int maxSize;
    private final int sizeAfterHarvest;
    private final int matureLight;
    private final List<ResourceLocation> textures;

    public BaseCropCard(String id, CropProperties properties, String[] attributes,
            Supplier<ItemStack> gainSupplier, boolean catalyzed) {
        this(id, properties, attributes, gainSupplier, catalyzed, 4, 2, 0);
    }

    public BaseCropCard(String id, CropProperties properties, String[] attributes,
            Supplier<ItemStack> gainSupplier, boolean catalyzed, int matureLight) {
        this(id, properties, attributes, gainSupplier, catalyzed, 4, 2, matureLight);
    }

    public BaseCropCard(String id, CropProperties properties, String[] attributes,
            Supplier<ItemStack> gainSupplier, boolean catalyzed, int maxSize, int sizeAfterHarvest, int matureLight) {
        this.id = id;
        this.properties = properties;
        this.attributes = attributes.clone();
        this.gainSupplier = gainSupplier;
        this.catalyzed = catalyzed;
        this.maxSize = maxSize;
        this.sizeAfterHarvest = sizeAfterHarvest;
        this.matureLight = matureLight;
        List<ResourceLocation> list = new ArrayList<>(maxSize);
        for (int stage = 1; stage <= maxSize; stage++) {
            list.add(new ResourceLocation(CropManager.ID, "blocks/crops/" + id + "_" + stage));
        }
        this.textures = Collections.unmodifiableList(list);
    }

    public boolean requiresCatalyst() { return catalyzed; }
    public ItemStack previewGain() {
        ItemStack drop = gainSupplier.get();
        return drop == null ? ItemStack.EMPTY : drop.copy();
    }

    @Override public String getId() { return id; }
    @Override public String getOwner() { return CropManager.ID; }
    @Override public String getUnlocalizedName() { return CropManager.ID + ".crop." + id; }
    @Override public String getDiscoveredBy() { return "Crop Manager"; }
    @Override public CropProperties getProperties() { return properties; }
    @Override public String[] getAttributes() { return attributes.clone(); }
    @Override public int getMaxSize() { return maxSize; }
    @Override public int getOptimalHarvestSize(ICropTile crop) { return maxSize; }
    @Override public int getSizeAfterHarvest(ICropTile crop) { return sizeAfterHarvest; }
    @Override public List<ResourceLocation> getTexturesLocation() { return textures; }
    @Override public int getEmittedLight(ICropTile crop) {
        if (matureLight <= 0) return 0;
        int size = crop.getCurrentSize();
        return size >= maxSize ? matureLight : size == maxSize - 1 ? matureLight / 2 : 0;
    }

    @Override
    public boolean canGrow(ICropTile crop) {
        if (crop.getCurrentSize() >= maxSize) return false;
        if (crop.getLightLevel() < 9) return false;
        // Catalyzed species grow up to stage 2 normally, then require Catalytic Fertilizer soil charges.
        if (catalyzed && crop.getCurrentSize() >= 2 && FertilizerItem.catalyzedCharges(crop) <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canBeHarvested(ICropTile crop) {
        return crop.getCurrentSize() >= maxSize;
    }

    @Override
    public ItemStack getGain(ICropTile crop) {
        if (catalyzed) FertilizerItem.consumeCatalystCharge(crop);
        ItemStack drop = gainSupplier.get();
        return drop == null ? ItemStack.EMPTY : drop.copy();
    }
}
