package br.cropmanager;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the 3x5x3 Industrial Fluid Reservoir OBJ mesh and single-column sight-glass fluid level. */
@SideOnly(Side.CLIENT)
public final class IndustrialTankRenderer extends TileEntitySpecialRenderer<IndustrialTankTile> {
    private final MultiblockObjModel model = new MultiblockObjModel("models/block/fluid_tank", 1.1875, 4.000);

    @Override
    public void render(IndustrialTankTile tile, double x, double y, double z,
            float partialTicks, int destroyStage, float alpha) {
        if (!tile.formed || tile.getWorld() == null || tile.center == null) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (state.getBlock() != CropManager.FLUID_TANK) return;
        EnumFacing facing = state.getValue(IndustrialTankBlock.FACING);
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

            FluidStack fluid = tile.tank.getFluid();
            double fraction = fluid != null && tile.tank.getCapacity() > 0
                    ? Math.min(1.0, (double) fluid.amount / tile.tank.getCapacity())
                    : 0.0;
            int rgb = fluidColor(fluid);
            model.render(fraction, rgb);
        } finally {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        }
    }

    private static int fluidColor(FluidStack fluid) {
        if (fluid == null) return 0x438cbd;
        if (fluid.getFluid() == FluidRegistry.WATER) return 0x438cbd;
        if (fluid.getFluid() == ic2.core.ref.FluidName.weed_ex.getInstance()) return 0xb9a247;
        if (fluid.getFluid() == CropManager.GRIN_EXTRACT) return 0x83aa55;
        int rgb = fluid.getFluid().getColor(fluid);
        return (rgb & 0xffffff) == 0xffffff ? 0x438cbd : rgb;
    }
}
