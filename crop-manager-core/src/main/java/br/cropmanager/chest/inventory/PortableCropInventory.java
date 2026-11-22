package br.cropmanager.chest.inventory;

import br.cropmanager.chest.item.ItemPortableCropDatabase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.HashSet;
import java.util.Set;

/** Reuses the chest container without registering a world TileEntity or changing Seed Bag NBT. */
public final class PortableCropInventory extends TileCropDatabaseChest {
    public static final String DATA = "PortableCropDatabase";
    private final EntityPlayer owner;
    private final int carrierSlot;
    private final ItemStack carrier;
    private boolean pickupEnabled;
    private final Set<String> favorites = new HashSet<>();
    private boolean loading = true;

    private PortableCropInventory(EntityPlayer owner, int carrierSlot) {
        super(500);
        this.owner = owner;
        this.carrierSlot = carrierSlot;
        carrier = owner.inventory.getStackInSlot(carrierSlot);
        setWorld(owner.world);
        NBTTagCompound data = carrier.hasTagCompound() ? carrier.getTagCompound().getCompoundTag(DATA) : new NBTTagCompound();
        seeds().deserializeNBT(data.getCompoundTag("Seeds"));
        pickupEnabled = data.getBoolean("Pickup");
        net.minecraft.nbt.NBTTagList list = data.getTagList("Favorites", 8);
        for (int i = 0; i < Math.min(SeedInventory.SIZE, list.tagCount()); i++) {
            String identity = list.getStringTagAt(i);
            if (identity.startsWith("bag:") && identity.length() <= 4096) favorites.add(identity);
        }
        loading = false;
    }

    public static PortableCropInventory open(EntityPlayer player, int slot) {
        if (slot < 0 || (slot >= 36 && slot != 40)) return null;
        ItemStack stack = player.inventory.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getCount() == 1 && stack.getItem() instanceof ItemPortableCropDatabase
                ? new PortableCropInventory(player, slot) : null;
    }
    @Override public boolean isPortable() { return true; }
    @Override public boolean isCarrierSlot(int index) { return index == carrierSlot; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        if (player != owner || isInvalid()) return false;
        ItemStack stack = owner.inventory.getStackInSlot(carrierSlot);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPortableCropDatabase)) return false;
        if (world != null && world.isRemote) return true;
        return stack == carrier;
    }
    public boolean isPickupEnabled() { return pickupEnabled; }
    public void setPickupEnabled(boolean enabled) {
        if (pickupEnabled != enabled) { pickupEnabled = enabled; markDirty(); }
    }
    public static boolean isPickupEnabled(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && stack.getItem() instanceof ItemPortableCropDatabase
                && stack.hasTagCompound() && stack.getTagCompound().getCompoundTag(DATA).getBoolean("Pickup");
    }
    @Override public boolean isFavorite(ItemStack stack) {
        return SeedInventory.accepts(stack) && favorites.contains(favoriteIdentity(stack));
    }
    public void toggleFavorite(ItemStack stack) {
        if (!SeedInventory.accepts(stack)) return;
        String identity = favoriteIdentity(stack);
        if (!favorites.remove(identity) && favorites.size() < SeedInventory.SIZE) favorites.add(identity);
        markDirty();
    }
    private static String favoriteIdentity(ItemStack stack) { return "bag:" + stack.serializeNBT().toString(); }
    @Override public void markDirty() {
        if (loading || carrier == null || world == null || world.isRemote || !isUsableByPlayer(owner)) return;
        ItemStack current = owner.inventory.getStackInSlot(carrierSlot);
        if (current.isEmpty() || !(current.getItem() instanceof ItemPortableCropDatabase)) return;
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Seeds", seeds().serializeNBT());
        data.setBoolean("Pickup", pickupEnabled);
        net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        for (String identity : favorites) list.appendTag(new net.minecraft.nbt.NBTTagString(identity));
        data.setTag("Favorites", list);
        NBTTagCompound root = current.hasTagCompound() ? current.getTagCompound() : new NBTTagCompound();
        root.setTag(DATA, data);
        current.setTagCompound(root);
        if (current != carrier) carrier.setTagCompound(root);
        owner.inventory.markDirty();
    }
    @Override public String getName() { return "item.cropmanager.portable_crop_database.name"; }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public void update() { }
}
