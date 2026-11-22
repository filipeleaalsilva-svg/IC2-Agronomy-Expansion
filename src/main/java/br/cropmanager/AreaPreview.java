package br.cropmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/** One local preview; never requests chunks or changes machine settings. */
@Mod.EventBusSubscriber(modid = CropManager.ID, value = Side.CLIENT)
public final class AreaPreview {
    private static FarmTile target;
    private static int radius, layers;
    private AreaPreview() {}
    static boolean selected(FarmTile tile) { return target == tile; }
    static void toggle(FarmTile tile) {
        if (selected(tile)) target = null;
        else { target = tile; if (tile instanceof ManagerTile || tile instanceof WeederTile) refresh(tile); else { radius = 4; layers = 7; } }
    }
    static void refresh(FarmTile tile) {
        if (selected(tile)) { radius = tile instanceof ManagerTile ? ((ManagerTile)tile).radius : ((WeederTile)tile).radius; layers = tile instanceof ManagerTile ? ((ManagerTile)tile).layers : ((WeederTile)tile).layers; }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || target == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || target.getWorld() != mc.world || target.isInvalid()
                || !mc.world.isBlockLoaded(target.getPos(), false)
                || mc.world.getTileEntity(target.getPos()) != target) target = null;
    }
    private static void markers(int[] data, double x, double y, double z, boolean unknown) {
        for (int i = 0; i + 2 < data.length; i += 3) marker(new net.minecraft.util.math.BlockPos(data[i], data[i+1], data[i+2]).toLong(), x, y, z, unknown);
    }
    private static void marker(long point, double x, double y, double z, boolean unknown) {
        net.minecraft.util.math.BlockPos p = net.minecraft.util.math.BlockPos.fromLong(point);
        double inset = unknown ? 0.30 : 0.05;
        AxisAlignedBB box = new AxisAlignedBB(p.getX() + inset, p.getY() + inset, p.getZ() + inset,
                p.getX() + 1 - inset, p.getY() + 1 - inset, p.getZ() + 1 - inset).offset(-x, -y, -z);
        RenderGlobal.drawSelectionBoundingBox(box, 1, unknown ? 0.8f : 0.25f, unknown ? 0.2f : 0.25f, 0.9f);
    }
    @SubscribeEvent public static void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (target == null || mc.world != target.getWorld()) return;
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) return;
        double t = event.getPartialTicks();
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * t;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * t;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * t;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        try {
            if (target instanceof ManagerTile && ((ManagerTile)target).advanced) {
                markers(((ManagerTile)target).report.getIntArray("Rejected"), x, y, z, false);
                markers(((ManagerTile)target).report.getIntArray("Unknown"), x, y, z, true);
            }
            for (int bit = 0; bit < 3; bit++) if ((layers & (1 << bit)) != 0) {
                int level = target.getPos().getY() + bit - 1;
                if (level < 0 || level >= mc.world.getHeight()) continue;
                AxisAlignedBB box = new AxisAlignedBB(target.getPos().getX() - radius, level,
                        target.getPos().getZ() - radius, target.getPos().getX() + radius + 1,
                        level + 1, target.getPos().getZ() + radius + 1).grow(0.002).offset(-x, -y, -z);
                RenderGlobal.drawSelectionBoundingBox(box, 0.55f, 0.95f, 0.35f, 0.8f);
            }
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
    }
}
