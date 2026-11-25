package br.cropmanager.chest.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Compact view-mapping packet replacing 506 {@code SPacketWindowProperty} updates
 * whenever the Crop Database Chest view changes.
 */
public final class ChestViewMessage implements IMessage {
    private static final int MAX_SLOTS = 500;

    private int window;
    private int page;
    private int pageCount;
    private int occupied;
    private boolean pickup;
    private boolean favoritesOnly;
    private int revision;
    private int[] positions = new int[0];

    public ChestViewMessage() { }

    public ChestViewMessage(int window, int page, int pageCount, int occupied,
            boolean pickup, boolean favoritesOnly, int revision, int[] positions) {
        this.window = window;
        this.page = page;
        this.pageCount = pageCount;
        this.occupied = occupied;
        this.pickup = pickup;
        this.favoritesOnly = favoritesOnly;
        this.revision = revision;
        this.positions = positions != null ? positions.clone() : new int[0];
    }

    public int getWindow() { return window; }
    public int getPage() { return page; }
    public int getPageCount() { return pageCount; }
    public int getOccupied() { return occupied; }
    public boolean isPickup() { return pickup; }
    public boolean isFavoritesOnly() { return favoritesOnly; }
    public int getRevision() { return revision; }
    public int[] getPositions() { return positions; }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(page);
        buffer.writeInt(pageCount);
        buffer.writeInt(occupied);
        buffer.writeBoolean(pickup);
        buffer.writeBoolean(favoritesOnly);
        buffer.writeInt(revision);
        int len = Math.min(MAX_SLOTS, positions.length);
        buffer.writeShort(len);
        for (int i = 0; i < len; i++) {
            buffer.writeShort(positions[i]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        page = buffer.readInt();
        pageCount = buffer.readInt();
        occupied = buffer.readInt();
        pickup = buffer.readBoolean();
        favoritesOnly = buffer.readBoolean();
        revision = buffer.readInt();
        int len = Math.max(0, Math.min(MAX_SLOTS, buffer.readUnsignedShort()));
        positions = new int[len];
        for (int i = 0; i < len; i++) {
            positions[i] = buffer.readShort();
        }
    }

    public static final class Handler implements IMessageHandler<ChestViewMessage, IMessage> {
        @Override
        public IMessage onMessage(ChestViewMessage message, MessageContext context) {
            br.cropmanager.CropManager.proxy.receiveChestView(message);
            return null;
        }
    }
}
