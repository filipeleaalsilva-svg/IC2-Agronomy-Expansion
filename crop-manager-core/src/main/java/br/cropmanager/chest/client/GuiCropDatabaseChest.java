package br.cropmanager.chest.client;

import br.cropmanager.CropManager;
import static br.cropmanager.chest.client.CropGuiLayout.*;
import br.cropmanager.chest.integration.CropInformation;
import br.cropmanager.chest.integration.SeedSearch;
import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.SeedInventory;
import br.cropmanager.chest.inventory.SeedSortMode;
import br.cropmanager.chest.network.ChestMessage;
import ic2.api.crops.CropCard;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Fixed-size crop display and independently opening side drawers. No font scaling. */
public final class GuiCropDatabaseChest extends GuiContainer {
    private static final net.minecraft.util.ResourceLocation GUI_TEXTURE =
            new net.minecraft.util.ResourceLocation(CropManager.ID, "textures/gui/crop_database_chest.png");
    private static final net.minecraft.util.ResourceLocation DRAWER_TEXTURE =
            new net.minecraft.util.ResourceLocation(CropManager.ID, "textures/gui/crop_database_drawer.png");
    private static final ItemStack[] TAB_ICONS = {
            new ItemStack(Items.NETHER_STAR), new ItemStack(Items.REDSTONE), new ItemStack(Items.COMPARATOR)};
    private final ContainerCropDatabaseChest container;
    private ItemStack lastSeed = ItemStack.EMPTY;
    private CropInformation info;
    private GuiButton previous, next, sort, pickup, favorite;
    private GuiButton favorites, deposit, collector, redstone;
    private GuiTextField search;
    private String sentQuery = "";
    private long searchContentsRevision = -1;
    private Set<String> sentMatches = Collections.emptySet();
    private SeedSortMode nextSortMode = SeedSortMode.TYPE;
    private int productRow, openTab = -1, lastMouseX, lastMouseY, selectedSeedSlot = -1;

