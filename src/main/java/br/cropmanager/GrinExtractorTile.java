package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.energy.tile.IHeatSource;
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
 * <p>Processa Grin Powder e água em Extrato de Grin exigindo energia elétrica (EU) e calor contínuo
 * ({@link IHeatSource}) recebido de qualquer uma das 6 faces, diretamente ou via {@link ThermalPipeTile}.
 */
public final class GrinExtractorTile extends FarmTile implements ITickable, IUpgradableBlock {
    public static final int CAPACITY = 16000, WATER_PER_RECIPE = 1000, EXTRACT_PER_RECIPE = 1000, DURATION = 100, EU_PER_TICK = 5, HEAT_PER_TICK = 20;
    public int progress, clientDuration = ManagerConfig.extractorDuration, workTicks = ManagerConfig.extractorDuration, clientHeatPerTick = ManagerConfig.extractorHeatPerTick;
    public double euPerTick = ManagerConfig.extractorEuPerTick;
    private boolean upgradesDirty = true;
    public boolean heatConnected;
    public String condition = "extractor_ready";
    public final BasicSink energy = createSink(ManagerConfig.extractorEnergyCapacity, ManagerConfig.extractorSinkTier);
    @Override public BasicSink energySink() { return energy; }
    @Override public boolean isSupportMachine() { return true; }
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

