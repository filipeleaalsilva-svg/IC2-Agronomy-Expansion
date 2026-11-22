package br.cropmanager.chest.crafting;

import br.cropmanager.CropManager;
import ic2.api.item.IC2Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;

public final class PortableCropRecipe {
    private PortableCropRecipe() { }
    public static ShapedRecipes create() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        ingredients.set(0, Ingredient.fromStacks(IC2Items.getItem("crafting", "alloy")));
        ingredients.set(1, Ingredient.fromStacks(IC2Items.getItem("crafting", "advanced_circuit")));
        ingredients.set(2, Ingredient.fromStacks(IC2Items.getItem("crafting", "alloy")));
        ingredients.set(3, Ingredient.fromStacks(IC2Items.getItem("crafting", "circuit")));
        ingredients.set(4, new EmptyChestIngredient(new ItemStack(CropManager.DATABASE_CHEST)));
        ingredients.set(5, Ingredient.fromStacks(IC2Items.getItem("crafting", "circuit")));
        ingredients.set(6, Ingredient.fromStacks(IC2Items.getItem("plate", "iron")));
        ingredients.set(7, new CropChestRecipe.ChargedCrystalIngredient(IC2Items.getItem("energy_crystal")));
        ingredients.set(8, Ingredient.fromStacks(IC2Items.getItem("plate", "iron")));
        return new ShapedRecipes("", 3, 3, ingredients, new ItemStack(CropManager.PORTABLE_DATABASE));
    }
    static final class EmptyChestIngredient extends Ingredient {
        private final ItemStack chest;
        EmptyChestIngredient(ItemStack chest) { super(chest); this.chest = chest.copy(); }
        @Override public boolean apply(ItemStack input) {
            return input != null && !input.isEmpty() && ItemStack.areItemsEqual(chest, input)
                    && (!input.hasTagCompound() || input.getTagCompound().isEmpty());
        }
        @Override public boolean isSimple() { return false; }
    }
}
