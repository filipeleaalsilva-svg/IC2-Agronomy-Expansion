package br.cropmanager;

import java.util.Objects;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Non-ticking casing endpoint that forwards formed-structure capabilities to its controller. */
public final class ReservoirCasingTile extends TileEntity implements IEnergySink {
    private int controllerDx;
    private int controllerDy;
    private int controllerDz;
    private boolean linked;
    private boolean registered;
    private final IFluidHandler[] sides = new IFluidHandler[6];
    private final IFluidHandler unsided = createPort(null);

    public ReservoirCasingTile() {
        for (EnumFacing side : EnumFacing.values()) sides[side.getIndex()] = createPort(side);
    }

    private IFluidHandler createPort(@Nullable EnumFacing side) {
        return new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() {
            IFluidHandler handler = activeFluidHandler(side);
            return handler == null ? new IFluidTankProperties[0] : handler.getTankProperties();
        }
        @Override public int fill(FluidStack resource, boolean execute) {
            IFluidHandler handler = activeFluidHandler(side);
            return handler == null ? 0 : handler.fill(resource, execute);
        }
        @Override public FluidStack drain(FluidStack resource, boolean execute) {
            IFluidHandler handler = activeFluidHandler(side);
            return handler == null ? null : handler.drain(resource, execute);
        }
        @Override public FluidStack drain(int amount, boolean execute) {
            IFluidHandler handler = activeFluidHandler(side);
            return handler == null ? null : handler.drain(amount, execute);
        }
        };
    }

    @Override public void onLoad() {
        super.onLoad();
        if (!world.isRemote && !registered) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            registered = true;
        }
    }

    @Override public void invalidate() {
        unloadEnergy();
        super.invalidate();
    }

    @Override public void onChunkUnload() {
        unloadEnergy();
        super.onChunkUnload();
    }

    void refreshEnergyConnection() {
        if (registered) {
            unloadEnergy();
            onLoad();
        }
    }

    private void unloadEnergy() {
        if (registered) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            registered = false;
        }
    }

    void setController(BlockPos masterPos) {
        if (world == null || masterPos == null) return;
        controllerDx = masterPos.getX() - pos.getX();
        controllerDy = masterPos.getY() - pos.getY();
        controllerDz = masterPos.getZ() - pos.getZ();
        linked = true;
        markDirty();
        syncBlock();
    }

    private BlockPos controllerPos() {
        return linked ? pos.add(controllerDx, controllerDy, controllerDz) : null;
    }

    void clearController(BlockPos expected) {
        if (!expected.equals(controllerPos())) return;
        linked = false;
        markDirty();
        syncBlock();
    }

    @Nullable public AgriculturalReservoirTile controller() {
        BlockPos masterPos = controllerPos();
        if (masterPos == null || world == null || !world.isBlockLoaded(masterPos, false)) return null;
        TileEntity master = world.getTileEntity(masterPos);
        if (!(master instanceof AgriculturalReservoirTile) || master.isInvalid()) return null;
        AgriculturalReservoirTile reservoir = (AgriculturalReservoirTile) master;
        return reservoir.containsCasing(pos) ? reservoir : null;
    }

    ReservoirCasingBlock.Part renderPart() {
        AgriculturalReservoirTile master = controller();
        return master == null ? ReservoirCasingBlock.Part.CASING : master.renderPart(pos);
    }

    boolean isFormed() {
        AgriculturalReservoirTile master = controller();
        return master != null && master.formed;
    }

    int comparatorSignal() {
        AgriculturalReservoirTile master = controller();
        return master != null && master.formed ? master.comparatorSignal() : 0;
    }

    void notifyControllerBroken() {
        AgriculturalReservoirTile master = controller();
        if (master != null) master.casingBroken(pos);
        linked = false;
    }

    @Override public double getDemandedEnergy() {
        AgriculturalReservoirTile master = controller();
        return master != null && master.formed ? master.energy.getFreeCapacity() : 0;
    }
    @Override public int getSinkTier() {
        AgriculturalReservoirTile master = controller();
        return master == null ? AgriculturalReservoirTile.ENERGY_TIER : master.energy.getSinkTier();
    }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) { return true; }
    @Override public double injectEnergy(EnumFacing side, double amount, double voltage) {
        AgriculturalReservoirTile master = controller();
        return master == null || !master.formed ? amount : master.injectCableEnergy(amount);
    }

    @Nullable private IFluidHandler activeFluidHandler(@Nullable EnumFacing side) {
        if (isInvalid() || world == null || !world.isBlockLoaded(pos, false)
                || world.getTileEntity(pos) != this) return null;
        AgriculturalReservoirTile master = controller();
        return master == null || !master.isExterior(pos, side) ? null : master.fluidAccess(side);
    }

    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && activeFluidHandler(facing) != null
                || super.hasCapability(capability, facing);
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && activeFluidHandler(facing) != null)
            return (T) (facing == null ? unsided : sides[facing.getIndex()]);
        return super.getCapability(capability, facing);
    }

    private void syncBlock() {
        if (world == null || world.isRemote || !world.isBlockLoaded(pos, false)) return;
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("ReservoirLinked", linked);
        tag.setInteger("ControllerDx", controllerDx);
        tag.setInteger("ControllerDy", controllerDy);
        tag.setInteger("ControllerDz", controllerDz);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        linked = tag.getBoolean("ReservoirLinked");
        controllerDx = tag.getInteger("ControllerDx");
        controllerDy = tag.getInteger("ControllerDy");
        controllerDz = tag.getInteger("ControllerDz");
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 1, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
        BlockPos previousController = controllerPos();
        readFromNBT(packet.getNbtCompound());
        if (world != null && !Objects.equals(previousController, controllerPos()))
            world.markBlockRangeForRenderUpdate(pos, pos);
    }
    @Override public void handleUpdateTag(NBTTagCompound tag) { readFromNBT(tag); }
    @Override public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos,
            IBlockState oldState, IBlockState newState) { return oldState.getBlock() != newState.getBlock(); }
}
