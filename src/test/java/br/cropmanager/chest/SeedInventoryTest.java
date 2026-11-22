package br.cropmanager.chest;

import br.cropmanager.chest.integration.CropInformation;
import br.cropmanager.chest.integration.ProductionInformation;
import br.cropmanager.chest.inventory.SeedInventory;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SeedInventoryTest {
    @BeforeClass public static void setup() { SeedFixtures.bootstrap(); }

    @Test public void acceptsSeedsAtEveryAnalysisLevelAndRejectsOtherItems() {
        SeedInventory storage = new SeedInventory();
        for (int scan = 0; scan <= 4; scan++) assertTrue(storage.insertItem(scan, SeedFixtures.bag(scan, scan), false).isEmpty());
        ItemStack stone = new ItemStack(Items.STICK);
        assertEquals(stone, storage.insertItem(5, stone, false));
        assertTrue(storage.getStackInSlot(5).isEmpty());
    }

    @Test public void fillsAllPagesAndReturnsOverflowWithoutChangingInput() {
        SeedInventory storage = new SeedInventory();
        for (int slot = 0; slot < 500; slot++) {
            ItemStack seed = SeedFixtures.bag(slot, 4);
            NBTTagCompound before = seed.serializeNBT();
            assertTrue(storage.insertAnywhere(seed, false).isEmpty());
            assertEquals(before, seed.serializeNBT());
        }
        ItemStack overflow = SeedFixtures.bag(126, 4);
        assertEquals(overflow.serializeNBT(), storage.insertAnywhere(overflow, false).serializeNBT());
        assertEquals(499, storage.getStackInSlot(499).getTagCompound().getInteger("identity"));
    }

    @Test public void legacy250ChestExpandsToTierThreeWithoutLosingItsSeeds() {
        br.cropmanager.chest.inventory.TileCropDatabaseChest250 tile = new br.cropmanager.chest.inventory.TileCropDatabaseChest250();
        for (int slot = 0; slot < 250; slot++) assertTrue(tile.seeds().insertAnywhere(SeedFixtures.bag(slot, 4), false).isEmpty());
        assertEquals(500, tile.getSizeInventory());
        for (EnumFacing face : EnumFacing.values()) {
            assertEquals(500, tile.getSlotsForFace(face).length);
            assertTrue(tile.canInsertItem(499, SeedFixtures.bag(1, 4), face));
        }
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        assertEquals("cropdatabasechest:chest_250", saved.getString("id"));
        saved.getCompoundTag("Seeds").setInteger("Size", 250);
        br.cropmanager.chest.inventory.TileCropDatabaseChest250 loaded = new br.cropmanager.chest.inventory.TileCropDatabaseChest250();
        loaded.readFromNBT(saved);
        assertEquals(500, loaded.seeds().getSlots());
        for (int slot = 0; slot < 250; slot++) assertEquals(SeedFixtures.bag(slot, 4).serializeNBT(), loaded.getStackInSlot(slot).serializeNBT());
        for (int slot = 250; slot < 500; slot++) assertTrue(loaded.getStackInSlot(slot).isEmpty());
        loaded.seeds().sort(br.cropmanager.chest.inventory.SeedSortMode.ATTRIBUTES);
        assertEquals(500, loaded.seeds().getSlots());
        assertTrue(loaded.seeds().insertItem(499, SeedFixtures.bag(499, 4), false).isEmpty());
    }

    @Test public void legacy125ChestExpandsToTierThreeAndRetainsItsOldTileId() {
        br.cropmanager.chest.inventory.TileCropDatabaseChest125 tile = new br.cropmanager.chest.inventory.TileCropDatabaseChest125();
        for (int slot = 0; slot < 125; slot++) assertTrue(tile.seeds().insertAnywhere(SeedFixtures.bag(slot, 4), false).isEmpty());
        assertEquals(500, tile.getSizeInventory());
        assertEquals(500, tile.getSlotsForFace(EnumFacing.UP).length);
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        saved.getCompoundTag("Seeds").setInteger("Size", 125);
        net.minecraft.tileentity.TileEntity loaded = net.minecraft.tileentity.TileEntity.create(null, saved);
        assertTrue(loaded instanceof br.cropmanager.chest.inventory.TileCropDatabaseChest125);
        assertEquals(500, ((net.minecraft.inventory.IInventory) loaded).getSizeInventory());
        for (int slot = 0; slot < 125; slot++) assertEquals(SeedFixtures.bag(slot, 4).serializeNBT(), ((net.minecraft.inventory.IInventory) loaded).getStackInSlot(slot).serializeNBT());
        for (int slot = 125; slot < 500; slot++) assertTrue(((net.minecraft.inventory.IInventory) loaded).getStackInSlot(slot).isEmpty());
        tile.seeds().sort(br.cropmanager.chest.inventory.SeedSortMode.TYPE);
        assertEquals(500, tile.seeds().getSlots());
        tile.clear();
        assertTrue(tile.isEmpty());
    }

    @Test public void simulationAndSlotLimitDoNotConsumeItems() {
        SeedInventory storage = new SeedInventory();
        ItemStack stack = SeedFixtures.bag(1, 0);
        stack.setCount(3);
        assertEquals(2, storage.insertItem(0, stack, true).getCount());
        assertTrue(storage.getStackInSlot(0).isEmpty());
        assertEquals(2, storage.insertItem(0, stack, false).getCount());
        assertEquals(1, storage.getStackInSlot(0).getCount());
        assertEquals(3, stack.getCount());
    }

    @Test public void consumingPlayerStackCannotEraseStoredBag() {
        SeedInventory storage = new SeedInventory();
        ItemStack playerStack = SeedFixtures.bag(1, 4);
        assertTrue(storage.insertAnywhere(playerStack, false).isEmpty());
        playerStack.setCount(0);
        assertEquals(1, storage.getStackInSlot(0).getCount());
    }

    @Test public void tileRoundTripPreservesAll500CompleteItemStacks() {
        TileCropDatabaseChest original = new TileCropDatabaseChest();
        for (int slot = 0; slot < 500; slot++) original.setInventorySlotContents(slot, SeedFixtures.bag(slot, slot % 5));
        NBTTagCompound saved = original.writeToNBT(new NBTTagCompound());
        TileCropDatabaseChest loaded = new TileCropDatabaseChest();
        loaded.readFromNBT(saved);
        assertEquals(500, loaded.getSizeInventory());
        for (int slot = 0; slot < 500; slot++) assertEquals(original.getStackInSlot(slot).serializeNBT(), loaded.getStackInSlot(slot).serializeNBT());
        assertEquals(saved, loaded.writeToNBT(new NBTTagCompound()));
    }

    @Test public void extractAndClearDoNotDuplicateSeeds() {
        TileCropDatabaseChest tile = new TileCropDatabaseChest();
        tile.setInventorySlotContents(499, SeedFixtures.bag(499, 4));
        assertEquals(1, tile.removeStackFromSlot(499).getCount());
        assertTrue(tile.removeStackFromSlot(499).isEmpty());
        tile.setInventorySlotContents(0, SeedFixtures.bag(0, 0));
        tile.clear();
        assertTrue(tile.isEmpty());
    }

    @Test public void chunkAnimationTagHasCoordinatesAndDoesNotReplaceTheInventory() {
        TileCropDatabaseChest tile = new TileCropDatabaseChest();
        tile.setPos(new net.minecraft.util.math.BlockPos(10, 64, -20));
        tile.setInventorySlotContents(0, SeedFixtures.bag(1, 4));
        NBTTagCompound tag = tile.getUpdateTag();
        assertEquals(10, tag.getInteger("x"));
        assertEquals(64, tag.getInteger("y"));
        assertEquals(-20, tag.getInteger("z"));
        assertFalse(tag.hasKey("Seeds"));
        tag.setInteger("Viewers", 2);
        tile.handleUpdateTag(tag);
        assertFalse(tile.getStackInSlot(0).isEmpty());
        assertEquals(2, tile.getUpdateTag().getInteger("Viewers"));
    }

    @Test public void persistedSizeCannotResizeStorage() {
        SeedInventory storage = new SeedInventory();
        NBTTagCompound saved = storage.serializeNBT();
        saved.setInteger("Size", 1000);
        storage.deserializeNBT(saved);
        assertEquals(500, storage.getSlots());
    }

    @Test public void loadsLegacy125SlotsWithoutLosingSeeds() {
        SeedInventory legacy = new SeedInventory();
        for (int slot = 0; slot < 125; slot++) legacy.setStackInSlot(slot, SeedFixtures.bag(slot, slot % 5));
        NBTTagCompound saved = legacy.serializeNBT();
        saved.setInteger("Size", 125);
        SeedInventory expanded = new SeedInventory();
        expanded.deserializeNBT(saved);
        assertEquals(500, expanded.getSlots());
        assertEquals(20, SeedInventory.PAGES);
        for (int slot = 0; slot < 125; slot++) {
            assertEquals(legacy.getStackInSlot(slot).serializeNBT(), expanded.getStackInSlot(slot).serializeNBT());
        }
        for (int slot = 125; slot < 500; slot++) assertTrue(expanded.getStackInSlot(slot).isEmpty());
        assertTrue(expanded.insertItem(499, SeedFixtures.bag(499, 4), false).isEmpty());
    }

    @Test public void automationExposesAllSlotsAndFiltersEveryFace() {
        TileCropDatabaseChest tile = new TileCropDatabaseChest();
        for (EnumFacing face : EnumFacing.values()) {
            assertEquals(500, tile.getSlotsForFace(face).length);
            assertEquals(499, tile.getSlotsForFace(face)[499]);
            assertTrue(tile.canInsertItem(0, SeedFixtures.bag(0, 0), face));
            assertFalse(tile.canInsertItem(0, new ItemStack(Items.STICK), face));
            assertTrue(tile.canExtractItem(0, SeedFixtures.bag(0, 0), face));
            assertFalse(tile.canExtractItem(500, ItemStack.EMPTY, face));
            // FML injects the actual capability during real server startup (covered by the probe).
        }
    }

    @Test public void analysisLevelsMatchIC2WithoutModifyingNbt() {
        for (int scan = 0; scan <= 4; scan++) {
            ItemStack bag = SeedFixtures.bag(1, scan);
            NBTTagCompound before = bag.serializeNBT();
            CropInformation info = CropInformation.read(bag);
            assertEquals(scan >= 2 ? 6 : -1, info.tier);
            assertEquals(scan >= 4 ? 21 : -1, info.growth);
            assertEquals(scan >= 4 ? 31 : -1, info.gain);
            assertEquals(scan >= 4 ? 0 : -1, info.resistance);
            assertEquals(scan == 0 ? "cropdatabasechest.unknown" : SeedFixtures.CROP.getUnlocalizedName(), info.nameKey);
            assertEquals(scan == 0 ? ProductionInformation.Status.HIDDEN : ProductionInformation.Status.UNAVAILABLE, info.production.status);
            assertEquals(before, bag.serializeNBT());
        }
    }
}
