package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

final class CrossingToolContainer extends Container {
    private final InventoryPlayer inventory;
    private final int carrierSlot;
    private final ItemStack carrier;

    static CrossingToolContainer open(EntityPlayer player, int slot) {
        if (slot != 40 && (slot < 0 || slot >= 9)) return null;
        ItemStack carrier = player.inventory.getStackInSlot(slot);
        return carrier.getItem() == CropManager.CROSSING_TOOL && carrier.getCount() == 1
                ? new CrossingToolContainer(player.inventory, slot, carrier) : null;
    }

    CrossingToolContainer(InventoryPlayer inventory, int carrierSlot, ItemStack carrier) {
        this.inventory = inventory;
        this.carrierSlot = carrierSlot;
        this.carrier = carrier;
        ItemStackHandler stock = CrossingTool.supplies(carrier);
        for (int i = 0; i < CrossingTool.SLOTS; i++) {
            addSlotToContainer(new SlotItemHandler(stock, i, 62 + i * 18, 25) {
                @Override public boolean isItemValid(ItemStack stack) {
                    return CrossingTool.isCropStick(stack);
                }
                @Override public void onSlotChanged() {
                    stock.setStackInSlot(getSlotIndex(), getStack());
                    inventory.markDirty();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addPlayerSlot(9 + row * 9 + col, 8 + col * 18, 62 + row * 18);
        }
        for (int col = 0; col < 9; col++) addPlayerSlot(col, 8 + col * 18, 120);
    }

    private void addPlayerSlot(int index, int x, int y) {
        addSlotToContainer(new Slot(inventory, index, x, y) {
            @Override public boolean canTakeStack(EntityPlayer player) { return index != carrierSlot; }
            @Override public boolean isItemValid(ItemStack stack) { return index != carrierSlot; }
        });
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return inventory.player == player && inventory.getStackInSlot(carrierSlot) == carrier;
    }

    @Override public ItemStack slotClick(int slot, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player) && !player.world.isRemote) return ItemStack.EMPTY;
        if ((type == ClickType.SWAP && button == carrierSlot)
                || (carrierSlot < 9 && slot == CrossingTool.SLOTS + 27 + carrierSlot)) return ItemStack.EMPTY;
        return super.slotClick(slot, button, type, player);
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < CrossingTool.SLOTS) {
            moved = mergeItemStack(stack, CrossingTool.SLOTS, inventorySlots.size(), true);
        } else {
            moved = CrossingTool.isCropStick(stack) && mergeItemStack(stack, 0, CrossingTool.SLOTS, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return original;
    }
}
