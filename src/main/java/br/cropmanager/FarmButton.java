package br.cropmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

final class FarmButton extends GuiButton {
    boolean selected;
    FarmButton(int id, int x, int y, int w, int h, String text) { super(id, x, y, w, h, text); }
    @Override public void drawButton(Minecraft mc, int mouseX, int mouseY, float ticks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        if (id == 12) { FarmGuiStyle.info(x, y); return; }
        FarmGuiStyle.button(x, y, width, height, enabled, selected, hovered);
        String label = mc.fontRenderer.trimStringToWidth(displayString, width - 6);
        mc.fontRenderer.drawString(label, x + (width - mc.fontRenderer.getStringWidth(label)) / 2, y + (height - 8) / 2, FarmGuiStyle.buttonInk(enabled, hovered));
    }
}
