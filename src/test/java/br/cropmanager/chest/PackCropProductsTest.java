package br.cropmanager.chest;

import br.cropmanager.chest.integration.PackCropProducts;
import ic2.api.crops.CropCard;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PackCropProductsTest {
    @BeforeClass public static void setup() { SeedFixtures.bootstrap(); }
    private CropCard crop(String owner, String id) {
        CropCard crop = mock(CropCard.class);
        when(crop.getOwner()).thenReturn(owner);
        when(crop.getId()).thenReturn(id);
        return crop;
    }
    @Test public void reedResolvesItsDeclaredProduct() {
        assertSame(Items.REEDS, PackCropProducts.query(crop("ic2", "reed")).get(0).getItem());
    }
    @Test public void potatoListsBothAlternativesAndReturnsIndependentStacks() {
        CropCard crop = crop("ic2", "potato");
        List<ItemStack> products = PackCropProducts.query(crop);
        assertEquals(2, products.size());
        assertSame(Items.POTATO, products.get(0).getItem());
        assertSame(Items.POISONOUS_POTATO, products.get(1).getItem());
        products.get(0).setCount(0);
        assertEquals(1, PackCropProducts.query(crop).get(0).getCount());
        verify(crop, never()).getGain(any());
    }
    @Test public void ownerIsPartOfTheKeyAndUnknownCropsUseTheFallback() {
        assertNull(PackCropProducts.query(crop("another_mod", "potato")));
        assertNull(PackCropProducts.query(crop("ic2", "not_in_this_pack")));
    }
}