    public GuiCropDatabaseChest(ContainerCropDatabaseChest container) {
        super(container);
        this.container = container;
        xSize = WIDTH; ySize = HEIGHT;
    }
    @Override public void initGui() {
        String text = search == null ? "" : search.getText();
        super.initGui();
        guiLeft = CropGuiLayout.guiLeft(width, container.hasManagement());
        previous = addButton(new GuiButton(0, guiLeft + 99, guiTop + 107, 14, 16, "<"));
        next = addButton(new GuiButton(1, guiLeft + 115, guiTop + 107, 14, 16, ">"));
        favorite = addButton(new GuiButton(10, guiLeft + 131, guiTop + 107, 16, 16, "") {
            @Override public void drawButton(net.minecraft.client.Minecraft mc, int mx, int my, float pt) {
                if (!visible) return;
                hovered = mx >= x && my >= y && mx < x + width && my < y + height;
                drawRect(x, y, x + width, y + height, hovered ? 0xff9f9f9f : 0xff7f7f7f);
                drawRect(x + 1, y + 1, x + width - 1, y + height - 1, container.favoritesOnly() ? 0xff455a3c : 0xffa6a6a6);
                mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("cropmanager", "textures/gui/icons.png"));
                GlStateManager.color(1, 1, 1, 1);
                drawScaledCustomSizeModalRect(x, y, (container.favoritesOnly() ? 1 : 2) * 16, 2 * 16, 16, 16, 16, 16, 64, 64);
            }
        });
        sort = addButton(new GuiButton(3, guiLeft + 149, guiTop + 107, 18, 16, "S"));
        pickup = addButton(new GuiButton(5, guiLeft + 169, guiTop + 107, 18, 16, "↑"));
        favorites = deposit = collector = redstone = null;
        if (container.hasManagement()) {
            favorites = addButton(new GuiButton(6, 0, 0, 80, 16, ""));
            deposit = addButton(new GuiButton(7, 0, 0, 80, 16, ""));
            collector = addButton(new GuiButton(8, 0, 0, 80, 16, ""));
            redstone = addButton(new GuiButton(9, 0, 0, 80, 16, ""));
        }
        search = new GuiTextField(4, fontRenderer, guiLeft + 9, guiTop + 111, 82, 11);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(64); search.setText(text);
        updateDrawerButtons();
        Keyboard.enableRepeatEvents(true);
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); super.onGuiClosed(); }
    @Override public void updateScreen() {
        super.updateScreen(); search.updateCursorCounter();
        boolean editable = mc.player.inventory.getItemStack().isEmpty();
        search.setEnabled(editable);
        if (!editable) { search.setFocused(false); openTab = -1; }
        sendSearch();
    }
    private void sendSearch() {
        if (!mc.player.inventory.getItemStack().isEmpty()) return;
        String text = search.getText().trim().toLowerCase(Locale.ROOT);
        long contents = container.getContentsRevision();
        if (sentQuery.equals(text) && searchContentsRevision == contents) return;
        searchContentsRevision = contents;
        Set<String> matches = new HashSet<>();
        if (!text.isEmpty()) for (int i = 0; i < container.getCapacity(); i++) {
            CropCard crop = SeedSearch.identifiedCrop(container.inventorySlots.get(i).getStack());
            if (crop != null && I18n.format(crop.getUnlocalizedName()).toLowerCase(Locale.ROOT).contains(text)) matches.add(SeedSearch.identity(crop));
        }
        if (!sentQuery.equals(text) || !sentMatches.equals(matches)) {
            br.cropmanager.ManagerNetwork.CHANNEL.sendToServer(new ChestMessage(container.windowId, text, matches));
            sentQuery = text; sentMatches = matches;
        }
    }
    @Override protected void keyTyped(char character, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            if (openTab >= 0) { openTab = -1; updateDrawerButtons(); return; }
            super.keyTyped(character, key); return;
        }
        if (insideDrawer(lastMouseX, lastMouseY)) {
            if (mc.gameSettings.keyBindInventory.isActiveAndMatches(key)) mc.player.closeScreen();
            return; // Hotbar keys and drop shortcuts must not act on covered slots.
        }
        if (!search.isFocused() && key == Keyboard.KEY_R && isShiftKeyDown()) {
            Slot hovered = findHoveredSeed();
            if (hovered != null && hovered.getHasStack() && SeedInventory.accepts(hovered.getStack())) {
                br.cropmanager.ManagerNetwork.CHANNEL.sendToServer(ChestMessage.favorite(container.windowId, hovered.slotNumber, container.getRevision()));
                return;
            }
        }
        if (search.isFocused()) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) search.setFocused(false);
            else if (search.textboxKeyTyped(character, key)) sendSearch();
            return;
        }
        super.keyTyped(character, key);
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (container.hasManagement()) {
            int tabCount = 3;
            for (int tab = 0; tab < tabCount; tab++) if (inside(x, y, tabX(), tabY(tab), TAB_WIDTH, 24)) {
                int drawerTab = tab;
                if (button == 0 && mc.player.inventory.getItemStack().isEmpty()) openTab = openTab == drawerTab ? -1 : drawerTab;
                search.setFocused(false); updateDrawerButtons(); return;
            }
            if (insideDrawer(x, y)) {
                if (button == 0 && mc.player.inventory.getItemStack().isEmpty()) {
                    for (GuiButton control : drawerButtons()) if (control.visible && control.mousePressed(mc, x, y)) {
                        control.playPressSound(mc.getSoundHandler()); actionPerformed(control); break;
                    }
                }
                return; // An open drawer never clicks a seed slot underneath it.
            }
        }
        if (button == 0 && favorite.mousePressed(mc, x, y)) {
            favorite.playPressSound(mc.getSoundHandler());
            mc.playerController.sendEnchantPacket(container.windowId, 111);
            return;
        }
        Slot hovered = findHoveredSeed();
        if (hovered != null && hovered.slotNumber < container.getCapacity()) {
            if (button == 2) {
                if (hovered.getHasStack() && SeedInventory.accepts(hovered.getStack())) {
                    br.cropmanager.ManagerNetwork.CHANNEL.sendToServer(ChestMessage.favorite(container.windowId, hovered.slotNumber, container.getRevision()));
                }
                return;
            }
            if (button == 1 && hovered.getHasStack() && SeedInventory.accepts(hovered.getStack())) {
                selectedSeedSlot = hovered.slotNumber;
                lastSeed = hovered.getStack().copy();
                info = CropInformation.read(lastSeed);
                productRow = 0;
                return;
            }
        }
        if (button == 0 && sort.mousePressed(mc, x, y)) {
            sort.playPressSound(mc.getSoundHandler()); actionPerformed(sort); return;
        }
        if (button == 0 && previous.mousePressed(mc, x, y)) {
            previous.playPressSound(mc.getSoundHandler()); actionPerformed(previous); return;
        }
        if (button == 0 && next.mousePressed(mc, x, y)) {
            next.playPressSound(mc.getSoundHandler()); actionPerformed(next); return;
        }
        if (button == 0 && pickup.mousePressed(mc, x, y)) {
            pickup.playPressSound(mc.getSoundHandler()); actionPerformed(pickup); return;
        }
        search.mouseClicked(x, y, button); super.mouseClicked(x, y, button);
    }
    @Override protected void handleMouseClick(Slot slot, int slotId, int button, ClickType type) {
        br.cropmanager.ManagerNetwork.CHANNEL.sendToServer(new ChestMessage(container.windowId, container.getRevision()));
        super.handleMouseClick(slot, slotId, button, type);
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int x = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        if (wheel != 0 && x >= TEXT_LEFT && x < TEXT_RIGHT && y >= PRODUCT_Y && y < PRODUCT_Y + 16 && info != null) {
            int maximum = Math.max(0, (info.production.products.size() + PRODUCTS_PER_ROW - 1) / PRODUCTS_PER_ROW - 1);
            productRow = Math.max(0, Math.min(maximum, productRow + (wheel < 0 ? 1 : -1)));
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!mc.player.inventory.getItemStack().isEmpty()) return;
        if (button.id >= 6 && button.id <= 9) {
            int[] requests = {111, 112, 113, 114};
            mc.playerController.sendEnchantPacket(container.windowId, requests[button.id - 6]); return;
        }
        if (button.id == 5) {
            if (container.isPortable()) mc.playerController.sendEnchantPacket(container.windowId, ContainerCropDatabaseChest.TOGGLE_PICKUP);
            else mc.playerController.sendEnchantPacket(container.windowId, 115);
            return;
        }
        if (button.id == 10) return;
        if (button.id == 3) {
            mc.playerController.sendEnchantPacket(container.windowId, nextSortMode.getRequestId());
            SeedSortMode[] modes = SeedSortMode.values();
            nextSortMode = modes[(nextSortMode.ordinal() + 1) % modes.length];
        } else {
            int page = container.getPage() + (button.id == 0 ? -1 : 1);
            if (page >= 0 && page < container.getPageCount()) mc.playerController.sendEnchantPacket(container.windowId, page);
        }
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        lastMouseX = mouseX; lastMouseY = mouseY;
        boolean covered = insideDrawer(mouseX, mouseY);
        Slot hovered = findHoveredSeed();
        if (hovered != null && hovered.getHasStack() && SeedInventory.accepts(hovered.getStack())) selectedSeedSlot = hovered.slotNumber;
        if (hovered != null && hovered.getHasStack() && SeedInventory.accepts(hovered.getStack())
                && !ItemStack.areItemStacksEqual(lastSeed, hovered.getStack())) {
            lastSeed = hovered.getStack().copy(); info = CropInformation.read(lastSeed); productRow = 0;
        } else if (container.getOccupied() == 0) {
            info = null; lastSeed = ItemStack.EMPTY;
        }
        boolean empty = mc.player.inventory.getItemStack().isEmpty();
        if (!empty) openTab = -1;
        sort.enabled = empty;
        favorite.enabled = empty;
        previous.enabled = empty && container.getPage() > 0;
        next.enabled = empty && container.getPage() < container.getPageCount() - 1;
        if (pickup != null) {
            pickup.enabled = empty;
            pickup.displayString = container.isPortable()
                    ? (container.isPickupEnabled() ? "↑" : "↓")
                    : (container.collectingEnabled() ? "↑" : "↓");
        }
        updateDrawerButtons();
        drawDefaultBackground(); super.drawScreen(mouseX, mouseY, partialTicks);
        search.drawTextBox();
        for (Slot slot : inventorySlots.inventorySlots) {
            if (slot.slotNumber < container.getCapacity() && slot.isEnabled() && container.isSeedFavorite(slot.slotNumber)) {
                fontRenderer.drawString("*", guiLeft + slot.xPos + 11, guiTop + slot.yPos, 0xFFFF55);
            }
        }
        if (search.getText().isEmpty() && !search.isFocused()) fontRenderer.drawString(I18n.format("cropdatabasechest.search"), guiLeft + 10, guiTop + 111, 0x888888);
        if (container.hasManagement()) drawSideTabs(mouseX, mouseY, partialTicks);
        covered = insideDrawer(mouseX, mouseY);
        if (!covered) renderHoveredToolTip(mouseX, mouseY);
        if (!covered && favorite.isMouseOver()) drawHoveringText(java.util.Arrays.asList(
                I18n.format("cropdatabasechest.tip.fav_title"),
                (container.favoritesOnly() ? "\u00a7a" : "\u00a77") + I18n.format(container.favoritesOnly() ? "cropdatabasechest.tabs.favorites.only" : "cropdatabasechest.tabs.favorites.all"),
                I18n.format("cropdatabasechest.tip.fav_hint")), mouseX, mouseY);
        if (!covered && sort.isMouseOver()) {
            String mode = I18n.format("cropdatabasechest.sort." + nextSortMode.name().toLowerCase(Locale.ROOT));
            drawHoveringText(I18n.format("cropdatabasechest.sort.next", mode, container.getCapacity()), mouseX, mouseY);
        }
        if (!covered && hovered != null && hovered.slotNumber < container.getCapacity() && container.hasManagement()) {
            drawHoveringText(I18n.format("cropdatabasechest.favorite.tip"), mouseX, mouseY);
        }
        if (pickup != null && pickup.isMouseOver()) drawHoveringText(I18n.format(container.isPortable() ? "cropdatabasechest.pickup.tooltip" : "cropdatabasechest.tabs.collect.tip"), mouseX, mouseY);
        if (container.hasManagement()) drawDrawerTooltips(mouseX, mouseY);
        int x = mouseX - guiLeft, y = mouseY - guiTop;
        if (info != null && x >= TEXT_LEFT && x < TEXT_RIGHT && y >= NAME_Y && y < PRODUCES_Y) {
            drawHoveringText(buildCropTooltip(info), mouseX, mouseY);
        }
        if (info != null && x >= TEXT_LEFT && x < TEXT_RIGHT && y >= PRODUCT_Y && y < PRODUCT_Y + 16) {
            int offset = productRow * PRODUCTS_PER_ROW;
            int count = Math.min(PRODUCTS_PER_ROW, info.production.products.size() - offset);
            int start = productStart(count);
            if (count > 0 && x >= start && x < start + count * 16) renderToolTip(info.production.products.get(offset + (x - start) / 16), mouseX, mouseY);
        }
    }
    private void drawDrawerFrame(int x, int y, boolean tab) {
        mc.getTextureManager().bindTexture(DRAWER_TEXTURE);
        if (tab) drawModalRectWithCustomSizedTexture(x, y, 104, 0, TAB_WIDTH, 24, 128, 128);
        else drawModalRectWithCustomSizedTexture(x, y, 0, 0, DRAWER_WIDTH, DRAWER_HEIGHT, 128, 128);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, WIDTH, HEIGHT, 256, 256);
        drawRect(guiLeft + 7, guiTop + 108, guiLeft + 93, guiTop + 122, 0xff2b2b2b);
        if (container.getPageCount() == 1 && !sentQuery.isEmpty()) {
            boolean any = false;
            for (int i = 0; i < container.getCapacity(); i++) if (container.getPosition(i) >= 0) any = true;
            if (!any) fontRenderer.drawSplitString(I18n.format("cropdatabasechest.no_results"), guiLeft + 102, guiTop + 44, 84, 0x404040);
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawCropDisplay();
        if (info != null && !info.production.products.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            int offset = productRow * PRODUCTS_PER_ROW;
            int count = Math.min(PRODUCTS_PER_ROW, info.production.products.size() - offset);
            for (int col = 0; col < count; col++) itemRender.renderItemAndEffectIntoGUI(info.production.products.get(offset + col), productStart(count) + col * 16, PRODUCT_Y);
            RenderHelper.disableStandardItemLighting();
        }
    }
    private void drawCropDisplay() {
        fontRenderer.drawString("\u00a7l" + "CROPS", TEXT_LEFT, HEADER_Y, 0x55FFFF);
        String occupied = String.valueOf(container.getOccupied());
        fontRenderer.drawString(occupied, TEXT_RIGHT - fontRenderer.getStringWidth(occupied), HEADER_Y, 0x55FFFF);
        if (info == null) return;
        List<String> nameLines = fontRenderer.listFormattedStringToWidth("\u00a7l" + I18n.format(info.nameKey).toUpperCase(Locale.ROOT), TEXT_WIDTH);
        for (int i = 0; i < Math.min(2, nameLines.size()); i++) {
            String line = nameLines.get(i);
            if (i == 1 && nameLines.size() > 2) line = fontRenderer.trimStringToWidth(line, TEXT_WIDTH - fontRenderer.getStringWidth("...")) + "...";
            centered(line, nameLines.size() == 1 ? NAME_Y + 4 : NAME_Y + i * NAME_LINE_HEIGHT, 0xFFFF55);
        }
        fontRenderer.drawString("\u00a7lT:" + stat(info.tier), TEXT_LEFT, STAT_Y[0], 0xCC88FF);
        String scanText = "S:" + info.scanLevel;
        fontRenderer.drawString(scanText, TEXT_RIGHT - fontRenderer.getStringWidth(scanText), STAT_Y[0], 0xAAAAAA);
        fontRenderer.drawString("\u00a7lGR:" + stat(info.growth), TEXT_LEFT, STAT_Y[1], 0xAAFF55);
        String maxText = "MAX:" + stat(info.maxSize);
        fontRenderer.drawString(maxText, TEXT_RIGHT - fontRenderer.getStringWidth(maxText), STAT_Y[1], 0xAAAAAA);
        fontRenderer.drawString("\u00a7lGA:" + stat(info.gain), TEXT_LEFT, STAT_Y[2], 0xFFCC55);
        String reText = "\u00a7lRE:" + stat(info.resistance);
        fontRenderer.drawString(reText, TEXT_RIGHT - fontRenderer.getStringWidth(reText), STAT_Y[2], 0x55FFFF);
        centered("\u00a7l" + I18n.format("cropdatabasechest.produces").toUpperCase(Locale.ROOT), PRODUCES_Y, 0xFF88CC);
    }
    private List<String> buildCropTooltip(CropInformation cropInfo) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("\u00a7e\u00a7l" + I18n.format(cropInfo.nameKey));
        if (!cropInfo.discoveredBy.isEmpty()) {
            lines.add(I18n.format("cropdatabasechest.info.discovered_by", cropInfo.discoveredBy));
        }
        lines.add(I18n.format("cropdatabasechest.info.scan", cropInfo.scanLevel));
        lines.add(I18n.format("cropdatabasechest.info.tier_size", stat(cropInfo.tier), stat(cropInfo.maxSize)));
        lines.add(I18n.format("cropdatabasechest.info.stats", stat(cropInfo.growth), stat(cropInfo.gain), stat(cropInfo.resistance)));
        if (!cropInfo.attributes.isEmpty()) {
            lines.add(I18n.format("cropdatabasechest.info.attributes", String.join(", ", cropInfo.attributes)));
        }
        return lines;
    }
    private void centered(String text, int y, int color) {
        fontRenderer.drawString(text, TEXT_LEFT + (TEXT_WIDTH - fontRenderer.getStringWidth(text)) / 2, y, color);
    }
    private int tabX() { return CropGuiLayout.tab(guiLeft, guiTop, 0).x; }
    private int tabY(int tab) { return CropGuiLayout.tab(guiLeft, guiTop, container.hasManagement() ? tab : 2).y; }
    private int drawerX() {
        // Outside the inventory when possible; opaque inset drawer at narrow resolutions.
        return CropGuiLayout.drawer(width, guiLeft, guiTop).x;
    }
    private int drawerY() { return CropGuiLayout.drawer(width, guiLeft, guiTop).y; }
    private boolean insideDrawer(int x, int y) { return openTab >= 0 && CropGuiLayout.drawer(width, guiLeft, guiTop).contains(x, y); }
    private static boolean inside(int x, int y, int left, int top, int width, int height) { return x >= left && x < left + width && y >= top && y < top + height; }
    private GuiButton[] drawerButtons() { return new GuiButton[]{favorites, deposit, collector, redstone}; }
    private void updateDrawerButtons() {
        if (favorites == null) return;
        int left = drawerX() + 8, top = drawerY() + 29;
        for (GuiButton button : drawerButtons()) { button.visible = false; button.enabled = mc.player.inventory.getItemStack().isEmpty(); button.x = left; button.y = top; button.width = 84; }
        favorites.visible = openTab == 0;
        favorites.displayString = I18n.format(container.favoritesOnly() ? "cropdatabasechest.tabs.favorites.only" : "cropdatabasechest.tabs.favorites.all");
        collector.visible = deposit.visible = container.hasManagement() && openTab == 2;
        collector.displayString = I18n.format(container.collectingEnabled() ? "cropdatabasechest.tabs.collect.on" : "cropdatabasechest.tabs.collect.off");
        deposit.y = top + 25; deposit.displayString = I18n.format("cropdatabasechest.tabs.deposit");
        redstone.visible = container.hasManagement() && openTab == 1;
        redstone.displayString = I18n.format("cropdatabasechest.tabs.redstone." + container.redstoneMode());
    }
    private void drawSideTabs(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableDepth();
        if (openTab >= 0) {
            drawDrawerFrame(drawerX(), drawerY(), false);
            String title = I18n.format("cropdatabasechest.tabs." + (container.hasManagement() ? openTab : 0));
            fontRenderer.drawString(fontRenderer.trimStringToWidth(title, 84), drawerX() + 8, drawerY() + 10, 0x303030);
            String help = I18n.format("cropdatabasechest.tabs.help." + (container.hasManagement() ? openTab : 0));
            List<String> lines = fontRenderer.listFormattedStringToWidth(help, 84);
            for (int i = 0; i < Math.min(3, lines.size()); i++) fontRenderer.drawString(lines.get(i), drawerX() + 8, drawerY() + 81 + i * 9, 0x505050);
            for (GuiButton button : drawerButtons()) if (button.visible) button.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        for (int tab = 0; tab < (container.hasManagement() ? 3 : 0); tab++) {
            drawDrawerFrame(tabX(), tabY(tab), true);
            int drawerTab = tab;
            if (openTab == drawerTab) drawRect(tabX() + 2, tabY(tab) + 2, tabX() + 22, tabY(tab) + 22, tab == 0 ? 0xFF8F803A : tab == 1 ? 0xFF914C4C : 0xFF587B8B);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(TAB_ICONS[tab], tabX() + 4, tabY(tab) + 4);
            RenderHelper.disableStandardItemLighting();
        }
        GlStateManager.enableDepth();
    }
    private void drawDrawerTooltips(int x, int y) {
        for (int tab = 0; tab < (container.hasManagement() ? 3 : 0); tab++) if (inside(x, y, tabX(), tabY(tab), TAB_WIDTH, 24)) drawHoveringText(I18n.format("cropdatabasechest.tabs." + tab), x, y);
        if (favorites.visible && favorites.isMouseOver()) drawHoveringText(I18n.format("cropdatabasechest.tabs.favorites.tip"), x, y);
        if (collector.visible && collector.isMouseOver()) drawHoveringText(I18n.format("cropdatabasechest.tabs.collect.tip"), x, y);
        if (deposit.visible && deposit.isMouseOver()) drawHoveringText(I18n.format("cropdatabasechest.tabs.deposit.tip"), x, y);
        if (redstone.visible && redstone.isMouseOver()) drawHoveringText(I18n.format("cropdatabasechest.tabs.redstone.tip"), x, y);
    }
    private Slot findHoveredSeed() {
        int x = lastMouseX - guiLeft, y = lastMouseY - guiTop;
        for (Slot slot : inventorySlots.inventorySlots) {
            if (slot.slotNumber < container.getCapacity() && slot.isEnabled()
                    && x >= slot.xPos && x < slot.xPos + 16 && y >= slot.yPos && y < slot.yPos + 16) return slot;
        }
        return null;
    }
    private static String stat(int value) { return value < 0 ? "?" : Integer.toString(value); }
}


