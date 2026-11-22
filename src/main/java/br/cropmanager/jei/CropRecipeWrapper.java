package br.cropmanager.jei;

import br.cropmanager.CropManager;
import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.RadbloomCropCard;
import ic2.core.item.ItemCropSeed;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/** HEI/JEI recipe entry for a Crop Manager custom IC2 crop. */
public final class CropRecipeWrapper implements IRecipeWrapper {
    public final BaseCropCard card;
    private final List<ItemStack> inputs = new ArrayList<>();
    private final ItemStack output;

    public CropRecipeWrapper(BaseCropCard card) {
        this.card = card;
        this.inputs.add(ItemCropSeed.generateItemStackFromValues(card, 1, 1, 1, 4));
        if (card.requiresCatalyst()) {
            this.inputs.add(new ItemStack(CropManager.CATALYTIC_FERTILIZER));
        }
        if (card instanceof RadbloomCropCard) {
            this.inputs.add(new ItemStack(CropManager.RADIOACTIVE_SOIL));
        }
        this.output = card.previewGain();
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    @Override
    public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String title = I18n.format(card.getUnlocalizedName());
        int tier = card.getProperties() == null ? 1 : card.getProperties().getTier();
        mc.fontRenderer.drawString(title + " (T" + tier + ")", 4, 4, 0x202020);

        int y = 40;
        if (card.requiresCatalyst()) {
            mc.fontRenderer.drawString(I18n.format("cropmanager.jei_requires_catalyst"), 4, y, 0x1F6B7A);
            y += 10;
        }
        if (card instanceof RadbloomCropCard) {
            mc.fontRenderer.drawString(I18n.format("cropmanager.jei_requires_rad_soil"), 4, y, 0x2D7A24);
        } else if (!card.requiresCatalyst()) {
            mc.fontRenderer.drawString(I18n.format("cropmanager.jei_normal_growth"), 4, y, 0x555555);
        }
    }
}
