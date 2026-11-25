package br.cropmanager;

import static br.cropmanager.AgriculturalReservoirTile.INVENTORY_SLOTS;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.inventory.ClickType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Container slots align with the supplied IC2-style reservoir panel. */
public final class ReservoirContainer extends Container {
    public final AgriculturalReservoirTile machine;
    private final ItemStackHandler items;
    private NBTTagCompound previous;
    private boolean upgrades;

    public ReservoirContainer(InventoryPlayer player, AgriculturalReservoirTile machine) {
        this.machine = machine;
        this.items = machine.inventory;
        addReservoirSlot(0, 9, 21);
        addReservoirSlot(1, 151, 21);
        addReservoirSlot(2, 9, 57);
        addReservoirSlot(3, 151, 57);
        addReservoirSlot(4, 48, 46);
        addReservoirSlot(5, 80, 46);
        addReservoirSlot(6, 112, 46);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 8 + col * 18, 179));
    }

    private void addReservoirSlot(final int slot, int x, int y) {
        addSlotToContainer(new SlotItemHandler(items, slot, x, y) {
            @Override public boolean isEnabled() { return isReservoirSlotVisible(slot); }
            @Override public boolean isItemValid(ItemStack stack) { return isReservoirSlotVisible(slot) && items.isItemValid(slot, stack); }
            @Override public boolean canTakeStack(EntityPlayer player) { return isReservoirSlotVisible(slot); }
            @Override public int getSlotStackLimit() { return items.getSlotLimit(slot); }
            @Override public int getItemStackLimit(ItemStack stack) {
                return Math.min(getSlotStackLimit(), stack.getMaxStackSize());
            }
            @Override public void onSlotChanged() { machine.inventoryChanged(slot); }
        });
    }

    private boolean isReservoirSlotVisible(int slot) {
        return upgrades == (slot >= AgriculturalReservoirTile.CAPACITY_SLOT);
    }

    boolean showingUpgrades() { return upgrades; }

    @Override public boolean enchantItem(EntityPlayer player, int id) {
        if ((id != 0 && id != 1) || !canInteractWith(player)) return false;
        if (player.inventory != null && !player.inventory.getItemStack().isEmpty()) return false;
        upgrades = id == 1;
        return true;
    }

    @Override public ItemStack slotClick(int slot, int button, ClickType type, EntityPlayer player) {
        if (slot >= 0 && slot < INVENTORY_SLOTS && !isReservoirSlotVisible(slot)) return ItemStack.EMPTY;
        return super.slotClick(slot, button, type, player);
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return (machine.getWorld().isRemote || machine.formed) && machine.usable(player);
    }

    private void send(net.minecraft.inventory.IContainerListener listener, NBTTagCompound state) {
        if (listener instanceof net.minecraft.entity.player.EntityPlayerMP) {
            ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.State(machine.getPos(), windowId, state),
                    (net.minecraft.entity.player.EntityPlayerMP) listener);
        }
    }
    @Override public void addListener(net.minecraft.inventory.IContainerListener listener) {
        super.addListener(listener);
        send(listener, machine.guiState());
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        NBTTagCompound state = machine.guiState();
        if (!state.equals(previous)) {
            for (net.minecraft.inventory.IContainerListener listener : listeners) send(listener, state);
            previous = state.copy();
        }
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()
                || (index < INVENTORY_SLOTS && !isReservoirSlotVisible(index))) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < INVENTORY_SLOTS) moved = mergeItemStack(stack, INVENTORY_SLOTS, inventorySlots.size(), true);
        else if (upgrades) moved = mergeItemStack(stack, AgriculturalReservoirTile.CAPACITY_SLOT, INVENTORY_SLOTS, false);
        else moved = mergeItemStack(stack, 0, 2, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}
