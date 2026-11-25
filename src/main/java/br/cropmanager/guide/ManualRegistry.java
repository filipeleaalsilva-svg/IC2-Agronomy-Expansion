package br.cropmanager.guide;

import br.cropmanager.CropManager;
import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.CropRegistry;
import ic2.api.item.IC2Items;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class ManualRegistry {
    private static final List<ManualCategory> CATEGORIES = new ArrayList<>();
    private static final Map<String, ManualEntry> ENTRIES_BY_ID = new LinkedHashMap<>();
    private static boolean initialized = false;

    private ManualRegistry() {}

    public static synchronized void init() {
        if (initialized) return;
        ItemStack cropStick = ic2("crop_stick", null, new ItemStack(Items.STICK));

        ManualCategory catCrops = new ManualCategory("crops", "cropmanager.guide.cat.crops", cropStick.copy());

        ManualEntry eCropSticks = entry("crop_sticks", "crops", cropStick,
                "cropmanager.guide.crop_sticks.title", "cropmanager.guide.crop_sticks.desc", "cropmanager.guide.crop_sticks.info");
        eCropSticks.withIc2Recipes();
        catCrops.addEntry(eCropSticks);

        ManualEntry eProbe = entry("agricultural_probe", "crops", new ItemStack(CropManager.PROBE),
                "cropmanager.guide.agricultural_probe.title", "cropmanager.guide.agricultural_probe.desc", "cropmanager.guide.agricultural_probe.info");
        eProbe.withRecipes("agricultural_probe");
        catCrops.addEntry(eProbe);

        ManualEntry eAnalyzer = entry("seed_analyzer", "crops", new ItemStack(CropManager.SEED_ANALYZER),
                "cropmanager.guide.seed_analyzer.title", "cropmanager.guide.seed_analyzer.desc", "cropmanager.guide.seed_analyzer.info");
        eAnalyzer.withRecipes("seed_analyzer");
        catCrops.addEntry(eAnalyzer);
        addIc2Entry(catCrops, "cropnalyzer", IC2Items.getItem("cropnalyzer"));

        ManualEntry eRadioactive = entry("radioactive_soil", "crops", new ItemStack(CropManager.RADIOACTIVE_SOIL),
                "cropmanager.guide.radioactive_soil.title", "cropmanager.guide.radioactive_soil.desc", "cropmanager.guide.radioactive_soil.info");
        eRadioactive.withRecipes("radioactive_soil");
        catCrops.addEntry(eRadioactive);

        ManualEntry eGlass = entry("greenhouse_glass", "crops", new ItemStack(CropManager.GREENHOUSE_GLASS),
                "cropmanager.guide.greenhouse_glass.title", "cropmanager.guide.greenhouse_glass.desc", "cropmanager.guide.greenhouse_glass.info");
        eGlass.withRecipes("greenhouse_glass");
        catCrops.addEntry(eGlass);
        catCrops.addEntry(entry("greenhouse_frame", "crops", new ItemStack(CropManager.GREENHOUSE_FRAME),
                "tile.cropmanager.greenhouse_frame.name", "cropmanager.greenhouse_frame_help", "cropmanager.guide.greenhouse_frame.info")
                .withRecipes("greenhouse_frame"));
        catCrops.addEntry(entry("garden_trellis", "crops", new ItemStack(CropManager.GARDEN_TRELLIS),
                "tile.cropmanager.garden_trellis.name", "cropmanager.garden_trellis_help", "cropmanager.guide.garden_trellis.info")
                .withRecipes("garden_trellis"));
        catCrops.addEntry(entry("farm_lantern", "crops", new ItemStack(CropManager.FARM_LANTERN),
                "tile.cropmanager.farm_lantern.name", "cropmanager.farm_lantern_help", "cropmanager.guide.farm_lantern.info")
                .withRecipes("farm_lantern"));

        catCrops.addEntry(entry("crossing_tool", "crops", new ItemStack(CropManager.CROSSING_TOOL),
                "item.cropmanager.crossing_tool.name", "cropmanager.crossing_tool.help", "cropmanager.guide.crossing_tool.info")
                .withRecipes("crossing_tool"));
        for (br.cropmanager.FarmLanternBlock lamp : new br.cropmanager.FarmLanternBlock[] {
                CropManager.WOODEN_SCONCE, CropManager.INDUSTRIAL_LANTERN, CropManager.GLASS_LAMP}) {
            String name = lamp.getRegistryName().getPath();
            catCrops.addEntry(entry(name, "crops", new ItemStack(lamp),
                    "tile.cropmanager." + name + ".name", "cropmanager." + name + "_help",
                    "cropmanager.guide.mounted_lights.info").withRecipes(name));
        }

        // Custom crops
        for (BaseCropCard card : CropRegistry.ALL) {
            String crop = card.getId();
            ManualEntry cEntry = entry("crop_" + crop, "crops", cropStick.copy(),
                    "cropmanager.guide.crop." + crop + ".title",
                    "cropmanager.guide.crop." + crop + ".desc",
                    "cropmanager.guide.crop." + crop + ".info");
            catCrops.addEntry(cEntry);
        }

        CATEGORIES.add(catCrops);

        // ==========================================
        // 2. MÁQUINAS & AUTOMAÇÃO (machines)
        // ==========================================
        ManualCategory catMachines = new ManualCategory("machines", "cropmanager.guide.cat.machines", new ItemStack(CropManager.ADVANCED));

        ManualEntry eAdvanced = entry("advanced_crop_manager", "machines", new ItemStack(CropManager.ADVANCED),
                "cropmanager.guide.advanced_crop_manager.title", "cropmanager.guide.advanced_crop_manager.desc", "cropmanager.guide.advanced_crop_manager.info");
        eAdvanced.withRecipes("advanced_crop_manager");
        catMachines.addEntry(eAdvanced);
        addIc2Entry(catMachines, "crop_harvester", IC2Items.getItem("te", "crop_harvester"));
        addIc2Entry(catMachines, "cropmatron", IC2Items.getItem("te", "cropmatron"));

        ManualEntry eMonitor = entry("agricultural_monitor", "machines", new ItemStack(CropManager.MONITOR),
                "cropmanager.guide.agricultural_monitor.title", "cropmanager.guide.agricultural_monitor.desc", "cropmanager.guide.agricultural_monitor.info");
        eMonitor.withRecipes("agricultural_monitor");
        catMachines.addEntry(eMonitor);

        ManualEntry eWeeder = entry("biogas_weeder", "machines", new ItemStack(CropManager.WEEDER),
                "cropmanager.guide.biogas_weeder.title", "cropmanager.guide.biogas_weeder.desc", "cropmanager.guide.biogas_weeder.info");
        eWeeder.withRecipes("biogas_weeder");
        catMachines.addEntry(eWeeder);

        ManualEntry eComposter = entry("electric_composter", "machines", new ItemStack(CropManager.COMPOSTER),
                "cropmanager.guide.electric_composter.title", "cropmanager.guide.electric_composter.desc", "cropmanager.guide.electric_composter.info");
        eComposter.withRecipes("electric_composter");
        catMachines.addEntry(eComposter);

        ManualEntry eThermal = entry("thermal_pipe", "machines", new ItemStack(CropManager.THERMAL_PIPE),
                "cropmanager.guide.thermal_pipe.title", "cropmanager.guide.thermal_pipe.desc", "cropmanager.guide.thermal_pipe.info");
        eThermal.withRecipes("thermal_pipe");
        catMachines.addEntry(eThermal);

        catMachines.addEntry(entry("biogas_heat_burner", "machines", new ItemStack(CropManager.BIOGAS_HEAT_BURNER),
                "tile.cropmanager.biogas_heat_burner.name", "cropmanager.guide.biogas_heat_burner.desc",
                "cropmanager.guide.biogas_heat_burner.info").withRecipes("biogas_heat_burner"));

        catMachines.addEntry(entry("biogas_generator", "machines", new ItemStack(CropManager.BIOGAS_GENERATOR),
                "tile.cropmanager.biogas_generator.name", "cropmanager.guide.biogas_generator.desc",
                "cropmanager.guide.biogas_generator.info").withRecipes("biogas_generator"));

        catMachines.addEntry(entry("biogas_generator_casing", "machines", new ItemStack(CropManager.BIOGAS_GENERATOR_CASING),
                "tile.cropmanager.biogas_generator_casing.name", "cropmanager.guide.biogas_generator_casing.desc",
                "cropmanager.guide.biogas_generator_casing.info").withRecipes("biogas_generator_casing"));

        catMachines.addEntry(entry("biogas_sprayer", "machines", new ItemStack(CropManager.BIOGAS_SPRAYER),
                "tile.cropmanager.biogas_sprayer.name", "cropmanager.guide.biogas_sprayer.desc",
                "cropmanager.guide.biogas_sprayer.info").withRecipes("biogas_sprayer"));

        catMachines.addEntry(entry("hydroponic_chamber", "machines", new ItemStack(CropManager.HYDROPONIC_CHAMBER),
                "tile.cropmanager.hydroponic_chamber.name", "cropmanager.guide.hydroponic_chamber.desc",
                "cropmanager.guide.hydroponic_chamber.info").withRecipes("hydroponic_chamber"));

        CATEGORIES.add(catMachines);

        // ==========================================
        // 3. QUÍMICA AGRÍCOLA & FERTILIZANTES (chemistry)
        // ==========================================
        ManualCategory catChem = new ManualCategory("chemistry", "cropmanager.guide.cat.chemistry", new ItemStack(CropManager.CATALYTIC_FERTILIZER));

        ManualEntry eManualComp = entry("manual_composter", "chemistry", new ItemStack(CropManager.MANUAL_COMPOSTER),
                "cropmanager.guide.manual_composter.title", "cropmanager.guide.manual_composter.desc", "cropmanager.guide.manual_composter.info");
        eManualComp.withRecipes("manual_composter");
        catChem.addEntry(eManualComp);

        ManualEntry eOrgComp = entry("organic_compost", "chemistry", new ItemStack(CropManager.ORGANIC_COMPOST),
                "cropmanager.guide.organic_compost.title", "cropmanager.guide.organic_compost.desc", "cropmanager.guide.organic_compost.info");
        catChem.addEntry(eOrgComp);

        ManualEntry eCatFert = entry("catalytic_fertilizer", "chemistry", new ItemStack(CropManager.CATALYTIC_FERTILIZER),
                "cropmanager.guide.catalytic_fertilizer.title", "cropmanager.guide.catalytic_fertilizer.desc", "cropmanager.guide.catalytic_fertilizer.info");
        eCatFert.withRecipes("catalytic_fertilizer");
        catChem.addEntry(eCatFert);

        ManualEntry eMutagen = entry("mutagenic_powder", "chemistry", new ItemStack(CropManager.MUTAGENIC_POWDER),
                "cropmanager.guide.mutagenic_powder.title", "cropmanager.guide.mutagenic_powder.desc", "cropmanager.guide.mutagenic_powder.info");
        eMutagen.withRecipes("mutagenic_powder");
        catChem.addEntry(eMutagen);

        ManualEntry eHive = entry("pollinator_hive", "chemistry", new ItemStack(CropManager.POLLINATOR_HIVE),
                "cropmanager.guide.pollinator_hive.title", "cropmanager.guide.pollinator_hive.desc", "cropmanager.guide.pollinator_hive.info");
        eHive.withRecipes("pollinator_hive");
        catChem.addEntry(eHive);

        ManualEntry eBlade = entry("grinding_blade", "chemistry", new ItemStack(CropManager.GRINDING_BLADE),
                "cropmanager.guide.grinding_blade.title", "cropmanager.guide.grinding_blade.desc", "cropmanager.guide.grinding_blade.info");
        eBlade.withRecipes("grinding_blade");
        catChem.addEntry(eBlade);

        ManualEntry eGrinder = entry("grin_grinder", "chemistry", new ItemStack(CropManager.GRIN_GRINDER),
                "cropmanager.guide.grin_grinder.title", "cropmanager.guide.grin_grinder.desc", "cropmanager.guide.grin_grinder.info");
        eGrinder.withRecipes("grin_grinder");
        catChem.addEntry(eGrinder);

        ManualEntry eExtractor = entry("grin_extractor", "chemistry", new ItemStack(CropManager.GRIN_EXTRACTOR),
                "cropmanager.guide.grin_extractor.title", "cropmanager.guide.grin_extractor.desc", "cropmanager.guide.grin_extractor.info");
        eExtractor.withRecipes("grin_extractor");
        catChem.addEntry(eExtractor);

        ManualEntry eMixer = entry("weed_ex_fabricator", "chemistry", new ItemStack(CropManager.WEED_EX_FABRICATOR),
                "cropmanager.guide.weed_ex_fabricator.title", "cropmanager.guide.weed_ex_fabricator.desc", "cropmanager.guide.weed_ex_fabricator.info");
        eMixer.withRecipes("weed_ex_fabricator");
        catChem.addEntry(eMixer);

        ManualEntry eNitro = entry("nitro_essence", "chemistry", new ItemStack(CropManager.NITRO_ESSENCE),
                "cropmanager.guide.nitro_essence.title", "cropmanager.guide.nitro_essence.desc", "cropmanager.guide.nitro_essence.info");
        catChem.addEntry(eNitro);

        ManualEntry eBioFiber = entry("bio_fiber", "chemistry", new ItemStack(CropManager.BIO_FIBER),
                "cropmanager.guide.bio_fiber.title", "cropmanager.guide.bio_fiber.desc", "cropmanager.guide.bio_fiber.info");
        eBioFiber.withRecipes("bio_fiber_plant_ball");
        catChem.addEntry(eBioFiber);

        CATEGORIES.add(catChem);

        // ==========================================
        // 4. IRRIGAÇÃO & MULTIBLOCO (irrigation)
        // ==========================================
        ManualCategory catIrrig = new ManualCategory("irrigation", "cropmanager.guide.cat.irrigation", new ItemStack(CropManager.AGRICULTURAL_RESERVOIR));

        ManualEntry eSp1 = entry("sprinkler_mk1", "irrigation", new ItemStack(CropManager.SPRINKLER_MK1),
                "cropmanager.guide.sprinkler_mk1.title", "cropmanager.guide.sprinkler_mk1.desc", "cropmanager.guide.sprinkler_mk1.info");
        eSp1.withRecipes("sprinkler_mk1");
        catIrrig.addEntry(eSp1);

        ManualEntry eSp2 = entry("sprinkler_mk2", "irrigation", new ItemStack(CropManager.SPRINKLER_MK2),
                "cropmanager.guide.sprinkler_mk2.title", "cropmanager.guide.sprinkler_mk2.desc", "cropmanager.guide.sprinkler_mk2.info");
        eSp2.withRecipes("sprinkler_mk2");
        catIrrig.addEntry(eSp2);

        ManualEntry eSp3 = entry("sprinkler_mk3", "irrigation", new ItemStack(CropManager.SPRINKLER_MK3),
                "cropmanager.guide.sprinkler_mk3.title", "cropmanager.guide.sprinkler_mk3.desc", "cropmanager.guide.sprinkler_mk3.info");
        eSp3.withRecipes("sprinkler_mk3");
        catIrrig.addEntry(eSp3);

        catIrrig.addEntry(entry("sprinkler_diagonal", "irrigation", new ItemStack(CropManager.SPRINKLER_DIAGONAL),
                "tile.cropmanager.sprinkler_diagonal.name", "cropmanager.guide.sprinkler_diagonal.desc",
                "cropmanager.guide.sprinkler_diagonal.info").withRecipes("sprinkler_diagonal"));

        ManualEntry eReservoir = entry("agricultural_reservoir", "irrigation", new ItemStack(CropManager.AGRICULTURAL_RESERVOIR),
                "cropmanager.guide.agricultural_reservoir.title", "cropmanager.guide.agricultural_reservoir.desc", "cropmanager.guide.agricultural_reservoir.info");
        eReservoir.withRecipes("agricultural_reservoir");
        catIrrig.addEntry(eReservoir);

        ManualEntry eCasing = entry("reservoir_casing", "irrigation", new ItemStack(CropManager.RESERVOIR_CASING),
                "cropmanager.guide.reservoir_casing.title", "cropmanager.guide.reservoir_casing.desc", "cropmanager.guide.reservoir_casing.info");
        eCasing.withRecipes("reservoir_casing");
        catIrrig.addEntry(eCasing);

        ManualEntry eCapacity = entry("reservoir_capacity", "irrigation", new ItemStack(CropManager.RESERVOIR_CAPACITY),
                "cropmanager.guide.reservoir_capacity.title", "cropmanager.guide.reservoir_capacity.desc", "cropmanager.guide.reservoir_capacity.info");
        eCapacity.withRecipes("reservoir_capacity");
        catIrrig.addEntry(eCapacity);

        ManualEntry ePump = entry("reservoir_pump", "irrigation", new ItemStack(CropManager.RESERVOIR_PUMP),
                "cropmanager.guide.reservoir_pump.title", "cropmanager.guide.reservoir_pump.desc", "cropmanager.guide.reservoir_pump.info");
        ePump.withRecipes("reservoir_pump");
        catIrrig.addEntry(ePump);

        ManualEntry eReserve = entry("reservoir_reserve", "irrigation", new ItemStack(CropManager.RESERVOIR_RESERVE),
                "cropmanager.guide.reservoir_reserve.title", "cropmanager.guide.reservoir_reserve.desc", "cropmanager.guide.reservoir_reserve.info");
        eReserve.withRecipes("reservoir_reserve");
        catIrrig.addEntry(eReserve);

        ManualEntry eFluidTank = entry("fluid_tank", "irrigation", new ItemStack(CropManager.FLUID_TANK),
                "cropmanager.guide.fluid_tank.title", "cropmanager.guide.fluid_tank.desc", "cropmanager.guide.fluid_tank.info");
        eFluidTank.withRecipes("fluid_tank");
        catIrrig.addEntry(eFluidTank);

        ManualEntry eFluidTankCasing = entry("fluid_tank_casing", "irrigation", new ItemStack(CropManager.FLUID_TANK_CASING),
                "cropmanager.guide.fluid_tank_casing.title", "cropmanager.guide.fluid_tank_casing.desc", "cropmanager.guide.fluid_tank_casing.info");
        eFluidTankCasing.withRecipes("fluid_tank_casing");
        catIrrig.addEntry(eFluidTankCasing);

        ManualEntry eFluidPipe = entry("fluid_pipe", "irrigation", new ItemStack(CropManager.FLUID_PIPE),
                "cropmanager.guide.fluid_pipe.title", "cropmanager.guide.fluid_pipe.desc", "cropmanager.guide.fluid_pipe.info");
        eFluidPipe.withRecipes("fluid_pipe");
        catIrrig.addEntry(eFluidPipe);

        CATEGORIES.add(catIrrig);

        // ==========================================
        // 5. UPGRADES & FERRAMENTAS (upgrades)
        // ==========================================
        ManualCategory catUpgrades = new ManualCategory("upgrades", "cropmanager.guide.cat.upgrades", new ItemStack(CropManager.WIRELESS_EU));

        ManualEntry eWeedKill = entry("weed_killer_upgrade", "upgrades", new ItemStack(CropManager.WEED_KILLER),
                "cropmanager.guide.weed_killer_upgrade.title", "cropmanager.guide.weed_killer_upgrade.desc", "cropmanager.guide.weed_killer_upgrade.info");
        eWeedKill.withRecipes("weed_killer_upgrade");
        catUpgrades.addEntry(eWeedKill);

        ManualEntry eRange = entry("wireless_range_upgrade", "upgrades", new ItemStack(CropManager.WIRELESS_RANGE),
                "cropmanager.guide.wireless_range_upgrade.title", "cropmanager.guide.wireless_range_upgrade.desc", "cropmanager.guide.wireless_range_upgrade.info");
        eRange.withRecipes("wireless_range_upgrade");
        catUpgrades.addEntry(eRange);

        ManualEntry eWirelessEu = entry("wireless_eu_upgrade", "upgrades", new ItemStack(CropManager.WIRELESS_EU),
                "cropmanager.guide.wireless_eu_upgrade.title", "cropmanager.guide.wireless_eu_upgrade.desc", "cropmanager.guide.wireless_eu_upgrade.info");
        eWirelessEu.withRecipes("wireless_eu_upgrade");
        catUpgrades.addEntry(eWirelessEu);

        ManualEntry eTransfer = entry("harvest_transfer_upgrade", "upgrades", new ItemStack(CropManager.HARVEST_TRANSFER),
                "cropmanager.guide.harvest_transfer_upgrade.title", "cropmanager.guide.harvest_transfer_upgrade.desc", "cropmanager.guide.harvest_transfer_upgrade.info");
        eTransfer.withRecipes("harvest_transfer_upgrade");
        catUpgrades.addEntry(eTransfer);

        ManualEntry eLinker = entry("upgrade_linker", "upgrades", new ItemStack(CropManager.UPGRADE_LINKER),
                "cropmanager.guide.upgrade_linker.title", "cropmanager.guide.upgrade_linker.desc", "cropmanager.guide.upgrade_linker.info");
        eLinker.withRecipes("upgrade_linker");
        catUpgrades.addEntry(eLinker);

        ManualEntry eManual = entry("agricultural_manual", "upgrades", new ItemStack(CropManager.AGRICULTURAL_MANUAL),
                "cropmanager.guide.agricultural_manual.title", "cropmanager.guide.agricultural_manual.desc", "cropmanager.guide.agricultural_manual.info");
        eManual.withRecipes("agricultural_manual_starter", "reservoir_guide");
        catUpgrades.addEntry(eManual);
        catUpgrades.addEntry(entry("eu_wifi_plug", "upgrades", new ItemStack(CropManager.EU_WIFI_PLUG),
                "tile.cropmanager.eu_wifi_plug.name", "cropmanager.eu_wifi_plug_help", "cropmanager.guide.eu_wifi_plug.info")
                .withRecipes("eu_wifi_plug"));
        catUpgrades.addEntry(entry("eu_wifi_point", "upgrades", new ItemStack(CropManager.EU_WIFI_POINT),
                "tile.cropmanager.eu_wifi_point.name", "cropmanager.eu_wifi_point_help", "cropmanager.guide.eu_wifi_point.info")
                .withRecipes("eu_wifi_point"));
        catUpgrades.addEntry(entry("eu_dish_antenna", "upgrades", new ItemStack(CropManager.EU_DISH_ANTENNA),
                "tile.cropmanager.eu_dish_antenna.name", "cropmanager.eu_dish_antenna_help", "cropmanager.guide.eu_dish_antenna.info")
                .withRecipes("eu_dish_antenna"));

        CATEGORIES.add(catUpgrades);

        // ==========================================
        // 6. ARMAZENAMENTO & BANCO DE DADOS (storage)
        // ==========================================
        ManualCategory catStorage = new ManualCategory("storage", "cropmanager.guide.cat.storage", new ItemStack(CropManager.DATABASE_CHEST));

        ManualEntry eChest = entry("crop_database_chest", "storage", new ItemStack(CropManager.DATABASE_CHEST),
                "cropmanager.guide.crop_database_chest.title", "cropmanager.guide.crop_database_chest.desc", "cropmanager.guide.crop_database_chest.info");
        eChest.withRecipes("crop_database_chest");
        catStorage.addEntry(eChest);

        ManualEntry ePortable = entry("portable_crop_database", "storage", new ItemStack(CropManager.PORTABLE_DATABASE),
                "cropmanager.guide.portable_crop_database.title", "cropmanager.guide.portable_crop_database.desc", "cropmanager.guide.portable_crop_database.info");
        ePortable.withRecipes("portable_crop_database", "portable_database_link");
        catStorage.addEntry(ePortable);

        catStorage.addEntry(entry("agricultural_silo", "storage", new ItemStack(CropManager.AGRICULTURAL_SILO),
                "tile.cropmanager.agricultural_silo.name", "cropmanager.silo_help",
                "cropmanager.guide.agricultural_silo.info").withRecipes("agricultural_silo"));
        catStorage.addEntry(entry("silo_casing", "storage", new ItemStack(CropManager.SILO_CASING),
                "tile.cropmanager.silo_casing.name", "cropmanager.guide.silo_casing.desc",
                "cropmanager.guide.silo_casing.info").withRecipes("silo_casing"));

        CATEGORIES.add(catStorage);
        initialized = true;
    }

    private static ManualEntry entry(String id, String cat, ItemStack stack, String title, String desc, String info) {
        ManualEntry e = new ManualEntry(id, cat, stack, title, desc, info);
        ENTRIES_BY_ID.put(id, e);
        return e;
    }

    private static void addIc2Entry(ManualCategory category, String id, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String key = "cropmanager.guide." + id;
        category.addEntry(entry(id, category.getId(), stack.copy(), key + ".title", key + ".desc", key + ".info")
                .withIc2Recipes());
    }

    private static ItemStack ic2(String name, String variant, ItemStack fallback) {
        ItemStack item = variant == null ? IC2Items.getItem(name) : IC2Items.getItem(name, variant);
        return (item != null && !item.isEmpty()) ? item.copy() : fallback;
    }

    public static List<ManualCategory> getCategories() {
        init();
        return Collections.unmodifiableList(CATEGORIES);
    }

    public static ManualEntry getEntry(String id) {
        init();
        return ENTRIES_BY_ID.get(id);
    }

    public static ManualEntry findEntry(ItemStack stack) {
        init();
        if (stack == null || stack.isEmpty()) return null;
        for (ManualCategory category : CATEGORIES) {
            for (ManualEntry e : category.getEntries()) {
                ItemStack display = e.getDisplayStack();
                if (!display.isEmpty() && ItemStack.areItemsEqual(display, stack)) return e;
            }
        }
        return null;
    }

    public static List<ManualEntry> search(String query, Function<String, String> translate) {
        init();
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        String q = normalize(query.trim());
        List<ManualEntry> list = new ArrayList<>();
        for (ManualEntry e : ENTRIES_BY_ID.values()) {
            String name = normalize(translate.apply(e.getTitleKey()));
            if (name.contains(q) || normalize(e.getId()).contains(q)) {
                list.add(e);
            }
        }
        return list;
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
