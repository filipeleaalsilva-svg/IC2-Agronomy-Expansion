package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.core.block.machine.tileentity.TileEntityCropmatron;
import ic2.core.block.comp.Energy;
import ic2.core.ref.FluidName;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Entidade de bloco do Monitor Agrícola.
 *
 * <p>Exibe telemetria em tempo real de uma máquina vinculada ({@link ManagerTile}, {@link WeederTile}
 * ou {@link TileEntityCropmatron}), emite sinal redstone configurável (alarme/normal/teste), atua
 * como buffer de fluidos, fertilizante e colheita, e opcionalmente distribui EU sem fio via
 * upgrades Wireless EU para o alvo vinculado e máquinas de suporte próximas.
 */
public final class MonitorTile extends FarmTile implements ITickable {
    public static final int BASE_RANGE = 32, MAX_RANGE_UPGRADES = 4, BUFFER_CAPACITY = 16000, TRANSFER_PER_SECOND = 1000;
    public static final int HARVEST_SLOTS = 15, HARVEST_TRANSFER_PER_SECOND = 64;
    public static final int WIRELESS_EU_SLOT = 8, MAX_WIRELESS_EU_UPGRADES = 4, WIRELESS_EU_CAPACITY_PER_UPGRADE = 50000, WIRELESS_EU_RATE_PER_UPGRADE = 256, SUPPORT_WIRELESS_RADIUS = 16;
    private BlockPos linked;
    private int dimension, ticks;
    private long testUntil;
    private boolean upgradesDirty = true;
    public int outputMode = 1, signal, testRemaining;
    public boolean alarmActive;
    public NBTTagCompound snapshot = new NBTTagCompound();
    public String connection = "unlinked";
    public final BasicSink energy = new BasicSink(this, ManagerConfig.monitorWirelessEuCapacityPerUpgrade, 2) {
        @Override public double getDemandedEnergy() {
            return wirelessEuUpgrades() > 0 ? super.getDemandedEnergy() : 0;
        }
        @Override public double injectEnergy(EnumFacing direction, double amount, double voltage) {
            if (wirelessEuUpgrades() <= 0) return amount;
            double accepted = Math.min(Math.max(0, amount), getFreeCapacity());
            setEnergyStored(getEnergyStored() + accepted);
            if (accepted > 0) markDirty();
            return amount - accepted;
        }
    };
    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && !isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            return slot < 4 ? isWirelessRange(stack) : slot < 7 ? ManagerTile.isFertilizer(stack) : slot == 7 ? isHarvestTransfer(stack) : slot == WIRELESS_EU_SLOT && isWirelessEu(stack);
        }
        @Override public int getSlotLimit(int slot) {
            return slot == WIRELESS_EU_SLOT ? MAX_WIRELESS_EU_UPGRADES : slot < 4 || slot == 7 ? 1 : 64;
        }
        @Override protected void onContentsChanged(int slot) {
            if (slot == WIRELESS_EU_SLOT) upgradesDirty = true;
            markDirty();
        }
    };
    public final ItemStackHandler harvestInventory = new ItemStackHandler(HARVEST_SLOTS) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    public final FluidTank water = new FluidTank(ManagerConfig.monitorTankCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) { return fluid != null && fluid.getFluid() == FluidRegistry.WATER; }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public final FluidTank weed = new FluidTank(ManagerConfig.monitorTankCapacity) {
        @Override public boolean canFillFluidType(FluidStack fluid) { return fluid != null && fluid.getFluid() == FluidName.weed_ex.getInstance(); }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() { return new IFluidTankProperties[] {properties(water), properties(weed)}; }
        @Override public int fill(FluidStack resource, boolean execute) {
            if (resource == null) return 0;
            return resource.getFluid() == FluidRegistry.WATER ? water.fill(resource, execute)
                    : resource.getFluid() == FluidName.weed_ex.getInstance() ? weed.fill(resource, execute) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, boolean execute) {
            if (resource == null) return null;
            return resource.getFluid() == FluidRegistry.WATER ? water.drain(resource, execute)
                    : resource.getFluid() == FluidName.weed_ex.getInstance() ? weed.drain(resource, execute) : null;
        }
        @Override public FluidStack drain(int amount, boolean execute) { return water.getFluidAmount() > 0 ? water.drain(amount, execute) : weed.drain(amount, execute); }
    };
    private final IItemHandler automationItems = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots() + harvestInventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException("slot " + slot);
            return slot < inventory.getSlots() ? inventory.getStackInSlot(slot) : harvestInventory.getStackInSlot(slot - inventory.getSlots());
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException("slot " + slot);
            return slot >= 4 && slot <= 6 ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException("slot " + slot);
            if (slot >= inventory.getSlots()) return harvestInventory.extractItem(slot - inventory.getSlots(), amount, simulate);
            return slot >= 4 && slot <= 6 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return slot < inventory.getSlots() ? inventory.getSlotLimit(slot) : harvestInventory.getSlotLimit(slot - inventory.getSlots()); }
    };

    private static IFluidTankProperties properties(FluidTank tank) {
        return new IFluidTankProperties() {
            @Override public FluidStack getContents() { return tank.getFluid() == null ? null : tank.getFluid().copy(); }
            @Override public int getCapacity() { return tank.getCapacity(); }
            @Override public boolean canFill() { return true; }
            @Override public boolean canDrain() { return tank.getFluidAmount() > 0; }
            @Override public boolean canFillFluidType(FluidStack fluid) { return tank.canFillFluidType(fluid); }
            @Override public boolean canDrainFluidType(FluidStack fluid) { return tank.getFluid() != null && fluid != null && fluid.isFluidEqual(tank.getFluid()); }
        };
    }
    public static boolean isWirelessRange(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == CropManager.WIRELESS_RANGE; }
    public static boolean isHarvestTransfer(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == CropManager.HARVEST_TRANSFER; }
    public static boolean isWirelessEu(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == CropManager.WIRELESS_EU; }
    public int wirelessEuUpgrades() {
        ItemStack stack = inventory.getStackInSlot(WIRELESS_EU_SLOT);
        return isWirelessEu(stack) ? Math.min(MAX_WIRELESS_EU_UPGRADES, stack.getCount()) : 0;
    }
    public int wirelessEuRate() { return wirelessEuUpgrades() * ManagerConfig.monitorWirelessEuRatePerUpgrade; }
    public double wirelessEuCapacity() {
        int upgrades = wirelessEuUpgrades();
        return upgrades > 0 ? (double) upgrades * ManagerConfig.monitorWirelessEuCapacityPerUpgrade : 0;
    }
    public void recalculateWirelessEu() {
        int upgrades = wirelessEuUpgrades();
        double cap = upgrades > 0 ? (double) upgrades * ManagerConfig.monitorWirelessEuCapacityPerUpgrade : ManagerConfig.monitorWirelessEuCapacityPerUpgrade;
        int tier = Math.max(1, 1 + upgrades);
        if (tier != energy.getSinkTier() && world != null && !world.isRemote) energy.invalidate();
        energy.setSinkTier(tier);
        energy.setCapacity(cap);
        energy.setEnergyStored(upgrades > 0 ? Math.min(cap, energy.getEnergyStored()) : 0);
        upgradesDirty = false;
    }
    public double transferWirelessEnergyTo(BasicSink targetSink, double maxAmount) {
        if (targetSink == null || maxAmount <= 0 || wirelessEuUpgrades() <= 0) return 0;
        double current = energy.getEnergyStored();
        double available = Math.min(current, maxAmount);
        double free = targetSink.getFreeCapacity();
        double sent = Math.min(available, Math.max(0, free));
        if (sent <= 0) return 0;
        energy.setEnergyStored(current - sent);
        targetSink.setEnergyStored(targetSink.getEnergyStored() + sent);
        markDirty();
        return sent;
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, face);
    }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluids);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automationItems);
        return super.getCapability(capability, face);
    }
    public int connectionRange() {
        int upgrades = 0;
        for (int slot = 0; slot < MAX_RANGE_UPGRADES; slot++) if (isWirelessRange(inventory.getStackInSlot(slot))) upgrades++;
        return ManagerConfig.monitorBaseRange << Math.min(upgrades, ManagerConfig.monitorMaxRangeUpgrades);
    }
    static boolean supported(TileEntity tile) { return tile instanceof ManagerTile || tile instanceof TileEntityCropmatron || tile instanceof WeederTile; }
    /**
     * Links this Agricultural Monitor to a target machine in the given dimension.
     *
     * <p><b>Wireless Connection Rules:</b></p>
     * <ul>
     *   <li><b>Dimension:</b> Stored in {@code dimension}. Must match the monitor's dimension;
     *       cross-dimension connections are reported as {@code "dimension"} and suspend transfers.</li>
     *   <li><b>Range:</b> Determined by {@link #connectionRange()}, starting at 32 blocks and doubling
     *       with each Wireless Range upgrade up to 512 blocks. Exceeding distance reports {@code "distance"}.</li>
     *   <li><b>Chunk Loading:</b> Requires the target chunk to be loaded ({@code world.isBlockLoaded(linked, false)}).
     *       If unloaded, status is {@code "unloaded"} and no chunks are force-loaded.</li>
     *   <li><b>Target & Broken Block:</b> The target must be a supported machine ({@link ManagerTile},
     *       {@link TileEntityCropmatron}, or {@link WeederTile}). If the block is broken or replaced,
     *       the connection safely reverts to {@code "unlinked"} without throwing exceptions.</li>
     * </ul>
     */
    public void link(BlockPos target, int dim) { linked = target.toImmutable(); dimension = dim; markDirty(); refresh(); }
    @Override public boolean shouldRefresh(net.minecraft.world.World w, BlockPos p, net.minecraft.block.state.IBlockState a, net.minecraft.block.state.IBlockState b) { return a.getBlock() != b.getBlock(); }
    @Override public void onLoad() {
        if (!world.isRemote) {
            recalculateWirelessEu();
            energy.onLoad();
            refresh();
        }
    }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }
    @Override public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateWirelessEu();
        energy.update();
        transferWirelessEnergy();
        if (testUntil > 0 && world.getTotalWorldTime() >= testUntil) { testUntil = 0; refresh(); }
        if (++ticks >= 20) { ticks = 0; refresh(); transferResources(); }
    }
    private void transferWirelessEnergy() {
        int upgrades = wirelessEuUpgrades();
        if (upgrades <= 0 || energy.getEnergyStored() <= 0) return;
        double remaining = Math.min(energy.getEnergyStored(), wirelessEuRate());
        if (linked != null && dimension == world.provider.getDimension()
                && pos.distanceSq(linked) <= (double) connectionRange() * connectionRange()
                && world.isBlockLoaded(linked, false)) {
            TileEntity target = world.getTileEntity(linked);
            if (target instanceof ManagerTile) {
                ManagerTile manager = (ManagerTile) target;
                double sent = transferWirelessEnergyTo(manager.energy, remaining);
                if (sent > 0) { manager.markDirty(); remaining -= sent; }
            } else if (target instanceof TileEntityCropmatron) {
                Energy matronEnergy = ((TileEntityCropmatron) target).getComponent(Energy.class);
                if (matronEnergy != null) {
                    double offer = Math.min(remaining, Math.max(0, matronEnergy.getCapacity() - matronEnergy.getEnergy()));
                    if (offer > 0 && energy.useEnergy(offer)) {
                        matronEnergy.addEnergy(offer);
                        target.markDirty();
                        markDirty();
                        remaining -= offer;
                    }
                }
            }
        }
        if (remaining <= 0) return;
        double maxDistSq = (double) ManagerConfig.monitorSupportWirelessRadius * ManagerConfig.monitorSupportWirelessRadius;
        for (TileEntity te : new java.util.ArrayList<>(world.loadedTileEntityList)) {
            if (remaining <= 0) break;
            if (te == null || te == this || te.isInvalid() || te.getWorld() != world || te.getPos().distanceSq(pos) > maxDistSq) continue;
            if (!(te instanceof FarmTile) || !((FarmTile) te).isSupportMachine()) continue;
            BasicSink sink = ((FarmTile) te).energySink();
            if (sink != null) {
                double sent = transferWirelessEnergyTo(sink, remaining);
                if (sent > 0) { te.markDirty(); remaining -= sent; }
            }
        }
    }
    static NBTTagCompound readTarget(TileEntity tile) {
        if (tile instanceof ManagerTile) {
            ManagerTile m = (ManagerTile)tile; NBTTagCompound s = m.guiState(); s.setBoolean("Advanced", m.advanced);
            int count = 0; for (int i = 0; i < 3; i++) count += m.inventory.getStackInSlot(i).getCount();
            s.setInteger("Fertilizer", count); s.setString("Target", "manager"); return s;
        }
        if(tile instanceof WeederTile){
            WeederTile w=(WeederTile)tile;NBTTagCompound s=w.guiState();s.setString("Target","weeder");s.setString("State",!w.enabled?"manual_off":w.paused?"paused":w.gas.getFluidAmount()==0?"no_biogas":"running");return s;
        }
        TileEntityCropmatron matron = (TileEntityCropmatron)tile;
        NBTTagCompound s = new NBTTagCompound(); Energy energy = matron.getComponent(Energy.class);
        s.setString("Target", "cropmatron"); s.setDouble("EU", matron.getEnergy()); s.setDouble("Capacity", energy.getCapacity());
        s.setInteger("Water", matron.getWaterTank().getFluidAmount()); s.setInteger("Weed", matron.getExTank().getFluidAmount());
        s.setInteger("TankCapacity", matron.getWaterTank().getCapacity()); s.setInteger("WeedCapacity", matron.getExTank().getCapacity());
        int count = 0; for (int i = 0; i < matron.fertilizerSlot.size(); i++) count += matron.fertilizerSlot.get(i).getCount();
        s.setInteger("Fertilizer", count); s.setBoolean("LowEnergy", matron.getEnergy() < 1);
        s.setInteger("Status", (count == 0 ? 2 : 0) | (s.getInteger("Water") == 0 ? 4 : 0) | (s.getInteger("Weed") == 0 ? 8 : 0));
        return s;
    }
    void refresh() {
        snapshot = new NBTTagCompound(); connection = "unlinked";
        if (linked != null) {
            if (dimension != world.provider.getDimension()) connection = "dimension";
            else if (pos.distanceSq(linked) > (double)connectionRange() * connectionRange()) connection = "distance";
            else if (!world.isBlockLoaded(linked, false)) connection = "unloaded";
            else {
                TileEntity tile = world.getTileEntity(linked);
                if (supported(tile)) {
                    connection = "connected"; snapshot = readTarget(tile);
                    if (tile instanceof ManagerTile) appendHarvestItems((ManagerTile)tile);
                }
                else connection = "removed";
            }
        }
        alarmActive = AlarmLogic.active(connection, snapshot, 63) || "connected".equals(connection) && snapshot.getCompoundTag("Automation").getBoolean("Stopped") || "weeder".equals(snapshot.getString("Target")) && snapshot.getInteger("Gas")==0;
        int next = testUntil > world.getTotalWorldTime() ? 15 : outputMode == 1 && alarmActive || outputMode == 2 && "connected".equals(connection) && !alarmActive && (!snapshot.hasKey("Enabled") || snapshot.getBoolean("Enabled") && !snapshot.getBoolean("Paused")) ? 15 : 0;
        boolean changed = signal != next; signal = next;
        setActive(alarmActive || signal > 0);
        if (changed) {
            markDirty();
            for (EnumFacing face : EnumFacing.values()) {
                BlockPos neighbor = pos.offset(face);
                if (world.isBlockLoaded(neighbor, false)) world.neighborChanged(neighbor, CropManager.MONITOR, pos);
            }
        }
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
    }
    private void transferResources() {
        if (!"connected".equals(connection) || linked == null) return;
        TileEntity tile = world.getTileEntity(linked);
        if (tile instanceof TileEntityCropmatron) {
            TileEntityCropmatron matron = (TileEntityCropmatron) tile;
            transfer(water, matron.getWaterTank());
            transfer(weed, matron.getExTank());
            IItemHandler matronItems = matron.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
            if (matronItems != null) {
                int moved = 0;
                while (moved < ManagerConfig.monitorFertilizerTransferPerSecond) {
                    int sourceSlot = -1;
                    for (int slot = 4; slot < 7; slot++) if (ManagerTile.isFertilizer(inventory.getStackInSlot(slot))) { sourceSlot = slot; break; }
                    if (sourceSlot < 0) break;
                    ItemStack one = inventory.getStackInSlot(sourceSlot).copy(); one.setCount(1);
                    if (ItemHandlerHelper.insertItemStacked(matronItems, one, false).isEmpty()) {
                        inventory.extractItem(sourceSlot, 1, false);
                        matron.markDirty();
                        moved++;
                    } else break;
                }
            }
            return;
        }
        if (!(tile instanceof ManagerTile)) return;
        ManagerTile manager = (ManagerTile)tile;
        transfer(water, manager.water);
        transfer(weed, manager.weed);
        transferHarvest(manager);
        int moved = 0;
        while (moved < ManagerConfig.monitorFertilizerTransferPerSecond) {
            int sourceSlot = -1;
            for (int slot = 4; slot < 7; slot++) if (ManagerTile.isFertilizer(inventory.getStackInSlot(slot))) { sourceSlot = slot; break; }
            if (sourceSlot < 0) break;
            boolean inserted = false;
            for (int destination = 0; destination < 3; destination++) {
                ItemStack one = inventory.getStackInSlot(sourceSlot).copy(); one.setCount(1);
                if (manager.inventory.insertItem(destination, one, true).isEmpty()) {
                    manager.inventory.insertItem(destination, one, false); inventory.extractItem(sourceSlot, 1, false);
                    moved++; inserted = true; break;
                }
            }
            if (!inserted) break;
        }
    }
    private void appendHarvestItems(ManagerTile manager) {
        NBTTagList items = new NBTTagList();
        NBTTagList managerSlots = new NBTTagList();
        for (int slot = ManagerTile.OUTPUT_START; slot < ManagerTile.OUTPUT_END; slot++) {
            ItemStack stack = manager.inventory.getStackInSlot(slot);
            appendHarvestItem(items, stack, 0);
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setInteger("Slot", slot - ManagerTile.OUTPUT_START);
            if (!stack.isEmpty()) stack.writeToNBT(slotTag);
            managerSlots.appendTag(slotTag);
        }
        for (int slot = 0; slot < harvestInventory.getSlots(); slot++) appendHarvestItem(items, harvestInventory.getStackInSlot(slot), 1);
        snapshot.setTag("HarvestItems", items);
        snapshot.setTag("ManagerSlots", managerSlots);
    }
    private static void appendHarvestItem(NBTTagList items, ItemStack stack, int source) {
        if (stack.isEmpty()) return;
        NBTTagCompound entry = new NBTTagCompound(); entry.setInteger("Source", source);
        entry.setTag("Stack", stack.writeToNBT(new NBTTagCompound())); items.appendTag(entry);
    }
    private void transferHarvest(ManagerTile manager) {
        if (!isHarvestTransfer(inventory.getStackInSlot(7))) return;
        int remaining = ManagerConfig.monitorHarvestTransferPerSecond;
        for (int source = ManagerTile.OUTPUT_START; source < ManagerTile.OUTPUT_END && remaining > 0; source++) {
            ItemStack stack = manager.inventory.getStackInSlot(source);
            if (stack.isEmpty()) continue;
            ItemStack offered = stack.copy(); offered.setCount(Math.min(remaining, offered.getCount()));
            ItemStack rejected = ItemHandlerHelper.insertItemStacked(harvestInventory, offered, true);
            int accepted = offered.getCount() - rejected.getCount();
            if (accepted <= 0) continue;
            ItemStack extracted = manager.inventory.extractItem(source, accepted, false);
            ItemStack rest = ItemHandlerHelper.insertItemStacked(harvestInventory, extracted, false);
            if (!rest.isEmpty()) manager.inventory.insertItem(source, rest, false);
            remaining -= accepted - rest.getCount();
        }
    }
    private static void transfer(FluidTank source, FluidTank destination) {
        FluidStack sample = source.drain(ManagerConfig.monitorFluidTransferPerSecond, false);
        if (sample == null || sample.amount <= 0) return;
        int accepted = destination.fill(sample, false);
        if (accepted <= 0) return;
        source.drain(accepted, true);
        destination.fill(new FluidStack(sample, accepted), true);
    }
    @Override public boolean control(int action, EntityPlayer player) {
        if (action == 13 || action >= 200 && action <= 204 || action >= 220 && action <= 230) {
            if (linked == null || dimension != world.provider.getDimension() || pos.distanceSq(linked) > (double)connectionRange() * connectionRange() || !world.isBlockLoaded(linked, false)) return false;
            TileEntity target = world.getTileEntity(linked);
            boolean changed;
            if(target instanceof ManagerTile)changed=((ManagerTile)target).control(action,player);
            else if(target instanceof WeederTile && (action==200 || action==13))changed=((WeederTile)target).control(action,player);
            else return false;
            refresh(); return changed;
        }
        if (action == 146) outputMode = (outputMode + 1) % 3;
        else if (action == 147) testUntil = world.getTotalWorldTime() + ManagerConfig.monitorTestSignalTicks;
        else if (action == 148) { linked = null; testUntil = 0; }
        else return false;
        markDirty(); refresh(); return true;
    }
    @Override public NBTTagCompound guiState() {
        NBTTagCompound t = new NBTTagCompound(); t.setBoolean("Alarm", alarmActive); t.setInteger("OutputMode", outputMode); t.setInteger("Signal", signal);
        t.setInteger("Water", water.getFluidAmount()); t.setInteger("Weed", weed.getFluidAmount()); t.setInteger("TankCapacity", water.getCapacity()); t.setInteger("BufferCapacity", water.getCapacity()); t.setInteger("Range", connectionRange());
        t.setDouble("WirelessEU", wirelessEuUpgrades() > 0 ? energy.getEnergyStored() : 0);
        t.setDouble("WirelessEUCapacity", wirelessEuCapacity());
        t.setInteger("WirelessEURate", wirelessEuRate());
        t.setInteger("WirelessEUTier", energy.getSinkTier());
        t.setInteger("Test", world != null && !world.isRemote ? (int)Math.max(0, testUntil - world.getTotalWorldTime()) : testRemaining);
        if (linked != null) { t.setLong("Linked", linked.toLong()); t.setInteger("Dimension", dimension); }
        t.setString("Connection", connection); t.setTag("Snapshot", snapshot.copy()); return t;
    }
    @Override public void acceptGuiState(NBTTagCompound t) {
        alarmActive = t.getBoolean("Alarm"); outputMode = t.getInteger("OutputMode"); signal = t.getInteger("Signal"); testRemaining = t.getInteger("Test");
        int bufCap = t.getInteger("BufferCapacity");
        if (bufCap > 0) { water.setCapacity(bufCap); weed.setCapacity(bufCap); }
        water.setFluid(t.getInteger("Water") > 0 ? new FluidStack(FluidRegistry.WATER, t.getInteger("Water")) : null);
        weed.setFluid(t.getInteger("Weed") > 0 ? new FluidStack(FluidName.weed_ex.getInstance(), t.getInteger("Weed")) : null);
        double euCap = t.getDouble("WirelessEUCapacity");
        energy.setCapacity(euCap > 0 ? euCap : ManagerConfig.monitorWirelessEuCapacityPerUpgrade);
        energy.setEnergyStored(euCap > 0 ? Math.min(euCap, t.getDouble("WirelessEU")) : 0);
        energy.setSinkTier(Math.max(1, t.getInteger("WirelessEUTier")));
        linked = t.hasKey("Linked") ? BlockPos.fromLong(t.getLong("Linked")) : null; dimension = t.getInteger("Dimension"); connection = t.getString("Connection"); snapshot = t.getCompoundTag("Snapshot");
    }
    public String targetCoordinates() { return linked == null ? "-" : linked.getX() + ", " + linked.getY() + ", " + linked.getZ(); }
    @Override public void dropContents() {
        dropHandler(inventory);
        dropHandler(harvestInventory);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t); energy.writeToNBT(t); t.setInteger("OutputMode", outputMode); t.setTag("Inventory", inventory.serializeNBT()); t.setTag("HarvestInventory", harvestInventory.serializeNBT());
        t.setTag("Water", water.writeToNBT(new NBTTagCompound())); t.setTag("Weed", weed.writeToNBT(new NBTTagCompound()));
        if (linked != null) { t.setLong("Linked", linked.toLong()); t.setInteger("Dimension", dimension); } return t;
    }
    @Override public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t); outputMode = t.hasKey("OutputMode") ? Math.max(0, Math.min(2, t.getInteger("OutputMode"))) : 1;
        NBTTagCompound items = t.getCompoundTag("Inventory").copy(); items.setInteger("Size", 9); inventory.deserializeNBT(items);
        recalculateWirelessEu();
        energy.readFromNBT(t);
        double savedEu = energy.getEnergyStored();
        energy.setEnergyStored(wirelessEuUpgrades() > 0 && Double.isFinite(savedEu) ? Math.max(0, Math.min(energy.getCapacity(), savedEu)) : 0);
        NBTTagCompound harvest = t.getCompoundTag("HarvestInventory").copy(); harvest.setInteger("Size", HARVEST_SLOTS); harvestInventory.deserializeNBT(harvest);
        water.readFromNBT(t.getCompoundTag("Water")); weed.readFromNBT(t.getCompoundTag("Weed"));
        resizeTank(water, ManagerConfig.monitorTankCapacity); resizeTank(weed, ManagerConfig.monitorTankCapacity);
        linked = t.hasKey("Linked") ? BlockPos.fromLong(t.getLong("Linked")) : null; dimension = t.getInteger("Dimension");
    }
    @Override public NBTTagCompound getUpdateTag() { NBTTagCompound t = super.getUpdateTag(); t.setTag("Display", guiState()); return t; }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) { handleUpdateTag(packet.getNbtCompound()); }
    @Override public void handleUpdateTag(NBTTagCompound t) { readFromNBT(t); acceptGuiState(t.getCompoundTag("Display")); }
}
