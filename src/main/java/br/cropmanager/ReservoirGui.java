package br.cropmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

public final class ReservoirGui extends GuiContainer {
    private static final ResourceLocation ATLAS = new ResourceLocation("cropmanager", "textures/gui/agricultural_reservoir.png");
    private static final ResourceLocation IC2 = new ResourceLocation("ic2", "textures/gui/common.png");
    private static final int[] TANK_X = {32, 128};
    private static final int[] FILTER_X = {8, 150};
    private static final int[] AUTO_X = {28, 130};
    private static final int[] REDSTONE_X = {48, 110};
    // EnumFacing indices: bottom, top, back, front, left, right.
    private static final int[] PORT_X = {80, 80, 98, 80, 62, 98};
    private static final int[] PORT_Y = {56, 20, 56, 38, 38, 38};
    private static final String[] FACES = {"bottom", "top", "back", "front", "left", "right"};
    private static final String[] MODES = {"closed", "input", "output"};
    private static final String[] SIGNALS = {"ignore", "high", "low"};
    private final AgriculturalReservoirTile machine;
    private final ReservoirContainer container;

    public ReservoirGui(InventoryPlayer player, AgriculturalReservoirTile tile) {
        this(new ReservoirContainer(player, tile), tile);
    }

    private ReservoirGui(ReservoirContainer container, AgriculturalReservoirTile tile) {
        super(container);
        this.container = container;
        this.machine = tile;
        xSize = 176;
        ySize = 204;
    }

    private static String tr(String key, Object... args) { return ManagerGui.tr("reservoir_" + key, args); }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (button == 0 && isPointInRegion(153, 5, 15, 13, x, y)) {
            int tab = container.showingUpgrades() ? 0 : 1;
            container.enchantItem(mc.player, tab);
            mc.playerController.sendEnchantPacket(container.windowId, tab);
            return;
        }
        if (container.showingUpgrades()) {
            if (button == 0 && machine.hasModule(2)) for (int i = 0; i < 2; i++) {
                if (isPointInRegion(38 + i * 66, 75, 34, 14, x, y)) { send(40 + i); return; }
            }
        } else {
            for (int i = 0; i < 2; i++) {
                if (isPointInRegion(FILTER_X[i], 80, 18, 18, x, y) && button <= 1) {
                    send(button == 0 ? 30 + i : 32 + i);
                    return;
                }
                if (button != 0) continue;
                if (isPointInRegion(AUTO_X[i], 80, 18, 18, x, y)) { send(i); return; }
                if (isPointInRegion(REDSTONE_X[i], 80, 18, 18, x, y)) { send(2 + i); return; }
            }
            if (button == 0 && isPointInRegion(79, 80, 18, 18, x, y)) { send(4); return; }
            for (EnumFacing face : EnumFacing.values()) {
                int i = face.getIndex();
                if (button <= 1 && isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y)) {
                    send((button == 0 ? 10 : 20) + i);
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
    }

    private void send(int action) {
        ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(machine, container.windowId, action));
    }

