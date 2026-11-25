package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.*;

/**
 * Container compartilhado pelas máquinas de suporte ({@link MonitorTile}, {@link WeederTile},
 * {@link ComposterTile}, {@link GrinGrinderTile}, {@link GrinExtractorTile},
 * {@link WeedExFabricatorTile} e {@link SeedAnalyzerTile}).
 *
 * <p>Delega coordenadas, validação de inserção manual e roteamento de shift-click ao
 * {@link EquipmentLayout} declarado por cada máquina, sincronizando {@code guiState()} quando
 * ocorrem alterações no servidor.
 */
public final class EquipmentContainer extends Container {
    public final FarmTile machine;
    public final int machineSlots, playerY;
    private final EquipmentLayout layout;
    private NBTTagCompound previous;

    public EquipmentContainer(InventoryPlayer player, FarmTile tile) {
        this.machine = tile;
        this.layout = tile.equipmentLayout();
        ItemStackHandler items = layout == null ? null : layout.inventory;
        this.machineSlots = items == null ? 0 : items.getSlots();
        this.playerY = layout == null ? 126 : layout.playerY;
        for (int i = 0; i < machineSlots; i++) {
            final int slot = i;
            int x = i < layout.xs.length ? layout.xs[i] : 8 + i % 9 * 18;
            int y = i < layout.ys.length ? layout.ys[i] : 34 + i / 9 * 18;
            addSlotToContainer(new SlotItemHandler(items, i, x, y) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return layout.canPlaceInSlot(slot, stack);
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

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return machine.usable(p);
    }

    private void send(IContainerListener l, NBTTagCompound s) {
        if (l instanceof EntityPlayerMP) {
            ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.State(machine.getPos(), windowId, s), (EntityPlayerMP) l);
        }
    }

    @Override
    public void addListener(IContainerListener l) {
        super.addListener(l);
        send(l, machine.guiState());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        NBTTagCompound s = machine.guiState();
        if (!s.equals(previous)) {
            for (IContainerListener l : listeners) send(l, s);
            previous = s;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        boolean moved;
        if (index < machineSlots) {
            moved = mergeItemStack(stack, machineSlots, inventorySlots.size(), true);
        } else {
            int[] range = layout == null ? null : layout.shiftTarget.apply(stack);
            if (range == null) return ItemStack.EMPTY;
            moved = mergeItemStack(stack, range[0], range[1], false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        slot.onTake(player, stack);
        return original;
    }
}
