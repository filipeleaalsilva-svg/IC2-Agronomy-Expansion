package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

/** Container for the Agricultural Grain Silo GUI (ID 17) with virtual bulk deposit/withdraw slots. */
public final class AgriculturalSiloContainer extends Container {
    static final int DEPOSIT_SLOT = 0;
    static final int WITHDRAW_SLOT = 1;
    static final int UPGRADE_SLOT = 2;
    static final int SILO_SLOTS = 3;

    public final AgriculturalSiloTile machine;
    private final InventoryBasic virtualBacking = new InventoryBasic("", false, 2);
    private NBTTagCompound previous;

    public AgriculturalSiloContainer(InventoryPlayer player, AgriculturalSiloTile machine) {
        this.machine = machine;
        addSlotToContainer(new DepositSlot(virtualBacking, 0, 9, 21));
        addSlotToContainer(new WithdrawSlot(virtualBacking, 1, 9, 57));
        addSlotToContainer(new SlotItemHandler(machine.upgrades, 0, 148, 21) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return machine.upgrades.isItemValid(0, stack);
            }

            @Override
            public int getSlotStackLimit() {
                return machine.upgrades.getSlotLimit(0);
            }

            @Override
            public int getItemStackLimit(ItemStack stack) {
                return Math.min(getSlotStackLimit(), stack.getMaxStackSize());
            }

            @Override
            public void onSlotChanged() {
                machine.changed();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player, col, 8 + col * 18, 179));
        }
    }

    private final class DepositSlot extends Slot {
        DepositSlot(InventoryBasic inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return machine.acceptsItem(stack) && machine.getStoredCount() < machine.getCapacity();
        }

        @Override
        public int getItemStackLimit(ItemStack stack) {
            int space = Math.max(0, machine.getCapacity() - machine.getStoredCount());
            return Math.min(stack.getMaxStackSize(), space);
        }

        @Override
        public void putStack(ItemStack stack) {
            if (!stack.isEmpty() && machine.getWorld() != null && !machine.getWorld().isRemote) {
                ItemStack remainder = machine.insertBulk(stack, false);
                super.putStack(remainder);
                return;
            }
            super.putStack(ItemStack.EMPTY);
        }
    }

    private final class WithdrawSlot extends Slot {
        private boolean decrHandled;

        WithdrawSlot(InventoryBasic inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return machine.getStoredCount() > 0 && !machine.getStoredTemplate().isEmpty();
        }

        @Override
        public ItemStack getStack() {
            if (machine.getStoredCount() <= 0 || machine.getStoredTemplate().isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack tpl = machine.getStoredTemplate();
            return ItemHandlerHelper.copyStackWithSize(
                    tpl, Math.min(machine.getStoredCount(), tpl.getMaxStackSize()));
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            if (machine.getWorld() != null && !machine.getWorld().isRemote) {
                ItemStack extracted = machine.extractBulk(amount, false);
                if (!extracted.isEmpty()) decrHandled = true;
                return extracted;
            }
            return machine.extractBulk(amount, true);
        }

        @Override
        public void putStack(ItemStack stack) {
            if (!stack.isEmpty() && machine.getWorld() != null && !machine.getWorld().isRemote) {
                machine.insertBulk(stack, false);
            }
            onSlotChanged();
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            if (decrHandled) {
                decrHandled = false;
            } else if (!stack.isEmpty() && machine.getWorld() != null && !machine.getWorld().isRemote) {
                // Covers hotbar SWAP clicks where Container.slotClick bypasses decrStackSize.
                machine.extractBulk(stack.getCount(), false);
            }
            onSlotChanged();
            return stack;
        }
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
        if (index == WITHDRAW_SLOT) {
            ItemStack preview = machine.extractBulk(64, true);
            if (preview.isEmpty()) return ItemStack.EMPTY;
            int requested = preview.getCount();
            ItemStack toMove = preview.copy();
            if (!mergeItemStack(toMove, SILO_SLOTS, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
            int actuallyMoved = requested - toMove.getCount();
            if (actuallyMoved > 0 && machine.getWorld() != null && !machine.getWorld().isRemote) {
                machine.extractBulk(actuallyMoved, false);
            }
            // Return EMPTY so each Shift-Click moves at most one stack instead of flooding the inventory.
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index == DEPOSIT_SLOT || index == UPGRADE_SLOT) {
            if (!mergeItemStack(stack, SILO_SLOTS, inventorySlots.size(), true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            slot.onTake(player, stack);
            return original;
        }
        if (stack.getItem() == CropManager.RESERVOIR_CAPACITY) {
            if (!mergeItemStack(stack, UPGRADE_SLOT, UPGRADE_SLOT + 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            slot.onTake(player, stack);
            return original;
        }
        if (machine.acceptsItem(stack) && machine.getStoredCount() < machine.getCapacity()) {
            ItemStack remainder = machine.insertBulk(stack,
                    machine.getWorld() != null && machine.getWorld().isRemote);
            if (remainder.getCount() == stack.getCount()) return ItemStack.EMPTY;
            slot.putStack(remainder);
            slot.onSlotChanged();
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }
}
