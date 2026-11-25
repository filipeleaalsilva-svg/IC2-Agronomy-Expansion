package br.cropmanager;

import java.util.function.Function;
import java.util.function.IntPredicate;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Declarative slot layout and shift-click routing descriptor for {@link EquipmentContainer}.
 */
final class EquipmentLayout {
    final ItemStackHandler inventory;
    final int playerY;
    final int[] xs;
    final int[] ys;
    final IntPredicate manualInputSlot;
    final Function<ItemStack, int[]> shiftTarget;

    EquipmentLayout(ItemStackHandler inventory, int playerY, int[] xs, int[] ys,
            int outputSlot, boolean allowAllSlotsManual, Function<ItemStack, int[]> shiftTarget) {
        this(inventory, playerY, xs, ys,
                allowAllSlotsManual ? slot -> true : slot -> slot != outputSlot,
                shiftTarget);
    }

    EquipmentLayout(ItemStackHandler inventory, int playerY, int[] xs, int[] ys,
            IntPredicate manualInputSlot, Function<ItemStack, int[]> shiftTarget) {
        this.inventory = inventory;
        this.playerY = playerY;
        this.xs = xs;
        this.ys = ys;
        this.manualInputSlot = manualInputSlot;
        this.shiftTarget = shiftTarget;
    }

    boolean canPlaceInSlot(int slot, ItemStack stack) {
        return manualInputSlot.test(slot) && inventory.isItemValid(slot, stack);
    }
}
