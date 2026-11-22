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
 * <p>Mapeia as coordenadas visuais dos slots por tipo de máquina, protege slots de saída contra
 * inserção manual e sincroniza {@code guiState()} quando ocorrem alterações no servidor.
 */
public final class EquipmentContainer extends Container {
    public final FarmTile machine;
    public final int machineSlots, playerY;
    private NBTTagCompound previous;
    public EquipmentContainer(InventoryPlayer player, FarmTile tile) {
        machine = tile;
        ItemStackHandler items = tile instanceof MonitorTile ? ((MonitorTile)tile).inventory : tile instanceof ComposterTile ? ((ComposterTile)tile).inventory : tile instanceof WeederTile ? ((WeederTile)tile).inventory : tile instanceof WeedExFabricatorTile ? ((WeedExFabricatorTile)tile).inventory : tile instanceof GrinGrinderTile ? ((GrinGrinderTile)tile).inventory : tile instanceof GrinExtractorTile ? ((GrinExtractorTile)tile).inventory : tile instanceof SeedAnalyzerTile ? ((SeedAnalyzerTile)tile).inventory : null;
        machineSlots = items == null ? 0 : items.getSlots(); playerY = tile instanceof MonitorTile ? 152 : 126;
        for (int i = 0; i < machineSlots; i++) {
            final int slot = i;
            int x, y;
            if (tile instanceof ComposterTile) { if (i == 6) { x = 8; y = 62; } else { x = i == 0 ? 54 : i == 1 ? 112 : 148; y = i < 2 ? 42 : 22 + (i - 2) * 18; } }
            else if (tile instanceof MonitorTile) { x = i < 4 ? 8 + i * 18 : i < 7 ? 98 + (i - 4) * 18 : i == 7 ? 98 : 8; y = i >= 7 ? 130 : 106; }
            else if (tile instanceof WeederTile) { x = 44 + i * 60; y = 64; }
            else if (tile instanceof WeedExFabricatorTile) { if (i == 8) { x = 8; y = 62; } else if (i < 4) { int[] xs = {72, 28, 84, 116}; int[] ys = {32, 84, 84, 84}; x = xs[i]; y = ys[i]; } else { x = 148; y = 22 + (i - 4) * 18; } }
            else if (tile instanceof GrinGrinderTile) { if (i == 7) { x = 8; y = 62; } else if (i < 3) { int[] xs = {38, 80, 122}; x = xs[i]; y = 52; } else { x = 148; y = 22 + (i - 3) * 18; } }
            else if (tile instanceof GrinExtractorTile) { if (i == 8) { x = 8; y = 62; } else if (i < 4) { int[] xs = {24, 54, 84, 114}; x = xs[i]; y = 84; } else { x = 148; y = 22 + (i - 4) * 18; } }
            else if (tile instanceof SeedAnalyzerTile) { if (i == 6) { x = 8; y = 62; } else if (i < 2) { x = i == 0 ? 34 : 110; y = 24; } else { x = 148; y = 22 + (i - 2) * 18; } }
            else { x = 8 + i % 9 * 18; y = 34 + i / 9 * 18; }
            addSlotToContainer(new SlotItemHandler(items, i, x, y) {
                @Override public boolean isItemValid(ItemStack stack) {
                    if (tile instanceof ComposterTile) return slot != 1 && items.isItemValid(slot, stack);
                    if (tile instanceof MonitorTile) return items.isItemValid(slot, stack);
                    if (tile instanceof WeederTile) return slot == 0 && items.isItemValid(slot, stack);
                    if (tile instanceof WeedExFabricatorTile) return slot != 3 && items.isItemValid(slot, stack);
                    if (tile instanceof GrinGrinderTile) return slot != 2 && items.isItemValid(slot, stack);
                    if (tile instanceof GrinExtractorTile) return slot != 3 && items.isItemValid(slot, stack);
                    if (tile instanceof SeedAnalyzerTile) return slot != 1 && items.isItemValid(slot, stack);
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, playerY + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 8 + col * 18, playerY + 58));
    }
    @Override public boolean canInteractWith(EntityPlayer p) { return machine.usable(p); }
    private void send(IContainerListener l, NBTTagCompound s) { if (l instanceof EntityPlayerMP) ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.State(machine.getPos(), windowId, s), (EntityPlayerMP)l); }
    @Override public void addListener(IContainerListener l) { super.addListener(l); send(l, machine.guiState()); }
    @Override public void detectAndSendChanges() { super.detectAndSendChanges(); NBTTagCompound s = machine.guiState(); if (!s.equals(previous)) { for (IContainerListener l : listeners) send(l, s); previous = s; } }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index); if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy(); boolean moved;
        if (index < machineSlots) moved = mergeItemStack(stack, machineSlots, inventorySlots.size(), true);
        else if (machine instanceof ComposterTile) moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 6, 7, false) : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 2, 6, false) : mergeItemStack(stack, 0, 1, false);
        else if (machine instanceof MonitorTile) moved = MonitorTile.isWirelessRange(stack) ? mergeItemStack(stack, 0, 4, false) : ManagerTile.isFertilizer(stack) ? mergeItemStack(stack, 4, 7, false) : MonitorTile.isHarvestTransfer(stack) ? mergeItemStack(stack, 7, 8, false) : MonitorTile.isWirelessEu(stack) ? mergeItemStack(stack, 8, 9, false) : false;
        else if (machine instanceof WeederTile) moved = mergeItemStack(stack, 0, 1, false);
        else if (machine instanceof WeedExFabricatorTile) moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 8, 9, false) : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 4, 8, false) : mergeItemStack(stack, 0, 3, false);
        else if (machine instanceof GrinGrinderTile) moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 7, 8, false) : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 3, 7, false) : mergeItemStack(stack, 0, 2, false);
        else if (machine instanceof GrinExtractorTile) moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 8, 9, false) : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 4, 8, false) : mergeItemStack(stack, 0, 3, false);
        else if (machine instanceof SeedAnalyzerTile) moved = FarmTile.isBattery(stack) ? mergeItemStack(stack, 6, 7, false) : ManagerTile.isIc2Upgrade(stack) ? mergeItemStack(stack, 2, 6, false) : mergeItemStack(stack, 0, 1, false);
        else return ItemStack.EMPTY;
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged(); slot.onTake(player, stack); return original;
    }
}
