package br.cropmanager;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/** Reads retired Crop Manager tile IDs so older saves can finish loading safely. */
public class LegacyRecoveryTile extends TileEntity {
    public static final class Collector extends LegacyRecoveryTile {}
    public static final class Sorter extends LegacyRecoveryTile {}
    public static final class Bench extends LegacyRecoveryTile {}

    private final List<ItemStack> contents = new ArrayList<>();
    private final List<ItemStack> fluidContainers = new ArrayList<>();
    private boolean retired;

    @Override public void onLoad() {
        if (!world.isRemote) LegacyMigration.request(this);
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        contents.clear();
        fluidContainers.clear();
        append(tag.getCompoundTag("Inventory").getTagList("Items", 10));
        append(tag.getTagList("Recovery", 10));
        collectFluidContainer(tag.getCompoundTag("Water"));
        collectFluidContainer(tag.getCompoundTag("Weed"));
    }

    private void append(NBTTagList list) {
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = new ItemStack(list.getCompoundTagAt(i));
            if (!stack.isEmpty()) contents.add(stack);
        }
    }

    private void collectFluidContainer(NBTTagCompound tank) {
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(tank);
        if (fluid == null || fluid.amount < 1000) return;
        ItemStack bucket = FluidUtil.getFilledBucket(new FluidStack(fluid, 1000));
        if (bucket.isEmpty()) return;
        int count = Math.min(64, fluid.amount / 1000);
        if (bucket.getMaxStackSize() == 1) {
            for (int i = 0; i < count; i++) fluidContainers.add(bucket.copy());
        } else {
            bucket.setCount(count);
            fluidContainers.add(bucket);
        }
    }

    void retire() {
        if (retired || world == null || world.isRemote) return;
        retired = true;
        for (ItemStack stack : contents) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
        for (ItemStack stack : fluidContainers) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
        contents.clear();
        fluidContainers.clear();
        world.setBlockToAir(pos);
    }

    void dropContents() { retire(); }
}