    @Override public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);
        List<String> tip = new ArrayList<>();
        if (isPointInRegion(153, 5, 15, 13, x, y)) tip.add(tr(container.showingUpgrades() ? "main_tab" : "upgrades_tab"));
        else if (container.showingUpgrades()) {
            for (int i = 0; i < 3; i++)
                if (!container.getSlot(4 + i).getHasStack() && isPointInRegion(48 + i * 32, 46, 16, 16, x, y))
                    tip.add(tr(i == 0 ? "capacity_help" : "module_help"));
            for (int i = 0; i < 2; i++) if (machine.hasModule(2) && isPointInRegion(38 + i * 66, 75, 34, 14, x, y))
                tip.add(tr("reserve_tip", i + 1, machine.reserveStep(i) * 25));
        } else {
            if (isPointInRegion(57, 20, 14, 15, x, y))
                tip.add(UiNumbers.exactResource(machine.getEnergy(), machine.energy.getCapacity(), "EU") + " · T" + machine.energy.getSinkTier());
            for (int i = 0; i < 2; i++) tankTooltip(tip, i, x, y);
            if (isPointInRegion(79, 80, 18, 18, x, y)) tip.add(tr("comparator", target(machine.comparatorTank())));
            for (int i = 0; i < PORT_X.length; i++) if (isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y)) {
                if (machine.usesLegacyPorts()) tip.add(tr("legacy_ports"));
                else {
                    tip.add(tr("face_" + FACES[i]) + " · " + tr("port_" + MODES[machine.portMode(i)]));
                    tip.add(target(machine.portTank(i)));
                }
                tip.add(tr("port_hint"));
            }
        }
        if (tip.isEmpty()) renderHoveredToolTip(x, y);
        else drawHoveringText(tip, x, y);
    }

    private void tankTooltip(List<String> tip, int i, int x, int y) {
        FluidTank tank = i == 0 ? machine.water : machine.weed;
        FluidStack sample = machine.filterFluid(i);
        if (isPointInRegion(TANK_X[i], 20, 16, 54, x, y)) {
            tip.add(tank.getFluid() == null ? tr("empty") : tank.getFluid().getLocalizedName());
            tip.add(UiNumbers.exactResource(tank.getFluidAmount(), tank.getCapacity(), "mB"));
        } else if (isPointInRegion(FILTER_X[i], 80, 18, 18, x, y)) {
            tip.add(sample == null ? tr("filter") : tr("sample", sample.getLocalizedName()));
            tip.add(tr("sample_hint"));
            tip.add(tr("sample_clear"));
        } else if (isPointInRegion(FILTER_X[i], 20, 18, 18, x, y) && !container.getSlot(i).getHasStack())
            tip.add(tr("bucket_insert"));
        else if (isPointInRegion(FILTER_X[i], 56, 18, 18, x, y) && !container.getSlot(i + 2).getHasStack())
            tip.add(tr("bucket_take"));
        else if (isPointInRegion(AUTO_X[i], 80, 18, 18, x, y)) {
            tip.add(tr((i == 0 ? machine.autoWater : machine.autoWeed) ? "auto_on" : "auto_off"));
            tip.add(tr("auto_rate"));
        } else if (isPointInRegion(REDSTONE_X[i], 80, 18, 18, x, y))
            tip.add(tr("rs_" + SIGNALS[machine.redstoneMode(i)]));
    }

    private static String target(int index) {
        return tr(index == 0 ? "tank_a" : index == 1 ? "tank_b" : "both");
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        blit(ATLAS, 0, 0, 0, 0, 176, 204);
        icon(153, 5, 15, 13, container.showingUpgrades(), container.showingUpgrades() ? "‹" : "+");
        if (container.showingUpgrades()) {
            mc.getTextureManager().bindTexture(ATLAS);
            drawScaledCustomSizeModalRect(guiLeft + 6, guiTop + 17, 6, 110, 1, 1, 164, 83, 256, 256);
            for (int i = 0; i < 3; i++) blit(IC2, 47 + i * 32, 45, 103, 7, 18, 18);
            if (machine.hasModule(2)) for (int i = 0; i < 2; i++)
                icon(38 + i * 66, 75, 34, 14, true, (machine.reserveStep(i) * 25) + "%");
            return;
        }
        for (int i = 0; i < 2; i++) {
            drawTank(i, i == 0 ? machine.water : machine.weed);
            blit(ATLAS, AUTO_X[i], 80, (i == 0 ? machine.autoWater : machine.autoWeed) ? 212 : 194, 36, 18, 18);
            blit(ATLAS, REDSTONE_X[i], 80, 194 + 18 * machine.redstoneMode(i), 56, 18, 18);
        }
        for (int i = 0; i < PORT_X.length; i++) {
            blit(ATLAS, PORT_X[i], PORT_Y[i], 194 + 16 * machine.portMode(i), 0, 16, 16);
            blit(ATLAS, PORT_X[i], PORT_Y[i], 194 + 16 * machine.portTank(i), 16, 16, 16);
            if (isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y))
                blit(ATLAS, PORT_X[i], PORT_Y[i], 242, 0, 14, 16);
        }
        blit(ATLAS, 79, 80, 194 + 18 * machine.comparatorTank(), 76, 18, 18);
        blit(IC2, 60, 21, machine.getEnergy() > 0 ? 114 : 99, 64, 7, 13);
    }

    private void icon(int x, int y, int width, int height, boolean selected, String text) {
        FarmGuiStyle.button(guiLeft + x, guiTop + y, width, height, true, selected, false);
        drawCenteredString(fontRenderer, text, guiLeft + x + width / 2, guiTop + y + (height - 8) / 2, 0xffffff);
    }

    private void drawTank(int i, FluidTank tank) {
        FluidStack fluid = tank.getFluid();
        if (fluid != null && tank.getCapacity() > 0) {
            int filled = MathHelper.clamp((int) Math.ceil(52.0 * fluid.amount / tank.getCapacity()), 1, 52);
            int color = fluid.getFluid() == FluidRegistry.WATER ? 0xff438cbd
                    : fluid.getFluid() == ic2.core.ref.FluidName.weed_ex.getInstance() ? 0xffb9a247
                    : fluid.getFluid().getColor(fluid) | 0xff000000;
            drawRect(guiLeft + TANK_X[i] + 1, guiTop + 73 - filled, guiLeft + TANK_X[i] + 15, guiTop + 73, color);
        }
        blit(ATLAS, TANK_X[i], 20, 176, 0, 16, 54);
        if (machine.filterFluid(i) != null) {
            blit(ATLAS, TANK_X[i] + 8, 19, 194, 96, 10, 10);
            blit(ATLAS, FILTER_X[i] + 4, 84, 194, 96, 10, 10);
        }
    }

    private void blit(ResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1, 1, 1, 1);
        drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, u, v, w, h, 256, 256);
    }

    @Override protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRenderer.drawString("\u00a7l" + tr(container.showingUpgrades() ? "upgrades_tab" : "title"), 8, 7, 0x202020);
    }
}
