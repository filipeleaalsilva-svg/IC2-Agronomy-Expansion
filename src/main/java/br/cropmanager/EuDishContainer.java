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

/**
 * Container for {@link EuDishAntennaTile} with 4 Wireless Range upgrade slots.
 */
public final class EuDishContainer extends Container {
    public final EuDishAntennaTile machine;
    private NBTTagCompound previous;

    public EuDishContainer(InventoryPlayer player, EuDishAntennaTile machine) {
        this.machine = machine;
        for (int i = 0; i < EuDishAntennaTile.MAX_RANGE_UPGRADES; i++) {
            final int slot = i;
            addSlotToContainer(new SlotItemHandler(machine.inventory, i, 53 + i * 18, 28) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return machine.inventory.isItemValid(slot, stack);
                }
            });
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
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        boolean moved;
        if (index < EuDishAntennaTile.MAX_RANGE_UPGRADES) {
            moved = mergeItemStack(stack, EuDishAntennaTile.MAX_RANGE_UPGRADES, inventorySlots.size(), true);
        } else {
            moved = MonitorTile.isWirelessRange(stack)
                    && mergeItemStack(stack, 0, EuDishAntennaTile.MAX_RANGE_UPGRADES, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}
