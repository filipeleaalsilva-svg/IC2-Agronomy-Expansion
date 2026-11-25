package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ref.FluidName;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Misturador de Weed-Ex (Tier 1): combina Extrato de Grin e redstone para produzir fluido Weed-Ex.
 *
 * <p>Aceita upgrades IC2, bateria auxiliar, recipientes de fluido nos slots de entrada/saída e
 * preserva itens de layouts legados de 5 slots em {@code legacyDrops} para nunca descartar itens do jogador.
 */
public final class WeedExFabricatorTile extends PoweredMachineTile {
    public static final int EXTRACT_CAPACITY = 16000, WEED_CAPACITY = 16000, EXTRACT_PER_RECIPE = 1000;
    public static final int WEED_PER_RECIPE = 500, DURATION = 100, EU_PER_TICK = 5;
    public int clientDuration = ManagerConfig.fabricatorDuration;
    public WeedExFabricatorTile() {
        super(ManagerConfig.fabricatorEnergyCapacity, ManagerConfig.fabricatorSinkTier,
                ManagerConfig.fabricatorDuration, ManagerConfig.fabricatorEuPerTick, "weedex_ready");
    }
    @Override protected ItemStackHandler machineInventory() { return inventory; }
    @Override protected int upgradeStartSlot() { return 4; }
    @Override protected int upgradeEndSlot() { return 8; }
    @Override protected int batterySlot() { return 8; }
    @Override protected int baseDuration() { return ManagerConfig.fabricatorDuration; }
    @Override protected double baseEuPerTick() { return ManagerConfig.fabricatorEuPerTick; }
    @Override protected int baseEnergyCapacity() { return ManagerConfig.fabricatorEnergyCapacity; }
    @Override protected int baseSinkTier() { return ManagerConfig.fabricatorSinkTier; }
    public final FluidTank extract = new FluidTank(ManagerConfig.fabricatorExtractCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) { return fluid != null && fluid.getFluid() == CropManager.GRIN_EXTRACT; }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public final FluidTank weed = new FluidTank(ManagerConfig.fabricatorWeedCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) { return fluid != null && fluid.getFluid() == FluidName.weed_ex.getInstance(); }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    /** Slots: redstone, full Grin Extract container, empty container for Weed-Ex, processed containers, 4 IC2 upgrades. */
    public static final int REDSTONE = 0, EXTRACT_IN = 1, BOTTLE_IN = 2, CONTAINER_OUT = 3;
    private final java.util.List<ItemStack> legacyDrops = new java.util.ArrayList<>();
    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && slot != CONTAINER_OUT && !isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == REDSTONE) return stack.getItem() == net.minecraft.init.Items.REDSTONE;
            if (slot == EXTRACT_IN) return isExtractContainer(stack);
            if (slot == BOTTLE_IN) return isWeedExContainer(stack);
            if (slot == 8) return isBattery(stack);
            return slot >= 4 && slot < 8 && ManagerTile.isIc2Upgrade(stack);
        }
        @Override protected void onContentsChanged(int slot) { if (slot >= 4 && slot < 8) upgradesDirty = true; markDirty(); }
    };
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {tankProperty(extract, true, false), tankProperty(weed, false, true)};
        }
        @Override public int fill(FluidStack resource, boolean execute) {
            return resource != null && resource.getFluid() == CropManager.GRIN_EXTRACT ? extract.fill(resource, execute) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, boolean execute) {
            return resource != null && resource.getFluid() == FluidName.weed_ex.getInstance() ? weed.drain(resource, execute) : null;
        }
        @Override public FluidStack drain(int amount, boolean execute) { return weed.drain(amount, execute); }
    };

    private static boolean isWeedExContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidTank sample = new FluidTank(1000);
        sample.setFluid(new FluidStack(FluidName.weed_ex.getInstance(), 1000));
        return FluidUtil.tryFillContainer(stack, sample, 1000, null, false).isSuccess();
    }
    /** A container that holds Grin Extract and can be emptied into the input tank. */
    private static boolean isExtractContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidStack fluid = FluidUtil.getFluidContained(stack);
        return fluid != null && fluid.getFluid() == CropManager.GRIN_EXTRACT;
    }

    @Override protected boolean processMachineTick() {
        if (!legacyDrops.isEmpty()) {
            for (ItemStack stack : legacyDrops) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY() + 1, pos.getZ(), stack);
            legacyDrops.clear(); markDirty();
        }
        emptyFluidContainer(inventory, EXTRACT_IN, CONTAINER_OUT, extract);
        fillFluidContainer(inventory, BOTTLE_IN, CONTAINER_OUT, weed);
        return processRecipe();
    }

    private boolean processRecipe() {
        ItemStack redstone = inventory.getStackInSlot(REDSTONE);
        if (redstone.isEmpty()) { condition = "weedex_ingredients"; return false; }
        int extractCost = ManagerConfig.fabricatorExtractPerRecipe, weedYield = ManagerConfig.fabricatorWeedPerRecipe;
        if (extract.getFluidAmount() < extractCost) { condition = "weedex_extract"; return false; }
        if (weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), weedYield), false) < weedYield) { condition = "weedex_output"; return false; }
        if (!energy.useEnergy(euPerTick)) { condition = "weedex_energy"; return false; }
        condition = "weedex_working"; progress++; markDirty();
        if (progress >= workTicks) {
            inventory.extractItem(REDSTONE, 1, false);
            extract.drain(extractCost, true); weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), weedYield), true);
            progress = 0; condition = "weedex_ready"; markDirty();
        }
        return true;
    }

    private IItemHandler itemsForFace(@Nullable EnumFacing face) {
        return exposedItems(inventory, 3, CONTAINER_OUT);
    }
    @Override EquipmentLayout equipmentLayout() {
        return new EquipmentLayout(inventory, 126,
                new int[]{72, 28, 84, 116, 148, 148, 148, 148, 8},
                new int[]{32, 84, 84, 84, 22, 40, 58, 76, 62},
                CONTAINER_OUT, false,
                s -> isBattery(s) ? new int[]{8, 9} : ManagerTile.isIc2Upgrade(s) ? new int[]{4, 8} : new int[]{0, 3});
    }

    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, face);
    }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluids);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemsForFace(face));
        return super.getCapability(capability, face);
    }
    @Override public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound(); tag.setInteger("Extract", extract.getFluidAmount()); tag.setInteger("Weed", weed.getFluidAmount());
        tag.setInteger("ExtractCapacity", extract.getCapacity()); tag.setInteger("WeedCapacity", weed.getCapacity());
        tag.setDouble("EU", energy.getEnergyStored()); tag.setDouble("Capacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier());
        tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks); tag.setString("Condition", condition); return tag;
    }
    @Override public void acceptGuiState(NBTTagCompound tag) {
        if (tag.getInteger("ExtractCapacity") > 0) extract.setCapacity(tag.getInteger("ExtractCapacity"));
        if (tag.getInteger("WeedCapacity") > 0) weed.setCapacity(tag.getInteger("WeedCapacity"));
        extract.setFluid(tag.getInteger("Extract") > 0 ? new FluidStack(CropManager.GRIN_EXTRACT, tag.getInteger("Extract")) : null);
        weed.setFluid(tag.getInteger("Weed") > 0 ? new FluidStack(FluidName.weed_ex.getInstance(), tag.getInteger("Weed")) : null);
        energy.setCapacity(tag.getDouble("Capacity")); energy.setEnergyStored(tag.getDouble("EU")); energy.setSinkTier(Math.max(1, tag.getInteger("Tier")));
        progress = tag.getInteger("Work"); if (tag.getInteger("Duration") > 0) { clientDuration = tag.getInteger("Duration"); workTicks = clientDuration; } condition = tag.getString("Condition");
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); energy.writeToNBT(tag); tag.setTag("Inventory", inventory.serializeNBT());
        tag.setTag("Extract", extract.writeToNBT(new NBTTagCompound())); tag.setTag("Weed", weed.writeToNBT(new NBTTagCompound())); tag.setInteger("Work", progress); tag.setInteger("Duration", workTicks);
        NBTTagList drops = new NBTTagList();
        for (ItemStack stack : legacyDrops) drops.appendTag(stack.writeToNBT(new NBTTagCompound()));
        if (drops.tagCount() > 0) tag.setTag("LegacyDrops", drops);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); energy.readFromNBT(tag); double eu = energy.getEnergyStored();
        NBTTagCompound items = tag.getCompoundTag("Inventory").copy();
        legacyDrops.clear();
        NBTTagList savedDrops = tag.getTagList("LegacyDrops", 10);
        for (int i = 0; i < savedDrops.tagCount(); i++) { ItemStack stack = new ItemStack(savedDrops.getCompoundTagAt(i)); if (!stack.isEmpty()) legacyDrops.add(stack); }
        if (items.getInteger("Size") == 5) migrateFiveSlotInventory(items);
        else { items.setInteger("Size", 9); inventory.deserializeNBT(items); }
        extract.readFromNBT(tag.getCompoundTag("Extract")); weed.readFromNBT(tag.getCompoundTag("Weed"));
        resizeTank(extract, ManagerConfig.fabricatorExtractCapacity); resizeTank(weed, ManagerConfig.fabricatorWeedCapacity);
        workTicks = Math.max(1, tag.hasKey("Duration") ? tag.getInteger("Duration") : ManagerConfig.fabricatorDuration);
        progress = Math.max(0, Math.min(workTicks - 1, tag.getInteger("Work")));
        upgradesDirty = true; recalculateUpgrades();
        energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0);
        if (extract.getFluid() != null && extract.getFluid().getFluid() != CropManager.GRIN_EXTRACT) extract.setFluid(null);
    }
    /** Alpha3-6 layout: 0 unused, 1 redstone, 2 extract container, 3 Weed-Ex container, 4 output. */
    private void migrateFiveSlotInventory(NBTTagCompound items) {
        ItemStackHandler old = new ItemStackHandler(5); old.deserializeNBT(items);
        for (int slot = 0; slot < 4; slot++) inventory.setStackInSlot(slot, ItemStack.EMPTY);
        int[] target = {-1, REDSTONE, -1, BOTTLE_IN, CONTAINER_OUT};
        for (int slot = 0; slot < 5; slot++) {
            ItemStack stack = old.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (target[slot] >= 0) inventory.setStackInSlot(target[slot], stack.copy());
            else legacyDrops.add(stack.copy()); // never delete items whose old slot no longer exists
        }
    }
    @Override public void dropContents() {
        for (ItemStack stack : legacyDrops) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        legacyDrops.clear();
        dropHandler(inventory);
    }
}
