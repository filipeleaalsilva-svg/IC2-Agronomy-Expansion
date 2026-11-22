package br.cropmanager.chest.inventory;

import br.cropmanager.ManagerConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.AxisAlignedBB;

/** Settings for the tier 3 chest. Seed Bag NBT is never modified. */
public final class ChestSettings {
    private final TileCropDatabaseChest owner;
    private final Set<String> favorites = new HashSet<>();
    private boolean collecting;
    private int redstoneMode;

    ChestSettings(TileCropDatabaseChest owner) { this.owner = owner; }
    public boolean collecting() { return collecting; }
    public int redstoneMode() { return redstoneMode; }
    public void setCollecting(boolean enabled) { if (collecting != enabled) { collecting = enabled; owner.changed(); } }
    public void setRedstoneMode(int mode) {
        if (mode < 0 || mode > 2) return;
        if (redstoneMode != mode) { redstoneMode = mode; owner.changed(); }
    }
    public boolean automationEnabled() {
        if (owner.getWorld() == null || redstoneMode == 0) return true;
        boolean signal = owner.getWorld().isBlockPowered(owner.getPos());
        return redstoneMode == 1 ? !signal : signal;
    }
    private static String identity(ItemStack stack) {
        if (!SeedInventory.accepts(stack)) return "";
        return "bag:" + stack.serializeNBT().toString();
    }
    public boolean isFavorite(ItemStack stack) { String id = identity(stack); return !id.isEmpty() && favorites.contains(id); }
    public void toggleFavorite(int slot) {
        if (!owner.isSeedSlotEnabled(slot)) return;
        String id = identity(owner.getStackInSlot(slot));
        if (id.isEmpty()) return;
        if (!favorites.remove(id) && favorites.size() < SeedInventory.SIZE) favorites.add(id);
        owner.changed();
    }
    public void deposit(EntityPlayer player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (SeedInventory.accepts(stack)) player.inventory.setInventorySlotContents(i, owner.seeds().insertAnywhere(stack, false));
        }
        player.inventory.markDirty();
    }
    public void tick() {
        if (!collecting || !automationEnabled() || owner.getWorld().getTotalWorldTime() % ManagerConfig.chestCollectIntervalTicks != 0) return;
        int examined = 0;
        for (EntityItem item : owner.getWorld().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(owner.getPos()).grow(ManagerConfig.chestCollectRadius))) {
            if (examined++ >= ManagerConfig.chestCollectMaxItems) break;
            if (item.isDead || item.cannotPickup() || item.getOwner() != null || !SeedInventory.accepts(item.getItem())) continue;
            ItemStack remainder = owner.seeds().insertAnywhere(item.getItem(), false);
            if (remainder.isEmpty()) item.setDead();
            else if (remainder.getCount() != item.getItem().getCount()) item.setItem(remainder);
        }
    }
    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound(); NBTTagList list = new NBTTagList();
        for (String id : favorites) list.appendTag(new NBTTagString(id));
        tag.setTag("Favorites", list); tag.setBoolean("Collecting", collecting); tag.setInteger("Redstone", redstoneMode);
        return tag;
    }
    public void read(NBTTagCompound tag) {
        favorites.clear(); NBTTagList list = tag.getTagList("Favorites", 8);
        for (int i = 0; i < Math.min(SeedInventory.SIZE, list.tagCount()); i++) {
            String id = list.getStringTagAt(i);
            if (id.startsWith("bag:") && id.length() <= 4096) favorites.add(id);
        }
        collecting = tag.getBoolean("Collecting"); redstoneMode = Math.max(0, Math.min(2, tag.getInteger("Redstone")));
    }
}
