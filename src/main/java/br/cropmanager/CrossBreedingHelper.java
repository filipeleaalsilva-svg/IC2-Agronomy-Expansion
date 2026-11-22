package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import java.util.Random;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Shared cross-breeding acceleration and safe stat-bonus logic for IC2 double crop sticks.
 *
 * <p>In IC2 Experimental, crops with {@code GR >= 24} behave as weeds and can overtake adjacent
 * crops; {@link #SAFE_MAX_GROWTH} caps growth stat bonuses at 23 while Gain and Resistance cap at 31.
 */
public final class CrossBreedingHelper {
    public static final int SAFE_MAX_GROWTH = 23;
    public static final int MAX_STAT = 31;

    private CrossBreedingHelper() {}

    /**
     * Requires a loaded crossing site and all four loaded neighbors because IC2 reads each neighbor.
     * At least two neighbors must contain non-weed crops eligible to cross-breed.
     */
    public static boolean hasReadyParents(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        int y = pos.getY();
        if (y < 0 || y >= 256 || !world.isBlockLoaded(pos, false)) return false;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            if (!world.isBlockLoaded(pos.offset(facing), false)) return false;
        }
        int ready = 0;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = pos.offset(facing);
            TileEntity tile = world.getTileEntity(neighbor);
            if (!(tile instanceof TileEntityCrop)) continue;
            TileEntityCrop parent = (TileEntityCrop) tile;
            CropCard card = parent.getCrop();
            if (card != null && card != Crops.weed && card.canCross(parent)) {
                ready++;
                if (ready >= 2) return true;
            }
        }
        return false;
    }

    /** Returns {@code true} if {@code crop} is an empty double crop stick with at least two ready parents. */
    public static boolean canAccelerate(World world, TileEntityCrop crop) {
        return world != null
                && crop != null
                && crop.isCrossingBase()
                && crop.getCrop() == null
                && hasReadyParents(world, crop.getPos());
    }

    /**
     * Runs up to {@code ticks} simulated crop ticks on an empty double crop stick to accelerate
     * IC2 cross-breeding while blocking spontaneous weeds and preserving Weed-Ex storage.
     *
     * @return {@code true} if the target was a valid crossing base with ready parents and ticks ran
     */
    public static boolean accelerateCrossing(World world, TileEntityCrop crop, int ticks,
            boolean allowStatBonus, Random rand) {
        if (!canAccelerate(world, crop) || world.isRemote || ticks <= 0) return false;
        int savedWeedEx = crop.getStorageWeedEX();
        for (int i = 0; i < ticks; i++) {
            crop.performTick();
            CropCard result = crop.getCrop();
            if (result == null) continue;
            if (result == Crops.weed) {
                crop.reset();
                crop.setCrossingBase(true);
            } else {
                if (allowStatBonus && rand != null && rand.nextDouble() < ManagerConfig.mutagenicStatBonusChance) {
                    applySafeStatBonus(crop, rand);
                }
                break;
            }
        }
        crop.setStorageWeedEX(savedWeedEx);
        crop.dirty = true;
        crop.updateState();
        BlockPos pos = crop.getPos();
        if (pos != null && pos.getY() >= 0 && pos.getY() < 256 && world.isBlockLoaded(pos, false)) {
            world.markChunkDirty(pos, crop);
        }
        return true;
    }

    /**
     * Increments one randomly chosen eligible stat by {@code +1} (Growth up to {@link #SAFE_MAX_GROWTH},
     * Gain and Resistance up to {@link #MAX_STAT}).
     *
     * @return {@code true} if a stat was incremented
     */
    public static boolean applySafeStatBonus(TileEntityCrop crop, Random rand) {
        if (crop == null || rand == null) return false;
        int[] eligible = new int[3];
        int count = 0;
        if (crop.getStatGrowth() < SAFE_MAX_GROWTH) eligible[count++] = 0;
        if (crop.getStatGain() < MAX_STAT) eligible[count++] = 1;
        if (crop.getStatResistance() < MAX_STAT) eligible[count++] = 2;
        if (count == 0) return false;
        int chosen = eligible[rand.nextInt(count)];
        if (chosen == 0) {
            crop.setStatGrowth(crop.getStatGrowth() + 1);
        } else if (chosen == 1) {
            crop.setStatGain(crop.getStatGain() + 1);
        } else {
            crop.setStatResistance(crop.getStatResistance() + 1);
        }
        return true;
    }
}
