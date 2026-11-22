package br.cropmanager.chest.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TileCropDatabaseChest extends TileEntity implements ISidedInventory, ITickable {
    private long contentsRevision;
    private final long[] slotRevisions;
    private int viewers, ticks;
    public float lidAngle, previousLidAngle;
    private final SeedInventory seeds;
    private final ChestSettings settings = new ChestSettings(this);
    private final IItemHandler controlledAutomation = new IItemHandler() {
        @Override public int getSlots() { return getSizeInventory(); }
        @Override public ItemStack getStackInSlot(int slot) { return seeds.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return automationEnabled() ? seeds.insertItem(slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) { return automationEnabled() ? seeds.extractItem(slot, count, simulate) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    public TileCropDatabaseChest() { this(SeedInventory.SIZE); }
    protected TileCropDatabaseChest(int capacity) {
        slotRevisions = new long[capacity];
        seeds = new SeedInventory(capacity) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return isSeedSlotEnabled(slot) && super.isItemValid(slot, stack); }
            @Override protected void onContentsChanged(int slot) {
                contentsRevision++;
                slotRevisions[slot]++;
                TileCropDatabaseChest.this.markDirty();
            }
        };
    }
    public SeedInventory seeds() { return seeds; }
    public int getActiveCapacity() { return getSizeInventory(); }
    public boolean isSeedSlotEnabled(int index) { return index >= 0 && index < getSizeInventory(); }
    protected void changed() { contentsRevision++; markDirty(); }
    public long getContentsRevision() { return contentsRevision; }
    public long getSlotRevision(int slot) { return slotRevisions[slot]; }
    public boolean isPortable() { return false; }
    public boolean isCarrierSlot(int index) { return false; }
    public boolean hasManagement() { return !isPortable(); }
    public boolean collectingEnabled() { return settings.collecting(); }
    public void setCollectingEnabled(boolean value) { if (hasManagement()) settings.setCollecting(value); }
    public int redstoneMode() { return settings.redstoneMode(); }
    public void setRedstoneMode(int mode) { if (hasManagement()) settings.setRedstoneMode(mode); }
    public boolean automationEnabled() { return !hasManagement() || settings.automationEnabled(); }
    public boolean isFavorite(ItemStack seed) { return hasManagement() && settings.isFavorite(seed); }
    public void toggleFavorite(int slot) { if (hasManagement()) settings.toggleFavorite(slot); }
    public void deposit(EntityPlayer player) { if (hasManagement()) settings.deposit(player); }
    public NBTTagCompound managementTag() { return settings.write(); }
    public void readManagement(NBTTagCompound tag) { if (hasManagement()) settings.read(tag); }
    public IItemHandler automation() { return hasManagement() ? controlledAutomation : seeds; }
    @Override public int[] getSlotsForFace(EnumFacing side) {
        if (!automationEnabled()) return new int[0];
        int[] slots = new int[getSizeInventory()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }
    @Override public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return automationEnabled() && slot >= 0 && slot < getSizeInventory() && SeedInventory.accepts(stack);
    }
    @Override public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) { return automationEnabled() && slot >= 0 && slot < getSizeInventory(); }
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automation()) : super.getCapability(capability, side);
    }
    @Override public int getSizeInventory() { return seeds.getSlots(); }
    @Override public boolean isEmpty() {
        for (int i = 0; i < getSizeInventory(); i++) if (!getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int index) { return seeds.getStackInSlot(index); }
    @Override public ItemStack decrStackSize(int index, int count) { return seeds.extractItem(index, count, false); }
    @Override public ItemStack removeStackFromSlot(int index) { return seeds.extractItem(index, Integer.MAX_VALUE, false); }
    @Override public void setInventorySlotContents(int index, ItemStack stack) { seeds.setStackInSlot(index, stack); }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && !isInvalid() && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
    public boolean isRemotelyUsableByPlayer(EntityPlayer player) {
        return world != null && !isInvalid() && world.isBlockLoaded(pos) && world.getTileEntity(pos) == this;
    }
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) { return SeedInventory.accepts(stack); }
    @Override public void openInventory(EntityPlayer player) {
        if (world != null && !world.isRemote && !player.isSpectator()) { viewers++; syncViewers(); }
    }
    @Override public void closeInventory(EntityPlayer player) {
        if (world != null && !world.isRemote && !player.isSpectator()) { viewers = Math.max(0, viewers - 1); syncViewers(); }
    }
    private void syncViewers() { world.addBlockEvent(pos, getBlockType(), 1, viewers); }
    @Override public boolean receiveClientEvent(int id, int value) {
        if (id == 1) { viewers = Math.max(0, value); return true; }
        return super.receiveClientEvent(id, value);
    }
    @Override public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", pos.getX()); tag.setInteger("y", pos.getY()); tag.setInteger("z", pos.getZ());
        tag.setInteger("Viewers", viewers);
        return tag;
    }
    @Override public void handleUpdateTag(NBTTagCompound tag) { viewers = tag.getInteger("Viewers"); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) { handleUpdateTag(packet.getNbtCompound()); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(pos).grow(0.5, 1, 0.5); }
    @Override public void update() {
        if (world == null) return;
        if (!world.isRemote && hasManagement()) settings.tick();
        if (!world.isRemote && ++ticks % 200 == 0) {
            int actual = 0;
            for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(8))) {
                if (!player.isSpectator() && player.openContainer instanceof ContainerCropDatabaseChest
                        && ((ContainerCropDatabaseChest) player.openContainer).usesChest(this)) actual++;
            }
            if (viewers != actual) { viewers = actual; syncViewers(); }
        }
        previousLidAngle = lidAngle;
        lidAngle = Math.max(0, Math.min(1, lidAngle + (viewers > 0 ? 0.1F : -0.1F)));
        if (!world.isRemote) {
            if (previousLidAngle == 0 && viewers > 0) world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, 1.0F);
            else if (previousLidAngle >= 0.5F && lidAngle < 0.5F) world.playSound(null, pos, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, 1.0F);
        }
    }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public String getName() { return "tile.cropmanager.crop_database_chest.name"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }
    @Override public void clear() { for (int i = 0; i < getSizeInventory(); i++) seeds.setStackInSlot(i, ItemStack.EMPTY); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); tag.setTag("Seeds", seeds.serializeNBT()); tag.removeTag("Management"); if (hasManagement()) tag.setTag("Settings", settings.write()); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); seeds.deserializeNBT(tag.getCompoundTag("Seeds")); if (hasManagement()) settings.read(tag.hasKey("Settings", 10) ? tag.getCompoundTag("Settings") : tag); }
}
