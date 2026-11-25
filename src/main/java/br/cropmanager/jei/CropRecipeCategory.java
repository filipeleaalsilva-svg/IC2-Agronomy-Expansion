package br.cropmanager.jei;

import br.cropmanager.CropManager;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/** HEI/JEI category displaying IC2: Agronomy Expansion custom crops, their soil/catalyst needs, and drops. */
public final class CropRecipeCategory implements IRecipeCategory<CropRecipeWrapper> {
    public static final String UID = CropManager.ID + ".crops";
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;

    public CropRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(156, 62);
        this.icon = helper.createDrawableIngredient(new ItemStack(CropManager.CATALYTIC_FERTILIZER));
        this.slot = helper.getSlotDrawable();
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("cropmanager.jei_crops_title"); }
    @Override public String getModName() { return CropManager.NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void drawExtras(Minecraft mc) {
        slot.draw(mc, 4, 17);
        slot.draw(mc, 26, 17);
        slot.draw(mc, 48, 17);
        slot.draw(mc, 128, 17);
        // Clean minimal arrow between inputs and drop output
        Gui.drawRect(78, 25, 114, 27, 0xff555555);
        Gui.drawRect(110, 23, 112, 29, 0xff555555);
        Gui.drawRect(112, 24, 114, 28, 0xff555555);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, CropRecipeWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 4, 17);
        items.init(1, true, 26, 17);
        items.init(2, true, 48, 17);
        items.init(3, false, 128, 17);
        items.set(ingredients);
    }
}
