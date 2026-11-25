package br.cropmanager;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/**
 * Configuração centralizada do Crop Manager carregada a partir de {@code config/cropmanager.cfg}.
 *
 * <p>Os valores definidos pelo servidor governam área máxima, custos de energia, taxas de transferência
 * sem fio, capacidades de tanques e receitas de todas as máquinas de suporte.
 */
public final class ManagerConfig {
    // Area
    public static int defaultRadius = 4;
    public static int maxRadius = 8;
    public static int defaultLayers = 7;

    // Processing
    public static int interval = 2;
    public static int maxScans = 8;

    // Energy (Crop Manager actions)
    public static double scanEU = 1;
    public static double careEU = 10;
    public static double harvestEU = 20;

    // Advanced / Basic Crop Manager
    public static int managerBasicEnergyCapacity = 20000;
    public static int managerAdvancedEnergyCapacity = 100000;
    public static int managerBasicSinkTier = 1;
    public static int managerAdvancedSinkTier = 2;
    public static double managerAdvancedTimeMultiplier = 0.5;
    public static int managerBasicTankCapacity = 10000;
    public static int managerAdvancedTankCapacity = 20000;
    public static int managerWeedKillerCostMB = 20;
    public static int managerFlushPerTick = 15;

    // Agricultural Monitor
    public static int monitorBaseRange = 32;
    public static int monitorMaxRangeUpgrades = 4;
    public static int monitorTankCapacity = 16000;
    public static int monitorFluidTransferPerSecond = 1000;
    public static int monitorFertilizerTransferPerSecond = 16;
    public static int monitorHarvestTransferPerSecond = 64;
    public static int monitorWirelessEuCapacityPerUpgrade = 50000;
    public static int monitorWirelessEuRatePerUpgrade = 256;
    public static int monitorSupportWirelessRadius = 16;
    public static int monitorTestSignalTicks = 100;

    // Biogas Weeder
    public static int weederGasCapacity = 16000;
    public static int weederScansPerTick = 2;
    public static int weederScanGasCostMB = 1;
    public static int weederRemoveGasCostMB = 20;

    // Electric Composter
    public static int composterRequiredItems = 4;
    public static int composterWaterPerRecipe = 1000;
    public static int composterTankCapacity = 16000;
    public static int composterDuration = 200;
    public static int composterEuPerTick = 5;
    public static int composterEnergyCapacity = 20000;
    public static int composterSinkTier = 1;

    // Grin Grinder & Grinding Blade
    public static int grinderDuration = 100;
    public static int grinderEuPerTick = 5;
    public static int grinderEnergyCapacity = 20000;
    public static int grinderSinkTier = 1;
    public static int grinderBladeDurability = 256;

    // Grin Extractor
    public static int extractorTankCapacity = 16000;
    public static int extractorDuration = 100;
    public static int extractorEuPerTick = 5;
    public static int extractorWaterPerRecipe = 1000;
    public static int extractorExtractPerRecipe = 1000;
    public static int extractorEnergyCapacity = 20000;
    public static int extractorSinkTier = 1;

    // Weed-Ex Fabricator (Mixer)
    public static int fabricatorExtractCapacity = 16000;
    public static int fabricatorWeedCapacity = 16000;
    public static int fabricatorDuration = 100;
    public static int fabricatorEuPerTick = 5;
    public static int fabricatorExtractPerRecipe = 1000;
    public static int fabricatorWeedPerRecipe = 500;
    public static int fabricatorEnergyCapacity = 20000;
    public static int fabricatorSinkTier = 1;

    // Thermal Pipe
    public static int thermalPipeMaxHeatPerTick = 100;

    // Crop Database Chest
    public static int chestCollectRadius = 3;
    public static int chestCollectIntervalTicks = 10;
    public static int chestCollectMaxItems = 32;

    // Manual Composter
    public static int manualComposterRequiredItems = 8;
    public static int manualComposterDuration = 1200;
    public static double manualComposterWaterSpeed = 2.0;
    public static int manualComposterOutputCount = 2;

