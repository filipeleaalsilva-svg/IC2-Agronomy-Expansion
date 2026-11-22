package br.cropmanager.chest;

import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.PortableCropInventory;
import br.cropmanager.chest.item.PortableSeedPickup;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PortableSeedPickupTest {
    // FML adds these flags through ASM in game; plain JUnit needs the equivalent fixture.
    private static final class TestPickupEvent extends EntityItemPickupEvent {
        TestPickupEvent(EntityPlayer player, EntityItem item) { super(player, item); }
        @Override public boolean isCancelable() { return true; }
        @Override public boolean hasResult() { return true; }
    }
    private EntityPlayer player;
    private World world;
    private ItemStack carrier;
    private PortableCropInventory storage;
    private AtomicReference<ItemStack> ground;
    @BeforeClass public static void bootstrap() { SeedFixtures.bootstrap(); }
    @Before public void setup() throws ReflectiveOperationException {
        world = mock(World.class); player = mock(EntityPlayer.class); player.world = world;
        player.inventory = new InventoryPlayer(player);
        java.lang.reflect.Field capabilities = EntityPlayer.class.getDeclaredField("capabilities");
        capabilities.setAccessible(true); capabilities.set(player, new PlayerCapabilities());
        when(player.getName()).thenReturn("PickupTester");
        carrier = new ItemStack(SeedFixtures.portable); player.inventory.setInventorySlotContents(0, carrier);
        storage = PortableCropInventory.open(player, 0);
    }
    private EntityItem entity(ItemStack stack) {
        ground = new AtomicReference<>(stack);
        EntityItem entity = mock(EntityItem.class); entity.world = world; entity.lifespan = 6000;
        when(entity.getItem()).thenAnswer(call -> ground.get());
        doAnswer(call -> { ground.set(call.getArgument(0)); return null; }).when(entity).setItem(any(ItemStack.class));
        return entity;
    }
    private void fill(int count) { for (int i = 0; i < count; i++) storage.seeds().setStackInSlot(i, SeedFixtures.bag(i, 4)); }
    private void fillPlayer() { for (int i = 1; i < 36; i++) player.inventory.setInventorySlotContents(i, new ItemStack(Items.STICK, 64)); }
    @Test public void disabledAndNonSeedPickupsRemainVanilla() {
        EntityItem seed = entity(SeedFixtures.bag(999, 0));
        EntityItemPickupEvent event = new TestPickupEvent(player, seed);
        PortableSeedPickup.pickup(event);
        assertFalse(event.isCanceled()); assertTrue(storage.isEmpty());
        verify(seed, never()).setItem(any(ItemStack.class));
        storage.setPickupEnabled(true);
        EntityItem ordinary = entity(new ItemStack(Items.WHEAT_SEEDS));
        event = new TestPickupEvent(player, ordinary); PortableSeedPickup.pickup(event);
        assertFalse(event.isCanceled()); assertTrue(PortableCropInventory.open(player, 0).isEmpty());
    }
    @Test public void enabledPortableCollectsWithFullPlayerInventoryAndPreservesNbt() {
        storage.setPickupEnabled(true); fillPlayer();
        ItemStack incoming = SeedFixtures.bag(999, 0); NBTTagCompound before = incoming.serializeNBT();
        EntityItem item = entity(incoming); EntityItemPickupEvent event = new TestPickupEvent(player, item);
        PortableSeedPickup.pickup(event);
        assertTrue(event.isCanceled()); assertTrue(ground.get().isEmpty());
        verify(item).setDead(); verify(player).onItemPickup(item, 1);
        assertEquals(before, incoming.serializeNBT());
        assertEquals(before, PortableCropInventory.open(player, 0).getStackInSlot(0).serializeNBT());
        assertEquals(Items.STICK, player.inventory.getStackInSlot(1).getItem());
    }
    @Test public void fullPortableLeavesTheEventForNormalPickup() {
        fill(500); storage.setPickupEnabled(true);
        EntityItem item = entity(SeedFixtures.bag(999, 4)); EntityItemPickupEvent event = new TestPickupEvent(player, item);
        PortableSeedPickup.pickup(event);
        assertFalse(event.isCanceled()); assertEquals(1, ground.get().getCount());
        verify(item, never()).setDead();
    }
    @Test public void partialPickupKeepsTheRemainderOnTheGroundWhenPlayerIsFull() {
        fill(499); storage.setPickupEnabled(true); fillPlayer();
        ItemStack incoming = SeedFixtures.bag(999, 4); incoming.setCount(3);
        EntityItem item = entity(incoming); EntityItemPickupEvent event = new TestPickupEvent(player, item);
        PortableSeedPickup.pickup(event);
        assertTrue(event.isCanceled()); assertEquals(2, ground.get().getCount()); assertEquals(3, incoming.getCount());
        assertEquals(999, PortableCropInventory.open(player, 0).getStackInSlot(499).getTagCompound().getInteger("identity"));
        verify(item, never()).setDead(); verify(player, never()).onItemPickup(any(), anyInt());
    }
    @Test public void partialPickupUsesNormalInventoryForTheRemainder() {
        fill(499); storage.setPickupEnabled(true);
        ItemStack incoming = SeedFixtures.bag(999, 4); incoming.setCount(2);
        EntityItem item = entity(incoming); EntityItemPickupEvent event = new TestPickupEvent(player, item);
        PortableSeedPickup.pickup(event);
        assertTrue(event.isCanceled()); assertTrue(ground.get().isEmpty());
        assertEquals(999, player.inventory.getStackInSlot(1).getTagCompound().getInteger("identity"));
        verify(player).onItemPickup(item, 2);
    }
    @Test public void pickupWhileGuiIsOpenUsesTheLiveInventoryInsteadOfAStaleSnapshot() {
        storage.setPickupEnabled(true); storage.seeds().setStackInSlot(0, SeedFixtures.bag(1, 4));
        player.openContainer = new ContainerCropDatabaseChest(player.inventory, storage);
        assertTrue(PortableSeedPickup.store(player, SeedFixtures.bag(2, 4)).isEmpty());
        assertEquals(2, storage.getStackInSlot(1).getTagCompound().getInteger("identity"));
        storage.seeds().setStackInSlot(2, SeedFixtures.bag(3, 4));
        PortableCropInventory loaded = PortableCropInventory.open(player, 0);
        for (int i = 0; i < 3; i++) assertEquals(i + 1, loaded.getStackInSlot(i).getTagCompound().getInteger("identity"));
    }
    @Test public void multiplePortablesUseTheOffhandAfterTheFirstOneFills() {
        fill(500); storage.setPickupEnabled(true);
        ItemStack offhand = new ItemStack(SeedFixtures.portable); player.inventory.setInventorySlotContents(40, offhand);
        PortableCropInventory.open(player, 40).setPickupEnabled(true);
        assertTrue(PortableSeedPickup.store(player, SeedFixtures.bag(999, 4)).isEmpty());
        assertEquals(999, PortableCropInventory.open(player, 40).getStackInSlot(0).getTagCompound().getInteger("identity"));
        assertEquals(499, PortableCropInventory.open(player, 0).getStackInSlot(499).getTagCompound().getInteger("identity"));
    }
    @Test public void cancelledDeniedDelayedAndOtherOwnersItemsAreRespected() {
        storage.setPickupEnabled(true);
        EntityItem item = entity(SeedFixtures.bag(999, 4));
        EntityItemPickupEvent cancelled = new TestPickupEvent(player, item); cancelled.setCanceled(true); PortableSeedPickup.pickup(cancelled);
        EntityItemPickupEvent denied = new TestPickupEvent(player, item); denied.setResult(Event.Result.DENY); PortableSeedPickup.pickup(denied);
        when(item.cannotPickup()).thenReturn(true); PortableSeedPickup.pickup(new TestPickupEvent(player, item));
        when(item.cannotPickup()).thenReturn(false); when(item.getOwner()).thenReturn("SomeoneElse"); PortableSeedPickup.pickup(new TestPickupEvent(player, item));
        assertTrue(PortableCropInventory.open(player, 0).isEmpty());
        verify(item, never()).setItem(any(ItemStack.class));
    }
}
