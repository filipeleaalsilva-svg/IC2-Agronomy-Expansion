package br.cropmanager;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

/**
 * Dark-themed (#1C1C1D) GUI for {@link EuWifiPlugTile} and {@link EuWifiPointTile}.
 */
public final class EuWifiGui extends GuiContainer {
    private final FarmTile machine;
    private final boolean isPoint;

    public EuWifiGui(InventoryPlayer player, FarmTile machine) {
        super(new EuWifiContainer(player, machine));
        this.machine = machine;
        this.isPoint = machine instanceof EuWifiPointTile;
        this.xSize = 176;
        this.ySize = 176;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        int left = guiLeft, top = guiTop;
        buttonList.add(new FarmButton(1, left + 10, top + 24, 24, 16, "-10"));
        buttonList.add(new FarmButton(2, left + 36, top + 24, 20, 16, "-1"));
        buttonList.add(new FarmButton(3, left + 120, top + 24, 20, 16, "+1"));
        buttonList.add(new FarmButton(4, left + 142, top + 24, 24, 16, "+10"));
        if (isPoint) {
            buttonList.add(new FarmButton(5, left + 10, top + 48, 50, 16, ""));
            buttonList.add(new FarmButton(6, left + 64, top + 48, 48, 16, ""));
            buttonList.add(new FarmButton(7, left + 116, top + 48, 50, 16, ""));
        } else {
            buttonList.add(new FarmButton(8, left + 10, top + 48, 156, 16, ""));
        }
    }

    private int currentChannel() {
        return isPoint ? ((EuWifiPointTile) machine).channel : ((EuWifiPlugTile) machine).channel;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 1 && button.id <= 4) {
            int delta = button.id == 1 ? -10 : button.id == 2 ? -1 : button.id == 3 ? 1 : 10;
            int next = WirelessEuGridData.clampChannel(currentChannel() + delta);
            ManagerNetwork.CHANNEL.sendToServer(
                    new ManagerNetwork.Action(machine, inventorySlots.windowId, 1000 + next));
            return;
        }
        if (isPoint) {
            int action = button.id == 5 ? 1 : button.id == 6 ? (isShiftKeyDown() ? 3 : 4) : 2;
            ManagerNetwork.CHANNEL.sendToServer(
                    new ManagerNetwork.Action(machine, inventorySlots.windowId, action));
        } else if (button.id == 8) {
            ManagerNetwork.CHANNEL.sendToServer(
                    new ManagerNetwork.Action(machine, inventorySlots.windowId, 1));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = guiLeft, y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, 0xFF1C1C1D);
        drawRect(x, y, x + xSize, y + 1, 0xFF4E4E52);
        drawRect(x, y, x + 1, y + ySize, 0xFF4E4E52);
        drawRect(x, y + ySize - 1, x + xSize, y + ySize, 0xFF080809);
        drawRect(x + xSize - 1, y, x + xSize, y + ySize, 0xFF080809);

        // Energy bar
        double gridEu = isPoint ? ((EuWifiPointTile) machine).clientGridEu : ((EuWifiPlugTile) machine).clientGridEu;
        double gridCap = isPoint ? ((EuWifiPointTile) machine).clientGridCap : ((EuWifiPlugTile) machine).clientGridCap;
        drawRect(x + 10, y + 72, x + 166, y + 82, 0xFF080809);
        int fill = (int) Math.round(154.0 * Math.min(1.0, Math.max(0.0, gridEu / Math.max(1.0, gridCap))));
        if (fill > 0) drawRect(x + 11, y + 73, x + 11 + fill, y + 81, 0xFFD94A38);

        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(x + 7 + col * 18, y + 93 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(x + 7 + col * 18, y + 151);
        }
    }

    private void drawSlotBox(int sx, int sy) {
        drawRect(sx, sy, sx + 18, sy + 18, 0xFF0D0D0E);
        drawRect(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF121213);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(isPoint ? "tile.cropmanager.eu_wifi_point.name" : "tile.cropmanager.eu_wifi_plug.name");
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 8, 0xFFE6E6E6);

        String chLabel = I18n.format("cropmanager.wifi_channel", currentChannel());
        fontRenderer.drawString(chLabel, (xSize - fontRenderer.getStringWidth(chLabel)) / 2, 28, 0xFFE6E6E6);

        if (isPoint) {
            EuWifiPointTile p = (EuWifiPointTile) machine;
            buttonList.get(4).displayString = "T" + p.outputTier;
            buttonList.get(5).displayString = p.effectiveRate() + " EU/t";
            buttonList.get(6).displayString = I18n.format("cropmanager.wifi_rs_" + p.redstoneMode);
            String rate = UiNumbers.exact(p.lastOutputRate) + " EU/t";
            fontRenderer.drawString(rate, 12, 84, 0xFFB0B0B0);
        } else {
            EuWifiPlugTile p = (EuWifiPlugTile) machine;
            buttonList.get(4).displayString = I18n.format(
                    p.clientPrivate ? "cropmanager.wifi_private" : "cropmanager.wifi_public");
            String rate = UiNumbers.exact(p.lastInputRate) + " EU/t";
            fontRenderer.drawString(rate, 12, 84, 0xFFB0B0B0);
        }

        double gridEu = isPoint ? ((EuWifiPointTile) machine).clientGridEu : ((EuWifiPlugTile) machine).clientGridEu;
        double gridCap = isPoint ? ((EuWifiPointTile) machine).clientGridCap : ((EuWifiPlugTile) machine).clientGridCap;
        String buf = UiNumbers.compact(gridEu) + " / " + UiNumbers.compact(gridCap) + " EU";
        fontRenderer.drawString(buf, xSize - 12 - fontRenderer.getStringWidth(buf), 84, 0xFFB0B0B0);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
