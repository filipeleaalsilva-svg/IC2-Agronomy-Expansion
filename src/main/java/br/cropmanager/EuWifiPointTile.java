package br.cropmanager;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * Wi-Fi EU Point ({@link IEnergySource}): draws EU from the selected {@link WirelessEuGridData}
 * channel and outputs it into adjacent IC2 cables or machines with configurable tier, rate limit,
 * and redstone control.
 */
public final class EuWifiPointTile extends FarmTile implements ITickable, IEnergySource {
    public int channel;
    public int outputTier = 1;
    public int rateLimit = 32;
    /** 0 = always active, 1 = active with redstone, 2 = active without redstone. */
    public int redstoneMode;
    @Nullable public UUID ownerUuid;
    public String ownerName = "";
    public double lastOutputRate;
    public double storedEu;
    public double capacity = 4096.0;
    private boolean addedToEnet;
    private int dirtyTicks;

    public static int maxRateForTier(int tier) {
        switch (Math.max(1, Math.min(4, tier))) {
            case 1: return 32;
            case 2: return 128;
            case 3: return 512;
            default: return 2048;
        }
    }

    public void setOwner(EntityPlayer player) {
        this.ownerUuid = player.getUniqueID();
        this.ownerName = player.getName();
        markDirty();
    }

    public int effectiveRate() {
        return Math.max(1, Math.min(maxRateForTier(outputTier), rateLimit));
    }

    public boolean redstoneAllows() {
        boolean powered = world != null && pos != null && world.isBlockLoaded(pos, false) && world.isBlockPowered(pos);
        return ManagerTile.redstoneAllows(redstoneMode, powered);
    }

    private void syncTier() {
        outputTier = Math.max(1, Math.min(ManagerConfig.wifiPointMaxOutputTier, outputTier));
        rateLimit = Math.max(1, Math.min(maxRateForTier(outputTier), rateLimit));
        capacity = Math.max(4096.0, maxRateForTier(outputTier) * 4.0);
        storedEu = Math.min(storedEu, capacity);
    }

    @Override
    public void onLoad() {
        syncTier();
        if (world != null && !world.isRemote && !addedToEnet && EnergyNet.instance != null) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            addedToEnet = true;
        }
    }

    @Override
    public void invalidate() {
        if (addedToEnet && world != null && !world.isRemote && EnergyNet.instance != null) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnet = false;
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (addedToEnet && world != null && !world.isRemote && EnergyNet.instance != null) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnet = false;
        }
        super.onChunkUnload();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        if (!addedToEnet && EnergyNet.instance != null) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            addedToEnet = true;
        }
        if (redstoneAllows()) {
            double room = Math.max(0.0, capacity - storedEu);
            double want = Math.min(room, effectiveRate());
            if (want > 0) {
                WirelessEuGridData grid = WirelessEuGridData.get(world);
                double pulled = grid.extractEu(channel, want, world.provider.getDimension(), ownerUuid);
                lastOutputRate = pulled;
                if (pulled > 0) {
                    storedEu = Math.min(capacity, storedEu + pulled);
                    if (++dirtyTicks >= 20) {
                        dirtyTicks = 0;
                        markDirty();
                    }
                }
            } else {
                lastOutputRate = 0;
            }
        } else {
            lastOutputRate = 0;
        }
    }

    @Override
    public double getOfferedEnergy() {
        return redstoneAllows() ? Math.min(storedEu, effectiveRate()) : 0.0;
    }

    @Override
    public void drawEnergy(double amount) {
        if (Double.isFinite(amount) && amount > 0) {
            storedEu = Math.max(0.0, storedEu - amount);
            markDirty();
        }
    }

    @Override
    public int getSourceTier() {
        return outputTier;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
        return true;
    }

    @Override
    public boolean control(int action, EntityPlayer player) {
        if (world == null || world.isRemote) return false;
        if (action >= 1000 && action <= 1000 + WirelessEuGridData.MAX_CHANNEL) {
            int next = action - 1000;
            if (!WirelessEuGridData.get(world).canAccess(next, player.getUniqueID())) return false;
            channel = next;
            if (ownerUuid == null) setOwner(player);
            markDirty();
            return true;
        }
        if (action == 1) {
            outputTier = outputTier >= ManagerConfig.wifiPointMaxOutputTier ? 1 : outputTier + 1;
            rateLimit = maxRateForTier(outputTier);
            syncTier();
            if (addedToEnet && world != null && !world.isRemote && EnergyNet.instance != null) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
                MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            }
            markDirty();
            return true;
        }
        if (action == 2) {
            redstoneMode = (redstoneMode + 1) % 3;
            markDirty();
            return true;
        }
        if (action == 3) {
            int max = maxRateForTier(outputTier);
            int step = Math.max(8, max / 4);
            rateLimit = rateLimit <= step ? max : Math.max(step, rateLimit - step);
            markDirty();
            return true;
        }
        if (action == 4) {
            int max = maxRateForTier(outputTier);
            int step = Math.max(8, max / 4);
            rateLimit = rateLimit >= max ? step : Math.min(max, rateLimit + step);
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Channel", channel);
        tag.setInteger("Tier", outputTier);
        tag.setInteger("Rate", effectiveRate());
        tag.setInteger("Redstone", redstoneMode);
        tag.setDouble("OutputRate", lastOutputRate);
        if (world != null && !world.isRemote) {
            WirelessEuGridData grid = WirelessEuGridData.get(world);
            WirelessEuGridData.Channel ch = grid.getIfPresent(channel);
            tag.setDouble("GridEU", ch == null ? 0 : ch.storedEu);
            tag.setDouble("GridCap", ManagerConfig.wifiChannelBufferEU);
            tag.setBoolean("Private", ch != null && ch.isPrivate);
        }
        return tag;
    }

    @Override
    public void acceptGuiState(NBTTagCompound tag) {
        channel = WirelessEuGridData.clampChannel(tag.getInteger("Channel"));
        outputTier = Math.max(1, Math.min(4, tag.getInteger("Tier")));
        rateLimit = Math.max(1, tag.getInteger("Rate"));
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("Redstone")));
        lastOutputRate = tag.getDouble("OutputRate");
        clientGridEu = tag.getDouble("GridEU");
        clientGridCap = Math.max(1, tag.getDouble("GridCap"));
        clientPrivate = tag.getBoolean("Private");
    }

    public double clientGridEu;
    public double clientGridCap = ManagerConfig.wifiChannelBufferEU;
    public boolean clientPrivate;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("StoredEU", storedEu);
        tag.setInteger("Channel", channel);
        tag.setInteger("OutputTier", outputTier);
        tag.setInteger("RateLimit", rateLimit);
        tag.setInteger("Redstone", redstoneMode);
        if (ownerUuid != null) tag.setUniqueId("Owner", ownerUuid);
        tag.setString("OwnerName", ownerName);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        double eu = tag.getDouble("StoredEU");
        channel = WirelessEuGridData.clampChannel(tag.getInteger("Channel"));
        outputTier = tag.hasKey("OutputTier") ? Math.max(1, Math.min(4, tag.getInteger("OutputTier"))) : 1;
        rateLimit = tag.hasKey("RateLimit") ? tag.getInteger("RateLimit") : maxRateForTier(outputTier);
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("Redstone")));
        ownerUuid = tag.hasUniqueId("Owner") ? tag.getUniqueId("Owner") : null;
        ownerName = tag.getString("OwnerName");
        syncTier();
        storedEu = Double.isFinite(eu) ? Math.max(0.0, Math.min(capacity, eu)) : 0.0;
    }
}
