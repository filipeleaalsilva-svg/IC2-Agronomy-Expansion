package br.cropmanager.chest;

import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import br.cropmanager.chest.block.BlockCropDatabaseChest;
import br.cropmanager.chest.item.ItemUpgradeLinker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.WorldProviderSurface;
import org.mockito.ArgumentCaptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraft.init.Blocks;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ContainerTest {
    private EntityPlayer player;
    private TileCropDatabaseChest tile;
    private World world;
    private ContainerCropDatabaseChest container;

    @BeforeClass public static void bootstrap() { SeedFixtures.bootstrap(); }
    @Before public void setup() {
        world = mock(World.class);
        player = mock(EntityPlayer.class);
        player.world = world;
        player.inventory = new InventoryPlayer(player);
        tile = new TileCropDatabaseChest();
        tile.setWorld(world);
        tile.setPos(BlockPos.ORIGIN);
        when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(tile);
        when(world.getBlockState(BlockPos.ORIGIN)).thenReturn(Blocks.CHEST.getDefaultState());
        container = new ContainerCropDatabaseChest(player.inventory, tile);
    }

    @Test public void pagesArePerPlayerAndServerRejectsInvalidSelections() {
        EntityPlayer second = mock(EntityPlayer.class);
        second.world = world;
        second.inventory = new InventoryPlayer(second);
        ContainerCropDatabaseChest other = new ContainerCropDatabaseChest(second.inventory, tile);
        IContainerListener listener = mock(IContainerListener.class);
        container.addListener(listener);
        assertTrue(container.enchantItem(player, 10));
        assertEquals(10, container.getPage());
        assertEquals(0, other.getPage());
        verify(listener).sendWindowProperty(container, 0, 10);
        assertFalse(container.enchantItem(player, -1));
        assertFalse(container.enchantItem(player, 20));
        player.inventory.setItemStack(SeedFixtures.bag(1, 0));
        assertFalse(container.enchantItem(player, 0));
        assertEquals(10, container.getPage());
    }







    @Test public void hiddenSlotsCannotBeClickedOrShiftClicked() {
        tile.setInventorySlotContents(499, SeedFixtures.bag(499, 4));
        assertTrue(container.slotClick(499, 0, ClickType.PICKUP, player).isEmpty());
        assertTrue(player.inventory.getItemStack().isEmpty());
        assertTrue(container.transferStackInSlot(player, 499).isEmpty());
        assertFalse(tile.getStackInSlot(499).isEmpty());
    }
    @Test public void placedChestsRejectThePortablePickupRequestAndIgnoreLegacyManagement() {
        assertFalse(container.enchantItem(player, ContainerCropDatabaseChest.TOGGLE_PICKUP));
        tile.setInventorySlotContents(0, SeedFixtures.bag(1, 4));
        net.minecraft.nbt.NBTTagCompound saved = tile.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        net.minecraft.nbt.NBTTagCompound management = new net.minecraft.nbt.NBTTagCompound();
        management.setBoolean("Enabled", true);
        saved.setTag("Management", management);
        tile.readFromNBT(saved);
        assertFalse(tile.automation().extractItem(0, 1, false).isEmpty());
        assertFalse(tile.writeToNBT(new net.minecraft.nbt.NBTTagCompound()).hasKey("Management"));
    }

    @Test public void lastPageHas25SlotsAndRightClickOrCloneDoesNotDuplicate() {
        assertTrue(container.enchantItem(player, 19));
        int enabled = 0;
        for (int i = 0; i < 500; i++) if (container.inventorySlots.get(i).isEnabled()) enabled++;
        assertEquals(25, enabled);
        assertTrue(container.inventorySlots.get(475).isEnabled());
        assertTrue(container.inventorySlots.get(499).isEnabled());
        tile.setInventorySlotContents(475, SeedFixtures.bag(475, 4));
        container.detectAndSendChanges();
        container.acknowledgeView(container.getRevision());
        assertTrue(container.slotClick(475, 1, ClickType.PICKUP, player).isEmpty());
        assertTrue(container.slotClick(475, 2, ClickType.CLONE, player).isEmpty());
        assertTrue(player.inventory.getItemStack().isEmpty());
        assertFalse(tile.getStackInSlot(475).isEmpty());
    }

    @Test public void legacySmallChestTilesUseTheTierThreeCapacity() {
        for (TileCropDatabaseChest legacy : new TileCropDatabaseChest[] {
                new br.cropmanager.chest.inventory.TileCropDatabaseChest125(),
                new br.cropmanager.chest.inventory.TileCropDatabaseChest250()}) {
            legacy.setWorld(world);
            legacy.setPos(BlockPos.ORIGIN);
            when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(legacy);
            assertEquals(500, legacy.getActiveCapacity());
            legacy.setInventorySlotContents(499, SeedFixtures.bag(499, 4));
            ContainerCropDatabaseChest upgraded = new ContainerCropDatabaseChest(player.inventory, legacy);
            assertEquals(536, upgraded.inventorySlots.size());
            assertTrue(upgraded.enchantItem(player, 19));
            assertTrue(upgraded.inventorySlots.get(499).isEnabled());
        }
    }

    @Test public void searchCompactsAllStorageWithoutMovingBagsOrRevealingUnknownSeeds() {
        tile.setInventorySlotContents(7, SeedFixtures.bag(7, 0));
        ItemStack last = SeedFixtures.bag(499, 4);
        tile.setInventorySlotContents(499, last.copy());
        assertTrue(container.setSearch(player, "FERR", java.util.Collections.emptySet()));
        assertEquals(0, container.getPosition(499));
        assertEquals(-1, container.getPosition(7));
        assertEquals(1, container.getPageCount());
        assertEquals(last.serializeNBT(), tile.getStackInSlot(499).serializeNBT());
        container.detectAndSendChanges();
        assertEquals(2, container.getOccupied());
        assertTrue(container.setSearch(player, "translated name", java.util.Collections.singleton("test:ferru")));
        assertEquals(0, container.getPosition(499));
        assertTrue(container.setSearch(player, "missing", java.util.Collections.emptySet()));
        assertEquals(-1, container.getPosition(499));
        assertEquals(1, container.getPageCount());
        assertTrue(container.setSearch(player, "", java.util.Collections.emptySet()));
        assertEquals(20, container.getPageCount());
        assertEquals(7, container.getPosition(7));
    }

    @Test public void automationRefreshesFilteredResultsAndClampsThePage() {
        for (int i = 0; i < 50; i++) tile.setInventorySlotContents(i, SeedFixtures.bag(i, 4));
        assertTrue(container.setSearch(player, "ferru", java.util.Collections.emptySet()));
        assertEquals(2, container.getPageCount());
        assertTrue(container.enchantItem(player, 1));
        int oldRevision = container.getRevision();
        for (int i = 0; i < 26; i++) tile.seeds().extractItem(i, 1, false);
        container.detectAndSendChanges();
        assertEquals(0, container.getPage());
        assertEquals(1, container.getPageCount());
        assertEquals(24, container.getOccupied());
        assertEquals(0, container.getPosition(26));
        assertNotEquals(oldRevision, container.getRevision());
    }

    @Test public void replacingAVisibleBagInvalidatesTheViewEvenAtTheSamePosition() {
        tile.setInventorySlotContents(0, SeedFixtures.bag(0, 4));
        container.detectAndSendChanges();
        int before = container.getRevision();
        tile.setInventorySlotContents(0, SeedFixtures.bag(99, 4));
        container.detectAndSendChanges();
        assertNotEquals(before, container.getRevision());
    }

    @Test public void staleClientCannotTakeAReplacementBag() {
        net.minecraft.entity.player.EntityPlayerMP client = mock(net.minecraft.entity.player.EntityPlayerMP.class);
        client.world = world;
        client.inventory = new InventoryPlayer(client);
        tile.setInventorySlotContents(0, SeedFixtures.bag(0, 4));
        container.detectAndSendChanges();
        container.acknowledgeView(container.getRevision());
        tile.setInventorySlotContents(0, SeedFixtures.bag(99, 4));
        assertTrue(container.slotClick(0, 0, ClickType.PICKUP, client).isEmpty());
        assertEquals(99, tile.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertTrue(client.inventory.getItemStack().isEmpty());
    }

    @Test public void clientPredictionKeepsTheServerFilteredMapping() throws ReflectiveOperationException {
        tile.setInventorySlotContents(499, SeedFixtures.bag(499, 4));
        assertTrue(container.setSearch(player, "ferru", java.util.Collections.emptySet()));
        java.lang.reflect.Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, true);
        container.slotClick(499, 0, ClickType.PICKUP, player);
        assertEquals(0, container.getPosition(499));
        assertEquals(499, player.inventory.getItemStack().getTagCompound().getInteger("identity"));
    }

    @Test public void remoteClientPredictsClicksWithoutAWorldTileAndLocksTheCarrier() throws ReflectiveOperationException {
        java.lang.reflect.Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, true);
        ItemStack carrier = new ItemStack(SeedFixtures.portable);
        ItemUpgradeLinker.setLink(carrier, 1, BlockPos.ORIGIN);
        player.inventory.setInventorySlotContents(0, carrier);
        TileCropDatabaseChest clientChest = new TileCropDatabaseChest();
        clientChest.setInventorySlotContents(0, SeedFixtures.bag(17, 4));
        ContainerCropDatabaseChest linked = new ContainerCropDatabaseChest(player.inventory, clientChest, 0);

        assertTrue(linked.canInteractWith(player));
        assertTrue(linked.slotClick(0, 0, ClickType.SWAP, player).isEmpty());
        assertSame(carrier, player.inventory.getStackInSlot(0));
        linked.slotClick(0, 0, ClickType.PICKUP, player);
        assertEquals(17, player.inventory.getItemStack().getTagCompound().getInteger("identity"));
        assertTrue(clientChest.getStackInSlot(0).isEmpty());
    }

    @Test public void remoteServerRequiresTheCarrierToResolveToTheOpenChest() throws ReflectiveOperationException {
        int dimension = 4721;
        BlockPos pos = new BlockPos(2, 64, 3);
        WorldServer target = mock(WorldServer.class);
        when(target.isBlockLoaded(pos)).thenReturn(true);
        when(target.getTileEntity(pos)).thenReturn(tile);
        tile.setWorld(target);
        tile.setPos(pos);
        when(target.getBlockState(pos)).thenReturn(Blocks.CHEST.getDefaultState());
        ItemStack carrier = new ItemStack(SeedFixtures.portable);
        ItemUpgradeLinker.setLink(carrier, dimension, pos);
        player.inventory.setInventorySlotContents(0, carrier);
        ContainerCropDatabaseChest linked = new ContainerCropDatabaseChest(player.inventory, tile, 0);

        assertFalse(linked.canInteractWith(player));
        verify(world, never()).getMinecraftServer();
        java.lang.reflect.Field worldsField = DimensionManager.class.getDeclaredField("worlds");
        worldsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<Integer, WorldServer> worlds = (java.util.Map<Integer, WorldServer>) worldsField.get(null);
        WorldServer previous = worlds.put(dimension, target);
        try {
            assertTrue(linked.canInteractWith(player));
            when(target.isBlockLoaded(pos)).thenReturn(false);
            assertFalse(linked.canInteractWith(player));
            when(target.isBlockLoaded(pos)).thenReturn(true);

            ItemUpgradeLinker.setLink(carrier, dimension, pos.up());
            assertFalse(linked.canInteractWith(player));
            ItemStack linker = new ItemStack(br.cropmanager.CropManager.UPGRADE_LINKER);
            ItemUpgradeLinker.setLink(linker, dimension, pos);
            player.inventory.setInventorySlotContents(0, linker);
            assertFalse(linked.canInteractWith(player));
        } finally {
            if (previous == null) worlds.remove(dimension);
            else worlds.put(dimension, previous);
        }
    }

    @Test public void lidStaysOpenUntilLastViewerCloses() {
        EntityPlayer second = mock(EntityPlayer.class);
        second.world = world;
        second.inventory = new InventoryPlayer(second);
        ContainerCropDatabaseChest other = new ContainerCropDatabaseChest(second.inventory, tile);
        for (int i = 0; i < 12; i++) tile.update();
        assertEquals(1.0F, tile.lidAngle, 0.001F);
        container.onContainerClosed(player);
        tile.update();
        assertEquals(1.0F, tile.lidAngle, 0.001F);
        other.onContainerClosed(second);
        for (int i = 0; i < 12; i++) tile.update();
        assertEquals(0.0F, tile.lidAngle, 0.001F);
    }

    @Test public void shiftClickFindsSpaceOnLastPageAndReturnsExactStack() {
        for (int i = 0; i < 499; i++) tile.setInventorySlotContents(i, SeedFixtures.bag(i, 4));
        ItemStack bag = SeedFixtures.bag(999, 1);
        player.inventory.setInventorySlotContents(9, bag.copy());
        assertFalse(container.transferStackInSlot(player, 500).isEmpty());
        assertTrue(player.inventory.getStackInSlot(9).isEmpty());
        assertEquals(bag.serializeNBT(), tile.getStackInSlot(499).serializeNBT());
        assertTrue(container.enchantItem(player, 19));
        assertFalse(container.transferStackInSlot(player, 499).isEmpty());
        assertTrue(tile.getStackInSlot(499).isEmpty());
        assertEquals(bag.serializeNBT(), player.inventory.getStackInSlot(8).serializeNBT());
    }

    @Test public void shiftClickIntoFullChestKeepsPlayerStack() {
        for (int i = 0; i < 500; i++) tile.setInventorySlotContents(i, SeedFixtures.bag(i, 4));
        ItemStack bag = SeedFixtures.bag(999, 0);
        player.inventory.setInventorySlotContents(9, bag.copy());
        assertTrue(container.transferStackInSlot(player, 500).isEmpty());
        assertEquals(bag.serializeNBT(), player.inventory.getStackInSlot(9).serializeNBT());
    }

    @Test public void removingTileClosesAccessAndBlocksTransfers() {
        when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(null);
        assertFalse(container.canInteractWith(player));
        assertFalse(container.enchantItem(player, 1));
        assertTrue(container.transferStackInSlot(player, 0).isEmpty());
    }

    @Test public void dragBlocksPageChangesAndHiddenSlotsAreNotMergeTargets() {
        container.slotClick(-999, 0, ClickType.QUICK_CRAFT, player);
        assertFalse(container.enchantItem(player, 1));
        container.slotClick(-999, 2, ClickType.QUICK_CRAFT, player);
        assertTrue(container.enchantItem(player, 1));
        assertFalse(container.canDragIntoSlot(container.inventorySlots.get(0)));
        assertFalse(container.canMergeSlot(SeedFixtures.bag(0, 0), container.inventorySlots.get(0)));
        assertTrue(container.canDragIntoSlot(container.inventorySlots.get(49)));
    }

    @Test public void twoPlayersCannotTakeTheSameBagTwice() {
        EntityPlayer second = mock(EntityPlayer.class);
        second.world = world;
        second.inventory = new InventoryPlayer(second);
        ContainerCropDatabaseChest other = new ContainerCropDatabaseChest(second.inventory, tile);
        tile.setInventorySlotContents(0, SeedFixtures.bag(10, 4));
        assertFalse(container.transferStackInSlot(player, 0).isEmpty());
        assertTrue(other.transferStackInSlot(second, 0).isEmpty());
        assertTrue(tile.getStackInSlot(0).isEmpty());
        assertEquals(10, player.inventory.getStackInSlot(8).getTagCompound().getInteger("identity"));
        assertTrue(second.inventory.isEmpty());
    }

    @Test public void breakingChestDropsEveryBagOnceAndClearsStorage() throws ReflectiveOperationException {
        // Mockito bypasses World's constructor; supply its final provider only in this fixture.
        java.lang.reflect.Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        for (int i = 0; i < 500; i++) tile.setInventorySlotContents(i, SeedFixtures.bag(i, i % 5));
        BlockCropDatabaseChest block = new BlockCropDatabaseChest();
        block.breakBlock(world, BlockPos.ORIGIN, block.getDefaultState());
        ArgumentCaptor<Entity> drops = ArgumentCaptor.forClass(Entity.class);
        verify(world, times(500)).spawnEntity(drops.capture());
        boolean[] identities = new boolean[500];
        for (Entity drop : drops.getAllValues()) {
            ItemStack seed = ((EntityItem) drop).getItem();
            int identity = seed.getTagCompound().getInteger("identity");
            assertFalse(identities[identity]);
            identities[identity] = true;
            assertEquals(SeedFixtures.bag(identity, identity % 5).serializeNBT(), seed.serializeNBT());
        }
        assertTrue(tile.isEmpty());
        verify(world).removeTileEntity(BlockPos.ORIGIN);
    }

    @Test public void rightClickSplitAndCreativeCloneAreBlockedOnSeedSlots() {
        tile.setInventorySlotContents(0, SeedFixtures.bag(0, 4));
        container.detectAndSendChanges();
        ItemStack rightClick = container.slotClick(0, 1, ClickType.PICKUP, player);
        assertTrue(rightClick.isEmpty());
        ItemStack cloneClick = container.slotClick(0, 2, ClickType.CLONE, player);
        assertTrue(cloneClick.isEmpty());
    }
}
