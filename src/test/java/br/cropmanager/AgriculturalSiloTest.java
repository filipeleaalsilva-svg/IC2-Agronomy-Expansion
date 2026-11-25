package br.cropmanager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Archetype B test suite for the EU-free 3x5x3 Agricultural Grain Silo. */
public final class AgriculturalSiloTest {

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void boot() throws ReflectiveOperationException {
        Bootstrap.register();
        Field providersField = CapabilityManager.class.getDeclaredField("providers");
        providersField.setAccessible(true);
        Map<String, Capability<?>> providers =
                (Map<String, Capability<?>>) providersField.get(CapabilityManager.INSTANCE);
        if (!providers.containsKey(IItemHandler.class.getName().intern())) {
            CapabilityItemHandler.register();
        }
        if (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == null) {
            Field itemCapField = CapabilityItemHandler.class.getDeclaredField("ITEM_HANDLER_CAPABILITY");
            itemCapField.setAccessible(true);
            itemCapField.set(null, providers.get(IItemHandler.class.getName().intern()));
        }
        if (TileEntity.getKey(AgriculturalSiloTile.class) == null) {
            TileEntity.register("cropmanager:agricultural_silo", AgriculturalSiloTile.class);
        }
        if (TileEntity.getKey(StorageCasingTile.class) == null) {
            TileEntity.register("cropmanager:storage_casing", StorageCasingTile.class);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.RESERVOIR_CAPACITY.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.RESERVOIR_CAPACITY);
        }
    }

    private static final class MockStructure {
        final World world = mock(World.class);
        final BlockPos center = new BlockPos(0, 64, 0);
        final BlockPos controllerPos = center.north().up();
        final IBlockState controllerState = CropManager.AGRICULTURAL_SILO.getDefaultState()
                .withProperty(AgriculturalSiloBlock.FACING, EnumFacing.NORTH);
        final IBlockState casingState = CropManager.SILO_CASING.getDefaultState();
        final AgriculturalSiloTile controller = new AgriculturalSiloTile();
        final Map<BlockPos, StorageCasingTile> casings = new HashMap<>();
        final Map<BlockPos, IBlockState> overrides = new HashMap<>();
        final Map<BlockPos, TileEntity> externalTiles = new HashMap<>();
        boolean powered = false;

        MockStructure() {
            when(world.getHeight()).thenReturn(256);
            when(world.isBlockLoaded(any(BlockPos.class), eq(false))).thenReturn(true);
            when(world.isBlockPowered(any(BlockPos.class))).thenAnswer(call -> powered);
            controller.setWorld(world);
            controller.setPos(controllerPos);
            for (int y = 0; y < AgriculturalSiloTile.FIXED_HEIGHT; y++) {
                boolean cap = y == 0 || y == AgriculturalSiloTile.FIXED_HEIGHT - 1;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (!cap && dx == 0 && dz == 0) continue;
                        BlockPos p = new BlockPos(center.getX() + dx, center.getY() + y, center.getZ() + dz);
                        if (!p.equals(controllerPos)) {
                            StorageCasingTile ct = new StorageCasingTile();
                            ct.setWorld(world);
                            ct.setPos(p);
                            casings.put(p, ct);
                        }
                    }
                }
            }
            when(world.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos pos = call.getArgument(0);
                if (overrides.containsKey(pos)) return overrides.get(pos);
                if (pos.equals(controllerPos)) return controllerState;
                if (casings.containsKey(pos)) return casingState;
                return Blocks.AIR.getDefaultState();
            });
            when(world.getTileEntity(any(BlockPos.class))).thenAnswer(call -> {
                BlockPos pos = call.getArgument(0);
                if (pos.equals(controllerPos)) return controller;
                if (casings.containsKey(pos)) return casings.get(pos);
                return externalTiles.get(pos);
            });
        }
    }

    @Test
    public void formsWith41CasingsRejectsInvalidCenterAndDefersUnloaded() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        assertEquals(41, s.casings.size());
        for (StorageCasingTile casing : s.casings.values()) {
            assertSame(s.controller, casing.controller());
        }

        // Unloaded shell block must defer without unforming
        when(s.world.isBlockLoaded(eq(s.center.up(4)), eq(false))).thenReturn(false);
        when(s.world.getTotalWorldTime()).thenReturn(20L);
        s.controller.update();
        assertTrue("Unloaded block must defer", s.controller.formed);

        // Restoring loaded + putting non-air inside core unforms cleanly
        when(s.world.isBlockLoaded(eq(s.center.up(4)), eq(false))).thenReturn(true);
        s.overrides.put(s.center.up(2), Blocks.STONE.getDefaultState());
        s.controller.update();
        assertFalse("Core obstruction must unform", s.controller.formed);

        // Clear core, form again, then break a casing -> unforms and unlinks casings
        s.overrides.remove(s.center.up(2));
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        BlockPos corner = new BlockPos(1, 68, 1);
        s.casings.get(corner).notifyControllerBroken();
        assertFalse(s.controller.formed);
        assertNull(s.casings.get(new BlockPos(-1, 64, -1)).controller());

        // Re-form, then break controller block -> unforms and unlinks all casings
        s.controller.tryForm();
        assertTrue(s.controller.formed);
        CropManager.AGRICULTURAL_SILO.breakBlock(s.world, s.controllerPos, s.controllerState);
        assertFalse(s.controller.formed);
        for (StorageCasingTile casing : s.casings.values()) {
            assertNull(casing.controller());
        }
    }

    @Test
    public void rejectsDuplicateControllerInShell() {
        MockStructure s = new MockStructure();
        s.overrides.put(s.controllerPos.up(), s.controllerState);
        s.controller.tryForm();
        assertFalse(s.controller.formed);
    }

    @Test
    public void hasZeroEuDependencyAndSupportsUpTo8CapacityUpgrades() {
        AgriculturalSiloTile tile = new AgriculturalSiloTile();
        assertFalse(tile.isSupportMachine());
        assertNull(tile.energySink());
        assertFalse(ic2.api.energy.tile.IEnergySink.class.isAssignableFrom(StorageCasingTile.class));
        assertFalse(ic2.api.energy.tile.IEnergySink.class.isAssignableFrom(AgriculturalSiloTile.class));

        assertEquals(128000, tile.getCapacity());
        ItemStack upg = new ItemStack(CropManager.RESERVOIR_CAPACITY, 8);
        assertTrue(tile.upgrades.isItemValid(0, upg));
        tile.upgrades.setStackInSlot(0, upg);
        assertEquals(384000, tile.getCapacity());

        // Fill near max, then remove upgrades -> items above new capacity must be preserved safely
        ItemStack bigStack = new ItemStack(Items.WHEAT, 64);
        for (int i = 0; i < 5468; i++) {
            assertTrue(tile.insertBulk(bigStack, false).isEmpty());
        }
        assertTrue(tile.insertBulk(new ItemStack(Items.WHEAT, 48), false).isEmpty());
        assertEquals(350000, tile.getStoredCount());

        tile.upgrades.setStackInSlot(0, ItemStack.EMPTY);
        assertEquals(128000, tile.getCapacity());
        assertEquals(350000, tile.getStoredCount());
        assertEquals(64, tile.insertBulk(new ItemStack(Items.WHEAT, 64), false).getCount());
        assertEquals(64, tile.extractBulk(64, false).getCount());
        assertEquals(349936, tile.getStoredCount());
    }

    @Test
    public void lockingNonStackableRejectionAndBulkInsertExtract() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();

        // Non-stackable items are rejected
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        assertFalse(s.controller.acceptsItem(pickaxe));
        assertEquals(1, s.controller.insertBulk(pickaxe, false).getCount());

        // Simulate insert/extract does not mutate
        assertTrue(s.controller.insertBulk(new ItemStack(Items.WHEAT, 32), true).isEmpty());
        assertEquals(0, s.controller.getStoredCount());

        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        player.world = s.world;

        // Lock template via cursor item when empty
        player.inventory.setItemStack(new ItemStack(Items.WHEAT, 5));
        assertTrue(s.controller.control(2, player));
        assertTrue(s.controller.isLocked());
        assertEquals(Items.WHEAT, s.controller.getStoredTemplate().getItem());
        assertEquals(0, s.controller.getStoredCount());

        // Mismatched item rejected while locked
        assertEquals(16, s.controller.insertBulk(new ItemStack(Items.CARROT, 16), false).getCount());

        // Action 3 requires empty cursor to clear lock
        assertFalse(s.controller.control(3, player));
        assertTrue(s.controller.isLocked());
        player.inventory.setItemStack(ItemStack.EMPTY);
        assertTrue(s.controller.control(3, player));
        assertFalse(s.controller.isLocked());
        assertTrue(s.controller.getStoredTemplate().isEmpty());

        // Insert wheat, lock, toggle unlock, then drain -> template clears when unlocked & empty
        assertTrue(s.controller.insertBulk(new ItemStack(Items.WHEAT, 10), false).isEmpty());
        assertTrue(s.controller.control(2, player));
        assertTrue(s.controller.isLocked());
        assertTrue(s.controller.control(2, player));
        assertFalse(s.controller.isLocked());
        assertEquals(10, s.controller.extractBulk(64, false).getCount());
        assertTrue(s.controller.getStoredTemplate().isEmpty());
    }

    @Test
    public void sixFacesNullCapabilityRedstoneValveAndAutoEject() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();
        assertTrue(s.controller.formed);

        BlockPos topPos = new BlockPos(0, 68, 0);
        BlockPos botPos = new BlockPos(0, 64, 0);
        StorageCasingTile topCasing = s.casings.get(topPos);
        StorageCasingTile botCasing = s.casings.get(botPos);

        // Interior face of top casing is NOT exterior
        assertFalse(topCasing.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN));
        assertTrue(topCasing.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP));
        assertTrue(topCasing.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null));

        // Insert through UP input face, verify extraction blocked on INPUT face
        IItemHandler upHandler = topCasing.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        assertNotNull(upHandler);
        assertTrue(upHandler.insertItem(0, new ItemStack(Items.WHEAT, 64), false).isEmpty());
        assertTrue(upHandler.insertItem(0, new ItemStack(Items.WHEAT, 64), false).isEmpty());
        assertEquals(128, s.controller.getStoredCount());
        assertTrue(upHandler.extractItem(0, 64, true).isEmpty());

        // Null face capability returns unsided handler
        IItemHandler nullFaceHandler = topCasing.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        assertNotNull(nullFaceHandler);

        // Set Redstone mode = HIGH (1) via GUI action
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        player.world = s.world;
        assertTrue(s.controller.control(1, player));
        assertEquals(1, s.controller.redstoneMode());

        // Attach destination inventory under botPos
        final ItemStackHandler destHandler = new ItemStackHandler(4);
        TileEntity destTile = mock(TileEntity.class);
        when(destTile.isInvalid()).thenReturn(false);
        when(destTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)).thenReturn(destHandler);
        s.externalTiles.put(botPos.down(), destTile);

        IItemHandler downHandler = botCasing.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        assertNotNull(downHandler);

        // Redstone is LOW -> extract blocked, auto-eject blocked, but UP insert still allowed
        assertTrue(downHandler.extractItem(0, 64, false).isEmpty());
        s.controller.autoEjectItems();
        assertTrue(destHandler.getStackInSlot(0).isEmpty());
        assertTrue(upHandler.insertItem(0, new ItemStack(Items.WHEAT, 64), false).isEmpty());
        assertEquals(192, s.controller.getStoredCount());

        // Turn Redstone HIGH -> extract and passive auto-eject work (0 EU)
        s.powered = true;
        assertEquals(64, downHandler.extractItem(0, 64, false).getCount());
        assertEquals(128, s.controller.getStoredCount());

        // Configure BACK (relative NORTH) to OUTPUT as well -> self-casings must be skipped
        s.controller.control(10 + EnumFacing.NORTH.getIndex(), player);
        s.controller.control(10 + EnumFacing.NORTH.getIndex(), player);
        s.controller.autoEjectItems();
        assertEquals(64, destHandler.getStackInSlot(0).getCount());
        assertEquals(64, s.controller.getStoredCount());

        // Client update() is strictly a no-op
        try {
            Field remoteField = World.class.getDeclaredField("isRemote");
            remoteField.setAccessible(true);
            remoteField.setBoolean(s.world, true);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        when(s.world.getTotalWorldTime()).thenReturn(40L);
        s.controller.update();
        assertEquals(64, s.controller.getStoredCount());
    }

    @Test
    public void comparatorSignalNbtClampingAndShouldRefresh() {
        MockStructure s = new MockStructure();
        s.controller.tryForm();
        assertEquals(0, s.controller.comparatorSignal());
        StorageCasingTile someCasing = s.casings.get(new BlockPos(1, 65, 1));
        assertEquals(0, CropManager.SILO_CASING.getComparatorInputOverride(
                s.casingState, s.world, new BlockPos(1, 65, 1)));

        for (int i = 0; i < 2000; i++) {
            assertTrue(s.controller.insertBulk(new ItemStack(Items.WHEAT, 64), false).isEmpty());
        }
        assertEquals(128000, s.controller.getStoredCount());
        assertEquals(15, s.controller.comparatorSignal());
        assertEquals(15, CropManager.SILO_CASING.getComparatorInputOverride(
                s.casingState, s.world, new BlockPos(1, 65, 1)));
        assertSame(s.controller, someCasing.controller());

        // Corrupt NBT clamping + writeItemData resets formation while preserving item/count/upgrades/settings
        s.controller.upgrades.setStackInSlot(0, new ItemStack(CropManager.RESERVOIR_CAPACITY, 3));
        NBTTagCompound itemTag = s.controller.writeItemData();
        assertFalse(itemTag.hasKey("x"));
        assertFalse(itemTag.getBoolean("SiloFormed"));
        itemTag.setInteger("SiloRedstone", 99);
        itemTag.setIntArray("SiloPorts", new int[] {-5, 8, 2, 1, 0, 99});
        itemTag.getCompoundTag("Upgrades").setInteger("Size", 5);

        AgriculturalSiloTile restored = new AgriculturalSiloTile();
        restored.readFromNBT(itemTag);
        assertFalse(restored.formed);
        assertEquals(1, restored.upgrades.getSlots());
        assertEquals(3, restored.upgradeCount());
        assertEquals(128000 + 3 * 32000, restored.getCapacity());
        assertEquals(128000, restored.getStoredCount());
        assertEquals(2, restored.redstoneMode());
        assertEquals(0, restored.portMode(0));
        assertEquals(2, restored.portMode(1));
        assertEquals(2, restored.portMode(5));

        // Corrupt non-stackable stored template in NBT is rejected safely
        NBTTagCompound corruptItem = new NBTTagCompound();
        corruptItem.setTag("StoredItem", new ItemStack(Items.IRON_PICKAXE).writeToNBT(new NBTTagCompound()));
        corruptItem.setInteger("StoredCount", 50);
        corruptItem.setBoolean("Locked", true);
        AgriculturalSiloTile sanitized = new AgriculturalSiloTile();
        sanitized.readFromNBT(corruptItem);
        assertTrue(sanitized.getStoredTemplate().isEmpty());
        assertEquals(0, sanitized.getStoredCount());
        assertFalse(sanitized.isLocked());

        // shouldRefresh contract
        IBlockState same = CropManager.AGRICULTURAL_SILO.getDefaultState();
        IBlockState rotated = same.withProperty(AgriculturalSiloBlock.FACING, EnumFacing.SOUTH);
        assertFalse(restored.shouldRefresh(s.world, s.controllerPos, same, rotated));
        assertTrue(restored.shouldRefresh(s.world, s.controllerPos, same, Blocks.AIR.getDefaultState()));
        assertFalse(someCasing.shouldRefresh(s.world, someCasing.getPos(), s.casingState,
                s.casingState.withProperty(StorageCasingBlock.FORMED, Boolean.TRUE)));
        assertTrue(someCasing.shouldRefresh(s.world, someCasing.getPos(), s.casingState, Blocks.AIR.getDefaultState()));
    }

    @Test
    public void casingInteractionAndVirtualContainerSlots() {
        MockStructure s = new MockStructure();
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);
        when(player.getDistanceSq(s.controllerPos)).thenReturn(4.0);
        player.world = s.world;
        BlockPos casingPos = new BlockPos(1, 66, 1);

        assertFalse(CropManager.SILO_CASING.onBlockActivated(
                s.world, casingPos, s.casingState, player, EnumHand.MAIN_HAND, EnumFacing.EAST, .5F, .5F, .5F));
        verify(player, never()).openGui(any(), eq(17), any(), eq(s.controllerPos.getX()),
                eq(s.controllerPos.getY()), eq(s.controllerPos.getZ()));

        s.controller.tryForm();
        assertTrue(CropManager.SILO_CASING.onBlockActivated(
                s.world, casingPos, s.casingState, player, EnumHand.MAIN_HAND, EnumFacing.EAST, .5F, .5F, .5F));
        verify(player).openGui(CropManager.instance, 17, s.world,
                s.controllerPos.getX(), s.controllerPos.getY(), s.controllerPos.getZ());

        AgriculturalSiloContainer container = new AgriculturalSiloContainer(player.inventory, s.controller);
        player.openContainer = container;
        assertSame(s.controller, FarmTile.openMachine(player));

        // Virtual deposit slot inserts directly into silo bulk storage
        container.getSlot(AgriculturalSiloContainer.DEPOSIT_SLOT).putStack(new ItemStack(Items.WHEAT, 32));
        assertFalse(container.getSlot(AgriculturalSiloContainer.DEPOSIT_SLOT).getHasStack());
        assertEquals(32, s.controller.getStoredCount());
        assertEquals(32, container.getSlot(AgriculturalSiloContainer.WITHDRAW_SLOT).getStack().getCount());

        // Shift-click withdraw moves exactly one stack into player hotbar (reverse merge -> slot 8) without duplication
        assertTrue(container.transferStackInSlot(player, AgriculturalSiloContainer.WITHDRAW_SLOT).isEmpty());
        assertEquals(0, s.controller.getStoredCount());
        assertEquals(32, player.inventory.getStackInSlot(8).getCount());

        // Shift-click from player slot inserts back into silo
        player.inventory.setInventorySlotContents(9, new ItemStack(Items.WHEAT, 16));
        assertTrue(container.transferStackInSlot(player, AgriculturalSiloContainer.SILO_SLOTS).isEmpty());
        assertEquals(16, s.controller.getStoredCount());
        assertTrue(player.inventory.getStackInSlot(9).isEmpty());
    }
}
