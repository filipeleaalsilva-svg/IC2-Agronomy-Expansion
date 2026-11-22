package br.cropmanager;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Uses real Minecraft/Forge objects; does not assert that a game was launched. */
public class PersistenceTest {
    @BeforeClass public static void bootstrapVanilla() {
        Bootstrap.register();
        if (!net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.containsKey(CropManager.WIRELESS_EU.getRegistryName())) {
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(CropManager.WIRELESS_EU);
        }
        TileEntity.register("cropmanager:crop_manager", ManagerTile.class);
        TileEntity.register("cropmanager:advanced_crop_manager", AdvancedManagerTile.class);
        TileEntity.register("cropmanager:agricultural_monitor", MonitorTile.class);
        TileEntity.register("cropmanager:electric_composter", ComposterTile.class);
        TileEntity.register("cropmanager:weed_ex_fabricator", WeedExFabricatorTile.class);
    }

    @Test public void restoresEnergyFluidsOutputsSettingsAndPendingProducts() {
        ManagerTile source = new ManagerTile();
        source.energy.setEnergyStored(12345.5);
        source.water.fill(new FluidStack(FluidRegistry.WATER, 4567), true);
        source.inventory.setStackInSlot(3, new ItemStack(Items.WHEAT, 37));
        source.settings(8, 5, 9);
        NBTTagCompound saved = source.writeToNBT(new NBTTagCompound());
        saved.setInteger("Cursor", 123); saved.setDouble("Progress", 0.75);
        NBTTagCompound product = new ItemStack(Items.WHEAT_SEEDS, 1).writeToNBT(new NBTTagCompound());
        product.setInteger("PendingCount", 200);
        NBTTagList pending = new NBTTagList(); pending.appendTag(product); saved.setTag("Pending", pending);
        ManagerTile restored = new ManagerTile(); restored.readFromNBT(saved);
        NBTTagCompound again = restored.writeToNBT(new NBTTagCompound());
        assertEquals(12345.5, restored.getEnergy(), 0);
        assertEquals(4567, restored.water.getFluidAmount());
        assertEquals(37, restored.inventory.getStackInSlot(3).getCount());
        assertEquals(8, restored.radius); assertEquals(5, restored.layers); assertEquals(15, restored.functions);
        assertEquals(123, again.getInteger("Cursor"));
        assertEquals(0.75, again.getDouble("Progress"), 0);
        assertEquals(200, again.getTagList("Pending", 10).getCompoundTagAt(0).getInteger("PendingCount"));
        assertEquals(Items.WHEAT_SEEDS, new ItemStack(again.getTagList("Pending", 10).getCompoundTagAt(0)).getItem());
    }

    @Test public void automationCannotInsertIntoOutputsOrExtractInputs() {
        ManagerTile tile = new ManagerTile();
        IItemHandler output = tile.itemsForFace(EnumFacing.DOWN);
        IItemHandler input = tile.itemsForFace(EnumFacing.UP);
        ItemStack wheat = new ItemStack(Items.WHEAT, 12);
        assertEquals(12, output.insertItem(ManagerTile.OUTPUT_START, wheat, false).getCount());
        assertTrue(tile.inventory.getStackInSlot(3).isEmpty());
        tile.inventory.setStackInSlot(0, wheat.copy());
        assertTrue(input.extractItem(0, 12, false).isEmpty());
        assertEquals(12, tile.inventory.getStackInSlot(0).getCount());
        tile.inventory.setStackInSlot(3, wheat.copy());
        assertEquals(5, output.extractItem(ManagerTile.OUTPUT_START, 5, false).getCount());
        assertEquals(7, tile.inventory.getStackInSlot(3).getCount());
    }

    @Test public void fullOutputRejectsItemsWithoutDestroyingRemainder() {
        ManagerTile tile = new ManagerTile();
        tile.inventory.setStackInSlot(3, new ItemStack(Items.WHEAT, 63));
        ItemStack remainder = tile.inventory.insertItem(3, new ItemStack(Items.WHEAT, 9), false);
        assertEquals(8, remainder.getCount());
        assertEquals(64, tile.inventory.getStackInSlot(3).getCount());
    }

