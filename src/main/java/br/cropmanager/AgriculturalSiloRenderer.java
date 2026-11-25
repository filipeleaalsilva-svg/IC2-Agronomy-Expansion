package br.cropmanager;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the 3x5x3 Agricultural Grain Silo OBJ mesh, grain level gauge, and stored item icon. */
@SideOnly(Side.CLIENT)
public final class AgriculturalSiloRenderer extends TileEntitySpecialRenderer<AgriculturalSiloTile> {
    private static final int GRAIN_AMBER_RGB = 0xd4a942;
    private final MultiblockObjModel model = new MultiblockObjModel("models/block/agricultural_silo", 2.000, 3.500);

    @Override
    public void render(AgriculturalSiloTile tile, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha) {
        if (!tile.formed || tile.getWorld() == null || tile.center == null) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (state.getBlock() != CropManager.AGRICULTURAL_SILO) return;
        EnumFacing facing = state.getValue(AgriculturalSiloBlock.FACING);
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        try {
            GlStateManager.translate(
                    tile.center.getX() - tile.getPos().getX() + 0.5,
                    tile.baseY - tile.getPos().getY(),
                    tile.center.getZ() - tile.getPos().getZ() + 0.5);
            GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
            GlStateManager.translate(-0.5, 0, -0.5);

            int capacity = tile.getCapacity();
            double fraction = capacity > 0
                    ? Math.min(1.0, (double) tile.getStoredCount() / capacity)
                    : 0.0;
            model.render(fraction, GRAIN_AMBER_RGB);

            ItemStack stored = tile.getStoredTemplate();
            if (!stored.isEmpty()) {
                renderFrontItemPlaque(stored);
            }
        } finally {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        }
    }

    private static void renderFrontItemPlaque(ItemStack stack) {
        GlStateManager.pushMatrix();
        GlStateManager.enableCull();
        GlStateManager.translate(0.5, 1.46, 2.34);
        GlStateManager.scale(0.38F, 0.38F, 0.38F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }
}
