package br.cropmanager.jei;

import br.cropmanager.CropManager;
import br.cropmanager.ManagerConfig;
import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.CropRegistry;
import ic2.api.crops.CropCard;
import ic2.api.crops.ICropSeed;
import ic2.api.item.IC2Items;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

/** Optional HEI/JEI plugin: separates IC2 Seed Bags by species and displays Crop Manager crop drops/requirements. */
@JEIPlugin
public final class CropManagerJeiPlugin implements IModPlugin {
    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        ItemStack seedBag = IC2Items.getItem("crop_seed_bag");
        if (seedBag == null || seedBag.isEmpty()) return;
        if (!registry.hasSubtypeInterpreter(seedBag)) {
            registry.registerSubtypeInterpreter(seedBag.getItem(), stack -> {
                if (!(stack.getItem() instanceof ICropSeed)) return ISubtypeRegistry.ISubtypeInterpreter.NONE;
                CropCard card = ((ICropSeed) stack.getItem()).getCropFromStack(stack);
                if (card == null) return ISubtypeRegistry.ISubtypeInterpreter.NONE;
                return card.getOwner() + ":" + card.getId();
            });
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new CropRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registry) {
        if (!ManagerConfig.enableCustomCrops) return;
        List<CropRecipeWrapper> recipes = new ArrayList<>();
        for (BaseCropCard card : CropRegistry.ALL) {
            recipes.add(new CropRecipeWrapper(card));
        }
        registry.addRecipes(recipes, CropRecipeCategory.UID);
        registry.addRecipeCatalyst(new ItemStack(CropManager.SEED_ANALYZER), CropRecipeCategory.UID);
        registry.addRecipeCatalyst(new ItemStack(CropManager.ADVANCED), CropRecipeCategory.UID);
        ItemStack sticks = IC2Items.getItem("crop_stick");
        if (sticks != null && !sticks.isEmpty()) {
            registry.addRecipeCatalyst(sticks, CropRecipeCategory.UID);
        }
    }
}
