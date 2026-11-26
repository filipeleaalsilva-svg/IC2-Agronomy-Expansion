package br.cropmanager;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.core.ref.FluidName;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class BiogasGeneratorTile extends FarmTile implements ITickable, IEnergySource, StorageMultiblock {
    static final int GUI = 22;
    static final int HEIGHT = 3;
    private static final int OUTPUT = 32;
    private static final int BUFFER = 1024;
    private boolean formed;
    private boolean registered;
    private BlockPos center;
    private int checkTicks;
    private int previousSignal = -1;
    private double storedEu;
    final FluidTank gas = new FluidTank(16000) {
        { setCanDrain(false); }
        @Override public boolean canFillFluidType(FluidStack stack) {
            return stack != null && stack.getFluid() == FluidName.biogas.getInstance();
        }
        @Override protected void onContentsChanged() {
            markDirty();
            updateComparator(center);
        }
    };

    @Nullable
    private EnumFacing front() {
        if (world == null || pos == null || !world.isBlockLoaded(pos, false)) return null;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == CropManager.BIOGAS_GENERATOR ? state.getValue(AgriculturalMachineBlock.FACING) : null;
    }

    void unformOnBreak() {
        BlockPos oldCenter = center;
        formed = false;
        center = null;
        previousSignal = 0;
        if (oldCenter != null && world != null) {
            StorageMultiblockSupport.linkStorageCasings(world, pos, oldCenter, pos.getY() - 1, HEIGHT, null);
            StorageMultiblockSupport.updateShellComparators(world, pos, oldCenter, pos.getY() - 1, HEIGHT,
                    CropManager.BIOGAS_GENERATOR_CASING);
        }
    }

    private void updateComparator(@Nullable BlockPos shellCenter) {
        if (world == null || world.isRemote || pos == null || !world.isBlockLoaded(pos, false)) return;
        int signal = comparatorSignal();
        if (signal == previousSignal) return;
        previousSignal = signal;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == CropManager.BIOGAS_GENERATOR) {
            world.updateComparatorOutputLevel(pos, state.getBlock());
        }
        if (shellCenter != null) {
            StorageMultiblockSupport.updateShellComparators(world, pos, shellCenter, pos.getY() - 1, HEIGHT,
                    CropManager.BIOGAS_GENERATOR_CASING);
        }
    }

    private void unformAndSync() {
        unformOnBreak();
        markDirty();
        if (world != null && pos != null && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            if (!world.isRemote && state.getBlock() == CropManager.BIOGAS_GENERATOR) {
                world.updateComparatorOutputLevel(pos, state.getBlock());
            }
        }
    }

    void validateShell() {
        EnumFacing facing = front();
        if (facing == null) {
            if (formed || center != null) unformAndSync();
            return;
        }
        BlockPos proposed = pos.offset(facing.getOpposite());
        StorageMultiblockSupport.Validation result = StorageMultiblockSupport.validateStructure(world, pos, proposed,
                pos.getY() - 1, HEIGHT, facing, CropManager.BIOGAS_GENERATOR, CropManager.BIOGAS_GENERATOR_CASING);
        if (result == StorageMultiblockSupport.Validation.DEFER) return;
        boolean valid = result == StorageMultiblockSupport.Validation.VALID;
        if (formed == valid && (!valid ? center == null : proposed.equals(center))) return;
        BlockPos oldCenter = center;
        if (oldCenter != null) {
            StorageMultiblockSupport.linkStorageCasings(world, pos, oldCenter, pos.getY() - 1, HEIGHT, null);
        }
        center = valid ? proposed : null;
        formed = valid;
        if (valid) StorageMultiblockSupport.linkStorageCasings(world, pos, center, pos.getY() - 1, HEIGHT, pos);
        updateComparator(valid ? center : (oldCenter != null ? oldCenter : proposed));
        markDirty();
        if (world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (checkTicks-- <= 0) {
            validateShell();
            checkTicks = 19;
        }
        if (formed && storedEu <= BUFFER - OUTPUT && gas.getFluidAmount() > 0) {
            gas.drainInternal(1, true);
            storedEu += OUTPUT;
            markDirty();
        }
    }

    @Override public void onLoad() {
        if (!world.isRemote && !registered && EnergyNet.instance != null) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            registered = true;
        }
    }

    private void unregisterEnergy() {
        if (registered) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            registered = false;
        }
    }

    @Override public void invalidate() {
        unregisterEnergy();
        super.invalidate();
    }

    @Override public void onChunkUnload() {
        unregisterEnergy();
        super.onChunkUnload();
    }

    @Override public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
        EnumFacing facing = front();
        return facing != null && side == facing;
    }
    @Override public double getOfferedEnergy() { return formed ? Math.min(OUTPUT, storedEu) : 0; }
    @Override public int getSourceTier() { return 1; }
    @Override public void drawEnergy(double amount) {
        if (!formed || !Double.isFinite(amount) || amount <= 0) return;
        storedEu = Math.max(0, storedEu - amount);
        markDirty();
    }

    @Override public boolean isFormed() { return formed; }
    BlockPos center() { return center; }
    int baseY() { return pos.getY() - 1; }
    @Override public boolean containsCasing(BlockPos part) {
        return StorageMultiblockSupport.containsCasing(pos, center, pos.getY() - 1, HEIGHT, part);
    }
    @Override public boolean isExterior(BlockPos part, EnumFacing side) {
        return StorageMultiblockSupport.isExterior(center, pos.getY() - 1, HEIGHT, part, side);
    }
    @Override public void casingBroken(BlockPos part) {
        if (!containsCasing(part)) return;
        unformOnBreak();
        checkTicks = 0;
        markDirty();
        if (world != null && pos != null && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            if (!world.isRemote && state.getBlock() == CropManager.BIOGAS_GENERATOR) {
                world.updateComparatorOutputLevel(pos, state.getBlock());
            }
        }
    }
    @Override public int comparatorSignal() {
        return formed ? StorageMultiblockSupport.comparatorSignal(gas.getFluidAmount(), gas.getCapacity()) : 0;
    }

    @Override public boolean usable(EntityPlayer player) {
        return super.usable(player) || (formed && StorageMultiblockSupport.canUseFromShell(
                this, player, center, pos.getY() - 1, HEIGHT, CropManager.BIOGAS_GENERATOR_CASING));
    }
    @Override public boolean openFromCasing(EntityPlayer player) {
        if (!formed || !usable(player)) return false;
        openGui(player);
        return true;
    }
    void openGui(EntityPlayer player) {
        player.openGui(CropManager.instance, GUI, world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override public boolean hasPortCapability(Capability<?> capability, EnumFacing side) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }
    @Override public <T> T getPortCapability(Capability<T> capability, EnumFacing side) {
        return hasPortCapability(capability, side) ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(gas) : null;
    }
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
        return hasPortCapability(capability, side) || super.hasCapability(capability, side);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        return hasPortCapability(capability, side) ? getPortCapability(capability, side) : super.getCapability(capability, side);
    }

    @Override public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Formed", formed);
        tag.setInteger("Gas", gas.getFluidAmount());
        tag.setDouble("EU", storedEu);
        return tag;
    }
    @Override public void acceptGuiState(NBTTagCompound tag) {
        formed = tag.getBoolean("Formed");
        gas.setFluid(tag.getInteger("Gas") == 0 ? null : new FluidStack(FluidName.biogas.getInstance(), tag.getInteger("Gas")));
        storedEu = tag.getDouble("EU");
    }

    public NBTTagCompound writeItemData() {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.removeTag("x");
        tag.removeTag("y");
        tag.removeTag("z");
        return tag;
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("GasTank", gas.writeToNBT(new NBTTagCompound()));
        tag.setDouble("StoredEU", storedEu);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        StorageMultiblockSupport.readFluidTank(gas, tag.getCompoundTag("GasTank"));
        FluidStack fuel = gas.getFluid();
        if (!gas.canFillFluidType(fuel)) gas.setFluid(null);
        else fuel.amount = Math.min(gas.getCapacity(), fuel.amount);
        double saved = tag.getDouble("StoredEU");
        storedEu = Double.isFinite(saved) ? Math.max(0, Math.min(BUFFER, saved)) : 0;
        formed = false;
        center = null;
        checkTicks = 0;
        previousSignal = -1;
    }

    @Override public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.setBoolean("Formed", formed);
        if (center != null) {
            tag.setInteger("CenterX", center.getX());
            tag.setInteger("CenterY", center.getY());
            tag.setInteger("CenterZ", center.getZ());
        }
        return tag;
    }

    @Override public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
        boolean previousFormed = formed;
        BlockPos previousCenter = center;
        handleUpdateTag(packet.getNbtCompound());
        if (previousFormed != formed && world != null) {
            BlockPos redraw = center != null ? center : previousCenter;
            if (redraw != null) {
                world.markBlockRangeForRenderUpdate(
                        redraw.add(-1, -1, -1), redraw.add(1, HEIGHT - 1, 1));
            } else {
                world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
            }
        }
    }

    @Override public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
        formed = tag.getBoolean("Formed");
        center = tag.hasKey("CenterX")
                ? new BlockPos(tag.getInteger("CenterX"), tag.getInteger("CenterY"), tag.getInteger("CenterZ"))
                : null;
    }

    @Override public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @SideOnly(Side.CLIENT)
    @Override public AxisAlignedBB getRenderBoundingBox() {
        if (center == null) return new AxisAlignedBB(pos);
        return new AxisAlignedBB(center.getX() - 1.25, baseY(), center.getZ() - 1.25,
                center.getX() + 2.25, baseY() + HEIGHT, center.getZ() + 2.25);
    }
}
