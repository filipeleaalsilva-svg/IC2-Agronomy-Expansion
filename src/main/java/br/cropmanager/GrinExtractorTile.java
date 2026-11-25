package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.item.IC2Items;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Entidade de bloco do Extrator de Grin (Tier 1).
 *
 * <p>Processa Grin Powder e água em Extrato de Grin usando energia elétrica (EU).
 */
public final class GrinExtractorTile extends PoweredMachineTile {
    public static final int CAPACITY = 16000, WATER_PER_RECIPE = 1000, EXTRACT_PER_RECIPE = 1000, DURATION = 100, EU_PER_TICK = 5;
    public int clientDuration = ManagerConfig.extractorDuration;
    public GrinExtractorTile() {
        super(ManagerConfig.extractorEnergyCapacity, ManagerConfig.extractorSinkTier,
                ManagerConfig.extractorDuration, ManagerConfig.extractorEuPerTick, "extractor_ready");
    }
    @Override protected ItemStackHandler machineInventory() { return inventory; }
    @Override protected int upgradeStartSlot() { return 4; }
    @Override protected int upgradeEndSlot() { return 8; }
    @Override protected int batterySlot() { return 8; }
    @Override protected int baseDuration() { return ManagerConfig.extractorDuration; }
    @Override protected double baseEuPerTick() { return ManagerConfig.extractorEuPerTick; }
    @Override protected int baseEnergyCapacity() { return ManagerConfig.extractorEnergyCapacity; }
    @Override protected int baseSinkTier() { return ManagerConfig.extractorSinkTier; }
    public final FluidTank water = new FluidTank(ManagerConfig.extractorTankCapacity) {
        @Override public boolean canFillFluidType(FluidStack stack) { return stack != null && stack.getFluid() == net.minecraftforge.fluids.FluidRegistry.WATER; }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public final FluidTank extract = new FluidTank(ManagerConfig.extractorTankCapacity) {
        @Override public boolean canFillFluidType(FluidStack stack) { return stack != null && stack.getFluid() == CropManager.GRIN_EXTRACT; }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot == 3 || isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == 0) return ItemStack.areItemsEqual(stack, IC2Items.getItem("crop_res", "grin_powder"));
            if (slot == 1) { FluidStack fluid = FluidUtil.getFluidContained(stack); return fluid != null && fluid.getFluid() == net.minecraftforge.fluids.FluidRegistry.WATER; }
            if (slot == 2) return isExtractContainer(stack);
            if (slot == 8) return isBattery(stack);
            if (slot >= 4 && slot < 8) return ManagerTile.isIc2Upgrade(stack);
            return false;
        }
        @Override protected void onContentsChanged(int slot) { if (slot >= 4 && slot < 8) upgradesDirty = true; markDirty(); }
    };
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return new IFluidTankProperties[] {tankProperty(water, true, false), tankProperty(extract, false, true)}; }
        @Override public int fill(FluidStack stack, boolean execute) { return stack != null && stack.getFluid() == net.minecraftforge.fluids.FluidRegistry.WATER ? water.fill(stack, execute) : 0; }
        @Override public FluidStack drain(FluidStack stack, boolean execute) { return stack != null && stack.getFluid() == CropManager.GRIN_EXTRACT ? extract.drain(stack, execute) : null; }
        @Override public FluidStack drain(int amount, boolean execute) { return extract.drain(amount, execute); }
    };
    private static boolean isExtractContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidTank sample = new FluidTank(1000); sample.setFluid(new FluidStack(CropManager.GRIN_EXTRACT, 1000));
        return FluidUtil.tryFillContainer(stack, sample, 1000, null, false).isSuccess();
    }

    @Override protected boolean processMachineTick() {
        emptyFluidContainer(inventory, 1, 3, water);
        fillFluidContainer(inventory, 2, 3, extract);
        ItemStack grin = inventory.getStackInSlot(0);
        if (grin.isEmpty()) { condition = "extractor_ingredients"; return false; }
        int waterCost = ManagerConfig.extractorWaterPerRecipe, extractYield = ManagerConfig.extractorExtractPerRecipe;
        if (water.getFluidAmount() < waterCost) { condition = "extractor_water"; return false; }
        FluidStack batch = new FluidStack(CropManager.GRIN_EXTRACT, extractYield);
        if (extract.fill(batch, false) < extractYield) { condition = "extractor_output"; return false; }
        if (energy.getEnergyStored() < euPerTick) { condition = "extractor_energy"; return false; }
        energy.useEnergy(euPerTick); progress++; condition = "extractor_working"; markDirty();
        if (progress >= workTicks) {
            inventory.extractItem(0, 1, false); water.drain(waterCost, true); extract.fill(batch, true);
            progress = 0; condition = "extractor_ready"; markDirty();
        }
        return true;
    }
    private IItemHandler exposed() { return exposedItems(inventory, 3, 3); }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, face); }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) { if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(exposed()); if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluids); return super.getCapability(capability, face); }
    @Override public NBTTagCompound guiState() { NBTTagCompound tag = new NBTTagCompound(); tag.setInteger("Water", water.getFluidAmount()); tag.setInteger("Extract", extract.getFluidAmount()); tag.setInteger("TankCapacity", water.getCapacity()); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); tag.setString("Condition", condition); tag.setDouble("EU", energy.getEnergyStored()); tag.setDouble("Capacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier()); return tag; }
    @Override public void acceptGuiState(NBTTagCompound tag) { if (tag.getInteger("TankCapacity") > 0) { water.setCapacity(tag.getInteger("TankCapacity")); extract.setCapacity(tag.getInteger("TankCapacity")); } water.setFluid(tag.getInteger("Water") > 0 ? new FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, tag.getInteger("Water")) : null); extract.setFluid(tag.getInteger("Extract") > 0 ? new FluidStack(CropManager.GRIN_EXTRACT, tag.getInteger("Extract")) : null); progress = tag.getInteger("Work"); if (tag.getInteger("Duration") > 0) { clientDuration = tag.getInteger("Duration"); workTicks = clientDuration; } condition = tag.getString("Condition"); energy.setCapacity(tag.getDouble("Capacity")); energy.setEnergyStored(tag.getDouble("EU")); energy.setSinkTier(Math.max(1, tag.getInteger("Tier"))); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); writePoweredNBT(tag); tag.setTag("Water", water.writeToNBT(new NBTTagCompound())); tag.setTag("Extract", extract.writeToNBT(new NBTTagCompound())); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); water.readFromNBT(tag.getCompoundTag("Water")); extract.readFromNBT(tag.getCompoundTag("Extract")); resizeTank(water, ManagerConfig.extractorTankCapacity); resizeTank(extract, ManagerConfig.extractorTankCapacity); readPoweredNBT(tag); }
    @Override EquipmentLayout equipmentLayout() {
        return new EquipmentLayout(inventory, 126,
                new int[]{24, 54, 84, 114, 148, 148, 148, 148, 8},
                new int[]{84, 84, 84, 84, 22, 40, 58, 76, 62},
                3, false,
                s -> isBattery(s) ? new int[]{8, 9} : ManagerTile.isIc2Upgrade(s) ? new int[]{4, 8} : new int[]{0, 3});
    }
}
