package br.cropmanager.guide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.cropmanager.CropManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public final class GuiAgriculturalManual extends GuiScreen {
    private static final ResourceLocation BOOK = new ResourceLocation(CropManager.ID, "textures/gui/guide_book.png");
    private static final ResourceLocation PORTABLE_LINK = new ResourceLocation(CropManager.ID, "portable_database_link");
    private static final int BOOK_WIDTH = 290;
    private static final int BOOK_HEIGHT = 180;
    private static final int INK = 0xFFE2E4E6;
    private static final int ACCENT = 0xFF73D17F;
    private static final int MUTED = 0xFF8A8E94;
    private static final int HOVER_BG = 0x3373D17F;
    private static final int ROWS_PER_PAGE = 6;
    private static final int TEXT_LINES = 8;
    private static final int GRID_X = 169;
    private static final int GRID_Y = 73;
    private static final int OUTPUT_X = GRID_X + 89;

    private final List<Page> history = new ArrayList<>();
    private int historyIndex;
    private int recipeIndex;
    private int scroll;
    private int maxScroll;
    private int left;
    private int top;
    private float zoom;
    private GuiTextField search;
    private final ItemStack[] ingredients = new ItemStack[9];
    private ItemStack output = ItemStack.EMPTY;
    private List<IRecipe> recipes = Collections.emptyList();
    private List<ManualEntry> results = Collections.emptyList();

    public GuiAgriculturalManual() {
        history.add(Page.home());
    }

    @Override public void initGui() {
        zoom = Math.min(1, Math.min((width - 8f) / BOOK_WIDTH, (height - 8f) / BOOK_HEIGHT));
        left = Math.round((width - BOOK_WIDTH * zoom) / 2);
        top = Math.round((height - BOOK_HEIGHT * zoom) / 2);
        search = new GuiTextField(0, fontRenderer, 165, 14, 94, 11);
        search.setEnableBackgroundDrawing(false);
        search.setTextColor(0xFFE6E6E6);
        search.setMaxStringLength(64);
        search.setText(page().query == null ? "" : page().query);
        Keyboard.enableRepeatEvents(true);
    }

    @Override public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override public boolean doesGuiPauseGame() {
        return false;
    }

    @Override public void updateScreen() {
        search.updateCursorCounter();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = localX(mouseX);
        int y = localY(mouseY);

        GlStateManager.pushMatrix();
        GlStateManager.translate(left, top, 0);
        GlStateManager.scale(zoom, zoom, 1);
        texture(0, 0, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);
        drawNavigation(x, y);
        if (page().entry == null) {
            drawIndex(x, y);
        } else {
            drawEntry(page().entry, x, y);
        }
        GlStateManager.popMatrix();

        ItemStack stack = stackAt(x, y);
        if (!stack.isEmpty()) {
            renderToolTip(stack, mouseX, mouseY);
        } else if (inside(x, y, 71, 11, 26, 14)) {
            drawHoveringText(tr("back"), mouseX, mouseY);
        } else if (inside(x, y, 101, 11, 26, 14)) {
            drawHoveringText(tr("forward"), mouseX, mouseY);
        } else if (inside(x, y, 8, 30, 132, 20)) {
            int tab = (x - 8) / 22;
            List<ManualCategory> categories = ManualRegistry.getCategories();
            if (tab < categories.size()) {
                drawHoveringText(I18n.format(categories.get(tab).getTitleKey()), mouseX, mouseY);
            }
        }
    }

    private void drawNavigation(int x, int y) {
        button(10, 11, 56, tr("home"), true, x, y);
        button(71, 11, 26, "<", historyIndex > 0, x, y);
        button(101, 11, 26, ">", historyIndex + 1 < history.size(), x, y);
        search.drawTextBox();
        if (!search.isFocused() && search.getText().isEmpty()) {
            label(tr("search"), 166, 14, 92, MUTED);
        } else if (!search.getText().isEmpty()) {
            boolean hoverClear = inside(x, y, 260, 12, 10, 12);
            label("x", 263, 14, 10, hoverClear ? ACCENT : MUTED);
        }
        List<ManualCategory> categories = ManualRegistry.getCategories();
        Page current = page();
        String activeCategory = current.entry != null ? current.entry.getCategoryId()
                : current.category == null ? null : current.category.getId();
        for (int i = 0; i < categories.size(); i++) {
            ManualCategory category = categories.get(i);
            int tabX = 8 + 22 * i;
            texture(tabX, 29, category.getId().equals(activeCategory) ? 0 : 34, 199, 22, 23);
            drawItem(category.getIcon(), tabX + 3, 32);
        }
    }

    private void drawIndex(int x, int y) {
        Page current = page();
        boolean home = current.category == null && current.query == null;
        if (home) {
            label(tr("contents"), 10, 56, 122, ACCENT);
            label(tr("search"), 157, 56, 122, ACCENT);
            List<ManualCategory> categories = ManualRegistry.getCategories();
            for (int i = 0; i < categories.size(); i++) {
                int rowY = 70 + i * 15;
                if (inside(x, y, 9, rowY, 124, 14)) drawRect(9, rowY, 133, rowY + 14, HOVER_BG);
                drawMiniItem(categories.get(i).getIcon(), 11, rowY + 1);
                label(I18n.format(categories.get(i).getTitleKey()), 26, rowY + 3, 105, INK);
            }
            drawWrapped(tr("index_help"), 157, 71, 119, 0, TEXT_LINES);
            maxScroll = 0;
            return;
        }
        label(current.category == null ? tr("search") : I18n.format(current.category.getTitleKey()),
                10, 56, 124, ACCENT);
        List<ManualEntry> entries = entries(current);
        if (entries.isEmpty()) {
            label(tr("no_results"), 10, 74, 122, INK);
        }
        int pageSize = 2 * ROWS_PER_PAGE;
        maxScroll = Math.max(0, (entries.size() - 1) / pageSize);
        for (int i = 0; i < pageSize && scroll * pageSize + i < entries.size(); i++) {
            ManualEntry entry = entries.get(scroll * pageSize + i);
            int colX = i < ROWS_PER_PAGE ? 9 : 156;
            int rowY = 69 + (i % ROWS_PER_PAGE) * 14;
            if (inside(x, y, colX, rowY, 124, 14)) drawRect(colX, rowY, colX + 124, rowY + 14, HOVER_BG);
            drawMiniItem(entry.getDisplayStack(), colX + 2, rowY + 1);
            label(I18n.format(entry.getTitleKey()), colX + 17, rowY + 3, 105, INK);
        }
        if (maxScroll > 0) {
            button(156, 158, 22, "<", scroll > 0, x, y);
            label(tr("recipe_index", scroll + 1, maxScroll + 1), 182, 161, 68, ACCENT);
            button(253, 158, 22, ">", scroll < maxScroll, x, y);
        }
    }

    private void drawEntry(ManualEntry entry, int x, int y) {
        drawItem(entry.getDisplayStack(), 10, 55);
        drawWrapped(I18n.format(entry.getTitleKey()), 30, 54, 102, 0, 2);
        String description = I18n.format(entry.getDescKey()) + "\n\n" + I18n.format(entry.getInfoKey());
        List<String> lines = fontRenderer.listFormattedStringToWidth(description, 121);
        maxScroll = Math.max(0, lines.size() - TEXT_LINES);
        drawLines(lines, 10, 77, scroll, TEXT_LINES);
        if (maxScroll > 0) {
            button(10, 157, 22, "<", scroll > 0, x, y);
            label((scroll + 1) + " / " + (maxScroll + 1), 36, 160, 62, ACCENT);
            button(107, 157, 22, ">", scroll < maxScroll, x, y);
        }

        Arrays.fill(ingredients, ItemStack.EMPTY);
        output = ItemStack.EMPTY;
        if (recipes.isEmpty()) {
            drawWrapped(tr("no_recipe"), 157, 74, 120, 0, 3);
            return;
        }
        IRecipe recipe = recipes.get(recipeIndex);
        label(tr("recipe_index", recipeIndex + 1, recipes.size()), 157, 56, 120, ACCENT);
        if (PORTABLE_LINK.equals(recipe.getRegistryName())) {
            drawWrapped(tr("portable_link"), 157, 72, 119, 0, TEXT_LINES);
        } else if (!recipe.isDynamic()) {
            drawRecipe(recipe);
        } else {
            drawWrapped(tr("no_recipe"), 157, 74, 120, 0, 3);
        }
        if (recipes.size() > 1) {
            button(156, 157, 22, "<", recipeIndex > 0, x, y);
            button(253, 157, 22, ">", recipeIndex + 1 < recipes.size(), x, y);
        }
    }

    private void drawRecipe(IRecipe recipe) {
        texture(GRID_X, GRID_Y, 300, 0, 114, 54);
        int variant = (int) (Minecraft.getSystemTime() / 1200 % Integer.MAX_VALUE);
        int recipeWidth = recipe instanceof IShapedRecipe ? ((IShapedRecipe) recipe).getRecipeWidth() : 3;
        if (recipeWidth >= 1 && recipeWidth <= 3) {
            List<Ingredient> input = recipe.getIngredients();
            for (int i = 0; i < input.size() && i / recipeWidth < 3; i++) {
                ItemStack[] matches = input.get(i).getMatchingStacks();
                if (matches.length == 0) continue;
                int col = i % recipeWidth;
                int row = i / recipeWidth;
                ItemStack stack = matches[variant % matches.length];
                ingredients[row * 3 + col] = stack;
                drawItem(stack, GRID_X + 1 + col * 18, GRID_Y + 1 + row * 18);
            }
        }
        output = recipe.getRecipeOutput();
        drawItem(output, OUTPUT_X + 4, GRID_Y + 17);
        if (!(recipe instanceof IShapedRecipe)) label(tr("shapeless"), 157, 134, 120, ACCENT);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int x = localX(mouseX);
        int y = localY(mouseY);
        if (mouseButton == 0 && !search.getText().isEmpty() && inside(x, y, 260, 12, 10, 12)) {
            search.setText("");
            navigate(Page.home());
            return;
        }
        search.mouseClicked(x, y, mouseButton);
        if (mouseButton != 0 || search.isFocused()) return;

        if (inside(x, y, 10, 11, 56, 14)) navigate(Page.home());
        else if (inside(x, y, 71, 11, 26, 14)) go(-1);
        else if (inside(x, y, 101, 11, 26, 14)) go(1);
        else if (inside(x, y, 8, 29, 132, 23)) {
            List<ManualCategory> categories = ManualRegistry.getCategories();
            int tab = (x - 8) / 22;
            if (tab < categories.size()) navigate(Page.category(categories.get(tab)));
        } else if (page().entry != null && maxScroll > 0 && inside(x, y, 10, 157, 22, 14)) {
            scrollBy(-1);
        } else if (page().entry != null && maxScroll > 0 && inside(x, y, 107, 157, 22, 14)) {
            scrollBy(1);
        } else if (inside(x, y, 156, 157, 22, 14)) {
            if (page().entry == null) scrollBy(-1);
            else selectRecipe(-1);
        } else if (inside(x, y, 253, 157, 22, 14)) {
            if (page().entry == null) scrollBy(1);
            else selectRecipe(1);
        } else if (page().entry != null) {
            ItemStack ingredient = stackAt(x, y);
            ManualEntry target = ingredient.isEmpty() ? null : ManualRegistry.findEntry(ingredient);
            if (target != null && target != page().entry) navigate(Page.entry(target));
        } else {
            clickIndex(x, y);
        }
    }

    private void clickIndex(int x, int y) {
        Page current = page();
        if (current.query == null && current.category == null) {
            if (!inside(x, y, 9, 70, 124, 90)) return;
            List<ManualCategory> categories = ManualRegistry.getCategories();
            int row = (y - 70) / 15;
            if (row < categories.size()) navigate(Page.category(categories.get(row)));
            return;
        }
        int column = inside(x, y, 9, 69, 124, 84) ? 0 : inside(x, y, 156, 69, 124, 84) ? 1 : -1;
        if (column < 0) return;
        List<ManualEntry> entries = entries(current);
        int index = (scroll * 2 + column) * ROWS_PER_PAGE + (y - 69) / 14;
        if (index < entries.size()) navigate(Page.entry(entries.get(index)));
    }

    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode != Keyboard.KEY_ESCAPE && search.textboxKeyTyped(typedChar, keyCode)) {
            String query = search.getText();
            if (page().query == null && !query.isEmpty()) {
                navigate(Page.search(query));
                search.setFocused(true);
            } else if (page().query != null && !page().query.equals(query)) {
                history.set(historyIndex, Page.search(query));
                results = ManualRegistry.search(query, I18n::format);
                history.subList(historyIndex + 1, history.size()).clear();
                scroll = 0;
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scrollBy(wheel < 0 ? 1 : -1);
    }

    private void scrollBy(int offset) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + offset));
    }

    private List<ManualEntry> entries(Page page) {
        return page.query != null ? results : page.category.getEntries();
    }

    private void navigate(Page page) {
        rememberPosition();
        history.subList(historyIndex + 1, history.size()).clear();
        history.add(page);
        historyIndex++;
        refreshPage();
    }

    private void go(int step) {
        int target = historyIndex + step;
        if (target < 0 || target >= history.size()) return;
        rememberPosition();
        historyIndex = target;
        refreshPage();
    }

    private void refreshPage() {
        Page page = page();
        scroll = page.scroll;
        maxScroll = scroll;
        recipes = page.entry == null ? Collections.emptyList() : page.entry.getRecipes();
        recipeIndex = Math.min(page.recipeIndex, Math.max(0, recipes.size() - 1));
        results = page.query == null ? Collections.emptyList() : ManualRegistry.search(page.query, I18n::format);
        search.setText(page.query == null ? "" : page.query);
        search.setFocused(false);
        Arrays.fill(ingredients, ItemStack.EMPTY);
        output = ItemStack.EMPTY;
    }

    private void rememberPosition() {
        page().scroll = scroll;
        page().recipeIndex = recipeIndex;
    }

    private void selectRecipe(int step) {
        int target = recipeIndex + step;
        if (target < 0 || target >= recipes.size()) return;
        recipeIndex = target;
        Arrays.fill(ingredients, ItemStack.EMPTY);
        output = ItemStack.EMPTY;
    }

    private Page page() { return history.get(historyIndex); }
    private int localX(int x) { return (int) Math.floor((x - left) / zoom); }
    private int localY(int y) { return (int) Math.floor((y - top) / zoom); }

    private ItemStack stackAt(int x, int y) {
        if (page().entry == null) return ItemStack.EMPTY;
        if (inside(x, y, GRID_X, GRID_Y, 54, 54)) {
            return ingredients[(y - GRID_Y) / 18 * 3 + (x - GRID_X) / 18];
        }
        return inside(x, y, OUTPUT_X, GRID_Y + 14, 25, 26) ? output : ItemStack.EMPTY;
    }

    private void texture(int x, int y, int u, int v, int w, int h) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(BOOK);
        drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, 512, 512);
    }

    private void drawItem(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x, y, null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
    }

    private void drawMiniItem(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.75F, 0.75F, 1.0F);
        drawItem(stack, 0, 0);
        GlStateManager.popMatrix();
    }

    private void button(int x, int y, int w, String text, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, w, 14);
        drawRect(x, y, x + w, y + 14, 0xFF0D0D0E);
        drawRect(x + 1, y + 1, x + w - 1, y + 13, hover ? 0xFF343A40 : 0xFF25282B);
        label(text, x + (w - fontRenderer.getStringWidth(text)) / 2, y + 3, w, enabled ? ACCENT : MUTED);
    }

    private void label(String text, int x, int y, int w, int color) {
        fontRenderer.drawString(fontRenderer.trimStringToWidth(text, w), x, y, color);
    }

    private void drawWrapped(String text, int x, int y, int width, int start, int count) {
        drawLines(fontRenderer.listFormattedStringToWidth(text, width), x, y, start, count);
    }

    private void drawLines(List<String> lines, int x, int y, int start, int count) {
        for (int i = start; i < Math.min(start + count, lines.size()); i++) {
            fontRenderer.drawString(lines.get(i), x, y + (i - start) * 10, INK);
        }
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static String tr(String key, Object... arguments) {
        return I18n.format("cropmanager.guide.ui." + key, arguments);
    }

    private static final class Page {
        final ManualCategory category;
        final ManualEntry entry;
        final String query;
        int scroll;
        int recipeIndex;

        private Page(ManualCategory category, ManualEntry entry, String query) {
            this.category = category;
            this.entry = entry;
            this.query = query;
        }

        static Page home() { return new Page(null, null, null); }
        static Page category(ManualCategory category) { return new Page(category, null, null); }
        static Page entry(ManualEntry entry) { return new Page(null, entry, null); }
        static Page search(String query) { return new Page(null, null, query); }
    }
}
