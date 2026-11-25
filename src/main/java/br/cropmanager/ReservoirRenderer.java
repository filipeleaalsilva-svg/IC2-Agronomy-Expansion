package br.cropmanager;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import org.lwjgl.opengl.GL11;

/** Draws the two decorative liquid columns inside the structure's strictly-air core. */
public final class ReservoirRenderer extends TileEntitySpecialRenderer<AgriculturalReservoirTile> {
    private final ReservoirTowerModel tower = new ReservoirTowerModel();
    private static final ResourceLocation BLOCK_ATLAS = new ResourceLocation("textures/atlas/blocks.png");

    @Override public void render(AgriculturalReservoirTile tile, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha) {
        if (!tile.formed || tile.getWorld() == null || tile.center == null) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (state.getBlock() != CropManager.AGRICULTURAL_RESERVOIR) return;
        EnumFacing facing = state.getValue(AgriculturalReservoirBlock.FACING);
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        try {
            if (tile.usesTowerModel()) {
                GlStateManager.translate(tile.center.getX() - tile.getPos().getX() + .5,
                        tile.baseY - tile.getPos().getY(), tile.center.getZ() - tile.getPos().getZ() + .5);
                GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
                GlStateManager.translate(-.5, 0, -.5);
                tower.render(tile.water, tile.weed);
                return;
            }
            bindTexture(BLOCK_ATLAS);
            drawTank(tile, tile.water, 0);
            drawTank(tile, tile.weed, 1);
            drawStatusLight(tile, facing);
        } finally {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        }
    }

    private void drawTank(AgriculturalReservoirTile tile, FluidTank tank, int index) {
        FluidStack fluid = tank.getFluid();
        if (fluid == null || fluid.amount <= 0 || tank.getCapacity() <= 0) return;
        ResourceLocation location = fluid.getFluid().getStill(fluid);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
        double centerX = tile.center.getX() - tile.getPos().getX();
        double centerZ = tile.center.getZ() - tile.getPos().getZ();
        double floorY = tile.baseY - tile.getPos().getY() + 1;
        double innerHeight = tile.height - 2;
        double fillHeight = innerHeight * Math.min(1.0, (double) fluid.amount / tank.getCapacity());
        double minX = centerX + (index == 0 ? 0.08 : 0.53);
        double maxX = centerX + (index == 0 ? 0.47 : 0.92);
        double minZ = centerZ + 0.08;
        double maxZ = centerZ + 0.92;
        int color = fluid.getFluid().getColor(fluid);
        GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(true);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        double y0 = floorY, y1 = floorY + fillHeight;
        double u0 = sprite.getMinU(), u1 = sprite.getMaxU(), v0 = sprite.getMinV(), v1 = sprite.getMaxV();
        face(buffer, minX, y0, minZ, maxX, y1, maxZ, u0, v0, u1, v1, EnumFacing.NORTH);
        face(buffer, minX, y0, minZ, maxX, y1, maxZ, u0, v0, u1, v1, EnumFacing.SOUTH);
        face(buffer, minX, y0, minZ, maxX, y1, maxZ, u0, v0, u1, v1, EnumFacing.WEST);
        face(buffer, minX, y0, minZ, maxX, y1, maxZ, u0, v0, u1, v1, EnumFacing.EAST);
        face(buffer, minX, y0, minZ, maxX, y1, maxZ, u0, v0, u1, v1, EnumFacing.UP);
        Tessellator.getInstance().draw();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableLighting();
    }

    private void face(BufferBuilder buffer, double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ, double u0, double v0, double u1, double v1,
            EnumFacing face) {
        switch (face) {
            case NORTH:
                vertex(buffer, minX,minY,minZ,u0,v1); vertex(buffer,minX,maxY,minZ,u0,v0); vertex(buffer,maxX,maxY,minZ,u1,v0); vertex(buffer,maxX,minY,minZ,u1,v1); break;
            case SOUTH:
                vertex(buffer, maxX,minY,maxZ,u0,v1); vertex(buffer,maxX,maxY,maxZ,u0,v0); vertex(buffer,minX,maxY,maxZ,u1,v0); vertex(buffer,minX,minY,maxZ,u1,v1); break;
            case WEST:
                vertex(buffer, minX,minY,maxZ,u0,v1); vertex(buffer,minX,maxY,maxZ,u0,v0); vertex(buffer,minX,maxY,minZ,u1,v0); vertex(buffer,minX,minY,minZ,u1,v1); break;
            case EAST:
                vertex(buffer, maxX,minY,minZ,u0,v1); vertex(buffer,maxX,maxY,minZ,u0,v0); vertex(buffer,maxX,maxY,maxZ,u1,v0); vertex(buffer,maxX,minY,maxZ,u1,v1); break;
            case UP:
                vertex(buffer,minX,maxY,minZ,u0,v0); vertex(buffer,minX,maxY,maxZ,u0,v1); vertex(buffer,maxX,maxY,maxZ,u1,v1); vertex(buffer,maxX,maxY,minZ,u1,v0); break;
            default: break;
        }
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, double u, double v) {
        buffer.pos(x, y, z).tex(u, v).endVertex();
    }

    private void drawStatusLight(AgriculturalReservoirTile tile, EnumFacing facing) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.color(tile.formed ? 0.2F : 1.0F, tile.formed ? 0.9F : 0.18F, 0.12F, 1.0F);
        // The controller texture supplies the gauge; this small emissive face marks formed state.
        double offset = 0.501;
        double px = tile.center.getX() - tile.getPos().getX();
        double py = 0.83;
        double pz = tile.center.getZ() - tile.getPos().getZ();
        double fx = px + facing.getXOffset() * offset;
        double fz = pz + facing.getZOffset() * offset;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        double s = 0.08;
        if (facing.getAxis() == EnumFacing.Axis.Z) {
            buffer.pos(px-s,py-s,fz).endVertex(); buffer.pos(px-s,py+s,fz).endVertex();
            buffer.pos(px+s,py+s,fz).endVertex(); buffer.pos(px+s,py-s,fz).endVertex();
        } else {
            buffer.pos(fx,py-s,pz-s).endVertex(); buffer.pos(fx,py+s,pz-s).endVertex();
            buffer.pos(fx,py+s,pz+s).endVertex(); buffer.pos(fx,py-s,pz+s).endVertex();
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1, 1, 1, 1);
    }
}
