package br.cropmanager;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

/**
 * Standard-themed GUI for {@link EuDishAntennaTile}.
 */
public final class EuDishGui extends GuiContainer {
    private final EuDishAntennaTile machine;

    public EuDishGui(InventoryPlayer player, EuDishAntennaTile machine) {
        super(new EuDishContainer(player, machine));
        this.machine = machine;
        this.xSize = 176;
        this.ySize = 176;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new FarmButton(1, guiLeft + 114, guiTop + 50, 52, 16, ""));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(machine, inventorySlots.windowId, 1));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = guiLeft, y = guiTop;
        FarmGuiStyle.panel(x, y, xSize, ySize);

        for (int i = 0; i < EuDishAntennaTile.MAX_RANGE_UPGRADES; i++) {
            FarmGuiStyle.slot(x + 52 + i * 18, y + 27);
        }

        int barBg = FarmGuiStyle.darkTheme() ? 0xFF080809 : 0xFF555555;
        drawRect(x + 10, y + 72, x + 166, y + 82, barBg);
        int fill = (int) Math.round(154.0 * Math.min(1.0, Math.max(0.0, machine.energy.getEnergyStored() / Math.max(1.0, machine.energy.getCapacity()))));
        if (fill > 0) drawRect(x + 11, y + 73, x + 11 + fill, y + 81, 0xFFD94A38);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                FarmGuiStyle.slot(x + 7 + col * 18, y + 93 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            FarmGuiStyle.slot(x + 7 + col * 18, y + 151);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("tile.cropmanager.eu_dish_antenna.name");
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 8, FarmGuiStyle.ink());
        buttonList.get(0).displayString = I18n.format("cropmanager.wifi_rs_" + machine.redstoneMode);

        String info = I18n.format("cropmanager.dish_info", machine.clientRadius, machine.clientLinkedCount);
        fontRenderer.drawString(info, 10, 54, FarmGuiStyle.mutedInk());

        String sent = UiNumbers.exact(machine.lastSentRate) + " EU/t";
        fontRenderer.drawString(sent, 10, 84, FarmGuiStyle.mutedInk());
        String buf = UiNumbers.compact(machine.energy.getEnergyStored()) + " / " + UiNumbers.compact(machine.energy.getCapacity()) + " EU";
        fontRenderer.drawString(buf, xSize - 10 - fontRenderer.getStringWidth(buf), 84, FarmGuiStyle.mutedInk());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
