package br.cropmanager;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.ref.FluidName;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

public final class BiogasHeatBurnerTile extends TileEntity implements IHeatSource {
    static final int HEAT_PER_MB = 32;
    private int prepaidHeat;
    private long lastDrawTick = Long.MIN_VALUE;
    private int emittedHeat;

    final FluidTank gas = new FluidTank(4000) {
        @Override public boolean canFillFluidType(FluidStack stack) {
            return stack != null && stack.getFluid() == FluidName.biogas.getInstance();
        }
        @Override protected void onContentsChanged() {
            if (world != null && !world.isRemote) markDirty();
        }
    };

    public BiogasHeatBurnerTile() {
        gas.setCanDrain(false);
    }

    @Override public int maxrequestHeatTick(EnumFacing side) {
        if (world == null || world.isRemote || pos == null || !world.isBlockLoaded(pos, false)) return 0;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.BIOGAS_HEAT_BURNER
                || side != state.getValue(AgriculturalMachineBlock.FACING)) {
            return 0;
        }
        int spent = lastDrawTick == world.getTotalWorldTime() ? emittedHeat : 0;
        return Math.min(HEAT_PER_MB - spent, prepaidHeat + gas.getFluidAmount() * HEAT_PER_MB);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override public int requestHeat(EnumFacing side, int request) {
        return drawHeat(side, request, false);
    }

    @Override public int drawHeat(EnumFacing side, int request, boolean simulate) {
        int delivered = Math.min(Math.max(0, request), maxrequestHeatTick(side));
        if (simulate || delivered == 0) return delivered;
        if (lastDrawTick != world.getTotalWorldTime()) {
            lastDrawTick = world.getTotalWorldTime();
            emittedHeat = 0;
        }
        if (delivered > prepaidHeat) {
            gas.drainInternal(1, true);
            prepaidHeat += HEAT_PER_MB;
        }
        prepaidHeat -= delivered;
        emittedHeat += delivered;
        markDirty();
        return delivered;
    }

    @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(gas) : super.getCapability(capability, facing);
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Gas", gas.writeToNBT(new NBTTagCompound()));
        tag.setInteger("PrepaidHeat", prepaidHeat);
        return tag;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        gas.readFromNBT(tag.getCompoundTag("Gas"));
        FluidStack fuel = gas.getFluid();
        if (!gas.canFillFluidType(fuel)) gas.setFluid(null);
        else fuel.amount = Math.min(gas.getCapacity(), fuel.amount);
        prepaidHeat = Math.max(0, Math.min(HEAT_PER_MB - 1, tag.getInteger("PrepaidHeat")));
    }
}
