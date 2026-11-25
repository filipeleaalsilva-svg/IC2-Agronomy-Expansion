package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.tile.IEnergyStorage;
import ic2.api.upgrade.IUpgradableBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * TileEntity for {@link EuDishAntennaBlock}: draws EU from IC2 cables or adjacent
 * {@link IEnergyStorage} blocks and broadcasts power to {@link IUpgradableBlock} machines
 * within its radius that hold a {@link WirelessEuUpgrade} linked to this antenna.
 */
public final class EuDishAntennaTile extends FarmTile implements ITickable {
    public static final int MAX_RANGE_UPGRADES = 4;

    public int redstoneMode;
    public int clientLinkedCount;
    public double lastSentRate;
    private int scanCooldown;
    private int dirtyTicks;
    private final List<BlockPos> cachedReceivers = new ArrayList<>();

    public final BasicSink energy = createSink(
            Math.max(1000, ManagerConfig.dishEnergyCapacity),
            Math.max(1, ManagerConfig.dishSinkTier));

    public final ItemStackHandler inventory = new ItemStackHandler(MAX_RANGE_UPGRADES) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MonitorTile.isWirelessRange(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            scanCooldown = 0;
            markDirty();
        }
    };

    @Override
    public BasicSink energySink() {
        return energy;
    }

    public int rangeUpgrades() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (MonitorTile.isWirelessRange(inventory.getStackInSlot(i))) count++;
        }
        return Math.min(MAX_RANGE_UPGRADES, count);
    }

    public int radius() {
        return Math.max(4, ManagerConfig.dishBaseRadius + rangeUpgrades() * ManagerConfig.dishRadiusPerUpgrade);
    }

    public boolean redstoneAllows() {
        boolean powered = world != null && pos != null && world.isBlockLoaded(pos, false) && world.isBlockPowered(pos);
        return ManagerTile.redstoneAllows(redstoneMode, powered);
    }

    @Override
    public void onLoad() {
        if (world != null && !world.isRemote && ic2.api.energy.EnergyNet.instance != null) energy.onLoad();
    }

    @Override
    public void invalidate() {
        if (ic2.api.energy.EnergyNet.instance != null) energy.invalidate();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (ic2.api.energy.EnergyNet.instance != null) energy.onChunkUnload();
        super.onChunkUnload();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || pos == null) return;
        if (ic2.api.energy.EnergyNet.instance != null) energy.update();
        pullFromAdjacentStorage();

        if (!redstoneAllows()) {
            lastSentRate = 0;
            return;
        }

        if (--scanCooldown <= 0) {
            scanCooldown = Math.max(5, ManagerConfig.dishScanIntervalTicks);
            refreshReceiverCache();
        }

        distributeToReceivers();
    }

    void pullFromAdjacentStorage() {
        double free = energy.getFreeCapacity();
        if (free <= 0) return;
        for (EnumFacing face : EnumFacing.VALUES) {
            BlockPos adjPos = pos.offset(face);
            if (adjPos.getY() < 0 || adjPos.getY() >= world.getHeight() || !world.isBlockLoaded(adjPos, false)) continue;
            TileEntity adj = world.getTileEntity(adjPos);
            if (!(adj instanceof IEnergyStorage)) continue;
            IEnergyStorage storage = (IEnergyStorage) adj;
            int pull = (int) Math.min(free, Math.min(ManagerConfig.dishMaxTransferPerTick, storage.getStored()));
            if (pull > 0) {
                storage.addEnergy(-pull);
                energy.setEnergyStored(energy.getEnergyStored() + pull);
                adj.markDirty();
                free -= pull;
                if (free <= 0) break;
            }
        }
    }

    void refreshReceiverCache() {
        cachedReceivers.clear();
        IChunkProvider provider = world.getChunkProvider();
        if (provider == null) return;
        int r = radius();
        double maxDistSq = (double) r * r;
        int minCx = (pos.getX() - r) >> 4, maxCx = (pos.getX() + r) >> 4;
        int minCz = (pos.getZ() - r) >> 4, maxCz = (pos.getZ() + r) >> 4;
        int dim = world.provider.getDimension();

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                Chunk chunk = provider.getLoadedChunk(cx, cz);
                if (chunk == null) continue;
                for (TileEntity te : chunk.getTileEntityMap().values()) {
                    if (te == null || te == this || te.isInvalid() || te.getWorld() != world) continue;
                    BlockPos tp = te.getPos();
                    if (tp.distanceSq(pos) > maxDistSq) continue;
                    if (te instanceof IUpgradableBlock && hasCompatibleUpgrade(te, dim, pos)) {
                        cachedReceivers.add(tp.toImmutable());
                    }
                }
            }
        }
    }

    void distributeToReceivers() {
        double budget = Math.min(energy.getEnergyStored(), Math.max(1, ManagerConfig.dishMaxTransferPerTick));
        if (budget <= 0 || cachedReceivers.isEmpty()) {
            lastSentRate = 0;
            return;
        }
        int dim = world.provider.getDimension();
        int r = radius();
        double maxDistSq = (double) r * r;
        double sentTotal = 0;

        for (BlockPos targetPos : cachedReceivers) {
            if (budget <= 0) break;
            if (targetPos.distanceSq(pos) > maxDistSq || !world.isBlockLoaded(targetPos, false)) continue;
            TileEntity te = world.getTileEntity(targetPos);
            if (!(te instanceof IUpgradableBlock) || te.isInvalid()) continue;
            int count = countCompatibleUpgrades(te, dim, pos);
            if (count <= 0) continue;
            double perMachine = Math.min(budget, Math.max(1, ManagerConfig.monitorWirelessEuRatePerUpgrade * count));
            double accepted = WirelessEuUpgrade.injectEnergy((IUpgradableBlock) te, perMachine);
            if (accepted > 0) {
                energy.useEnergy(accepted);
                budget -= accepted;
                sentTotal += accepted;
            }
        }
        lastSentRate = sentTotal;
        if (sentTotal > 0 && ++dirtyTicks >= 20) {
            dirtyTicks = 0;
            markDirty();
        }
    }

    private static boolean hasCompatibleUpgrade(TileEntity te, int dim, BlockPos dishPos) {
        return countCompatibleUpgrades(te, dim, dishPos) > 0;
    }

    private static int countCompatibleUpgrades(TileEntity te, int dim, BlockPos dishPos) {
        IItemHandler handler = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY != null
                ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                : null;
        if (handler == null && te instanceof FarmTile) {
            EquipmentLayout layout = ((FarmTile) te).equipmentLayout();
            if (layout != null) handler = layout.inventory;
        }
        if (handler == null) return 0;
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (WirelessEuUpgrade.acceptsDish(s, dim, dishPos)) {
                total += Math.max(1, s.getCount());
            }
        }
        return total;
    }

    @Override
    public boolean control(int action, EntityPlayer player) {
        if (world == null || world.isRemote) return false;
        if (action == 1) {
            redstoneMode = (redstoneMode + 1) % 3;
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("EU", energy.getEnergyStored());
        tag.setDouble("Capacity", energy.getCapacity());
        tag.setInteger("Tier", energy.getSinkTier());
        tag.setInteger("Radius", radius());
        tag.setInteger("Receivers", cachedReceivers.size());
        tag.setInteger("Redstone", redstoneMode);
        tag.setDouble("SentRate", lastSentRate);
        return tag;
    }

    @Override
    public void acceptGuiState(NBTTagCompound tag) {
        energy.setCapacity(Math.max(1000, tag.getDouble("Capacity")));
        energy.setSinkTier(Math.max(1, tag.getInteger("Tier")));
        energy.setEnergyStored(tag.getDouble("EU"));
        clientRadius = tag.getInteger("Radius");
        clientLinkedCount = tag.getInteger("Receivers");
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("Redstone")));
        lastSentRate = tag.getDouble("SentRate");
    }

    public int clientRadius = ManagerConfig.dishBaseRadius;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        energy.writeToNBT(tag);
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setInteger("Redstone", redstoneMode);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy.readFromNBT(tag);
        NBTTagCompound items = tag.getCompoundTag("Inventory").copy();
        items.setInteger("Size", MAX_RANGE_UPGRADES);
        inventory.deserializeNBT(items);
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("Redstone")));
        double eu = energy.getEnergyStored();
        energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0);
    }

    @Override
    public void dropContents() {
        dropHandler(inventory);
    }
}
