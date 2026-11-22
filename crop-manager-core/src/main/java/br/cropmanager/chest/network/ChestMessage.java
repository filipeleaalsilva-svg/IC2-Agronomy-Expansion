package br.cropmanager.chest.network;

import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Search requests and view acknowledgements use the currently open window only. */
public final class ChestMessage implements IMessage {
    private int window, revision;
    private boolean search;
    private int favoriteSlot = -1;
    public static ChestMessage favorite(int window,int slot,int revision) { ChestMessage message=new ChestMessage(window,revision);message.favoriteSlot=slot;return message; }
    private String query = "";
    private final Set<String> translatedMatches = new HashSet<>();

    public ChestMessage() { }
    public ChestMessage(int window, int revision) { this.window = window; this.revision = revision; }
    public ChestMessage(int window, String query, Set<String> matches) {
        this.window = window;
        search = true;
        this.query = query;
        translatedMatches.addAll(matches);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(window);
        buf.writeBoolean(search);
        buf.writeInt(revision);
        if (search) {
            ByteBufUtils.writeUTF8String(buf, query);
            buf.writeShort(translatedMatches.size());
            for (String key : translatedMatches) ByteBufUtils.writeUTF8String(buf, key);
        }
        buf.writeInt(favoriteSlot);
    }
    @Override public void fromBytes(ByteBuf buf) {
        window = buf.readInt();
        search = buf.readBoolean();
        revision = buf.readInt();
        if (search) {
            query = ByteBufUtils.readUTF8String(buf);
            if (query.length() > 64) throw new IllegalArgumentException("Search too long");
            int count = buf.readUnsignedShort();
            if (count > 500) throw new IllegalArgumentException("Too many matching crops");
            for (int i = 0; i < count; i++) {
                String key = ByteBufUtils.readUTF8String(buf);
                if (key.length() > 256) throw new IllegalArgumentException("Crop identity too long");
                translatedMatches.add(key);
            }
        }
        favoriteSlot = buf.readInt();
    }
    public static final class Handler implements IMessageHandler<ChestMessage, IMessage> {
        @Override public IMessage onMessage(ChestMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerCropDatabaseChest
                        && player.openContainer.windowId == message.window) {
                    ContainerCropDatabaseChest chest = (ContainerCropDatabaseChest) player.openContainer;
                    if(message.favoriteSlot>=0) chest.toggleFavorite(player,message.favoriteSlot,message.revision);
                    else if (message.search) chest.setSearch(player, message.query, message.translatedMatches);
                    else chest.acknowledgeView(message.revision);
                }
            });
            return null;
        }
    }
}
