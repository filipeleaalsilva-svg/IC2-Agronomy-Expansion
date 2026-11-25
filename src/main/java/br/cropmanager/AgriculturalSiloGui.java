package br.cropmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** Clean single-tab GUI for the EU-free Agricultural Grain Silo. */
public final class AgriculturalSiloGui extends GuiContainer {
    private static final ResourceLocation ATLAS = new ResourceLocation("cropmanager", "textures/gui/agricultural_silo.png");
    private static final int[] PORT_X = {80, 80, 98, 80, 62, 98};
    private static final int[] PORT_Y = {56, 20, 56, 38, 38, 38};
    private static final String[] FACES = {"bottom", "top", "back", "front", "left", "right"};
    private static final String[] MODES = {"closed", "input", "output"};
    private static final String[] SIGNALS = {"ignore", "high", "low"};

    private final AgriculturalSiloTile machine;
    private final AgriculturalSiloContainer container;

    public AgriculturalSiloGui(InventoryPlayer player, AgriculturalSiloTile tile) {
        this(new AgriculturalSiloContainer(player, tile), tile);
    }

    private AgriculturalSiloGui(AgriculturalSiloContainer container, AgriculturalSiloTile tile) {
        super(container);
        this.container = container;
        this.machine = tile;
        xSize = 176;
        ySize = 204;
    }

    private static String tr(String key, Object... args) {
        return ManagerGui.tr("reservoir_" + key, args);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        if (isPointInRegion(8, 80, 18, 18, x, y) && button <= 1) {
            send(button == 0 ? 2 : 3);
            return;
        }
        if (button == 0) {
            if (isPointInRegion(126, 80, 18, 18, x, y)) {
                send(0);
                return;
            }
            if (isPointInRegion(148, 80, 18, 18, x, y)) {
                send(1);
                return;
            }
            for (EnumFacing face : EnumFacing.values()) {
                int i = face.getIndex();
                if (isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y)) {
                    send(10 + i);
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
    }

    private void send(int action) {
        ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(machine, container.windowId, action));
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(x, y, partialTicks);
        List<String> tip = new ArrayList<>();
        ItemStack template = machine.getStoredTemplate();
        int count = machine.getStoredCount();
        int cap = machine.getCapacity();

        if (isPointInRegion(34, 20, 20, 54, x, y)) {
            tip.add(template.isEmpty() ? tr("empty") : template.getDisplayName());
            tip.add(UiNumbers.exact(count) + " / " + UiNumbers.exact(cap));
            if (!template.isEmpty() && template.getMaxStackSize() > 1) {
                int stackSize = template.getMaxStackSize();
                tip.add(ManagerGui.tr("silo_stacks_tip",
                        Integer.valueOf(count / stackSize), Integer.valueOf(count % stackSize)));
            }
        } else if (isPointInRegion(8, 80, 18, 18, x, y)) {
            tip.add(ManagerGui.tr(machine.isLocked() ? "silo_locked" : "silo_unlocked"));
            if (!template.isEmpty()) tip.add(tr("sample", template.getDisplayName()));
            tip.add(ManagerGui.tr("silo_lock_hint"));
        } else if (isPointInRegion(8, 20, 18, 18, x, y) && !container.getSlot(0).getHasStack()) {
            tip.add(ManagerGui.tr("silo_deposit_tip"));
        } else if (isPointInRegion(8, 56, 18, 18, x, y) && !container.getSlot(1).getHasStack()) {
            tip.add(ManagerGui.tr("silo_withdraw_tip"));
        } else if (isPointInRegion(147, 20, 18, 18, x, y) && !container.getSlot(2).getHasStack()) {
            tip.add(ManagerGui.tr("silo_capacity_tip"));
        } else if (isPointInRegion(148, 56, 18, 18, x, y)) {
            tip.add(ManagerGui.tr("fluid_tank_comparator_tip", Integer.valueOf(machine.comparatorSignal())));
            tip.add(ManagerGui.tr("silo_comparator_desc"));
        } else if (isPointInRegion(126, 80, 18, 18, x, y)) {
            tip.add(tr(machine.autoOutput ? "auto_on" : "auto_off"));
            tip.add(ManagerGui.tr("silo_auto_desc", Integer.valueOf(ManagerConfig.siloAutoEjectRate)));
        } else if (isPointInRegion(148, 80, 18, 18, x, y)) {
            tip.add(tr("rs_" + SIGNALS[machine.redstoneMode()]));
            tip.add(ManagerGui.tr("silo_rs_desc"));
        } else {
            for (int i = 0; i < PORT_X.length; i++) {
                if (isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y)) {
                    tip.add(tr("face_" + FACES[i]) + " · " + tr("port_" + MODES[machine.portMode(i)]));
                    tip.add(ManagerGui.tr("fluid_tank_port_hint"));
                    break;
                }
            }
        }
        if (tip.isEmpty()) renderHoveredToolTip(x, y);
        else drawHoveringText(tip, x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y) {
        blit(ATLAS, 0, 0, 0, 0, 176, 204);
        drawStorageMeter();
        for (int i = 0; i < PORT_X.length; i++) {
            blit(ATLAS, PORT_X[i], PORT_Y[i], 194 + 16 * machine.portMode(i), 0, 16, 16);
            if (isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y)) {
                blit(ATLAS, PORT_X[i], PORT_Y[i], 242, 0, 14, 16);
            }
        }
        blit(ATLAS, 126, 80, machine.autoOutput ? 212 : 194, 36, 18, 18);
        blit(ATLAS, 148, 80, 194 + 18 * machine.redstoneMode(), 56, 18, 18);
        if (machine.isLocked()) {
            blit(ATLAS, 44, 19, 194, 96, 10, 10);
            blit(ATLAS, 12, 84, 194, 96, 10, 10);
        }
    }

    private void drawStorageMeter() {
        int count = machine.getStoredCount();
        int cap = machine.getCapacity();
        if (count > 0 && cap > 0) {
            int filled = MathHelper.clamp((int) Math.ceil(52.0 * count / cap), 1, 52);
            drawRect(guiLeft + 35, guiTop + 73 - filled, guiLeft + 53, guiTop + 73, 0xffd4a942);
        }
        blit(ATLAS, 34, 20, 176, 0, 20, 54);
        ItemStack template = machine.getStoredTemplate();
        if (!template.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemIntoGUI(template, guiLeft + 36, guiTop + 78);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
        }
    }

    private void blit(ResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1, 1, 1, 1);
        drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, u, v, w, h, 256, 256);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        FarmGuiStyle.title(fontRenderer, ManagerGui.tr("silo_title"), 8, 7, 160);
        String upg = machine.upgradeCount() + "/" + AgriculturalSiloTile.MAX_CAPACITY_UPGRADES;
        fontRenderer.drawString(upg, 156 - fontRenderer.getStringWidth(upg) / 2, 41, FarmGuiStyle.mutedInk());
        String sig = Integer.toString(machine.comparatorSignal());
        fontRenderer.drawString(sig, 157 - fontRenderer.getStringWidth(sig) / 2, 61, FarmGuiStyle.ink());
        if (!machine.getStoredTemplate().isEmpty() || machine.getStoredCount() > 0) {
            String qty = UiNumbers.compact(machine.getStoredCount());
            fontRenderer.drawString(qty, 56, 85, FarmGuiStyle.ink());
        }
    }
}
