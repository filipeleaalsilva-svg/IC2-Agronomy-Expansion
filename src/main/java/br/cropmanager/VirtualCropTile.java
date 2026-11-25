package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.ICropTile;
import ic2.core.item.ItemCropSeed;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Lightweight {@link ICropTile} adapter used by {@link HydroponicChamberTile} to evaluate
 * {@link CropCard#getGain(ICropTile)} safely for both vanilla IC2 and custom crops without
 * triggering world crop ticks or radiation auras.
 */
final class VirtualCropTile implements ICropTile {
    private final World world;
    private final BlockPos pos;
    private CropCard crop;
    private int currentSize;
    private int statGrowth;
    private int statGain;
    private int statResistance;
    private final Block subsoil;
    private final NBTTagCompound customData = new NBTTagCompound();

    VirtualCropTile(World world, BlockPos pos, CropCard crop, int growth, int gain, int resistance, Block subsoil) {
        this.world = world;
        this.pos = pos;
        this.crop = crop;
        this.currentSize = crop == null ? 0 : crop.getMaxSize();
        this.statGrowth = Math.max(0, Math.min(31, growth));
        this.statGain = Math.max(0, Math.min(31, gain));
        this.statResistance = Math.max(0, Math.min(31, resistance));
        this.subsoil = subsoil;
    }

    @Override public CropCard getCrop() { return crop; }
    @Override public void setCrop(CropCard cropCard) { this.crop = cropCard; }
    @Override public int getCurrentSize() { return currentSize; }
    @Override public void setCurrentSize(int size) { this.currentSize = size; }
    @Override public int getStatGrowth() { return statGrowth; }
    @Override public void setStatGrowth(int growth) { this.statGrowth = growth; }
    @Override public int getStatGain() { return statGain; }
    @Override public void setStatGain(int gain) { this.statGain = gain; }
    @Override public int getStatResistance() { return statResistance; }
    @Override public void setStatResistance(int resistance) { this.statResistance = resistance; }
    @Override public int getStorageNutrients() { return 100; }
    @Override public void setStorageNutrients(int nutrients) {}
    @Override public int getStorageWater() { return 100; }
    @Override public void setStorageWater(int water) {}
    @Override public int getStorageWeedEX() { return 100; }
    @Override public void setStorageWeedEX(int weedEX) {}
    @Override public int getScanLevel() { return 4; }
    @Override public void setScanLevel(int scanLevel) {}
    @Override public int getGrowthPoints() { return 0; }
    @Override public void setGrowthPoints(int growthPoints) {}
    @Override public boolean isCrossingBase() { return false; }
    @Override public void setCrossingBase(boolean crossingBase) {}
    @Override public NBTTagCompound getCustomData() { return customData; }
    @Override public int getTerrainHumidity() { return 10; }
    @Override public int getTerrainNutrients() { return 10; }
    @Override public int getTerrainAirQuality() { return 10; }
    @Override public World getWorld() { return world; }
    @Override public World getWorldObj() { return world; }
    @Override public BlockPos getLocation() { return pos; }
    @Override public BlockPos getPosition() { return pos; }
    @Override public int getLightLevel() { return 15; }
    @Override public boolean pick() { return false; }
    @Override public boolean performManualHarvest() { return false; }
    @Override public List<ItemStack> performHarvest() { return Collections.emptyList(); }
    @Override public void reset() {}
    @Override public void updateState() {}

    @Override
    public boolean isBlockBelow(Block reqBlock) {
        return reqBlock != null && subsoil == reqBlock;
    }

    @Override
    public boolean isBlockBelow(String oreDictionaryName) {
        if (oreDictionaryName == null || subsoil == null || !OreDictionary.doesOreNameExist(oreDictionaryName)) return false;
        ItemStack stack = new ItemStack(subsoil);
        if (stack.isEmpty()) return false;
        int targetId = OreDictionary.getOreID(oreDictionaryName);
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (id == targetId) return true;
        }
        return false;
    }

    @Override
    public ItemStack generateSeeds(CropCard crop, int growth, int gain, int resistance, int scan) {
        if (crop == null) return ItemStack.EMPTY;
        return ItemCropSeed.generateItemStackFromValues(crop, growth, gain, resistance, scan);
    }
}
