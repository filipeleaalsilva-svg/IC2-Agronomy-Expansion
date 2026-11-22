package br.cropmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.item.IC2Items;
import ic2.core.item.ItemCropSeed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Local presentation of completed surveys; no inventory slots or server actions. */
final class CropSurveyWidgets {
    private int page;
    private String selectedOwner="",selectedId="";
    private boolean selected;
    private NBTTagList species=new NBTTagList();
    private static final String[] METRICS={"terrain_nutrients","humidity","air","light"};
    void add(List<GuiButton> buttons,int x,int y,int width) {
        buttons.add(new FarmButton(300,x,y+116,14,14,"<"));
        buttons.add(new FarmButton(301,x+width-14,y+116,14,14,">"));
        for(int i=0;i<4;i++)buttons.add(new FarmButton(302+i,x+width-14,y+17+i*24,14,14,">"));
        buttons.add(new FarmButton(306,x,y+116,Math.min(width,86),14,ManagerGui.tr("survey_all")));
    }
    void refresh(List<GuiButton> buttons,NBTTagCompound state,boolean visible,boolean environment) {
        species=survey(state).getTagList("Species",10);
        page=Math.max(0,Math.min(page,Math.max(0,(species.tagCount()-1)/4)));
        for(GuiButton button:buttons) {
            if(button.id<300 || button.id>306)continue;
            button.visible=visible && species.tagCount()>0 && (environment?button.id==306 && selected:button.id!=306);
            if(button.id==300 || button.id==301)button.visible &= species.tagCount()>4;
            if(button.id==300)button.enabled=page>0;
            if(button.id==301)button.enabled=(page+1)*4<species.tagCount();
            if(button.id>=302 && button.id<=305)button.visible &= page*4+button.id-302<species.tagCount();
            if(button.id==306)button.enabled=selected;
        }
    }
    boolean action(int id) {
        if(id==300)page=Math.max(0,page-1);
        else if(id==301)page=Math.min(Math.max(0,(species.tagCount()-1)/4),page+1);
        else if(id>=302 && id<=305) {
            int index=page*4+id-302;if(index>=species.tagCount())return true;
            NBTTagCompound row=species.getCompoundTagAt(index);
            selectedOwner=row.getString("Owner");selectedId=row.getString("Id");selected=true;
        } else if(id==306)selected=false;
        else return false;
        return true;
    }
    private static NBTTagCompound survey(NBTTagCompound state) {return state.getCompoundTag("Report").getCompoundTag("Survey");}
    private NBTTagCompound selection(NBTTagCompound data) {
        if(selected)for(int i=0;i<species.tagCount();i++) {
            NBTTagCompound row=species.getCompoundTagAt(i);
            if(row.getString("Owner").equals(selectedOwner) && row.getString("Id").equals(selectedId))return row;
        }
        return data;
    }
    private static CropCard card(NBTTagCompound row) {
        return row.getString("Id").isEmpty() || Crops.instance==null?null:Crops.instance.getCropCard(row.getString("Owner"),row.getString("Id"));
    }
    static String name(NBTTagCompound row) {
        if(row.getString("Id").isEmpty())return ManagerGui.tr("unanalyzed");
        CropCard card=card(row);
        return card==null?row.getString("Owner")+":"+row.getString("Id"):I18n.format(card.getUnlocalizedName());
    }
    private static ItemStack icon(NBTTagCompound row) {
        CropCard card=card(row);
        return card==null?IC2Items.getItem("crop_seed_bag"):ItemCropSeed.generateItemStackFromValues(card,0,0,0,1);
    }
    void draw(FontRenderer font,NBTTagCompound state,int x,int y,int width,boolean environment) {
        NBTTagCompound report=state.getCompoundTag("Report"),data=survey(state);
        if(!report.hasKey("Survey")) {font.drawSplitString(ManagerGui.tr("survey_waiting"),x,y,width,FarmGuiStyle.mutedInk());return;}
        if(data.getInteger("Count")==0) {font.drawSplitString(ManagerGui.tr("survey_empty"),x,y,width,FarmGuiStyle.mutedInk());return;}
        if(environment)drawEnvironment(font,selection(data),x,y,width);
        else {
            FarmGuiStyle.title(font,ManagerGui.tr("survey_cultures"),x,y,width);
            for(int i=0;i<4 && page*4+i<species.tagCount();i++) {
                NBTTagCompound row=species.getCompoundTagAt(page*4+i);int rowY=y+16+i*24;
                RenderHelper.enableGUIStandardItemLighting();
                Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(icon(row),x,rowY);
                RenderHelper.disableStandardItemLighting();
                font.drawString(font.trimStringToWidth(name(row),width-40),x+22,rowY,FarmGuiStyle.ink());
                font.drawString("x "+UiNumbers.compact(row.getInteger("Count")),x+22,rowY+10,FarmGuiStyle.mutedInk());
            }
            if(species.tagCount()>4) {
                String pages=(page+1)+" / "+((species.tagCount()+3)/4);
                font.drawString(pages,x+(width-font.getStringWidth(pages))/2,y+119,FarmGuiStyle.mutedInk());
            }
        }
        int[] counts=report.getIntArray("Counts");
        if(counts.length>5 && counts[5]>0)font.drawString(font.trimStringToWidth(ManagerGui.tr("survey_partial"),width),x,y+138,FarmGuiStyle.warningInk());
    }
    private void drawEnvironment(FontRenderer font,NBTTagCompound data,int x,int y,int width) {
        FarmGuiStyle.title(font,selected && data.hasKey("Owner")?name(data):ManagerGui.tr("survey_area"),x,y,width);
        int[] sum=data.getIntArray("Sum"),min=data.getIntArray("Min"),max=data.getIntArray("Max");int count=data.getInteger("Count");
        font.drawString(ManagerGui.tr("survey_range_head"),x,y+16,FarmGuiStyle.mutedInk());
        for(int i=0;i<4;i++) {
            int rowY=y+32+i*20;
            font.drawString(font.trimStringToWidth(ManagerGui.tr(METRICS[i]),width),x,rowY,FarmGuiStyle.ink());
            String values=UiNumbers.compact(i<min.length?min[i]:0)+" / "+UiNumbers.compact(count>0 && i<sum.length?sum[i]/(double)count:0)+" / "+UiNumbers.compact(i<max.length?max[i]:0);
            font.drawString(font.trimStringToWidth(values,width),x,rowY+10,FarmGuiStyle.mutedInk());
        }
    }
    List<String> tooltip(int mouseX,int mouseY,NBTTagCompound state,int x,int y,int width,boolean environment) {
        if(mouseX<x || mouseX>=x+width)return Collections.emptyList();
        if(environment) {
            if(mouseY<y+16 || mouseY>y+112)return Collections.emptyList();
            return Collections.singletonList(ManagerGui.tr("survey_environment_help"));
        }
        int row=(mouseY-y-16)/24;
        if(mouseY<y+16 || row<0 || row>=4 || page*4+row>=species.tagCount())return Collections.emptyList();
        NBTTagCompound data=species.getCompoundTagAt(page*4+row);
        List<String> lines=new ArrayList<>();lines.add(name(data));
        lines.add(ManagerGui.tr("survey_planted")+": "+UiNumbers.exact(data.getInteger("Count")));
        lines.add(ManagerGui.tr("survey_select_help"));return lines;
    }
}
