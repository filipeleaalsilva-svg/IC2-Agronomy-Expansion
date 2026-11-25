package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.FluidName;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class BiogasSprayerTile extends FarmTile implements ITickable {
    static final int GUI = 23;
    private int cursor;
    final FluidTank gas = inputTank(true);
    final FluidTank water = inputTank(false);
    final ItemStackHandler fertilizer = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return ManagerTile.isFertilizer(stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{gas.getTankProperties()[0], water.getTankProperties()[0]};
        }
        @Override public int fill(FluidStack stack, boolean execute) {
            if (stack == null) return 0;
            return (stack.getFluid() == FluidRegistry.WATER ? water : gas).fill(stack, execute);
        }
        @Override public FluidStack drain(FluidStack stack, boolean execute) { return null; }
        @Override public FluidStack drain(int amount, boolean execute) { return null; }
    };

    private FluidTank inputTank(boolean isGas) {
        return new FluidTank(4000) {
            { setCanDrain(false); }
            @Override public boolean canFillFluidType(FluidStack stack) {
                return stack != null && stack.getFluid() == (isGas ? FluidName.biogas.getInstance() : FluidRegistry.WATER);
            }
            @Override protected void onContentsChanged() { markDirty(); }
        };
    }

    @Override public void update() {
        if (world == null || world.isRemote || gas.getFluidAmount() < 1
                || (water.getFluidAmount() < 25 && fertilizer.getStackInSlot(0).isEmpty())) return;
        for (int attempt = 0; attempt < 4 && gas.getFluidAmount() > 0; attempt++) {
            BlockPos target = pos.add(cursor % 9 - 4, 0, cursor / 9 - 4);
            cursor = (cursor + 1) % 81;
            if (target.equals(pos) || target.getY() < 0 || target.getY() >= world.getHeight()
                    || !world.isBlockLoaded(target, false)) continue;
            TileEntity entity = world.getTileEntity(target);
            if (entity instanceof TileEntityCrop) tend((TileEntityCrop) entity);
        }
    }

    void tend(TileEntityCrop crop) {
        if (crop.getCrop() == null || crop.getCrop() == ic2.api.crops.Crops.weed) return;
        boolean changed = false;
        if (gas.getFluidAmount() > 0 && water.getFluidAmount() >= 25
                && SprinklerTile.hydrateCrop(crop, ManagerConfig.sprinklerHydrationBonus, 0)) {
            water.drainInternal(25, true);
            gas.drainInternal(1, true);
            changed = true;
        }
        ItemStack stack = fertilizer.getStackInSlot(0);
        if (gas.getFluidAmount() > 0 && ManagerTile.isFertilizer(stack) && FertilizerItem.applyAny(stack, crop, false)) {
            fertilizer.extractItem(0, 1, false);
            gas.drainInternal(1, true);
            changed = true;
        }
        if (changed) world.markChunkDirty(crop.getPos(), crop);
    }

    @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(cap, side);
    }
    @Override public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluids);
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(fertilizer);
        return super.getCapability(cap, side);
    }
    void dropFertilizer() {
        ItemStack stack = fertilizer.extractItem(0, 64, false);
        if (!stack.isEmpty()) net.minecraft.block.Block.spawnAsEntity(world, pos, stack);
    }

    @Override public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Gas", gas.getFluidAmount());
        tag.setInteger("Water", water.getFluidAmount());
        return tag;
    }
    @Override public void acceptGuiState(NBTTagCompound tag) {
        gas.setFluid(tag.getInteger("Gas") == 0 ? null : new FluidStack(FluidName.biogas.getInstance(), tag.getInteger("Gas")));
        water.setFluid(tag.getInteger("Water") == 0 ? null : new FluidStack(FluidRegistry.WATER, tag.getInteger("Water")));
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Gas", gas.writeToNBT(new NBTTagCompound()));
        tag.setTag("Water", water.writeToNBT(new NBTTagCompound()));
        tag.setTag("Fertilizer", fertilizer.serializeNBT());
        tag.setInteger("Cursor", cursor);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagCompound inventory = tag.getCompoundTag("Fertilizer").copy();
        inventory.setInteger("Size", 1);
        fertilizer.deserializeNBT(inventory);
        if (!ManagerTile.isFertilizer(fertilizer.getStackInSlot(0))) fertilizer.setStackInSlot(0, ItemStack.EMPTY);
        readTank(gas, tag.getCompoundTag("Gas"));
        readTank(water, tag.getCompoundTag("Water"));
        cursor = Math.max(0, Math.min(80, tag.getInteger("Cursor")));
    }
    private static void readTank(FluidTank tank, NBTTagCompound tag) {
        tank.readFromNBT(tag);
        FluidStack stack = tank.getFluid();
        if (!tank.canFillFluidType(stack)) tank.setFluid(null);
        else stack.amount = Math.min(tank.getCapacity(), stack.amount);
    }
}
