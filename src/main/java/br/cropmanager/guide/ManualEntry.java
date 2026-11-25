package br.cropmanager.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import br.cropmanager.CropManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ManualEntry {
    private final String id;
    private final String categoryId;
    private final ItemStack displayStack;
    private final String titleKey;
    private final String descKey;
    private final String infoKey;
    private final List<ResourceLocation> recipeIds = new ArrayList<>();
    private boolean includeIc2Recipes;

    public ManualEntry(String id, String categoryId, ItemStack displayStack, String titleKey, String descKey, String infoKey) {
        this.id = id;
        this.categoryId = categoryId;
        this.displayStack = displayStack;
        this.titleKey = titleKey;
        this.descKey = descKey;
        this.infoKey = infoKey;
    }

    ManualEntry withRecipes(String... ids) {
        for (String id : ids) recipeIds.add(new ResourceLocation(CropManager.ID, id));
        return this;
    }

    ManualEntry withIc2Recipes() {
        includeIc2Recipes = true;
        return this;
    }

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public ItemStack getDisplayStack() { return displayStack; }
    public String getTitleKey() { return titleKey; }
    public String getDescKey() { return descKey; }
    public String getInfoKey() { return infoKey; }
    public List<IRecipe> getRecipes() {
        List<IRecipe> recipes = new ArrayList<>();
        for (ResourceLocation id : recipeIds) {
            if (ForgeRegistries.RECIPES.containsKey(id)) {
                recipes.add(ForgeRegistries.RECIPES.getValue(id));
            }
        }
        if (includeIc2Recipes) {
            for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
                ResourceLocation id = recipe.getRegistryName();
                if (id != null && "ic2".equals(id.getNamespace())
                        && !recipe.isDynamic() && ItemStack.areItemsEqual(displayStack, recipe.getRecipeOutput())) {
                    recipes.add(recipe);
                }
            }
        }
        return Collections.unmodifiableList(recipes);
    }
}
