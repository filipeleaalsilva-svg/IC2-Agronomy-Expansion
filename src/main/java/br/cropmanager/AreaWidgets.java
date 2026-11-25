package br.cropmanager;

import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

final class AreaWidgets {
    private AreaWidgets(){}
    static void add(List<GuiButton> buttons,int x,int y){
        add(buttons,x,y,false);
    }
    static void add(List<GuiButton> buttons,int x,int y,boolean withOffsets){
        buttons.add(new FarmButton(210,x+48,y+16,14,14,"-"));buttons.add(new FarmButton(211,x+88,y+16,14,14,"+"));
        int w=Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("layer_below"),ManagerGui.tr("layer_level"),ManagerGui.tr("layer_above")));
        int step=withOffsets?15:18;
        int layerTop=withOffsets?y+34:y+40;
        for(int i=0;i<3;i++)buttons.add(new FarmButton(212+i,x,layerTop+i*step,w,14,""));
        if(withOffsets){
            for(int axis=0;axis<3;axis++){
                int rowY=y+82+axis*16;
                buttons.add(new FarmButton(240+axis*2,x+48,rowY,14,14,"-"));
                buttons.add(new FarmButton(241+axis*2,x+88,rowY,14,14,"+"));
            }
            buttons.add(new FarmButton(11,x,y+132,Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("hide_area"),ManagerGui.tr("show_area"))),14,""));
            buttons.add(new FarmButton(215,x,y+148,Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("area_default"))),14,ManagerGui.tr("area_default")));
        }else{
            buttons.add(new FarmButton(11,x,y+98,Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("hide_area"),ManagerGui.tr("show_area"))),14,""));
            buttons.add(new FarmButton(215,x,y+116,Math.min(102,FarmGuiStyle.buttonWidth(ManagerGui.tr("area_default"))),14,ManagerGui.tr("area_default")));
        }
    }
    static void refresh(List<GuiButton> buttons,FarmTile tile,int radius,int layers,int maximum){
        for(GuiButton b:buttons){
            if(b.id==210)b.enabled=radius>1;if(b.id==211)b.enabled=radius<maximum;
            if(b.id>=212 && b.id<=214){int bit=1<<(b.id-212);b.displayString=ManagerGui.tr(new String[]{"layer_below","layer_level","layer_above"}[b.id-212]);b.enabled=(layers&bit)==0 || Integer.bitCount(layers)>1;if(b instanceof FarmButton)((FarmButton)b).selected=(layers&bit)!=0;}
            if(b.id==11){b.displayString=ManagerGui.tr(AreaPreview.selected(tile)?"hide_area":"show_area");if(b instanceof FarmButton)((FarmButton)b).selected=AreaPreview.selected(tile);}
        }
        if(tile instanceof ManagerTile){
            ManagerTile m=(ManagerTile)tile;
            for(GuiButton b:buttons){
                if(b.id==240)b.enabled=m.offsetX>-ScanLogic.MAX_HORIZONTAL_OFFSET;
                if(b.id==241)b.enabled=m.offsetX<ScanLogic.MAX_HORIZONTAL_OFFSET;
                if(b.id==242)b.enabled=m.offsetY>-ScanLogic.MAX_VERTICAL_OFFSET;
                if(b.id==243)b.enabled=m.offsetY<ScanLogic.MAX_VERTICAL_OFFSET;
                if(b.id==244)b.enabled=m.offsetZ>-ScanLogic.MAX_HORIZONTAL_OFFSET;
                if(b.id==245)b.enabled=m.offsetZ<ScanLogic.MAX_HORIZONTAL_OFFSET;
            }
        }
    }
    static void draw(FontRenderer font,int x,int y,int radius,int layers){
        int color=ManagerGui.ink(),side=radius*2+1; FarmGuiStyle.title(font,side+"x"+side+"x"+Integer.bitCount(layers),x,y,104);
        font.drawString(ManagerGui.tr("radius"),x,y+18,color);font.drawString(Integer.toString(radius),x+71,y+18,color);
    }
    static void draw(FontRenderer font,int x,int y,ManagerTile tile){
        draw(font,x,y,tile.radius,tile.layers);
        int color=ManagerGui.ink();
        drawOffsetRow(font,x,y+84,"Off X",tile.offsetX,color);
        drawOffsetRow(font,x,y+100,"Off Y",tile.offsetY,color);
        drawOffsetRow(font,x,y+116,"Off Z",tile.offsetZ,color);
    }
    private static void drawOffsetRow(FontRenderer font,int x,int y,String label,int val,int color){
        font.drawString(label,x,y,color);
        String text=(val>0?"+":"")+val;
        font.drawString(text,x+75-font.getStringWidth(text)/2,y,color);
    }
}
