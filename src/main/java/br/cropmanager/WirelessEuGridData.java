package br.cropmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * World-level storage for Flux-style Wi-Fi EU channels shared between
 * {@link EuWifiPlugTile}, {@link EuWifiPointTile}, and linked {@link WirelessEuUpgrade}s.
 */
public final class WirelessEuGridData extends WorldSavedData {
    public static final String DATA_NAME = CropManager.ID + "_wifi_eu_grid";
    public static final int MAX_CHANNEL = 999;

    public static final class Channel {
        public final int id;
        @Nullable public UUID owner;
        public String ownerName = "";
        public boolean isPrivate;
        public double storedEu;
        public int lastSourceDim;

        Channel(int id) {
            this.id = id;
        }
    }

    private final Map<Integer, Channel> channels = new HashMap<>();

    public WirelessEuGridData() {
        super(DATA_NAME);
    }

    public WirelessEuGridData(String name) {
        super(name);
    }

    public static WirelessEuGridData get(World world) {
        World root = world.getMinecraftServer() != null
                ? world.getMinecraftServer().getWorld(0)
                : world;
        if (root == null) root = world;
        MapStorage storage = root.getMapStorage();
        if (storage == null) return new WirelessEuGridData();
        WirelessEuGridData data = (WirelessEuGridData) storage.getOrLoadData(WirelessEuGridData.class, DATA_NAME);
        if (data == null) {
            data = new WirelessEuGridData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public static int clampChannel(int id) {
        return Math.max(0, Math.min(MAX_CHANNEL, id));
    }

    public synchronized Channel getOrCreate(int channelId, @Nullable UUID actorUuid, @Nullable String actorName) {
        int id = clampChannel(channelId);
        Channel ch = channels.get(id);
        if (ch == null) {
            ch = new Channel(id);
            ch.owner = actorUuid;
            ch.ownerName = actorName != null ? actorName : "";
            channels.put(id, ch);
            markDirty();
        }
        return ch;
    }

    @Nullable
    public synchronized Channel getIfPresent(int channelId) {
        return channels.get(clampChannel(channelId));
    }

    public synchronized boolean canAccess(int channelId, @Nullable UUID actorUuid) {
        Channel ch = channels.get(clampChannel(channelId));
        if (ch == null || !ch.isPrivate || ch.owner == null) return true;
        return ch.owner.equals(actorUuid);
    }

    public synchronized boolean setPrivacy(int channelId, boolean isPrivate,
            @Nullable UUID actorUuid, @Nullable String actorName) {
        Channel ch = getOrCreate(channelId, actorUuid, actorName);
        if (ch.owner != null && !ch.owner.equals(actorUuid)) return false;
        if (ch.owner == null && actorUuid != null) {
            ch.owner = actorUuid;
            ch.ownerName = actorName != null ? actorName : "";
        }
        ch.isPrivate = isPrivate;
        markDirty();
        return true;
    }

    synchronized void refundEu(int channelId, double amount) {
        if (amount <= 0 || !Double.isFinite(amount)) return;
        Channel ch = channels.get(clampChannel(channelId));
        if (ch == null) return;
        double cap = Math.max(1000, ManagerConfig.wifiChannelBufferEU);
        double next = Math.min(cap, ch.storedEu + amount);
        if (next > ch.storedEu) {
            ch.storedEu = next;
            markDirty();
        }
    }

    public synchronized double getStoredEu(int channelId) {
        Channel ch = channels.get(clampChannel(channelId));
        return ch == null ? 0.0 : ch.storedEu;
    }

    public synchronized double insertEu(int channelId, double amount, int sourceDim,
            @Nullable UUID actorUuid, @Nullable String actorName) {
        if (amount <= 0 || !Double.isFinite(amount)) return 0.0;
        if (!canAccess(channelId, actorUuid)) return 0.0;
        Channel ch = getOrCreate(channelId, actorUuid, actorName);
        double cap = Math.max(1000, ManagerConfig.wifiChannelBufferEU);
        double room = Math.max(0.0, cap - ch.storedEu);
        double accepted = Math.min(amount, room);
        if (accepted > 0) {
            ch.storedEu += accepted;
            ch.lastSourceDim = sourceDim;
            markDirty();
        }
        return accepted;
    }

    public synchronized double extractEu(int channelId, double maxDeliver, int targetDim,
            @Nullable UUID actorUuid) {
        if (maxDeliver <= 0 || !Double.isFinite(maxDeliver)) return 0.0;
        if (!canAccess(channelId, actorUuid)) return 0.0;
        Channel ch = channels.get(clampChannel(channelId));
        if (ch == null || ch.storedEu <= 0) return 0.0;

        double loss = (ch.lastSourceDim != targetDim)
                ? Math.max(0.0, Math.min(0.5, ManagerConfig.wifiCrossDimLossPercent / 100.0))
                : 0.0;
        double efficiency = 1.0 - loss;
        double maxFromBuffer = ch.storedEu * efficiency;
        double delivered = Math.min(maxDeliver, maxFromBuffer);
        if (delivered > 0) {
            double drained = Math.min(ch.storedEu, delivered / efficiency);
            ch.storedEu = Math.max(0.0, ch.storedEu - drained);
            markDirty();
        }
        return delivered;
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound nbt) {
        channels.clear();
        NBTTagList list = nbt.getTagList("Channels", 10);
        double cap = Math.max(1000, ManagerConfig.wifiChannelBufferEU);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int id = clampChannel(entry.getInteger("Id"));
            Channel ch = new Channel(id);
            if (entry.hasUniqueId("Owner")) ch.owner = entry.getUniqueId("Owner");
            ch.ownerName = entry.getString("OwnerName");
            ch.isPrivate = entry.getBoolean("Private");
            double eu = entry.getDouble("EU");
            ch.storedEu = Double.isFinite(eu) ? Math.max(0.0, Math.min(cap, eu)) : 0.0;
            ch.lastSourceDim = entry.getInteger("LastDim");
            channels.put(id, ch);
        }
    }

    @Override
    public synchronized NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Channel ch : channels.values()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("Id", ch.id);
            if (ch.owner != null) entry.setUniqueId("Owner", ch.owner);
            entry.setString("OwnerName", ch.ownerName);
            entry.setBoolean("Private", ch.isPrivate);
            entry.setDouble("EU", ch.storedEu);
            entry.setInteger("LastDim", ch.lastSourceDim);
            list.appendTag(entry);
        }
        compound.setTag("Channels", list);
        return compound;
    }
}