    @Override public void onLoad() { if (!world.isRemote) { recalculateUpgrades(); energy.onLoad(); } }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }
    @Override public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateUpgrades();
        tickUpgrades(this, inventory, 4, 8);
        dischargeBattery(inventory, 8, energy);
        energy.update();
        emptyFluidContainer(inventory, 1, 3, water);
        fillFluidContainer(inventory, 2, 3, extract);
        heatConnected = hasHeatNeighbor();
        ItemStack grin = inventory.getStackInSlot(0);
        if (grin.isEmpty()) { condition = "extractor_ingredients"; stop(); return; }
        int waterCost = ManagerConfig.extractorWaterPerRecipe, extractYield = ManagerConfig.extractorExtractPerRecipe;
        int heatCost = ManagerConfig.extractorHeatPerTick;
        if (water.getFluidAmount() < waterCost) { condition = "extractor_water"; stop(); return; }
        FluidStack batch = new FluidStack(CropManager.GRIN_EXTRACT, extractYield);
        if (extract.fill(batch, false) < extractYield) { condition = "extractor_output"; stop(); return; }
        EnumFacing heatDir = findReadyHeatSide(heatCost);
        if (heatCost > 0 && heatDir == null) { condition = "extractor_heat"; stop(); return; }
        if (energy.getEnergyStored() < euPerTick) { condition = "extractor_energy"; stop(); return; }
        if (heatCost > 0 && drawHeatFrom(heatDir, heatCost) < heatCost) { condition = "extractor_heat"; stop(); return; }
        energy.useEnergy(euPerTick); progress++; condition = "extractor_working"; markDirty(); setActive(true);
        if (progress >= workTicks) {
            inventory.extractItem(0, 1, false); water.drain(waterCost, true); extract.fill(batch, true);
            progress = 0; condition = "extractor_ready"; markDirty();
        }
    }
    private void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, inventory, 4, 8, energy, 1, ManagerConfig.extractorEnergyCapacity, ManagerConfig.extractorSinkTier);
        int next = Math.max(1, (int)Math.ceil(ManagerConfig.extractorDuration * multipliers[0]));
        progress = Math.min(next - 1, (int)((long)progress * next / Math.max(1, workTicks))); workTicks = next;
        euPerTick = Math.ceil(ManagerConfig.extractorEuPerTick * multipliers[1]);
        upgradesDirty = false;
    }
    @Override public double getEnergy() { return energy.getEnergyStored(); }
    @Override public boolean useEnergy(double amount) { boolean used = energy.useEnergy(amount); if (used) markDirty(); return used; }
    @Override public Set<UpgradableProperty> getUpgradableProperties() { return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer); }
    private void stop() { setActive(false); }
    private boolean hasHeatNeighbor() { for (EnumFacing side : EnumFacing.values()) if (adjacent(side) != null) return true; return false; }
    @Nullable private EnumFacing findReadyHeatSide(int heatCost) {
        if (heatCost <= 0) return EnumFacing.NORTH;
        for (EnumFacing side : EnumFacing.values()) {
            IHeatSource src = adjacent(side);
            if (src != null && src.requestHeat(side.getOpposite(), heatCost) >= heatCost) return side;
        }
        return null;
    }
    private int drawHeatFrom(@Nullable EnumFacing side, int heatCost) {
        if (side == null) return 0;
        IHeatSource src = adjacent(side);
        return src == null ? 0 : src.drawHeat(side.getOpposite(), heatCost, false);
    }
    @Nullable private IHeatSource adjacent(EnumFacing side) {
        BlockPos target = pos.offset(side);
        if (!world.isBlockLoaded(target, false)) return null;
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(target);
        return tile instanceof IHeatSource ? (IHeatSource) tile : null;
    }
    private IItemHandler exposed() { return new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot < 3 ? inventory.insertItem(slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot == 3 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
    }; }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, face); }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) { if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(exposed()); if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluids); return super.getCapability(capability, face); }
    @Override public NBTTagCompound guiState() { NBTTagCompound tag = new NBTTagCompound(); tag.setInteger("Water", water.getFluidAmount()); tag.setInteger("Extract", extract.getFluidAmount()); tag.setInteger("TankCapacity", water.getCapacity()); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); tag.setInteger("HeatPerTick", ManagerConfig.extractorHeatPerTick); tag.setString("Condition", condition); tag.setDouble("EU", energy.getEnergyStored()); tag.setDouble("Capacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier()); tag.setBoolean("Heat", heatConnected); return tag; }
    @Override public void acceptGuiState(NBTTagCompound tag) { if (tag.getInteger("TankCapacity") > 0) { water.setCapacity(tag.getInteger("TankCapacity")); extract.setCapacity(tag.getInteger("TankCapacity")); } water.setFluid(tag.getInteger("Water") > 0 ? new FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, tag.getInteger("Water")) : null); extract.setFluid(tag.getInteger("Extract") > 0 ? new FluidStack(CropManager.GRIN_EXTRACT, tag.getInteger("Extract")) : null); progress = tag.getInteger("Work"); if (tag.getInteger("Duration") > 0) { clientDuration = tag.getInteger("Duration"); workTicks = clientDuration; } if (tag.hasKey("HeatPerTick")) clientHeatPerTick = tag.getInteger("HeatPerTick"); condition = tag.getString("Condition"); energy.setCapacity(tag.getDouble("Capacity")); energy.setEnergyStored(tag.getDouble("EU")); energy.setSinkTier(Math.max(1, tag.getInteger("Tier"))); heatConnected = tag.getBoolean("Heat"); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); energy.writeToNBT(tag); tag.setTag("Inventory", inventory.serializeNBT()); tag.setTag("Water", water.writeToNBT(new NBTTagCompound())); tag.setTag("Extract", extract.writeToNBT(new NBTTagCompound())); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); energy.readFromNBT(tag); double eu = energy.getEnergyStored(); NBTTagCompound items = tag.getCompoundTag("Inventory").copy(); items.setInteger("Size", 9); inventory.deserializeNBT(items); water.readFromNBT(tag.getCompoundTag("Water")); extract.readFromNBT(tag.getCompoundTag("Extract")); resizeTank(water, ManagerConfig.extractorTankCapacity); resizeTank(extract, ManagerConfig.extractorTankCapacity); workTicks = Math.max(1, tag.hasKey("Duration") ? tag.getInteger("Duration") : ManagerConfig.extractorDuration); progress = Math.max(0, Math.min(workTicks - 1, tag.getInteger("Work"))); upgradesDirty = true; recalculateUpgrades(); energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0); }
    @Override public void dropContents() { dropHandler(inventory); }
}
