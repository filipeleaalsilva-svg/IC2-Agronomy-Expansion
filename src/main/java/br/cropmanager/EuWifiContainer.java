package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Shared server/client container for {@link EuWifiPlugTile} and {@link EuWifiPointTile}.
 */
public final class EuWifiContainer extends Container {
    public final FarmTile machine;
    private NBTTagCompound previous;

    public EuWifiContainer(InventoryPlayer player, FarmTile machine) {
        this.machine = machine;
        if (machine instanceof BiogasSprayerTile) {
            addSlotToContainer(new net.minecraftforge.items.SlotItemHandler(((BiogasSprayerTile) machine).fertilizer, 0, 146, 49));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player, col, 8 + col * 18, 152));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return machine.usable(player);
    }

    private void send(IContainerListener listener, NBTTagCompound state) {
        if (listener instanceof EntityPlayerMP) {
            ManagerNetwork.CHANNEL.sendTo(
                    new ManagerNetwork.State(machine.getPos(), windowId, state),
                    (EntityPlayerMP) listener);
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        send(listener, machine.guiState());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        NBTTagCompound current = machine.guiState();
        if (!current.equals(previous)) {
            for (IContainerListener listener : listeners) send(listener, current);
            previous = current.copy();
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!(machine instanceof BiogasSprayerTile) || index < 0 || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved;
        if (index == 0) {
            moved = mergeItemStack(stack, 1, inventorySlots.size(), true);
        } else {
            moved = ManagerTile.isFertilizer(stack) && mergeItemStack(stack, 0, 1, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}
