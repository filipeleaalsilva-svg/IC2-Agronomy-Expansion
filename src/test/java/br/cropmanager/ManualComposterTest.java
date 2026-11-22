package br.cropmanager;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public final class ManualComposterTest {
    @BeforeClass
    public static void boot() {
        Bootstrap.register();
        TileEntity.register("cropmanager:manual_composter", ManualComposterTile.class);
    }

    @Test
    public void rejectsNonOrganicAndAcceptsOrganicUntilFull() {
        ManualComposterTile tile = new ManualComposterTile();
        assertFalse(tile.addOrganic(new ItemStack(Items.IRON_INGOT)));
        assertEquals(0, tile.itemCount());
        assertEquals(0, tile.visualFill());

        for (int i = 0; i < ManagerConfig.manualComposterRequiredItems; i++) {
            assertTrue("Should accept organic item #" + i, tile.addOrganic(new ItemStack(Items.WHEAT)));
        }
        assertTrue(tile.isFermenting());
        assertEquals(4, tile.visualFill());
        assertFalse("Full composter must not accept more items", tile.addOrganic(new ItemStack(Items.WHEAT)));
    }

    @Test
    public void hydrationDoublesCompostingSpeedAndProducesOrganicCompost() {
        ManualComposterTile tile = new ManualComposterTile();
        World serverWorld = Mockito.mock(World.class);
        tile.setWorld(serverWorld);
        tile.duration = 20;

        for (int i = 0; i < ManagerConfig.manualComposterRequiredItems; i++) {
            tile.addOrganic(new ItemStack(CropManager.BIO_FIBER));
        }
        assertTrue(tile.hydrate());
        assertFalse("Second hydration should be ignored", tile.hydrate());

        // 20 ticks duration with 2x water multiplier finishes in 10 ticks
        for (int t = 0; t < 10; t++) tile.update();
        assertTrue(tile.isReady());
        assertEquals(5, tile.visualFill());

        ItemStack out = tile.collectCompost();
        assertEquals(CropManager.ORGANIC_COMPOST, out.getItem());
        assertEquals(ManagerConfig.manualComposterOutputCount, out.getCount());
        assertFalse(tile.isReady());
        assertEquals(0, tile.visualFill());
    }

    @Test
    public void persistsProgressHydrationAndReadyStateInNbt() {
        ManualComposterTile original = new ManualComposterTile();
        original.duration = 400;
        original.addOrganic(new ItemStack(Items.CARROT));
        original.addOrganic(new ItemStack(Items.POTATO));
        original.hydrate();
        original.progress = 123;

        NBTTagCompound tag = original.writeToNBT(new NBTTagCompound());
        ManualComposterTile loaded = new ManualComposterTile();
        loaded.readFromNBT(tag);

        assertEquals(2, loaded.itemCount());
        assertTrue(loaded.hydrated);
        assertEquals(123, loaded.progress);
        assertEquals(400, loaded.duration);
    }
}
