package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.upgrade.IEnergyStorageUpgrade;
import ic2.api.upgrade.IProcessingUpgrade;
import ic2.api.upgrade.ITransformerUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Classe base para entidades de bloco (TileEntities) das máquinas agrícolas do mod.
 *
 * <p>Padroniza validação de acesso do jogador no servidor, manipulação segura de recipientes
 * de fluidos, aplicação de upgrades IC2 ({@link IUpgradableBlock}) e descarregamento de baterias.
 */
public abstract class FarmTile extends TileEntity {
    public abstract NBTTagCompound guiState();
    public abstract void acceptGuiState(NBTTagCompound tag);
    public boolean control(int action, EntityPlayer player) { return false; }
    public void dropContents() {}
    @Nullable public BasicSink energySink() { return null; }
    public boolean isSupportMachine() { return false; }

    protected BasicSink createSink(double capacity, int tier) {
        return new BasicSink(this, capacity, tier) {
            @Override public double injectEnergy(EnumFacing side, double amount, double voltage) {
                double accepted = Math.min(Math.max(0, amount), getFreeCapacity());
                setEnergyStored(getEnergyStored() + accepted);
                if (accepted > 0) markDirty();
                return amount - accepted;
            }
        };
    }

    protected void dropHandler(ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    protected static void emptyFluidContainer(ItemStackHandler inv, int inSlot, int outSlot, FluidTank tank) {
        ItemStack source = inv.getStackInSlot(inSlot);
        if (source.isEmpty()) return;
        ItemStack one = source.copy(); one.setCount(1);
        FluidActionResult preview = FluidUtil.tryEmptyContainer(one, tank, tank.getCapacity(), null, false);
        if (!preview.isSuccess() || !inv.insertItem(outSlot, preview.getResult(), true).isEmpty()) return;
        FluidActionResult result = FluidUtil.tryEmptyContainer(one, tank, tank.getCapacity(), null, true);
        if (result.isSuccess()) {
            inv.extractItem(inSlot, 1, false);
            inv.insertItem(outSlot, result.getResult(), false);
        }
    }

    protected static void fillFluidContainer(ItemStackHandler inv, int inSlot, int outSlot, FluidTank tank) {
        ItemStack source = inv.getStackInSlot(inSlot);
        if (source.isEmpty() || tank.getFluidAmount() == 0) return;
        ItemStack one = source.copy(); one.setCount(1);
        FluidActionResult preview = FluidUtil.tryFillContainer(one, tank, Integer.MAX_VALUE, null, false);
        if (!preview.isSuccess() || !inv.insertItem(outSlot, preview.getResult(), true).isEmpty()) return;
        FluidActionResult result = FluidUtil.tryFillContainer(one, tank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            inv.extractItem(inSlot, 1, false);
            inv.insertItem(outSlot, result.getResult(), false);
        }
    }

    protected static IFluidTankProperties tankProperty(FluidTank tank, boolean canFill, boolean canDrain) {
        return new IFluidTankProperties() {
            @Override public FluidStack getContents() { return tank.getFluid() == null ? null : tank.getFluid().copy(); }
            @Override public int getCapacity() { return tank.getCapacity(); }
            @Override public boolean canFill() { return canFill; }
            @Override public boolean canDrain() { return canDrain; }
            @Override public boolean canFillFluidType(FluidStack fluid) { return canFill && fluid != null && tank.canFillFluidType(fluid); }
            @Override public boolean canDrainFluidType(FluidStack fluid) { return canDrain && tank.getFluid() != null && fluid != null && fluid.isFluidEqual(tank.getFluid()); }
        };
    }
    /**
     * Garante que o jogador esteja no mesmo mundo, com o chunk carregado e dentro do alcance padrão
     * de interação (8 blocos) antes de aceitar alterações enviadas por pacotes de rede.
     */
    public boolean usable(EntityPlayer player) {
        return !isInvalid() && player.world == world && world.isBlockLoaded(pos, false)
                && world.getTileEntity(pos) == this && player.getDistanceSq(pos) <= 64;
    }
    @Override public void markDirty() {
        if (world != null && !world.isRemote && world.isBlockLoaded(pos, false)) world.markChunkDirty(pos, this);
    }
    protected void setActive(boolean active) {
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ManagerBlock) || state.getValue(ManagerBlock.ACTIVE) == active) return;
        world.setBlockState(pos, state.withProperty(ManagerBlock.ACTIVE, active), 18);
        // Vanilla observer notifications read unloaded neighbors unless checked here.
        for (net.minecraft.util.EnumFacing face : net.minecraft.util.EnumFacing.values()) {
            net.minecraft.util.math.BlockPos neighbor = pos.offset(face);
            if (world.isBlockLoaded(neighbor, false)) world.observedNeighborChanged(neighbor, state.getBlock(), pos);
        }
    }
    public static FarmTile openMachine(EntityPlayer player) {
        if (player.openContainer instanceof ManagerContainer) return ((ManagerContainer)player.openContainer).machine;
        if (player.openContainer instanceof EquipmentContainer) return ((EquipmentContainer)player.openContainer).machine;
        return null;
    }

    /** Computes [timeMultiplier, demandMultiplier] and applies storage/tier upgrades to a BasicSink. */
    protected double[] applyIc2Upgrades(IUpgradableBlock machine, ItemStackHandler inv, int start, int end,
            BasicSink sink, double baseTime, double baseStorage, int baseTier) {
        double time = baseTime, demand = 1, storage = baseStorage;
        int tier = baseTier;
        for (int slot = start; slot < end; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!ManagerTile.isIc2Upgrade(stack)) continue;
            IUpgradeItem item = (IUpgradeItem) stack.getItem();
            int count = stack.getCount();
            if (item instanceof IProcessingUpgrade) {
                IProcessingUpgrade upgrade = (IProcessingUpgrade) item;
                time *= Math.pow(upgrade.getProcessTimeMultiplier(stack, machine), count);
                demand *= Math.pow(upgrade.getEnergyDemandMultiplier(stack, machine), count);
            }
            if (item instanceof IEnergyStorageUpgrade)
                storage += (double) ((IEnergyStorageUpgrade) item).getExtraEnergyStorage(stack, machine) * count;
            if (item instanceof ITransformerUpgrade)
                tier += ((ITransformerUpgrade) item).getExtraTier(stack, machine) * count;
        }
        if (sink.getSinkTier() != tier && world != null && !world.isRemote) sink.invalidate();
        sink.setSinkTier(tier);
        sink.setCapacity(storage);
        sink.setEnergyStored(Math.min(storage, sink.getEnergyStored()));
        return new double[] {time, demand};
    }

    /** Returns true if the item is an IC2 electric item usable in a machine battery slot. */
    protected static boolean isBattery(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ic2.api.item.IElectricItem;
    }

    /** Discharges EU from a battery slot into the machine's BasicSink. */
    protected void dischargeBattery(ItemStackHandler inv, int slot, BasicSink sink) {
        ItemStack stack = inv.getStackInSlot(slot);
        if (!isBattery(stack)) return;
        double free = sink.getFreeCapacity();
        if (free <= 0) return;
        double pulled = ic2.api.item.ElectricItem.manager.discharge(stack, free, sink.getSinkTier(), false, true, false);
        if (pulled > 0) {
            sink.setEnergyStored(sink.getEnergyStored() + pulled);
            markDirty();
        }
    }

    /** Ticks any custom IUpgradeItem (e.g. WirelessEuUpgrade) installed in the machine's upgrade slots. */
    protected static void tickUpgrades(IUpgradableBlock machine, ItemStackHandler inv, int start, int end) {
        for (int slot = start; slot < end; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof IUpgradeItem)
                ((IUpgradeItem) stack.getItem()).onTick(stack, machine);
        }
    }

    static void resizeTank(FluidTank tank, int capacity) {
        tank.setCapacity(capacity);
        FluidStack fluid = tank.getFluid();
        if (fluid != null && fluid.amount > capacity) tank.setFluid(new FluidStack(fluid, capacity));
    }

    static void configureSink(BasicSink sink, double capacity, int tier) {
        sink.setCapacity(capacity);
        sink.setSinkTier(tier);
        double stored = sink.getEnergyStored();
        sink.setEnergyStored(Double.isFinite(stored) ? Math.max(0, Math.min(capacity, stored)) : 0);
    }
}
