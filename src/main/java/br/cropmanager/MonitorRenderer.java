package br.cropmanager;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public final class MonitorRenderer extends TileEntitySpecialRenderer<MonitorTile> {
    @Override public void render(MonitorTile tile, double x, double y, double z, float ticks, int destroy, float alpha) {
        if (tile.getWorld() == null) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof ManagerBlock)) return;
        EnumFacing facing = state.getValue(ManagerBlock.FACING);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
        GlStateManager.translate(0, 0, 0.502);
        GlStateManager.scale(0.0042, -0.0042, 0.0042);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        try {
            String status = net.minecraft.client.resources.I18n.format("cropmanager." + (tile.alarmActive ? "alarm_active" : "monitor_" + tile.connection));
            getFontRenderer().drawString(getFontRenderer().trimStringToWidth(status, 104), -52, -27, tile.alarmActive ? 0xff6258 : 0xb1e7d7);
            if ("connected".equals(tile.connection)) {
                NBTTagCompound s = tile.snapshot;
                if("weeder".equals(s.getString("Target"))){line("Gas "+s.getInteger("Gas")+"/16000",-8,0xffddaa);line("Weeds: "+s.getInteger("Removed"),8,0xb1e7d7);}
                else { line("EU "+(int)s.getDouble("EU")+"/"+(int)s.getDouble("Capacity"), -8, 0xffdddd);
                line("H2O "+s.getInteger("Water")+"/"+s.getInteger("TankCapacity"), 8, 0x9ed9ff);
                line("EX "+s.getInteger("Weed")+"/"+(s.hasKey("WeedCapacity")?s.getInteger("WeedCapacity"):s.getInteger("TankCapacity")), 24, 0xffe29b); }
            }
        } finally { GlStateManager.color(1, 1, 1, 1); GlStateManager.depthMask(true); GlStateManager.enableCull(); GlStateManager.enableLighting(); GlStateManager.popMatrix(); OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY); }
    }
    private void line(String text,int y,int color) { getFontRenderer().drawString(getFontRenderer().trimStringToWidth(text,104),-52,y,color); }
}
