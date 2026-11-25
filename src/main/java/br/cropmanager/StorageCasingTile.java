package br.cropmanager;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

/** Non-ticking, EU-free casing endpoint that forwards formed-structure access to its controller. */
public final class StorageCasingTile extends TileEntity {
    private int controllerDx;
    private int controllerDy;
    private int controllerDz;
    private boolean linked;

    void setController(@Nullable BlockPos masterPos) {
        if (masterPos == null) {
            if (!linked) return;
            linked = false;
            controllerDx = 0;
            controllerDy = 0;
            controllerDz = 0;
        } else {
            controllerDx = masterPos.getX() - pos.getX();
            controllerDy = masterPos.getY() - pos.getY();
            controllerDz = masterPos.getZ() - pos.getZ();
            linked = true;
        }
        markDirty();
        syncBlock();
    }

    @Nullable
    private BlockPos controllerPos() {
        return linked ? pos.add(controllerDx, controllerDy, controllerDz) : null;
    }

    @Nullable
    public StorageMultiblock controller() {
        BlockPos masterPos = controllerPos();
        if (masterPos == null || world == null || !world.isBlockLoaded(masterPos, false)) return null;
        TileEntity master = world.getTileEntity(masterPos);
        if (!(master instanceof StorageMultiblock) || master.isInvalid()) return null;
        StorageMultiblock multiblock = (StorageMultiblock) master;
        return multiblock.containsCasing(pos) ? multiblock : null;
    }

    boolean isFormed() {
        StorageMultiblock multiblock = controller();
        return multiblock != null && multiblock.isFormed();
    }

    int comparatorSignal() {
        StorageMultiblock multiblock = controller();
        return multiblock != null && multiblock.isFormed() ? multiblock.comparatorSignal() : 0;
    }

    void notifyControllerBroken() {
        StorageMultiblock multiblock = controller();
        if (multiblock != null) multiblock.casingBroken(pos);
        linked = false;
    }

    @Nullable
    private StorageMultiblock activePortTarget(@Nullable EnumFacing side) {
        if (isInvalid() || world == null || !world.isBlockLoaded(pos, false)
                || world.getTileEntity(pos) != this) return null;
        StorageMultiblock multiblock = controller();
        if (multiblock == null || !multiblock.isFormed() || !multiblock.isExterior(pos, side)) return null;
        return multiblock;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        StorageMultiblock target = activePortTarget(facing);
        return (target != null && target.hasPortCapability(capability, facing))
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        StorageMultiblock target = activePortTarget(facing);
        if (target != null && target.hasPortCapability(capability, facing)) {
            T result = target.getPortCapability(capability, facing);
            if (result != null) return result;
        }
        return super.getCapability(capability, facing);
    }

    private void syncBlock() {
        if (world == null || world.isRemote || !world.isBlockLoaded(pos, false)) return;
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("StorageLinked", linked);
        tag.setInteger("ControllerDx", controllerDx);
        tag.setInteger("ControllerDy", controllerDy);
        tag.setInteger("ControllerDz", controllerDz);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        linked = tag.getBoolean("StorageLinked");
        controllerDx = tag.getInteger("ControllerDx");
        controllerDy = tag.getInteger("ControllerDy");
        controllerDz = tag.getInteger("ControllerDz");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
        boolean previousLinked = linked;
        readFromNBT(packet.getNbtCompound());
        if (previousLinked != linked && world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
