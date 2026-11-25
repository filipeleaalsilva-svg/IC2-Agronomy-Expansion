package br.cropmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = CropManager.ID, value = Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    private static final KeyBinding TOGGLE_SEED_PICKUP = new KeyBinding(
            "key.cropmanager.toggle_seed_pickup", Keyboard.KEY_K, "key.categories.cropmanager");

    @Override public void openGuideBook() {
        Minecraft.getMinecraft().displayGuiScreen(new br.cropmanager.guide.GuiAgriculturalManual());
    }

    @Override public void registerClientResources() {
        FarmGuiStyle.registerPalette();
        ClientRegistry.registerKeyBinding(TOGGLE_SEED_PICKUP);
    }

    @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        while (TOGGLE_SEED_PICKUP.isPressed()) {
            if (mc.currentScreen == null && mc.player != null
                    && (mc.player.getHeldItemMainhand().getItem() == CropManager.PORTABLE_DATABASE
                    || mc.player.getHeldItemOffhand().getItem() == CropManager.PORTABLE_DATABASE)) {
                ManagerNetwork.CHANNEL.sendToServer(new ManagerNetwork.ToggleSeedPickup());
            }
        }
    }
    @SubscribeEvent public static void stitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(CropManager.GRIN_EXTRACT.getStill());
        event.getMap().registerSprite(CropManager.GRIN_EXTRACT.getFlowing());
    }
    @SubscribeEvent public static void models(ModelRegistryEvent event) {
        for (net.minecraft.block.Block block : new net.minecraft.block.Block[] {
                CropManager.BIOGAS_HEAT_BURNER, CropManager.BIOGAS_GENERATOR, CropManager.BIOGAS_GENERATOR_CASING, CropManager.BIOGAS_SPRAYER, CropManager.SPRINKLER_DIAGONAL}) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        net.minecraft.client.renderer.block.statemap.StateMap machineMap =
                new net.minecraft.client.renderer.block.statemap.StateMap.Builder()
                        .ignore(ManagerBlock.NORTH, ManagerBlock.EAST, ManagerBlock.SOUTH, ManagerBlock.WEST, ManagerBlock.UP, ManagerBlock.DOWN).build();
        net.minecraft.client.renderer.block.statemap.StateMap pipeMap =
                new net.minecraft.client.renderer.block.statemap.StateMap.Builder()
                        .ignore(ManagerBlock.FACING, ManagerBlock.ACTIVE).build();
        for (ManagerBlock block : CropManager.BLOCKS) {
            ModelLoader.setCustomStateMapper(block, block == CropManager.THERMAL_PIPE ? pipeMap : machineMap);
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.MANUAL_COMPOSTER), 0, new ModelResourceLocation(CropManager.MANUAL_COMPOSTER.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.RADIOACTIVE_SOIL), 0, new ModelResourceLocation(CropManager.RADIOACTIVE_SOIL.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SPRINKLER_MK1), 0, new ModelResourceLocation(CropManager.SPRINKLER_MK1.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SPRINKLER_MK2), 0, new ModelResourceLocation(CropManager.SPRINKLER_MK2.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SPRINKLER_MK3), 0, new ModelResourceLocation(CropManager.SPRINKLER_MK3.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.FLUID_PIPE), 0, new ModelResourceLocation(CropManager.FLUID_PIPE.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.DATABASE_CHEST), 0, new ModelResourceLocation(CropManager.DATABASE_CHEST.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.AGRICULTURAL_RESERVOIR), 0,
                new ModelResourceLocation(CropManager.AGRICULTURAL_RESERVOIR.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.RESERVOIR_CASING), 0,
                new ModelResourceLocation(CropManager.RESERVOIR_CASING.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.FLUID_TANK), 0,
                new ModelResourceLocation(CropManager.FLUID_TANK.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.FLUID_TANK_CASING), 0,
                new ModelResourceLocation(CropManager.FLUID_TANK_CASING.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.AGRICULTURAL_SILO), 0,
                new ModelResourceLocation(CropManager.AGRICULTURAL_SILO.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SILO_CASING), 0,
                new ModelResourceLocation(CropManager.SILO_CASING.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.POLLINATOR_HIVE), 0,
                new ModelResourceLocation(CropManager.POLLINATOR_HIVE.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.GREENHOUSE_GLASS), 0,
                new ModelResourceLocation(CropManager.GREENHOUSE_GLASS.getRegistryName(), "inventory"));
        for (DecorativeBlock decor : new DecorativeBlock[] {CropManager.GREENHOUSE_FRAME, CropManager.GARDEN_TRELLIS}) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(decor), 0,
                    new ModelResourceLocation(decor.getRegistryName(), "inventory"));
        }
        for (FarmLanternBlock lamp : CropManager.LANTERNS) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(lamp), 0,
                    new ModelResourceLocation(lamp.getRegistryName(), "inventory"));
        }
        for (net.minecraft.block.Block wifi : new net.minecraft.block.Block[] {
                CropManager.EU_WIFI_PLUG, CropManager.EU_WIFI_POINT, CropManager.EU_DISH_ANTENNA}) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(wifi), 0,
                    new ModelResourceLocation(wifi.getRegistryName(), "inventory"));
        }
        for (Item item : new Item[] {CropManager.PROBE, CropManager.WEED_KILLER, CropManager.WIRELESS_RANGE, CropManager.HARVEST_TRANSFER, CropManager.WIRELESS_EU, CropManager.GRINDING_BLADE, CropManager.RESERVOIR_CAPACITY, CropManager.RESERVOIR_PUMP, CropManager.RESERVOIR_RESERVE, CropManager.ORGANIC_COMPOST, CropManager.CATALYTIC_FERTILIZER, CropManager.MUTAGENIC_POWDER, CropManager.NITRO_ESSENCE, CropManager.BIO_FIBER, CropManager.PORTABLE_DATABASE, CropManager.CROSSING_TOOL, CropManager.UPGRADE_LINKER, CropManager.AGRICULTURAL_MANUAL}) ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(MonitorTile.class, new MonitorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(AgriculturalReservoirTile.class, new ReservoirRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(IndustrialTankTile.class, new IndustrialTankRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(AgriculturalSiloTile.class, new AgriculturalSiloRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(BiogasGeneratorTile.class, new BiogasGeneratorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(br.cropmanager.chest.inventory.TileCropDatabaseChest.class, new br.cropmanager.chest.client.CropChestRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(br.cropmanager.chest.inventory.TileCropDatabaseChest250.class, new br.cropmanager.chest.client.CropChestRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(br.cropmanager.chest.inventory.TileCropDatabaseChest125.class, new br.cropmanager.chest.client.CropChestRenderer());
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == CrossingTool.GUI) {
            CrossingToolContainer container = CrossingToolContainer.open(player, x);
            return container == null ? null : new CrossingToolGui(container);
        }
        if (id == 12) {
            br.cropmanager.chest.inventory.PortableCropInventory portable =
                    br.cropmanager.chest.inventory.PortableCropInventory.open(player, x);
            return portable == null ? null : new br.cropmanager.chest.client.GuiCropDatabaseChest(
                    new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(player.inventory, portable));
        }
        if (id == 13) {
            return new br.cropmanager.chest.client.GuiCropDatabaseChest(
                    new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(
                            player.inventory, new br.cropmanager.chest.inventory.TileCropDatabaseChest(), x));
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == DiagonalSprinklerTile.GUI && tile instanceof DiagonalSprinklerTile) return new AgriculturalMachineGui(player.inventory, (FarmTile) tile);
        if (id == BiogasSprayerTile.GUI && tile instanceof BiogasSprayerTile) return new AgriculturalMachineGui(player.inventory, (FarmTile) tile);
        if (id == BiogasGeneratorTile.GUI && tile instanceof BiogasGeneratorTile) return new AgriculturalMachineGui(player.inventory, (BiogasGeneratorTile) tile);
        if (id == 15 && tile instanceof AgriculturalReservoirTile)
            return new ReservoirGui(player.inventory, (AgriculturalReservoirTile) tile);
        if (id == 16 && tile instanceof IndustrialTankTile)
            return new IndustrialTankGui(player.inventory, (IndustrialTankTile) tile);
        if (id == 17 && tile instanceof AgriculturalSiloTile)
            return new AgriculturalSiloGui(player.inventory, (AgriculturalSiloTile) tile);
        if (id == 18 && tile instanceof EuWifiPlugTile || id == 19 && tile instanceof EuWifiPointTile)
            return new EuWifiGui(player.inventory, (FarmTile) tile);
        if (id == 20 && tile instanceof EuDishAntennaTile)
            return new EuDishGui(player.inventory, (EuDishAntennaTile) tile);
        if (id == 11 && tile instanceof br.cropmanager.chest.inventory.TileCropDatabaseChest) {
            return new br.cropmanager.chest.client.GuiCropDatabaseChest(
                    new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(
                            player.inventory, (br.cropmanager.chest.inventory.TileCropDatabaseChest) tile));
        }
        if (id == 0 && tile instanceof ManagerTile) return new ManagerGui(player.inventory, (ManagerTile)tile);
        if (id == 8 && tile instanceof WeedExFabricatorTile) return new EquipmentGui(player.inventory, (FarmTile)tile);
        if (id == 9 && tile instanceof GrinGrinderTile || id == 10 && tile instanceof GrinExtractorTile || id == 14 && tile instanceof SeedAnalyzerTile || id == HydroponicChamberTile.GUI && tile instanceof HydroponicChamberTile) return new EquipmentGui(player.inventory, (FarmTile)tile);
        return (id == 2 && tile instanceof MonitorTile || id == 3 && tile instanceof ComposterTile || id == 7 && tile instanceof WeederTile) ? new EquipmentGui(player.inventory, (FarmTile)tile) : null;
    }
    @Override public void receiveState(ManagerNetwork.State message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.player == null || message.tag == null) return;
            FarmTile tile = FarmTile.openMachine(mc.player);
            if (tile != null && mc.player.openContainer.windowId == message.window && tile.getPos().equals(message.pos)) {
                tile.acceptGuiState(message.tag);
                if (tile instanceof ManagerTile || tile instanceof WeederTile) AreaPreview.refresh(tile);
            }
        });
    }
    @Override public void showReadout(net.minecraft.nbt.NBTTagCompound tag) {
        Minecraft.getMinecraft().addScheduledTask(() -> { if (tag != null) Minecraft.getMinecraft().displayGuiScreen(new ProbeGui(tag)); });
    }
    @Override public void receiveChestSettings(int window, net.minecraft.nbt.NBTTagCompound tag) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null && tag != null && player.openContainer.windowId == window
                    && player.openContainer instanceof br.cropmanager.chest.inventory.ContainerCropDatabaseChest) {
                ((br.cropmanager.chest.inventory.ContainerCropDatabaseChest) player.openContainer).readManagement(tag);
            }
        });
    }
    @Override public void receiveChestView(br.cropmanager.chest.network.ChestViewMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null && message != null && player.openContainer.windowId == message.getWindow()
                    && player.openContainer instanceof br.cropmanager.chest.inventory.ContainerCropDatabaseChest) {
                ((br.cropmanager.chest.inventory.ContainerCropDatabaseChest) player.openContainer).applyView(
                        message.getPage(), message.getPageCount(), message.getOccupied(),
                        message.isPickup(), message.isFavoritesOnly(), message.getRevision(), message.getPositions());
            }
        });
    }
}
