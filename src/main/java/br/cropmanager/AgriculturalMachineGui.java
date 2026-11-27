package br.cropmanager;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

final class AgriculturalMachineGui extends GuiContainer {
    private final FarmTile machine;

    AgriculturalMachineGui(InventoryPlayer player, FarmTile machine) {
        super(new EuWifiContainer(player, machine));
        this.machine = machine;
        ySize = 176;
    }

    @Override public void initGui() {
        super.initGui();
        if (!(machine instanceof DiagonalSprinklerTile)) return;
        for (int bit = 0; bit < 4; bit++) {
            addButton(new FarmButton(bit, guiLeft + 20 + (bit & 1) * 50,
                    guiTop + 37 + (bit >> 1) * 19, 46, 17, ""));
        }
        addButton(new FarmButton(4, guiLeft + 123, guiTop + 37, 43, 17, ""));
    }

    @Override protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {
        ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(machine, inventorySlots.windowId, button.id));
    }

    @Override public void updateScreen() {
        super.updateScreen();
        if (!(machine instanceof DiagonalSprinklerTile)) return;
        DiagonalSprinklerTile sprinkler = (DiagonalSprinklerTile) machine;
        for (int bit = 0; bit < 4; bit++) {
            buttonList.get(bit).displayString = I18n.format("cropmanager.sprinkler_diagonal.zone_" + bit)
                    + (sprinkler.enabled(bit) ? " +" : " -");
        }
        buttonList.get(4).displayString = I18n.format("cropmanager.sprinkler_diagonal.rs_" + sprinkler.redstoneMode());
    }

    @Override public void drawScreen(int x, int y, float tick) {
        drawDefaultBackground();
        super.drawScreen(x, y, tick);
        renderHoveredToolTip(x, y);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float tick, int x, int y) {
        FarmGuiStyle.panel(guiLeft, guiTop, xSize, ySize);
        for (net.minecraft.inventory.Slot slot : inventorySlots.inventorySlots) {
            FarmGuiStyle.slot(guiLeft + slot.xPos, guiTop + slot.yPos);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int x, int y) {
        int titleColor = FarmGuiStyle.ink();
        int waterColor = FarmGuiStyle.darkTheme() ? 0x80caff : 0x1d6a96;
        int gasColor = FarmGuiStyle.darkTheme() ? 0x9aea64 : 0x487a22;
        int energyColor = FarmGuiStyle.darkTheme() ? 0xffd65a : 0x9c6500;
        if (machine instanceof DiagonalSprinklerTile) {
            DiagonalSprinklerTile sprinkler = (DiagonalSprinklerTile) machine;
            fontRenderer.drawString(I18n.format("tile.cropmanager.sprinkler_diagonal.name"), 8, 8, titleColor);
            fontRenderer.drawString(I18n.format("cropmanager.biogas_sprayer.water", sprinkler.water.getFluidAmount()), 8, 23, waterColor);
            return;
        }
        if (machine instanceof BiogasSprayerTile) {
            BiogasSprayerTile sprayer = (BiogasSprayerTile) machine;
            fontRenderer.drawString(I18n.format("tile.cropmanager.biogas_sprayer.name"), 8, 8, titleColor);
            fontRenderer.drawString(I18n.format("cropmanager.biogas_sprayer.gas", sprayer.gas.getFluidAmount()), 8, 30, gasColor);
            fontRenderer.drawString(I18n.format("cropmanager.biogas_sprayer.water", sprayer.water.getFluidAmount()), 8, 50, waterColor);
            return;
        }
        BiogasGeneratorTile generator = (BiogasGeneratorTile) machine;
        fontRenderer.drawString(I18n.format("tile.cropmanager.biogas_generator.name"), 8, 8, titleColor);
        fontRenderer.drawString(I18n.format(generator.isFormed() ? "cropmanager.biogas_generator.ready"
                : "cropmanager.biogas_generator.incomplete"), 8, 28, titleColor);
        fontRenderer.drawString(I18n.format("cropmanager.biogas_generator.gas", generator.gas.getFluidAmount()), 8, 43, gasColor);
        fontRenderer.drawString(I18n.format("cropmanager.biogas_generator.energy", (int) generator.guiState().getDouble("EU")), 8, 58, energyColor);
    }
}
