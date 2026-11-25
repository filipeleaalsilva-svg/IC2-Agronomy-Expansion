package br.cropmanager.chest.client;

import br.cropmanager.chest.block.BlockCropDatabaseChest;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Two textured boxes preserve the original six faces, with a vanilla-style hinge. */
public final class CropChestRenderer extends TileEntitySpecialRenderer<TileCropDatabaseChest> {
    private static final String[] FACES = {"front", "back", "left", "right", "top", "bottom"};
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[6];
    static {
        TEXTURES[0] = new ResourceLocation("cropmanager", "textures/blocks/crop_database_chest_front.png");
        TEXTURES[1] = new ResourceLocation("cropmanager", "textures/blocks/crop_database_chest_back.png");
        TEXTURES[2] = new ResourceLocation("cropmanager", "textures/blocks/crop_database_chest_left.png");
        TEXTURES[3] = new ResourceLocation("cropmanager", "textures/blocks/crop_database_chest_right.png");
        TEXTURES[4] = new ResourceLocation("cropmanager", "textures/blocks/crop_database_chest_top.png");
        TEXTURES[5] = new ResourceLocation("cropmanager", "textures/blocks/crop_database_chest_bottom.png");
    }
    @Override public void render(TileCropDatabaseChest tile, double x, double y, double z,
                                 float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        if (tile.hasWorld()) {
            IBlockState state = tile.getWorld().getBlockState(tile.getPos());
            if (state.getBlock() instanceof BlockCropDatabaseChest) {
                float facing = state.getValue(BlockCropDatabaseChest.FACING).getHorizontalAngle();
                GlStateManager.rotate(180 - facing, 0, 1, 0);
            }
        }
        GlStateManager.translate(-0.5, 0, -0.5);
        GlStateManager.color(1, 1, 1, alpha);
        GlStateManager.enableRescaleNormal();
        box(0, 0.75, destroyStage, TEXTURES);
        float angle = tile.previousLidAngle + (tile.lidAngle - tile.previousLidAngle) * partialTicks;
        angle = 1 - (1 - angle) * (1 - angle) * (1 - angle);
        GlStateManager.translate(0, 0.75, 1);
        GlStateManager.rotate(angle * 90, 1, 0, 0);
        GlStateManager.translate(0, -0.75, -1);
        box(0.75, 1, destroyStage, TEXTURES);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }
    private void box(double low, double high, int destroyStage, ResourceLocation[] textures) {
        double bottomV = 1 - low, topV = 1 - high;
        face(0, textures, destroyStage, new double[][] {{0,low,0,0,bottomV}, {0,high,0,0,topV}, {1,high,0,1,topV}, {1,low,0,1,bottomV}}, 0,0,-1);
        face(1, textures, destroyStage, new double[][] {{1,low,1,0,bottomV}, {1,high,1,0,topV}, {0,high,1,1,topV}, {0,low,1,1,bottomV}}, 0,0,1);
        face(2, textures, destroyStage, new double[][] {{0,low,1,0,bottomV}, {0,high,1,0,topV}, {0,high,0,1,topV}, {0,low,0,1,bottomV}}, -1,0,0);
        face(3, textures, destroyStage, new double[][] {{1,low,0,0,bottomV}, {1,high,0,0,topV}, {1,high,1,1,topV}, {1,low,1,1,bottomV}}, 1,0,0);
        face(4, textures, destroyStage, new double[][] {{0,high,0,0,0}, {0,high,1,0,1}, {1,high,1,1,1}, {1,high,0,1,0}}, 0,1,0);
        face(5, textures, destroyStage, new double[][] {{0,low,1,0,1}, {0,low,0,0,0}, {1,low,0,1,0}, {1,low,1,1,1}}, 0,-1,0);
    }
    private void face(int face, ResourceLocation[] textures, int destroyStage, double[][] vertices, float nx, float ny, float nz) {
        bindTexture(destroyStage >= 0 ? DESTROY_STAGES[destroyStage] : textures[face]);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
        for (double[] v : vertices) buffer.pos(v[0], v[1], v[2]).tex(v[3], v[4]).normal(nx, ny, nz).endVertex();
        Tessellator.getInstance().draw();
    }
}
