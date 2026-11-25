package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
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

public final class DiagonalSprinklerTile extends FarmTile implements ITickable {
    static final int GUI = 24;
    private int enabled = 15;
    private int nextQuadrant;
    private int active = -1;
    private int offset;
    private int countdown;
    private int redstone = 2;
    final FluidTank water = new FluidTank(4000) {
        { setCanDrain(false); }
        @Override public boolean canFillFluidType(FluidStack stack) {
            return stack != null && stack.getFluid() == FluidRegistry.WATER;
        }
        @Override protected void onContentsChanged() { markDirty(); }
    };

    static BlockPos column(BlockPos origin, int quadrant, int index) {
        int dx = (index % 9 + 1) * ((quadrant & 1) == 0 ? -1 : 1);
        int dz = (index / 9 + 1) * ((quadrant & 2) == 0 ? -1 : 1);
        return origin.add(dx, 0, dz);
    }

    private boolean sourceBelow() {
        BlockPos below = pos.down();
        if (below.getY() < 0 || !world.isBlockLoaded(below, false)) return false;
        IBlockState state = world.getBlockState(below);
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && state.getBlock().getMetaFromState(state) == 0;
    }

    @Override public void update() {
        if (world == null || world.isRemote || enabled == 0
                || !SprinklerTile.redstoneAllows(redstone, world.isBlockPowered(pos))) return;

        if (countdown > 0) countdown--;
        if (active < 0) {
            if (countdown > 0) return;
            int cost = SprinklerTile.waterCostForTier(1);
            if (!sourceBelow()) {
                if (water.getFluidAmount() < cost) return;
                water.drainInternal(cost, true);
            }
            while ((enabled & (1 << nextQuadrant)) == 0) nextQuadrant = (nextQuadrant + 1) & 3;
            active = nextQuadrant;
            nextQuadrant = (nextQuadrant + 1) & 3;
            offset = 0;
            countdown = Math.max(6, ManagerConfig.sprinklerBaseInterval / Integer.bitCount(enabled));
        }

        int limit = Math.min(81, offset + 16);
        for (; offset < limit; offset++) {
            BlockPos column = column(pos, active, offset);
            for (int dy = -1; dy <= 1; dy++) waterAt(column.up(dy));
        }
        if (offset == 81) active = -1;
        markDirty();
    }

    private void waterAt(BlockPos target) {
        if (target.getY() < 0 || target.getY() >= world.getHeight() || !world.isBlockLoaded(target, false)) return;
        IBlockState state = world.getBlockState(target);
        if (state.getBlock() instanceof BlockFarmland) {
            if (state.getValue(BlockFarmland.MOISTURE) < 7) {
                world.setBlockState(target, state.withProperty(BlockFarmland.MOISTURE, 7), 2);
            }
            return;
        }
        TileEntity tile = world.getTileEntity(target);
        if (tile instanceof TileEntityCrop && SprinklerTile.hydrateCrop((TileEntityCrop) tile,
                ManagerConfig.sprinklerHydrationBonus, ManagerConfig.sprinklerGrowthBonus)) {
            world.markChunkDirty(target, tile);
        }
    }

    @Override public boolean control(int action, EntityPlayer player) {
        if (world == null || world.isRemote || action < 0 || action > 4) return false;
        if (action == 4) redstone = (redstone + 1) % 3;
        else {
            enabled ^= 1 << action;
            if (active == action) active = -1;
        }
        markDirty();
        return true;
    }

    boolean enabled(int quadrant) { return (enabled & (1 << quadrant)) != 0; }
    int redstoneMode() { return redstone; }

    @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(cap, side);
    }
    @Override public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(water) : super.getCapability(cap, side);
    }
    @Override public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Quadrants", enabled);
        tag.setInteger("Redstone", redstone);
        tag.setInteger("Water", water.getFluidAmount());
        return tag;
    }
    @Override public void acceptGuiState(NBTTagCompound tag) {
        enabled = tag.getInteger("Quadrants") & 15;
        redstone = Math.max(0, Math.min(2, tag.getInteger("Redstone")));
        int amount = Math.max(0, Math.min(water.getCapacity(), tag.getInteger("Water")));
        water.setFluid(amount == 0 ? null : new FluidStack(FluidRegistry.WATER, amount));
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.merge(guiState());
        tag.setInteger("Active", active);
        tag.setInteger("Next", nextQuadrant);
        tag.setInteger("Offset", offset);
        tag.setInteger("Countdown", countdown);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        acceptGuiState(tag);
        if (!tag.hasKey("Quadrants")) enabled = 15;
        if (!tag.hasKey("Redstone")) redstone = 2;
        active = tag.hasKey("Active") ? Math.max(-1, Math.min(3, tag.getInteger("Active"))) : -1;
        if (active >= 0 && !enabled(active)) active = -1;
        nextQuadrant = tag.getInteger("Next") & 3;
        offset = Math.max(0, Math.min(81, tag.getInteger("Offset")));
        countdown = Math.max(0, Math.min(1200, tag.getInteger("Countdown")));
    }
}
