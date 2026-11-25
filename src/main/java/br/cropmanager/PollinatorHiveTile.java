package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Stores Mutagenic Powder charges and runs one safe cross-breeding pulse at a time. */
public final class PollinatorHiveTile extends TileEntity implements ITickable {
    private int charges;
    private int cooldown;
    private int dirtyTicks;

    public int getCharges() {
        return charges;
    }

    public int getCooldown() {
        return cooldown;
    }

    /** Loads one powder, filling at most the available room. Returns false when already full. */
    public boolean addPowderCharge() {
        int max = Math.max(1, ManagerConfig.pollinatorMaxCharges);
        if (charges >= max) return false;
        int amount = Math.max(1, ManagerConfig.pollinatorChargesPerPowder);
        charges = Math.min(max, charges + amount);
        markDirty();
        syncChargedState();
        return true;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || !world.isBlockLoaded(pos, false)) return;
        syncChargedState();

        int interval = Math.max(1, ManagerConfig.pollinatorBaseInterval);
        cooldown += charges > 0 ? 3 : 1;
        if (cooldown < interval) {
            if (++dirtyTicks >= 20) {
                dirtyTicks = 0;
                markDirty();
            }
            return;
        }
        cooldown -= interval;
        dirtyTicks = 0;

        boolean accelerated = charges > 0;
        if (runOnePulse() && accelerated) {
            charges--;
            syncChargedState();
        }
        markDirty();
    }

    /** Scans the configured 5x5x3 area and advances only the first eligible crossing site. */
    boolean runOnePulse() {
        if (world == null || world.isRemote) return false;
        int radius = Math.max(0, ManagerConfig.pollinatorRadius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos target = pos.add(dx, dy, dz);
                    if (target.getY() < 0 || target.getY() >= world.getHeight()
                            || !world.isBlockLoaded(target, false)) continue;
                    TileEntity tile = world.getTileEntity(target);
                    if (tile instanceof TileEntityCrop
                            && CrossBreedingHelper.accelerateCrossing(world, (TileEntityCrop) tile, 1, true, world.rand)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void syncChargedState() {
        if (world == null || world.isRemote || pos == null || !world.isBlockLoaded(pos, false)) return;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == CropManager.POLLINATOR_HIVE
                && state.getValue(PollinatorHiveBlock.CHARGED) != (charges > 0)) {
            world.setBlockState(pos, state.withProperty(PollinatorHiveBlock.CHARGED, charges > 0), 2);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Charges", charges);
        tag.setInteger("Cooldown", cooldown);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        int max = Math.max(1, ManagerConfig.pollinatorMaxCharges);
        charges = Math.max(0, Math.min(max, tag.getInteger("Charges")));
        int interval = Math.max(1, ManagerConfig.pollinatorBaseInterval);
        cooldown = Math.max(0, Math.min(interval - 1, tag.getInteger("Cooldown")));
    }
}
