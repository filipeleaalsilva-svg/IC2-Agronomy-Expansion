package br.cropmanager.chest;

import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.PortableCropInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PortableInventoryTest {
    private EntityPlayer player;
    private World world;
    private ItemStack carrier;
    private PortableCropInventory inventory;
    private ContainerCropDatabaseChest container;
    @BeforeClass public static void bootstrap() { SeedFixtures.bootstrap(); }
    @Before public void setup() {
        world = mock(World.class);
        player = mock(EntityPlayer.class);
        player.world = world;
        player.inventory = new InventoryPlayer(player);
        carrier = new ItemStack(SeedFixtures.portable);
        player.inventory.setInventorySlotContents(0, carrier);
        inventory = PortableCropInventory.open(player, 0);
        container = new ContainerCropDatabaseChest(player.inventory, inventory);
    }
    @Test public void persistsAll500BagsAndPickupInTheItem() {
        assertEquals(1, carrier.getMaxStackSize());
        assertEquals(500, container.getCapacity());
        assertEquals(20, container.getPageCount());
        assertTrue(container.isPortable());
        for (int i = 0; i < 500; i++) assertTrue(inventory.seeds().insertAnywhere(SeedFixtures.bag(i, i % 5), false).isEmpty());
        assertFalse(inventory.seeds().insertAnywhere(SeedFixtures.bag(999, 4), false).isEmpty());
        inventory.setPickupEnabled(true);
        ItemStack loadedItem = new ItemStack(carrier.serializeNBT());
        player.inventory.setInventorySlotContents(0, loadedItem);
        PortableCropInventory loaded = PortableCropInventory.open(player, 0);
        assertNotNull(loaded);
        assertTrue(loaded.isPickupEnabled());
        assertEquals(500, loaded.getSizeInventory());
        for (int i = 0; i < 500; i++) assertEquals(SeedFixtures.bag(i, i % 5).serializeNBT(), loaded.getStackInSlot(i).serializeNBT());
    }
    @Test public void carrierCannotBePickedDroppedShiftClickedSwappedOrUsedAsADragTarget() {
        int carrierSlot = 500 + 27;
        inventory.seeds().setStackInSlot(0, SeedFixtures.bag(1, 4));
        assertFalse(container.inventorySlots.get(carrierSlot).canTakeStack(player));
        assertFalse(container.inventorySlots.get(carrierSlot).isItemValid(SeedFixtures.bag(2, 4)));
        assertFalse(container.canDragIntoSlot(container.inventorySlots.get(carrierSlot)));
        assertFalse(container.canMergeSlot(carrier, container.inventorySlots.get(carrierSlot)));
        for (ClickType type : new ClickType[] {ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.THROW, ClickType.CLONE}) {
            assertTrue(container.slotClick(carrierSlot, 0, type, player).isEmpty());
            assertSame(carrier, player.inventory.getStackInSlot(0));
        }
        assertTrue(container.transferStackInSlot(player, carrierSlot).isEmpty());
        assertTrue(container.slotClick(0, 0, ClickType.SWAP, player).isEmpty());
        assertSame(carrier, player.inventory.getStackInSlot(0));
        assertEquals(1, inventory.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertTrue(player.inventory.getItemStack().isEmpty());
    }
    @Test public void portableRejectsNestingAndShiftClickStillMovesSeeds() {
        assertFalse(inventory.seeds().insertAnywhere(carrier, false).isEmpty());
        player.inventory.setInventorySlotContents(9, SeedFixtures.bag(99, 4));
        assertFalse(container.transferStackInSlot(player, 500).isEmpty());
        assertTrue(player.inventory.getStackInSlot(9).isEmpty());
        assertEquals(99, inventory.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertFalse(container.transferStackInSlot(player, 0).isEmpty());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertSame(carrier, player.inventory.getStackInSlot(0));
        assertEquals(99, player.inventory.getStackInSlot(8).getTagCompound().getInteger("identity"));
        assertTrue(PortableCropInventory.open(player, 0).isEmpty());
    }
    @Test public void movingOrReplacingTheCarrierInvalidatesTheOldSession() {
        inventory.seeds().setStackInSlot(0, SeedFixtures.bag(1, 4));
        player.inventory.setInventorySlotContents(1, carrier);
        player.inventory.setInventorySlotContents(0, ItemStack.EMPTY);
        assertFalse(container.canInteractWith(player));
        assertTrue(container.transferStackInSlot(player, 0).isEmpty());
        assertEquals(1, PortableCropInventory.open(player, 1).getStackInSlot(0).getTagCompound().getInteger("identity"));
        player.inventory.setInventorySlotContents(0, carrier.copy());
        assertFalse(container.canInteractWith(player));
    }
    @Test public void offhandBindingAndDifferentPortableItemsStayIndependent() {
        ItemStack otherCarrier = new ItemStack(SeedFixtures.portable);
        player.inventory.setInventorySlotContents(40, otherCarrier);
        PortableCropInventory offhand = PortableCropInventory.open(player, 40);
        assertNotNull(offhand);
        inventory.seeds().setStackInSlot(0, SeedFixtures.bag(1, 4));
        offhand.seeds().setStackInSlot(0, SeedFixtures.bag(2, 4));
        ContainerCropDatabaseChest other = new ContainerCropDatabaseChest(player.inventory, offhand);
        assertTrue(other.slotClick(0, 40, ClickType.SWAP, player).isEmpty());
        assertSame(otherCarrier, player.inventory.getStackInSlot(40));
        assertEquals(2, PortableCropInventory.open(player, 40).getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertEquals(1, PortableCropInventory.open(player, 0).getStackInSlot(0).getTagCompound().getInteger("identity"));
    }

    @Test public void invalidSlotsAndWrongItemsCannotOpenPortableGui() {
        assertNull(PortableCropInventory.open(player, -1));
        assertNull(PortableCropInventory.open(player, 36));
        assertNull(PortableCropInventory.open(player, 41));
        assertNull(PortableCropInventory.open(player, 1));
        player.inventory.setInventorySlotContents(1, SeedFixtures.bag(1, 4));
        assertNull(PortableCropInventory.open(player, 1));
        assertNull(new br.cropmanager.CommonProxy().getServerGuiElement(12, player, world, 1, 0, 0));
    }
    @Test public void pickupToggleIsServerValidatedAndPersists() throws java.io.IOException {
        assertFalse(container.isPickupEnabled());
        net.minecraft.network.PacketBuffer buffer = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        net.minecraft.network.play.client.CPacketEnchantItem outgoing = new net.minecraft.network.play.client.CPacketEnchantItem(1, ContainerCropDatabaseChest.TOGGLE_PICKUP);
        outgoing.writePacketData(buffer);
        net.minecraft.network.play.client.CPacketEnchantItem incoming = new net.minecraft.network.play.client.CPacketEnchantItem();
        incoming.readPacketData(buffer);
        assertTrue(container.enchantItem(player, incoming.getButton()));
        buffer.release();
        assertTrue(container.isPickupEnabled());
        assertTrue(PortableCropInventory.open(player, 0).isPickupEnabled());
        player.inventory.setItemStack(SeedFixtures.bag(1, 4));
        assertFalse(container.enchantItem(player, ContainerCropDatabaseChest.TOGGLE_PICKUP));
        player.inventory.setItemStack(ItemStack.EMPTY);
        assertTrue(container.enchantItem(player, ContainerCropDatabaseChest.TOGGLE_PICKUP));
        assertFalse(PortableCropInventory.open(player, 0).isPickupEnabled());
    }
    @Test public void portableSortAndPageRequestsUseTheSharedContainerView() {
        for (int i = 0; i < 40; i++) inventory.setInventorySlotContents(i, SeedFixtures.bag(i, 4));
        container.detectAndSendChanges();
        assertTrue(container.enchantItem(player, br.cropmanager.chest.inventory.SeedSortMode.TYPE.getRequestId()));
        assertEquals(20, container.getPageCount());
        assertTrue(container.enchantItem(player, 1));
        assertEquals(1, container.getPage());
    }
    @Test public void portableFavoriteIdentityAndStatePersistOnItem() {
        ItemStack seed = SeedFixtures.bag(41, 4);
        inventory.seeds().setStackInSlot(0, seed);
        inventory.toggleFavorite(seed);
        PortableCropInventory reopened = PortableCropInventory.open(player, 0);
        assertTrue(reopened.isFavorite(seed));
        reopened.toggleFavorite(seed);
        assertFalse(PortableCropInventory.open(player, 0).isFavorite(seed));
    }
    @Test public void legacy250SlotItemExpandsWithoutRetainingOldLocksOrFilters() {
        br.cropmanager.chest.inventory.SeedInventory legacy = new br.cropmanager.chest.inventory.SeedInventory(250);
        legacy.setStackInSlot(249, SeedFixtures.bag(249, 4));
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Seeds", legacy.serializeNBT());
        NBTTagCompound oldManagement = new NBTTagCompound();
        oldManagement.setBoolean("Enabled", true);
        data.setTag("Management", oldManagement);
        NBTTagCompound root = new NBTTagCompound(); root.setTag(PortableCropInventory.DATA, data); carrier.setTagCompound(root);
        PortableCropInventory migrated = PortableCropInventory.open(player, 0);
        assertEquals(500, migrated.getSizeInventory());
        assertFalse(migrated.isPickupEnabled());
        assertEquals(SeedFixtures.bag(249, 4).serializeNBT(), migrated.getStackInSlot(249).serializeNBT());
        assertFalse(migrated.decrStackSize(249, 1).isEmpty());
        assertFalse(carrier.getTagCompound().getCompoundTag(PortableCropInventory.DATA).hasKey("Management"));
    }
    @Test public void itemSizeTagCannotEnlargeCapacityAndCustomItemNbtIsPreserved() {
        inventory.seeds().setStackInSlot(499, SeedFixtures.bag(499, 4));
        NBTTagCompound root = carrier.getTagCompound();
        root.setString("custom", "keep item data");
        root.getCompoundTag(PortableCropInventory.DATA).getCompoundTag("Seeds").setInteger("Size", 1000);
        PortableCropInventory loaded = PortableCropInventory.open(player, 0);
        assertEquals(500, loaded.getSizeInventory());
        assertFalse(loaded.getStackInSlot(499).isEmpty());
        loaded.setPickupEnabled(true);
        assertEquals("keep item data", carrier.getTagCompound().getString("custom"));
        assertEquals(500, carrier.getTagCompound().getCompoundTag(PortableCropInventory.DATA).getCompoundTag("Seeds").getInteger("Size"));
    }
}
