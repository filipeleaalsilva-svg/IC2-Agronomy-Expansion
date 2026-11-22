package br.cropmanager;

import java.io.File;
import java.lang.reflect.Field;
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
}
