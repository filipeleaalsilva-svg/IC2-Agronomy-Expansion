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
    private static int radius, layers, offsetX, offsetY, offsetZ;
    private AreaPreview() {}
    static boolean selected(FarmTile tile) { return target == tile; }
    static void toggle(FarmTile tile) {
        if (selected(tile)) target = null;
        else { target = tile; if (tile instanceof ManagerTile || tile instanceof WeederTile) refresh(tile); else { radius = 4; layers = 7; offsetX = offsetY = offsetZ = 0; } }
    }
    static void refresh(FarmTile tile) {
        if (selected(tile)) {
            if (tile instanceof ManagerTile) {
                ManagerTile m = (ManagerTile) tile;
                radius = m.radius; layers = m.layers;
                offsetX = m.offsetX; offsetY = m.offsetY; offsetZ = m.offsetZ;
            } else {
                radius = ((WeederTile) tile).radius; layers = ((WeederTile) tile).layers;
                offsetX = offsetY = offsetZ = 0;
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || target == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || target.getWorld() != mc.world || target.isInvalid()
                || !mc.world.isBlockLoaded(target.getPos(), false)
                || mc.world.getTileEntity(target.getPos()) != target) target = null;
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
            for (int bit = 0; bit < 3; bit++) if ((layers & (1 << bit)) != 0) {
                int level = target.getPos().getY() + offsetY + bit - 1;
                if (level < 0 || level >= mc.world.getHeight()) continue;
                AxisAlignedBB box = new AxisAlignedBB(target.getPos().getX() + offsetX - radius, level,
                        target.getPos().getZ() + offsetZ - radius, target.getPos().getX() + offsetX + radius + 1,
                        level + 1, target.getPos().getZ() + offsetZ + radius + 1).grow(0.002).offset(-x, -y, -z);
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
