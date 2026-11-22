package br.cropmanager;

import ic2.api.crops.Crops;
import ic2.api.energy.prefab.BasicSink;
import ic2.api.item.IC2Items;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.crop.TileEntityCrop;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class WeederTile extends FarmTile implements ITickable, IUpgradableBlock {
    private static Item FALLBACK_WEED_ITEM;

    public int serverMaxRadius = ManagerConfig.maxRadius;
    public int radius = ManagerConfig.defaultRadius, layers = ManagerConfig.defaultLayers, redstoneMode, removed, cursor;
    public boolean enabled = true, paused;
    public double scanEu = ManagerConfig.weederScanEu, harvestEu = ManagerConfig.weederHarvestEu;
    public int scansPerTick = ManagerConfig.weederScansPerTick;
    private boolean upgradesDirty = true;
    private int activeTicks;

    public final BasicSink energy = createSink(ManagerConfig.weederEnergyCapacity, ManagerConfig.weederSinkTier);
    @Override public BasicSink energySink() { return energy; }
    @Override public boolean isSupportMachine() { return true; }

    public final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot < 4) return ComposterTile.isIc2Weed(stack);
            if (slot < 8) return ManagerTile.isIc2Upgrade(stack);
            return slot == 8 && isBattery(stack);
        }
        @Override protected void onContentsChanged(int slot) {
            if (slot >= 4 && slot < 8) upgradesDirty = true;
            markDirty();
        }
    };

    public static ItemStack weedItem() {
        ItemStack ic2Weed = IC2Items.getItem("crop_res", "weed");
        if (ic2Weed != null && !ic2Weed.isEmpty()) {
            ItemStack copy = ic2Weed.copy();
            copy.setCount(1);
            return copy;
        }
        if (FALLBACK_WEED_ITEM == null) {
            FALLBACK_WEED_ITEM = new Item().setTranslationKey("ic2.crop_res.weed").setRegistryName("cropmanager", "test_ic2_weed");
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(FALLBACK_WEED_ITEM);
        }
        return new ItemStack(FALLBACK_WEED_ITEM, 1);
    }

    @Override public void onLoad() { if (!world.isRemote) { recalculateUpgrades(); energy.onLoad(); } }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }
    @Override public boolean shouldRefresh(net.minecraft.world.World w, BlockPos p, IBlockState a, IBlockState b) { return a.getBlock() != b.getBlock(); }

    @Override public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateUpgrades();
        tickUpgrades(this, inventory, 4, 8);
        dischargeBattery(inventory, 8, energy);
        energy.update();
        if (activeTicks > 0) activeTicks--;
        paused = !enabled || !ManagerTile.redstoneAllows(redstoneMode, redstoneMode != 0 && world.isAreaLoaded(pos.add(-2, -2, -2), pos.add(2, 2, 2), false) && world.isBlockPowered(pos));
        if (!paused) {
            int limit = Math.max(1, scansPerTick);
            for (int i = 0; i < limit && (scanEu <= 0 || energy.getEnergyStored() >= scanEu); i++) {
                scanOne();
            }
        }
        setActive(!paused && activeTicks > 0);
    }

    boolean scanOne() {
        if (upgradesDirty) recalculateUpgrades();
        int[] offset = ScanLogic.offset(cursor, radius, layers);
        cursor = (cursor + 1) % ScanLogic.volume(radius, layers);
        markDirty();
        BlockPos target = pos.add(offset[0], offset[1], offset[2]);
        if (target.getY() < 0 || target.getY() >= world.getHeight() || !world.isBlockLoaded(target, false) || energy.getEnergyStored() < scanEu) return false;
        TileEntity tile = world.getTileEntity(target);
        if (!(tile instanceof TileEntityCrop)) return false;
        if (scanEu > 0 && !energy.useEnergy(scanEu)) return false;
        TileEntityCrop crop = (TileEntityCrop) tile;
        // CropCard.isWeed also flags valuable high-growth crops; never use it for deletion.
        if (crop.getCrop() != Crops.weed) return false;
        ItemStack weedStack = weedItem();
        if (!canInsertOutput(weedStack) || energy.getEnergyStored() < harvestEu) return false;
        if (harvestEu > 0 && !energy.useEnergy(harvestEu)) return false;
        insertOutput(weedStack);
        crop.reset();
        crop.updateState();
        world.markChunkDirty(target, crop);
        if (removed < Integer.MAX_VALUE) removed++;
        activeTicks = 8;
        markDirty();
        return true;
    }

    public boolean canInsertOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot = 0; slot < 4 && !rest.isEmpty(); slot++) {
            rest = inventory.insertItem(slot, rest, true);
        }
        return rest.isEmpty();
    }

    private void insertOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot = 0; slot < 4 && !rest.isEmpty(); slot++) {
            rest = inventory.insertItem(slot, rest, false);
        }
    }

    private void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, inventory, 4, 8, energy, 1, ManagerConfig.weederEnergyCapacity, ManagerConfig.weederSinkTier);
        scansPerTick = Math.max(1, Math.min(64, (int) Math.round(ManagerConfig.weederScansPerTick / Math.max(0.01, multipliers[0]))));
        scanEu = ManagerConfig.weederScanEu * multipliers[1];
        harvestEu = ManagerConfig.weederHarvestEu * multipliers[1];
        upgradesDirty = false;
    }

    @Override public double getEnergy() { return energy.getEnergyStored(); }
    @Override public boolean useEnergy(double amount) { boolean used = energy.useEnergy(amount); if (used) markDirty(); return used; }
    @Override public Set<UpgradableProperty> getUpgradableProperties() { return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer); }

    @Override public boolean control(int action, EntityPlayer player) {
        if (action == 200) enabled = !enabled;
        else if (action == 13) redstoneMode = (redstoneMode + 1) % 3;
        else if (action >= 210 && action <= 214) {
            int[] next = ManagerSettings.applyControl(action < 212 ? action - 210 + 4 : action - 212 + 6, radius, layers, 0, ManagerConfig.maxRadius);
            if (next == null) return false;
            radius = next[0]; layers = next[1]; cursor = 0;
        } else if (action == 215) {
            radius = ManagerConfig.defaultRadius; layers = ManagerConfig.defaultLayers; cursor = 0;
        } else return false;
        markDirty();
        return true;
    }

    private final IItemHandler items = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 8 && isBattery(stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= 0 && slot < 4 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
    };

    public IItemHandler itemsForFace(@Nullable EnumFacing face) { return items; }

    @Override public boolean hasCapability(Capability<?> c, @Nullable EnumFacing f) {
        return c == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(c, f);
    }

    @Override public <T> T getCapability(Capability<T> c, @Nullable EnumFacing f) {
        return c == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(items) : super.getCapability(c, f);
    }

    @Override public NBTTagCompound guiState() {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("MaxRadius", ManagerConfig.maxRadius);
        t.setDouble("EU", energy.getEnergyStored());
        t.setDouble("Capacity", energy.getCapacity());
        t.setInteger("Tier", energy.getSinkTier());
        t.setInteger("Removed", removed);
        t.setInteger("Radius", radius);
        t.setInteger("Layers", layers);
        t.setInteger("Redstone", redstoneMode);
        t.setBoolean("Enabled", enabled);
        t.setBoolean("Paused", paused);
        return t;
    }

    @Override public void acceptGuiState(NBTTagCompound t) {
        serverMaxRadius = t.getInteger("MaxRadius");
        if (t.getDouble("Capacity") > 0) energy.setCapacity(t.getDouble("Capacity"));
        energy.setEnergyStored(t.getDouble("EU"));
        energy.setSinkTier(Math.max(1, t.getInteger("Tier")));
        removed = t.getInteger("Removed");
        radius = t.getInteger("Radius");
        layers = t.getInteger("Layers");
        redstoneMode = t.getInteger("Redstone");
        enabled = !t.hasKey("Enabled") || t.getBoolean("Enabled");
        paused = t.getBoolean("Paused");
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        energy.writeToNBT(t);
        t.setTag("Inventory", inventory.serializeNBT());
        t.setInteger("Removed", removed);
        t.setInteger("Radius", radius);
        t.setInteger("Layers", layers);
        t.setInteger("Redstone", redstoneMode);
        t.setBoolean("Enabled", enabled);
        t.setInteger("Cursor", cursor);
        return t;
    }

    @Override public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        energy.readFromNBT(t);
        double eu = energy.getEnergyStored();
        NBTTagCompound itemsTag = t.getCompoundTag("Inventory").copy();
        itemsTag.setInteger("Size", 9);
        inventory.deserializeNBT(itemsTag);
        removed = Math.max(0, t.getInteger("Removed"));
        radius = Math.max(1, Math.min(ManagerConfig.maxRadius, t.hasKey("Radius") ? t.getInteger("Radius") : ManagerConfig.defaultRadius));
        layers = t.getInteger("Layers") & 7;
        if (layers == 0) layers = ManagerConfig.defaultLayers;
        redstoneMode = Math.max(0, Math.min(2, t.getInteger("Redstone")));
        enabled = !t.hasKey("Enabled") || t.getBoolean("Enabled");
        cursor = Math.floorMod(t.getInteger("Cursor"), ScanLogic.volume(radius, layers));
        upgradesDirty = true;
        recalculateUpgrades();
        energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(energy.getCapacity(), eu)) : 0);
    }

    @Override public void dropContents() { dropHandler(inventory); }
}
