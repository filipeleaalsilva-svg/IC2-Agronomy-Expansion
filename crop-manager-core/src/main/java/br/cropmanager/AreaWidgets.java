package br.cropmanager;

import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

final class AreaWidgets {
    private AreaWidgets(){}
    static void add(List<GuiButton> buttons,int x,int y){
        buttons.add(new FarmButton(210,x+48,y+16,14,14,"-"));buttons.add(new FarmButton(211,x+88,y+16,14,14,"+"));
        int w=Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("layer_below"),ManagerGui.tr("layer_level"),ManagerGui.tr("layer_above")));
        for(int i=0;i<3;i++)buttons.add(new FarmButton(212+i,x,y+40+i*18,w,14,""));
        buttons.add(new FarmButton(11,x,y+98,Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("hide_area"),ManagerGui.tr("show_area"))),14,""));
        buttons.add(new FarmButton(215,x,y+116,Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("area_default"))),14,ManagerGui.tr("area_default")));
    }
    static void refresh(List<GuiButton> buttons,FarmTile tile,int radius,int layers,int maximum){
        for(GuiButton b:buttons){
            if(b.id==210)b.enabled=radius>1;if(b.id==211)b.enabled=radius<maximum;
            if(b.id>=212 && b.id<=214){int bit=1<<(b.id-212);b.displayString=ManagerGui.tr(new String[]{"layer_below","layer_level","layer_above"}[b.id-212]);b.enabled=(layers&bit)==0 || Integer.bitCount(layers)>1;if(b instanceof FarmButton)((FarmButton)b).selected=(layers&bit)!=0;}
            if(b.id==11){b.displayString=ManagerGui.tr(AreaPreview.selected(tile)?"hide_area":"show_area");if(b instanceof FarmButton)((FarmButton)b).selected=AreaPreview.selected(tile);}
        }
    }
    static void draw(FontRenderer font,int x,int y,int radius,int layers){
        int color=ManagerGui.ink(),side=radius*2+1; FarmGuiStyle.title(font,side+"x"+side+"x"+Integer.bitCount(layers),x,y,104);
        font.drawString(ManagerGui.tr("radius"),x,y+18,color);font.drawString(Integer.toString(radius),x+71,y+18,color);
    }
}
