package br.cropmanager;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ManagerConfigTest {
    @BeforeClass public static void bootstrapVanilla() {
        Bootstrap.register();
        if (TileEntity.getKey(AdvancedManagerTile.class) == null)
            TileEntity.register("cropmanager:advanced_crop_manager", AdvancedManagerTile.class);
    }
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Field minecraftHome;
    private Object previousHome;

    @Before public void configureForgeHome() throws Exception {
        minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }
    @After public void restoreForgeHome() throws Exception { minecraftHome.set(null, previousHome); }

    @Test public void rejectsNaNEnergyCostsAndRepairsSavedConfiguration() throws Exception {
        File file = temporary.newFile("cropmanager.cfg");
        Configuration config = new Configuration(file);
        for (String name : new String[] {"scanEU", "careEU", "harvestEU"})
            config.get("energy", name, "NaN");
        config.get("advanced_manager", "advancedTimeMultiplier", "NaN");
        config.save();
        try {
            ManagerConfig.load(file);
            assertEquals(1, ManagerConfig.scanEU, 0);
            assertEquals(10, ManagerConfig.careEU, 0);
            assertEquals(20, ManagerConfig.harvestEU, 0);
            assertEquals(0.5, ManagerConfig.managerAdvancedTimeMultiplier, 1e-6);
            Configuration repaired = new Configuration(file);
            repaired.load();
            for (String name : new String[] {"scanEU", "careEU", "harvestEU"})
                assertFalse("NaN persisted in " + name, "NaN".equals(repaired.getCategory("energy").get(name).getString()));
            assertFalse("NaN persisted in advancedTimeMultiplier",
                    "NaN".equals(repaired.getCategory("advanced_manager").get("advancedTimeMultiplier").getString()));
        } finally {
            ManagerConfig.load(temporary.newFile("defaults.cfg"));
        }
    }

    @Test public void boundsMutagenicSettingsAndRepairsNaN() throws Exception {
        File file = temporary.newFile("mutagenic.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(12, ManagerConfig.mutagenicPowderBurstTicks);
            assertEquals(0.5, ManagerConfig.mutagenicStatBonusChance, 0.0);

            Configuration config = new Configuration(file);
            config.get("fertilizers", "mutagenicPowderBurstTicks", 12).set(18);
            config.get("fertilizers", "mutagenicStatBonusChance", 0.5).set(0.25);
            config.save();
            ManagerConfig.load(file);
            assertEquals(18, ManagerConfig.mutagenicPowderBurstTicks);
            assertEquals(0.25, ManagerConfig.mutagenicStatBonusChance, 0.0);

            config = new Configuration(file);
            config.get("fertilizers", "mutagenicPowderBurstTicks", 12).set(1000);
            config.get("fertilizers", "mutagenicStatBonusChance", 0.5).set("NaN");
            config.save();
            ManagerConfig.load(file);
            assertEquals(64, ManagerConfig.mutagenicPowderBurstTicks);
            assertEquals(0.5, ManagerConfig.mutagenicStatBonusChance, 0.0);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-mutagenic-defaults.cfg"));
        }
    }

    @Test public void boundsPollinatorHiveSettings() throws Exception {
        File file = temporary.newFile("pollinator-hive.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(2, ManagerConfig.pollinatorRadius);
            assertEquals(100, ManagerConfig.pollinatorBaseInterval);
            assertEquals(16, ManagerConfig.pollinatorChargesPerPowder);
            assertEquals(64, ManagerConfig.pollinatorMaxCharges);

            Configuration config = new Configuration(file);
            config.get("pollinator_hive", "radius", 2).set(5);
            config.get("pollinator_hive", "baseIntervalTicks", 100).set(220);
            config.get("pollinator_hive", "chargesPerPowder", 16).set(24);
            config.get("pollinator_hive", "maxCharges", 64).set(48);
            config.save();
            ManagerConfig.load(file);
            assertEquals(5, ManagerConfig.pollinatorRadius);
            assertEquals(220, ManagerConfig.pollinatorBaseInterval);
            assertEquals(24, ManagerConfig.pollinatorChargesPerPowder);
            assertEquals(48, ManagerConfig.pollinatorMaxCharges);

            config = new Configuration(file);
            config.get("pollinator_hive", "radius", 2).set(100);
            config.get("pollinator_hive", "baseIntervalTicks", 100).set(1);
            config.get("pollinator_hive", "chargesPerPowder", 16).set(1000);
            config.get("pollinator_hive", "maxCharges", 64).set(1000);
            config.save();
            ManagerConfig.load(file);
            assertEquals(8, ManagerConfig.pollinatorRadius);
            assertEquals(10, ManagerConfig.pollinatorBaseInterval);
            assertEquals(64, ManagerConfig.pollinatorChargesPerPowder);
            assertEquals(64, ManagerConfig.pollinatorMaxCharges);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-pollinator-defaults.cfg"));
        }
    }

    @Test public void boundsGreenhouseGrowthBonus() throws Exception {
        File file = temporary.newFile("greenhouse.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(20, ManagerConfig.greenhouseGrowthBonus);

            Configuration config = new Configuration(file);
            config.get("greenhouse", "growthBonus", 20).set(80);
            config.save();
            ManagerConfig.load(file);
            assertEquals(80, ManagerConfig.greenhouseGrowthBonus);

            config = new Configuration(file);
            config.get("greenhouse", "growthBonus", 20).set(500);
            config.save();
            ManagerConfig.load(file);
            assertEquals(200, ManagerConfig.greenhouseGrowthBonus);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-greenhouse-defaults.cfg"));
        }
    }

    @Test public void boundsFluidPipeSettings() throws Exception {
        File file = temporary.newFile("fluid-pipe.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(250, ManagerConfig.fluidPipeTransferMb);
            assertEquals(5, ManagerConfig.fluidPipeIntervalTicks);

            Configuration config = new Configuration(file);
            config.get("fluid_pipe", "transferMbPerPulse", 250).set(500);
            config.get("fluid_pipe", "intervalTicks", 5).set(10);
            config.save();
            ManagerConfig.load(file);
            assertEquals(500, ManagerConfig.fluidPipeTransferMb);
            assertEquals(10, ManagerConfig.fluidPipeIntervalTicks);

            config = new Configuration(file);
            config.get("fluid_pipe", "transferMbPerPulse", 250).set(999999);
            config.get("fluid_pipe", "intervalTicks", 5).set(0);
            config.save();
            ManagerConfig.load(file);
            assertEquals(10000, ManagerConfig.fluidPipeTransferMb);
            assertEquals(1, ManagerConfig.fluidPipeIntervalTicks);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-fluid-pipe-defaults.cfg"));
        }
    }

    @Test public void appliesExtendedConfigParametersAndClampsReloadedMachineCapacities() throws Exception {
        File customFile = temporary.newFile("custom-cropmanager.cfg");
        Configuration custom = new Configuration(customFile);
        custom.get("advanced_manager", "advancedEnergyCapacity", 250000);
        custom.get("advanced_manager", "advancedTankCapacity", 32000);
        custom.get("advanced_manager", "advancedSinkTier", 3);
        custom.get("monitor", "baseRange", 48);
        custom.get("monitor", "tankCapacity", 24000);
        custom.get("monitor", "wirelessEuCapacityPerUpgrade", 80000);
        custom.get("monitor", "wirelessEuRatePerUpgrade", 512);
        custom.get("weeder", "gasCapacity", 24000);
        custom.get("composter", "energyCapacity", 40000);
        custom.get("composter", "durationTicks", 120);
        custom.get("composter", "euPerTick", 8);
        custom.get("grin_grinder", "bladeDurability", 512);
        custom.get("grin_extractor", "tankCapacity", 12000);
        custom.get("grin_extractor", "waterPerRecipe", 500);
        custom.get("weed_ex_fabricator", "weedTankCapacity", 12000);
        custom.get("thermal_pipe", "maxHeatPerTick", 250);
        custom.get("database_chest", "autoCollectRadius", 6);
        custom.save();

        try {
            ManagerConfig.load(customFile);

            AdvancedManagerTile manager = new AdvancedManagerTile();
            assertEquals(250000, manager.energy.getCapacity(), 0);
            assertEquals(3, manager.energy.getSinkTier());
            assertEquals(32000, manager.tankCapacity());
            manager.energy.setEnergyStored(200000);
            manager.water.fill(new FluidStack(FluidRegistry.WATER, 30000), true);
            NBTTagCompound savedManager = manager.writeToNBT(new NBTTagCompound());

            MonitorTile monitor = new MonitorTile();
            assertEquals(48, monitor.connectionRange());
            assertEquals(24000, monitor.water.getCapacity());

            ComposterTile composter = new ComposterTile();
            assertEquals(40000, composter.energy.getCapacity(), 0);
            assertEquals(120, composter.workTicks);
            assertEquals(8, composter.euPerTick, 0);

            assertEquals(512, new ItemStack(CropManager.GRINDING_BLADE).getMaxDamage());
            assertEquals(250, new ThermalPipeTile().maxrequestHeatTick(null));
            assertEquals(24000, new WeederTile().gas.getCapacity());
            assertEquals(12000, new GrinExtractorTile().water.getCapacity());
            assertEquals(12000, new WeedExFabricatorTile().weed.getCapacity());
            assertEquals(6, ManagerConfig.chestCollectRadius);

            // Now shrink capacities and verify reload clamps existing NBT state to new limits.
            File shrunkFile = temporary.newFile("shrunk-cropmanager.cfg");
            Configuration shrunk = new Configuration(shrunkFile);
            shrunk.get("advanced_manager", "advancedEnergyCapacity", 50000);
            shrunk.get("advanced_manager", "advancedTankCapacity", 8000);
            shrunk.save();
            ManagerConfig.load(shrunkFile);

            AdvancedManagerTile reloaded = new AdvancedManagerTile();
            reloaded.readFromNBT(savedManager);
            assertEquals(50000, reloaded.energy.getCapacity(), 0);
            assertEquals(50000, reloaded.getEnergy(), 0);
            assertEquals(8000, reloaded.water.getCapacity());
            assertEquals(8000, reloaded.water.getFluidAmount());
        } finally {
            ManagerConfig.load(temporary.newFile("restore-defaults.cfg"));
        }
    }

    @Test public void ignoresLegacyGrinExtractorHeatConfigWithoutRestoringHeatDependency() throws Exception {
        File file = temporary.newFile("legacy-grin-extractor.cfg");
        Configuration legacy = new Configuration(file);
        legacy.get("grin_extractor", "heatPerTick", 20);
        legacy.get("grin_extractor", "durationTicks", 140);
        legacy.get("grin_extractor", "euPerTick", 9);
        legacy.get("grin_extractor", "tankCapacity", 24000);
        legacy.get("grin_extractor", "waterPerRecipe", 500);
        legacy.get("grin_extractor", "extractPerRecipe", 750);
        legacy.get("grin_extractor", "energyCapacity", 32000);
        legacy.get("grin_extractor", "sinkTier", 2);
        legacy.save();
        assertTrue(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).contains("I:heatPerTick=20"));

        try {
            ManagerConfig.load(file);
            assertEquals(140, ManagerConfig.extractorDuration);
            assertEquals(9, ManagerConfig.extractorEuPerTick);
            assertEquals(24000, ManagerConfig.extractorTankCapacity);
            assertEquals(500, ManagerConfig.extractorWaterPerRecipe);
            assertEquals(750, ManagerConfig.extractorExtractPerRecipe);
            assertEquals(32000, ManagerConfig.extractorEnergyCapacity);
            assertEquals(2, ManagerConfig.extractorSinkTier);

            GrinExtractorTile extractor = new GrinExtractorTile();
            assertEquals(140, extractor.workTicks);
            assertEquals(9, extractor.euPerTick, 0);
            assertEquals(24000, extractor.water.getCapacity());
            assertEquals(24000, extractor.extract.getCapacity());
            assertEquals(32000, extractor.energy.getCapacity(), 0);
            assertEquals(2, extractor.energy.getSinkTier());

            for (Field field : ManagerConfig.class.getDeclaredFields()) {
                String lower = field.getName().toLowerCase(Locale.ROOT);
                assertFalse("Legacy heat config field must not exist: " + field.getName(),
                        lower.contains("heat") && lower.contains("extractor"));
            }

            File onlyLegacyHeat = temporary.newFile("only-legacy-heat.cfg");
            Configuration onlyHeat = new Configuration(onlyLegacyHeat);
            onlyHeat.get("grin_extractor", "heatPerTick", 20);
            onlyHeat.save();
            assertTrue(new String(Files.readAllBytes(onlyLegacyHeat.toPath()), StandardCharsets.UTF_8).contains("I:heatPerTick=20"));
            ManagerConfig.load(onlyLegacyHeat);
            assertEquals(100, ManagerConfig.extractorDuration);
            assertEquals(5, ManagerConfig.extractorEuPerTick);
            assertEquals(16000, ManagerConfig.extractorTankCapacity);
            assertEquals(1000, ManagerConfig.extractorWaterPerRecipe);
            assertEquals(1000, ManagerConfig.extractorExtractPerRecipe);
            assertEquals(20000, ManagerConfig.extractorEnergyCapacity);
            assertEquals(1, ManagerConfig.extractorSinkTier);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-grin-extractor-defaults.cfg"));
        }
    }

    @Test public void boundsIndustrialTankSettings() throws Exception {
        File file = temporary.newFile("industrial-tank.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(128000, ManagerConfig.industrialTankBaseCapacity);
            assertEquals(32000, ManagerConfig.industrialTankCapacityPerUpgrade);
            assertEquals(2000, ManagerConfig.industrialTankAutoEjectRate);

            Configuration config = new Configuration(file);
            config.get("industrial_tank", "baseCapacity", 128000).set(256000);
            config.get("industrial_tank", "capacityPerUpgrade", 32000).set(64000);
            config.get("industrial_tank", "autoEjectRateMB", 2000).set(4000);
            config.save();
            ManagerConfig.load(file);
            assertEquals(256000, ManagerConfig.industrialTankBaseCapacity);
            assertEquals(64000, ManagerConfig.industrialTankCapacityPerUpgrade);
            assertEquals(4000, ManagerConfig.industrialTankAutoEjectRate);

            config = new Configuration(file);
            config.get("industrial_tank", "baseCapacity", 128000).set(100);
            config.get("industrial_tank", "capacityPerUpgrade", 32000).set(99999999);
            config.get("industrial_tank", "autoEjectRateMB", 2000).set(1);
            config.save();
            ManagerConfig.load(file);
            assertEquals(16000, ManagerConfig.industrialTankBaseCapacity);
            assertEquals(1000000, ManagerConfig.industrialTankCapacityPerUpgrade);
            assertEquals(100, ManagerConfig.industrialTankAutoEjectRate);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-industrial-tank-defaults.cfg"));
        }
    }

    @Test public void boundsAgriculturalSiloSettings() throws Exception {
        File file = temporary.newFile("agricultural-silo.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(128000, ManagerConfig.siloBaseCapacity);
            assertEquals(32000, ManagerConfig.siloCapacityPerUpgrade);
            assertEquals(64, ManagerConfig.siloAutoEjectRate);

            Configuration config = new Configuration(file);
            config.get("agricultural_silo", "baseCapacity", 128000).set(256000);
            config.get("agricultural_silo", "capacityPerUpgrade", 32000).set(64000);
            config.get("agricultural_silo", "autoEjectRate", 64).set(128);
            config.save();
            ManagerConfig.load(file);
            assertEquals(256000, ManagerConfig.siloBaseCapacity);
            assertEquals(64000, ManagerConfig.siloCapacityPerUpgrade);
            assertEquals(128, ManagerConfig.siloAutoEjectRate);

            config = new Configuration(file);
            config.get("agricultural_silo", "baseCapacity", 128000).set(100);
            config.get("agricultural_silo", "capacityPerUpgrade", 32000).set(99999999);
            config.get("agricultural_silo", "autoEjectRate", 64).set(0);
            config.save();
            ManagerConfig.load(file);
            assertEquals(1000, ManagerConfig.siloBaseCapacity);
            assertEquals(1000000, ManagerConfig.siloCapacityPerUpgrade);
            assertEquals(1, ManagerConfig.siloAutoEjectRate);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-silo-defaults.cfg"));
        }
    }

    @Test public void boundsWifiGridAndDishAntennaSettingsAndRepairsNaN() throws Exception {
        File file = temporary.newFile("wifi-dish.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(100000, ManagerConfig.wifiChannelBufferEU);
            assertEquals(4, ManagerConfig.wifiPlugSinkTier);
            assertEquals(4, ManagerConfig.wifiPointMaxOutputTier);
            assertEquals(0.0F, ManagerConfig.wifiCrossDimLossPercent, 0.0F);
            assertEquals(16, ManagerConfig.dishBaseRadius);
            assertEquals(16, ManagerConfig.dishRadiusPerUpgrade);
            assertEquals(512, ManagerConfig.dishMaxTransferPerTick);
            assertEquals(100000, ManagerConfig.dishEnergyCapacity);
            assertEquals(4, ManagerConfig.dishSinkTier);
            assertEquals(20, ManagerConfig.dishScanIntervalTicks);

            Configuration config = new Configuration(file);
            config.get("wifi_grid", "channelBufferEU", 100000).set(250000);
            config.get("wifi_grid", "plugSinkTier", 4).set(3);
            config.get("wifi_grid", "pointMaxOutputTier", 4).set(2);
            config.get("wifi_grid", "crossDimLossPercent", 0.0).set(12.5);
            config.get("dish_antenna", "baseRadius", 16).set(24);
            config.get("dish_antenna", "radiusPerUpgrade", 16).set(8);
            config.get("dish_antenna", "maxTransferPerTick", 512).set(1024);
            config.get("dish_antenna", "energyCapacity", 100000).set(200000);
            config.get("dish_antenna", "sinkTier", 4).set(2);
            config.get("dish_antenna", "scanIntervalTicks", 20).set(40);
            config.save();
            ManagerConfig.load(file);
            assertEquals(250000, ManagerConfig.wifiChannelBufferEU);
            assertEquals(3, ManagerConfig.wifiPlugSinkTier);
            assertEquals(2, ManagerConfig.wifiPointMaxOutputTier);
            assertEquals(12.5F, ManagerConfig.wifiCrossDimLossPercent, 1e-6F);
            assertEquals(24, ManagerConfig.dishBaseRadius);
            assertEquals(8, ManagerConfig.dishRadiusPerUpgrade);
            assertEquals(1024, ManagerConfig.dishMaxTransferPerTick);
            assertEquals(200000, ManagerConfig.dishEnergyCapacity);
            assertEquals(2, ManagerConfig.dishSinkTier);
            assertEquals(40, ManagerConfig.dishScanIntervalTicks);

            config = new Configuration(file);
            config.get("wifi_grid", "channelBufferEU", 100000).set(10);
            config.get("wifi_grid", "plugSinkTier", 4).set(99);
            config.get("wifi_grid", "pointMaxOutputTier", 4).set(99);
            config.get("wifi_grid", "crossDimLossPercent", 0.0).set("NaN");
            config.get("dish_antenna", "baseRadius", 16).set(1000);
            config.get("dish_antenna", "radiusPerUpgrade", 16).set(1);
            config.get("dish_antenna", "maxTransferPerTick", 512).set(999999);
            config.get("dish_antenna", "energyCapacity", 100000).set(1);
            config.get("dish_antenna", "sinkTier", 4).set(99);
            config.get("dish_antenna", "scanIntervalTicks", 20).set(1000);
            config.save();
            ManagerConfig.load(file);
            assertEquals(1000, ManagerConfig.wifiChannelBufferEU);
            assertEquals(8, ManagerConfig.wifiPlugSinkTier);
            assertEquals(4, ManagerConfig.wifiPointMaxOutputTier);
            assertEquals(0.0F, ManagerConfig.wifiCrossDimLossPercent, 0.0F);
            assertEquals(64, ManagerConfig.dishBaseRadius);
            assertEquals(4, ManagerConfig.dishRadiusPerUpgrade);
            assertEquals(16384, ManagerConfig.dishMaxTransferPerTick);
            assertEquals(1000, ManagerConfig.dishEnergyCapacity);
            assertEquals(8, ManagerConfig.dishSinkTier);
            assertEquals(200, ManagerConfig.dishScanIntervalTicks);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-wifi-dish-defaults.cfg"));
        }
    }

    @Test
    public void loadsCustomAndClampedHydroponicChamberConfig() throws Exception {
        File file = temporary.newFile("hydroponic-config.cfg");
        try {
            ManagerConfig.load(file);
            assertEquals(200, ManagerConfig.hydroponicDuration);
            assertEquals(16, ManagerConfig.hydroponicEuPerTick);
            assertEquals(50000, ManagerConfig.hydroponicEnergyCapacity);
            assertEquals(2, ManagerConfig.hydroponicSinkTier);
            assertEquals(16000, ManagerConfig.hydroponicTankCapacity);
            assertEquals(500, ManagerConfig.hydroponicWaterPerCycle);
            assertEquals(20, ManagerConfig.hydroponicHeatPerTick);

            Configuration custom = new Configuration(file);
            custom.get("hydroponic_chamber", "durationTicks", 200).set(120);
            custom.get("hydroponic_chamber", "euPerTick", 16).set(24);
            custom.get("hydroponic_chamber", "energyCapacity", 50000).set(80000);
            custom.get("hydroponic_chamber", "sinkTier", 2).set(3);
            custom.get("hydroponic_chamber", "tankCapacity", 16000).set(24000);
            custom.get("hydroponic_chamber", "waterPerCycle", 500).set(750);
            custom.get("hydroponic_chamber", "heatPerTick", 20).set(40);
            custom.save();
            ManagerConfig.load(file);
            assertEquals(120, ManagerConfig.hydroponicDuration);
            assertEquals(24, ManagerConfig.hydroponicEuPerTick);
            assertEquals(80000, ManagerConfig.hydroponicEnergyCapacity);
            assertEquals(3, ManagerConfig.hydroponicSinkTier);
            assertEquals(24000, ManagerConfig.hydroponicTankCapacity);
            assertEquals(750, ManagerConfig.hydroponicWaterPerCycle);
            assertEquals(40, ManagerConfig.hydroponicHeatPerTick);

            Configuration clamped = new Configuration(file);
            clamped.get("hydroponic_chamber", "durationTicks", 200).set(1);
            clamped.get("hydroponic_chamber", "euPerTick", 16).set(99999);
            clamped.get("hydroponic_chamber", "energyCapacity", 50000).set(5);
            clamped.get("hydroponic_chamber", "sinkTier", 2).set(99);
            clamped.get("hydroponic_chamber", "tankCapacity", 16000).set(100);
            clamped.get("hydroponic_chamber", "waterPerCycle", 500).set(999999);
            clamped.get("hydroponic_chamber", "heatPerTick", 20).set(0);
            clamped.save();
            ManagerConfig.load(file);
            assertEquals(10, ManagerConfig.hydroponicDuration);
            assertEquals(10000, ManagerConfig.hydroponicEuPerTick);
            assertEquals(1000, ManagerConfig.hydroponicEnergyCapacity);
            assertEquals(8, ManagerConfig.hydroponicSinkTier);
            assertEquals(1000, ManagerConfig.hydroponicTankCapacity);
            assertEquals(1000, ManagerConfig.hydroponicWaterPerCycle);
            assertEquals(1, ManagerConfig.hydroponicHeatPerTick);
        } finally {
            ManagerConfig.load(temporary.newFile("restore-hydroponic-defaults.cfg"));
        }
    }
}
