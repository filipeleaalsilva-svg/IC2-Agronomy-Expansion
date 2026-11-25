package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Wi-Fi EU Plug ({@code IEnergySink}): receives EU from adjacent IC2 cables/generators
 * and feeds it into the selected {@link WirelessEuGridData} channel.
 */
public final class EuWifiPlugTile extends FarmTile implements ITickable {
    public int channel;
    @Nullable public UUID ownerUuid;
    public String ownerName = "";
    public double lastInputRate;
    private int dirtyTicks;

    public final BasicSink energy = createSink(
            Math.max(2048, ManagerConfig.wifiChannelBufferEU),
            Math.max(1, ManagerConfig.wifiPlugSinkTier));

    @Override
    public BasicSink energySink() {
        return energy;
    }

    public void setOwner(EntityPlayer player) {
        this.ownerUuid = player.getUniqueID();
        this.ownerName = player.getName();
        markDirty();
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
        if (world == null || world.isRemote) return;
        if (ic2.api.energy.EnergyNet.instance != null) energy.update();
        double stored = energy.getEnergyStored();
        if (stored > 0) {
            WirelessEuGridData grid = WirelessEuGridData.get(world);
            double sent = grid.insertEu(channel, stored, world.provider.getDimension(), ownerUuid, ownerName);
            lastInputRate = sent;
            if (sent > 0) {
                energy.useEnergy(sent);
                if (++dirtyTicks >= 20) {
                    dirtyTicks = 0;
                    markDirty();
                }
            }
        } else {
            lastInputRate = 0;
        }
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
            WirelessEuGridData grid = WirelessEuGridData.get(world);
            WirelessEuGridData.Channel ch = grid.getIfPresent(channel);
            boolean nextPrivacy = ch == null || !ch.isPrivate;
            if (grid.setPrivacy(channel, nextPrivacy, player.getUniqueID(), player.getName())) {
                markDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Channel", channel);
        tag.setDouble("InputRate", lastInputRate);
        tag.setInteger("Tier", energy.getSinkTier());
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
        lastInputRate = tag.getDouble("InputRate");
        energy.setSinkTier(Math.max(1, tag.getInteger("Tier")));
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
        energy.writeToNBT(tag);
        tag.setInteger("Channel", channel);
        if (ownerUuid != null) tag.setUniqueId("Owner", ownerUuid);
        tag.setString("OwnerName", ownerName);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy.readFromNBT(tag);
        channel = WirelessEuGridData.clampChannel(tag.getInteger("Channel"));
        ownerUuid = tag.hasUniqueId("Owner") ? tag.getUniqueId("Owner") : null;
        ownerName = tag.getString("OwnerName");
        double eu = energy.getEnergyStored();
        energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0);
    }
}