    @Test public void repairsNonFiniteStoredEnergy() {
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            NBTTagCompound saved = new ManagerTile().writeToNBT(new NBTTagCompound());
            saved.getCompoundTag("IC2BasicSink").setDouble("energy", invalid);
            ManagerTile restored = new ManagerTile();
            restored.readFromNBT(saved);
            assertEquals(0, restored.getEnergy(), 0);
        }
    }

    @Test public void composterRejectsNonIc2UpgradesAndAutomationOnlyInsertsOrganics() {
        ComposterTile composter = new ComposterTile();
        ItemStack weedKiller = new ItemStack(CropManager.WEED_KILLER);
        assertFalse(ManagerTile.isIc2Upgrade(weedKiller));
        assertFalse(composter.inventory.isItemValid(2, weedKiller));
        IItemHandler face = composter.itemsForFace(EnumFacing.UP);
        assertEquals(1, face.insertItem(2, weedKiller, false).getCount());
        assertEquals(1, face.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount());
        assertTrue(face.insertItem(0, new ItemStack(Items.WHEAT, 8), false).isEmpty());
    }

    @Test public void monitorWirelessEuUpgradeScalesBufferTransfersEnergyAndPersists() {
        MonitorTile monitor = new MonitorTile();
        ManagerTile manager = new AdvancedManagerTile();
        assertEquals(0, monitor.wirelessEuUpgrades());
        assertEquals(1000.0, monitor.energy.injectEnergy(EnumFacing.UP, 1000.0, 128), 0);
        assertEquals(0.0, monitor.energy.getEnergyStored(), 0);

        monitor.inventory.setStackInSlot(MonitorTile.WIRELESS_EU_SLOT, new ItemStack(CropManager.WIRELESS_EU, 2));
        monitor.recalculateWirelessEu();
        assertEquals(2, monitor.wirelessEuUpgrades());
        assertEquals(100000.0, monitor.wirelessEuCapacity(), 0);
        assertEquals(512, monitor.wirelessEuRate());
        assertEquals(3, monitor.energy.getSinkTier());

        double rejected = monitor.energy.injectEnergy(EnumFacing.UP, 4000.0, 512);
        assertEquals(0.0, rejected, 0);
        assertEquals(4000.0, monitor.energy.getEnergyStored(), 0);

        double transferred = monitor.transferWirelessEnergyTo(manager.energy, monitor.wirelessEuRate());
        assertEquals(512.0, transferred, 0);
        assertEquals(512.0, manager.getEnergy(), 0);
        assertEquals(3488.0, monitor.energy.getEnergyStored(), 0);

        NBTTagCompound saved = monitor.writeToNBT(new NBTTagCompound());
        MonitorTile restored = new MonitorTile();
        restored.readFromNBT(saved);
        assertEquals(2, restored.wirelessEuUpgrades());
        assertEquals(3488.0, restored.energy.getEnergyStored(), 0);
        assertEquals(100000.0, restored.wirelessEuCapacity(), 0);
    }

    @Test public void weedExMixerMigratesFiveSlotSavesWithoutDeletingItems() {
        ItemStackHandler legacy = new ItemStackHandler(5);
        legacy.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE, 3));
        legacy.setStackInSlot(1, new ItemStack(Items.REDSTONE, 12));
        legacy.setStackInSlot(3, new ItemStack(Items.GLASS_BOTTLE, 4));
        legacy.setStackInSlot(4, new ItemStack(Items.BUCKET, 1));
        NBTTagCompound tag = new WeedExFabricatorTile().writeToNBT(new NBTTagCompound());
        tag.setTag("Inventory", legacy.serializeNBT());

        WeedExFabricatorTile migrated = new WeedExFabricatorTile();
        migrated.readFromNBT(tag);
        assertEquals(12, migrated.inventory.getStackInSlot(WeedExFabricatorTile.REDSTONE).getCount());
        assertEquals(4, migrated.inventory.getStackInSlot(WeedExFabricatorTile.BOTTLE_IN).getCount());
        assertEquals(1, migrated.inventory.getStackInSlot(WeedExFabricatorTile.CONTAINER_OUT).getCount());
        NBTTagCompound savedAgain = migrated.writeToNBT(new NBTTagCompound());
        assertEquals(1, savedAgain.getTagList("LegacyDrops", 10).tagCount());
        assertEquals(3, new ItemStack(savedAgain.getTagList("LegacyDrops", 10).getCompoundTagAt(0)).getCount());
    }

    @Test public void monitorSnapshotsManagerOutputSlotsAndRespectsWirelessRateLimit() {
        MonitorTile monitor = new MonitorTile();
        AdvancedManagerTile manager = new AdvancedManagerTile();
        manager.inventory.setStackInSlot(ManagerTile.OUTPUT_START, new ItemStack(Items.WHEAT, 19));
        manager.inventory.setStackInSlot(ManagerTile.OUTPUT_END - 1, new ItemStack(Items.WHEAT_SEEDS, 7));

        monitor.inventory.setStackInSlot(MonitorTile.WIRELESS_EU_SLOT, new ItemStack(CropManager.WIRELESS_EU, 1));
        monitor.recalculateWirelessEu();
        monitor.energy.setEnergyStored(10000.0);
        double sent = monitor.transferWirelessEnergyTo(manager.energy, monitor.wirelessEuRate());
        assertEquals(256.0, sent, 0);
        assertEquals(256.0, manager.getEnergy(), 0);

        NBTTagCompound snap = MonitorTile.readTarget(manager);
        assertEquals(256.0, snap.getDouble("EU"), 0);
        assertEquals("manager", snap.getString("Target"));
        // Verify Weed-Ex mixer slot layout invariants (9 slots: redstone at 0, output at 3, upgrades at 4..7, battery at 8)
        WeedExFabricatorTile mixer = new WeedExFabricatorTile();
        assertEquals(9, mixer.inventory.getSlots());
        assertTrue(mixer.inventory.isItemValid(WeedExFabricatorTile.REDSTONE, new ItemStack(Items.REDSTONE)));
        assertFalse(mixer.inventory.isItemValid(WeedExFabricatorTile.REDSTONE, new ItemStack(Items.SPIDER_EYE)));
        assertFalse(mixer.inventory.isItemValid(WeedExFabricatorTile.CONTAINER_OUT, new ItemStack(Items.BUCKET)));
    }

    @Test public void composterAcceptsOreDictionaryOrganicsAndRejectsMineralOres() {
        net.minecraftforge.oredict.OreDictionary.registerOre("treeSapling", new ItemStack(Items.STICK));
        assertTrue(ComposterTile.organic(new ItemStack(Items.STICK)));
        assertFalse(ComposterTile.organic(new ItemStack(Items.DIAMOND)));
    }

    @Test public void enderIoCapabilityQueryAcrossAllSixFaces() throws Exception {
        net.minecraftforge.common.capabilities.Capability<IItemHandler> itemCap = org.mockito.Mockito.mock(net.minecraftforge.common.capabilities.Capability.class);
        org.mockito.Mockito.when(itemCap.cast(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        java.lang.reflect.Field itemField = net.minecraftforge.items.CapabilityItemHandler.class.getDeclaredField("ITEM_HANDLER_CAPABILITY");
        itemField.setAccessible(true);
        itemField.set(null, itemCap);

        AdvancedManagerTile advanced = new AdvancedManagerTile();
        for (EnumFacing face : EnumFacing.VALUES) {
            boolean hasItem = advanced.hasCapability(itemCap, face);
            IItemHandler itemHandler = advanced.getCapability(itemCap, face);
            if (face == EnumFacing.DOWN) {
                assertFalse("Bottom face must reject ITEM_HANDLER to avoid Ender IO crash", hasItem);
                assertNull("Bottom face must return null handler", itemHandler);
            } else {
                assertTrue("Non-bottom face must support ITEM_HANDLER: " + face, hasItem);
                assertNotNull("Non-bottom face must return handler: " + face, itemHandler);
            }
        }
        // Null face query should also be handled gracefully
        assertNotNull(advanced.getCapability(itemCap, null));
    }
}
