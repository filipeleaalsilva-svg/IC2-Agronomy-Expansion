package br.cropmanager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fluids.*;
import ic2.api.item.IC2Items;
import ic2.core.ref.FluidName;

/** Opt-in real-client GUI/network fixtures; excluded from the release JAR. */
@Mod.EventBusSubscriber(modid="cropmanager_tests",value=Side.CLIENT)
public final class ClientChecks {
    private static final boolean RESERVOIR_QA = Boolean.getBoolean("cropmanager.qaReservoir");
    private static final boolean LIGHTS_QA = Boolean.getBoolean("cropmanager.qaLights");
    private static final boolean MANUAL_QA = Boolean.getBoolean("cropmanager.qaManual");
    private static final boolean SHOWCASE_QA = Boolean.getBoolean("cropmanager.qaShowcase");
    private static final String[] SHOWCASE_SHOTS = {
            "01-overview-aerial", "02-hub-wifi-database", "03-automated-crop-farm",
            "04-multiblock-courtyard", "05-chemistry-and-biogas", "06-botanical-greenhouse-15crops",
            "07-hydroponic-thermal-lab", "08-irrigation-sprinklers"
    };
    private static final double[][] SHOWCASE_VIEWS = {
            // {x, y, z, yaw, pitch}
            {0.5, 36.0, 46.0, 180.0, 38.0},    // 01-overview-aerial
            {0.5, 8.5, 17.5, 180.0, 14.0},     // 02-hub-wifi-database
            {0.5, 10.5, -2.5, 180.0, 24.0},    // 03-automated-crop-farm
            {12.0, 11.5, 6.5, -132.0, 18.0},   // 04-multiblock-courtyard
            {-14.0, 10.0, 6.5, 142.0, 18.0},   // 05-chemistry-and-biogas
            {-25.0, 7.2, 14.2, 0.0, 18.0},     // 06-botanical-greenhouse-15crops
            {26.5, 10.5, 25.5, -180.0, 24.0},  // 07-hydroponic-thermal-lab
            {32.0, 12.5, -14.0, 135.0, 28.0}   // 08-irrigation-sprinklers
    };
    private static ShowcaseFarmBuilder.ShowcaseSummary showcaseSummary;
    private static final String[] RESERVOIR_SHOTS = {"reservoir-gui", "reservoir-gui-scale1", "reservoir-upgrades",
            "reservoir-south", "reservoir-west", "reservoir-north", "reservoir-east", "reservoir-teardown"};
    private static final EnumFacing[] RESERVOIR_FACES = {EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST};
    private static final boolean CONTRAST=Boolean.getBoolean("cropmanager.qaContrast");
    private static net.minecraft.client.resources.IResourcePack contrastPack;
    private static boolean launched, preparing;
    private static volatile boolean ready;
    private static int remoteFunctions;
    private static int age, settle, stage=Integer.getInteger("cropmanager.qaStart",0);
    private static final BlockPos BASE=new BlockPos(8,4,8);
    private static final BlockPos RESERVOIR_CENTER = BASE.south(10);
    private static final BlockPos RESERVOIR_CONTROLLER = RESERVOIR_CENTER.south().up();
    private static final String[] SHOTS=CONTRAST?new String[]{"monitor-dark-contrast","monitor-light-contrast"}:new String[]{"manager-operation","manager-data","manager-filter","manager-collapsed","composter","monitor-data","monitor-controls","monitor-native","reservoir","collector","probe","world","network-manager","network-monitor","network-collector","scale1-manager","scale1-composter","scale1-monitor","scale1-reservoir","scale1-collector","weeder","weeder-world","monitor-automation","manager-area","weeder-area","network-remote","network-rule","monitor-environment","manager-environment","crop-selected-environment","survey-pagination","scale1-weeder","scale1-monitor-controls","scale1-monitor-automation"};
    private static EntityPlayerMP player(Minecraft mc) { return mc.getIntegratedServer().getPlayerList().getPlayers().get(0); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if((!Boolean.getBoolean("cropmanager.clientQa") && !SHOWCASE_QA) || e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        try {
            if(!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched=true;mc.gameSettings.guiScale=2;mc.gameSettings.renderDistanceChunks=SHOWCASE_QA?8:4;mc.gameSettings.pauseOnLostFocus=false;
                String lang=System.getProperty("cropmanager.qaLanguage","en_us");mc.gameSettings.language=lang;mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage(lang));if(CONTRAST) {
                    contrastPack=new net.minecraft.client.resources.FileResourcePack(new File(System.getProperty("cropmanager.qaContrastPack")));
                    defaultPacks(mc).add(contrastPack);
                }
                mc.refreshResources();
                mc.launchIntegratedServer(SHOWCASE_QA ? ShowcaseFarmBuilder.SAVE_FOLDER
                                : LIGHTS_QA ? "cropmanager-lights-client-test"
                                : MANUAL_QA ? "cropmanager-manual-client-test"
                                : RESERVOIR_QA ? "cropmanager-reservoir-client-test" : "cropmanager-client-test",
                        SHOWCASE_QA ? "Crop Manager Showcase Farm" : "Crop Manager GUI QA",
                        new WorldSettings(1,GameType.CREATIVE,false,false,WorldType.FLAT).enableCommands());return;
            }
            if(mc.world==null || mc.player==null)return;
            if (SHOWCASE_QA) {
                checkShowcase(mc);
                return;
            }
            if (LIGHTS_QA) {
                checkLights(mc);
                return;
            }
            if (MANUAL_QA) {
                checkManual(mc);
                return;
            }
            if(!preparing) { preparing=true;mc.getIntegratedServer().addScheduledTask(()->prepare(mc));return; }
            if(!ready)return;
            if(settle < 40) { settle++; return; }
            if(!(mc.world.getTileEntity(BASE) instanceof AdvancedManagerTile) || !(mc.world.getTileEntity(BASE.east(2)) instanceof MonitorTile) || !(mc.world.getTileEntity(BASE.east(4)) instanceof ComposterTile) || !(mc.world.getTileEntity(BASE.east(6)) instanceof GrinGrinderTile) || !(mc.world.getTileEntity(BASE.east(8)) instanceof WeedExFabricatorTile)) {
                if(++settle > 200)throw new AssertionError("Fixture block entities did not synchronize to client");
                return;
            }
            if (RESERVOIR_QA) {
                captureReservoir(mc);
                return;
            }
            if(age++==0)show(mc);
            if(age==5)org.lwjgl.input.Mouse.setCursorPosition(0,0);
            if(!CONTRAST && age==8 && (stage==1 || stage==2 || stage==3 || stage==6 || stage==22 || stage==23)) {
                if(stage==1 || stage==2 || stage==23) {Field f=ManagerGui.class.getDeclaredField("tab");f.setAccessible(true);f.setInt(mc.currentScreen,stage==23?3:stage);}
                if(stage==3 || stage==6 || stage==22 || stage==23) {
                    int window=mc.player.openContainer.windowId;
                    Method m=mc.currentScreen.getClass().getDeclaredMethod("actionPerformed",GuiButton.class);m.setAccessible(true);m.invoke(mc.currentScreen,new GuiButton(stage==3?14:stage==22?33:stage==6?32:31,0,0,""));
                    if(mc.player.openContainer.windowId!=window)throw new AssertionError("Local GUI control replaced the container");
                }
            }
            if(!CONTRAST && age==8 && stage>=27) {
                if(stage==27)local(mc,31);
                if(stage==28)local(mc,21);
                if(stage==29)local(mc,303);
                if(stage==30)local(mc,301);
                if(stage==32)local(mc,32);
                if(stage==33)local(mc,33);
            }
            if(!CONTRAST && stage==28 && age==10)local(mc,41);
            if(stage==9 && age==8) {Method m=EquipmentGui.class.getDeclaredMethod("actionPerformed",GuiButton.class);m.setAccessible(true);m.invoke(mc.currentScreen,new GuiButton(182,0,0,""));}
            if(stage==12) {
                if(age==8)send(mc,13,0);
                if(age==14) {if(((ManagerTile)FarmTile.openMachine(mc.player)).redstoneMode!=1)throw new AssertionError("Manager redstone packet failed");send(mc,200,0);}
                if(age==22) {if(((ManagerTile)FarmTile.openMachine(mc.player)).enabled)throw new AssertionError("Manager power packet failed");send(mc,200,1);}
                if(age==32) {if(((ManagerTile)FarmTile.openMachine(mc.player)).enabled)throw new AssertionError("Wrong-window packet accepted");CropManager.log.info("CLIENT_QA_MANAGER_NETWORK_PASS");}
            }
            if(stage==13 && age>=8) {
                MonitorTile m=(MonitorTile)FarmTile.openMachine(mc.player);
                if(age==8)send(mc,146,0);
                if(age==14){if(m.outputMode!=2 || m.signal!=15)throw new AssertionError("Normal signal mode failed");send(mc,146,0);}
                if(age==20){if(m.outputMode!=0 || m.signal!=0)throw new AssertionError("Output off failed");send(mc,147,0);}
                if(age==26){if(m.signal!=15)throw new AssertionError("Signal test failed");send(mc,148,0);}
                if(age==32){if(m.signal!=0 || !"unlinked".equals(m.connection))throw new AssertionError("Unlink did not clear signal");CropManager.log.info("CLIENT_QA_MONITOR_NETWORK_PASS");}
            }
            if(stage==14 && age==32) {
                if(!(FarmTile.openMachine(mc.player) instanceof WeedExFabricatorTile))throw new AssertionError("Expected Weed-Ex Mixer GUI");
                CropManager.log.info("CLIENT_QA_MIXER_NETWORK_PASS");
            }
            if(stage==22 && age==20 && ManagerGui.tr("resume_weed_full").startsWith("Format error"))throw new AssertionError("Automation percentage tooltip is malformed");
            if(stage==23 && age==10)send(mc,211,0);
            if(stage==23 && age==18){if(((ManagerTile)FarmTile.openMachine(mc.player)).radius!=5)throw new AssertionError("Area radius packet failed");send(mc,212,0);}
            if(stage==23 && age==26){if(((ManagerTile)FarmTile.openMachine(mc.player)).layers!=6)throw new AssertionError("Layer packet failed");CropManager.log.info("CLIENT_QA_AREA_PASS");}
            if(stage==24 && age==10)send(mc,211,0);
            if(stage==24 && age==24){if(((WeederTile)FarmTile.openMachine(mc.player)).radius!=5)throw new AssertionError("Weeder area packet failed");CropManager.log.info("CLIENT_QA_WEEDER_PASS");}
            if(stage==25 && age==8){remoteFunctions=((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getInteger("Functions");send(mc,200,0);}
            if(stage==25 && age==14){if(((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getBoolean("Enabled"))throw new AssertionError("Remote switch failed");send(mc,201,0);}
            if(stage==25 && age==22){if(((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getInteger("Functions")!=(remoteFunctions^1))throw new AssertionError("Remote harvest toggle failed");send(mc,200,0);}
            if(stage==25 && age==32){if(!((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getBoolean("Enabled"))throw new AssertionError("Remote resume failed");CropManager.log.info("CLIENT_QA_REMOTE_PASS");}
            if(stage==26 && age==8)send(mc,220,0);
            if(stage==26 && age==14){if(((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getCompoundTag("Automation").getInteger("Mode")!=1)throw new AssertionError("Rule mode packet failed");send(mc,225,0);}
            if(stage==26 && age==22){if(((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getCompoundTag("Automation").getInteger("Mode")!=2)throw new AssertionError("All empty preset failed");send(mc,220,1);}
            if(stage==26 && age==32){if(((MonitorTile)FarmTile.openMachine(mc.player)).snapshot.getCompoundTag("Automation").getInteger("Mode")!=2)throw new AssertionError("Invalid window rule packet accepted");CropManager.log.info("CLIENT_QA_RULE_PASS");}
            if(age==35) {
                if (stage == 0 && !mc.player.openContainer.getSlot(18).getHasStack())
                    throw new AssertionError("Manager upgrade did not synchronize to its GUI slot");
                if (stage == 5) {
                    MonitorTile monitor = (MonitorTile)FarmTile.openMachine(mc.player);
                    if (!mc.player.openContainer.getSlot(0).getHasStack()
                            || !mc.player.openContainer.getSlot(MonitorTile.WIRELESS_EU_SLOT).getHasStack()
                            || new ItemStack(monitor.snapshot.getTagList("ManagerUpgrades", 10).getCompoundTagAt(0)).isEmpty())
                        throw new AssertionError("Monitor local or linked Manager upgrades did not synchronize");
                }
                if(stage!=11 && stage!=21 && mc.currentScreen==null)throw new AssertionError("Expected GUI did not open: "+SHOTS[stage]);
                if(mc.currentScreen!=null)checkLayout(mc);
                if(!CONTRAST && stage==27) {
                    boolean portuguese=mc.gameSettings.language.startsWith("pt_");
                    if(!UiNumbers.compact(12599).equals(portuguese?"~12,6k":"~12.6k") || !UiNumbers.exact(12599).equals(portuguese?"12.599":"12,599"))throw new AssertionError("Resource abbreviation or exact tooltip locale failed");
                    CropManager.log.info("CLIENT_QA_COMPACT_LOCALE_PASS");
                }
                if(!CONTRAST && (stage==29 || stage==30)) {
                    Field field=EquipmentGui.class.getDeclaredField("survey");field.setAccessible(true);Object widget=field.get(mc.currentScreen);
                    Field value=CropSurveyWidgets.class.getDeclaredField(stage==29?"selected":"page");value.setAccessible(true);
                    if(stage==29?!value.getBoolean(widget):value.getInt(widget)!=1)throw new AssertionError("Local survey selection or pagination failed");
                    CropManager.log.info("CLIENT_QA_SURVEY_LOCAL_PASS {}",stage);
                }
                File out=new File(System.getProperty("cropmanager.qaDir"));out.mkdirs();ScreenShotHelper.saveScreenshot(out,SHOTS[stage]+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                CropManager.log.info("CLIENT_QA_SCREENSHOT {}",SHOTS[stage]);stage++;age=0;
                if(stage==SHOTS.length){CropManager.log.info("CROP_MANAGER_CLIENT_QA_PASSED");mc.shutdown();}
            }
        }catch(Throwable err){CropManager.log.error("CROP_MANAGER_CLIENT_QA_FAILED",err);mc.shutdown();}
    }
    private static void checkLights(Minecraft mc) {
        if (!preparing) {
            preparing = true;
            mc.getIntegratedServer().addScheduledTask(() -> {
                WorldServer world = mc.getIntegratedServer().getWorld(0);
                world.setWorldTime(6000);
                for (int i = 0; i < CropManager.LANTERNS.length; i++) {
                    FarmLanternBlock lamp = CropManager.LANTERNS[i];
                    BlockPos floor = new BlockPos(8 + i * 2, 4, 12);
                    BlockPos wall = floor.up(3);
                    BlockPos ceiling = floor.up(6);
                    world.setBlockState(floor.down(), CropManager.GREENHOUSE_FRAME.getDefaultState(), 3);
                    world.setBlockState(wall.south(), CropManager.GREENHOUSE_FRAME.getDefaultState(), 3);
                    world.setBlockState(ceiling.up(), CropManager.GREENHOUSE_GLASS.getDefaultState(), 3);
                    world.setBlockState(floor, lamp.getDefaultState().withProperty(FarmLanternBlock.FACING, EnumFacing.DOWN), 3);
                    world.setBlockState(wall, lamp.getDefaultState().withProperty(FarmLanternBlock.FACING, EnumFacing.SOUTH), 3);
                    world.setBlockState(ceiling, lamp.getDefaultState().withProperty(FarmLanternBlock.FACING, EnumFacing.UP), 3);
                }
                player(mc).connection.setPlayerLocation(11.5, 6, 3.0, 0, 0);
                ready = true;
            });
            return;
        }
        if (!ready || ++settle < 60) return;
        if (mc.world.getBlockState(new BlockPos(14, 10, 12)).getBlock() != CropManager.GLASS_LAMP) {
            throw new AssertionError("Lamp fixture failed to synchronize to the client");
        }
        File out = new File(System.getProperty("cropmanager.qaDir"));
        out.mkdirs();
        ScreenShotHelper.saveScreenshot(out, "greenhouse-lights.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        CropManager.log.info("CROP_MANAGER_CLIENT_QA_PASSED");
        mc.shutdown();
    }

    private static void checkShowcase(Minecraft mc) {
        if (!preparing) {
            preparing = true;
            String lang = System.getProperty("cropmanager.qaLanguage", "pt_br");
            mc.getIntegratedServer().addScheduledTask(() -> {
                try {
                    WorldServer world = mc.getIntegratedServer().getWorld(0);
                    EntityPlayerMP p = player(mc);
                    showcaseSummary = ShowcaseFarmBuilder.build(world, ShowcaseFarmBuilder.DEFAULT_ORIGIN, lang, p);
                    ShowcaseFarmBuilder.verifyShowcaseIntegrity(world, showcaseSummary);
                    ready = true;
                } catch (Throwable err) {
                    CropManager.log.error("CROP_MANAGER_SHOWCASE_QA_FAILED", err);
                    mc.addScheduledTask(mc::shutdown);
                }
            });
            return;
        }
        if (!ready) {
            return;
        }
        if (age == 0) {
            mc.displayGuiScreen(null);
            mc.gameSettings.hideGUI = true;
            mc.gameSettings.fovSetting = 70.0f;
            mc.gameSettings.renderDistanceChunks = 8;
            double[] view = SHOWCASE_VIEWS[stage];
            mc.getIntegratedServer().addScheduledTask(() -> {
                EntityPlayerMP p = player(mc);
                p.capabilities.isFlying = true;
                p.sendPlayerAbilities();
                p.connection.setPlayerLocation(view[0], view[1], view[2], (float) view[3], (float) view[4]);
            });
        }
        int requiredSettle = stage == 0 ? 80 : 40;
        if (++age < requiredSettle) {
            return;
        }
        File out = new File(System.getProperty("cropmanager.qaDir"));
        out.mkdirs();
        ScreenShotHelper.saveScreenshot(out, SHOWCASE_SHOTS[stage] + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        CropManager.log.info("CLIENT_QA_SCREENSHOT {}", SHOWCASE_SHOTS[stage]);
        stage++;
        age = 0;
        if (stage == SHOWCASE_SHOTS.length) {
            mc.gameSettings.hideGUI = false;
            mc.getIntegratedServer().addScheduledTask(() -> {
                EntityPlayerMP p = player(mc);
                p.capabilities.isFlying = false;
                p.sendPlayerAbilities();
                p.connection.setPlayerLocation(0.5, 5.0, 28.5, 180.0f, 10.0f);
                mc.getIntegratedServer().saveAllWorlds(false);
                CropManager.log.info("CROP_MANAGER_SHOWCASE_QA_PASSED");
                mc.addScheduledTask(mc::shutdown);
            });
            ready = false;
        }
    }

    @SuppressWarnings("unchecked") private static java.util.List<GuiButton> buttons(GuiScreen screen) throws Exception {
        Field field=GuiScreen.class.getDeclaredField("buttonList");field.setAccessible(true);return (java.util.List<GuiButton>)field.get(screen);
    }
    private static void local(Minecraft mc,int id) throws Exception {
        GuiButton target=null;for(GuiButton button:buttons(mc.currentScreen))if(button.id==id && button.visible)target=button;
        if(target==null || !target.enabled)throw new AssertionError("Expected local control unavailable: "+id);
        int window=mc.player.openContainer.windowId;
        Method action=mc.currentScreen.getClass().getDeclaredMethod("actionPerformed",GuiButton.class);action.setAccessible(true);action.invoke(mc.currentScreen,target);
        if(window!=mc.player.openContainer.windowId)throw new AssertionError("Survey/tab action replaced inventory window");
    }
    private static void checkLayout(Minecraft mc) throws Exception {
        java.util.List<GuiButton> buttons=buttons(mc.currentScreen);
        for(int i=0;i<buttons.size();i++) {
            GuiButton a=buttons.get(i);if(!a.visible)continue;
            if(a.x<0 || a.y<0 || a.x+a.width>mc.currentScreen.width || a.y+a.height>mc.currentScreen.height)throw new AssertionError("Button outside screen: "+a.id+" stage="+stage);
            if(a.id!=12 && mc.fontRenderer.getStringWidth(a.displayString)>a.width-4)throw new AssertionError("Button label clipped: "+a.id+" "+a.displayString+" stage="+stage);
            for(int j=i+1;j<buttons.size();j++) {
                GuiButton b=buttons.get(j);if(!b.visible)continue;
                if(a.x<b.x+b.width && a.x+a.width>b.x && a.y<b.y+b.height && a.y+a.height>b.y)throw new AssertionError("Buttons overlap: "+a.id+"/"+b.id+" stage="+stage);
            }
        }
        CropManager.log.info("CLIENT_QA_LAYOUT_PASS {}",stage);
    }
    private static void send(Minecraft mc,int action,int wrongWindow) {ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.Action(FarmTile.openMachine(mc.player),mc.player.openContainer.windowId+wrongWindow,action));}
    private static void prepare(Minecraft mc) {
        WorldServer w=mc.getIntegratedServer().getWorld(0);w.setWorldTime(6000);
        prepareReservoir(w);
        ManagerBlock[] blocks={CropManager.ADVANCED,CropManager.MONITOR,CropManager.COMPOSTER,CropManager.GRIN_GRINDER,CropManager.WEED_EX_FABRICATOR,CropManager.WEEDER};
        for(int i=-3;i<=14;i++)w.setBlockToAir(BASE.east(i));
        for(int i=0;i<blocks.length;i++)w.setBlockState(BASE.east(i*2),blocks[i].getDefaultState().withProperty(ManagerBlock.FACING,EnumFacing.SOUTH),2);
        w.setBlockToAir(BASE.west(2));
        ManagerTile m=(ManagerTile)w.getTileEntity(BASE);m.energy.setEnergyStored(80000);m.functions=15;m.inventory.setStackInSlot(0,new ItemStack(IC2Items.getItem("crop_res","fertilizer").getItem(),16,IC2Items.getItem("crop_res","fertilizer").getMetadata()));
        m.inventory.setStackInSlot(3,new ItemStack(Items.WHEAT,16));m.water.fill(new FluidStack(FluidRegistry.WATER,13000),true);m.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(),8000),true);
        m.inventory.setStackInSlot(18,IC2Items.getItem("upgrade","energy_storage"));
        MonitorTile monitor = (MonitorTile)w.getTileEntity(BASE.east(2));
        monitor.inventory.setStackInSlot(0, new ItemStack(CropManager.WIRELESS_RANGE));
        monitor.inventory.setStackInSlot(MonitorTile.WIRELESS_EU_SLOT, new ItemStack(CropManager.WIRELESS_EU));
        ic2.api.crops.CropCard wheat=ic2.api.crops.Crops.instance.getCropCard("ic2","wheat");
        for(int i=0;i<2;i++){BlockPos cp=BASE.north(i+1);w.setBlockToAir(cp);w.setBlockState(cp.down(),Blocks.FARMLAND.getDefaultState(),2);w.setBlockState(cp,IC2Items.getItemAPI().getBlockState("te","crop"),2);ic2.core.crop.TileEntityCrop crop=new ic2.core.crop.TileEntityCrop();w.setTileEntity(cp,crop);crop.setCrop(wheat);crop.setCurrentSize(wheat.getMaxSize());crop.setScanLevel(i==0?4:0);crop.setStatGrowth(20);}
        java.util.List<ic2.api.crops.CropCard> cards=new java.util.ArrayList<>(ic2.api.crops.Crops.instance.getCrops());
        cards.removeIf(card->card==ic2.api.crops.Crops.weed || card==wheat || !"ic2".equals(card.getOwner()));
        cards.sort(java.util.Comparator.comparing(ic2.api.crops.CropCard::getId));
        if(cards.size()<5)throw new AssertionError("Survey fixture needs five IC2 species in addition to wheat");
        for(int i=0;i<5;i++) {
            BlockPos cp=BASE.add(i-2,0,-3);w.setBlockToAir(cp);w.setBlockState(cp.down(),Blocks.FARMLAND.getDefaultState(),2);w.setBlockState(cp,IC2Items.getItemAPI().getBlockState("te","crop"),2);
            ic2.core.crop.TileEntityCrop crop=new ic2.core.crop.TileEntityCrop();w.setTileEntity(cp,crop);crop.setCrop(cards.get(i));crop.setCurrentSize(1);crop.setScanLevel(1);
        }
        for(int i=0;i<700;i++)m.update();
        GrinGrinderTile g=(GrinGrinderTile)w.getTileEntity(BASE.east(6));g.energy.setEnergyStored(16000);g.inventory.setStackInSlot(0,new ItemStack(Items.SPIDER_EYE,16));g.inventory.setStackInSlot(1,new ItemStack(CropManager.GRINDING_BLADE));
        WeedExFabricatorTile mx=(WeedExFabricatorTile)w.getTileEntity(BASE.east(8));mx.energy.setEnergyStored(12000);mx.inventory.setStackInSlot(0,new ItemStack(Items.REDSTONE,32));mx.extract.fill(new FluidStack(CropManager.GRIN_EXTRACT,8000),true);mx.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(),4000),true);
        WeederTile weeder=(WeederTile)w.getTileEntity(BASE.east(10));weeder.gas.fill(new FluidStack(FluidName.biogas.getInstance(),8000),true);
        BlockPos weedPos=BASE.east(10).north();w.setBlockToAir(weedPos);w.setBlockState(weedPos.down(),Blocks.FARMLAND.getDefaultState(),18);
        ic2.core.crop.TileEntityCrop weedCrop=new ic2.core.crop.TileEntityCrop();ic2.core.item.block.ItemBlockTileEntity.placeTeBlock(IC2Items.getItem("te","crop"),player(mc),w,weedPos,EnumFacing.UP,weedCrop);weedCrop.setCrop(ic2.api.crops.Crops.weed);weedCrop.setCurrentSize(2);weedCrop.updateState();
        for(net.minecraft.entity.item.EntityItem drop:w.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,new net.minecraft.util.math.AxisAlignedBB(BASE).grow(30)))drop.setDead();
        ic2.core.block.machine.tileentity.TileEntityCropmatron matron=new ic2.core.block.machine.tileentity.TileEntityCropmatron();if(!ic2.core.item.block.ItemBlockTileEntity.placeTeBlock(IC2Items.getItem("te","cropmatron"),player(mc),w,BASE.west(2),EnumFacing.UP,matron))throw new AssertionError("Native Crop-Matron placement failed");matron.getComponent(ic2.core.block.comp.Energy.class).addEnergy(2000);matron.getWaterTank().fill(new FluidStack(FluidRegistry.WATER,6000),true);matron.getExTank().fill(new FluidStack(FluidName.weed_ex.getInstance(),3000),true);matron.fertilizerSlot.put(IC2Items.getItem("crop_res","fertilizer"));
        EntityPlayerMP p=player(mc);p.inventory.clear();p.inventory.setInventorySlotContents(0,new ItemStack(CropManager.PROBE));for(int i=0;i<5;i++)p.inventory.setInventorySlotContents(i+1,new ItemStack(blocks[i]));p.setPositionAndUpdate(10.5,4,16);
        mc.addScheduledTask(()->ready=true);
    }

    private static void prepareReservoir(WorldServer world) {
        placeReservoir(world, EnumFacing.SOUTH);
    }

    private static AgriculturalReservoirTile placeReservoir(WorldServer world, EnumFacing facing) {
        for (int y = 0; y < 5; y++) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                boolean shell = y == 0 || y == 4 || x != 0 || z != 0;
                world.setBlockState(RESERVOIR_CENTER.add(x, y, z), shell
                        ? CropManager.RESERVOIR_CASING.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
            }
        }
        BlockPos control = RESERVOIR_CENTER.offset(facing).up();
        world.setBlockState(control, CropManager.AGRICULTURAL_RESERVOIR.getDefaultState()
                .withProperty(AgriculturalReservoirBlock.FACING, facing), 18);
        AgriculturalReservoirTile reservoir = (AgriculturalReservoirTile) world.getTileEntity(control);
        reservoir.tryForm();
        if (!reservoir.formed) throw new AssertionError("Client reservoir fixture failed to form");
        reservoir.energy.setEnergyStored(12500);
        reservoir.water.fill(new FluidStack(FluidRegistry.WATER, 12000), true);
        reservoir.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), 24000), true);
        return reservoir;
    }

    private static void captureReservoir(Minecraft mc) throws Exception {
        boolean teardown = stage == RESERVOIR_SHOTS.length - 1;
        BlockPos eastController = RESERVOIR_CENTER.east().up();
        if (age++ == 0) {
            mc.gameSettings.guiScale = stage == 1 ? 1 : 2;
            mc.resize(mc.displayWidth, mc.displayHeight);
            if (stage < 3) {
                mc.getIntegratedServer().addScheduledTask(() -> {
                    WorldServer world = mc.getIntegratedServer().getWorld(0);
                    EntityPlayerMP p = player(mc);
                    p.setPositionAndUpdate(RESERVOIR_CONTROLLER.getX() + .5, 6, RESERVOIR_CONTROLLER.getZ() + 3);
                    p.openGui(CropManager.instance, 15, world, RESERVOIR_CONTROLLER.getX(),
                            RESERVOIR_CONTROLLER.getY(), RESERVOIR_CONTROLLER.getZ());
                });
            } else if (teardown) {
                assertReservoirCasingPart(mc, ReservoirCasingBlock.Part.TOWER);
                mc.getIntegratedServer().addScheduledTask(() ->
                        mc.getIntegratedServer().getWorld(0).setBlockToAir(eastController));
            } else {
                mc.player.closeScreen();
                mc.gameSettings.hideGUI = true;
                EnumFacing face = RESERVOIR_FACES[stage - 3];
                mc.getIntegratedServer().addScheduledTask(() -> {
                    WorldServer world = mc.getIntegratedServer().getWorld(0);
                    placeReservoir(world, face);
                    double x = RESERVOIR_CENTER.getX() + .5 + 8 * face.getXOffset() + 4 * face.rotateYCCW().getXOffset();
                    double z = RESERVOIR_CENTER.getZ() + .5 + 8 * face.getZOffset() + 4 * face.rotateYCCW().getZOffset();
                    float yaw = (float) Math.toDegrees(Math.atan2(x - RESERVOIR_CENTER.getX() - .5, RESERVOIR_CENTER.getZ() + .5 - z));
                    EntityPlayerMP p = player(mc);
                    p.capabilities.isFlying = true;
                    p.sendPlayerAbilities();
                    p.connection.setPlayerLocation(x, 6.8, z, yaw, 12);
                });
            }
        }
        if (stage == 2 && age == 25) {
            if (!(mc.currentScreen instanceof ReservoirGui)) throw new AssertionError("Reservoir GUI failed to open");
            ReservoirContainer container = (ReservoirContainer) mc.player.openContainer;
            container.enchantItem(mc.player, 1);
            mc.playerController.sendEnchantPacket(container.windowId, 1);
        }
        if (age != 45) return;
        if (stage < 3 && !(mc.currentScreen instanceof ReservoirGui)) throw new AssertionError("Missing reservoir GUI");
        if (teardown) {
            if (!mc.world.isAirBlock(eastController)) throw new AssertionError("Reservoir controller was not removed on client");
            assertReservoirCasingPart(mc, ReservoirCasingBlock.Part.CASING);
            AgriculturalReservoirTile stale = new AgriculturalReservoirTile();
            stale.setWorld(mc.world);
            stale.setPos(eastController);
            stale.formed = true;
            stale.center = RESERVOIR_CENTER;
            new ReservoirRenderer().render(stale, 0, 0, 0, 0, -1, 1);
        }
        File out = new File(System.getProperty("cropmanager.qaDir"));
        out.mkdirs();
        ScreenShotHelper.saveScreenshot(out, RESERVOIR_SHOTS[stage] + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        CropManager.log.info("CLIENT_QA_SCREENSHOT {}", RESERVOIR_SHOTS[stage]);
        stage++;
        age = 0;
        if (stage == RESERVOIR_SHOTS.length) {
            CropManager.log.info("CROP_MANAGER_RESERVOIR_CLIENT_QA_PASSED");
            mc.shutdown();
        }
    }

    private static void assertReservoirCasingPart(Minecraft mc, ReservoirCasingBlock.Part expected) {
        net.minecraft.block.state.IBlockState state = mc.world.getBlockState(RESERVOIR_CENTER);
        if (state.getBlock() != CropManager.RESERVOIR_CASING
                || state.getActualState(mc.world, RESERVOIR_CENTER).getValue(ReservoirCasingBlock.PART) != expected) {
            throw new AssertionError("Expected reservoir casing part " + expected);
        }
    }

    private static void checkManual(Minecraft mc) throws Exception {
        if (++age == 2) {
            for (br.cropmanager.guide.ManualCategory category : br.cropmanager.guide.ManualRegistry.getCategories()) {
                assertManualTranslation(category.getTitleKey());
                for (br.cropmanager.guide.ManualEntry entry : category.getEntries()) {
                    assertManualTranslation(entry.getTitleKey());
                    assertManualTranslation(entry.getDescKey());
                    assertManualTranslation(entry.getInfoKey());
                }
            }
            assertManualItemTexture(mc, CropManager.RESERVOIR_CAPACITY, "minecraft:items/bucket_empty");
            assertManualItemTexture(mc, CropManager.RESERVOIR_PUMP, "minecraft:items/redstone_dust");
            mc.player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(CropManager.AGRICULTURAL_MANUAL));
            CropManager.AGRICULTURAL_MANUAL.onItemRightClick(mc.world, mc.player, net.minecraft.util.EnumHand.MAIN_HAND);
            if (!(mc.currentScreen instanceof br.cropmanager.guide.GuiAgriculturalManual)) {
                throw new AssertionError("Main-hand Manual did not open");
            }
            mc.displayGuiScreen(null);
            mc.player.setHeldItem(net.minecraft.util.EnumHand.OFF_HAND, new ItemStack(CropManager.AGRICULTURAL_MANUAL));
            CropManager.AGRICULTURAL_MANUAL.onItemRightClick(mc.world, mc.player, net.minecraft.util.EnumHand.OFF_HAND);
            if (!(mc.currentScreen instanceof br.cropmanager.guide.GuiAgriculturalManual)) {
                throw new AssertionError("Off-hand Manual did not open");
            }
            mc.gameSettings.guiScale = 4;
            ScaledResolution resolution = new ScaledResolution(mc);
            mc.currentScreen.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
        }
        if (age < 2) return;
        GuiScreen manual = mc.currentScreen;
        if (age == 18) {
            saveManualShot(mc, "manual-index");
            manualClick(manual, 164, 16);
            Method typed = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
            typed.setAccessible(true);
            String query = "pt_br".equals(mc.gameSettings.language) ? "manual agricola" : "agricultural manual";
            for (char character : query.toCharArray()) typed.invoke(manual, character, 0);
        }
        if (age == 24) {
            saveManualShot(mc, "manual-search");
            manualClick(manual, 12, 72);
            assertManualPage(manual, "agricultural_manual");
        }
        if (age == 30) {
            manualClick(manual, 261, 162);
        }
        if (age == 36) {
            saveManualShot(mc, "manual-recipe");
            manualClick(manual, 193, 83);
            assertManualPage(manual, "reservoir_casing");
            manualClick(manual, 80, 17);
            assertManualPage(manual, "agricultural_manual");
            if (manualField(manual, "recipeIndex").getInt(manual) != 1) {
                throw new AssertionError("Manual history lost the selected recipe");
            }
            manualClick(manual, 109, 17);
            assertManualPage(manual, "reservoir_casing");
            manualClick(manual, 80, 17);
            manualClick(manual, 12, 17);
            manualClick(manual, 109, 17);
            assertManualPage(manual, null);
            CropManager.log.info("CROP_MANAGER_MANUAL_CLIENT_QA_PASSED {}", mc.gameSettings.language);
            mc.shutdown();
        }
    }

    private static void assertManualTranslation(String key) {
        if (!net.minecraft.client.resources.I18n.hasKey(key)) throw new AssertionError("Missing Manual translation " + key);
    }

    private static void assertManualItemTexture(Minecraft mc, net.minecraft.item.Item item, String expected) {
        String sprite = mc.getRenderItem().getItemModelWithOverrides(new ItemStack(item), mc.world, mc.player)
                .getParticleTexture().getIconName();
        if (!expected.equals(sprite)) throw new AssertionError("Wrong reservoir module sprite: " + sprite);
    }

    private static void manualClick(GuiScreen screen, int x, int y) throws Exception {
        float zoom = manualField(screen, "zoom").getFloat(screen);
        int left = manualField(screen, "left").getInt(screen);
        int top = manualField(screen, "top").getInt(screen);
        Method click = GuiScreen.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
        click.setAccessible(true);
        click.invoke(screen, left + Math.round(x * zoom), top + Math.round(y * zoom), 0);
    }

    private static void assertManualPage(GuiScreen screen, String expected) throws Exception {
        Method page = screen.getClass().getDeclaredMethod("page");
        page.setAccessible(true);
        Object current = page.invoke(screen);
        br.cropmanager.guide.ManualEntry entry = (br.cropmanager.guide.ManualEntry) manualField(current, "entry").get(current);
        if (!java.util.Objects.equals(expected, entry == null ? null : entry.getId())) {
            throw new AssertionError("Expected manual page " + expected + ", found " + (entry == null ? "index" : entry.getId()));
        }
    }

    private static Field manualField(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void saveManualShot(Minecraft mc, String name) {
        File out = new File(System.getProperty("cropmanager.qaDir"));
        out.mkdirs();
        ScreenShotHelper.saveScreenshot(out, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    @SuppressWarnings("unchecked") private static java.util.List<net.minecraft.client.resources.IResourcePack> defaultPacks(Minecraft mc) throws Exception {
        Field f=Minecraft.class.getDeclaredField("defaultResourcePacks");f.setAccessible(true);return (java.util.List<net.minecraft.client.resources.IResourcePack>)f.get(mc);
    }
    private static void show(Minecraft mc) throws Exception {
        if(CONTRAST) {
            if(stage==1){defaultPacks(mc).remove(contrastPack);mc.refreshResources();}
            int expected=stage==0?0xf0f0f0:0x202020;
            if(ManagerGui.ink()!=expected)throw new AssertionError("Resource-pack contrast/reload failed: "+Integer.toHexString(ManagerGui.ink()));
            CropManager.log.info("CLIENT_QA_CONTRAST_PASS {} {}",stage,Integer.toHexString(ManagerGui.ink()));
            mc.displayGuiScreen(null);
            mc.getIntegratedServer().addScheduledTask(()->{
                EntityPlayerMP p=player(mc);BlockPos target=BASE.east(2);p.setPositionAndUpdate(target.getX()+0.5,4,target.getZ()+3);
                ((MonitorTile)p.world.getTileEntity(target)).link(BASE,0);p.openGui(CropManager.instance,2,p.world,target.getX(),target.getY(),target.getZ());
            });
            return;
        }
        if(stage==15){mc.gameSettings.guiScale=1;mc.resize(mc.displayWidth,mc.displayHeight);}
        if(stage==20){mc.gameSettings.guiScale=2;mc.resize(mc.displayWidth,mc.displayHeight);}
        if(stage==31){mc.gameSettings.guiScale=1;mc.resize(mc.displayWidth,mc.displayHeight);}
        if(stage==11 || stage==21){mc.displayGuiScreen(null);mc.player.setPosition(stage==21?18.5:12.5,4,stage==21?12:16);mc.player.rotationYaw=180;mc.player.rotationPitch=15;mc.setIngameNotInFocus();return;}
        mc.displayGuiScreen(null);
        mc.getIntegratedServer().addScheduledTask(()->{
            WorldServer w=mc.getIntegratedServer().getWorld(0);EntityPlayerMP p=player(mc);int index=stage<=3 || stage==12 || stage==15 || stage==28?0:stage==4 || stage==16?2:stage==8 || stage==18?3:stage==9 || stage==14 || stage==19?4:stage==20 || stage==24 || stage==31?5:stage==23?0:1;
            if (stage == 8 || stage == 18) {
                p.setPositionAndUpdate(RESERVOIR_CONTROLLER.getX() + 0.5, 4, RESERVOIR_CONTROLLER.getZ() + 3);
                p.openGui(CropManager.instance, 15, w, RESERVOIR_CONTROLLER.getX(), RESERVOIR_CONTROLLER.getY(), RESERVOIR_CONTROLLER.getZ());
                return;
            }
            BlockPos target=BASE.east(index*2);p.setPositionAndUpdate(target.getX()+0.5,4,target.getZ()+3);
            if(stage==10){p.connection.sendPacket(new net.minecraft.network.play.server.SPacketCloseWindow(p.openContainer.windowId));ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.Readout(FarmTool.cropReadout((ic2.core.crop.TileEntityCrop)w.getTileEntity(BASE.north()))),p);return;}
            if(index==2){ComposterTile c=(ComposterTile)w.getTileEntity(target);c.inventory.setStackInSlot(0,new ItemStack(Items.WHEAT,64));c.inventory.setStackInSlot(1,ItemStack.EMPTY);c.inventory.setStackInSlot(2,IC2Items.getItem("upgrade","overclocker"));c.inventory.setStackInSlot(3,IC2Items.getItem("upgrade","energy_storage"));c.inventory.setStackInSlot(4,IC2Items.getItem("upgrade","transformer"));c.water.fill(new FluidStack(FluidRegistry.WATER,4000),true);c.energy.setEnergyStored(20000);c.processTick();c.progress=c.workTicks/2;}
            if(index==1){MonitorTile m=(MonitorTile)w.getTileEntity(target);m.outputMode=1;m.link(stage==7?BASE.west(2):BASE,0);}
            if(stage==12){ManagerTile m=(ManagerTile)w.getTileEntity(BASE);m.redstoneMode=0;}
            if(stage==13 || stage==22 || stage==25 || stage==26){ManagerTile m=(ManagerTile)w.getTileEntity(BASE);m.redstoneMode=0;m.update();((MonitorTile)w.getTileEntity(target)).refresh();}
            if(stage>=27 && stage<=30) {
                ManagerTile m=(ManagerTile)w.getTileEntity(BASE);m.enabled=true;m.redstoneMode=0;m.automation.mode=0;
                for(int i=0;i<1000 && !m.report.hasKey("Survey");i++)m.update();
                if(m.report.getCompoundTag("Survey").getTagList("Species",10).tagCount()<5)throw new AssertionError("Survey fixture lost planted species before pagination test");
                if(index==1)((MonitorTile)w.getTileEntity(target)).refresh();
            }
            int id=index==0?0:index==1?2:index==2?3:index==3?9:index==4?8:7;p.openGui(CropManager.instance,id,w,target.getX(),target.getY(),target.getZ());
        });
    }
}
