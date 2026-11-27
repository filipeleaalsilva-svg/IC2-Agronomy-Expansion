package br.cropmanager;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

/**
 * Standard-themed GUI for {@link EuWifiPlugTile} and {@link EuWifiPointTile}.
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
        FarmGuiStyle.panel(x, y, xSize, ySize);

        // Energy bar
        double gridEu = isPoint ? ((EuWifiPointTile) machine).clientGridEu : ((EuWifiPlugTile) machine).clientGridEu;
        double gridCap = isPoint ? ((EuWifiPointTile) machine).clientGridCap : ((EuWifiPlugTile) machine).clientGridCap;
        int barBg = FarmGuiStyle.darkTheme() ? 0xFF080809 : 0xFF555555;
        drawRect(x + 10, y + 72, x + 166, y + 82, barBg);
        int fill = (int) Math.round(154.0 * Math.min(1.0, Math.max(0.0, gridEu / Math.max(1.0, gridCap))));
        if (fill > 0) drawRect(x + 11, y + 73, x + 11 + fill, y + 81, 0xFFD94A38);

        // Player inventory slots
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
        String title = I18n.format(isPoint ? "tile.cropmanager.eu_wifi_point.name" : "tile.cropmanager.eu_wifi_plug.name");
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 8, FarmGuiStyle.ink());

        String chLabel = I18n.format("cropmanager.wifi_channel", currentChannel());
        fontRenderer.drawString(chLabel, (xSize - fontRenderer.getStringWidth(chLabel)) / 2, 28, FarmGuiStyle.ink());

        if (isPoint) {
            EuWifiPointTile p = (EuWifiPointTile) machine;
            buttonList.get(4).displayString = "T" + p.outputTier;
            buttonList.get(5).displayString = p.effectiveRate() + " EU/t";
            buttonList.get(6).displayString = I18n.format("cropmanager.wifi_rs_" + p.redstoneMode);
            String rate = UiNumbers.exact(p.lastOutputRate) + " EU/t";
            fontRenderer.drawString(rate, 12, 84, FarmGuiStyle.mutedInk());
        } else {
            EuWifiPlugTile p = (EuWifiPlugTile) machine;
            buttonList.get(4).displayString = I18n.format(
                    p.clientPrivate ? "cropmanager.wifi_private" : "cropmanager.wifi_public");
            String rate = UiNumbers.exact(p.lastInputRate) + " EU/t";
            fontRenderer.drawString(rate, 12, 84, FarmGuiStyle.mutedInk());
        }

        double gridEu = isPoint ? ((EuWifiPointTile) machine).clientGridEu : ((EuWifiPlugTile) machine).clientGridEu;
        double gridCap = isPoint ? ((EuWifiPointTile) machine).clientGridCap : ((EuWifiPlugTile) machine).clientGridCap;
        String buf = UiNumbers.compact(gridEu) + " / " + UiNumbers.compact(gridCap) + " EU";
        fontRenderer.drawString(buf, xSize - 12 - fontRenderer.getStringWidth(buf), 84, FarmGuiStyle.mutedInk());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
