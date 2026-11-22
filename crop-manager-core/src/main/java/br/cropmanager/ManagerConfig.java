package br.cropmanager;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

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

    // Advanced Crop Manager
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

    // Electric Weed Harvester (Weeder)
    public static int weederEnergyCapacity = 10000;
    public static int weederSinkTier = 1;
    public static int weederScansPerTick = 4;
    public static double weederScanEu = 1.0;
    public static double weederHarvestEu = 20.0;
    public static int weederGasCapacity = 16000;

    // Fertilizer Composter
    public static int composterEnergyCapacity = 10000;
    public static int composterSinkTier = 1;
    public static int composterTankCapacity = 16000;
    public static int composterRequiredItems = 4;
    public static int composterWaterPerRecipe = 1000;
    public static int composterWeedItems = 2;
    public static int composterWeedWater = 500;
    public static int composterDuration = 200;
    public static double composterEuPerTick = 5.0;

    // Weed-Ex Mixer (WeedExFabricatorTile)
    public static int fabricatorEnergyCapacity = 10000;
    public static int fabricatorSinkTier = 1;
    public static int fabricatorSideTankCapacity = 4000;
    public static int fabricatorWeedCapacity = 8000;
    public static int fabricatorWaterPerConvert = 1000;
    public static int fabricatorSidePerMix = 500;
    public static int fabricatorWeedPerRecipe = 1000;
    public static int fabricatorDuration = 100;
    public static double fabricatorEuPerTick = 5.0;

    // Crop Database Chest
    public static int chestCollectRadius = 3;
    public static int chestCollectIntervalTicks = 10;
    public static int chestCollectMaxItems = 32;

    private ManagerConfig() {}

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

        managerAdvancedEnergyCapacity = c.getInt("advancedEnergyCapacity", "advanced_manager", 100000, 1000, 10000000, "Base EU capacity for Advanced Crop Managers.");
        managerAdvancedSinkTier = c.getInt("advancedSinkTier", "advanced_manager", 2, 1, 8, "Base IC2 energy tier for Advanced Crop Managers.");
        managerAdvancedTimeMultiplier = finiteFloat(c, "advanced_manager", "advancedTimeMultiplier", 0.5F, 0.05F, 10.0F, "Base interval multiplier for Advanced Crop Managers before IC2 upgrades.");
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

        weederEnergyCapacity = c.getInt("energyCapacity", "weeder", 10000, 1000, 10000000, "Base EU buffer capacity before Energy Storage upgrades.");
        weederSinkTier = c.getInt("sinkTier", "weeder", 1, 1, 8, "Base IC2 energy tier before Transformer upgrades.");
        weederScansPerTick = c.getInt("scansPerTick", "weeder", 4, 1, 32, "Positions scanned per tick when active.");
        weederScanEu = finiteFloat(c, "weeder", "scanEU", 1.0F, 0.0F, 1000.0F, "EU consumed per loaded crop tile scanned.");
        weederHarvestEu = finiteFloat(c, "weeder", "harvestEU", 20.0F, 0.0F, 1000.0F, "Additional EU consumed per weed harvested.");

        composterEnergyCapacity = c.getInt("energyCapacity", "composter", 10000, 1000, 10000000, "Base EU buffer capacity before Energy Storage upgrades.");
        composterSinkTier = c.getInt("sinkTier", "composter", 1, 1, 8, "Base IC2 energy tier before Transformer upgrades.");
        composterTankCapacity = c.getInt("tankCapacity", "composter", 16000, 1000, 1000000, "Internal water tank capacity in mB.");
        composterRequiredItems = c.getInt("requiredItems", "composter", 4, 1, 64, "Organic items consumed per fertilizer crafted.");
        composterWaterPerRecipe = c.getInt("waterPerRecipe", "composter", 1000, 0, 16000, "Water in mB consumed per fertilizer crafted from common organics.");
        composterWeedItems = c.getInt("weedItems", "composter", 2, 1, 64, "IC2 Weed items consumed per fertilizer crafted (2x efficiency bonus).");
        composterWeedWater = c.getInt("weedWater", "composter", 500, 0, 16000, "Water in mB consumed per fertilizer crafted from IC2 Weed (2x efficiency bonus).");
        composterDuration = c.getInt("durationTicks", "composter", 200, 1, 72000, "Base processing time in ticks without Overclockers.");
        composterEuPerTick = finiteFloat(c, "composter", "euPerTick", 5.0F, 1.0F, 10000.0F, "Base EU/t consumed while composting, before Overclockers.");

        fabricatorEnergyCapacity = c.getInt("energyCapacity", "weed_ex_fabricator", 10000, 1000, 10000000, "Internal EU buffer capacity.");
        fabricatorSinkTier = c.getInt("sinkTier", "weed_ex_fabricator", 1, 1, 8, "IC2 energy input tier.");
        fabricatorSideTankCapacity = c.getInt("sideTankCapacity", "weed_ex_fabricator", 4000, 1000, 1000000, "Capacity (mB) of the Spider Extract and Liquid Redstone side tanks.");
        fabricatorWeedCapacity = c.getInt("weedTankCapacity", "weed_ex_fabricator", 8000, 1000, 1000000, "Output Weed-Ex tank capacity (mB).");
        fabricatorWaterPerConvert = c.getInt("waterPerConvert", "weed_ex_fabricator", 1000, 1, 16000, "Water (mB) converted per Spider Eye or Redstone item into side tank extract.");
        fabricatorSidePerMix = c.getInt("sidePerMix", "weed_ex_fabricator", 500, 1, 16000, "Spider Extract and Liquid Redstone (mB) consumed from each side tank per Weed-Ex batch.");
        fabricatorWeedPerRecipe = c.getInt("weedExPerRecipe", "weed_ex_fabricator", 1000, 1, 16000, "Weed-Ex (mB) produced per mixing batch.");
        fabricatorDuration = c.getInt("durationTicks", "weed_ex_fabricator", 100, 1, 72000, "Ticks required per Weed-Ex mixing batch.");
        fabricatorEuPerTick = finiteFloat(c, "weed_ex_fabricator", "euPerTick", 5.0F, 1.0F, 10000.0F, "EU/t consumed while mixing.");

        chestCollectRadius = c.getInt("autoCollectRadius", "database_chest", 3, 1, 16, "Block radius around the chest to collect dropped seed bags.");
        chestCollectIntervalTicks = c.getInt("autoCollectIntervalTicks", "database_chest", 10, 1, 200, "Ticks between dropped seed collection scans.");
        chestCollectMaxItems = c.getInt("autoCollectMaxItemsPerScan", "database_chest", 32, 1, 256, "Maximum entity items inspected per collection scan.");

        if (c.hasChanged()) c.save();
    }

    private static float finiteFloat(Configuration c, String category, String name, float def, float min, float max, String comment) {
        float value = c.getFloat(name, category, def, min, max, comment);
        if (!Float.isFinite(value)) {
            value = def;
            c.getCategory(category).get(name).set(value);
        }
        return Math.max(min, Math.min(max, value));
    }
}
