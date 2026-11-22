package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import javax.annotation.Nullable;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

/** Tiered agricultural sprinkler: hydrates farmland & IC2 crops and accelerates crop growth without EU. */
public final class SprinklerTile extends TileEntity implements ITickable {
    public int tier;
    public int cooldown;
    public final FluidTank water = new FluidTank(4000) {
        { setCanDrain(false); }
        @Override public boolean canFillFluidType(FluidStack stack) { return stack != null && stack.getFluid() == FluidRegistry.WATER; }
        @Override protected void onContentsChanged() { markDirty(); }
    };

    public SprinklerTile() { this(1); }

    public SprinklerTile(int tier) {
        this.tier = Math.max(1, Math.min(3, tier));
        water.setCapacity(tankCapacityForTier(this.tier));
    }

    public static int radiusForTier(int tier) {
        return tier >= 3 ? ManagerConfig.sprinklerMk3Radius : ManagerConfig.sprinklerMk1Radius;
    }

    public static int speedMultiplierForTier(int tier) {
        return tier >= 2 ? ManagerConfig.sprinklerMk2Speed : 1;
    }

    public static int waterCostForTier(int tier) {
        return tier == 1 ? 10 : tier == 2 ? 25 : 50;
    }

    public static int tankCapacityForTier(int tier) {
        return tier == 1 ? 4000 : tier == 2 ? 8000 : 16000;
    }

    public int radius() { return radiusForTier(tier); }
    public int speedMultiplier() { return speedMultiplierForTier(tier); }

    public boolean hasWaterSourceBelow() {
        if (world == null || pos == null || pos.getY() <= 0) return false;
        BlockPos below = pos.down();
        if (!world.isBlockLoaded(below, false)) return false;
        IBlockState state = world.getBlockState(below);
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && state.getBlock().getMetaFromState(state) == 0;
    }

    public boolean hasWater() {
        return hasWaterSourceBelow() || water.getFluidAmount() >= waterCostForTier(tier);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        boolean ready = hasWater();
        syncActiveState(ready);
        if (!ready) return;

        int interval = Math.max(5, ManagerConfig.sprinklerBaseInterval / Math.max(1, speedMultiplier()));
        cooldown++;
        if (cooldown < interval) return;
        cooldown = 0;

        if (!hasWaterSourceBelow()) {
            water.drainInternal(waterCostForTier(tier), true);
        }
        sprinkleArea();
    }

    void sprinkleArea() {
        int r = radius();
        int growthBonus = ManagerConfig.sprinklerGrowthBonus * speedMultiplier();
        int hydrationBonus = ManagerConfig.sprinklerHydrationBonus;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos target = pos.add(dx, dy, dz);
                    if (target.getY() < 0 || target.getY() >= world.getHeight() || !world.isBlockLoaded(target, false)) continue;
                    IBlockState state = world.getBlockState(target);
                    if (state.getBlock() instanceof BlockFarmland) {
                        if (state.getValue(BlockFarmland.MOISTURE) < 7) {
                            world.setBlockState(target, state.withProperty(BlockFarmland.MOISTURE, 7), 2);
                        }
                        continue;
                    }
                    TileEntity te = world.getTileEntity(target);
                    if (te instanceof TileEntityCrop) {
                        hydrateCrop((TileEntityCrop) te, hydrationBonus, growthBonus);
                        world.markChunkDirty(target, te);
                    }
                }
            }
        }
    }

    public static void hydrateCrop(TileEntityCrop crop, int hydrationBonus, int growthBonus) {
        if (crop.getStorageWater() < 100) {
            crop.setStorageWater(Math.min(100, crop.getStorageWater() + hydrationBonus));
            crop.dirty = true;
        }
        if (crop.getCrop() != null && crop.getCurrentSize() < crop.getCrop().getMaxSize() && growthBonus > 0) {
            crop.setGrowthPoints(crop.getGrowthPoints() + growthBonus);
            crop.dirty = true;
        }
    }

    private void syncActiveState(boolean active) {
        if (!world.isBlockLoaded(pos, false)) return;
        IBlockState current = world.getBlockState(pos);
        if (current.getBlock() instanceof SprinklerBlock && current.getValue(SprinklerBlock.ACTIVE) != active) {
            world.setBlockState(pos, current.withProperty(SprinklerBlock.ACTIVE, active), 3);
        }
    }

    @Override
    public void markDirty() {
        if (world != null && !world.isRemote && world.isBlockLoaded(pos, false)) world.markChunkDirty(pos, this);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(water);
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Tier", tier);
        tag.setInteger("Cooldown", cooldown);
        tag.setTag("Water", water.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tier = Math.max(1, Math.min(3, tag.hasKey("Tier") ? tag.getInteger("Tier") : 1));
        cooldown = Math.max(0, tag.getInteger("Cooldown"));
        water.setCapacity(tankCapacityForTier(tier));
        water.readFromNBT(tag.getCompoundTag("Water"));
    }
}
