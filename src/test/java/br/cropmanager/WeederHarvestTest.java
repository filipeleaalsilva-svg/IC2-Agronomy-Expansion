package br.cropmanager;

import ic2.api.item.IItemAPI;
import ic2.api.item.IC2Items;
import java.lang.reflect.Field;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

public final class WeederHarvestTest {
    @BeforeClass
    public static void boot() {
        Bootstrap.register();
    }

    @Test
    public void outputSlotUsesIc2ApiWeedAndHasDedicatedOutput() throws Exception {
        Field apiField = IC2Items.class.getDeclaredField("instance");
        apiField.setAccessible(true);
        Object previousApi = apiField.get(null);
        IItemAPI testApi = mock(IItemAPI.class);
        Item testWeedItem = new Item();
        when(testApi.getItemStack("crop_res", "weed")).thenReturn(new ItemStack(testWeedItem));
        apiField.set(null, testApi);
        try {
            WeederTile tile = new WeederTile();
            ItemStack weed = WeederTile.weedStack();

            assertEquals(3, tile.inventory.getSlots());
            assertFalse("The mocked IC2 API must provide its weed item", weed.isEmpty());
            assertTrue(tile.inventory.isItemValid(2, weed));
            assertFalse(tile.inventory.isItemValid(2, new ItemStack(Items.WHEAT)));
            assertTrue("The internal return slot accepts the machine's container remainder",
                    tile.inventory.isItemValid(1, new ItemStack(Items.BUCKET)));
            Field automationField = WeederTile.class.getDeclaredField("items");
            automationField.setAccessible(true);
            IItemHandler automation = (IItemHandler) automationField.get(tile);
            assertEquals("Automation cannot insert into internal or output slots",
                    weed.getCount(), automation.insertItem(1, weed, true).getCount());
        } finally {
            apiField.set(null, previousApi);
        }
    }
}
