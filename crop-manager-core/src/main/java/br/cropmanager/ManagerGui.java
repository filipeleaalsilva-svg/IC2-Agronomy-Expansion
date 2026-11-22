package br.cropmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.MathHelper;

public final class ManagerGui extends GuiContainer {
    private final ManagerTile tile;
    private static final String[] PROBLEMS = {"no_energy", "no_fertilizer", "no_water", "no_weed", "no_space"};
    private int tab;
    private boolean sidebarExpanded = true;

    public ManagerGui(InventoryPlayer player, ManagerTile tile) {
        super(new ManagerContainer(player, tile));
        this.tile = tile;
        xSize = tile.advanced ? 310 : 194;
        ySize = 208;
    }

    static int ink() { return FarmGuiStyle.ink(); }
    static String tr(String key) { return I18n.format("cropmanager." + key); }

    @Override public void initGui() {
        xSize = tile.advanced && sidebarExpanded ? 310 : 194;
        buttonList.clear();
        super.initGui();
        guiLeft = (width - xSize) / 2;
        buttonList.add(new Control(200, 10, 104, 18, 18));
        buttonList.add(new Control(13, 30, 104, 18, 18));
        buttonList.add(new Control(0, 50, 104, 18, 18));
        buttonList.add(new Control(12, 176, 6, 10, 10));
        if (tile.advanced) buttonList.add(new Control(14, 156, 5, 14, 12));
        if (tile.advanced && sidebarExpanded) {
            buttonList.add(new Control(20, 200, 14, 22, 16));
            buttonList.add(new Control(21, 224, 14, 22, 16));
            AreaWidgets.add(buttonList, guiLeft + 200, guiTop + 38);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        for (GuiButton b : buttonList) {
            if (b.id == 11) b.displayString = tr(AreaPreview.selected(tile) ? "hide_area" : "show_area");
            if (b.id == 12) b.displayString = "i";
            if (b.id == 14) b.displayString = sidebarExpanded ? "<" : ">";
            if (b.id >= 210 && b.id <= 215 || b.id == 11) b.visible = tile.advanced && sidebarExpanded && tab == 1;
        }
        AreaWidgets.refresh(buttonList, tile, tile.radius, tile.layers, tile.serverMaxRadius);
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 14) { sidebarExpanded = !sidebarExpanded; initGui(); return; }
        if (button.id == 20 || button.id == 21) { tab = button.id - 20; refreshButtons(); return; }
        if (button.id == 13 || button.id == 200 || button.id >= 210 && button.id <= 215) {
            ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(tile, inventorySlots.windowId, button.id));
            return;
        }
        if (button.id == 0) ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Settings(tile, inventorySlots.windowId, 0));
        if (button.id == 11) AreaPreview.toggle(tile);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        refreshButtons();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        List<String> tooltip = new ArrayList<>();
        for (GuiButton b : buttonList) if (b.visible && b.isMouseOver()) {
            if (b.id == 200) {
                tooltip.add(tr("tip.power_title"));
                tooltip.add((tile.enabled ? "\u00a7a" : "\u00a7c") + tr(tile.enabled ? "on" : "off"));
                tooltip.add(tr("tip.power_desc"));
            }
            if (b.id == 13) {
                tooltip.add(tr("tip.redstone_title"));
                tooltip.add("\u00a7e" + tr("redstone_" + tile.redstoneMode));
                tooltip.add(tr("tip.redstone_cycle"));
            }
            if (b.id == 0) {
                boolean h = (tile.functions & 1) != 0;
                tooltip.add(tr("tip.harvest_title"));
                tooltip.add((h ? "\u00a7a" : "\u00a7c") + tr(h ? "on" : "off"));
                tooltip.add(tr("tip.harvest_desc"));
            }
            if (b.id == 11) {
                tooltip.add(tr(AreaPreview.selected(tile) ? "hide_area" : "show_area"));
                tooltip.add(I18n.format("cropmanager.area_size", tile.radius * 2 + 1, tile.radius * 2 + 1, Integer.bitCount(tile.layers & 7)));
                tooltip.add(tr("area_help"));
            }
            if (b.id == 14) tooltip.add(tr(sidebarExpanded ? "collapse_panel" : "expand_panel"));
            if (b.id == 20) {
                tooltip.add("\u00a7l" + tr("operation") + " · " + tr("diagnostics"));
                tooltip.add(tr("tip.tab_digital"));
            }
            if (b.id == 21) {
                tooltip.add("\u00a7l" + tr("tab_area"));
                tooltip.add(tr("tip.tab_area_desc"));
            }
            if (b.id == 12) {
                tooltip.addAll(Arrays.asList(tr("compatible_upgrades"), tr("upgrade_overclocker"), tr("upgrade_storage"), tr("upgrade_transformer"), tr("wireless_eu_help")));
                if (tile.advanced) tooltip.add(tr("upgrade_weed_killer"));
            }
        }
        if (isPointInRegion(8, 20, 18, 52, mouseX, mouseY)) tooltip.add(tr("water") + ": " + UiNumbers.compact(tile.water.getFluidAmount()) + " mB (" + UiNumbers.compact(tile.tankCapacity()) + " mB)");
        if (isPointInRegion(72, 20, 18, 52, mouseX, mouseY)) tooltip.add("Weed-Ex: " + UiNumbers.compact(tile.weed.getFluidAmount()) + " mB (" + UiNumbers.compact(tile.tankCapacity()) + " mB)");
        if (isPointInRegion(44, 70, 14, 16, mouseX, mouseY)) tooltip.add(String.format(Locale.ROOT, "%.0f EU (Max %.0f) · T%d", tile.getEnergy(), tile.energy.getCapacity(), tile.energy.getSinkTier()));
        if (isPointInRegion(72, 105, 16, 16, mouseX, mouseY)) {
            tooltip.add(tr("tip.status_title"));
            tooltip.add((tile.status != 0 || tile.paused ? "\u00a7e" : "\u00a7a") + tr(tile.status != 0 ? "needs_attention" : tile.stateKey()));
            if (tile.status == 0 && !tile.paused) tooltip.add(tr("tip.status_ok"));
            else for (int i = 0; i < PROBLEMS.length; i++) if ((tile.status & 1 << i) != 0) tooltip.add("\u00a7c• " + tr(PROBLEMS[i]));
        }
        Slot slot = getSlotUnderMouse();
        if (slot != null && slot.slotNumber < 26 && !slot.getHasStack()) {
            int n = slot.slotNumber;
            tooltip.add(n < 3 ? tr("fertilizer") : n < 18 ? tr("products") : n < 22 ? tr("upgrades") : (n == 22 || n == 24 ? tr("water") : "Weed-Ex") + " · " + tr(n < 24 ? "input" : "output"));
        }
        if (!tooltip.isEmpty()) drawHoveringText(tooltip, mouseX, mouseY);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float ticks, int mouseX, int mouseY) {
        GlStateManager.disableDepth();
        FarmGuiStyle.background(tile.advanced ? (sidebarExpanded ? "manager_advanced" : "manager_advanced_compact") : "manager_basic", guiLeft, guiTop, xSize, ySize);
        gauge(8, 20, 18, 52, tile.water.getFluidAmount() / (double) Math.max(1, tile.tankCapacity()), 0xff438cbd);
        gauge(72, 20, 18, 52, tile.weed.getFluidAmount() / (double) Math.max(1, tile.tankCapacity()), 0xffb9a247);
        FarmGuiStyle.tankScale(guiLeft + 8, guiTop + 20, 18, 52);
        FarmGuiStyle.tankScale(guiLeft + 72, guiTop + 20, 18, 52);
        FarmGuiStyle.bolt(guiLeft + 47, guiTop + 72, tile.getEnergy() / Math.max(1, tile.energy.getCapacity()));
        if (tile.status != 0 || tile.paused) {
            boolean critical = (tile.status & (ManagerTile.NO_ENERGY | ManagerTile.NO_SPACE)) != 0;
            FarmGuiStyle.warningTriangle(guiLeft + 72, guiTop + 106, critical);
        } else {
            FarmGuiStyle.icon(guiLeft + 72, guiTop + 105, 3, 1);
        }
        if (tile.advanced && sidebarExpanded && tab == 0) {
            FarmGuiStyle.digitalScreen(guiLeft + 200, guiTop + 34, 104, 166);
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableDepth();
    }

    private void gauge(int x, int y, int w, int h, double ratio, int color) {
        int left = guiLeft + x, top = guiTop + y;
        ratio = MathHelper.clamp(ratio, 0, 1);
        if (ratio > 0) drawRect(left + 3, top + h - 3 - (int) ((h - 5) * ratio), left + w - 3, top + h - 3, color);
    }

    @Override protected void drawGuiContainerForegroundLayer(int x, int y) {
        FarmGuiStyle.title(fontRenderer, tile.advanced ? tr("advanced_title_short") : "Crop Manager", 10, 7, 140);
        if (tile.advanced && sidebarExpanded) {
            if (tab == 1) AreaWidgets.draw(fontRenderer, 200, 38, tile.radius, tile.layers);
            else drawDigitalScreen();
        }
    }

    private void drawDigitalScreen() {
        int x = 204, y = 39;
        fontRenderer.drawString("\u00a7l" + tr("operation"), x, y, 0xff55ff55);
        int stColor = tile.status != 0 || tile.paused ? 0xffffaa44 : 0xff55ff55;
        fontRenderer.drawSplitString(tr(tile.stateKey()), x, y + 11, 96, stColor);
        fontRenderer.drawString("\u00a7l" + tr("tab_area") + ": \u00a7r" + (tile.radius * 2 + 1) + "x" + (tile.radius * 2 + 1) + "x" + Integer.bitCount(tile.layers & 7), x, y + 32, 0xffcccccc);
        fontRenderer.drawString("\u00a7lEU \u00a7r" + UiNumbers.compact(tile.getEnergy()), x, y + 46, 0xffff5555);
        fontRenderer.drawString("\u00a7l" + tr("water_short") + " \u00a7r" + UiNumbers.compact(tile.water.getFluidAmount()) + " mB", x, y + 57, 0xff55ccff);
        fontRenderer.drawString("\u00a7lWeed \u00a7r" + UiNumbers.compact(tile.weed.getFluidAmount()) + " mB", x, y + 68, 0xffddcc55);

        NBTTagCompound rep = tile.guiState().getCompoundTag("Report");
        NBTTagCompound surv = rep.getCompoundTag("Survey");
        int[] counts = rep.getIntArray("Counts");
        int crops = counts.length > 0 ? counts[0] : 0;
        int ready = counts.length > 1 ? counts[1] : 0;
        int weeds = counts.length > 2 ? counts[2] : 0;
        fontRenderer.drawString("\u00a7l" + tr("diagnostics"), x, y + 86, 0xff55ffff);
        fontRenderer.drawString(tr("crops_found") + ": " + crops, x, y + 98, 0xffdddddd);
        fontRenderer.drawString(tr("ready") + ": " + ready, x, y + 109, 0xff55ff55);
        fontRenderer.drawString(tr("weeds") + ": " + weeds, x, y + 120, weeds > 0 ? 0xffff5555 : 0xffaaaaaa);
        NBTTagList species = surv.getTagList("Species", 10);
        fontRenderer.drawString(tr("species") + ": " + species.tagCount(), x, y + 131, 0xffdddddd);
        if (surv.hasKey("AvgNutrients")) {
            fontRenderer.drawString("N" + surv.getInteger("AvgNutrients") + " H" + surv.getInteger("AvgHumidity") + " L" + surv.getInteger("AvgLight"), x, y + 145, 0xffaaaaaa);
        }
    }

    private final class Control extends GuiButton {
        Control(int id, int x, int y, int w, int h) { super(id, guiLeft + x, guiTop + y, w, h, ""); }
        @Override public void drawButton(Minecraft mc, int mouseX, int mouseY, float ticks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            if (id == 12) { FarmGuiStyle.info(x, y); return; }
            boolean on = id == 0 && (tile.functions & 1) != 0 || id == 200 && tile.enabled || (id == 20 || id == 21) && id - 20 == tab;
            FarmGuiStyle.button(x, y, width, height, enabled, on, hovered);
            if (id == 200) { FarmGuiStyle.icon(x + 1, y + 1, tile.enabled ? 0 : 1, 0); return; }
            if (id == 13) { FarmGuiStyle.icon(x + 1, y + 1, tile.redstoneMode == 0 ? 2 : tile.redstoneMode == 1 ? 3 : 0, tile.redstoneMode == 2 ? 1 : 0); return; }
            if (id == 0) { FarmGuiStyle.icon(x + 1, y + 1, (tile.functions & 1) != 0 ? 1 : 2, 1); return; }
            if (id == 20) { FarmGuiStyle.icon(x + 3, y, 3, 2); return; }
            if (id == 21) { FarmGuiStyle.icon(x + 3, y, 0, 3); return; }
            String label = fontRenderer.trimStringToWidth(displayString, width - 8);
            fontRenderer.drawString(label, x + (width - fontRenderer.getStringWidth(label)) / 2, y + (height - 8) / 2, FarmGuiStyle.buttonInk(enabled, hovered));
        }
    }
}
