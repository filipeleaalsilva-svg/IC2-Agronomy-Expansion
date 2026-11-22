package br.cropmanager.chest;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropSeed;
import ic2.api.crops.ICropTile;
import java.util.Collections;
import java.util.List;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;

public final class SeedFixtures {
    public static SeedItem item;
    public static br.cropmanager.chest.item.ItemPortableCropDatabase portable;
    public static final CropCard CROP = new CropCard() {
        @Override public String getId() { return "ferru"; }
        @Override public String getOwner() { return "test"; }
        @Override public CropProperties getProperties() { return new CropProperties(6, 2, 0, 0, 1, 0); }
        @Override public int getMaxSize() { return 4; }
        @Override public List<ResourceLocation> getTexturesLocation() { return Collections.emptyList(); }
        @Override public ItemStack getGain(ICropTile tile) { throw new AssertionError("Unknown implementations must not be harvested"); }
    };

    public static void bootstrap() {
        if (item != null) return;
        Bootstrap.register();
        item = new SeedItem();
        item.setRegistryName("cropdatabasechest_test", "seed");
        ForgeRegistries.ITEMS.register(item);
        portable = new br.cropmanager.chest.item.ItemPortableCropDatabase();
        ForgeRegistries.ITEMS.register(portable);
        GameRegistry.registerTileEntity(TileCropDatabaseChest.class, new ResourceLocation("cropdatabasechest", "chest"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest250.class,
                new ResourceLocation("cropdatabasechest", "chest_250"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest125.class,
                new ResourceLocation("cropdatabasechest", "chest_125"));
    }

    public static ItemStack bag(int identity, int scan) {
        ItemStack bag = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("owner", "test");
        tag.setString("id", "ferru");
        tag.setInteger("identity", identity);
        tag.setByte("scan", (byte) scan);
        tag.setByte("growth", (byte) 21);
        tag.setByte("gain", (byte) 31);
        tag.setByte("resistance", (byte) 0);
        NBTTagCompound custom = new NBTTagCompound();
        custom.setLong("unrelated", 9876543210L);
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("keep nested data"));
        custom.setTag("list", list);
        tag.setTag("custom", custom);
        bag.setTagCompound(tag);
        return bag;
    }

    public static final class SeedItem extends Item implements ICropSeed {
        SeedItem() { setMaxStackSize(1); }
        @Override public CropCard getCropFromStack(ItemStack stack) { return CROP; }
        @Override public void setCropFromStack(ItemStack stack, CropCard crop) { throw new AssertionError("read only"); }
        @Override public int getGrowthFromStack(ItemStack stack) { return stack.getTagCompound().getByte("growth"); }
        @Override public int getGainFromStack(ItemStack stack) { return stack.getTagCompound().getByte("gain"); }
        @Override public int getResistanceFromStack(ItemStack stack) { return stack.getTagCompound().getByte("resistance"); }
        @Override public int getScannedFromStack(ItemStack stack) { return stack.getTagCompound().getByte("scan"); }
        @Override public void setGrowthFromStack(ItemStack stack, int value) { throw new AssertionError("read only"); }
        @Override public void setGainFromStack(ItemStack stack, int value) { throw new AssertionError("read only"); }
        @Override public void setResistanceFromStack(ItemStack stack, int value) { throw new AssertionError("read only"); }
        @Override public void setScannedFromStack(ItemStack stack, int value) { throw new AssertionError("read only"); }
        @Override public void incrementScannedFromStack(ItemStack stack) { throw new AssertionError("read only"); }
    }
}
