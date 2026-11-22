package br.cropmanager.chest.crafting;

import br.cropmanager.chest.SeedFixtures;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class PortableCropRecipeTest {
    @BeforeClass public static void setup() { SeedFixtures.bootstrap(); }
    @Test public void baseChestMustNotCarryAnyData() {
        Ingredient empty = new PortableCropRecipe.EmptyChestIngredient(new ItemStack(Items.CHEST_MINECART));
        assertFalse(empty.isSimple());
        assertTrue(empty.apply(new ItemStack(Items.CHEST_MINECART)));
        assertFalse(empty.apply(new ItemStack(Items.MINECART)));
        ItemStack data = new ItemStack(Items.CHEST_MINECART);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("BlockEntityTag", SeedFixtures.bag(1, 4).serializeNBT());
        data.setTagCompound(tag);
        assertFalse(empty.apply(data));
        tag.setTag("BlockEntityTag", new NBTTagCompound());
        assertFalse(empty.apply(data));
    }
}
