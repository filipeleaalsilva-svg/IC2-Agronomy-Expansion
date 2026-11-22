package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ref.FluidName;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class WeedExFabricatorTile extends FarmTile implements ITickable, IUpgradableBlock {
    public static final Fluid SPIDER_EXTRACT = getOrRegisterFluid("grin_extract",
            new ResourceLocation("cropmanager", "blocks/grin_extract_still"),
            new ResourceLocation("cropmanager", "blocks/grin_extract_flow"));
    public static final Fluid LIQUID_REDSTONE = getOrRegisterFluid("liquid_redstone",
            new ResourceLocation("cropmanager", "blocks/grin_extract_still"),
            new ResourceLocation("cropmanager", "blocks/grin_extract_flow"));
    private static Fluid FALLBACK_WEED_EX;

    private static Fluid getOrRegisterFluid(String name, ResourceLocation still, ResourceLocation flow) {
        Fluid existing = FluidRegistry.getFluid(name);
        if (existing != null) return existing;
        Fluid fluid = new Fluid(name, still, flow).setUnlocalizedName(name);
        FluidRegistry.registerFluid(fluid);
        return FluidRegistry.getFluid(name) != null ? FluidRegistry.getFluid(name) : fluid;
    }

    public static Fluid weedExFluid() {
        if (FluidName.weed_ex.hasInstance()) return FluidName.weed_ex.getInstance();
        Fluid reg = FluidRegistry.getFluid("ic2weed_ex");
        if (reg != null) return reg;
        if (FALLBACK_WEED_EX == null) {
            FALLBACK_WEED_EX = getOrRegisterFluid("ic2weed_ex",
                    new ResourceLocation("ic2", "blocks/fluid/weed_ex_still"),
                    new ResourceLocation("ic2", "blocks/fluid/weed_ex_flow"));
        }
        return FALLBACK_WEED_EX;
    }

    /** Slots: 0 Spider Eye, 1 Redstone, 2 Water container in, 3 Weed-Ex container in, 4 Container out, 5..8 IC2 upgrades, 9 Battery. */
    public static final int SPIDER_EYE = 0, REDSTONE = 1, WATER_IN = 2, BOTTLE_IN = 3, CONTAINER_OUT = 4;
    public static final int UPGRADE_START = 5, UPGRADE_END = 9, BATTERY = 9;

    public int progress, clientDuration = ManagerConfig.fabricatorDuration, workTicks = ManagerConfig.fabricatorDuration;
    public double euPerTick = ManagerConfig.fabricatorEuPerTick;
    public int waterBuffer1, waterBuffer2;
    private boolean upgradesDirty = true;
    public String condition = "weedex_ready";

    public final BasicSink energy = createSink(ManagerConfig.fabricatorEnergyCapacity, ManagerConfig.fabricatorSinkTier);
    @Override public BasicSink energySink() { return energy; }
    @Override public boolean isSupportMachine() { return true; }

    public final FluidTank spiderTank = new FluidTank(ManagerConfig.fabricatorSideTankCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && (fluid.getFluid() == FluidRegistry.WATER || fluid.getFluid() == SPIDER_EXTRACT);
        }
        @Override protected void onContentsChanged() { markDirty(); }
    };

    public final FluidTank redstoneTank = new FluidTank(ManagerConfig.fabricatorSideTankCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && (fluid.getFluid() == FluidRegistry.WATER || fluid.getFluid() == LIQUID_REDSTONE);
        }
        @Override protected void onContentsChanged() { markDirty(); }
    };

    public final FluidTank weed = new FluidTank(ManagerConfig.fabricatorWeedCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && fluid.getFluid() == weedExFluid();
        }
        @Override protected void onContentsChanged() { markDirty(); }
    };

    public final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && slot != CONTAINER_OUT && !isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == SPIDER_EYE) return stack.getItem() == Items.SPIDER_EYE;
            if (slot == REDSTONE) return stack.getItem() == Items.REDSTONE;
            if (slot == WATER_IN) return isWaterContainer(stack);
            if (slot == BOTTLE_IN) return isWeedExContainer(stack);
            if (slot == BATTERY) return isBattery(stack);
            return slot >= UPGRADE_START && slot < UPGRADE_END && ManagerTile.isIc2Upgrade(stack);
        }
        @Override protected void onContentsChanged(int slot) {
            if (slot >= UPGRADE_START && slot < UPGRADE_END) upgradesDirty = true;
            markDirty();
        }
    };

    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {
                tankProperty(spiderTank, true, false),
                tankProperty(redstoneTank, true, false),
                tankProperty(weed, false, true)
            };
        }
        @Override public int fill(FluidStack resource, boolean execute) {
            if (resource == null || resource.amount <= 0) return 0;
            if (resource.getFluid() == FluidRegistry.WATER) return fillWater(resource.amount, execute);
            if (resource.getFluid() == SPIDER_EXTRACT) return spiderTank.fill(resource, execute);
            if (resource.getFluid() == LIQUID_REDSTONE) return redstoneTank.fill(resource, execute);
            return 0;
        }
        @Override public FluidStack drain(FluidStack resource, boolean execute) {
            return resource != null && resource.getFluid() == weedExFluid() ? weed.drain(resource, execute) : null;
        }
        @Override public FluidStack drain(int amount, boolean execute) {
            return weed.drain(amount, execute);
        }
    };

    public static boolean isWaterContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidStack fluid = FluidUtil.getFluidContained(stack);
        return fluid != null && fluid.getFluid() == FluidRegistry.WATER && fluid.amount > 0;
    }

    public static boolean isWeedExContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidTank sample = new FluidTank(1000);
        sample.setFluid(new FluidStack(weedExFluid(), 1000));
        return FluidUtil.tryFillContainer(stack, sample, 1000, null, false).isSuccess();
    }

    private int sideWaterSpace(FluidTank tank, int buffer) {
        FluidStack current = tank.getFluid();
        if (current == null || current.amount == 0 || current.getFluid() == FluidRegistry.WATER) {
            return Math.max(0, tank.getCapacity() - tank.getFluidAmount() - buffer);
        }
        return Math.max(0, tank.getCapacity() - buffer);
    }

    private int sideEffectiveLevel(FluidTank tank, int buffer) {
        return tank.getFluidAmount() + buffer;
    }

    private int addWaterToSide(boolean side1, int maxAdd, boolean execute) {
        FluidTank tank = side1 ? spiderTank : redstoneTank;
        int buffer = side1 ? waterBuffer1 : waterBuffer2;
        int space = sideWaterSpace(tank, buffer);
        int accepted = Math.min(maxAdd, space);
        if (accepted <= 0 || !execute) return accepted;
        FluidStack current = tank.getFluid();
        if (current == null || current.amount == 0 || current.getFluid() == FluidRegistry.WATER) {
            tank.fill(new FluidStack(FluidRegistry.WATER, accepted), true);
        } else {
            if (side1) waterBuffer1 += accepted;
            else waterBuffer2 += accepted;
            markDirty();
        }
        return accepted;
    }

    /** Distributes incoming water between Side 1 and Side 2 in conversion-sized chunks so buckets/pipes never jam. */
    public int fillWater(int amount, boolean execute) {
        if (amount <= 0) return 0;
        int step = Math.max(1, ManagerConfig.fabricatorWaterPerConvert);
        int cap = spiderTank.getCapacity();
        int simTank1 = spiderTank.getFluidAmount(), simBuf1 = waterBuffer1;
        boolean water1 = spiderTank.getFluid() == null || spiderTank.getFluidAmount() == 0 || spiderTank.getFluid().getFluid() == FluidRegistry.WATER;
        int simTank2 = redstoneTank.getFluidAmount(), simBuf2 = waterBuffer2;
        boolean water2 = redstoneTank.getFluid() == null || redstoneTank.getFluidAmount() == 0 || redstoneTank.getFluid().getFluid() == FluidRegistry.WATER;

        int remaining = amount;
        int toSide1 = 0, toSide2 = 0;
        while (remaining > 0) {
            int space1 = water1 ? Math.max(0, cap - simTank1 - simBuf1) : Math.max(0, cap - simBuf1);
            int space2 = water2 ? Math.max(0, cap - simTank2 - simBuf2) : Math.max(0, cap - simBuf2);
            if (space1 <= 0 && space2 <= 0) break;
            int level1 = simTank1 + simBuf1;
            int level2 = simTank2 + simBuf2;
            boolean pick1;
            if (space1 <= 0) pick1 = false;
            else if (space2 <= 0) pick1 = true;
            else {
                int rem1 = level1 % step;
                int rem2 = level2 % step;
                if (rem1 != 0 && rem2 == 0) pick1 = true;
                else if (rem2 != 0 && rem1 == 0) pick1 = false;
                else pick1 = level1 <= level2;
            }
            if (pick1) {
                int needToNextStep = step - (level1 % step);
                int chunk = Math.min(remaining, Math.min(space1, needToNextStep));
                toSide1 += chunk;
                if (water1) simTank1 += chunk; else simBuf1 += chunk;
                remaining -= chunk;
            } else {
                int needToNextStep = step - (level2 % step);
                int chunk = Math.min(remaining, Math.min(space2, needToNextStep));
                toSide2 += chunk;
                if (water2) simTank2 += chunk; else simBuf2 += chunk;
                remaining -= chunk;
            }
        }
        if (execute) {
            if (toSide1 > 0) addWaterToSide(true, toSide1, true);
            if (toSide2 > 0) addWaterToSide(false, toSide2, true);
        }
        return toSide1 + toSide2;
    }

    private void emptyWaterContainer() {
        ItemStack source = inventory.getStackInSlot(WATER_IN);
        if (source.isEmpty()) return;
        ItemStack one = source.copy();
        one.setCount(1);
        int maxFill = ManagerConfig.fabricatorSideTankCapacity * 2;
        FluidActionResult preview = FluidUtil.tryEmptyContainer(one, fluids, maxFill, null, false);
        if (!preview.isSuccess() || !inventory.insertItem(CONTAINER_OUT, preview.getResult(), true).isEmpty()) return;
        FluidActionResult result = FluidUtil.tryEmptyContainer(one, fluids, maxFill, null, true);
        if (result.isSuccess()) {
            inventory.extractItem(WATER_IN, 1, false);
            inventory.insertItem(CONTAINER_OUT, result.getResult(), false);
        }
    }

    public void updateSideConversions() {
        waterBuffer1 = convertSide(spiderTank, waterBuffer1, SPIDER_EYE, SPIDER_EXTRACT);
        waterBuffer2 = convertSide(redstoneTank, waterBuffer2, REDSTONE, LIQUID_REDSTONE);
    }

    private int convertSide(FluidTank tank, int buffer, int slot, Fluid targetFluid) {
        int waterCost = ManagerConfig.fabricatorWaterPerConvert;
        if (tank.getFluidAmount() == 0 && buffer > 0) {
            int restore = Math.min(tank.getCapacity(), buffer);
            tank.setFluid(new FluidStack(FluidRegistry.WATER, restore));
            buffer -= restore;
            markDirty();
        }
        FluidStack current = tank.getFluid();
        if (current != null && current.amount > 0 && current.getFluid() == FluidRegistry.WATER) {
            int totalWater = current.amount + buffer;
            if (!inventory.getStackInSlot(slot).isEmpty() && totalWater >= waterCost) {
                inventory.extractItem(slot, 1, false);
                totalWater -= waterCost;
                buffer = totalWater;
                tank.setFluid(new FluidStack(targetFluid, Math.min(tank.getCapacity(), waterCost)));
                markDirty();
            }
        }
        current = tank.getFluid();
        if (current == null || current.amount == 0 || current.getFluid() == targetFluid) {
            while (!inventory.getStackInSlot(slot).isEmpty()
                    && buffer >= waterCost
                    && tank.getFluidAmount() + waterCost <= tank.getCapacity()) {
                inventory.extractItem(slot, 1, false);
                buffer -= waterCost;
                tank.fill(new FluidStack(targetFluid, waterCost), true);
                markDirty();
            }
        }
        return buffer;
    }

    @Override public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, net.minecraft.block.state.IBlockState oldState,
            net.minecraft.block.state.IBlockState newState) { return oldState.getBlock() != newState.getBlock(); }
    @Override public void onLoad() { if (!world.isRemote) { recalculateUpgrades(); energy.onLoad(); } }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }

    @Override public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateUpgrades();
        tickUpgrades(this, inventory, UPGRADE_START, UPGRADE_END);
        dischargeBattery(inventory, BATTERY, energy);
        energy.update();
        emptyWaterContainer();
        updateSideConversions();
        fillFluidContainer(inventory, BOTTLE_IN, CONTAINER_OUT, weed);
        boolean working = processRecipe();
        updateSideConversions();
        setActive(working);
    }

    private void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, inventory, UPGRADE_START, UPGRADE_END, energy, 1, ManagerConfig.fabricatorEnergyCapacity, ManagerConfig.fabricatorSinkTier);
        int next = Math.max(1, (int) Math.ceil(ManagerConfig.fabricatorDuration * multipliers[0]));
        progress = Math.min(next - 1, (int) ((long) progress * next / Math.max(1, workTicks)));
        workTicks = next;
        euPerTick = Math.ceil(ManagerConfig.fabricatorEuPerTick * multipliers[1]);
        upgradesDirty = false;
    }

    @Override public double getEnergy() { return energy.getEnergyStored(); }
    @Override public boolean useEnergy(double amount) { boolean used = energy.useEnergy(amount); if (used) markDirty(); return used; }
    @Override public Set<UpgradableProperty> getUpgradableProperties() { return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer); }

    boolean processRecipe() {
        if (upgradesDirty) recalculateUpgrades();
        updateSideConversions();
        int sideCost = ManagerConfig.fabricatorSidePerMix;
        int weedYield = ManagerConfig.fabricatorWeedPerRecipe;
        FluidStack sFluid = spiderTank.getFluid();
        if (sFluid == null || sFluid.getFluid() != SPIDER_EXTRACT || sFluid.amount < sideCost) {
            condition = "weedex_extract";
            return false;
        }
        FluidStack rFluid = redstoneTank.getFluid();
        if (rFluid == null || rFluid.getFluid() != LIQUID_REDSTONE || rFluid.amount < sideCost) {
            condition = "weedex_ingredients";
            return false;
        }
        if (weed.fill(new FluidStack(weedExFluid(), weedYield), false) < weedYield) {
            condition = "weedex_output";
            return false;
        }
        if (!energy.useEnergy(euPerTick)) {
            condition = "weedex_energy";
            return false;
        }
        condition = "weedex_working";
        progress++;
        markDirty();
        if (progress >= workTicks) {
            spiderTank.drain(sideCost, true);
            redstoneTank.drain(sideCost, true);
            weed.fill(new FluidStack(weedExFluid(), weedYield), true);
            progress = 0;
            condition = "weedex_ready";
            updateSideConversions();
            markDirty();
        }
        return true;
    }

    public int spiderType() {
        FluidStack f = spiderTank.getFluid();
        if (f == null || f.amount <= 0) return 0;
        return f.getFluid() == FluidRegistry.WATER ? 1 : 2;
    }

    public int redstoneType() {
        FluidStack f = redstoneTank.getFluid();
        if (f == null || f.amount <= 0) return 0;
        return f.getFluid() == FluidRegistry.WATER ? 1 : 2;
    }

    public IItemHandler itemsForFace(@Nullable EnumFacing face) {
        return new IItemHandler() {
            private void valid(int slot) { if (slot < 0 || slot >= inventory.getSlots()) throw new IndexOutOfBoundsException("slot " + slot); }
            @Override public int getSlots() { return inventory.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { valid(slot); return inventory.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                valid(slot);
                if (slot == CONTAINER_OUT || (slot >= UPGRADE_START && slot < UPGRADE_END) || !inventory.isItemValid(slot, stack)) return stack;
                return inventory.insertItem(slot, stack, simulate);
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                valid(slot);
                return slot == CONTAINER_OUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { valid(slot); return inventory.getSlotLimit(slot); }
        };
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
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("SpiderAmount", spiderTank.getFluidAmount());
        tag.setInteger("SpiderType", spiderType());
        tag.setInteger("SpiderCap", spiderTank.getCapacity());
        tag.setInteger("RedstoneAmount", redstoneTank.getFluidAmount());
        tag.setInteger("RedstoneType", redstoneType());
        tag.setInteger("RedstoneCap", redstoneTank.getCapacity());
        tag.setInteger("Weed", weed.getFluidAmount());
        tag.setInteger("WeedCapacity", weed.getCapacity());
        tag.setDouble("EU", energy.getEnergyStored());
        tag.setDouble("Capacity", energy.getCapacity());
        tag.setInteger("Tier", energy.getSinkTier());
        tag.setInteger("Work", progress);
        tag.setInteger("Duration", workTicks);
        tag.setString("Condition", condition);
        return tag;
    }

    @Override public void acceptGuiState(NBTTagCompound tag) {
        if (tag.getInteger("SpiderCap") > 0) spiderTank.setCapacity(tag.getInteger("SpiderCap"));
        if (tag.getInteger("RedstoneCap") > 0) redstoneTank.setCapacity(tag.getInteger("RedstoneCap"));
        if (tag.getInteger("WeedCapacity") > 0) weed.setCapacity(tag.getInteger("WeedCapacity"));
        int sAmt = tag.getInteger("SpiderAmount"), sType = tag.getInteger("SpiderType");
        spiderTank.setFluid(sAmt > 0 && sType > 0 ? new FluidStack(sType == 1 ? FluidRegistry.WATER : SPIDER_EXTRACT, sAmt) : null);
        int rAmt = tag.getInteger("RedstoneAmount"), rType = tag.getInteger("RedstoneType");
        redstoneTank.setFluid(rAmt > 0 && rType > 0 ? new FluidStack(rType == 1 ? FluidRegistry.WATER : LIQUID_REDSTONE, rAmt) : null);
        weed.setFluid(tag.getInteger("Weed") > 0 ? new FluidStack(weedExFluid(), tag.getInteger("Weed")) : null);
        energy.setCapacity(tag.getDouble("Capacity"));
        energy.setEnergyStored(tag.getDouble("EU"));
        energy.setSinkTier(Math.max(1, tag.getInteger("Tier")));
        progress = tag.getInteger("Work");
        if (tag.getInteger("Duration") > 0) {
            clientDuration = tag.getInteger("Duration");
            workTicks = clientDuration;
        }
        condition = tag.getString("Condition");
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        energy.writeToNBT(tag);
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setTag("SpiderTank", spiderTank.writeToNBT(new NBTTagCompound()));
        tag.setTag("RedstoneTank", redstoneTank.writeToNBT(new NBTTagCompound()));
        tag.setTag("Weed", weed.writeToNBT(new NBTTagCompound()));
        tag.setInteger("WaterBuffer1", waterBuffer1);
        tag.setInteger("WaterBuffer2", waterBuffer2);
        tag.setInteger("Work", progress);
        tag.setInteger("Duration", workTicks);
        return tag;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy.readFromNBT(tag);
        double eu = energy.getEnergyStored();
        NBTTagCompound items = tag.getCompoundTag("Inventory").copy();
        items.setInteger("Size", 10);
        inventory.deserializeNBT(items);
        if (tag.hasKey("SpiderTank")) spiderTank.readFromNBT(tag.getCompoundTag("SpiderTank"));
        else if (tag.hasKey("Extract")) spiderTank.readFromNBT(tag.getCompoundTag("Extract"));
        redstoneTank.readFromNBT(tag.getCompoundTag("RedstoneTank"));
        weed.readFromNBT(tag.getCompoundTag("Weed"));
        resizeTank(spiderTank, ManagerConfig.fabricatorSideTankCapacity);
        resizeTank(redstoneTank, ManagerConfig.fabricatorSideTankCapacity);
        resizeTank(weed, ManagerConfig.fabricatorWeedCapacity);
        waterBuffer1 = Math.max(0, Math.min(ManagerConfig.fabricatorSideTankCapacity, tag.getInteger("WaterBuffer1")));
        waterBuffer2 = Math.max(0, Math.min(ManagerConfig.fabricatorSideTankCapacity, tag.getInteger("WaterBuffer2")));
        workTicks = Math.max(1, tag.hasKey("Duration") ? tag.getInteger("Duration") : ManagerConfig.fabricatorDuration);
        progress = Math.max(0, Math.min(workTicks - 1, tag.getInteger("Work")));
        upgradesDirty = true;
        recalculateUpgrades();
        energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0);
    }

    @Override public void dropContents() {
        dropHandler(inventory);
    }
}
