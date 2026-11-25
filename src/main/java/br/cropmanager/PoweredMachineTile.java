package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Shared base for Tier-1 EU support machines with IC2 upgrades, battery discharge,
 * progress scaling, and automated item capability exposure.
 */
abstract class PoweredMachineTile extends FarmTile implements ITickable, IUpgradableBlock {
    public int progress;
    public int workTicks;
    public double euPerTick;
    public String condition;
    protected boolean upgradesDirty = true;
    public final BasicSink energy;

    protected PoweredMachineTile(int energyCapacity, int sinkTier, int initialDuration,
            double initialEuPerTick, String initialCondition) {
        this.energy = createSink(energyCapacity, sinkTier);
        this.workTicks = initialDuration;
        this.euPerTick = initialEuPerTick;
        this.condition = initialCondition;
    }

    protected abstract ItemStackHandler machineInventory();

    protected abstract int upgradeStartSlot();

    protected abstract int upgradeEndSlot();

    protected abstract int batterySlot();

    protected abstract int baseDuration();

    protected abstract double baseEuPerTick();

    protected abstract int baseEnergyCapacity();

    protected abstract int baseSinkTier();

    protected abstract boolean processMachineTick();

    @Override
    public BasicSink energySink() {
        return energy;
    }

    @Override
    public boolean isSupportMachine() {
        return true;
    }

    @Override
    public double getEnergy() {
        return energy.getEnergyStored();
    }

    @Override
    public boolean useEnergy(double amount) {
        boolean used = energy.useEnergy(amount);
        if (used) markDirty();
        return used;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void onLoad() {
        if (!world.isRemote) {
            recalculateUpgrades();
            energy.onLoad();
        }
    }

    @Override
    public void invalidate() {
        energy.invalidate();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        energy.onChunkUnload();
        super.onChunkUnload();
    }

    @Override
    public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateUpgrades();
        ItemStackHandler inv = machineInventory();
        tickUpgrades(this, inv, upgradeStartSlot(), upgradeEndSlot());
        dischargeBattery(inv, batterySlot(), energy);
        energy.update();
        setActive(processMachineTick());
    }

    protected void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, machineInventory(), upgradeStartSlot(), upgradeEndSlot(),
                energy, 1, baseEnergyCapacity(), baseSinkTier());
        int next = Math.max(1, (int) Math.ceil(baseDuration() * multipliers[0]));
        progress = Math.min(next - 1, (int) ((long) progress * next / Math.max(1, workTicks)));
        workTicks = next;
        euPerTick = Math.ceil(baseEuPerTick() * multipliers[1]);
        upgradesDirty = false;
    }

    protected static IItemHandler exposedItems(final ItemStackHandler inv, final int inputSlots, final int outputSlot) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return inv.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inv.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot < 0 || slot >= inputSlots || !inv.isItemValid(slot, stack)) return stack;
                return inv.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == outputSlot ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return inv.getSlotLimit(slot);
            }
        };
    }

    protected void writePoweredNBT(NBTTagCompound tag) {
        energy.writeToNBT(tag);
        tag.setTag("Inventory", machineInventory().serializeNBT());
        tag.setInteger("Work", progress);
        tag.setInteger("Duration", workTicks);
    }

    protected void readPoweredNBT(NBTTagCompound tag) {
        energy.readFromNBT(tag);
        double savedEu = energy.getEnergyStored();
        ItemStackHandler inv = machineInventory();
        NBTTagCompound items = tag.getCompoundTag("Inventory").copy();
        items.setInteger("Size", inv.getSlots());
        inv.deserializeNBT(items);
        workTicks = Math.max(1, tag.hasKey("Duration") ? tag.getInteger("Duration") : baseDuration());
        progress = Math.max(0, Math.min(workTicks - 1, tag.getInteger("Work")));
        upgradesDirty = true;
        recalculateUpgrades();
        energy.setEnergyStored(Double.isFinite(savedEu) ? Math.max(0, Math.min(energy.getCapacity(), savedEu)) : 0);
    }

    @Override
    public void dropContents() {
        dropHandler(machineInventory());
    }
}
