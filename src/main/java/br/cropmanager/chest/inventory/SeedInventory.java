package br.cropmanager.chest.inventory;

import ic2.api.crops.CropCard;
import ic2.api.crops.ICropSeed;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

/** Seed-only storage shared by the GUI and automation. */
public class SeedInventory extends ItemStackHandler {
    public static final int SIZE = 500;
    public static final int COLUMNS = 5;
    public static final int PAGE_SIZE = 25;
    public static final int PAGES = (SIZE + PAGE_SIZE - 1) / PAGE_SIZE;

    private final int capacity;
    public SeedInventory() { this(SIZE); }
    public SeedInventory(int capacity) { super(capacity); this.capacity = capacity; }
    public static boolean accepts(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof ICropSeed; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(stack); }
    @Override public int getSlotLimit(int slot) { return 1; }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        // Forge 2847's insertItem does not call isItemValid; enforce the filter here too.
        // The legacy handler may retain the supplied object. Isolate it from player-stack edits.
        return stack.isEmpty() || isItemValid(slot, stack) ? super.insertItem(slot, stack.copy(), simulate) : stack;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!stack.isEmpty() && (!accepts(stack) || stack.getCount() != 1)) {
            throw new IllegalArgumentException("Only one IC2 Seed Bag per slot");
        }
        super.setStackInSlot(slot, stack);
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        NBTTagCompound fixedSize = tag.copy();
        fixedSize.setInteger("Size", capacity);
        super.deserializeNBT(fixedSize);
    }

    /** Snapshot sort keys before moving stacks; API reads never receive the stored bag. */
    public void sort(SeedSortMode mode) {
        List<SortEntry> entries = new ArrayList<>();
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) entries.add(new SortEntry(stack, mode));
        }
        entries.sort((left, right) -> left.compare(right, mode));
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack sorted = slot < entries.size() ? entries.get(slot).stack : ItemStack.EMPTY;
            if (!ItemStack.areItemStacksEqual(getStackInSlot(slot), sorted)) setStackInSlot(slot, sorted);
        }
    }

    private static final class SortEntry {
        private final ItemStack stack;
        private boolean known;
        private String type;
        private int tier, gain, growth, resistance;

        SortEntry(ItemStack stack, SeedSortMode mode) {
            this.stack = stack;
            ItemStack copy = stack.copy();
            ICropSeed seed = (ICropSeed) copy.getItem();
            int requiredScan = mode == SeedSortMode.TYPE ? 1 : mode == SeedSortMode.TIER ? 2 : 4;
            if (seed.getScannedFromStack(copy) < requiredScan) return;
            CropCard crop = seed.getCropFromStack(copy);
            if (crop == null) return;
            known = true;
            if (mode == SeedSortMode.TYPE) type = crop.getOwner() + ":" + crop.getId();
            else if (mode == SeedSortMode.TIER) tier = crop.getProperties().getTier();
            else {
                gain = seed.getGainFromStack(copy);
                growth = seed.getGrowthFromStack(copy);
                resistance = seed.getResistanceFromStack(copy);
            }
        }

        int compare(SortEntry other, SeedSortMode mode) {
            if (known != other.known) return known ? -1 : 1;
            if (!known) return 0;
            if (mode == SeedSortMode.TYPE) return type.compareTo(other.type);
            if (mode == SeedSortMode.TIER) return Integer.compare(other.tier, tier);
            int order = Integer.compare(other.gain, gain);
            if (order == 0) order = Integer.compare(other.growth, growth);
            if (order == 0) order = Integer.compare(resistance, other.resistance);
            return order;
        }
    }

    /** Inserts into the first free slot across all pages, preserving the original stack. */
    public ItemStack insertAnywhere(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack;
        for (int slot = 0; slot < capacity && !remainder.isEmpty(); slot++) {
            remainder = insertItem(slot, remainder, simulate);
        }
        return remainder;
    }
}
