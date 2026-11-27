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
        FarmGuiStyle.panel(guiLeft, guiTop, xSize, ySize);
        for (net.minecraft.inventory.Slot slot : inventorySlots.inventorySlots) {
            FarmGuiStyle.slot(guiLeft + slot.xPos, guiTop + slot.yPos);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("item.cropmanager.crossing_tool.name"), 8, 8, FarmGuiStyle.ink());
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 51, FarmGuiStyle.mutedInk());
    }
}
