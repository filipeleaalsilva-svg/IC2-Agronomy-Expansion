package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.*;

public final class EquipmentContainer extends Container {
    public final FarmTile machine;
    public final int machineSlots, playerY;
    private NBTTagCompound previous;

    public EquipmentContainer(InventoryPlayer player, FarmTile tile) {
        machine = tile;
        ItemStackHandler items = tile instanceof MonitorTile ? ((MonitorTile) tile).inventory
                : tile instanceof ComposterTile ? ((ComposterTile) tile).inventory
                : tile instanceof WeederTile ? ((WeederTile) tile).inventory
                : tile instanceof WeedExFabricatorTile ? ((WeedExFabricatorTile) tile).inventory
                : null;
        machineSlots = items == null ? 0 : items.getSlots();
        playerY = tile instanceof MonitorTile ? 152 : 126;
        for (int i = 0; i < machineSlots; i++) {
            final int slot = i;
            int x, y;
            if (tile instanceof ComposterTile) {
                if (i == 6) { x = 8; y = 62; }
                else if (i < 2) { x = i == 0 ? 54 : 112; y = 42; }
                else { x = 148; y = 22 + (i - 2) * 18; }
            } else if (tile instanceof MonitorTile) {
                x = i < 4 ? 8 + i * 18 : i < 7 ? 98 + (i - 4) * 18 : i == 7 ? 98 : 8;
                y = i >= 7 ? 130 : 106;
            } else if (tile instanceof WeederTile) {
                if (i < 4) { x = 62 + i * 18; y = 42; }
                else if (i < 8) { x = 148; y = 22 + (i - 4) * 18; }
                else { x = 8; y = 62; }
            } else if (tile instanceof WeedExFabricatorTile) {
                if (i == 9) { x = 8; y = 62; }
                else if (i == 0) { x = 27; y = 20; }
                else if (i == 1) { x = 117; y = 20; }
                else if (i == 2) { x = 27; y = 86; }
                else if (i == 3) { x = 72; y = 86; }
                else if (i == 4) { x = 117; y = 86; }
                else { x = 148; y = 22 + (i - 5) * 18; }
            } else {
                x = 8 + i % 9 * 18;
                y = 34 + i / 9 * 18;
            }
            addSlotToContainer(new SlotItemHandler(items, i, x, y) {
                @Override public boolean isItemValid(ItemStack stack) {
                    if (items == null) return false;
                    if (tile instanceof ComposterTile) return slot != 1 && items.isItemValid(slot, stack);
                    if (tile instanceof MonitorTile) return items.isItemValid(slot, stack);
                    if (tile instanceof WeederTile) return slot >= 4 && items.isItemValid(slot, stack);
                    if (tile instanceof WeedExFabricatorTile) return slot != WeedExFabricatorTile.CONTAINER_OUT && items.isItemValid(slot, stack);
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player, col, 8 + col * 18, playerY + 58));
        }
    }

    @Override public boolean canInteractWith(EntityPlayer p) { return machine.usable(p); }

    private void send(IContainerListener l, NBTTagCompound s) {
        if (l instanceof EntityPlayerMP) ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.State(machine.getPos(), windowId, s), (EntityPlayerMP) l);
    }

    @Override public void addListener(IContainerListener l) {
        super.addListener(l);
        send(l, machine.guiState());
    }

    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        NBTTagCompound s = machine.guiState();
        if (!s.equals(previous)) {
            for (IContainerListener l : listeners) send(l, s);
            previous = s;
        }
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        boolean moved;
        if (index < machineSlots) {
            moved = mergeItemStack(stack, machineSlots, inventorySlots.size(), true);
        } else if (machine instanceof ComposterTile) {
            moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 6, 7, false)
                    : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 2, 6, false)
                    : mergeItemStack(stack, 0, 1, false);
        } else if (machine instanceof MonitorTile) {
            moved = MonitorTile.isWirelessRange(stack) ? mergeItemStack(stack, 0, 4, false)
                    : ManagerTile.isFertilizer(stack) ? mergeItemStack(stack, 4, 7, false)
                    : MonitorTile.isHarvestTransfer(stack) ? mergeItemStack(stack, 7, 8, false)
                    : MonitorTile.isWirelessEu(stack) && mergeItemStack(stack, 8, 9, false);
        } else if (machine instanceof WeederTile) {
            moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 8, 9, false)
                    : ManagerTile.isIc2Upgrade(stack) && mergeItemStack(stack, 4, 8, false);
        } else if (machine instanceof WeedExFabricatorTile) {
            moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 9, 10, false)
                    : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 5, 9, false)
                    : mergeItemStack(stack, 0, 4, false);
        } else return ItemStack.EMPTY;
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}
