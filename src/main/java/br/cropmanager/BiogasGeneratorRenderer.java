package br.cropmanager;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the 3x3x3 IC2 Biogas Generator OBJ mesh and biogas sight-glass level. */
@SideOnly(Side.CLIENT)
public final class BiogasGeneratorRenderer extends TileEntitySpecialRenderer<BiogasGeneratorTile> {
    private static final int BIOGAS_RGB = 0x68b85c;
    private final MultiblockObjModel model = new MultiblockObjModel("models/block/biogas_generator", 0.72, 2.28);

    @Override
    public void render(BiogasGeneratorTile tile, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha) {
        BlockPos center = tile.center();
        if (!tile.isFormed() || tile.getWorld() == null || center == null) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (state.getBlock() != CropManager.BIOGAS_GENERATOR) return;
        EnumFacing facing = state.getValue(AgriculturalMachineBlock.FACING);
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        try {
            GlStateManager.translate(
                    center.getX() - tile.getPos().getX() + 0.5,
                    tile.baseY() - tile.getPos().getY(),
                    center.getZ() - tile.getPos().getZ() + 0.5);
            GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
            GlStateManager.translate(-0.5, 0, -0.5);

            FluidStack fluid = tile.gas.getFluid();
            double fraction = fluid != null && tile.gas.getCapacity() > 0
                    ? Math.min(1.0, (double) fluid.amount / tile.gas.getCapacity())
                    : 0.0;
            model.render(fraction, BIOGAS_RGB);
        } finally {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        }
    }
}
