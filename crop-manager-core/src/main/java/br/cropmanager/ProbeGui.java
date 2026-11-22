package br.cropmanager;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;

public final class ProbeGui extends GuiScreen {
    private final NBTTagCompound data;
    public ProbeGui(NBTTagCompound data) { this.data = data; }
    @Override public void initGui() {
        buttonList.clear();int x=width/2-112,y=height/2-108;
        buttonList.add(new FarmButton(12,x+206,y+7,10,10,"i"));
    }
    @Override protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {}
    @Override public boolean doesGuiPauseGame() { return false; }
    private void field(String label,String value,int x,int y) {
        fontRenderer.drawString(fontRenderer.trimStringToWidth(label,148),x,y,FarmGuiStyle.ink());
        fontRenderer.drawString(value,x+200-fontRenderer.getStringWidth(value),y,FarmGuiStyle.ink());
    }
    @Override public void drawScreen(int mouseX,int mouseY,float ticks) {
        drawDefaultBackground();int x=width/2-112,y=height/2-108;
        FarmGuiStyle.panel(x,y,224,216);
        FarmGuiStyle.title(fontRenderer,ManagerGui.tr("probe_name"),x+8,y+8,188);
        NBTTagCompound row=new NBTTagCompound();String species=data.getString("Species");int colon=species.indexOf(':');
        if(colon>0){row.setString("Owner",species.substring(0,colon));row.setString("Id",species.substring(colon+1));}
        fontRenderer.drawString(fontRenderer.trimStringToWidth(CropSurveyWidgets.name(row),200),x+8,y+28,FarmGuiStyle.ink());
        field(ManagerGui.tr("maturity"),data.getInteger("Size")+" / "+data.getInteger("MaxSize"),x+8,y+44);
        field(ManagerGui.tr("water"),UiNumbers.compact(data.getInteger("Water")),x+8,y+58);
        field(ManagerGui.tr("nutrients"),UiNumbers.compact(data.getInteger("Nutrients")),x+8,y+72);
        field("Weed-Ex",UiNumbers.compact(data.getInteger("Weed")),x+8,y+86);
        FarmGuiStyle.title(fontRenderer,ManagerGui.tr("survey_environment"),x+8,y+108,200);
        String[] labels={"terrain_nutrients","humidity","air","light"};String[] keys={"TerrainNutrients","Humidity","Air","Light"};
        for(int i=0;i<4;i++)field(ManagerGui.tr(labels[i]),UiNumbers.compact(data.getInteger(keys[i])),x+8,y+124+i*14);
        int[] stats=data.getIntArray("Stats");
        if(stats.length==3)fontRenderer.drawString("Gr "+stats[0]+"   Ga "+stats[1]+"   Re "+stats[2],x+8,y+188,FarmGuiStyle.mutedInk());
        else fontRenderer.drawString(ManagerGui.tr("unanalyzed"),x+8,y+188,FarmGuiStyle.mutedInk());
        super.drawScreen(mouseX,mouseY,ticks);
        for(net.minecraft.client.gui.GuiButton button:buttonList)if(button.isMouseOver())drawHoveringText(java.util.Arrays.asList(ManagerGui.tr("compatible_upgrades"),ManagerGui.tr("no_upgrades")),mouseX,mouseY);
    }
}
