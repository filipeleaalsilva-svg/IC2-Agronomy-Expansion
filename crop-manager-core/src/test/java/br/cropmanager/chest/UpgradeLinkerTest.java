package br.cropmanager.chest;

import br.cropmanager.CropManager;
import br.cropmanager.chest.crafting.PortableLinkRecipe;
import br.cropmanager.chest.integration.CropInformation;
import br.cropmanager.chest.inventory.PortableCropInventory;
import br.cropmanager.chest.item.ItemUpgradeLinker;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropSeed;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public final class UpgradeLinkerTest {
    @BeforeClass
    public static void setup() {
        Bootstrap.register();
    }

    @Test
    public void linkerStoresAndCopiesCoordinates() {
        ItemStack linker = new ItemStack(CropManager.UPGRADE_LINKER);
        assertFalse(ItemUpgradeLinker.hasLink(linker));

        ItemUpgradeLinker.setLink(linker, -1, new BlockPos(12, 64, -30));
        assertTrue(ItemUpgradeLinker.hasLink(linker));
        NBTTagCompound link = ItemUpgradeLinker.getLink(linker);
        assertNotNull(link);
        assertEquals(-1, link.getInteger("Dim"));
        assertEquals(12, link.getInteger("X"));
        assertEquals(64, link.getInteger("Y"));
        assertEquals(-30, link.getInteger("Z"));

        ItemStack portable = new ItemStack(CropManager.PORTABLE_DATABASE);
        ItemUpgradeLinker.copyLink(linker, portable);
        assertTrue(ItemUpgradeLinker.hasLink(portable));
        assertEquals(12, ItemUpgradeLinker.getLink(portable).getInteger("X"));
    }

    @Test
    public void portableLinkRecipePreservesPortableDataAndRequiresBoundLinker() {
        PortableLinkRecipe recipe = new PortableLinkRecipe();
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) { return false; }
        }, 3, 3);

        ItemStack portable = new ItemStack(CropManager.PORTABLE_DATABASE);
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("Pickup", true);
        portable.setTagCompound(new NBTTagCompound());
        portable.getTagCompound().setTag(PortableCropInventory.DATA, data);

        ItemStack unboundLinker = new ItemStack(CropManager.UPGRADE_LINKER);
        inv.setInventorySlotContents(0, portable);
        inv.setInventorySlotContents(1, unboundLinker);
        assertFalse(recipe.matches(inv, null));

        ItemStack boundLinker = new ItemStack(CropManager.UPGRADE_LINKER);
        ItemUpgradeLinker.setLink(boundLinker, 0, new BlockPos(100, 70, 200));
        inv.setInventorySlotContents(1, boundLinker);
        assertTrue(recipe.matches(inv, null));

        ItemStack out = recipe.getCraftingResult(inv);
        assertEquals(CropManager.PORTABLE_DATABASE, out.getItem());
        assertTrue(ItemUpgradeLinker.hasLink(out));
        assertTrue(out.getTagCompound().getCompoundTag(PortableCropInventory.DATA).getBoolean("Pickup"));
    }

    @Test
    public void cropInformationReadsExtendedCropnalyzerDetails() {
        CropCard card = mock(CropCard.class);
        when(card.getUnlocalizedName()).thenReturn("crop.test");
        when(card.getDiscoveredBy()).thenReturn("Notch");
        when(card.getMaxSize()).thenReturn(4);
        when(card.getAttributes()).thenReturn(new String[]{"Red", "Flower"});
        when(card.getProperties()).thenReturn(new CropProperties(3, 1, 2, 3, 4, 5));

        TestSeedItem seed = new TestSeedItem(card);
        ItemStack stack = new ItemStack(seed);
        CropInformation info = CropInformation.read(stack);

        assertEquals("crop.test", info.nameKey);
        assertEquals("Notch", info.discoveredBy);
        assertEquals(4, info.scanLevel);
        assertEquals(4, info.maxSize);
        assertEquals(3, info.tier);
        assertEquals(20, info.growth);
        assertEquals(21, info.gain);
        assertEquals(5, info.resistance);
        assertEquals(2, info.attributes.size());
        assertEquals("Red", info.attributes.get(0));
    }

    private static final class TestSeedItem extends Item implements ICropSeed {
        private final CropCard card;
        TestSeedItem(CropCard card) { this.card = card; }
        @Override public CropCard getCropFromStack(ItemStack is) { return card; }
        @Override public void setCropFromStack(ItemStack is, CropCard crop) {}
        @Override public int getGrowthFromStack(ItemStack is) { return 20; }
        @Override public void setGrowthFromStack(ItemStack is, int growth) {}
        @Override public int getGainFromStack(ItemStack is) { return 21; }
        @Override public void setGainFromStack(ItemStack is, int gain) {}
        @Override public int getResistanceFromStack(ItemStack is) { return 5; }
        @Override public void setResistanceFromStack(ItemStack is, int resistance) {}
        @Override public int getScannedFromStack(ItemStack is) { return 4; }
        @Override public void setScannedFromStack(ItemStack is, int scanned) {}
        @Override public void incrementScannedFromStack(ItemStack is) {}
    }
}
