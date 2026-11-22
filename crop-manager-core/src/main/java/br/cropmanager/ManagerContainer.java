package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.SlotItemHandler;

public final class ManagerContainer extends Container {
    public final ManagerTile machine;
    private NBTTagCompound lastState;

    public ManagerContainer(InventoryPlayer player, ManagerTile tile) {
        machine = tile;
        for (int i = 0; i < 3; i++) addMachineSlot(i, 10 + i * 18, 85, false);
        for (int i = 0; i < 15; i++) addMachineSlot(3 + i, 98 + (i % 5) * 18, 22 + (i / 5) * 18, true);
        for (int i = 0; i < 4; i++) addMachineSlot(18 + i, 98 + i * 18, 85, false);
        addMachineSlot(22, 33, 24, false); addMachineSlot(23, 51, 24, false);
        addMachineSlot(24, 33, 48, true); addMachineSlot(25, 51, 48, true);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player, col + row * 9 + 9, 16 + col * 18, 126 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 16 + col * 18, 184));
    }
    private void addMachineSlot(int index, int x, int y, boolean output) {
        addSlotToContainer(new SlotItemHandler(machine.inventory, index, x, y) {
            @Override public boolean isItemValid(ItemStack stack) { return !output && super.isItemValid(stack); }
            @Override public void onSlotChanged() { machine.inventoryChanged(index); }
        });
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return machine.usable(player); }
    @Override public void addListener(IContainerListener listener) {
        super.addListener(listener);
        if (listener instanceof EntityPlayerMP) sendState((EntityPlayerMP) listener, machine.guiState());
    }
    private void sendState(EntityPlayerMP player, NBTTagCompound tag) {
        ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.State(machine.getPos(), windowId, tag), player);
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        NBTTagCompound state = machine.guiState();
        if (!state.equals(lastState)) {
            for (IContainerListener listener : listeners)
                if (listener instanceof EntityPlayerMP) sendState((EntityPlayerMP) listener, state);
            lastState = state;
        }
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack source = slot.getStack(), original = source.copy();
        boolean moved;
        if (index < 26) moved = mergeItemStack(source, 26, inventorySlots.size(), true);
        else {
            moved = false;
            // Container order: fertilizer, products, upgrades, two inputs, two outputs.
            if (ManagerTile.isFertilizer(source)) moved = mergeItemStack(source, 0, 3, false);
            else if (ManagerTile.isUpgrade(source)) moved = mergeItemStack(source, 18, 22, false);
            else moved = mergeItemStack(source, 22, 24, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, source);
        return original;
    }
}
