package br.cropmanager;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.MapStorage;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public class WirelessEuGridTest {
    @org.junit.BeforeClass
    public static void boot() {
        net.minecraft.init.Bootstrap.register();
    }

    private final int prevBuffer = ManagerConfig.wifiChannelBufferEU;
    private final float prevLoss = ManagerConfig.wifiCrossDimLossPercent;

    @After
    public void tearDown() {
        ManagerConfig.wifiChannelBufferEU = prevBuffer;
        ManagerConfig.wifiCrossDimLossPercent = prevLoss;
    }

    @Test
    public void privateChannelBlocksOtherPlayersAndAllowsOwner() {
        WirelessEuGridData grid = new WirelessEuGridData();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        assertTrue(grid.setPrivacy(7, true, alice, "Alice"));
        assertTrue(grid.canAccess(7, alice));
        assertFalse(grid.canAccess(7, bob));
        assertEquals(0.0, grid.insertEu(7, 500.0, 0, bob, "Bob"), 1e-6);
        assertEquals(500.0, grid.insertEu(7, 500.0, 0, alice, "Alice"), 1e-6);
        assertEquals(0.0, grid.extractEu(7, 200.0, 0, bob), 1e-6);
        assertEquals(200.0, grid.extractEu(7, 200.0, 0, alice), 1e-6);
    }

    @Test
    public void crossDimLossAppliesOnlyAcrossDimensionsAndNbtPersists() {
        ManagerConfig.wifiChannelBufferEU = 10000;
        ManagerConfig.wifiCrossDimLossPercent = 20.0F;
        WirelessEuGridData grid = new WirelessEuGridData();
        UUID alice = UUID.randomUUID();

        assertEquals(1000.0, grid.insertEu(3, 1000.0, 0, alice, "Alice"), 1e-6);
        // Same dimension -> 0% loss
        assertEquals(200.0, grid.extractEu(3, 200.0, 0, alice), 1e-6);
        assertEquals(800.0, grid.getStoredEu(3), 1e-6);
        // Different dimension -> 20% loss (delivering 400 drains 500 from buffer)
        assertEquals(400.0, grid.extractEu(3, 400.0, -1, alice), 1e-6);
        assertEquals(300.0, grid.getStoredEu(3), 1e-6);

        NBTTagCompound tag = grid.writeToNBT(new NBTTagCompound());
        WirelessEuGridData restored = new WirelessEuGridData();
        restored.readFromNBT(tag);
        assertEquals(300.0, restored.getStoredEu(3), 1e-6);
    }

    @Test
    public void pointTileRespectsTierRateAndRedstoneMode() throws Exception {
        World world = Mockito.mock(World.class);
        WorldProvider provider = Mockito.mock(WorldProvider.class);
        Mockito.when(provider.getDimension()).thenReturn(0);
        java.lang.reflect.Field pf = World.class.getDeclaredField("provider");
        pf.setAccessible(true);
        pf.set(world, provider);
        MapStorage storage = Mockito.mock(MapStorage.class);
        WirelessEuGridData grid = new WirelessEuGridData();
        Mockito.when(world.getMapStorage()).thenReturn(storage);
        Mockito.when(storage.getOrLoadData(WirelessEuGridData.class, WirelessEuGridData.DATA_NAME)).thenReturn(grid);
        Mockito.when(world.isBlockLoaded(Mockito.any(), Mockito.eq(false))).thenReturn(true);
        Mockito.when(world.isBlockPowered(Mockito.any())).thenReturn(true);

        grid.insertEu(5, 5000.0, 0, null, null);
        EuWifiPointTile point = new EuWifiPointTile();
        point.setWorld(world);
        point.setPos(net.minecraft.util.math.BlockPos.ORIGIN);
        point.channel = 5;
        point.outputTier = 2; // max 128 EU/t
        point.rateLimit = 128;
        point.redstoneMode = 2; // active only when NOT powered

        point.update();
        assertEquals(0.0, point.lastOutputRate, 1e-6);

        point.redstoneMode = 1; // active WITH redstone signal
        point.update();
        assertEquals(128.0, point.lastOutputRate, 1e-6);
        assertEquals(128.0, point.storedEu, 1e-6);
    }

    @Test
    public void pointClampsSavedTierAndCyclesWithinConfiguredLimit() {
        int previous = ManagerConfig.wifiPointMaxOutputTier;
        try {
            ManagerConfig.wifiPointMaxOutputTier = 2;
            EuWifiPointTile point = new EuWifiPointTile();
            point.setWorld(Mockito.mock(World.class));
            NBTTagCompound saved = new NBTTagCompound();
            saved.setInteger("OutputTier", 4);
            saved.setInteger("RateLimit", 2048);
            point.readFromNBT(saved);
            assertEquals(2, point.getSourceTier());
            assertEquals(128, point.effectiveRate());
            assertTrue(point.control(1, null));
            assertEquals(1, point.getSourceTier());
            assertTrue(point.control(1, null));
            assertEquals(2, point.getSourceTier());
        } finally {
            ManagerConfig.wifiPointMaxOutputTier = previous;
        }
    }

    @Test
    public void wirelessUpgradeLinkedToPrivateWifiChannelPowersMachine() throws Exception {
        World world = Mockito.mock(World.class);
        WorldProvider provider = Mockito.mock(WorldProvider.class);
        Mockito.when(provider.getDimension()).thenReturn(0);
        java.lang.reflect.Field pf = World.class.getDeclaredField("provider");
        pf.setAccessible(true);
        pf.set(world, provider);
        MapStorage storage = Mockito.mock(MapStorage.class);
        WirelessEuGridData grid = new WirelessEuGridData();
        Mockito.when(world.getMapStorage()).thenReturn(storage);
        Mockito.when(storage.getOrLoadData(WirelessEuGridData.class, WirelessEuGridData.DATA_NAME)).thenReturn(grid);

        UUID alice = UUID.randomUUID();
        net.minecraft.entity.player.EntityPlayer alicePlayer = Mockito.mock(net.minecraft.entity.player.EntityPlayer.class);
        Mockito.when(alicePlayer.getUniqueID()).thenReturn(alice);
        Mockito.when(alicePlayer.getName()).thenReturn("Alice");
        Mockito.when(alicePlayer.isSneaking()).thenReturn(true);

        EuWifiPlugTile plug = new EuWifiPlugTile();
        plug.setWorld(world);
        plug.setPos(net.minecraft.util.math.BlockPos.ORIGIN);
        plug.setOwner(alicePlayer);
        assertTrue(plug.control(1009, alicePlayer)); // channel 9
        assertTrue(plug.control(1, alicePlayer));    // set private
        plug.energy.setEnergyStored(1000.0);
        plug.update();
        assertEquals(1000.0, grid.getStoredEu(9), 1e-6);

        Mockito.when(world.getTileEntity(net.minecraft.util.math.BlockPos.ORIGIN)).thenReturn(plug);
        net.minecraft.item.ItemStack upgrade = new net.minecraft.item.ItemStack(CropManager.WIRELESS_EU);
        Mockito.when(alicePlayer.getHeldItem(net.minecraft.util.EnumHand.MAIN_HAND)).thenReturn(upgrade);

        assertEquals(net.minecraft.util.EnumActionResult.SUCCESS,
                CropManager.WIRELESS_EU.onItemUseFirst(alicePlayer, world, net.minecraft.util.math.BlockPos.ORIGIN,
                        net.minecraft.util.EnumFacing.UP, 0.5F, 0.5F, 0.5F, net.minecraft.util.EnumHand.MAIN_HAND));

        ComposterTile composter = new ComposterTile();
        composter.setWorld(world);
        composter.setPos(new net.minecraft.util.math.BlockPos(10, 64, 10));
        assertTrue("Linked upgrade should pull from owner's private Wi-Fi channel",
                ((WirelessEuUpgrade) CropManager.WIRELESS_EU).onTick(upgrade, composter));
        assertTrue(composter.getEnergy() > 0);
    }

    @Test
    public void publicChannelOwnershipPreventsTakeoverByOtherPlayer() {
        WirelessEuGridData grid = new WirelessEuGridData();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        assertEquals(100.0, grid.insertEu(12, 100.0, 0, alice, "Alice"), 1e-6);
        assertFalse("Other player must not claim/lock Alice's public channel",
                grid.setPrivacy(12, true, bob, "Bob"));
        assertTrue(grid.canAccess(12, alice));
        assertTrue(grid.canAccess(12, bob));
        assertTrue(grid.setPrivacy(12, true, alice, "Alice"));
    }

    @Test
    public void fullMachineInOtherDimensionDoesNotLoseEuOrMutateSourceDim() throws Exception {
        ManagerConfig.wifiCrossDimLossPercent = 20.0F;
        World overworld = Mockito.mock(World.class);
        WorldProvider owProvider = Mockito.mock(WorldProvider.class);
        Mockito.when(owProvider.getDimension()).thenReturn(0);
        World nether = Mockito.mock(World.class);
        WorldProvider netherProvider = Mockito.mock(WorldProvider.class);
        Mockito.when(netherProvider.getDimension()).thenReturn(-1);
        java.lang.reflect.Field pf = World.class.getDeclaredField("provider");
        pf.setAccessible(true);
        pf.set(overworld, owProvider);
        pf.set(nether, netherProvider);

        MapStorage storage = Mockito.mock(MapStorage.class);
        WirelessEuGridData grid = new WirelessEuGridData();
        Mockito.when(nether.getMapStorage()).thenReturn(storage);
        Mockito.when(storage.getOrLoadData(WirelessEuGridData.class, WirelessEuGridData.DATA_NAME)).thenReturn(grid);

        UUID alice = UUID.randomUUID();
        grid.insertEu(8, 1000.0, 0, alice, "Alice");

        ComposterTile composter = new ComposterTile();
        composter.setWorld(nether);
        composter.setPos(new net.minecraft.util.math.BlockPos(0, 64, 0));
        composter.energySink().setEnergyStored(composter.energySink().getCapacity());

        net.minecraft.item.ItemStack upgrade = new net.minecraft.item.ItemStack(CropManager.WIRELESS_EU);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(WirelessEuUpgrade.WIFI_CHANNEL_TAG, 8);
        tag.setUniqueId(WirelessEuUpgrade.WIFI_OWNER_TAG, alice);
        upgrade.setTagCompound(tag);

        assertFalse(((WirelessEuUpgrade) CropManager.WIRELESS_EU).onTick(upgrade, composter));
        assertEquals(1000.0, grid.getStoredEu(8), 1e-6);
        assertEquals(0, grid.getIfPresent(8).lastSourceDim);
    }

    @Test
    public void wifiPointWrapsRateAndIgnoresNaNDraw() {
        EuWifiPointTile point = new EuWifiPointTile();
        point.setWorld(Mockito.mock(World.class));
        point.storedEu = 500.0;
        point.drawEnergy(Double.NaN);
        assertEquals(500.0, point.storedEu, 1e-6);

        assertEquals(32, point.effectiveRate());
        assertTrue(point.control(4, null));
        assertEquals(8, point.effectiveRate());
        assertTrue(point.control(4, null));
        assertEquals(16, point.effectiveRate());
        assertTrue(point.control(3, null));
        assertEquals(8, point.effectiveRate());
        assertTrue(point.control(3, null));
        assertEquals(32, point.effectiveRate());
    }
}
