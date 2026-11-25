package br.cropmanager;

import br.cropmanager.chest.inventory.PortableCropInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Canal de rede ({@link SimpleNetworkWrapper}) responsável pela sincronização cliente-servidor
 * de estados de máquina, ações de interface, leitura da sonda e buscas no Baú de Dados.
 */
public final class ManagerNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CropManager.ID);
    private ManagerNetwork() {}
    /** Registra todas as mensagens cliente-servidor e servidor-cliente do mod. */
    public static void init() {
        CHANNEL.registerMessage(SettingsHandler.class, Settings.class, 0, Side.SERVER);
        CHANNEL.registerMessage(StateHandler.class, State.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(ActionHandler.class, Action.class, 2, Side.SERVER);
        CHANNEL.registerMessage(ReadoutHandler.class, Readout.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(br.cropmanager.chest.network.ChestMessage.Handler.class,
                br.cropmanager.chest.network.ChestMessage.class, 4, Side.SERVER);
        CHANNEL.registerMessage(br.cropmanager.chest.network.ChestSettingsMessage.Handler.class,
                br.cropmanager.chest.network.ChestSettingsMessage.class, 5, Side.CLIENT);
        CHANNEL.registerMessage(br.cropmanager.chest.network.ChestViewMessage.Handler.class,
                br.cropmanager.chest.network.ChestViewMessage.class, 6, Side.CLIENT);
        CHANNEL.registerMessage(ToggleSeedPickup.Handler.class, ToggleSeedPickup.class, 7, Side.SERVER);
    }

    public static final class ToggleSeedPickup implements IMessage {
        @Override public void fromBytes(ByteBuf buffer) { }
        @Override public void toBytes(ByteBuf buffer) { }

        public static final class Handler implements IMessageHandler<ToggleSeedPickup, IMessage> {
            @Override public IMessage onMessage(ToggleSeedPickup message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    if (player.isSpectator() || !player.isEntityAlive()
                            || player.openContainer != player.inventoryContainer) return;
                    PortableCropInventory portable = PortableCropInventory.open(player, player.inventory.currentItem);
                    if (portable == null) portable = PortableCropInventory.open(player, 40);
                    if (portable == null) return;
                    boolean enabled = !portable.isPickupEnabled();
                    portable.setPickupEnabled(enabled);
                    player.sendStatusMessage(new TextComponentTranslation(enabled
                            ? "cropdatabasechest.pickup.tooltip_on" : "cropdatabasechest.pickup.tooltip_off"), true);
                });
                return null;
            }
        }
    }
    public static final class Settings implements IMessage {
        private BlockPos pos;
        private int window, control;
        public Settings() {}
        public Settings(ManagerTile tile, int window, int control) {
            this.pos = tile.getPos(); this.window = window; this.control = control;
        }
        @Override public void fromBytes(ByteBuf b) { pos = BlockPos.fromLong(b.readLong()); window = b.readInt(); control = b.readInt(); }
        @Override public void toBytes(ByteBuf b) { b.writeLong(pos.toLong()); b.writeInt(window); b.writeInt(control); }
    }
    public static final class SettingsHandler implements IMessageHandler<Settings, IMessage> {
        @Override public IMessage onMessage(Settings message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ManagerContainer) || player.openContainer.windowId != message.window) return;
                ManagerTile tile = ((ManagerContainer) player.openContainer).machine;
                if (tile.getWorld() == player.world && tile.getPos().equals(message.pos) && tile.usable(player)) {
                    int[] settings = message.control == 0
                            ? ManagerSettings.applyControl(message.control, tile.radius, tile.layers, tile.functions, ManagerConfig.maxRadius)
                            : null;
                    if (settings != null) tile.settings(settings[0], settings[1], settings[2]);
                    // Also returns unchanged authoritative settings when a request is rejected.
                    CHANNEL.sendTo(new State(tile.getPos(), player.openContainer.windowId, tile.guiState()), player);
                }
            });
            return null;
        }
    }
    public static final class Action implements IMessage {
        private BlockPos pos; private int window, action;
        public Action() {}
        public Action(FarmTile tile, int window, int action) { pos = tile.getPos(); this.window = window; this.action = action; }
        public void fromBytes(ByteBuf b) { pos = BlockPos.fromLong(b.readLong()); window = b.readInt(); action = b.readInt(); }
        public void toBytes(ByteBuf b) { b.writeLong(pos.toLong()); b.writeInt(window); b.writeInt(action); }
    }
    public static final class ActionHandler implements IMessageHandler<Action, IMessage> {
        public IMessage onMessage(Action message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                FarmTile tile = FarmTile.openMachine(player);
                if (tile != null && player.openContainer.windowId == message.window && tile.getPos().equals(message.pos) && tile.usable(player)) {
                    tile.control(message.action, player);
                    CHANNEL.sendTo(new State(tile.getPos(), message.window, tile.guiState()), player);
                }
            }); return null;
        }
    }
    public static final class Readout implements IMessage {
        public NBTTagCompound tag;
        public Readout() {}
        public Readout(NBTTagCompound tag) { this.tag = tag; }
        public void fromBytes(ByteBuf b) { tag = ByteBufUtils.readTag(b); }
        public void toBytes(ByteBuf b) { ByteBufUtils.writeTag(b, tag); }
    }
    public static final class ReadoutHandler implements IMessageHandler<Readout, IMessage> {
        public IMessage onMessage(Readout message, MessageContext context) { CropManager.proxy.showReadout(message.tag); return null; }
    }
    public static final class State implements IMessage {
        public BlockPos pos;
        public int window;
        public NBTTagCompound tag;
        public State() {}
        public State(BlockPos pos, int window, NBTTagCompound tag) { this.pos = pos; this.window = window; this.tag = tag; }
        @Override public void fromBytes(ByteBuf b) { pos = BlockPos.fromLong(b.readLong()); window = b.readInt(); tag = ByteBufUtils.readTag(b); }
        @Override public void toBytes(ByteBuf b) { b.writeLong(pos.toLong()); b.writeInt(window); ByteBufUtils.writeTag(b, tag); }
    }
    public static final class StateHandler implements IMessageHandler<State, IMessage> {
        @Override public IMessage onMessage(State message, MessageContext context) { CropManager.proxy.receiveState(message); return null; }
    }
}
