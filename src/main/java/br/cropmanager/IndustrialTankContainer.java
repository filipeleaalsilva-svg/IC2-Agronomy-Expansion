package br.cropmanager;

import static br.cropmanager.IndustrialTankTile.CAPACITY_SLOT;
import static br.cropmanager.IndustrialTankTile.CONTAINER_IN_SLOT;
import static br.cropmanager.IndustrialTankTile.CONTAINER_OUT_SLOT;
import static br.cropmanager.IndustrialTankTile.INVENTORY_SLOTS;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Container for the single-fluid Industrial Fluid Reservoir GUI (ID 16). */
public final class IndustrialTankContainer extends Container {
    public final IndustrialTankTile machine;
    private final ItemStackHandler items;
    private NBTTagCompound previous;

    public IndustrialTankContainer(InventoryPlayer player, IndustrialTankTile machine) {
        this.machine = machine;
        this.items = machine.inventory;
        addTankSlot(CONTAINER_IN_SLOT, 9, 21);
        addTankSlot(CONTAINER_OUT_SLOT, 9, 57);
        addTankSlot(CAPACITY_SLOT, 148, 21);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player, col, 8 + col * 18, 179));
        }
    }

    private void addTankSlot(final int slot, int x, int y) {
        addSlotToContainer(new SlotItemHandler(items, slot, x, y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return items.isItemValid(slot, stack);
            }

            @Override
            public int getSlotStackLimit() {
                return items.getSlotLimit(slot);
            }

            @Override
            public int getItemStackLimit(ItemStack stack) {
                return Math.min(getSlotStackLimit(), stack.getMaxStackSize());
            }

            @Override
            public void onSlotChanged() {
                machine.inventoryChanged(slot);
            }
        });
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return (machine.getWorld().isRemote || machine.formed) && machine.usable(player);
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
        NBTTagCompound state = machine.guiState();
        if (!state.equals(previous)) {
            for (IContainerListener listener : listeners) send(listener, state);
            previous = state.copy();
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < INVENTORY_SLOTS) {
            moved = mergeItemStack(stack, INVENTORY_SLOTS, inventorySlots.size(), true);
        } else if (stack.getItem() == CropManager.RESERVOIR_CAPACITY) {
            moved = mergeItemStack(stack, CAPACITY_SLOT, CAPACITY_SLOT + 1, false);
        } else {
            moved = mergeItemStack(stack, CONTAINER_IN_SLOT, CONTAINER_IN_SLOT + 1, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}
