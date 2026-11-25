package br.cropmanager;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;

final class CrossingToolGui extends GuiContainer {
    CrossingToolGui(CrossingToolContainer container) {
        super(container);
        xSize = 176;
        ySize = 144;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF1C1C1D);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, 0xFF4E4E52);
        drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, 0xFF4E4E52);
        drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, 0xFF080809);
        drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF080809);
        for (net.minecraft.inventory.Slot slot : inventorySlots.inventorySlots) {
            int x = guiLeft + slot.xPos;
            int y = guiTop + slot.yPos;
            drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF0D0D0E);
            drawRect(x, y, x + 16, y + 16, 0xFF121213);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("item.cropmanager.crossing_tool.name"), 8, 8, 0xFFE6E6E6);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 51, 0xFFB0B0B0);
    }
}
