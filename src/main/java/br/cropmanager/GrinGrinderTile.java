package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.item.IC2Items;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Entidade de bloco do Moedor de Grin (Tier 1).
 *
 * <p>Tritura olhos de aranha em Grin Powder IC2 consumindo EU e desgastando uma {@link GrindingBlade}
 * reutilizável a cada lote concluído.
 */
public final class GrinGrinderTile extends PoweredMachineTile {
    public static final int DURATION = 100, EU_PER_TICK = 5;
    public int clientDuration = ManagerConfig.grinderDuration;
    public GrinGrinderTile() {
        super(ManagerConfig.grinderEnergyCapacity, ManagerConfig.grinderSinkTier,
                ManagerConfig.grinderDuration, ManagerConfig.grinderEuPerTick, "grinder_ready");
    }
    @Override protected ItemStackHandler machineInventory() { return inventory; }
    @Override protected int upgradeStartSlot() { return 3; }
    @Override protected int upgradeEndSlot() { return 7; }
    @Override protected int batterySlot() { return 7; }
    @Override protected int baseDuration() { return ManagerConfig.grinderDuration; }
    @Override protected double baseEuPerTick() { return ManagerConfig.grinderEuPerTick; }
    @Override protected int baseEnergyCapacity() { return ManagerConfig.grinderEnergyCapacity; }
    @Override protected int baseSinkTier() { return ManagerConfig.grinderSinkTier; }
    @Override protected boolean processMachineTick() {
        ItemStack eye = inventory.getStackInSlot(0), blade = inventory.getStackInSlot(1);
        ItemStack grin = IC2Items.getItem("crop_res", "grin_powder");
        boolean canOutput = !grin.isEmpty() && inventory.insertItem(2, grin, true).isEmpty();
        if (eye.isEmpty() || blade.isEmpty() || blade.getItemDamage() >= blade.getMaxDamage()) {
            condition = "grinder_ingredients";
            return false;
        }
        if (!canOutput) {
            condition = "grinder_output";
            return false;
        }
        if (!energy.useEnergy(euPerTick)) { condition = "grinder_energy"; return false; }
        condition = "grinder_working"; progress++; markDirty();
        if (progress >= workTicks) {
            inventory.extractItem(0, 1, false);
            if (blade.getItemDamage() + 1 >= blade.getMaxDamage()) inventory.setStackInSlot(1, ItemStack.EMPTY);
            else { ItemStack damaged = blade.copy(); damaged.setItemDamage(damaged.getItemDamage() + 1); inventory.setStackInSlot(1, damaged); }
            inventory.insertItem(2, grin, false); progress = 0; condition = "grinder_ready"; markDirty();
        }
        return true;
    }
    public final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 2 || isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == 0) return stack.getItem() == Items.SPIDER_EYE;
            if (slot == 1) return stack.getItem() == CropManager.GRINDING_BLADE;
            if (slot == 7) return isBattery(stack);
            return slot >= 3 && slot < 7 && ManagerTile.isIc2Upgrade(stack);
        }
        @Override protected void onContentsChanged(int slot) { if (slot >= 3 && slot < 7) upgradesDirty = true; markDirty(); }
    };

    private IItemHandler exposed() {
        return exposedItems(inventory, 2, 2);
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, face); }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(exposed()) : super.getCapability(capability, face); }
    @Override public NBTTagCompound guiState() { NBTTagCompound tag = new NBTTagCompound(); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); tag.setString("Condition", condition); tag.setDouble("EU", energy.getEnergyStored()); tag.setDouble("Capacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier()); return tag; }
    @Override public void acceptGuiState(NBTTagCompound tag) { progress = tag.getInteger("Work"); if (tag.getInteger("Duration") > 0) { clientDuration = tag.getInteger("Duration"); workTicks = clientDuration; } condition = tag.getString("Condition"); energy.setCapacity(tag.getDouble("Capacity")); energy.setEnergyStored(tag.getDouble("EU")); energy.setSinkTier(Math.max(1, tag.getInteger("Tier"))); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); writePoweredNBT(tag); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); readPoweredNBT(tag); }
    @Override EquipmentLayout equipmentLayout() {
        return new EquipmentLayout(inventory, 126,
                new int[]{38, 80, 122, 148, 148, 148, 148, 8},
                new int[]{52, 52, 52, 22, 40, 58, 76, 62},
                2, false,
                s -> isBattery(s) ? new int[]{7, 8} : ManagerTile.isIc2Upgrade(s) ? new int[]{3, 7} : new int[]{0, 2});
    }
}
