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

    @Test
    public void halfHeightBoundingBoxAndArchetypeGuardsHold() throws Exception {
        net.minecraft.util.math.AxisAlignedBB bounds = CropManager.MANUAL_COMPOSTER.getBoundingBox(
                CropManager.MANUAL_COMPOSTER.getDefaultState(), null, net.minecraft.util.math.BlockPos.ORIGIN);
        assertEquals(0.0, bounds.minY, 1e-6);
        assertEquals(0.5, bounds.maxY, 1e-6);
        assertEquals(0.0625, bounds.minX, 1e-6);
        assertEquals(0.9375, bounds.maxX, 1e-6);

        // Client side guard (B2)
        ManualComposterTile clientTile = new ManualComposterTile();
        World clientWorld = Mockito.mock(World.class);
        java.lang.reflect.Field remoteField = World.class.getDeclaredField("isRemote");
        remoteField.setAccessible(true);
        remoteField.set(clientWorld, true);
        clientTile.setWorld(clientWorld);
        clientTile.duration = 10;
        for (int i = 0; i < ManagerConfig.manualComposterRequiredItems; i++) {
            clientTile.addOrganic(new ItemStack(Items.WHEAT));
        }
        clientTile.update();
        assertEquals("Client world must not advance server fermentation", 0, clientTile.progress);

        // Corrupted NBT clamping (B4) and shouldRefresh lifecycle (B5)
        NBTTagCompound corrupt = new NBTTagCompound();
        corrupt.setInteger("Duration", -50);
        corrupt.setInteger("Progress", 99999);
        corrupt.setInteger("ReadyCount", -3);
        ManualComposterTile clamped = new ManualComposterTile();
        clamped.readFromNBT(corrupt);
        assertTrue(clamped.duration >= 1);
        assertTrue(clamped.progress >= 0 && clamped.progress <= clamped.duration);
        assertEquals(0, clamped.readyCount);

        assertFalse(clamped.shouldRefresh(clientWorld, net.minecraft.util.math.BlockPos.ORIGIN,
                CropManager.MANUAL_COMPOSTER.getStateFromMeta(0),
                CropManager.MANUAL_COMPOSTER.getStateFromMeta(5)));
        assertTrue(clamped.shouldRefresh(clientWorld, net.minecraft.util.math.BlockPos.ORIGIN,
                CropManager.MANUAL_COMPOSTER.getDefaultState(),
                net.minecraft.init.Blocks.AIR.getDefaultState()));
    }
}