    // Custom Fertilizers
    public static int organicNutrients = 60;
    public static int organicWater = 40;
    public static int catalyticNutrients = 100;
    public static int catalyticGrowthBoost = 40;
    public static int catalyticCharges = 3;
    public static int mutagenicPowderBurstTicks = 12;
    public static double mutagenicStatBonusChance = 0.5;

    // Seed Analyzer
    public static int analyzerDuration = 100;
    public static int analyzerEuPerTick = 5;
    public static int analyzerEnergyCapacity = 20000;
    public static int analyzerSinkTier = 1;

    // Custom IC2 Crops
    public static boolean enableCustomCrops = true;
    public static double radbloomRadiationRadius = 3.5;
    public static int radbloomRadiationDuration = 200;

    // Tiered Sprinklers
    public static int sprinklerMk1Radius = 4;
    public static int sprinklerMk3Radius = 8;
    public static int sprinklerMk2Speed = 4;
    public static int sprinklerBaseInterval = 80;
    public static int sprinklerHydrationBonus = 25;
    public static int sprinklerGrowthBonus = 15;

    // Pollinator Hive
    public static int pollinatorRadius = 2;
    public static int pollinatorBaseInterval = 100;
    public static int pollinatorChargesPerPowder = 16;
    public static int pollinatorMaxCharges = 64;

    // Greenhouse Glass
    public static int greenhouseGrowthBonus = 20;

    // Industrial Fluid Reservoir
    public static int industrialTankBaseCapacity = 128000;
    public static int industrialTankCapacityPerUpgrade = 32000;
    public static int industrialTankAutoEjectRate = 2000;

    // Agricultural Grain Silo
    public static int siloBaseCapacity = 128000;
    public static int siloCapacityPerUpgrade = 32000;
    public static int siloAutoEjectRate = 64;

    // Fluid Logistics Pipes
    public static int fluidPipeTransferMb = 250;
    public static int fluidPipeIntervalTicks = 5;

    // Wi-Fi EU Grid
    public static int wifiChannelBufferEU = 100000;
    public static int wifiPlugSinkTier = 4;
    public static int wifiPointMaxOutputTier = 4;
    public static float wifiCrossDimLossPercent = 0.0F;

    // Dish Antenna
    public static int dishBaseRadius = 16;
    public static int dishRadiusPerUpgrade = 16;
    public static int dishMaxTransferPerTick = 512;
    public static int dishEnergyCapacity = 100000;
    public static int dishSinkTier = 4;
    public static int dishScanIntervalTicks = 20;

    // Hydroponic Chamber
    public static int hydroponicDuration = 200;
    public static int hydroponicEuPerTick = 16;
    public static int hydroponicEnergyCapacity = 50000;
    public static int hydroponicSinkTier = 2;
    public static int hydroponicTankCapacity = 16000;
    public static int hydroponicWaterPerCycle = 500;
    public static int hydroponicHeatPerTick = 20;

    private ManagerConfig() {}

