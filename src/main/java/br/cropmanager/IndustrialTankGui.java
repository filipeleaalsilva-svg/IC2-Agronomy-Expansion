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

/** Clean single-tab GUI for the EU-free Industrial Fluid Reservoir. */
public final class IndustrialTankGui extends GuiContainer {
    private static final ResourceLocation ATLAS = new ResourceLocation("cropmanager", "textures/gui/fluid_tank.png");
    private static final int[] PORT_X = {80, 80, 98, 80, 62, 98};
    private static final int[] PORT_Y = {56, 20, 56, 38, 38, 38};
    private static final String[] FACES = {"bottom", "top", "back", "front", "left", "right"};
    private static final String[] MODES = {"closed", "input", "output"};
    private static final String[] SIGNALS = {"ignore", "high", "low"};

    private final IndustrialTankTile machine;
    private final IndustrialTankContainer container;

    public IndustrialTankGui(InventoryPlayer player, IndustrialTankTile tile) {
        this(new IndustrialTankContainer(player, tile), tile);
    }

    private IndustrialTankGui(IndustrialTankContainer container, IndustrialTankTile tile) {
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
            send(button == 0 ? 30 : 31);
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
        FluidTank tank = machine.tank;
        FluidStack sample = machine.filterFluid();
        if (isPointInRegion(34, 20, 20, 54, x, y)) {
            tip.add(tank.getFluid() == null ? tr("empty") : tank.getFluid().getLocalizedName());
            tip.add(UiNumbers.exactResource(tank.getFluidAmount(), tank.getCapacity(), "mB"));
        } else if (isPointInRegion(8, 80, 18, 18, x, y)) {
            tip.add(sample == null ? tr("filter") : tr("sample", sample.getLocalizedName()));
            tip.add(tr("sample_hint"));
            tip.add(tr("sample_clear"));
        } else if (isPointInRegion(8, 20, 18, 18, x, y) && !container.getSlot(0).getHasStack()) {
            tip.add(tr("bucket_insert"));
        } else if (isPointInRegion(8, 56, 18, 18, x, y) && !container.getSlot(1).getHasStack()) {
            tip.add(tr("bucket_take"));
        } else if (isPointInRegion(147, 20, 18, 18, x, y) && !container.getSlot(2).getHasStack()) {
            tip.add(ManagerGui.tr("fluid_tank_capacity_tip"));
        } else if (isPointInRegion(148, 56, 18, 18, x, y)) {
            tip.add(ManagerGui.tr("fluid_tank_comparator_tip", Integer.valueOf(machine.comparatorSignal())));
            tip.add(ManagerGui.tr("fluid_tank_comparator_desc"));
        } else if (isPointInRegion(126, 80, 18, 18, x, y)) {
            tip.add(tr(machine.autoOutput ? "auto_on" : "auto_off"));
            tip.add(ManagerGui.tr("fluid_tank_auto_desc", Integer.valueOf(ManagerConfig.industrialTankAutoEjectRate)));
        } else if (isPointInRegion(148, 80, 18, 18, x, y)) {
            tip.add(tr("rs_" + SIGNALS[machine.redstoneMode()]));
            tip.add(ManagerGui.tr("fluid_tank_rs_desc"));
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
        drawTank(machine.tank);
        for (int i = 0; i < PORT_X.length; i++) {
            blit(ATLAS, PORT_X[i], PORT_Y[i], 194 + 16 * machine.portMode(i), 0, 16, 16);
            if (isPointInRegion(PORT_X[i], PORT_Y[i], 16, 16, x, y)) {
                blit(ATLAS, PORT_X[i], PORT_Y[i], 242, 0, 14, 16);
            }
        }
        blit(ATLAS, 126, 80, machine.autoOutput ? 212 : 194, 36, 18, 18);
        blit(ATLAS, 148, 80, 194 + 18 * machine.redstoneMode(), 56, 18, 18);
    }

    private void drawTank(FluidTank tank) {
        FluidStack fluid = tank.getFluid();
        if (fluid != null && tank.getCapacity() > 0) {
            int filled = MathHelper.clamp((int) Math.ceil(52.0 * fluid.amount / tank.getCapacity()), 1, 52);
            int color = fluid.getFluid() == FluidRegistry.WATER ? 0xff438cbd
                    : fluid.getFluid() == ic2.core.ref.FluidName.weed_ex.getInstance() ? 0xffb9a247
                    : fluid.getFluid() == CropManager.GRIN_EXTRACT ? 0xff83aa55
                    : fluid.getFluid().getColor(fluid) | 0xff000000;
            drawRect(guiLeft + 35, guiTop + 73 - filled, guiLeft + 53, guiTop + 73, color);
        }
        blit(ATLAS, 34, 20, 176, 0, 20, 54);
        if (machine.filterFluid() != null) {
            blit(ATLAS, 44, 19, 194, 96, 10, 10);
            blit(ATLAS, 12, 84, 194, 96, 10, 10);
        }
    }

    private void blit(ResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1, 1, 1, 1);
        drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, u, v, w, h, 256, 256);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        FarmGuiStyle.title(fontRenderer, ManagerGui.tr("fluid_tank_title"), 8, 7, 160);
        String count = machine.upgradeCount() + "/" + IndustrialTankTile.MAX_CAPACITY_UPGRADES;
        fontRenderer.drawString(count, 156 - fontRenderer.getStringWidth(count) / 2, 41, FarmGuiStyle.mutedInk());
        String sig = Integer.toString(machine.comparatorSignal());
        fontRenderer.drawString(sig, 157 - fontRenderer.getStringWidth(sig) / 2, 61, FarmGuiStyle.ink());
    }
}
