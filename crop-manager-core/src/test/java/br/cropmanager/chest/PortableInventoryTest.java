package br.cropmanager.chest;

import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.PortableCropInventory;
import br.cropmanager.chest.item.ItemUpgradeLinker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
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
    @Test public void unlinkingAnUnavailableChestRestoresAccessToStoredBagsAndSettings() {
        ItemStack bag = SeedFixtures.bag(21, 4);
        inventory.seeds().setStackInSlot(0, bag);
        inventory.setPickupEnabled(true);
        inventory.toggleFavorite(bag);
        ItemUpgradeLinker.setLink(carrier, 4721, BlockPos.ORIGIN);
        when(player.getHeldItem(EnumHand.MAIN_HAND)).thenReturn(carrier);
        when(player.isSneaking()).thenReturn(true);

        assertEquals(EnumActionResult.SUCCESS, SeedFixtures.portable.onItemRightClick(world, player, EnumHand.MAIN_HAND).getType());
        assertFalse(ItemUpgradeLinker.hasLink(carrier));
        PortableCropInventory reopened = PortableCropInventory.open(player, 0);
        assertEquals(bag.serializeNBT(), reopened.getStackInSlot(0).serializeNBT());
        assertTrue(reopened.isPickupEnabled());
        assertTrue(reopened.isFavorite(bag));
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

    @Test public void entityPlayerMPMouseClickAndDepositSeedsPersistCorrectly() {
        net.minecraft.entity.player.EntityPlayerMP mpPlayer = mock(net.minecraft.entity.player.EntityPlayerMP.class);
        mpPlayer.world = world;
        mpPlayer.inventory = new InventoryPlayer(mpPlayer);
        ItemStack mpCarrier = new ItemStack(SeedFixtures.portable);
        mpPlayer.inventory.setInventorySlotContents(0, mpCarrier);
        PortableCropInventory mpInventory = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest mpContainer = new ContainerCropDatabaseChest(mpPlayer.inventory, mpInventory);

        // Put a seed bag in player inventory slot 9 (container slot 500)
        ItemStack seedBag = SeedFixtures.bag(42, 4);
        mpPlayer.inventory.setInventorySlotContents(9, seedBag);

        // 1. Player clicks on slot 500 to pick it up on cursor
        ItemStack picked = mpContainer.slotClick(500, 0, ClickType.PICKUP, mpPlayer);
        assertFalse(picked.isEmpty());
        assertEquals(42, picked.getTagCompound().getInteger("identity"));
        assertFalse(mpPlayer.inventory.getItemStack().isEmpty());
        assertEquals(42, mpPlayer.inventory.getItemStack().getTagCompound().getInteger("identity"));
        assertTrue(mpPlayer.inventory.getStackInSlot(9).isEmpty());

        // 2. Player clicks on chest slot 0 to deposit it
        mpContainer.slotClick(0, 0, ClickType.PICKUP, mpPlayer);
        assertTrue(mpPlayer.inventory.getItemStack().isEmpty());
        assertFalse(mpInventory.getStackInSlot(0).isEmpty());
        assertEquals(42, mpInventory.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertEquals(1, mpContainer.getOccupied());

        // 3. Verify NBT was written to mpCarrier in slot 0
        NBTTagCompound data = mpPlayer.inventory.getStackInSlot(0).getTagCompound().getCompoundTag(PortableCropInventory.DATA);
        assertNotNull(data);
        assertFalse(data.getCompoundTag("Seeds").isEmpty());

        // 4. Reopen and verify persisted seeds
        PortableCropInventory reopened = PortableCropInventory.open(mpPlayer, 0);
        assertNotNull(reopened);
        assertFalse(reopened.isEmpty());
        assertEquals(42, reopened.getStackInSlot(0).getTagCompound().getInteger("identity"));
    }

    @Test public void entityPlayerMPMousePickupAndDepositMultiSeedsAndExtract() {
        net.minecraft.entity.player.EntityPlayerMP mpPlayer = mock(net.minecraft.entity.player.EntityPlayerMP.class);
        mpPlayer.world = world;
        mpPlayer.inventory = new InventoryPlayer(mpPlayer);
        ItemStack mpCarrier = new ItemStack(SeedFixtures.portable);
        mpPlayer.inventory.setInventorySlotContents(0, mpCarrier);
        PortableCropInventory mpInventory = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest mpContainer = new ContainerCropDatabaseChest(mpPlayer.inventory, mpInventory);

        // Put seed 101 in slot 9 (container slot 500) and seed 102 in slot 10 (container slot 501)
        mpPlayer.inventory.setInventorySlotContents(9, SeedFixtures.bag(101, 4));
        mpPlayer.inventory.setInventorySlotContents(10, SeedFixtures.bag(102, 4));

        // Deposit seed 101 to chest slot 0
        mpContainer.slotClick(500, 0, ClickType.PICKUP, mpPlayer);
        mpContainer.slotClick(0, 0, ClickType.PICKUP, mpPlayer);
        assertTrue(mpPlayer.inventory.getItemStack().isEmpty());
        assertEquals(101, mpInventory.getStackInSlot(0).getTagCompound().getInteger("identity"));

        // Deposit seed 102 to chest slot 1
        mpContainer.slotClick(501, 0, ClickType.PICKUP, mpPlayer);
        mpContainer.slotClick(1, 0, ClickType.PICKUP, mpPlayer);
        assertTrue(mpPlayer.inventory.getItemStack().isEmpty());
        assertEquals(102, mpInventory.getStackInSlot(1).getTagCompound().getInteger("identity"));
        assertEquals(2, mpContainer.getOccupied());

        // Extract seed 101 from slot 0 using cursor
        mpContainer.slotClick(0, 0, ClickType.PICKUP, mpPlayer);
        assertFalse(mpPlayer.inventory.getItemStack().isEmpty());
        assertEquals(101, mpPlayer.inventory.getItemStack().getTagCompound().getInteger("identity"));
        assertTrue(mpInventory.getStackInSlot(0).isEmpty());
        assertEquals(1, mpContainer.getOccupied());

        // Place extracted seed into player slot 11 (container slot 502)
        mpContainer.slotClick(502, 0, ClickType.PICKUP, mpPlayer);
        assertTrue(mpPlayer.inventory.getItemStack().isEmpty());
        assertEquals(101, mpPlayer.inventory.getStackInSlot(11).getTagCompound().getInteger("identity"));

        // Reopen and verify persistence
        PortableCropInventory reopened = PortableCropInventory.open(mpPlayer, 0);
        assertNotNull(reopened);
        assertTrue(reopened.getStackInSlot(0).isEmpty());
        assertEquals(102, reopened.getStackInSlot(1).getTagCompound().getInteger("identity"));
    }

    @Test public void entityPlayerMPShiftClickMoveSeedsInAndOutPersists() {
        net.minecraft.entity.player.EntityPlayerMP mpPlayer = mock(net.minecraft.entity.player.EntityPlayerMP.class);
        mpPlayer.world = world;
        mpPlayer.inventory = new InventoryPlayer(mpPlayer);
        ItemStack mpCarrier = new ItemStack(SeedFixtures.portable);
        mpPlayer.inventory.setInventorySlotContents(0, mpCarrier);
        PortableCropInventory mpInventory = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest mpContainer = new ContainerCropDatabaseChest(mpPlayer.inventory, mpInventory);

        // Put seed 201 in player slot 9 (container slot 500)
        mpPlayer.inventory.setInventorySlotContents(9, SeedFixtures.bag(201, 4));

        // Shift-click player slot 500 into the chest
        mpContainer.slotClick(500, 0, ClickType.QUICK_MOVE, mpPlayer);
        assertTrue(mpPlayer.inventory.getStackInSlot(9).isEmpty());
        assertFalse(mpInventory.getStackInSlot(0).isEmpty());
        assertEquals(201, mpInventory.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertEquals(1, mpContainer.getOccupied());

        // Reopen to verify persisted into carrier NBT
        PortableCropInventory check1 = PortableCropInventory.open(mpPlayer, 0);
        assertNotNull(check1);
        assertEquals(201, check1.getStackInSlot(0).getTagCompound().getInteger("identity"));

        // Shift-click chest slot 0 back into the player inventory
        mpContainer.slotClick(0, 0, ClickType.QUICK_MOVE, mpPlayer);
        assertTrue(mpInventory.getStackInSlot(0).isEmpty());
        assertEquals(0, mpContainer.getOccupied());

        // Player inventory should now have seed 201 back
        boolean foundInPlayer = false;
        for (int i = 0; i < mpPlayer.inventory.getSizeInventory(); i++) {
            ItemStack stack = mpPlayer.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack != mpCarrier && stack.hasTagCompound() && stack.getTagCompound().getInteger("identity") == 201) {
                foundInPlayer = true;
                break;
            }
        }
        assertTrue(foundInPlayer);

        // Reopen to verify carrier is now empty
        PortableCropInventory check2 = PortableCropInventory.open(mpPlayer, 0);
        assertNotNull(check2);
        assertTrue(check2.isEmpty());
    }

    @Test public void entityPlayerMPPaginationAndInsertSeedsOnSecondPage() {
        net.minecraft.entity.player.EntityPlayerMP mpPlayer = mock(net.minecraft.entity.player.EntityPlayerMP.class);
        mpPlayer.world = world;
        mpPlayer.inventory = new InventoryPlayer(mpPlayer);
        ItemStack mpCarrier = new ItemStack(SeedFixtures.portable);
        mpPlayer.inventory.setInventorySlotContents(0, mpCarrier);
        PortableCropInventory mpInventory = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest mpContainer = new ContainerCropDatabaseChest(mpPlayer.inventory, mpInventory);

        // Change to page 1 (second page, slots 25 to 49)
        assertTrue(mpContainer.enchantItem(mpPlayer, 1));
        assertEquals(1, mpContainer.getPage());
        assertTrue(mpContainer.getPosition(25) >= 0);
        assertEquals(-1, mpContainer.getPosition(0));

        // Put seed in player slot 9 and deposit it into chest slot 25
        mpPlayer.inventory.setInventorySlotContents(9, SeedFixtures.bag(301, 4));
        mpContainer.slotClick(500, 0, ClickType.PICKUP, mpPlayer);
        mpContainer.slotClick(25, 0, ClickType.PICKUP, mpPlayer);
        assertTrue(mpPlayer.inventory.getItemStack().isEmpty());
        assertEquals(301, mpInventory.getStackInSlot(25).getTagCompound().getInteger("identity"));

        // Return to page 0
        assertTrue(mpContainer.enchantItem(mpPlayer, 0));
        assertEquals(0, mpContainer.getPage());
        assertTrue(mpContainer.getPosition(0) >= 0);
        assertEquals(-1, mpContainer.getPosition(25));
        assertTrue(mpInventory.getStackInSlot(0).isEmpty());

        // Reopen and check slot 25 persists
        PortableCropInventory reopened = PortableCropInventory.open(mpPlayer, 0);
        assertNotNull(reopened);
        assertEquals(301, reopened.getStackInSlot(25).getTagCompound().getInteger("identity"));
    }

    @Test public void entityPlayerMPMultipleOpenCloseCyclesPreserveInventory() {
        net.minecraft.entity.player.EntityPlayerMP mpPlayer = mock(net.minecraft.entity.player.EntityPlayerMP.class);
        mpPlayer.world = world;
        mpPlayer.inventory = new InventoryPlayer(mpPlayer);
        ItemStack mpCarrier = new ItemStack(SeedFixtures.portable);
        mpPlayer.inventory.setInventorySlotContents(0, mpCarrier);

        // Session 1: open, add 3 seeds, close
        PortableCropInventory inv1 = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest container1 = new ContainerCropDatabaseChest(mpPlayer.inventory, inv1);
        mpPlayer.inventory.setInventorySlotContents(9, SeedFixtures.bag(401, 4));
        mpPlayer.inventory.setInventorySlotContents(10, SeedFixtures.bag(402, 4));
        mpPlayer.inventory.setInventorySlotContents(11, SeedFixtures.bag(403, 4));

        container1.slotClick(500, 0, ClickType.PICKUP, mpPlayer);
        container1.slotClick(0, 0, ClickType.PICKUP, mpPlayer);
        container1.slotClick(501, 0, ClickType.PICKUP, mpPlayer);
        container1.slotClick(1, 0, ClickType.PICKUP, mpPlayer);
        container1.slotClick(502, 0, ClickType.PICKUP, mpPlayer);
        container1.slotClick(2, 0, ClickType.PICKUP, mpPlayer);
        assertEquals(3, container1.getOccupied());
        container1.onContainerClosed(mpPlayer);

        // Session 2: open new container, verify seeds, remove slot 1, close
        PortableCropInventory inv2 = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest container2 = new ContainerCropDatabaseChest(mpPlayer.inventory, inv2);
        assertEquals(3, container2.getOccupied());
        assertEquals(401, inv2.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertEquals(402, inv2.getStackInSlot(1).getTagCompound().getInteger("identity"));
        assertEquals(403, inv2.getStackInSlot(2).getTagCompound().getInteger("identity"));

        // Extract slot 1 to cursor, deposit to slot 15 (container slot 506)
        container2.slotClick(1, 0, ClickType.PICKUP, mpPlayer);
        container2.slotClick(506, 0, ClickType.PICKUP, mpPlayer);
        assertEquals(2, container2.getOccupied());
        container2.onContainerClosed(mpPlayer);

        // Session 3: open new container, verify only slot 0 and slot 2 remain
        PortableCropInventory inv3 = PortableCropInventory.open(mpPlayer, 0);
        ContainerCropDatabaseChest container3 = new ContainerCropDatabaseChest(mpPlayer.inventory, inv3);
        assertEquals(2, container3.getOccupied());
        assertEquals(401, inv3.getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertTrue(inv3.getStackInSlot(1).isEmpty());
        assertEquals(403, inv3.getStackInSlot(2).getTagCompound().getInteger("identity"));
        assertEquals(402, mpPlayer.inventory.getStackInSlot(15).getTagCompound().getInteger("identity"));
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
