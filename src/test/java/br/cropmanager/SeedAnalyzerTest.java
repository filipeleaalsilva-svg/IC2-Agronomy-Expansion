package br.cropmanager;

import br.cropmanager.crop.CropRegistry;
import ic2.api.crops.CropCard;
import ic2.api.crops.ICropSeed;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public final class SeedAnalyzerTest {
    private static FakeSeedItem FAKE_SEED;

    private static final class FakeSeedItem extends Item implements ICropSeed {
        @Override public CropCard getCropFromStack(ItemStack stack) { return CropRegistry.IRID_MOSS; }
        @Override public void setCropFromStack(ItemStack stack, CropCard crop) {}
        @Override public int getGrowthFromStack(ItemStack stack) { return tag(stack).getInteger("gr"); }
        @Override public void setGrowthFromStack(ItemStack stack, int v) { tag(stack).setInteger("gr", v); }
        @Override public int getGainFromStack(ItemStack stack) { return tag(stack).getInteger("ga"); }
        @Override public void setGainFromStack(ItemStack stack, int v) { tag(stack).setInteger("ga", v); }
        @Override public int getResistanceFromStack(ItemStack stack) { return tag(stack).getInteger("re"); }
        @Override public void setResistanceFromStack(ItemStack stack, int v) { tag(stack).setInteger("re", v); }
        @Override public int getScannedFromStack(ItemStack stack) { return tag(stack).getInteger("scan"); }
        @Override public void setScannedFromStack(ItemStack stack, int v) { tag(stack).setInteger("scan", v); }
        @Override public void incrementScannedFromStack(ItemStack stack) { setScannedFromStack(stack, getScannedFromStack(stack) + 1); }
        private static NBTTagCompound tag(ItemStack s) {
            if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
            return s.getTagCompound();
        }
    }

    @BeforeClass
    public static void boot() {
        Bootstrap.register();
        TileEntity.register("cropmanager:seed_analyzer", SeedAnalyzerTile.class);
        FAKE_SEED = new FakeSeedItem();
    }

    private static ItemStack makeSeed(int scan, int gr, int ga, int re) {
        ItemStack s = new ItemStack(FAKE_SEED);
        FAKE_SEED.setScannedFromStack(s, scan);
        FAKE_SEED.setGrowthFromStack(s, gr);
        FAKE_SEED.setGainFromStack(s, ga);
        FAKE_SEED.setResistanceFromStack(s, re);
        return s;
    }

    @Test
    public void analyzesUnscannedSeedToLevelFourAndRecordsTelemetry() {
        SeedAnalyzerTile tile = new SeedAnalyzerTile();
        tile.setWorld(Mockito.mock(World.class));
        tile.energy.setEnergyStored(10000);
        tile.inventory.setStackInSlot(0, makeSeed(0, 12, 18, 9));

        for (int i = 0; i < ManagerConfig.analyzerDuration; i++) {
            assertTrue(tile.processTick());
        }

        assertTrue(tile.inventory.getStackInSlot(0).isEmpty());
        ItemStack out = tile.inventory.getStackInSlot(1);
        assertFalse(out.isEmpty());
        assertEquals(4, FAKE_SEED.getScannedFromStack(out));
        assertEquals("cropmanager:irid_moss", tile.lastSpecies);
        assertEquals(8, tile.lastTier);
        assertEquals(12, tile.lastGr);
        assertEquals(18, tile.lastGa);
        assertEquals(9, tile.lastRe);
        assertTrue(tile.lastCatalyzed);
        assertEquals("analyzer_ready", tile.condition);
    }

    @Test
    public void passesAlreadyScannedSeedWithoutSpendingEnergy() {
        SeedAnalyzerTile tile = new SeedAnalyzerTile();
        tile.energy.setEnergyStored(0);
        tile.inventory.setStackInSlot(0, makeSeed(4, 5, 6, 7));

        assertFalse(tile.processTick());
        assertEquals(0, tile.energy.getEnergyStored(), 0.01);
        assertFalse(tile.inventory.getStackInSlot(1).isEmpty());
        assertEquals(5, tile.lastGr);
        assertEquals("analyzer_ready", tile.condition);
    }

    @Test
    public void automationOnlyInsertsUnscannedAndExtractsOutput() {
        SeedAnalyzerTile tile = new SeedAnalyzerTile();
        IItemHandler handler = tile.itemsForFace(EnumFacing.NORTH);
        assertNotNull(handler);

        ItemStack scanned = makeSeed(4, 1, 1, 1);
        assertFalse("Automation must reject already-scanned seeds", handler.insertItem(0, scanned, false).isEmpty());

        ItemStack unscanned = makeSeed(1, 2, 3, 4);
        assertTrue("Automation must accept unscanned seeds", handler.insertItem(0, unscanned, false).isEmpty());
        assertTrue("Cannot extract from input slot", handler.extractItem(0, 1, false).isEmpty());

        tile.inventory.setStackInSlot(1, scanned.copy());
        assertFalse("Can extract from output slot", handler.extractItem(1, 1, false).isEmpty());
    }

    @Test
    public void persistsTelemetryAndProgressInNbt() {
        SeedAnalyzerTile tile = new SeedAnalyzerTile();
        tile.energy.setEnergyStored(5000);
        tile.lastSpecies = "cropmanager:lazuli_bloom";
        tile.lastTier = 5;
        tile.lastGr = 10;
        tile.lastGa = 11;
        tile.lastRe = 12;
        tile.lastCatalyzed = true;
        tile.lastAttributes = "Blue, Crystal, Catalyzed";
        tile.progress = 25;

        NBTTagCompound tag = tile.writeToNBT(new NBTTagCompound());
        SeedAnalyzerTile loaded = new SeedAnalyzerTile();
        loaded.readFromNBT(tag);

        assertEquals(5000, loaded.energy.getEnergyStored(), 0.01);
        assertEquals("cropmanager:lazuli_bloom", loaded.lastSpecies);
        assertEquals(5, loaded.lastTier);
        assertEquals(10, loaded.lastGr);
        assertEquals(11, loaded.lastGa);
        assertEquals(12, loaded.lastRe);
        assertTrue(loaded.lastCatalyzed);
        assertEquals("Blue, Crystal, Catalyzed", loaded.lastAttributes);
        assertEquals(25, loaded.progress);
    }
}
