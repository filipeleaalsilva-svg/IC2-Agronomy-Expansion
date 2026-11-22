package br.cropmanager.chest.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sends the placed chest's saved settings to the player viewing it. */
public final class ChestSettingsMessage implements IMessage {
    private int window;
    private NBTTagCompound tag = new NBTTagCompound();

    public ChestSettingsMessage() { }
    public ChestSettingsMessage(int window, NBTTagCompound tag) { this.window = window; this.tag = tag.copy(); }
    @Override public void toBytes(ByteBuf buffer) { buffer.writeInt(window); ByteBufUtils.writeTag(buffer, tag); }
    @Override public void fromBytes(ByteBuf buffer) { window = buffer.readInt(); tag = ByteBufUtils.readTag(buffer); }

    public static final class Handler implements IMessageHandler<ChestSettingsMessage, IMessage> {
        @Override public IMessage onMessage(ChestSettingsMessage message, MessageContext context) {
            br.cropmanager.CropManager.proxy.receiveChestSettings(message.window, message.tag);
            return null;
        }
    }
}
