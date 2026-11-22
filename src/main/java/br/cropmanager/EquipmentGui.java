package br.cropmanager;

import java.io.IOException;
import java.util.Arrays;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.ItemStack;

public final class EquipmentGui extends GuiContainer {
    private final FarmTile machine;
    private boolean sidebarExpanded = true;
    private int tab;
    private int harvestPage;
    private final CropSurveyWidgets survey = new CropSurveyWidgets();
    private boolean weeder() { return machine instanceof WeederTile; }
    private boolean side() { return monitor() || weeder(); }
    private boolean monitor() { return machine instanceof MonitorTile; }
    public EquipmentGui(InventoryPlayer player, FarmTile tile) { super(new EquipmentContainer(player, tile)); machine = tile; xSize = 176; ySize = tile instanceof MonitorTile ? 264 : 208; }
    @Override public void initGui() {
        xSize = side() && sidebarExpanded ? 322 : 176; buttonList.clear(); super.initGui();
        guiLeft = (width - xSize) / 2;
        button(12, xSize - 18, 5, 10, 10, "i");
        if (side()) button(14,140,5,14,12,sidebarExpanded ? "<" : ">");
        if (monitor()) {
            if (sidebarExpanded) {
                for(int i=0;i<5;i++)button(30+i,181+i*27,23,25,14,ManagerGui.tr(new String[]{"cultures_tab","environment_tab","control_tab","automation_tab","items_tab"}[i]));
                survey.add(buttonList,guiLeft+182,guiTop+46,132);
                if(tab==2){
                    buttonList.add(new IconButton(200,guiLeft+182,guiTop+46));
                    buttonList.add(new IconButton(13,guiLeft+202,guiTop+46));
                    buttonList.add(new IconButton(201,guiLeft+222,guiTop+46));
                    button(146,182,70,FarmGuiStyle.buttonWidth(ManagerGui.tr("output_mode_0"),ManagerGui.tr("output_mode_1"),ManagerGui.tr("output_mode_2")),14,"");
                    button(147,182,90,64,14,ManagerGui.tr("test_signal"));button(148,250,90,64,14,ManagerGui.tr("unlink"));
                }
                if(tab==3){
                    button(220,182,46,FarmGuiStyle.buttonWidth(ManagerGui.tr("rule_mode_0"),ManagerGui.tr("rule_mode_1"),ManagerGui.tr("rule_mode_2")),14,"");
                    button(221,230,72,12,12,"-");button(222,302,72,12,12,"+");
                    button(223,230,102,12,12,"-");button(224,302,102,12,12,"+");
                    button(226,230,132,12,12,"-");button(227,302,132,12,12,"+");
                    button(228,230,162,12,12,"-");button(229,302,162,12,12,"+");
                    button(225,182,184,FarmGuiStyle.buttonWidth(ManagerGui.tr("all_tanks_empty")),14,ManagerGui.tr("all_tanks_empty"));
                    button(230,182,206,132,14,"");
                }
                if(tab==4){button(235,182,206,18,14,"<");button(236,296,206,18,14,">");}
            }
        }
        if(weeder()){
            buttonList.add(new IconButton(200,guiLeft+8,guiTop+98));
            buttonList.add(new IconButton(13,guiLeft+28,guiTop+98));
            if(sidebarExpanded)AreaWidgets.add(buttonList,guiLeft+182,guiTop+30);
        }
    }
    private void button(int id,int x,int y,int w,int h,String label) { buttonList.add(new FarmButton(id,guiLeft+x,guiTop+y,("+".equals(label)||"-".equals(label))?14:w,id==12?10:14,label)); }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 14) { sidebarExpanded = !sidebarExpanded; initGui(); return; }
        if(survey.action(b.id)){if(b.id>=302 && b.id<=305){tab=1;initGui();}return;}
        if (b.id >= 30 && b.id <= 34) { tab = b.id - 30; initGui(); return; }
        if (b.id == 235) { harvestPage = Math.max(0, harvestPage - 1); return; }
        if (b.id == 236) { harvestPage++; return; }
        if (b.id == 11 && weeder()) { AreaPreview.toggle(machine); return; }
        if (b.id != 12) ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(machine, inventorySlots.windowId, b.id));
    }
    @Override public void drawScreen(int x,int y,float ticks) {
        drawDefaultBackground();
        for (GuiButton b : buttonList) {
            if (monitor()) {
                MonitorTile m=(MonitorTile)machine;
                boolean manager="manager".equals(m.snapshot.getString("Target"));
                boolean switchable=manager || "weeder".equals(m.snapshot.getString("Target"));
                if(b.id==200){b.enabled=switchable;}
                if(b.id==201){b.enabled=manager;}
                if(b.id==13){b.enabled=switchable;}
                if(b.id>=220 && b.id<=230)b.enabled=manager;
                if(b.id==235 || b.id==236){NBTTagList items=m.snapshot.getTagList("HarvestItems",10);int pages=Math.max(1,(items.tagCount()+5)/6);harvestPage=Math.min(harvestPage,pages-1);b.enabled=b.id==235?harvestPage>0:harvestPage+1<pages;}
                if(b.id==220)b.displayString=ManagerGui.tr("rule_mode_"+m.snapshot.getCompoundTag("Automation").getInteger("Mode"));
                if(b.id==146)b.displayString=ManagerGui.tr("output_mode_"+m.outputMode);
                if(b.id==230)b.displayString=ManagerGui.tr(m.snapshot.getCompoundTag("Automation").getBoolean("OutputFull")?"rule_output_full_on":"rule_output_full_off");
            }
        }
        if(weeder()){
            WeederTile w=(WeederTile)machine;AreaWidgets.refresh(buttonList,w,w.radius,w.layers,w.serverMaxRadius);
        }
        for(GuiButton b:buttonList)if(b instanceof FarmButton){
            FarmButton f=(FarmButton)b;
            if(monitor()){
                MonitorTile m=(MonitorTile)machine;
                if(b.id>=30 && b.id<=34)f.selected=b.id-30==tab;
            }
        }
        if(monitor())survey.refresh(buttonList,((MonitorTile)machine).snapshot,sidebarExpanded && tab<2,tab==1);
        super.drawScreen(x,y,ticks); renderHoveredToolTip(x,y);
        if(monitor()){
            MonitorTile m=(MonitorTile)machine;NBTTagCompound st=m.snapshot;
            if(sidebarExpanded && tab<2){java.util.List<String> tips=survey.tooltip(x-guiLeft,y-guiTop,st,182,46,132,tab==1);if(!tips.isEmpty())drawHoveringText(tips,x,y);}
            if(isPointInRegion(8,6,160,24,x,y))drawHoveringText(ManagerGui.tr("target_"+st.getString("Target"))+" | "+m.targetCoordinates()+" | "+ManagerGui.tr("range")+": "+m.connectionRange(),x,y);
            if(isPointInRegion(52,40,14,17,x,y))drawHoveringText(UiNumbers.exactResource(st.getDouble("EU"),st.getDouble("Capacity"),"EU"),x,y);
            if(isPointInRegion(27,130,62,16,x,y))drawHoveringText(m.wirelessEuUpgrades()>0?Arrays.asList(ManagerGui.tr("wireless_eu_buffer")+": "+UiNumbers.exactResource(m.energy.getEnergyStored(),m.wirelessEuCapacity(),"EU"),"T"+m.energy.getSinkTier()+" · "+m.wirelessEuRate()+" EU/t"):Arrays.asList(ManagerGui.tr("wireless_eu_buffer")+": 0 / 0 EU",ManagerGui.tr("wireless_eu_help")),x,y);
            if(isPointInRegion(8,42,16,36,x,y))drawHoveringText(Arrays.asList(UiNumbers.exactResource(st.hasKey("Water")?st.getInteger("Water"):m.water.getFluidAmount(),st.hasKey("TankCapacity")?st.getInteger("TankCapacity"):m.water.getCapacity(),"mB"),ManagerGui.tr("monitor_buffer")+": "+UiNumbers.exactResource(m.water.getFluidAmount(),m.water.getCapacity(),"mB")),x,y);
            if(isPointInRegion(30,42,16,36,x,y))drawHoveringText(Arrays.asList(UiNumbers.exactResource(st.hasKey("Weed")?st.getInteger("Weed"):m.weed.getFluidAmount(),st.hasKey("WeedCapacity")?st.getInteger("WeedCapacity"):m.weed.getCapacity(),"mB"),ManagerGui.tr("monitor_buffer")+": "+UiNumbers.exactResource(m.weed.getFluidAmount(),m.weed.getCapacity(),"mB")),x,y);
            if("connected".equals(m.connection) && "manager".equals(st.getString("Target"))){
                NBTTagList mSlots=st.getTagList("ManagerSlots",10);
                for(int i=0;i<mSlots.tagCount() && i<15;i++){
                    int sx=76+(i%5)*18, sy=36+(i/5)*18;
                    if(isPointInRegion(sx,sy,16,16,x,y)){
                        ItemStack stack=new ItemStack(mSlots.getCompoundTagAt(i));
                        if(!stack.isEmpty())renderToolTip(stack,x,y);
                        break;
                    }
                }
            }
            Slot monitorSlot=getSlotUnderMouse();
            if(monitorSlot!=null && monitorSlot.slotNumber<9 && !monitorSlot.getHasStack())drawHoveringText(ManagerGui.tr(monitorSlot.slotNumber<4?"wireless_upgrade_help":monitorSlot.slotNumber==7?"harvest_transfer_help":monitorSlot.slotNumber==8?"wireless_eu_help":"fertilizer_buffer_help"),x,y);
        }
        if(weeder() && isPointInRegion(8,36,160,28,x,y)){WeederTile w=(WeederTile)machine;drawHoveringText(UiNumbers.exactResource(w.gas.getFluidAmount(),w.gas.getCapacity(),"mB"),x,y);}
        for (GuiButton b : buttonList) if (b.visible && b.isMouseOver()) {
            if (b.id == 200) { boolean on = monitor() ? ((MonitorTile)machine).snapshot.getBoolean("Enabled") : !weeder() || ((WeederTile)machine).enabled; drawHoveringText(Arrays.asList(ManagerGui.tr("tip.power_title"), (on ? "\u00a7a" : "\u00a7c") + ManagerGui.tr(on ? "on" : "off"), ManagerGui.tr("tip.power_desc")), x, y); }
            if (b.id == 13) { int rs = monitor() ? ((MonitorTile)machine).snapshot.getInteger("Redstone") : weeder() ? ((WeederTile)machine).redstoneMode : 0; drawHoveringText(Arrays.asList(ManagerGui.tr("tip.redstone_title"), "\u00a7e" + ManagerGui.tr("redstone_" + rs), ManagerGui.tr("tip.redstone_cycle")), x, y); }
            if (b.id == 201 && monitor()) { boolean h = (((MonitorTile)machine).snapshot.getInteger("Functions") & 1) != 0; drawHoveringText(Arrays.asList(ManagerGui.tr("tip.harvest_title"), (h ? "\u00a7a" : "\u00a7c") + ManagerGui.tr(h ? "on" : "off"), ManagerGui.tr("tip.harvest_desc")), x, y); }
            if (b.id == 12) drawHoveringText((machine instanceof ComposterTile || machine instanceof GrinGrinderTile || machine instanceof GrinExtractorTile || machine instanceof WeedExFabricatorTile || machine instanceof SeedAnalyzerTile) ? Arrays.asList(ManagerGui.tr("compatible_upgrades"), ManagerGui.tr("upgrade_overclocker"), ManagerGui.tr("upgrade_storage"), ManagerGui.tr("upgrade_transformer"), ManagerGui.tr("wireless_eu_help")) : monitor() ? Arrays.asList(ManagerGui.tr("compatible_upgrades"),ManagerGui.tr("wireless_upgrade_help"),ManagerGui.tr("harvest_transfer_help"),ManagerGui.tr("wireless_eu_help")) : Arrays.asList(ManagerGui.tr("compatible_upgrades"),ManagerGui.tr("no_upgrades")),x,y);
            if (b.id == 146 && monitor()) drawHoveringText(ManagerGui.tr("signal_level")+": "+((MonitorTile)machine).signal,x,y);
            if (b.id == 147) drawHoveringText(ManagerGui.tr("test_signal_help"),x,y);
        }
        if (machine instanceof ComposterTile) {
            ComposterTile c = (ComposterTile)machine;
            if (isPointInRegion(11,42,12,17,x,y)) drawHoveringText((int)c.getEnergy()+" / "+(int)c.energy.getCapacity()+" EU | T"+c.energy.getSinkTier(),x,y);
            if (isPointInRegion(26,28,20,50,x,y)) drawHoveringText(ManagerGui.tr("water")+": "+UiNumbers.exactResource(c.water.getFluidAmount(),c.water.getCapacity(),"mB"),x,y);
            if (isPointInRegion(74,42,30,38,x,y)) drawHoveringText(UiNumbers.exact(c.euPerTick)+" EU/t | "+c.workTicks+" t",x,y);
        }
        if (machine instanceof GrinGrinderTile) {
            if (isPointInRegion(11,42,12,17,x,y)) { GrinGrinderTile g=(GrinGrinderTile)machine; drawHoveringText(UiNumbers.exactResource(g.energy.getEnergyStored(),g.energy.getCapacity(),"EU")+" | T"+g.energy.getSinkTier(),x,y); }
            Slot s=getSlotUnderMouse();
            if(s!=null && s.slotNumber<3 && !s.getHasStack())drawHoveringText(ManagerGui.tr(s.slotNumber==0?"spider_eye":s.slotNumber==1?"grinding_blade":"grin_powder"),x,y);
        }
        if (machine instanceof GrinExtractorTile) {
            GrinExtractorTile e=(GrinExtractorTile)machine;
            if (isPointInRegion(11,42,12,17,x,y)) drawHoveringText(UiNumbers.exactResource(e.energy.getEnergyStored(),e.energy.getCapacity(),"EU")+" | T"+e.energy.getSinkTier(),x,y);
            if (isPointInRegion(28,28,20,50,x,y)) drawHoveringText(ManagerGui.tr("water")+": "+UiNumbers.exactResource(e.water.getFluidAmount(),e.water.getCapacity(),"mB"),x,y);
            if (isPointInRegion(112,28,20,50,x,y)) drawHoveringText(ManagerGui.tr("grin_extract")+": "+UiNumbers.exactResource(e.extract.getFluidAmount(),e.extract.getCapacity(),"mB"),x,y);
            if (isPointInRegion(56,36,48,20,x,y)) drawHoveringText(e.clientHeatPerTick+" HU/t · "+ManagerGui.tr(e.heatConnected?"ready":"extractor_heat"),x,y);
            Slot s=getSlotUnderMouse();
            if(s!=null && s.slotNumber<4 && !s.getHasStack())drawHoveringText(ManagerGui.tr(s.slotNumber==0?"grin_powder":s.slotNumber==1?"slot_water_container":s.slotNumber==2?"slot_empty_container":"slot_output"),x,y);
        }
        if (machine instanceof WeedExFabricatorTile) {
            WeedExFabricatorTile f=(WeedExFabricatorTile)machine;
            if (isPointInRegion(11,42,12,17,x,y)) drawHoveringText(UiNumbers.exactResource(f.energy.getEnergyStored(),f.energy.getCapacity(),"EU")+" | T"+f.energy.getSinkTier(),x,y);
            if (isPointInRegion(28,28,20,50,x,y)) drawHoveringText(ManagerGui.tr("grin_extract")+": "+UiNumbers.exactResource(f.extract.getFluidAmount(),f.extract.getCapacity(),"mB"),x,y);
            if (isPointInRegion(112,28,20,50,x,y)) drawHoveringText("Weed-Ex: "+UiNumbers.exactResource(f.weed.getFluidAmount(),f.weed.getCapacity(),"mB"),x,y);
            Slot s=getSlotUnderMouse();
            if(s!=null && s.slotNumber<4 && !s.getHasStack())drawHoveringText(s.slotNumber==0?"Redstone":ManagerGui.tr(s.slotNumber==1?"slot_extract_container":s.slotNumber==2?"slot_empty_container":"slot_output"),x,y);
        }
        if (machine instanceof SeedAnalyzerTile) {
            SeedAnalyzerTile a=(SeedAnalyzerTile)machine;
            if (isPointInRegion(11,42,12,17,x,y)) drawHoveringText(UiNumbers.exactResource(a.energy.getEnergyStored(),a.energy.getCapacity(),"EU")+" | T"+a.energy.getSinkTier(),x,y);
            if (isPointInRegion(56,24,48,20,x,y)) drawHoveringText(UiNumbers.exact(a.euPerTick)+" EU/t | "+a.clientDuration+" t",x,y);
            Slot s=getSlotUnderMouse();
            if (s!=null && s.slotNumber<2 && !s.getHasStack()) drawHoveringText(ManagerGui.tr(s.slotNumber==0?"analyzer_slot_in":"analyzer_slot_out"),x,y);
        }
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks,int x,int y) {
        String texture;
        if (monitor()) texture=sidebarExpanded?"equipment_monitor":"equipment_monitor_compact";
        else if (weeder()) texture=sidebarExpanded?"equipment_weeder_expanded":"equipment_weeder_compact";
        else if (machine instanceof ComposterTile) texture="equipment_composter";
        else if (machine instanceof GrinGrinderTile) texture="equipment_grinder";
        else if (machine instanceof GrinExtractorTile) texture="equipment_extractor";
        else if (machine instanceof SeedAnalyzerTile) texture="equipment_analyzer";
        else texture="equipment_mixer";
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        FarmGuiStyle.background(texture,guiLeft,guiTop,xSize,ySize);
        net.minecraft.client.renderer.GlStateManager.enableDepth();
    }
    private void text(String line,int x,int y,int width) { fontRenderer.drawString(fontRenderer.trimStringToWidth(line,width),x,y,ManagerGui.ink()); }
    private void centeredText(String line,int centerX,int y,int maxWidth) {
        String trimmed=fontRenderer.trimStringToWidth(line,maxWidth);
        fontRenderer.drawString(trimmed,centerX-fontRenderer.getStringWidth(trimmed)/2,y,ManagerGui.ink());
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        String name = machine instanceof ComposterTile ? "composter_name" : machine instanceof GrinGrinderTile ? "grinder_name" : machine instanceof GrinExtractorTile ? "extractor_name" : machine instanceof WeedExFabricatorTile ? "mixer_name" : machine instanceof SeedAnalyzerTile ? "analyzer_name" : weeder() ? "weeder_name" : monitor() ? "monitor_name" : "legacy_name";
        FarmGuiStyle.title(fontRenderer,ManagerGui.tr(name),8,7,side()?128:146);
        if (machine instanceof ComposterTile) {
            ComposterTile c=(ComposterTile)machine;
            double euRatio=c.getEnergy()/Math.max(1,c.energy.getCapacity());
            FarmGuiStyle.bolt(13,44,euRatio);
            FarmGuiStyle.recycle(80,20,c.progress>0);
            vertical(26,28,20,50,c.water.getFluidAmount()/(double)Math.max(1,c.water.getCapacity()),0xff438cbd);
            FarmGuiStyle.tankScale(26,28,20,50);
            centeredText(ManagerGui.tr("water_short"),36,18,24);
            centeredText(ManagerGui.tr("input_short"),62,30,36); centeredText(ManagerGui.tr("output_short"),120,30,40);
            bar(74,62,30,c.progress/(double)Math.max(1,c.workTicks),0xff76a74b);
            centeredText(String.format(java.util.Locale.ROOT,"%.0f%%",c.progress*100.0/Math.max(1,c.workTicks)),89,71,45);
            text(ManagerGui.tr(c.condition),8,105,160);
        } else if (machine instanceof WeedExFabricatorTile) {
            WeedExFabricatorTile f=(WeedExFabricatorTile)machine;
            double euRatio=f.energy.getEnergyStored()/Math.max(1,f.energy.getCapacity());
            FarmGuiStyle.bolt(13,44,euRatio);
            vertical(28,28,20,50,f.extract.getFluidAmount()/(double)Math.max(1,f.extract.getCapacity()),0xff83aa55);
            vertical(112,28,20,50,f.weed.getFluidAmount()/(double)Math.max(1,f.weed.getCapacity()),0xffb9a247);
            FarmGuiStyle.tankScale(28,28,20,50);
            FarmGuiStyle.tankScale(112,28,20,50);
            centeredText(ManagerGui.tr("extract_short"),38,18,44); centeredText("Weed-Ex",122,18,52);
            text(ManagerGui.tr(f.condition),8,106,160);
            bar(56,59,48,f.progress/(double)Math.max(1,f.clientDuration),0xff76a74b);
            centeredText(f.progress+" / "+f.clientDuration+" t",80,69,56);
        } else if (machine instanceof GrinGrinderTile) {
            GrinGrinderTile g=(GrinGrinderTile)machine;
            double euRatio=g.energy.getEnergyStored()/Math.max(1,g.energy.getCapacity());
            FarmGuiStyle.bolt(13,44,euRatio);
            centeredText(ManagerGui.tr("input_short"),46,38,44); centeredText(ManagerGui.tr("output_short"),130,38,44);
            text(ManagerGui.tr(g.condition),8,106,160); bar(64,80,48,g.progress/(double)Math.max(1,g.clientDuration),0xff76a74b);
            centeredText(g.progress+" / "+g.clientDuration+" t",88,89,64);
        } else if (machine instanceof GrinExtractorTile) {
            GrinExtractorTile e=(GrinExtractorTile)machine;
            double euRatio=e.energy.getEnergyStored()/Math.max(1,e.energy.getCapacity());
            FarmGuiStyle.bolt(13,44,euRatio);
            vertical(28,28,20,50,e.water.getFluidAmount()/(double)Math.max(1,e.water.getCapacity()),0xff438cbd);
            vertical(112,28,20,50,e.extract.getFluidAmount()/(double)Math.max(1,e.extract.getCapacity()),0xff83aa55);
            FarmGuiStyle.tankScale(28,28,20,50);
            FarmGuiStyle.tankScale(112,28,20,50);
            FarmGuiStyle.heat(74,42,e.heatConnected || e.progress>0);
            centeredText(ManagerGui.tr("water"),38,18,44); centeredText(ManagerGui.tr("extract_short"),122,18,48);
            centeredText(e.clientHeatPerTick+" HU/t",80,28,50); bar(56,59,48,e.progress/(double)Math.max(1,e.clientDuration),0xff76a74b);
            centeredText(e.progress+" / "+e.clientDuration+" t",80,69,56); text(ManagerGui.tr(e.condition),8,106,160);
        } else if (machine instanceof SeedAnalyzerTile) {
            SeedAnalyzerTile a=(SeedAnalyzerTile)machine;
            double euRatio=a.energy.getEnergyStored()/Math.max(1,a.energy.getCapacity());
            FarmGuiStyle.bolt(13,44,euRatio);
            bar(58,36,42,a.progress/(double)Math.max(1,a.clientDuration),0xff76a74b);
            if (!a.lastSpecies.isEmpty()) {
                NBTTagCompound row=new NBTTagCompound();
                int colon=a.lastSpecies.indexOf(':');
                if (colon>0) { row.setString("Owner",a.lastSpecies.substring(0,colon)); row.setString("Id",a.lastSpecies.substring(colon+1)); }
                String title=CropSurveyWidgets.name(row);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(title,102),34,54,0xffe8f4ec);
                String stats="GR "+a.lastGr+"  GA "+a.lastGa+"  RE "+a.lastRe;
                fontRenderer.drawString(fontRenderer.trimStringToWidth(stats,102),34,66,0xff9cdeb0);
                String tierLine="T"+a.lastTier+" · 4/4"+(a.lastCatalyzed?" · "+ManagerGui.tr("catalyzed_badge"):"");
                fontRenderer.drawString(fontRenderer.trimStringToWidth(tierLine,102),34,77,a.lastCatalyzed?0xffffcc66:0xffa0c8b8);
                if (!a.lastAttributes.isEmpty()) fontRenderer.drawString(fontRenderer.trimStringToWidth(a.lastAttributes,102),34,88,0xff90a4ae);
            } else {
                fontRenderer.drawString(fontRenderer.trimStringToWidth(ManagerGui.tr("analyzer_no_data"),102),34,70,0xff8899a6);
            }
            text(ManagerGui.tr(a.condition),8,106,160);
        } else if(weeder()) {
            WeederTile w=(WeederTile)machine;
            int gasCap=Math.max(1,w.gas.getCapacity());
            text(ManagerGui.tr(!w.enabled?"manual_off":w.paused?"paused":w.gas.getFluidAmount()==0?"no_biogas":"running"),8,22,160);
            vertical(8,36,12,28,w.gas.getFluidAmount()/(double)gasCap,0xff83aa55);
            FarmGuiStyle.tankScale(8,36,12,28);
            text(ManagerGui.tr("biogas")+": "+UiNumbers.resource(w.gas.getFluidAmount(),gasCap,"mB"),25,37,143);
            text(ManagerGui.tr("input_short"),36,52,48);text(ManagerGui.tr("output_short"),100,52,48);
            
            if(sidebarExpanded)AreaWidgets.draw(fontRenderer,182,30,w.radius,w.layers);
        } else if (monitor()) {
            MonitorTile m=(MonitorTile)machine; NBTTagCompound s=m.snapshot;
            String state="connected".equals(m.connection) && s.hasKey("State")?s.getString("State"):"monitor_"+m.connection;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(ManagerGui.tr(state),160),8,22,m.alarmActive?FarmGuiStyle.warningInk():FarmGuiStyle.goodInk());
            double wirelessRatio=m.wirelessEuUpgrades()>0?m.energy.getEnergyStored()/Math.max(1,m.wirelessEuCapacity()):0;
            FarmGuiStyle.bolt(29,132,wirelessRatio);
            if("connected".equals(m.connection) && "weeder".equals(s.getString("Target"))){
                int gasCap=Math.max(1,s.hasKey("GasCapacity")?s.getInteger("GasCapacity"):ManagerConfig.weederGasCapacity);
                FarmGuiStyle.panel(74,34,94,58);
                bar(78,44,86,s.getInteger("Gas")/(double)gasCap,0xff83aa55);
                text(ManagerGui.tr("biogas")+": "+UiNumbers.resource(s.getInteger("Gas"),gasCap,"mB"),78,56,86);
                vertical(8,42,16,36,m.water.getFluidAmount()/(double)Math.max(1,m.water.getCapacity()),0xff438cbd);
                vertical(30,42,16,36,m.weed.getFluidAmount()/(double)Math.max(1,m.weed.getCapacity()),0xffb9a247);
                FarmGuiStyle.tankScale(8,42,16,36);
                FarmGuiStyle.tankScale(30,42,16,36);
                text(ManagerGui.tr("water_short"),6,80,22);text(ManagerGui.tr("weed_short"),30,80,22);
            } else {
                boolean remote="connected".equals(m.connection);
                int water=remote&&s.hasKey("Water")?s.getInteger("Water"):m.water.getFluidAmount();
                int weed=remote&&s.hasKey("Weed")?s.getInteger("Weed"):m.weed.getFluidAmount();
                int waterCap=remote&&s.hasKey("TankCapacity")?s.getInteger("TankCapacity"):m.water.getCapacity();
                int weedCap=remote&&s.hasKey("WeedCapacity")?s.getInteger("WeedCapacity"):(remote&&s.hasKey("TankCapacity")?s.getInteger("TankCapacity"):m.weed.getCapacity());
                double targetEuRatio=remote?s.getDouble("EU")/Math.max(1,s.getDouble("Capacity")):0;
                FarmGuiStyle.bolt(54,42,targetEuRatio);
                vertical(8,42,16,36,water/(double)Math.max(1,waterCap),0xff438cbd);
                vertical(30,42,16,36,weed/(double)Math.max(1,weedCap),0xffb9a247);
                FarmGuiStyle.tankScale(8,42,16,36);
                FarmGuiStyle.tankScale(30,42,16,36);
                text(ManagerGui.tr("water_short"),6,80,22);text(ManagerGui.tr("weed_short"),30,80,22);
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(ic2.api.item.IC2Items.getItem("crop_res","fertilizer"),50,59);
                int fertilizer=remote&&s.hasKey("Fertilizer")?s.getInteger("Fertilizer"):m.inventory.getStackInSlot(4).getCount()+m.inventory.getStackInSlot(5).getCount()+m.inventory.getStackInSlot(6).getCount();
                text("x"+UiNumbers.compact(fertilizer),50,77,24);
                if(remote && "manager".equals(s.getString("Target"))){
                    NBTTagList mSlots=s.getTagList("ManagerSlots",10);
                    for(int i=0;i<mSlots.tagCount() && i<15;i++){
                        ItemStack stack=new ItemStack(mSlots.getCompoundTagAt(i));
                        if(stack.isEmpty())continue;
                        int sx=76+(i%5)*18, sy=36+(i/5)*18;
                        mc.getRenderItem().renderItemAndEffectIntoGUI(stack,sx,sy);
                        mc.getRenderItem().renderItemOverlays(fontRenderer,stack,sx,sy);
                    }
                }
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            }
            text(ManagerGui.tr("wireless_short"),8,94,82);text(ManagerGui.tr("fertilizer_short"),98,94,70);
            text(ManagerGui.tr("wireless_eu_short"),39,134,54);
            text(ManagerGui.tr("harvest_transfer_short"),118,134,54);
            if(sidebarExpanded){
                if(tab<2){
                    survey.draw(fontRenderer,s,182,46,132,tab==1);
                } else if(tab==3){
                    NBTTagCompound rule=s.getCompoundTag("Automation");
                    text(ManagerGui.tr("water"),182,60,132);text(percent(rule.getInteger("Water")),252,74,48);
                    text("Weed-Ex",182,90,132);text(percent(rule.getInteger("Weed")),252,104,48);
                    text(ManagerGui.tr("fertilizer"),182,120,132);text(rule.getInteger("Fertilizer")<0?"-":String.valueOf(rule.getInteger("Fertilizer")),252,134,48);
                    text(ManagerGui.tr("energy"),182,150,132);text(percent(rule.getInteger("Energy")),252,164,48);
                } else if(tab==4) drawHarvestList(s);
            }
        } else text(ManagerGui.tr("legacy_help"),8,22,160);
    }
    private String percent(int value){return value<0?"-":value==0?ManagerGui.tr("empty_short"):value+"%";}
    private void drawHarvestList(NBTTagCompound state){
        NBTTagList items=state.getTagList("HarvestItems",10);int pages=Math.max(1,(items.tagCount()+5)/6);harvestPage=Math.min(harvestPage,pages-1);
        FarmGuiStyle.title(fontRenderer,ManagerGui.tr("harvest_inventory"),182,44,132);
        if(items.tagCount()==0){
            fontRenderer.drawSplitString(ManagerGui.tr("harvest_empty"),184,72,128,FarmGuiStyle.mutedInk());
            text(ManagerGui.tr("page")+" 1 / 1",228,210,72);
            return;
        }
        for(int row=0;row<6;row++){
            int index=harvestPage*6+row,y=62+row*22;
            drawRect(182,y,314,y+20,0xff000000|(row%2==0?0x303030:0x3a3a3a));
            if(index>=items.tagCount())continue;
            NBTTagCompound entry=items.getCompoundTagAt(index);ItemStack stack=new ItemStack(entry.getCompoundTag("Stack"));
            if(stack.isEmpty())continue;
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack,184,y+2);mc.getRenderItem().renderItemOverlays(fontRenderer,stack,184,y+2);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            text(stack.getDisplayName()+" x"+UiNumbers.compact(stack.getCount()),204,y+2,108);
            text(ManagerGui.tr(entry.getInteger("Source")==0?"harvest_manager_source":"harvest_monitor_source"),204,y+11,108);
        }
        text(ManagerGui.tr("page")+" "+(harvestPage+1)+" / "+pages,228,210,72);
        if(!"manager".equals(state.getString("Target")))text(ManagerGui.tr("harvest_unlinked"),182,196,132);
    }
    private void bar(int x,int y,int width,double ratio,int color) { fill(x,y,width,5,ratio,color,false); }
    private void vertical(int x,int y,int w,int h,double ratio,int color) { fill(x,y,w,h,ratio,color,true); }
    private void fill(int x,int y,int w,int h,double ratio,int color,boolean vertical) {
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        ratio=Math.max(0,Math.min(1,ratio));
        if(ratio>0) {
            if(vertical) drawRect(x+2,y+h-2-(int)((h-4)*ratio),x+w-2,y+h-2,color);
            else drawRect(x+2,y+2,x+2+(int)((w-4)*ratio),y+h-2,color);
        }
        net.minecraft.client.renderer.GlStateManager.enableDepth();
    }
    private final class IconButton extends GuiButton {
        IconButton(int id, int x, int y) { super(id, x, y, 18, 18, ""); }
        @Override public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float ticks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            boolean on = false;
            int col = 0, row = 0;
            if (id == 200) {
                on = monitor() ? ((MonitorTile)machine).snapshot.getBoolean("Enabled") : weeder() && ((WeederTile)machine).enabled;
                col = on ? 0 : 1; row = 0;
            } else if (id == 13) {
                int rs = monitor() ? ((MonitorTile)machine).snapshot.getInteger("Redstone") : weeder() ? ((WeederTile)machine).redstoneMode : 0;
                col = rs == 0 ? 2 : rs == 1 ? 3 : 0; row = rs == 2 ? 1 : 0;
            } else if (id == 201) {
                on = monitor() && (((MonitorTile)machine).snapshot.getInteger("Functions") & 1) != 0;
                col = on ? 1 : 2; row = 1;
            }
            FarmGuiStyle.button(x, y, width, height, enabled, on, hovered);
            FarmGuiStyle.icon(x + 1, y + 1, col, row);
        }
    }
}
