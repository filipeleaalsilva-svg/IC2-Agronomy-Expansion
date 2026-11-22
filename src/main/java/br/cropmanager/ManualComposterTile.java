package br.cropmanager;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Early-game non-electric composter tile: turns organic inputs into Organic Compost over time. */
public final class ManualComposterTile extends TileEntity implements ITickable {
    public final List<ItemStack> rawInputs = new ArrayList<>();
    public int progress;
    public int duration = ManagerConfig.manualComposterDuration;
    public boolean hydrated;
    public int readyCount;

    public int itemCount() {
        int total = 0;
        for (ItemStack s : rawInputs) total += s.getCount();
        return total;
    }

    public boolean isReady() { return readyCount > 0; }

    public boolean isFermenting() {
        return !isReady() && itemCount() >= ManagerConfig.manualComposterRequiredItems;
    }

    public int visualFill() {
        if (isReady()) return 5;
        int count = itemCount();
        if (count <= 0) return 0;
        int req = Math.max(1, ManagerConfig.manualComposterRequiredItems);
        if (count >= req) return 4;
        return Math.max(1, Math.min(3, (int) Math.ceil(count * 3.0 / req)));
    }

    public int progressPercent() {
        if (isReady()) return 100;
        if (!isFermenting()) return 0;
        return Math.min(99, progress * 100 / Math.max(1, duration));
    }

    public boolean addOrganic(ItemStack stack) {
        if (isReady() || isFermenting() || !ComposterTile.organic(stack)) return false;
        int remaining = ManagerConfig.manualComposterRequiredItems - itemCount();
        if (remaining <= 0) return false;
        ItemStack one = stack.copy();
        one.setCount(1);
        for (ItemStack existing : rawInputs) {
            if (ItemStack.areItemsEqual(existing, one) && ItemStack.areItemStackTagsEqual(existing, one)) {
                existing.grow(1);
                onStateChanged();
                return true;
            }
        }
        rawInputs.add(one);
        onStateChanged();
        return true;
    }

    public boolean hydrate() {
        if (hydrated || isReady() || itemCount() <= 0) return false;
        hydrated = true;
        markDirty();
        return true;
    }

    public ItemStack collectCompost() {
        if (!isReady()) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(CropManager.ORGANIC_COMPOST, readyCount);
        readyCount = 0;
        progress = 0;
        hydrated = false;
        rawInputs.clear();
        onStateChanged();
        return out;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        if (!isFermenting()) return;
        if (duration <= 0) duration = Math.max(1, ManagerConfig.manualComposterDuration);
        int step = hydrated ? Math.max(1, (int) Math.round(ManagerConfig.manualComposterWaterSpeed)) : 1;
        progress += step;
        if (progress >= duration) {
            rawInputs.clear();
            progress = 0;
            hydrated = false;
            readyCount = Math.max(1, ManagerConfig.manualComposterOutputCount);
            onStateChanged();
        } else if (progress % 20 == 0) {
            markDirty();
        }
    }

    private void onStateChanged() {
        if (duration <= 0) duration = Math.max(1, ManagerConfig.manualComposterDuration);
        markDirty();
        syncBlockState();
    }

    private void syncBlockState() {
        if (world == null || world.isRemote || !world.isBlockLoaded(pos, false)) return;
        IBlockState current = world.getBlockState(pos);
        if (!(current.getBlock() instanceof ManualComposterBlock)) return;
        int fill = visualFill();
        if (current.getValue(ManualComposterBlock.FILL) != fill) {
            world.setBlockState(pos, current.withProperty(ManualComposterBlock.FILL, fill), 3);
        }
    }

    public void dropContents() {
        if (world == null || world.isRemote) return;
        if (readyCount > 0) {
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(CropManager.ORGANIC_COMPOST, readyCount));
            readyCount = 0;
        }
        for (ItemStack s : rawInputs) {
            if (!s.isEmpty()) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), s.copy());
        }
        rawInputs.clear();
        progress = 0;
        hydrated = false;
        markDirty();
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
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Progress", progress);
        tag.setInteger("Duration", duration);
        tag.setBoolean("Hydrated", hydrated);
        tag.setInteger("ReadyCount", readyCount);
        NBTTagList list = new NBTTagList();
        for (ItemStack s : rawInputs) {
            if (!s.isEmpty()) list.appendTag(s.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("RawInputs", list);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        duration = Math.max(1, tag.hasKey("Duration") ? tag.getInteger("Duration") : ManagerConfig.manualComposterDuration);
        progress = Math.max(0, Math.min(duration, tag.getInteger("Progress")));
        hydrated = tag.getBoolean("Hydrated");
        readyCount = Math.max(0, tag.getInteger("ReadyCount"));
        rawInputs.clear();
        NBTTagList list = tag.getTagList("RawInputs", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack s = new ItemStack(list.getCompoundTagAt(i));
            if (!s.isEmpty()) rawInputs.add(s);
        }
    }
}
