package br.cropmanager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import ic2.api.item.IC2Items;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.apache.logging.log4j.Logger;

/**
 * Ponto de entrada principal do mod IC2: Agronomy Expansion para Minecraft 1.12.2 e IC2 Experimental.
 *
 * <p>Centraliza a instância dos blocos, itens e fluidos do addon, além de gerenciar o registro no
 * Forge, remapeamento de IDs legados ({@code MissingMappings}) e receitas de fabricação.
 */
@Mod(modid = CropManager.ID, name = CropManager.NAME, version = CropManager.VERSION,
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "[" + CropManager.VERSION + "]",
        dependencies = "required-after:ic2@[2.8.222-ex112]")
@Mod.EventBusSubscriber(modid = CropManager.ID)
public final class CropManager {
    public static final String ID = "cropmanager";
    public static final String NAME = "IC2: Agronomy Expansion";
    public static final String VERSION = "0.5.0-alpha11";
    public static final net.minecraft.creativetab.CreativeTabs TAB = new net.minecraft.creativetab.CreativeTabs(ID) {
        @Override public ItemStack createIcon() { return new ItemStack(ADVANCED); }
    };
    @Mod.Instance(ID)
    public static CropManager instance;
    public static Logger log;
    public static final ManagerBlock ADVANCED = new ManagerBlock("advanced_crop_manager", 1);
    public static final ManagerBlock MONITOR = new ManagerBlock("agricultural_monitor", 3);
    public static final ManagerBlock COMPOSTER = new ManagerBlock("electric_composter", 4);
    public static final ManagerBlock WEED_EX_FABRICATOR = new ManagerBlock("weed_ex_fabricator", 9);
    public static final ManagerBlock WEEDER = new ManagerBlock("biogas_weeder", 8);
    public static final ManagerBlock GRIN_GRINDER = new ManagerBlock("grin_grinder", 10);
    public static final ManagerBlock GRIN_EXTRACTOR = new ManagerBlock("grin_extractor", 11);
    public static final AgriculturalMachineBlock SPRINKLER_DIAGONAL = new AgriculturalMachineBlock("sprinkler_diagonal", DiagonalSprinklerTile::new);
    public static final AgriculturalMachineBlock BIOGAS_SPRAYER = new AgriculturalMachineBlock("biogas_sprayer", BiogasSprayerTile::new);
    public static final AgriculturalMachineBlock BIOGAS_GENERATOR =
            new AgriculturalMachineBlock("biogas_generator", BiogasGeneratorTile::new);
    public static final StorageCasingBlock BIOGAS_GENERATOR_CASING = new StorageCasingBlock("biogas_generator_casing");
    public static final AgriculturalMachineBlock BIOGAS_HEAT_BURNER = new AgriculturalMachineBlock("biogas_heat_burner", BiogasHeatBurnerTile::new);
    public static final ManagerBlock THERMAL_PIPE = new ManagerBlock("thermal_pipe", 12);
    public static final FluidPipeBlock FLUID_PIPE = FluidPipeBlock.pipe();
    public static final ManagerBlock SEED_ANALYZER = new ManagerBlock("seed_analyzer", 13);
    public static final ManagerBlock HYDROPONIC_CHAMBER = new ManagerBlock("hydroponic_chamber", 14);
    public static final AgriculturalReservoirBlock AGRICULTURAL_RESERVOIR = new AgriculturalReservoirBlock();
    public static final ReservoirCasingBlock RESERVOIR_CASING = new ReservoirCasingBlock();
    public static final IndustrialTankBlock FLUID_TANK = new IndustrialTankBlock();
    public static final StorageCasingBlock FLUID_TANK_CASING = new StorageCasingBlock("fluid_tank_casing");
    public static final AgriculturalSiloBlock AGRICULTURAL_SILO = new AgriculturalSiloBlock();
    public static final StorageCasingBlock SILO_CASING = new StorageCasingBlock("silo_casing");
    public static final PollinatorHiveBlock POLLINATOR_HIVE = new PollinatorHiveBlock();
    public static final GreenhouseGlassBlock GREENHOUSE_GLASS = new GreenhouseGlassBlock();
    public static final DecorativeBlock GREENHOUSE_FRAME = new DecorativeBlock(DecorativeBlock.Kind.GREENHOUSE_FRAME);
    public static final DecorativeBlock GARDEN_TRELLIS = new DecorativeBlock(DecorativeBlock.Kind.GARDEN_TRELLIS);
    public static final FarmLanternBlock FARM_LANTERN = new FarmLanternBlock(
            "farm_lantern", net.minecraft.block.material.Material.IRON, net.minecraft.block.SoundType.GLASS);
    public static final FarmLanternBlock WOODEN_SCONCE = new FarmLanternBlock(
            "wooden_sconce", net.minecraft.block.material.Material.WOOD, net.minecraft.block.SoundType.WOOD);
    public static final FarmLanternBlock INDUSTRIAL_LANTERN = new FarmLanternBlock(
            "industrial_lantern", net.minecraft.block.material.Material.IRON, net.minecraft.block.SoundType.METAL);
    public static final FarmLanternBlock GLASS_LAMP = new FarmLanternBlock(
            "glass_lamp", net.minecraft.block.material.Material.GLASS, net.minecraft.block.SoundType.GLASS);
    static final FarmLanternBlock[] LANTERNS = {FARM_LANTERN, WOODEN_SCONCE, INDUSTRIAL_LANTERN, GLASS_LAMP};
    public static final EuWifiBlock EU_WIFI_PLUG = new EuWifiBlock(EuWifiBlock.Kind.PLUG);
    public static final EuWifiBlock EU_WIFI_POINT = new EuWifiBlock(EuWifiBlock.Kind.POINT);
    public static final EuDishAntennaBlock EU_DISH_ANTENNA = new EuDishAntennaBlock();
    public static final ManualComposterBlock MANUAL_COMPOSTER = new ManualComposterBlock();
    public static final RadioactiveSoilBlock RADIOACTIVE_SOIL = new RadioactiveSoilBlock();
    public static final SprinklerBlock SPRINKLER_MK1 = new SprinklerBlock("sprinkler_mk1", 1);
    public static final SprinklerBlock SPRINKLER_MK2 = new SprinklerBlock("sprinkler_mk2", 2);
    public static final SprinklerBlock SPRINKLER_MK3 = new SprinklerBlock("sprinkler_mk3", 3);
    public static final br.cropmanager.chest.block.BlockCropDatabaseChest DATABASE_CHEST = new br.cropmanager.chest.block.BlockCropDatabaseChest();
    public static final CrossingTool CROSSING_TOOL = new CrossingTool();
    public static final br.cropmanager.chest.item.ItemPortableCropDatabase PORTABLE_DATABASE = new br.cropmanager.chest.item.ItemPortableCropDatabase();
    public static final br.cropmanager.chest.item.ItemUpgradeLinker UPGRADE_LINKER = new br.cropmanager.chest.item.ItemUpgradeLinker();
    public static final Item GRINDING_BLADE = new GrindingBlade();
    public static final FertilizerItem ORGANIC_COMPOST = new FertilizerItem("organic_compost", FertilizerItem.Kind.ORGANIC);
    public static final FertilizerItem CATALYTIC_FERTILIZER = new FertilizerItem("catalytic_fertilizer", FertilizerItem.Kind.CATALYTIC);
    public static final MutagenicPowderItem MUTAGENIC_POWDER = new MutagenicPowderItem();
    public static final Item NITRO_ESSENCE = new TooltipItem("nitro_essence", TAB, 64, "cropmanager.nitro_essence_help");
    public static final Item BIO_FIBER = new TooltipItem("bio_fiber", TAB, 64, "cropmanager.bio_fiber_help");
    public static final Fluid GRIN_EXTRACT = new Fluid("grin_extract", new ResourceLocation(ID, "blocks/grin_extract_still"), new ResourceLocation(ID, "blocks/grin_extract_flow")).setUnlocalizedName("grin_extract");
    public static final FarmTool PROBE = new FarmTool("agricultural_probe");
    public static final Item WEED_KILLER = new MonitorUpgradeItem(MonitorUpgradeItem.Role.WEED_KILLER);
    public static final Item WIRELESS_RANGE = new MonitorUpgradeItem(MonitorUpgradeItem.Role.WIRELESS_RANGE);
    public static final Item HARVEST_TRANSFER = new MonitorUpgradeItem(MonitorUpgradeItem.Role.HARVEST_TRANSFER);
    /** Up to four stack in the Monitor's wireless energy slot, or link to an IC2 energy storage for any IC2 machine. */
    public static final Item WIRELESS_EU = new WirelessEuUpgrade();
    public static final Item RESERVOIR_CAPACITY = new TooltipItem("reservoir_capacity", TAB, 8, "cropmanager.reservoir_capacity_help");
    public static final Item RESERVOIR_PUMP = new TooltipItem("reservoir_pump", TAB, 1, "cropmanager.reservoir_pump_help");
    public static final Item RESERVOIR_RESERVE = new TooltipItem("reservoir_reserve", TAB, 1, "cropmanager.reservoir_reserve_help");
    public static final Item AGRICULTURAL_MANUAL = new br.cropmanager.guide.ItemAgriculturalManual();
    public static final ManagerBlock[] BLOCKS = {ADVANCED, MONITOR, COMPOSTER, WEEDER, WEED_EX_FABRICATOR, GRIN_GRINDER, GRIN_EXTRACTOR, THERMAL_PIPE, SEED_ANALYZER, HYDROPONIC_CHAMBER};
    @SidedProxy(clientSide = "br.cropmanager.ClientProxy", serverSide = "br.cropmanager.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        ManagerConfig.load(event.getSuggestedConfigurationFile());
        GameRegistry.registerTileEntity(ManagerTile.class, new ResourceLocation(ID, "crop_manager"));
        GameRegistry.registerTileEntity(AdvancedManagerTile.class, new ResourceLocation(ID, "advanced_crop_manager"));
        GameRegistry.registerTileEntity(MonitorTile.class, new ResourceLocation(ID, "agricultural_monitor"));
        GameRegistry.registerTileEntity(ComposterTile.class, new ResourceLocation(ID, "electric_composter"));
        GameRegistry.registerTileEntity(WeedExFabricatorTile.class, new ResourceLocation(ID, "weed_ex_fabricator"));
        GameRegistry.registerTileEntity(WeederTile.class, new ResourceLocation(ID, "biogas_weeder"));
        GameRegistry.registerTileEntity(GrinGrinderTile.class, new ResourceLocation(ID, "grin_grinder"));
        GameRegistry.registerTileEntity(GrinExtractorTile.class, new ResourceLocation(ID, "grin_extractor"));
        GameRegistry.registerTileEntity(DiagonalSprinklerTile.class, new ResourceLocation(ID, "sprinkler_diagonal"));
        GameRegistry.registerTileEntity(BiogasSprayerTile.class, new ResourceLocation(ID, "biogas_sprayer"));
        GameRegistry.registerTileEntity(BiogasGeneratorTile.class, new ResourceLocation(ID, "biogas_generator"));
        GameRegistry.registerTileEntity(BiogasHeatBurnerTile.class, new ResourceLocation(ID, "biogas_heat_burner"));
        GameRegistry.registerTileEntity(ThermalPipeTile.class, new ResourceLocation(ID, "thermal_pipe"));
        GameRegistry.registerTileEntity(FluidPipeTile.class, new ResourceLocation(ID, "fluid_pipe"));
        GameRegistry.registerTileEntity(SeedAnalyzerTile.class, new ResourceLocation(ID, "seed_analyzer"));
        GameRegistry.registerTileEntity(HydroponicChamberTile.class, new ResourceLocation(ID, "hydroponic_chamber"));
        GameRegistry.registerTileEntity(AgriculturalReservoirTile.class, new ResourceLocation(ID, "agricultural_reservoir"));
        GameRegistry.registerTileEntity(ReservoirCasingTile.class, new ResourceLocation(ID, "reservoir_casing"));
        GameRegistry.registerTileEntity(IndustrialTankTile.class, new ResourceLocation(ID, "fluid_tank"));
        GameRegistry.registerTileEntity(AgriculturalSiloTile.class, new ResourceLocation(ID, "agricultural_silo"));
        GameRegistry.registerTileEntity(StorageCasingTile.class, new ResourceLocation(ID, "storage_casing"));
        GameRegistry.registerTileEntity(PollinatorHiveTile.class, new ResourceLocation(ID, "pollinator_hive"));
        GameRegistry.registerTileEntity(ManualComposterTile.class, new ResourceLocation(ID, "manual_composter"));
        GameRegistry.registerTileEntity(SprinklerTile.class, new ResourceLocation(ID, "sprinkler"));
        GameRegistry.registerTileEntity(EuWifiPlugTile.class, new ResourceLocation(ID, "eu_wifi_plug"));
        GameRegistry.registerTileEntity(EuWifiPointTile.class, new ResourceLocation(ID, "eu_wifi_point"));
        GameRegistry.registerTileEntity(EuDishAntennaTile.class, new ResourceLocation(ID, "eu_dish_antenna"));
        GameRegistry.registerTileEntity(LegacyRecoveryTile.Collector.class, new ResourceLocation(ID, "agricultural_collector"));
        GameRegistry.registerTileEntity(LegacyRecoveryTile.Sorter.class, new ResourceLocation(ID, "seed_classifier"));
        GameRegistry.registerTileEntity(LegacyRecoveryTile.Bench.class, new ResourceLocation(ID, "seed_workbench"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.LegacyModularChestTile.class, new ResourceLocation(ID, "modular_chest"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest.class, new ResourceLocation(ID, "chest"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest250.class, new ResourceLocation(ID, "chest_250"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest125.class, new ResourceLocation(ID, "chest_125"));
        FluidRegistry.registerFluid(GRIN_EXTRACT);
        ManagerNetwork.init();
        proxy.registerClientResources();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
    }

    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS);
        event.getRegistry().registerAll(BIOGAS_HEAT_BURNER, BIOGAS_GENERATOR, BIOGAS_GENERATOR_CASING, BIOGAS_SPRAYER, SPRINKLER_DIAGONAL);
        event.getRegistry().register(AGRICULTURAL_RESERVOIR);
        event.getRegistry().register(RESERVOIR_CASING);
        event.getRegistry().register(FLUID_TANK);
        event.getRegistry().register(FLUID_TANK_CASING);
        event.getRegistry().register(AGRICULTURAL_SILO);
        event.getRegistry().register(SILO_CASING);
        event.getRegistry().register(POLLINATOR_HIVE);
        event.getRegistry().register(GREENHOUSE_GLASS);
        event.getRegistry().register(MANUAL_COMPOSTER);
        event.getRegistry().register(RADIOACTIVE_SOIL);
        event.getRegistry().register(SPRINKLER_MK1);
        event.getRegistry().register(SPRINKLER_MK2);
        event.getRegistry().register(SPRINKLER_MK3);
        event.getRegistry().register(FLUID_PIPE);
        event.getRegistry().register(DATABASE_CHEST);
        event.getRegistry().registerAll(LANTERNS);
        event.getRegistry().registerAll(GREENHOUSE_FRAME, GARDEN_TRELLIS,
                EU_WIFI_PLUG, EU_WIFI_POINT, EU_DISH_ANTENNA);
    }
    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) {
        for (ManagerBlock block : BLOCKS) event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        for (net.minecraft.block.Block block : new net.minecraft.block.Block[] {
                BIOGAS_HEAT_BURNER, BIOGAS_GENERATOR, BIOGAS_GENERATOR_CASING, BIOGAS_SPRAYER, SPRINKLER_DIAGONAL}) {
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }
        event.getRegistry().register(new ItemBlock(AGRICULTURAL_RESERVOIR).setRegistryName(AGRICULTURAL_RESERVOIR.getRegistryName()));
        event.getRegistry().register(new ItemBlock(RESERVOIR_CASING).setRegistryName(RESERVOIR_CASING.getRegistryName()));
        event.getRegistry().register(new ItemBlock(FLUID_TANK).setRegistryName(FLUID_TANK.getRegistryName()));
        event.getRegistry().register(new ItemBlock(FLUID_TANK_CASING).setRegistryName(FLUID_TANK_CASING.getRegistryName()));
        event.getRegistry().register(new ItemBlock(AGRICULTURAL_SILO).setRegistryName(AGRICULTURAL_SILO.getRegistryName()));
        event.getRegistry().register(new ItemBlock(SILO_CASING).setRegistryName(SILO_CASING.getRegistryName()));
        event.getRegistry().register(new ItemBlock(POLLINATOR_HIVE).setRegistryName(POLLINATOR_HIVE.getRegistryName()));
        event.getRegistry().register(new ItemBlock(GREENHOUSE_GLASS).setRegistryName(GREENHOUSE_GLASS.getRegistryName()));
        event.getRegistry().register(new ItemBlock(GREENHOUSE_FRAME).setRegistryName(GREENHOUSE_FRAME.getRegistryName()));
        event.getRegistry().register(new ItemBlock(GARDEN_TRELLIS).setRegistryName(GARDEN_TRELLIS.getRegistryName()));
        for (FarmLanternBlock lamp : LANTERNS) {
            event.getRegistry().register(new ItemBlock(lamp).setRegistryName(lamp.getRegistryName()));
        }
        event.getRegistry().register(new ItemBlock(EU_WIFI_PLUG).setRegistryName(EU_WIFI_PLUG.getRegistryName()));
        event.getRegistry().register(new ItemBlock(EU_WIFI_POINT).setRegistryName(EU_WIFI_POINT.getRegistryName()));
        event.getRegistry().register(new ItemBlock(EU_DISH_ANTENNA).setRegistryName(EU_DISH_ANTENNA.getRegistryName()));
        event.getRegistry().register(new ItemBlock(MANUAL_COMPOSTER).setRegistryName(MANUAL_COMPOSTER.getRegistryName()));
        event.getRegistry().register(new ItemBlock(RADIOACTIVE_SOIL).setRegistryName(RADIOACTIVE_SOIL.getRegistryName()));
        event.getRegistry().register(new ItemBlock(SPRINKLER_MK1).setRegistryName(SPRINKLER_MK1.getRegistryName()));
        event.getRegistry().register(new ItemBlock(SPRINKLER_MK2).setRegistryName(SPRINKLER_MK2.getRegistryName()));
        event.getRegistry().register(new ItemBlock(SPRINKLER_MK3).setRegistryName(SPRINKLER_MK3.getRegistryName()));
        event.getRegistry().register(new ItemBlock(FLUID_PIPE).setRegistryName(FLUID_PIPE.getRegistryName()));
        event.getRegistry().register(new ItemBlock(DATABASE_CHEST).setRegistryName(DATABASE_CHEST.getRegistryName()));
        event.getRegistry().register(PROBE);
        event.getRegistry().register(WEED_KILLER);
        event.getRegistry().register(WIRELESS_RANGE);
        event.getRegistry().register(HARVEST_TRANSFER);
        event.getRegistry().register(WIRELESS_EU);
        event.getRegistry().register(GRINDING_BLADE);
        event.getRegistry().register(RESERVOIR_CAPACITY);
        event.getRegistry().register(RESERVOIR_PUMP);
        event.getRegistry().register(RESERVOIR_RESERVE);
        event.getRegistry().register(ORGANIC_COMPOST);
        event.getRegistry().register(CATALYTIC_FERTILIZER);
        event.getRegistry().register(MUTAGENIC_POWDER);
        event.getRegistry().register(NITRO_ESSENCE);
        event.getRegistry().register(BIO_FIBER);
        event.getRegistry().register(PORTABLE_DATABASE);
        event.getRegistry().register(CROSSING_TOOL);
        event.getRegistry().register(UPGRADE_LINKER);
        event.getRegistry().register(AGRICULTURAL_MANUAL);
    }
    @SubscribeEvent public static void crops(ic2.api.crops.Crops.CropRegisterEvent event) {
        br.cropmanager.crop.CropRegistry.registerAll();
    }
    @SubscribeEvent public static void missingBlocks(RegistryEvent.MissingMappings<Block> event) {
        for(RegistryEvent.MissingMappings.Mapping<Block> m:event.getMappings()){
            if("cropdatabasechest".equals(m.key.getNamespace())){
                String p=m.key.getPath();
                if(p.equals("crop_database_chest")||p.equals("modular_crop_database")||p.equals("crop_database_chest_250")||p.equals("crop_database_chest_125"))m.remap(DATABASE_CHEST);
                continue;
            }
            if(!ID.equals(m.key.getNamespace()))continue;
            String name=m.key.getPath();if(name.equals("crop_manager"))m.remap(ADVANCED);
            else if(name.equals("agricultural_collector") || name.equals("seed_classifier") || name.equals("seed_workbench"))m.remap(COMPOSTER);
        }
    }
    @SubscribeEvent public static void missingItems(RegistryEvent.MissingMappings<Item> event) {
        for(RegistryEvent.MissingMappings.Mapping<Item> m:event.getMappings()){
            if("cropdatabasechest".equals(m.key.getNamespace())){
                String p=m.key.getPath();
                if(p.equals("crop_database_chest")||p.equals("modular_crop_database")||p.equals("crop_database_chest_250")||p.equals("crop_database_chest_125"))m.remap(Item.getItemFromBlock(DATABASE_CHEST));
                else if(p.equals("portable_crop_database"))m.remap(PORTABLE_DATABASE);
                continue;
            }
            if(!ID.equals(m.key.getNamespace()))continue;
            String name=m.key.getPath();if(name.equals("crop_manager"))m.remap(Item.getItemFromBlock(ADVANCED));
            else if(name.equals("configuration_card"))m.remap(net.minecraft.init.Items.PAPER);
            else if(name.equals("agricultural_notebook") || name.equals("reservoir_guide"))m.remap(AGRICULTURAL_MANUAL);
            else if(name.equals("compost"))m.remap(Item.getItemFromBlock(net.minecraft.init.Blocks.DIRT));
            else if(name.equals("agricultural_collector") || name.equals("seed_classifier") || name.equals("seed_workbench"))m.remap(Item.getItemFromBlock(COMPOSTER));
        }
    }
    @SubscribeEvent public static void recipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(
                new ResourceLocation(ID, "agriculture"), new ItemStack(AGRICULTURAL_MANUAL),
                net.minecraft.init.Items.BOOK, RESERVOIR_CASING).setRegistryName(ID, "reservoir_guide"));
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(
                new ResourceLocation(ID, "agriculture"), new ItemStack(AGRICULTURAL_MANUAL),
                net.minecraft.init.Items.BOOK, ORGANIC_COMPOST).setRegistryName(ID, "agricultural_manual_starter"));
        ItemStack harvester = required("te", "crop_harvester");
        ItemStack matron = required("te", "cropmatron");
        ItemStack circuit = required("crafting", "advanced_circuit");
        ItemStack iridium = required("crafting", "iridium");
        ItemStack basicCircuit = required("crafting", "circuit");
        shapeless(event, RESERVOIR_CAPACITY, RESERVOIR_CASING, net.minecraft.init.Items.BUCKET,
                net.minecraft.init.Items.BUCKET, basicCircuit);
        shapeless(event, RESERVOIR_PUMP, net.minecraft.init.Blocks.PISTON, net.minecraft.init.Items.BUCKET,
                THERMAL_PIPE, basicCircuit);
        shapeless(event, RESERVOIR_RESERVE, RESERVOIR_CASING, THERMAL_PIPE, net.minecraft.init.Blocks.REDSTONE_TORCH);
        recipe(event, "advanced_crop_manager", new ItemStack(ADVANCED), "HIC", "CMC", "III", 'H', harvester, 'I', iridium, 'C', circuit, 'M', matron);
        recipe(event, "weed_killer_upgrade", new ItemStack(WEED_KILLER), "RGR", "CEC", "RGR", 'R', net.minecraft.init.Items.REDSTONE, 'G', net.minecraft.init.Items.GLOWSTONE_DUST, 'C', circuit, 'E', IC2Items.getItem("upgrade", "overclocker"));
        recipe(event, "wireless_range_upgrade", new ItemStack(WIRELESS_RANGE), " E ", "ECE", " R ", 'E', net.minecraft.init.Items.ENDER_PEARL, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "agricultural_monitor", new ItemStack(MONITOR), "IGI", "CIC", "IRI", 'I', iridium, 'G', net.minecraft.init.Blocks.GLASS, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "agricultural_reservoir", new ItemStack(AGRICULTURAL_RESERVOIR), "GBG", "BCB", "III",
                'G', net.minecraft.init.Blocks.GLASS, 'B', net.minecraft.init.Items.BUCKET,
                'C', basicCircuit, 'I', net.minecraft.init.Items.IRON_INGOT);
        recipe(event, "reservoir_casing", new ItemStack(RESERVOIR_CASING, 8), "III", "PPP", "III",
                'I', net.minecraft.init.Items.IRON_INGOT, 'P', "plankWood");
        recipe(event, "fluid_tank_casing", new ItemStack(FLUID_TANK_CASING, 8), "IGI", "I I", "IGI",
                'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS_PANE);
        recipe(event, "fluid_tank", new ItemStack(FLUID_TANK), "IGI", "BGB", "IRI",
                'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS,
                'B', net.minecraft.init.Items.BUCKET, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "silo_casing", new ItemStack(SILO_CASING, 8), "IBI", "IPI", "IBI",
                'I', net.minecraft.init.Items.IRON_INGOT, 'B', net.minecraft.init.Blocks.IRON_BARS,
                'P', "plankWood");
        recipe(event, "agricultural_silo", new ItemStack(AGRICULTURAL_SILO), "IHI", "ICI", "IRI",
                'I', net.minecraft.init.Items.IRON_INGOT, 'H', net.minecraft.init.Blocks.HOPPER,
                'C', "chestWood", 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "agricultural_probe", new ItemStack(PROBE), " G ", "ICI", " R ", 'G', net.minecraft.init.Blocks.GLASS, 'I', net.minecraft.init.Items.IRON_INGOT, 'C', basicCircuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "biogas_weeder", new ItemStack(WEEDER), "IBI", "FCF", "III", 'I', net.minecraft.init.Items.IRON_INGOT, 'B', net.minecraft.init.Items.BUCKET, 'F', net.minecraft.init.Blocks.FURNACE, 'C', basicCircuit);
        recipe(event, "electric_composter", new ItemStack(COMPOSTER), "III", "FCF", "III", 'I', net.minecraft.init.Items.IRON_INGOT, 'F', net.minecraft.init.Blocks.FURNACE, 'C', basicCircuit);
        recipe(event, "weed_ex_fabricator", new ItemStack(WEED_EX_FABRICATOR), "IGI", "GCG", "IRI", 'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "grinding_blade", new ItemStack(GRINDING_BLADE), "II", "FS", " I", 'I', net.minecraft.init.Items.IRON_INGOT, 'F', net.minecraft.init.Items.FLINT, 'S', net.minecraft.init.Items.STICK);
        recipe(event, "grin_grinder", new ItemStack(GRIN_GRINDER), "III", "ICI", "IRI", 'I', net.minecraft.init.Items.IRON_INGOT, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "grin_extractor", new ItemStack(GRIN_EXTRACTOR), "IGI", "FCF", "IRI", 'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS, 'F', net.minecraft.init.Blocks.FURNACE, 'C', basicCircuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "sprinkler_diagonal", new ItemStack(SPRINKLER_DIAGONAL), " S ", "SCS", " S ",
                'S', SPRINKLER_MK1, 'C', basicCircuit);
        recipe(event, "biogas_sprayer", new ItemStack(BIOGAS_SPRAYER), "WSB",
                'W', WEEDER, 'S', SPRINKLER_MK1, 'B', net.minecraft.init.Items.BUCKET);
        recipe(event, "biogas_generator", new ItemStack(BIOGAS_GENERATOR), "GCI", "II ",
                'I', required("plate", "iron"), 'C', basicCircuit, 'G', required("te", "semifluid_generator"));
        recipe(event, "biogas_generator_casing", new ItemStack(BIOGAS_GENERATOR_CASING, 4), "II", "II",
                'I', required("plate", "iron"));
        recipe(event, "biogas_heat_burner", new ItemStack(BIOGAS_HEAT_BURNER), "IFI", " T ",
                'I', net.minecraft.init.Items.IRON_INGOT, 'F', net.minecraft.init.Blocks.FURNACE, 'T', THERMAL_PIPE);
        recipe(event, "thermal_pipe", new ItemStack(THERMAL_PIPE, 8), "III", "ICI", "III", 'I', net.minecraft.init.Items.IRON_INGOT, 'C', required("crafting", "heat_conductor"));
        ItemStack fluidCell = IC2Items.getItem("fluid_cell");
        Object pipeCore = (fluidCell != null && !fluidCell.isEmpty()) ? fluidCell : net.minecraft.init.Items.BUCKET;
        recipe(event, "fluid_pipe", new ItemStack(FLUID_PIPE, 8), "III", "GCG", "III", 'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS, 'C', pipeCore);
        recipe(event, "harvest_transfer_upgrade", new ItemStack(HARVEST_TRANSFER), "IDI", "CEC", "IDI", 'I', iridium, 'D', net.minecraft.init.Items.DIAMOND, 'C', circuit, 'E', net.minecraft.init.Items.ENDER_PEARL);
        recipe(event, "wireless_eu_upgrade", new ItemStack(WIRELESS_EU), "ERE", "ASA", "ETE", 'E', net.minecraft.init.Items.ENDER_PEARL, 'R', net.minecraft.init.Items.REDSTONE, 'A', circuit, 'S', required("upgrade", "energy_storage"), 'T', required("upgrade", "transformer"));
        recipe(event, "upgrade_linker", new ItemStack(UPGRADE_LINKER), " E ", "ICI", " R ", 'E', net.minecraft.init.Items.ENDER_PEARL, 'I', net.minecraft.init.Items.IRON_INGOT, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "manual_composter", new ItemStack(MANUAL_COMPOSTER), "P P", "PIP", "ISI", 'P', "plankWood", 'I', net.minecraft.init.Items.IRON_INGOT, 'S', "slabWood");
        recipe(event, "pollinator_hive", new ItemStack(POLLINATOR_HIVE), "PPP", "FFF", "PPP",
                'P', "plankWood", 'F', net.minecraft.init.Blocks.YELLOW_FLOWER);
        recipe(event, "greenhouse_glass", new ItemStack(GREENHOUSE_GLASS, 8), "GGG", "GIG", "GGG",
                'G', net.minecraft.init.Blocks.GLASS, 'I', net.minecraft.init.Items.IRON_INGOT);
        recipe(event, "greenhouse_frame", new ItemStack(GREENHOUSE_FRAME, 8), "ISI", "S S", "ISI",
                'I', net.minecraft.init.Items.IRON_INGOT, 'S', "stickWood");
        recipe(event, "garden_trellis", new ItemStack(GARDEN_TRELLIS, 4), "S S", "SSS", "S S",
                'S', "stickWood");
        recipe(event, "crossing_tool", new ItemStack(CROSSING_TOOL), "SCC",
                'S', net.minecraft.init.Items.SHEARS, 'C', new ItemStack(ic2.core.ref.ItemName.crop_stick.getInstance()));
        recipe(event, "farm_lantern", new ItemStack(FARM_LANTERN, 4), " I ", "IGI", " I ",
                'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Items.GLOWSTONE_DUST);
        recipe(event, "wooden_sconce", new ItemStack(WOODEN_SCONCE), "ML",
                'M', "plankWood", 'L', FARM_LANTERN);
        recipe(event, "industrial_lantern", new ItemStack(INDUSTRIAL_LANTERN), "ML",
                'M', net.minecraft.init.Blocks.IRON_BARS, 'L', FARM_LANTERN);
        recipe(event, "glass_lamp", new ItemStack(GLASS_LAMP), "ML",
                'M', net.minecraft.init.Blocks.GLASS, 'L', FARM_LANTERN);
        recipe(event, "eu_wifi_plug", new ItemStack(EU_WIFI_PLUG), "ERE", "ICI", "IWI",
                'E', net.minecraft.init.Items.ENDER_PEARL, 'R', net.minecraft.init.Items.REDSTONE,
                'I', net.minecraft.init.Items.IRON_INGOT, 'C', circuit, 'W', WIRELESS_EU);
        recipe(event, "eu_wifi_point", new ItemStack(EU_WIFI_POINT), "IWI", "ICI", "ERE",
                'E', net.minecraft.init.Items.ENDER_PEARL, 'R', net.minecraft.init.Items.REDSTONE,
                'I', net.minecraft.init.Items.IRON_INGOT, 'C', circuit, 'W', WIRELESS_EU);
        recipe(event, "eu_dish_antenna", new ItemStack(EU_DISH_ANTENNA), "I I", "RER", "ICI",
                'I', net.minecraft.init.Items.IRON_INGOT, 'R', net.minecraft.init.Items.REDSTONE,
                'E', net.minecraft.init.Items.ENDER_PEARL, 'C', circuit);
        ItemStack cropnalyzer = IC2Items.getItem("cropnalyzer");
        if (cropnalyzer == null || cropnalyzer.isEmpty()) cropnalyzer = basicCircuit;
        recipe(event, "seed_analyzer", new ItemStack(SEED_ANALYZER), "IGI", "CAC", "IRI", 'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS, 'C', basicCircuit, 'A', cropnalyzer, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "hydroponic_chamber", new ItemStack(HYDROPONIC_CHAMBER), "GGG", "ICI", "RBR", 'G', GREENHOUSE_GLASS, 'I', net.minecraft.init.Items.IRON_INGOT, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE, 'B', net.minecraft.init.Items.WATER_BUCKET);
        ItemStack grinPowder = required("crop_res", "grin_powder");
        recipe(event, "catalytic_fertilizer", new ItemStack(CATALYTIC_FERTILIZER, 2), " C ", "NGN", " C ", 'C', ORGANIC_COMPOST, 'N', NITRO_ESSENCE, 'G', grinPowder);
        recipe(event, "mutagenic_powder", new ItemStack(MUTAGENIC_POWDER, 2), " G ", "NPN", " G ", 'G', net.minecraft.init.Items.GLOWSTONE_DUST, 'N', NITRO_ESSENCE, 'P', grinPowder);
        ItemStack plantBall = IC2Items.getItem("crafting", "plant_ball");
        if (plantBall != null && !plantBall.isEmpty()) {
            recipe(event, "bio_fiber_plant_ball", plantBall.copy(), "FF", "FF", 'F', BIO_FIBER);
        }
        ItemStack uraniumCore = IC2Items.getItem("crushed", "uranium");
        if (uraniumCore == null || uraniumCore.isEmpty()) uraniumCore = IC2Items.getItem("nuclear", "uranium_238");
        if (uraniumCore != null && !uraniumCore.isEmpty()) {
            recipe(event, "radioactive_soil", new ItemStack(RADIOACTIVE_SOIL, 2), "DCD", "CUC", "DCD",
                    'D', net.minecraft.init.Blocks.DIRT, 'C', ORGANIC_COMPOST, 'U', uraniumCore);
        }
        recipe(event, "sprinkler_mk1", new ItemStack(SPRINKLER_MK1), " I ", "IBI", "RIR",
                'I', net.minecraft.init.Items.IRON_INGOT, 'B', net.minecraft.init.Items.WATER_BUCKET, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "sprinkler_mk2", new ItemStack(SPRINKLER_MK2), "G G", "BMB", "RCR",
                'G', net.minecraft.init.Items.GOLD_INGOT, 'B', net.minecraft.init.Items.WATER_BUCKET, 'M', SPRINKLER_MK1, 'R', net.minecraft.init.Items.REDSTONE, 'C', basicCircuit);
        recipe(event, "sprinkler_mk3", new ItemStack(SPRINKLER_MK3), "D D", "BMB", "RCR",
                'D', net.minecraft.init.Items.DIAMOND, 'B', net.minecraft.init.Items.WATER_BUCKET, 'M', SPRINKLER_MK2, 'R', net.minecraft.init.Items.REDSTONE, 'C', circuit);
        event.getRegistry().register(br.cropmanager.chest.crafting.CropChestRecipe.create().setRegistryName(ID, "crop_database_chest"));
        event.getRegistry().register(br.cropmanager.chest.crafting.PortableCropRecipe.create().setRegistryName(ID, "portable_crop_database"));
        event.getRegistry().register(new br.cropmanager.chest.crafting.PortableLinkRecipe().setRegistryName(ID, "portable_database_link"));
    }
    private static void shapeless(RegistryEvent.Register<IRecipe> event, Item result, Object... ingredients) {
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(
                new ResourceLocation(ID, "agriculture"), new ItemStack(result), ingredients)
                .setRegistryName(result.getRegistryName()));
    }

    private static void recipe(RegistryEvent.Register<IRecipe> event, String name, ItemStack output, Object... shape) {
        event.getRegistry().register(new ShapedOreRecipe(new ResourceLocation(ID, "agriculture"), output, shape).setRegistryName(ID, name));
    }
    private static ItemStack required(String item, String variant) {
        ItemStack stack = IC2Items.getItem(item, variant);
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("Missing IC2 ingredient: " + item + "/" + variant);
        log.info("Recipe ingredient {}/{} = {}:{}", item, variant, stack.getItem().getRegistryName(), stack.getMetadata());
        return stack;
    }
}
