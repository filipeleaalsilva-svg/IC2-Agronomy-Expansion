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
public final class GrinGrinderTile extends FarmTile implements ITickable, IUpgradableBlock {
    public static final int DURATION = 100, EU_PER_TICK = 5;
    public int progress, clientDuration = ManagerConfig.grinderDuration, workTicks = ManagerConfig.grinderDuration;
    public double euPerTick = ManagerConfig.grinderEuPerTick;
    private boolean upgradesDirty = true;
    public String condition = "grinder_ready";
    public final BasicSink energy = createSink(ManagerConfig.grinderEnergyCapacity, ManagerConfig.grinderSinkTier);
    @Override public BasicSink energySink() { return energy; }
    @Override public boolean isSupportMachine() { return true; }
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

    @Override public void onLoad() { if (!world.isRemote) { recalculateUpgrades(); energy.onLoad(); } }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }
    @Override public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateUpgrades();
        tickUpgrades(this, inventory, 3, 7);
        dischargeBattery(inventory, 7, energy);
        energy.update();
        ItemStack eye = inventory.getStackInSlot(0), blade = inventory.getStackInSlot(1);
        ItemStack grin = IC2Items.getItem("crop_res", "grin_powder");
        boolean canOutput = !grin.isEmpty() && inventory.insertItem(2, grin, true).isEmpty();
        if (eye.isEmpty() || blade.isEmpty() || blade.getItemDamage() >= blade.getMaxDamage()) {
            condition = "grinder_ingredients";
            setActive(false); return;
        }
        if (!canOutput) {
            condition = "grinder_output";
            setActive(false); return;
        }
        if (!energy.useEnergy(euPerTick)) { condition = "grinder_energy"; setActive(false); return; }
        condition = "grinder_working"; progress++; markDirty(); setActive(true);
        if (progress >= workTicks) {
            inventory.extractItem(0, 1, false);
            if (blade.getItemDamage() + 1 >= blade.getMaxDamage()) inventory.setStackInSlot(1, ItemStack.EMPTY);
            else { ItemStack damaged = blade.copy(); damaged.setItemDamage(damaged.getItemDamage() + 1); inventory.setStackInSlot(1, damaged); }
            inventory.insertItem(2, grin, false); progress = 0; condition = "grinder_ready"; markDirty();
        }
    }
    private void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, inventory, 3, 7, energy, 1, ManagerConfig.grinderEnergyCapacity, ManagerConfig.grinderSinkTier);
        int next = Math.max(1, (int)Math.ceil(ManagerConfig.grinderDuration * multipliers[0]));
        progress = Math.min(next - 1, (int)((long)progress * next / Math.max(1, workTicks))); workTicks = next;
        euPerTick = Math.ceil(ManagerConfig.grinderEuPerTick * multipliers[1]);
        upgradesDirty = false;
    }
    @Override public double getEnergy() { return energy.getEnergyStored(); }
    @Override public boolean useEnergy(double amount) { boolean used = energy.useEnergy(amount); if (used) markDirty(); return used; }
    @Override public Set<UpgradableProperty> getUpgradableProperties() { return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer); }
    private IItemHandler exposed() {
        return new IItemHandler() {
            @Override public int getSlots() { return inventory.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot < 2 ? inventory.insertItem(slot, stack, simulate) : stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot == 2 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
            @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        };
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, face); }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(exposed()) : super.getCapability(capability, face); }
    @Override public NBTTagCompound guiState() { NBTTagCompound tag = new NBTTagCompound(); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); tag.setString("Condition", condition); tag.setDouble("EU", energy.getEnergyStored()); tag.setDouble("Capacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier()); return tag; }
    @Override public void acceptGuiState(NBTTagCompound tag) { progress = tag.getInteger("Work"); if (tag.getInteger("Duration") > 0) { clientDuration = tag.getInteger("Duration"); workTicks = clientDuration; } condition = tag.getString("Condition"); energy.setCapacity(tag.getDouble("Capacity")); energy.setEnergyStored(tag.getDouble("EU")); energy.setSinkTier(Math.max(1, tag.getInteger("Tier"))); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); energy.writeToNBT(tag); tag.setTag("Inventory", inventory.serializeNBT()); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); energy.readFromNBT(tag); double eu = energy.getEnergyStored(); NBTTagCompound items = tag.getCompoundTag("Inventory").copy(); items.setInteger("Size", 8); inventory.deserializeNBT(items); workTicks = Math.max(1, tag.hasKey("Duration") ? tag.getInteger("Duration") : ManagerConfig.grinderDuration); progress = Math.max(0, Math.min(workTicks - 1, tag.getInteger("Work"))); upgradesDirty = true; recalculateUpgrades(); energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0); }
    @Override public void dropContents() { dropHandler(inventory); }
}