    /** Carrega todas as categorias de configuração do arquivo {@code .cfg} e grava ajustes se necessário. */
    public static void load(File file) {
        Configuration c = new Configuration(file);
        c.load();

        interval = c.getInt("baseIntervalTicks", "processing", 2, 1, 20, "Ticks per position without upgrades.");
        maxScans = c.getInt("maxPositionsPerTick", "processing", 8, 1, 32, "Includes unloaded and empty positions; at most four actions per position.");

        maxRadius = c.getInt("maxRadius", "area", 8, 1, 16, "Server maximum horizontal radius.");
        defaultRadius = c.getInt("defaultRadius", "area", 4, 1, maxRadius, "Radius for newly placed machines.");
        defaultLayers = c.getInt("defaultLayers", "area", 7, 1, 7, "Bit mask: 1 below, 2 same height, 4 above.");

        scanEU = finiteFloat(c, "energy", "scanEU", 1, 1, 1000, "EU per loaded position, before upgrades.");
        careEU = finiteFloat(c, "energy", "careEU", 10, 1, 1000, "EU per successful care action, before upgrades.");
        harvestEU = finiteFloat(c, "energy", "harvestEU", 20, 1, 1000, "EU per successful harvest, before upgrades.");

        managerBasicEnergyCapacity = c.getInt("basicEnergyCapacity", "advanced_manager", 20000, 1000, 10000000, "Base EU capacity for basic Crop Managers.");
        managerAdvancedEnergyCapacity = c.getInt("advancedEnergyCapacity", "advanced_manager", 100000, 1000, 10000000, "Base EU capacity for Advanced Crop Managers.");
        managerBasicSinkTier = c.getInt("basicSinkTier", "advanced_manager", 1, 1, 8, "Base IC2 energy tier for basic Crop Managers.");
        managerAdvancedSinkTier = c.getInt("advancedSinkTier", "advanced_manager", 2, 1, 8, "Base IC2 energy tier for Advanced Crop Managers.");
        managerAdvancedTimeMultiplier = finiteFloat(c, "advanced_manager", "advancedTimeMultiplier", 0.5F, 0.05F, 10.0F, "Base interval multiplier for Advanced Crop Managers before IC2 upgrades.");
        managerBasicTankCapacity = c.getInt("basicTankCapacity", "advanced_manager", 10000, 1000, 1000000, "Water and Weed-Ex tank capacity (mB) for basic Crop Managers.");
        managerAdvancedTankCapacity = c.getInt("advancedTankCapacity", "advanced_manager", 20000, 1000, 1000000, "Water and Weed-Ex tank capacity (mB) for Advanced Crop Managers.");
        managerWeedKillerCostMB = c.getInt("weedKillerCostMB", "advanced_manager", 20, 0, 1000, "Weed-Ex consumed (mB) per weed removed using the Weed Killer upgrade.");
        managerFlushPerTick = c.getInt("flushPendingPerTick", "advanced_manager", 15, 1, 64, "Maximum pending harvest stacks flushed to output slots per tick.");

        monitorBaseRange = c.getInt("baseRange", "monitor", 32, 1, 512, "Base connection range in blocks without Wireless Range upgrades.");
        monitorMaxRangeUpgrades = c.getInt("maxRangeUpgrades", "monitor", 4, 0, 4, "Maximum Wireless Range upgrades applied (each doubles range).");
        monitorTankCapacity = c.getInt("tankCapacity", "monitor", 16000, 1000, 1000000, "Buffer tank capacity (mB) for water and Weed-Ex.");
        monitorFluidTransferPerSecond = c.getInt("fluidTransferPerSecond", "monitor", 1000, 1, 100000, "Maximum water and Weed-Ex (mB) transferred per second to the linked machine.");
        monitorFertilizerTransferPerSecond = c.getInt("fertilizerTransferPerSecond", "monitor", 16, 1, 192, "Maximum fertilizer items transferred per second to the linked machine.");
        monitorHarvestTransferPerSecond = c.getInt("harvestTransferPerSecond", "monitor", 64, 1, 960, "Maximum harvested items transferred per second from the linked manager.");
        monitorWirelessEuCapacityPerUpgrade = c.getInt("wirelessEuCapacityPerUpgrade", "monitor", 50000, 1000, 10000000, "EU buffer capacity added per Wireless EU upgrade.");
        monitorWirelessEuRatePerUpgrade = c.getInt("wirelessEuRatePerUpgrade", "monitor", 256, 1, 100000, "EU/t transfer rate added per Wireless EU upgrade.");
        monitorSupportWirelessRadius = c.getInt("supportWirelessRadius", "monitor", 16, 1, 64, "Radius in blocks around the Monitor to wirelessly power support machines.");
        monitorTestSignalTicks = c.getInt("testSignalTicks", "monitor", 100, 1, 1200, "Duration in ticks for the manual redstone test pulse.");

        weederGasCapacity = c.getInt("gasCapacity", "weeder", 16000, 1000, 1000000, "Internal biogas tank capacity (mB).");
        weederScansPerTick = c.getInt("scansPerTick", "weeder", 2, 1, 32, "Positions scanned per tick when active.");
        weederScanGasCostMB = c.getInt("scanGasCostMB", "weeder", 1, 0, 1000, "Biogas consumed (mB) per loaded crop tile scanned.");
        weederRemoveGasCostMB = c.getInt("removeWeedGasCostMB", "weeder", 20, 0, 1000, "Additional biogas consumed (mB) per weed removed.");

        composterRequiredItems = c.getInt("requiredItems", "composter", 4, 1, 64, "Organic items consumed per fertilizer crafted.");
        composterWaterPerRecipe = c.getInt("waterPerRecipe", "composter", 1000, 0, 16000, "Water in mB consumed per fertilizer crafted.");
        composterTankCapacity = c.getInt("tankCapacity", "composter", 16000, 1000, 1000000, "Internal water tank capacity in mB.");
        composterDuration = c.getInt("durationTicks", "composter", 200, 1, 72000, "Base processing time in ticks without Overclockers.");
        composterEuPerTick = c.getInt("euPerTick", "composter", 5, 1, 10000, "Base EU/t consumed while composting, before Overclockers.");
        composterEnergyCapacity = c.getInt("energyCapacity", "composter", 20000, 1000, 10000000, "Base EU buffer capacity before Energy Storage upgrades.");
        composterSinkTier = c.getInt("sinkTier", "composter", 1, 1, 8, "Base IC2 energy tier before Transformer upgrades.");

        grinderDuration = c.getInt("durationTicks", "grin_grinder", 100, 1, 72000, "Ticks required to grind one spider eye into Grin Powder.");
        grinderEuPerTick = c.getInt("euPerTick", "grin_grinder", 5, 1, 10000, "EU/t consumed while grinding.");
        grinderEnergyCapacity = c.getInt("energyCapacity", "grin_grinder", 20000, 1000, 10000000, "Internal EU buffer capacity.");
        grinderSinkTier = c.getInt("sinkTier", "grin_grinder", 1, 1, 8, "IC2 energy input tier.");
        grinderBladeDurability = c.getInt("bladeDurability", "grin_grinder", 256, 1, 100000, "Total uses of a Grinding Blade before breaking.");

        extractorTankCapacity = c.getInt("tankCapacity", "grin_extractor", 16000, 1000, 1000000, "Capacity (mB) of both water and Grin Extract tanks.");
        extractorDuration = c.getInt("durationTicks", "grin_extractor", 100, 1, 72000, "Ticks required per extraction batch.");
        extractorEuPerTick = c.getInt("euPerTick", "grin_extractor", 5, 1, 10000, "EU/t consumed while extracting.");
        extractorWaterPerRecipe = c.getInt("waterPerRecipe", "grin_extractor", 1000, 1, extractorTankCapacity, "Water (mB) consumed per extraction batch.");
        extractorExtractPerRecipe = c.getInt("extractPerRecipe", "grin_extractor", 1000, 1, extractorTankCapacity, "Grin Extract (mB) produced per extraction batch.");
        extractorEnergyCapacity = c.getInt("energyCapacity", "grin_extractor", 20000, 1000, 10000000, "Internal EU buffer capacity.");
        extractorSinkTier = c.getInt("sinkTier", "grin_extractor", 1, 1, 8, "IC2 energy input tier.");

        fabricatorExtractCapacity = c.getInt("extractTankCapacity", "weed_ex_fabricator", 16000, 1000, 1000000, "Input Grin Extract tank capacity (mB).");
        fabricatorWeedCapacity = c.getInt("weedTankCapacity", "weed_ex_fabricator", 16000, 1000, 1000000, "Output Weed-Ex tank capacity (mB).");
        fabricatorDuration = c.getInt("durationTicks", "weed_ex_fabricator", 100, 1, 72000, "Ticks required per Weed-Ex mixing batch.");
        fabricatorEuPerTick = c.getInt("euPerTick", "weed_ex_fabricator", 5, 1, 10000, "EU/t consumed while mixing.");
        fabricatorExtractPerRecipe = c.getInt("extractPerRecipe", "weed_ex_fabricator", 1000, 1, fabricatorExtractCapacity, "Grin Extract (mB) consumed per mixing batch.");
        fabricatorWeedPerRecipe = c.getInt("weedExPerRecipe", "weed_ex_fabricator", 500, 1, fabricatorWeedCapacity, "Weed-Ex (mB) produced per mixing batch.");
        fabricatorEnergyCapacity = c.getInt("energyCapacity", "weed_ex_fabricator", 20000, 1000, 10000000, "Internal EU buffer capacity.");
        fabricatorSinkTier = c.getInt("sinkTier", "weed_ex_fabricator", 1, 1, 8, "IC2 energy input tier.");

        thermalPipeMaxHeatPerTick = c.getInt("maxHeatPerTick", "thermal_pipe", 100, 1, 100000, "Maximum IC2 Heat (HU/t) transferred per tick through a Thermal Pipe.");

        chestCollectRadius = c.getInt("autoCollectRadius", "database_chest", 3, 1, 16, "Radius in blocks around the Crop Database Chest for magnetic seed collection.");
        chestCollectIntervalTicks = c.getInt("autoCollectIntervalTicks", "database_chest", 10, 1, 200, "Ticks between magnetic seed collection scans.");
        chestCollectMaxItems = c.getInt("autoCollectMaxItemsPerScan", "database_chest", 32, 1, 256, "Maximum dropped item entities inspected per collection scan.");

        manualComposterRequiredItems = c.getInt("requiredItems", "manual_composter", 8, 1, 64, "Organic items required to fill the Manual Composter.");
        manualComposterDuration = c.getInt("durationTicks", "manual_composter", 1200, 20, 72000, "Base composting time in ticks for the Manual Composter.");
        manualComposterWaterSpeed = finiteFloat(c, "manual_composter", "waterSpeedMultiplier", 2.0F, 1.0F, 10.0F, "Speed multiplier when the Manual Composter is hydrated with water.");
        manualComposterOutputCount = c.getInt("outputCount", "manual_composter", 2, 1, 16, "Organic Compost items produced per completed batch.");

        organicNutrients = c.getInt("organicNutrients", "fertilizers", 60, 1, 100, "Nutrients added to a crop by Organic Compost.");
        organicWater = c.getInt("organicWater", "fertilizers", 40, 0, 100, "Hydration added to a crop by Organic Compost.");
        catalyticNutrients = c.getInt("catalyticNutrients", "fertilizers", 100, 1, 100, "Nutrients added to a crop by Catalytic Fertilizer.");
        catalyticGrowthBoost = c.getInt("catalyticGrowthBoost", "fertilizers", 40, 0, 500, "Bonus growth points granted by Catalytic Fertilizer.");
        catalyticCharges = c.getInt("catalyticCharges", "fertilizers", 3, 1, 16, "Catalyzed soil charges applied by Catalytic Fertilizer.");
        mutagenicPowderBurstTicks = c.getInt("mutagenicPowderBurstTicks", "fertilizers", 12, 1, 64, "Accelerated cross-breeding ticks performed per use of Mutagenic Powder.");
        mutagenicStatBonusChance = finiteFloat(c, "fertilizers", "mutagenicStatBonusChance", 0.5F, 0.0F, 1.0F, "Chance (0.0-1.0) for Mutagenic Powder to grant a safe +1 stat bonus to a newly sprouted hybrid.");

        analyzerDuration = c.getInt("durationTicks", "seed_analyzer", 100, 1, 72000, "Base ticks required to analyze one Seed Bag to level 4.");
        analyzerEuPerTick = c.getInt("euPerTick", "seed_analyzer", 5, 1, 10000, "Base EU/t consumed while analyzing a Seed Bag.");
        analyzerEnergyCapacity = c.getInt("energyCapacity", "seed_analyzer", 20000, 1000, 10000000, "Internal EU buffer capacity for the Seed Analyzer.");
        analyzerSinkTier = c.getInt("sinkTier", "seed_analyzer", 1, 1, 8, "IC2 energy input tier for the Seed Analyzer.");

        enableCustomCrops = c.getBoolean("enableCustomCrops", "crops", true, "Register Crop Manager's custom IC2 crops (agricultural support and catalyzed species).");
        radbloomRadiationRadius = finiteFloat(c, "crops", "radbloomRadiationRadius", 3.5F, 0.0F, 16.0F, "Radiation aura radius in blocks emitted by mature Radbloom crops (0 disables).");
        radbloomRadiationDuration = c.getInt("radbloomRadiationDuration", "crops", 200, 0, 2400, "IC2 Radiation duration in ticks applied by mature Radbloom crops.");

        sprinklerMk1Radius = c.getInt("mk1AndMk2Radius", "sprinklers", 4, 1, 12, "Horizontal radius for Sprinkler Mk1 and Mk2 (4 = 9x9 area).");
        sprinklerMk3Radius = c.getInt("mk3Radius", "sprinklers", 8, 1, 16, "Horizontal radius for Sprinkler Mk3 (8 = 17x17 area).");
        sprinklerMk2Speed = c.getInt("mk2AndMk3SpeedMultiplier", "sprinklers", 4, 1, 10, "Speed and growth-bonus multiplier for Sprinkler Mk2 and Mk3.");
        sprinklerBaseInterval = c.getInt("baseIntervalTicks", "sprinklers", 80, 10, 1200, "Base ticks between hydration pulses for Sprinkler Mk1.");
        sprinklerHydrationBonus = c.getInt("hydrationBonus", "sprinklers", 25, 1, 100, "Water hydration added to IC2 crops per pulse.");
        sprinklerGrowthBonus = c.getInt("growthBonus", "sprinklers", 15, 0, 200, "Base growth points added to IC2 crops per pulse (scaled by tier speed).");

        pollinatorRadius = c.getInt("radius", "pollinator_hive", 2, 1, 8, "Horizontal radius of the Pollinator Hive scan area.");
        pollinatorBaseInterval = c.getInt("baseIntervalTicks", "pollinator_hive", 100, 10, 1200, "Base ticks per pollination pulse; charged operation runs at three pulses per interval.");
        pollinatorChargesPerPowder = c.getInt("chargesPerPowder", "pollinator_hive", 16, 1, 64, "Charges added when one Mutagenic Powder is inserted.");
        pollinatorMaxCharges = c.getInt("maxCharges", "pollinator_hive", 64, 1, 64, "Maximum stored Mutagenic Powder charges.");

        greenhouseGrowthBonus = c.getInt("growthBonus", "greenhouse", 20, 0, 200, "Growth points granted to a valid IC2 crop directly below Greenhouse Glass per random tick.");

        industrialTankBaseCapacity = c.getInt("baseCapacity", "industrial_tank", 128000, 16000, 10000000, "Base fluid capacity (mB) for the Industrial Fluid Reservoir.");
        industrialTankCapacityPerUpgrade = c.getInt("capacityPerUpgrade", "industrial_tank", 32000, 1000, 1000000, "Extra capacity (mB) added per Capacity Expansion upgrade (up to 8).");
        industrialTankAutoEjectRate = c.getInt("autoEjectRateMB", "industrial_tank", 2000, 100, 100000, "Maximum fluid (mB) auto-ejected per second when Redstone allows (0 EU).");

        siloBaseCapacity = c.getInt("baseCapacity", "agricultural_silo", 128000, 1000, 10000000, "Base item capacity for the Agricultural Grain Silo.");
        siloCapacityPerUpgrade = c.getInt("capacityPerUpgrade", "agricultural_silo", 32000, 100, 1000000, "Extra capacity added per Capacity Expansion upgrade (up to 8).");
        siloAutoEjectRate = c.getInt("autoEjectRate", "agricultural_silo", 64, 1, 10000, "Maximum items auto-ejected per second when Redstone allows (0 EU).");

        fluidPipeTransferMb = c.getInt("transferMbPerPulse", "fluid_pipe", 250, 10, 10000, "Maximum fluid (mB) transferred per pulse through Fluid Pipes, Restrictors, and Routers.");
        fluidPipeIntervalTicks = c.getInt("intervalTicks", "fluid_pipe", 5, 1, 200, "Ticks between automatic fluid transfer pulses.");

        wifiChannelBufferEU = c.getInt("channelBufferEU", "wifi_grid", 100000, 1000, 10000000, "Smoothing EU buffer per Wi-Fi EU channel.");
        wifiPlugSinkTier = c.getInt("plugSinkTier", "wifi_grid", 4, 1, 8, "IC2 energy input tier for the Wi-Fi EU Plug.");
        wifiPointMaxOutputTier = c.getInt("pointMaxOutputTier", "wifi_grid", 4, 1, 4, "Maximum selectable Wi-Fi EU Point output tier.");
        wifiCrossDimLossPercent = finiteFloat(c, "wifi_grid", "crossDimLossPercent", 0.0F, 0.0F, 50.0F, "Optional EU transmission loss percentage across dimensions (0-50).");

        dishBaseRadius = c.getInt("baseRadius", "dish_antenna", 16, 4, 64, "Base horizontal/vertical radius in blocks for the EU Dish Antenna.");
        dishRadiusPerUpgrade = c.getInt("radiusPerUpgrade", "dish_antenna", 16, 4, 32, "Extra radius per Wireless Range upgrade (up to 4).");
        dishMaxTransferPerTick = c.getInt("maxTransferPerTick", "dish_antenna", 512, 32, 16384, "Maximum EU/t distributed by a Dish Antenna across linked machines.");
        dishEnergyCapacity = c.getInt("energyCapacity", "dish_antenna", 100000, 1000, 10000000, "Internal EU buffer capacity for the EU Dish Antenna.");
        dishSinkTier = c.getInt("sinkTier", "dish_antenna", 4, 1, 8, "IC2 energy input tier for the EU Dish Antenna.");
        dishScanIntervalTicks = c.getInt("scanIntervalTicks", "dish_antenna", 20, 5, 200, "Ticks between loaded-chunk receiver cache refreshes.");

        hydroponicDuration = c.getInt("durationTicks", "hydroponic_chamber", 200, 10, 72000, "Base ticks per harvest cycle in the Hydroponic Chamber before dome/heat/Overclocker bonuses.");
        hydroponicEuPerTick = c.getInt("euPerTick", "hydroponic_chamber", 16, 1, 10000, "Base EU/t consumed while growing a crop in the Hydroponic Chamber.");
        hydroponicEnergyCapacity = c.getInt("energyCapacity", "hydroponic_chamber", 50000, 1000, 10000000, "Internal EU buffer capacity for the Hydroponic Chamber.");
        hydroponicSinkTier = c.getInt("sinkTier", "hydroponic_chamber", 2, 1, 8, "IC2 energy input tier for the Hydroponic Chamber.");
        hydroponicTankCapacity = c.getInt("tankCapacity", "hydroponic_chamber", 16000, 1000, 1000000, "Internal water tank capacity (mB) for the Hydroponic Chamber.");
        hydroponicWaterPerCycle = c.getInt("waterPerCycle", "hydroponic_chamber", 500, 0, hydroponicTankCapacity, "Water (mB) consumed per completed harvest cycle.");
        hydroponicHeatPerTick = c.getInt("heatPerTick", "hydroponic_chamber", 20, 1, 1000, "IC2 Heat (HU/t) drawn from adjacent heat sources for the +50% thermal speed bonus.");

        if (c.hasChanged()) c.save();
    }

    private static float finiteFloat(Configuration config, String category, String name, float fallback, float min, float max, String comment) {
        float value = config.getFloat(name, category, fallback, min, max, comment);
        if (!Float.isFinite(value)) {
            value = fallback;
            config.getCategory(category).get(name).set(value);
        }
        return value;
    }
}
