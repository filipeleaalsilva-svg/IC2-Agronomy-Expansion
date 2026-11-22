package br.cropmanager.chest;

import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.SeedInventory;
import br.cropmanager.chest.inventory.SeedSortMode;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropSeed;
import ic2.api.crops.ICropTile;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SortTest {
    private static SortSeed item;

    @BeforeClass public static void bootstrap() {
        SeedFixtures.bootstrap();
        item = new SortSeed();
        item.setRegistryName("cropdatabasechest_test", "sort_seed");
        ForgeRegistries.ITEMS.register(item);
    }

    private static ItemStack bag(int identity, String type, int tier, int scan, int gain, int growth, int resistance) {
        ItemStack result = new ItemStack(item);
        NBTTagCompound tag = SeedFixtures.bag(identity, scan).getTagCompound().copy();
        tag.setString("id", type);
        tag.setInteger("tier", tier);
        tag.setByte("gain", (byte) gain);
        tag.setByte("growth", (byte) growth);
        tag.setByte("resistance", (byte) resistance);
        result.setTagCompound(tag);
        return result;
    }

    private static void assertOrder(SeedInventory storage, int... identities) {
        for (int i = 0; i < identities.length; i++) {
            assertEquals("slot " + i, identities[i], storage.getStackInSlot(i).getTagCompound().getInteger("identity"));
        }
        for (int i = identities.length; i < SeedInventory.SIZE; i++) assertTrue(storage.getStackInSlot(i).isEmpty());
    }

    @Test public void typeGroupsByCropIdentityAndKeepsUnanalysedLast() {
        SeedInventory storage = new SeedInventory();
        storage.setStackInSlot(499, bag(1, "zinc", 1, 1, 0, 0, 0));
        storage.setStackInSlot(3, bag(2, "aloe", 1, 1, 0, 0, 0));
        storage.setStackInSlot(1, bag(3, "hidden", 99, 0, 31, 31, 0));
        storage.setStackInSlot(5, bag(4, "aloe", 1, 1, 0, 0, 0));
        storage.sort(SeedSortMode.TYPE);
        assertOrder(storage, 2, 4, 1, 3);
    }

    @Test public void tierSortsDescendingAndDoesNotReadHiddenTier() {
        SeedInventory storage = new SeedInventory();
        storage.setStackInSlot(0, bag(1, "a", 99, 1, 31, 31, 0));
        storage.setStackInSlot(1, bag(2, "b", 2, 2, 0, 0, 0));
        storage.setStackInSlot(2, bag(3, "c", 8, 2, 0, 0, 0));
        storage.sort(SeedSortMode.TIER);
        assertOrder(storage, 3, 2, 1);
    }

    @Test public void attributesPrioritizeGainThenGrowthThenLowerResistance() {
        SeedInventory storage = new SeedInventory();
        storage.setStackInSlot(0, bag(1, "a", 1, 4, 20, 30, 0));
        storage.setStackInSlot(1, bag(2, "a", 1, 4, 21, 10, 5));
        storage.setStackInSlot(2, bag(3, "a", 1, 4, 21, 11, 5));
        storage.setStackInSlot(3, bag(4, "a", 1, 4, 21, 11, 0));
        storage.setStackInSlot(4, bag(5, "a", 1, 3, 31, 31, 0));
        storage.sort(SeedSortMode.ATTRIBUTES);
        assertOrder(storage, 4, 3, 2, 1, 5);
    }

    @Test public void insufficientAnalysisKeepsItsOriginalRelativeOrderInEveryMode() {
        for (SeedSortMode mode : SeedSortMode.values()) {
            SeedInventory storage = new SeedInventory();
            storage.setStackInSlot(2, bag(1, "z", 10, 0, 31, 31, 0));
            storage.setStackInSlot(4, bag(2, "a", 1, 0, 0, 0, 31));
            storage.setStackInSlot(8, bag(3, "m", 4, 4, 5, 5, 5));
            storage.sort(mode);
            assertOrder(storage, 3, 1, 2);
        }
    }

    @Test public void sortingAll500PreservesEveryCompleteStackAndIsStableOnTies() {
        SeedInventory storage = new SeedInventory();
        Map<Integer, NBTTagCompound> before = new HashMap<>();
        for (int i = 0; i < SeedInventory.SIZE; i++) {
            ItemStack stack = bag(i, "crop" + (i % 7), i % 9, 4, i % 31, (i * 3) % 31, i % 5);
            storage.setStackInSlot(i, stack);
            before.put(i, stack.serializeNBT());
        }
        for (SeedSortMode mode : SeedSortMode.values()) {
            storage.sort(mode);
            Map<Integer, NBTTagCompound> after = new HashMap<>();
            for (int i = 0; i < SeedInventory.SIZE; i++) {
                ItemStack stack = storage.getStackInSlot(i);
                int id = stack.getTagCompound().getInteger("identity");
                assertNull("Duplicate identity", after.put(id, stack.serializeNBT()));
            }
            assertEquals(before, after);
            NBTTagCompound firstSort = storage.serializeNBT();
            storage.sort(mode);
            assertEquals(firstSort, storage.serializeNBT());
        }
        SeedInventory ties = new SeedInventory();
        ties.setStackInSlot(2, bag(1, "same", 5, 4, 7, 7, 7));
        ties.setStackInSlot(20, bag(2, "same", 5, 4, 7, 7, 7));
        for (SeedSortMode mode : SeedSortMode.values()) {
            ties.sort(mode);
            assertOrder(ties, 1, 2);
        }
    }

    @Test public void serverSortResetsRequestersPageSynchronizesAndBlocksUnsafeRequests() {
        World world = mock(World.class);
        EntityPlayer player = mock(EntityPlayer.class);
        player.world = world;
        player.inventory = new InventoryPlayer(player);
        TileCropDatabaseChest tile = new TileCropDatabaseChest();
        tile.setWorld(world);
        tile.setPos(BlockPos.ORIGIN);
        when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(tile);
        when(world.getBlockState(BlockPos.ORIGIN)).thenReturn(Blocks.CHEST.getDefaultState());
        ContainerCropDatabaseChest container = new ContainerCropDatabaseChest(player.inventory, tile);
        IContainerListener listener = mock(IContainerListener.class);
        container.addListener(listener);
        tile.setInventorySlotContents(499, bag(1, "a", 1, 4, 5, 5, 5));
        assertTrue(container.enchantItem(player, 10));
        clearInvocations(listener);
        assertTrue(container.enchantItem(player, 100));
        assertEquals(0, container.getPage());
        assertEquals(1, tile.getStackInSlot(0).getTagCompound().getInteger("identity"));
        verify(listener).sendWindowProperty(container, 0, 0);
        verify(listener, atLeastOnce()).sendSlotContents(eq(container), eq(0), any(ItemStack.class));
        NBTTagCompound sorted = tile.writeToNBT(new NBTTagCompound());
        player.inventory.setItemStack(SeedFixtures.bag(99, 4));
        assertFalse(container.enchantItem(player, 101));
        player.inventory.setItemStack(ItemStack.EMPTY);
        container.slotClick(-999, 0, ClickType.QUICK_CRAFT, player);
        assertFalse(container.enchantItem(player, 102));
        container.slotClick(-999, 2, ClickType.QUICK_CRAFT, player);
        assertFalse(container.enchantItem(player, 103));
        when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(null);
        assertFalse(container.enchantItem(player, 100));
        assertEquals(sorted, tile.writeToNBT(new NBTTagCompound()));
    }

    private static final class SortSeed extends Item implements ICropSeed {
        private NBTTagCompound tag(ItemStack stack) { return stack.getTagCompound(); }
        @Override public int getScannedFromStack(ItemStack stack) { return tag(stack).getByte("scan"); }
        @Override public CropCard getCropFromStack(ItemStack stack) {
            if (getScannedFromStack(stack) < 1) throw new AssertionError("Read hidden crop identity");
            final NBTTagCompound tag = tag(stack);
            return new CropCard() {
                @Override public String getId() { return tag.getString("id"); }
                @Override public String getOwner() { return tag.getString("owner"); }
                @Override public CropProperties getProperties() {
                    if (tag.getByte("scan") < 2) throw new AssertionError("Read hidden tier");
                    return new CropProperties(tag.getInteger("tier"), 0, 0, 0, 0, 0);
                }
                @Override public int getMaxSize() { return 4; }
                @Override public List<ResourceLocation> getTexturesLocation() { return Collections.emptyList(); }
                @Override public ItemStack getGain(ICropTile tile) { throw new AssertionError("Sorting queried production"); }
            };
        }
        private int stat(ItemStack stack, String key) {
            if (getScannedFromStack(stack) < 4) throw new AssertionError("Read hidden attribute");
            return tag(stack).getByte(key);
        }
        @Override public int getGrowthFromStack(ItemStack stack) { return stat(stack, "growth"); }
        @Override public int getGainFromStack(ItemStack stack) { return stat(stack, "gain"); }
        @Override public int getResistanceFromStack(ItemStack stack) { return stat(stack, "resistance"); }
        @Override public void setCropFromStack(ItemStack stack, CropCard crop) { throw new AssertionError("Sorting changed seed"); }
        @Override public void setGrowthFromStack(ItemStack stack, int value) { throw new AssertionError("Sorting changed seed"); }
        @Override public void setGainFromStack(ItemStack stack, int value) { throw new AssertionError("Sorting changed seed"); }
        @Override public void setResistanceFromStack(ItemStack stack, int value) { throw new AssertionError("Sorting changed seed"); }
        @Override public void setScannedFromStack(ItemStack stack, int value) { throw new AssertionError("Sorting changed seed"); }
        @Override public void incrementScannedFromStack(ItemStack stack) { throw new AssertionError("Sorting changed seed"); }
    }
}
