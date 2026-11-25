package br.cropmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Samples the installed IC2 skin directly, including its original info icon. */
final class FarmGuiStyle {
    private static final ResourceLocation PANEL = new ResourceLocation("cropmanager", "textures/gui/equipment_monitor.png");
    private static final ResourceLocation COMMON = new ResourceLocation("ic2", "textures/gui/common.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation("cropmanager", "textures/gui/ic2_widgets.png");
    private static final ResourceLocation INFO = new ResourceLocation("ic2", "textures/gui/infobutton.png");
    private static final ResourceLocation ICONS = new ResourceLocation("cropmanager", "textures/gui/icons.png");
    private static int ink = 0x202020;
    private static boolean lightText;
    private FarmGuiStyle() {}
    static void registerPalette() {
        net.minecraft.client.resources.IReloadableResourceManager resources = (net.minecraft.client.resources.IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager();
        resources.registerReloadListener(manager -> {
            try (net.minecraft.client.resources.IResource resource = manager.getResource(PANEL)) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(resource.getInputStream());
                if (image != null) {
                    // The panel fill is sampled from this exact pixel, including resource packs.
                int rgb = image.getRGB(10, 10);
                    double luminance = 0.2126 * linear(rgb >> 16 & 255) + 0.7152 * linear(rgb >> 8 & 255) + 0.0722 * linear(rgb & 255);
                    lightText = luminance < 0.179;
                    ink = lightText ? 0xf0f0f0 : 0x202020;
                }
            } catch (java.io.IOException exception) {
                ink = 0x202020; lightText = false;
            }
        });
    }
    private static double linear(int value) { double channel = value / 255.0; return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4); }
    static int ink() { return ink; }
    static boolean darkTheme() { return lightText; }
    static int warningInk() { return lightText ? 0xffb08a : 0x922d20; }
    static int goodInk() { return lightText ? 0x9cdeb0 : 0x245d37; }
    static int mutedInk() { return lightText ? 0xbcbcbc : 0x555555; }
    static int buttonInk(boolean enabled,boolean hovered) { return enabled ? ink() : mutedInk(); }
    static int buttonWidth(String... labels) {
        int width=20;
        for(String label:labels)width=Math.max(width,Minecraft.getMinecraft().fontRenderer.getStringWidth(label)+12);
        return Math.min(132,width);
    }
    static void title(FontRenderer font,String text,int x,int y,int maxwidth) {
        font.drawString(font.trimStringToWidth("\u00a7l"+text,maxwidth)+"\u00a7r",x,y,ink());
    }
    private static void piece(int x,int y,int w,int h,int u,int v,int sw,int sh) {
        Gui.drawScaledCustomSizeModalRect(x,y,u,v,sw,sh,w,h,256,256);
    }
    private static void piece512(int x,int y,int w,int h,int u,int v,int sw,int sh) {
        Gui.drawScaledCustomSizeModalRect(x,y,u,v,sw,sh,w,h,512,512);
    }
    static void panel(int x,int y,int w,int h) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(PANEL);
        float tint = 1;
        GlStateManager.color(tint,tint,tint,1);
        piece512(x+4,y+4,w-8,h-8,4,4,1,1);
        piece512(x,y,4,4,0,0,4,4); piece512(x+4,y,w-8,4,4,0,168,4); piece512(x+w-4,y,4,4,318,0,4,4);
        piece512(x,y+4,4,h-8,0,4,4,248); piece512(x+w-4,y+4,4,h-8,318,4,4,248);
        piece512(x,y+h-4,4,4,0,252,4,4); piece512(x+4,y+h-4,w-8,4,4,252,168,4); piece512(x+w-4,y+h-4,4,4,318,252,4,4);
        GlStateManager.color(1,1,1,1);
    }
    static void background(String name,int x,int y,int w,int h) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("cropmanager","textures/gui/"+name+".png"));
        GlStateManager.color(1,1,1,1);
        Gui.drawModalRectWithCustomSizedTexture(x,y,0,0,w,h,512,512);
    }
    static void slot(int x,int y) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(PANEL);
        float tint = 1; GlStateManager.color(tint,tint,tint,1);
        piece512(x-1,y-1,18,18,7,172,18,18); GlStateManager.color(1,1,1,1);
    }
    static void info(int x,int y) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(INFO); GlStateManager.color(1,1,1,1);
        piece(x,y,10,10,0,0,10,10);
    }
    /** Draws a 16x16 icon from cropmanager:textures/gui/icons.png (64x64 grid). */
    static void icon(int x, int y, int col, int row) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONS);
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawScaledCustomSizeModalRect(x, y, col * 16, row * 16, 16, 16, 16, 16, 64, 64);
    }
    /** Draws a flat black digital display without bevel/relief. */
    static void digitalScreen(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + h, 0xff000000);
    }
    /** Draws the IC2 downward lightning bolt (7x13) from ic2_widgets.png filled proportionally with red EU. */
    static void bolt(int x, int y, double ratio) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS);
        GlStateManager.color(1, 1, 1, 1);
        piece(x, y, 7, 13, 111, 24, 7, 13);
        int filled = (int) Math.round(Math.max(0, Math.min(1, ratio)) * 13);
        if (filled > 0) {
            int offset = 13 - filled;
            piece(x, y + offset, 7, filled, 110, 7 + offset, 7, filled);
        }
    }
    /** Draws the IC2 red or yellow warning triangle from ic2_widgets.png scaled cleanly to 16x14. */
    static void warningTriangle(int x, int y, boolean critical) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS);
        GlStateManager.color(1, 1, 1, 1);
        if (critical) piece(x, y, 16, 14, 161, 8, 30, 26);
        else piece(x, y, 16, 14, 162, 37, 30, 26);
    }
    /** Draws the IC2 recycle arrows (18x17) from ic2_widgets.png (gray idle or blue active). */
    static void recycle(int x, int y, boolean active) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS);
        GlStateManager.color(1, 1, 1, 1);
        if (active) piece(x, y, 18, 17, 135, 30, 18, 17);
        else piece(x, y, 18, 17, 135, 9, 18, 17);
    }
    /** Draws the IC2 green gauge tick marks over a fluid tank. */
    static void tankScale(int x, int y, int w, int h) {
        int scaleH = Math.min(37, h - 6);
        if (scaleH <= 4) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(COMMON);
        GlStateManager.color(1, 1, 1, 1);
        piece(x + Math.max(2, w - 10), y + (h - scaleH) / 2, 8, scaleH, 46, 109, 8, scaleH);
    }
    static void button(int x,int y,int w,int h,boolean enabled,boolean selected,boolean hovered) {
        int border=lightText?0xff777777:0xff6c6c6c;
        int fill=lightText?0xff444444:0xffb5b5b5;
        int edge=lightText?0xff626262:0xffdedede;
        if(!enabled){fill=lightText?0xff373737:0xffc1c1c1;edge=lightText?0xff454545:0xffcccccc;}
        else if(hovered){fill=lightText?0xff565656:0xffcecece;edge=lightText?0xff898989:0xffefefef;}
        if(selected){border=0xff000000|goodInk();if(enabled)fill=lightText?0xff3b5245:0xffb5c7ba;}
        Gui.drawRect(x,y,x+w,y+h,border);
        Gui.drawRect(x+1,y+1,x+w-1,y+h-1,fill);
        Gui.drawRect(x+1,y+1,x+w-1,y+2,edge);
        if(selected)Gui.drawRect(x+2,y+h-3,x+w-2,y+h-1,0xff000000|goodInk());
        GlStateManager.color(1,1,1,1);
    }
}
