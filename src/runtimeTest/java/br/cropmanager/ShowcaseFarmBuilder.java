package br.cropmanager;

import br.cropmanager.chest.block.BlockCropDatabaseChest;
import br.cropmanager.chest.inventory.PortableCropInventory;
import br.cropmanager.chest.inventory.SeedSortMode;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import br.cropmanager.chest.item.ItemUpgradeLinker;
import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.CropRegistry;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.item.IC2Items;
import ic2.core.block.machine.tileentity.TileEntityCropmatron;
import ic2.core.block.wiring.TileEntityElectricBatBox;
import ic2.core.block.wiring.TileEntityElectricMFE;
import ic2.core.block.wiring.TileEntityElectricMFSU;
import ic2.core.crop.TileEntityCrop;
import ic2.core.item.ItemCropSeed;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Builds a complete, self-sustaining Agronomy Complex showcasing every block,
 * multiblock, pipe, wireless EU tier, machine, tool and custom IC2 crop in the mod.
 * Kept strictly inside src/runtimeTest so the release JAR remains untouched.
 */
public final class ShowcaseFarmBuilder {
    public static final String SAVE_FOLDER = "cropmanager-showcase-farm";
    public static final BlockPos DEFAULT_ORIGIN = new BlockPos(0, 4, 0);

    private static final int HALF_X = 36;
    private static final int HALF_Z = 34;

    private ShowcaseFarmBuilder() {
    }

    public static final class ShowcaseSummary {
        public final BlockPos origin;
        public final BlockPos receptionPos;
        public final BlockPos hubCenter;
        public final BlockPos managerPos;
        public final BlockPos monitorPos;
        public final BlockPos databaseChestPos;
        public final BlockPos wifiPlugPos;
        public final BlockPos wifiPointPos;
        public final BlockPos dishAntennaPos;
        public final BlockPos reservoirController;
        public final BlockPos tankController;
        public final BlockPos siloController;
        public final BlockPos biogasGenController;
        public final BlockPos grinderPos;
        public final BlockPos weederPos;
        public final BlockPos pollinatorPos;
        public final BlockPos radbloomPos;
        public final BlockPos[] hydroponicChambers;
        public final BlockPos[] greenhouseCrops;
        public final BlockPos[] sprinklers;
        public final BlockPos diagonalSprinklerPos;

        ShowcaseSummary(
                BlockPos origin,
                BlockPos receptionPos,
                BlockPos hubCenter,
                BlockPos managerPos,
                BlockPos monitorPos,
                BlockPos databaseChestPos,
                BlockPos wifiPlugPos,
                BlockPos wifiPointPos,
                BlockPos dishAntennaPos,
                BlockPos reservoirController,
                BlockPos tankController,
                BlockPos siloController,
                BlockPos biogasGenController,
                BlockPos grinderPos,
                BlockPos weederPos,
                BlockPos pollinatorPos,
                BlockPos radbloomPos,
                BlockPos[] hydroponicChambers,
                BlockPos[] greenhouseCrops,
                BlockPos[] sprinklers,
                BlockPos diagonalSprinklerPos) {
            this.origin = origin;
            this.receptionPos = receptionPos;
            this.hubCenter = hubCenter;
            this.managerPos = managerPos;
            this.monitorPos = monitorPos;
            this.databaseChestPos = databaseChestPos;
            this.wifiPlugPos = wifiPlugPos;
            this.wifiPointPos = wifiPointPos;
            this.dishAntennaPos = dishAntennaPos;
            this.reservoirController = reservoirController;
            this.tankController = tankController;
            this.siloController = siloController;
            this.biogasGenController = biogasGenController;
            this.grinderPos = grinderPos;
            this.weederPos = weederPos;
            this.pollinatorPos = pollinatorPos;
            this.radbloomPos = radbloomPos;
            this.hydroponicChambers = hydroponicChambers;
            this.greenhouseCrops = greenhouseCrops;
            this.sprinklers = sprinklers;
            this.diagonalSprinklerPos = diagonalSprinklerPos;
        }
    }

    public static ShowcaseSummary build(WorldServer world, BlockPos origin, String locale, @Nullable EntityPlayerMP player) {
        boolean pt = locale == null || !locale.toLowerCase().startsWith("en");
        configureWorldRules(world);
        clearAndPaveCampus(world, origin);

        // Sector anchors relative to origin (0, 4, 0)
        BlockPos receptionPos = origin.add(0, 0, 26);
        BlockPos hubCenter = origin.add(0, 0, 8);
        BlockPos farmCenter = origin.add(0, 0, -14);
        BlockPos multiblockOrigin = origin.add(20, 0, -2);
        BlockPos chemOrigin = origin.add(-20, 0, -2);
        BlockPos greenhouseOrigin = origin.add(-20, 0, 18);
        BlockPos hydroOrigin = origin.add(20, 0, 18);
        BlockPos irrigationOrigin = origin.add(20, 0, -20);

        // 1. Central Hub & Wireless EU Tower
        BlockPos monitorPos = hubCenter.add(-2, 0, 0);
        BlockPos databaseChestPos = hubCenter.add(2, 0, 0);
        BlockPos wifiPlugPos = hubCenter.add(0, 1, -2);
        BlockPos wifiPointPos = hubCenter.add(2, 1, -2);
        BlockPos dishAntennaPos = hubCenter.add(0, 3, -2);

        // 2. North Automated Farm
        BlockPos managerPos = farmCenter.add(0, 0, 5);

        // Build Hub first so Wi-Fi grid & MFSU are ready
        buildHubSector(world, hubCenter, managerPos, monitorPos, databaseChestPos, wifiPlugPos, wifiPointPos, dishAntennaPos, pt);

        // Build North Farm (Advanced Crop Manager + Crop-Matron + 9x9 Crops)
        buildAutomatedFarmSector(world, farmCenter, managerPos, wifiPlugPos, pt);
        TileEntity monitorTe = world.getTileEntity(monitorPos);
        if (monitorTe instanceof MonitorTile) {
            ((MonitorTile) monitorTe).link(managerPos, world.provider.getDimension());
            ((MonitorTile) monitorTe).refresh();
        }

        // Build East Multiblock Courtyard (4 formed multiblocks + fluid pipes)
        BlockPos[] mbControllers = buildMultiblockCourtyard(world, multiblockOrigin, managerPos, pt);
        BlockPos reservoirController = mbControllers[0];
        BlockPos tankController = mbControllers[1];
        BlockPos siloController = mbControllers[2];
        BlockPos biogasGenController = mbControllers[3];

        // Build West Chemistry & Biogas Shed
        BlockPos[] chemAnchors = buildChemistryAndBiogasSector(world, chemOrigin, pt);
        BlockPos grinderPos = chemAnchors[0];
        BlockPos weederPos = chemAnchors[1];

        // Build Southwest Botanical Greenhouse (15 Custom Crops + Pollinator + 4 Lanterns + Radbloom Dome)
        GreenhouseResult ghResult = buildBotanicalGreenhouseSector(world, greenhouseOrigin, pt);

        // Build Southeast Hydroponic Thermal Lab (3 Chambers + Heat Burner + Thermal Pipes)
        BlockPos[] hydroChambers = buildHydroponicLabSector(world, hydroOrigin, pt);

        // Build Northeast Irrigation Field (Sprinklers Mk1-3 + Diagonal Sprinkler)
        IrrigationResult irrResult = buildIrrigationSector(world, irrigationOrigin, pt);

        // Build South Reception & Visitor Kit / Hazmat Chest
        buildReceptionSector(world, receptionPos, managerPos, databaseChestPos, player, pt);

        // Remove stray EntityItems dropped during setup
        AxisAlignedBB campusBox = new AxisAlignedBB(
                origin.getX() - HALF_X - 4, origin.getY() - 2, origin.getZ() - HALF_Z - 4,
                origin.getX() + HALF_X + 4, origin.getY() + 18, origin.getZ() + HALF_Z + 4);
        for (EntityItem drop : world.getEntitiesWithinAABB(EntityItem.class, campusBox)) {
            drop.setDead();
        }

        return new ShowcaseSummary(
                origin, receptionPos, hubCenter, managerPos, monitorPos, databaseChestPos,
                wifiPlugPos, wifiPointPos, dishAntennaPos,
                reservoirController, tankController, siloController, biogasGenController,
                grinderPos, weederPos, ghResult.pollinatorPos, ghResult.radbloomPos,
                hydroChambers, ghResult.cropPositions, irrResult.sprinklers, irrResult.diagonalPos
        );
    }

    private static void configureWorldRules(WorldServer world) {
        world.setWorldTime(6000);
        world.getWorldInfo().setCleanWeatherTime(100000);
        world.getWorldInfo().setRainTime(0);
        world.getWorldInfo().setRaining(false);
        world.getWorldInfo().setThundering(false);
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        world.getGameRules().setOrCreateGameRule("doWeatherCycle", "false");
    }

    private static void clearAndPaveCampus(WorldServer world, BlockPos origin) {
        IBlockState grass = Blocks.GRASS.getDefaultState();
        IBlockState stoneBrick = Blocks.STONEBRICK.getDefaultState();
        IBlockState chiseled = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED);
        IBlockState path = Blocks.GRASS_PATH.getDefaultState();

        for (int dx = -HALF_X; dx <= HALF_X; dx++) {
            for (int dz = -HALF_Z; dz <= HALF_Z; dz++) {
                BlockPos floor = origin.add(dx, -1, dz);
                for (int dy = 0; dy <= 14; dy++) {
                    world.setBlockToAir(origin.add(dx, dy, dz));
                }
                boolean border = Math.abs(dx) == HALF_X || Math.abs(dz) == HALF_Z;
                boolean mainCross = Math.abs(dx) <= 1 || Math.abs(dz - 8) <= 1 || Math.abs(dz + 2) <= 1 || Math.abs(dz - 18) <= 1;
                if (border) {
                    world.setBlockState(floor, chiseled, 2);
                    world.setBlockState(floor.up(), CropManager.GARDEN_TRELLIS.getDefaultState(), 2);
                } else if (mainCross) {
                    world.setBlockState(floor, stoneBrick, 2);
                } else if (Math.abs(dx) <= 6 && Math.abs(dz - 8) <= 6) {
                    world.setBlockState(floor, stoneBrick, 2);
                } else if (Math.abs(dx) <= 3 && dz >= 20 && dz <= 30) {
                    world.setBlockState(floor, path, 2);
                } else {
                    world.setBlockState(floor, grass, 2);
                }
            }
        }

        // Walkway lampposts using the 4 custom lanterns
        FarmLanternBlock[] lanterns = {
                CropManager.FARM_LANTERN, CropManager.INDUSTRIAL_LANTERN,
                CropManager.WOODEN_SCONCE, CropManager.GLASS_LAMP
        };
        int[][] posts = {
                {-3, 20}, {3, 20}, {-5, 13}, {5, 13},
                {-5, 3}, {5, 3}, {-11, -2}, {11, -2},
                {-11, 18}, {11, 18}, {-3, -6}, {3, -6}
        };
        for (int i = 0; i < posts.length; i++) {
            BlockPos base = origin.add(posts[i][0], 0, posts[i][1]);
            world.setBlockState(base, CropManager.GREENHOUSE_FRAME.getDefaultState(), 2);
            world.setBlockState(base.up(), CropManager.GREENHOUSE_FRAME.getDefaultState(), 2);
            world.setBlockState(base.up(2), lanterns[i % lanterns.length].getDefaultState()
                    .withProperty(FarmLanternBlock.FACING, EnumFacing.DOWN), 2);
        }
    }

    private static void buildReceptionSector(WorldServer world, BlockPos pos, BlockPos managerPos, BlockPos chestPos,
                                             @Nullable EntityPlayerMP player, boolean pt) {
        IBlockState frame = CropManager.GREENHOUSE_FRAME.getDefaultState();
        IBlockState glass = CropManager.GREENHOUSE_GLASS.getDefaultState();
        // Archway
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(pos.add(dx, -1, dz), Blocks.STONEBRICK.getDefaultState(), 2);
            }
        }
        for (int dy = 0; dy <= 3; dy++) {
            world.setBlockState(pos.add(-3, dy, 0), frame, 2);
            world.setBlockState(pos.add(3, dy, 0), frame, 2);
        }
        for (int dx = -2; dx <= 2; dx++) {
            world.setBlockState(pos.add(dx, 3, 0), glass, 2);
        }
        world.setBlockState(pos.add(-3, 4, 0), CropManager.FARM_LANTERN.getDefaultState()
                .withProperty(FarmLanternBlock.FACING, EnumFacing.DOWN), 2);
        world.setBlockState(pos.add(3, 4, 0), CropManager.FARM_LANTERN.getDefaultState()
                .withProperty(FarmLanternBlock.FACING, EnumFacing.DOWN), 2);

        placeSign(world, pos.add(0, 0, 2), EnumFacing.SOUTH,
                pt ? "IC2: Agronomy" : "IC2: Agronomy",
                pt ? "Fazenda Showcase" : "Showcase Farm",
                pt ? "/cmshowcase tp" : "/cmshowcase tp",
                pt ? "Kit nos Baus ->" : "Kits in Chests ->");

        // Visitor Kit Chest
        BlockPos kitChestPos = pos.add(-2, 0, -1);
        world.setBlockState(kitChestPos, Blocks.CHEST.getDefaultState(), 3);
        TileEntityChest kitChest = (TileEntityChest) world.getTileEntity(kitChestPos);
        if (kitChest != null) {
            ItemStack portable = createLinkedPortable(world, chestPos);
            ItemStack linker = new ItemStack(CropManager.UPGRADE_LINKER);
            ItemUpgradeLinker.setLink(linker, world.provider.getDimension(), chestPos);
            ItemStack probe = createLinkedProbe(world, managerPos);
            ItemStack crossing = createLoadedCrossingTool();

            kitChest.setInventorySlotContents(0, new ItemStack(CropManager.AGRICULTURAL_MANUAL));
            kitChest.setInventorySlotContents(1, portable);
            kitChest.setInventorySlotContents(2, linker);
            kitChest.setInventorySlotContents(3, probe);
            kitChest.setInventorySlotContents(4, crossing);
            kitChest.setInventorySlotContents(5, new ItemStack(CropManager.ORGANIC_COMPOST, 64));
            kitChest.setInventorySlotContents(6, new ItemStack(CropManager.CATALYTIC_FERTILIZER, 64));
            kitChest.setInventorySlotContents(7, new ItemStack(CropManager.MUTAGENIC_POWDER, 64));
            kitChest.setInventorySlotContents(8, new ItemStack(CropManager.BIO_FIBER, 64));
            kitChest.setInventorySlotContents(9, new ItemStack(CropManager.NITRO_ESSENCE, 64));
        }

        // Hazmat Safety Chest
        BlockPos hazmatChestPos = pos.add(2, 0, -1);
        world.setBlockState(hazmatChestPos, Blocks.CHEST.getDefaultState(), 3);
        TileEntityChest hazmatChest = (TileEntityChest) world.getTileEntity(hazmatChestPos);
        if (hazmatChest != null) {
            hazmatChest.setInventorySlotContents(0, IC2Items.getItem("hazmat_helmet"));
            hazmatChest.setInventorySlotContents(1, IC2Items.getItem("hazmat_chestplate"));
            hazmatChest.setInventorySlotContents(2, IC2Items.getItem("hazmat_leggings"));
            hazmatChest.setInventorySlotContents(3, IC2Items.getItem("rubber_boots"));
        }

        if (player != null) {
            player.inventory.clear();
            player.inventory.setInventorySlotContents(0, createLinkedPortable(world, chestPos));
            PortableCropInventory inv = PortableCropInventory.open(player, 0);
            if (inv != null) {
                inv.setPickupEnabled(true);
            }
            player.inventory.setInventorySlotContents(1, new ItemStack(CropManager.AGRICULTURAL_MANUAL));
            player.inventory.setInventorySlotContents(2, createLinkedProbe(world, managerPos));
            player.inventory.setInventorySlotContents(3, createLoadedCrossingTool());
            player.inventory.setInventorySlotContents(4, new ItemStack(CropManager.CATALYTIC_FERTILIZER, 64));
            player.inventory.setInventorySlotContents(5, new ItemStack(CropManager.MUTAGENIC_POWDER, 64));
            player.inventory.setInventorySlotContents(6, new ItemStack(CropManager.ORGANIC_COMPOST, 64));
        }
    }

    private static void buildHubSector(
            WorldServer world, BlockPos center, BlockPos managerPos, BlockPos monitorPos,
            BlockPos chestPos, BlockPos plugPos, BlockPos pointPos, BlockPos dishPos, boolean pt) {

        placeSign(world, center.add(0, 0, 4), EnumFacing.SOUTH,
                pt ? "1. Hub Central" : "1. Central Hub",
                pt ? "Monitor & Bau 500" : "Monitor & 500 Chest",
                pt ? "Torre Wi-Fi EU" : "EU Wi-Fi & Dish",
                pt ? "Analisador Sementes" : "Seed Analyzer");

        // Monitor linked to Advanced Crop Manager
        world.setBlockState(monitorPos, CropManager.MONITOR.getDefaultState()
                .withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        MonitorTile monitor = (MonitorTile) world.getTileEntity(monitorPos);
        monitor.link(managerPos, world.provider.getDimension());
        for (int i = 0; i < 4; i++) {
            monitor.inventory.setStackInSlot(i, new ItemStack(CropManager.WIRELESS_RANGE));
        }
        monitor.inventory.setStackInSlot(7, new ItemStack(CropManager.HARVEST_TRANSFER));
        monitor.inventory.setStackInSlot(MonitorTile.WIRELESS_EU_SLOT, createWifiLinkedUpgrade(1, 4));
        monitor.outputMode = 2;

        // Crop Database Chest populated with all 15 custom species (level 4) + classic IC2 crops
        world.setBlockState(chestPos, CropManager.DATABASE_CHEST.getDefaultState()
                .withProperty(BlockCropDatabaseChest.FACING, EnumFacing.SOUTH), 3);
        TileCropDatabaseChest chest = (TileCropDatabaseChest) world.getTileEntity(chestPos);
        for (BaseCropCard card : CropRegistry.ALL) {
            ItemStack bag = ItemCropSeed.generateItemStackFromValues(card, 21, 31, 0, 4);
            chest.seeds().insertAnywhere(bag, false);
        }
        String[] ic2Ids = {"wheat", "reed", "cyazint", "aurelia", "ferru"};
        for (String id : ic2Ids) {
            CropCard card = Crops.instance.getCropCard("ic2", id);
            if (card != null) {
                chest.seeds().insertAnywhere(ItemCropSeed.generateItemStackFromValues(card, 20, 28, 2, 4), false);
            }
        }
        chest.setCollectingEnabled(true);
        chest.setRedstoneMode(0);
        chest.seeds().sort(SeedSortMode.TIER);

        // Seed Analyzer
        BlockPos analyzerPos = center.add(0, 0, 1);
        world.setBlockState(analyzerPos, CropManager.SEED_ANALYZER.getDefaultState()
                .withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        SeedAnalyzerTile analyzer = (SeedAnalyzerTile) world.getTileEntity(analyzerPos);
        analyzer.energy.setEnergyStored(analyzer.energy.getCapacity());
        analyzer.inventory.setStackInSlot(0, ItemCropSeed.generateItemStackFromValues(CropRegistry.GRINBLOOM, 12, 16, 4, 1));
        analyzer.inventory.setStackInSlot(2, createWifiLinkedUpgrade(1, 1));

        // Wireless EU Tower: MFSU at base, Plug + Point at y+1, Frame + MFE at y+2, Dish Antenna at y+3
        BlockPos mfsuPos = plugPos.down();
        world.setBlockState(mfsuPos, IC2Items.getItemAPI().getBlockState("te", "mfsu"), 2);
        TileEntityElectricMFSU mfsu = new TileEntityElectricMFSU();
        world.setTileEntity(mfsuPos, mfsu);
        mfsu.energy.addEnergy(mfsu.energy.getCapacity());

        world.setBlockState(plugPos, CropManager.EU_WIFI_PLUG.getDefaultState()
                .withProperty(EuWifiBlock.FACING, EnumFacing.SOUTH), 3);
        EuWifiPlugTile plug = (EuWifiPlugTile) world.getTileEntity(plugPos);
        plug.channel = 1;
        plug.energy.setEnergyStored(plug.energy.getCapacity());

        WirelessEuGridData.get(world).extractEu(1, Double.MAX_VALUE, world.provider.getDimension(), null);
        WirelessEuGridData.get(world).insertEu(1, ManagerConfig.wifiChannelBufferEU, world.provider.getDimension(), null, "Showcase");

        world.setBlockState(pointPos.down(), CropManager.GREENHOUSE_FRAME.getDefaultState(), 2);
        world.setBlockState(pointPos, CropManager.EU_WIFI_POINT.getDefaultState()
                .withProperty(EuWifiBlock.FACING, EnumFacing.SOUTH), 3);
        EuWifiPointTile point = (EuWifiPointTile) world.getTileEntity(pointPos);
        point.channel = 1;
        point.outputTier = 3;
        point.rateLimit = EuWifiPointTile.maxRateForTier(3);
        point.capacity = Math.max(4096.0, point.rateLimit * 4.0);
        point.redstoneMode = 0;
        point.storedEu = point.capacity;

        BlockPos mfePos = dishPos.down();
        world.setBlockState(mfePos, IC2Items.getItemAPI().getBlockState("te", "mfe"), 2);
        TileEntityElectricMFE mfe = new TileEntityElectricMFE();
        world.setTileEntity(mfePos, mfe);
        mfe.energy.addEnergy(mfe.energy.getCapacity());

        world.setBlockState(dishPos, CropManager.EU_DISH_ANTENNA.getDefaultState()
                .withProperty(EuDishAntennaBlock.FACING, EnumFacing.SOUTH), 3);
        EuDishAntennaTile dish = (EuDishAntennaTile) world.getTileEntity(dishPos);
        dish.redstoneMode = 0;
        dish.energy.setEnergyStored(dish.energy.getCapacity());
        for (int i = 0; i < EuDishAntennaTile.MAX_RANGE_UPGRADES; i++) {
            dish.inventory.setStackInSlot(i, new ItemStack(CropManager.WIRELESS_RANGE));
        }
        plug.update();
        point.update();
        dish.update();
    }

    private static void buildAutomatedFarmSector(WorldServer world, BlockPos center, BlockPos managerPos,
                                                 BlockPos wifiPlugPos, boolean pt) {
        // 9x9 farmland centered at farmCenter
        IBlockState farmland = Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos plot = center.add(dx, 0, dz);
                if (dx == 0 && dz == 0) {
                    world.setBlockState(plot.down(), Blocks.WATER.getDefaultState(), 2);
                    world.setBlockState(plot, CropManager.GREENHOUSE_GLASS.getDefaultState(), 2);
                } else {
                    world.setBlockState(plot.down(), farmland, 2);
                }
            }
        }

        // Plant mixed IC2 & custom crops across the 9x9 field
        CropCard[] pool = {
                Crops.instance.getCropCard("ic2", "wheat"),
                Crops.instance.getCropCard("ic2", "reed"),
                CropRegistry.BIOREED,
                CropRegistry.GRINBLOOM,
                CropRegistry.NITROBULB,
                CropRegistry.GLOWBLOOM
        };
        int idx = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos cp = center.add(dx, 0, dz);
                CropCard card = pool[idx % pool.length];
                if (card != null) {
                    int size = Math.min(card.getMaxSize(), 1 + (idx % card.getMaxSize()));
                    plantCrop(world, cp, card, size, 4, 16, 20, 0, false);
                }
                idx++;
            }
        }

        // Place Advanced Crop Manager on South edge of the 9x9 field
        world.setBlockState(managerPos, CropManager.ADVANCED.getDefaultState()
                .withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        AdvancedManagerTile manager = (AdvancedManagerTile) world.getTileEntity(managerPos);
        manager.enabled = true;
        manager.redstoneMode = 0;
        manager.settings(4, 2, 15, 0, 0, -5);
        manager.functions = 15;
        manager.energy.setEnergyStored(manager.energy.getCapacity());
        manager.water.fill(new FluidStack(FluidRegistry.WATER, manager.water.getCapacity()), true);
        manager.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), manager.weed.getCapacity()), true);
        manager.inventory.setStackInSlot(0, new ItemStack(CropManager.ORGANIC_COMPOST, 64));
        manager.inventory.setStackInSlot(1, new ItemStack(IC2Items.getItem("crop_res", "fertilizer").getItem(), 64,
                IC2Items.getItem("crop_res", "fertilizer").getMetadata()));
        manager.inventory.setStackInSlot(18, IC2Items.getItem("upgrade", "overclocker"));
        manager.inventory.setStackInSlot(19, IC2Items.getItem("upgrade", "energy_storage"));
        manager.inventory.setStackInSlot(20, new ItemStack(CropManager.WEED_KILLER));
        manager.inventory.setStackInSlot(21, createWifiLinkedUpgrade(1, 1));

        // Pulse manager to populate Data/Survey tab & harvest items
        for (int t = 0; t < 800; t++) {
            manager.update();
        }
        // Keep a clean wireless link via Dish/Wifi plug
        if (world.getTileEntity(wifiPlugPos) instanceof EuWifiPlugTile) {
            ((EuWifiPlugTile) world.getTileEntity(wifiPlugPos)).update();
        }

        // Native IC2 Crop-Matron for side-by-side comparison
        BlockPos matronPos = managerPos.east(2);
        TileEntityCropmatron matron = new TileEntityCropmatron();
        if (ItemBlockTileEntity.placeTeBlock(IC2Items.getItem("te", "cropmatron"), null, world, matronPos, EnumFacing.NORTH, matron)) {
            matron.getComponent(ic2.core.block.comp.Energy.class).addEnergy(2000);
            matron.getWaterTank().fill(new FluidStack(FluidRegistry.WATER, 8000), true);
            matron.getExTank().fill(new FluidStack(FluidName.weed_ex.getInstance(), 4000), true);
            matron.fertilizerSlot.put(IC2Items.getItem("crop_res", "fertilizer"));
        }

        placeSign(world, managerPos.south(2), EnumFacing.SOUTH,
                pt ? "2. Lavoura Auto." : "2. Auto. Farm",
                pt ? "Advanced Manager" : "Advanced Manager",
                pt ? "Censo + WeedKiller" : "Survey + WeedKiller",
                pt ? "vs Crop-Matron IC2" : "vs IC2 Crop-Matron");
    }

    private static BlockPos[] buildMultiblockCourtyard(WorldServer world, BlockPos origin, BlockPos managerPos, boolean pt) {
        // 4 multiblocks lined up along Z, facing WEST toward the central walkway
        BlockPos resCenter = origin.add(4, 0, -6);
        BlockPos tankCenter = origin.add(4, 0, 0);
        BlockPos siloCenter = origin.add(4, 0, 6);
        BlockPos genCenter = origin.add(10, 0, 0);

        // 1) Agricultural Reservoir (3x5x3)
        buildShell(world, resCenter, 5, CropManager.RESERVOIR_CASING.getDefaultState());
        BlockPos resCtrl = resCenter.west().up();
        world.setBlockState(resCtrl, CropManager.AGRICULTURAL_RESERVOIR.getDefaultState()
                .withProperty(AgriculturalReservoirBlock.FACING, EnumFacing.WEST), 3);
        AgriculturalReservoirTile reservoir = (AgriculturalReservoirTile) world.getTileEntity(resCtrl);
        reservoir.tryForm();
        reservoir.inventory.setStackInSlot(4, new ItemStack(CropManager.RESERVOIR_CAPACITY, 4));
        reservoir.inventory.setStackInSlot(5, new ItemStack(CropManager.RESERVOIR_PUMP));
        reservoir.inventory.setStackInSlot(6, new ItemStack(CropManager.RESERVOIR_RESERVE));
        reservoir.energy.setEnergyStored(reservoir.energy.getCapacity());
        reservoir.water.fill(new FluidStack(FluidRegistry.WATER, 80000), true);
        reservoir.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), 64000), true);
        placeSign(world, resCtrl.west(2), EnumFacing.WEST,
                pt ? "Reservatorio" : "Agri. Reservoir",
                pt ? "3x5x3 (2 Tanques)" : "3x5x3 (2 Tanks)",
                pt ? "Agua + Weed-Ex" : "Water + Weed-Ex",
                pt ? "Bomba & Reserva" : "Pump & Reserve");

        // 2) Industrial Fluid Tank (3x5x3) - Biogas Reserve
        buildShell(world, tankCenter, 5, CropManager.FLUID_TANK_CASING.getDefaultState());
        BlockPos tankCtrl = tankCenter.west().up();
        world.setBlockState(tankCtrl, CropManager.FLUID_TANK.getDefaultState()
                .withProperty(IndustrialTankBlock.FACING, EnumFacing.WEST), 3);
        IndustrialTankTile tank = (IndustrialTankTile) world.getTileEntity(tankCtrl);
        tank.tryForm();
        tank.inventory.setStackInSlot(2, new ItemStack(CropManager.RESERVOIR_CAPACITY, 8));
        tank.tank.fill(new FluidStack(FluidName.biogas.getInstance(), 256000), true);
        placeSign(world, tankCtrl.west(2), EnumFacing.WEST,
                pt ? "Tanque Industrial" : "Industrial Tank",
                "3x5x3 (Zero-EU)",
                pt ? "Biogas 384.000 mB" : "Biogas 384,000 mB",
                pt ? "8x Capacidade" : "8x Capacity");

        // 3) Agricultural Silo (3x5x3) - Locked Bio-Fiber Stock
        buildShell(world, siloCenter, 5, CropManager.SILO_CASING.getDefaultState());
        BlockPos siloCtrl = siloCenter.west().up();
        world.setBlockState(siloCtrl, CropManager.AGRICULTURAL_SILO.getDefaultState()
                .withProperty(AgriculturalSiloBlock.FACING, EnumFacing.WEST), 3);
        AgriculturalSiloTile silo = (AgriculturalSiloTile) world.getTileEntity(siloCtrl);
        silo.tryForm();
        silo.upgrades.setStackInSlot(0, new ItemStack(CropManager.RESERVOIR_CAPACITY, 8));
        silo.insertBulk(new ItemStack(CropManager.BIO_FIBER, 8192), false);
        NBTTagCompound siloTag = silo.writeToNBT(new NBTTagCompound());
        siloTag.setBoolean("Locked", true);
        silo.readFromNBT(siloTag);
        silo.tryForm();
        silo.changed();
        placeSign(world, siloCtrl.west(2), EnumFacing.WEST,
                pt ? "Silo Agricola" : "Agri. Silo",
                "3x5x3 (Zero-EU)",
                pt ? "Template Travado" : "Template Locked",
                "8192x Bio-Fiber");

        // 4) Biogas Generator (3x3x3) + BatBox on front face
        buildShell(world, genCenter, 3, CropManager.BIOGAS_GENERATOR_CASING.getDefaultState());
        BlockPos genCtrl = genCenter.west().up();
        world.setBlockState(genCtrl, CropManager.BIOGAS_GENERATOR.getDefaultState()
                .withProperty(AgriculturalMachineBlock.FACING, EnumFacing.WEST), 3);
        BiogasGeneratorTile gen = (BiogasGeneratorTile) world.getTileEntity(genCtrl);
        gen.validateShell();
        gen.gas.fill(new FluidStack(FluidName.biogas.getInstance(), 16000), true);
        BlockPos batPos = genCtrl.west();
        world.setBlockState(batPos, IC2Items.getItemAPI().getBlockState("te", "batbox"), 2);
        TileEntityElectricBatBox batBox = new TileEntityElectricBatBox();
        world.setTileEntity(batPos, batBox);
        for (int t = 0; t < 20; t++) {
            gen.update();
        }
        world.setBlockState(batPos.north(2), CropManager.GREENHOUSE_FRAME.getDefaultState(), 2);
        world.setBlockState(batPos.north(2).up(), CropManager.INDUSTRIAL_LANTERN.getDefaultState()
                .withProperty(FarmLanternBlock.FACING, EnumFacing.DOWN), 2);
        placeSign(world, batPos.south(), EnumFacing.WEST,
                pt ? "Gerador Biogas" : "Biogas Generator",
                "3x3x3 -> 32 EU/t",
                pt ? "Saida Frontal" : "Front EU Output",
                "-> BatBox IC2");

        // Fluid Pipe line from Tank to Biogas Generator casing
        for (int x = tankCenter.getX() + 2; x <= genCenter.getX() - 2; x++) {
            world.setBlockState(new BlockPos(x, origin.getY() + 1, tankCenter.getZ()), CropManager.FLUID_PIPE.getDefaultState(), 3);
        }
        // Decorative fluid pipe run toward North farm
        for (int z = resCenter.getZ() - 2; z >= managerPos.getZ() + 1; z--) {
            world.setBlockState(new BlockPos(resCenter.getX(), origin.getY(), z), CropManager.FLUID_PIPE.getDefaultState(), 3);
        }

        return new BlockPos[]{resCtrl, tankCtrl, siloCtrl, genCtrl};
    }

    private static BlockPos[] buildChemistryAndBiogasSector(WorldServer world, BlockPos origin, boolean pt) {
        // Chemistry production line: Grin Grinder -> Grin Extractor -> Fluid Pipe -> Weed-Ex Fabricator -> Electric Composter
        BlockPos grinderPos = origin.add(-2, 0, -4);
        BlockPos extractorPos = origin.add(-4, 0, -4);
        BlockPos pipePos = origin.add(-5, 0, -4);
        BlockPos fabricatorPos = origin.add(-6, 0, -4);
        BlockPos composterPos = origin.add(-8, 0, -4);

        world.setBlockState(grinderPos, CropManager.GRIN_GRINDER.getDefaultState().withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        GrinGrinderTile grinder = (GrinGrinderTile) world.getTileEntity(grinderPos);
        grinder.energy.setEnergyStored(grinder.energy.getCapacity());
        grinder.inventory.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE, 32));
        grinder.inventory.setStackInSlot(1, new ItemStack(CropManager.GRINDING_BLADE));
        grinder.inventory.setStackInSlot(4, createWifiLinkedUpgrade(1, 1));
        grinder.update();

        world.setBlockState(extractorPos, CropManager.GRIN_EXTRACTOR.getDefaultState().withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        GrinExtractorTile extractor = (GrinExtractorTile) world.getTileEntity(extractorPos);
        extractor.energy.setEnergyStored(extractor.energy.getCapacity());
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, 8000), true);
        extractor.extract.fill(new FluidStack(CropManager.GRIN_EXTRACT, 4000), true);
        extractor.inventory.setStackInSlot(0, IC2Items.getItem("crop_res", "grin_powder"));
        extractor.inventory.setStackInSlot(2, createWifiLinkedUpgrade(1, 1));
        extractor.update();

        world.setBlockState(pipePos, CropManager.FLUID_PIPE.getDefaultState(), 3);

        world.setBlockState(fabricatorPos, CropManager.WEED_EX_FABRICATOR.getDefaultState().withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        WeedExFabricatorTile fab = (WeedExFabricatorTile) world.getTileEntity(fabricatorPos);
        fab.energy.setEnergyStored(fab.energy.getCapacity());
        fab.extract.fill(new FluidStack(CropManager.GRIN_EXTRACT, 4000), true);
        fab.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), 4000), true);
        fab.inventory.setStackInSlot(0, new ItemStack(Items.REDSTONE, 32));
        fab.inventory.setStackInSlot(2, createWifiLinkedUpgrade(1, 1));
        fab.update();

        world.setBlockState(composterPos, CropManager.COMPOSTER.getDefaultState().withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
        ComposterTile composter = (ComposterTile) world.getTileEntity(composterPos);
        composter.energy.setEnergyStored(composter.energy.getCapacity());
        composter.water.fill(new FluidStack(FluidRegistry.WATER, 8000), true);
        composter.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, 64));
        composter.inventory.setStackInSlot(2, createWifiLinkedUpgrade(1, 1));
        composter.update();

        // 6 Manual Composters showing all FILL stages 0..5
        for (int stage = 0; stage <= 5; stage++) {
            BlockPos mcPos = origin.add(-2 - stage, 0, -1);
            world.setBlockState(mcPos, CropManager.MANUAL_COMPOSTER.getDefaultState(), 3);
            ManualComposterTile mcTile = (ManualComposterTile) world.getTileEntity(mcPos);
            if (stage >= 1 && stage <= 4) {
                int items = stage * 2;
                for (int k = 0; k < items; k++) {
                    mcTile.addOrganic(new ItemStack(Items.WHEAT));
                }
                if (stage == 4) {
                    mcTile.hydrate();
                }
            } else if (stage == 5) {
                mcTile.readyCount = ManagerConfig.manualComposterOutputCount;
                world.setBlockState(mcPos, CropManager.MANUAL_COMPOSTER.getDefaultState()
                        .withProperty(ManualComposterBlock.FILL, 5), 3);
            }
        }

        // Biogas Weeder + Biogas Sprayer over a small 5x5 demonstration bed
        BlockPos weederPos = origin.add(-4, 0, 3);
        BlockPos sprayerPos = origin.add(-6, 0, 3);
        IBlockState farmland = Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 1; dz <= 4; dz++) {
                BlockPos cp = weederPos.add(dx, 0, dz);
                world.setBlockState(cp.down(), farmland, 2);
                if (dx == 0 && dz == 2) {
                    plantCrop(world, cp, Crops.weed, 2, 0, 1, 1, 0, false);
                } else if ((dx + dz) % 2 == 0) {
                    plantCrop(world, cp, CropRegistry.SULFURBUD, 3, 4, 15, 20, 0, false);
                }
            }
        }
        world.setBlockState(weederPos, CropManager.WEEDER.getDefaultState().withProperty(ManagerBlock.FACING, EnumFacing.NORTH), 3);
        WeederTile weeder = (WeederTile) world.getTileEntity(weederPos);
        weeder.gas.fill(new FluidStack(FluidName.biogas.getInstance(), 16000), true);

        world.setBlockState(sprayerPos, CropManager.BIOGAS_SPRAYER.getDefaultState().withProperty(AgriculturalMachineBlock.FACING, EnumFacing.NORTH), 3);
        BiogasSprayerTile sprayer = (BiogasSprayerTile) world.getTileEntity(sprayerPos);
        sprayer.gas.fill(new FluidStack(FluidName.biogas.getInstance(), 4000), true);
        sprayer.water.fill(new FluidStack(FluidRegistry.WATER, 4000), true);
        sprayer.fertilizer.setStackInSlot(0, new ItemStack(CropManager.ORGANIC_COMPOST, 64));

        placeSign(world, grinderPos.south(2), EnumFacing.SOUTH,
                pt ? "4. Quimica & Biogas" : "4. Chem & Biogas",
                "Grin -> Weed-Ex",
                pt ? "Composteira 0..5" : "Composter 0..5",
                "Weeder & Sprayer");

        return new BlockPos[]{grinderPos, weederPos};
    }

    private static final class GreenhouseResult {
        final BlockPos pollinatorPos;
        final BlockPos radbloomPos;
        final BlockPos[] cropPositions;

        GreenhouseResult(BlockPos pollinatorPos, BlockPos radbloomPos, BlockPos[] cropPositions) {
            this.pollinatorPos = pollinatorPos;
            this.radbloomPos = radbloomPos;
            this.cropPositions = cropPositions;
        }
    }

    private static GreenhouseResult buildBotanicalGreenhouseSector(WorldServer world, BlockPos origin, boolean pt) {
        IBlockState frame = CropManager.GREENHOUSE_FRAME.getDefaultState();
        IBlockState glass = CropManager.GREENHOUSE_GLASS.getDefaultState();
        IBlockState trellis = CropManager.GARDEN_TRELLIS.getDefaultState();
        IBlockState farmland = Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7);

        // Greenhouse shell: x in [-10..0], z in [-5..9] relative to origin
        for (int dx = -10; dx <= 0; dx++) {
            for (int dz = -5; dz <= 9; dz++) {
                world.setBlockState(origin.add(dx, -1, dz), Blocks.STONEBRICK.getDefaultState(), 2);
                boolean xEdge = dx == -10 || dx == 0;
                boolean zEdge = dz == -5 || dz == 9;
                if (xEdge || zEdge) {
                    boolean door = (dx == 0 && Math.abs(dz - 1) <= 1) || (dz == -5 && Math.abs(dx + 5) <= 1);
                    if (!door) {
                        world.setBlockState(origin.add(dx, 0, dz), frame, 2);
                        world.setBlockState(origin.add(dx, 1, dz), glass, 2);
                        world.setBlockState(origin.add(dx, 2, dz), glass, 2);
                        world.setBlockState(origin.add(dx, 3, dz), frame, 2);
                    }
                }
                world.setBlockState(origin.add(dx, 4, dz), glass, 2);
            }
        }

        // Ceiling & Wall lanterns inside greenhouse
        world.setBlockState(origin.add(-5, 3, -1), CropManager.GLASS_LAMP.getDefaultState().withProperty(FarmLanternBlock.FACING, EnumFacing.UP), 2);
        world.setBlockState(origin.add(-5, 3, 5), CropManager.INDUSTRIAL_LANTERN.getDefaultState().withProperty(FarmLanternBlock.FACING, EnumFacing.UP), 2);
        world.setBlockState(origin.add(-9, 1, 2), CropManager.WOODEN_SCONCE.getDefaultState().withProperty(FarmLanternBlock.FACING, EnumFacing.WEST), 2);
        world.setBlockState(origin.add(-2, 0, -4), trellis, 2);
        world.setBlockState(origin.add(-8, 0, -4), trellis, 2);

        // Plant all 15 custom species (14 in main beds + radbloom in isolated glass containment at z=7)
        List<BlockPos> planted = new ArrayList<>();
        int nonRadIdx = 0;
        BlockPos radPos = origin.add(-5, 0, 7);

        for (BaseCropCard card : CropRegistry.ALL) {
            if (card == CropRegistry.RADBLOOM) {
                // Sealed radiation dome >= 5 blocks from main entrance & walkways
                world.setBlockState(radPos.down(2), CropManager.RADIOACTIVE_SOIL.getDefaultState(), 2);
                world.setBlockState(radPos.down(), farmland, 2);
                plantCrop(world, radPos, card, card.getMaxSize(), 4, 20, 26, 2, true);
                for (int rdx = -1; rdx <= 1; rdx++) {
                    for (int rdz = -1; rdz <= 1; rdz++) {
                        for (int rdy = 0; rdy <= 2; rdy++) {
                            if (rdx == 0 && rdz == 0 && rdy <= 1) {
                                continue;
                            }
                            world.setBlockState(radPos.add(rdx, rdy, rdz), glass, 2);
                        }
                    }
                }
                planted.add(radPos);
                continue;
            }
            int col = nonRadIdx % 5; // 0..4 -> x = -8 .. -4
            int row = nonRadIdx / 5; // 0..2 -> z = -2, 0, 2
            BlockPos cp = origin.add(-8 + col, 0, -2 + row * 2);
            if (card == CropRegistry.CUPRIC_MOSS) {
                world.setBlockState(cp.down(2), IC2Items.getItemAPI().getBlockState("resource", "copper_block"), 2);
            } else if (card == CropRegistry.STANNUM_HERB) {
                world.setBlockState(cp.down(2), IC2Items.getItemAPI().getBlockState("resource", "tin_block"), 2);
            } else if (card == CropRegistry.VOLTREED) {
                world.setBlockState(cp.down(2), Blocks.REDSTONE_BLOCK.getDefaultState(), 2);
            }
            world.setBlockState(cp.down(), farmland, 2);
            plantCrop(world, cp, card, card.getMaxSize(), 4, 18, 24, 2, card.requiresCatalyst());
            planted.add(cp);
            nonRadIdx++;
        }

        // Crossbreeding corner with Pollinator Hive (CHARGED = true) + Double Crop Sticks
        BlockPos hivePos = origin.add(-2, 0, 2);
        world.setBlockState(hivePos, CropManager.POLLINATOR_HIVE.getDefaultState(), 3);
        PollinatorHiveTile hive = (PollinatorHiveTile) world.getTileEntity(hivePos);
        while (hive.addPowderCharge()) {
            // Fill to max charges and set CHARGED=true
        }
        BlockPos crossPos = hivePos.north();
        world.setBlockState(crossPos.down(), farmland, 2);
        plantDoubleCrossing(world, crossPos);

        placeSign(world, origin.add(1, 0, 1), EnumFacing.EAST,
                pt ? "5. Estufa Botanica" : "5. Botanical House",
                pt ? "15 Especies IC2" : "15 Custom Crops",
                pt ? "Colmeia Carregada" : "Charged Pollinator",
                pt ? "Radbloom Contido" : "Sealed Radbloom");

        return new GreenhouseResult(hivePos, radPos, planted.toArray(new BlockPos[0]));
    }

    private static BlockPos[] buildHydroponicLabSector(WorldServer world, BlockPos origin, boolean pt) {
        BlockPos[] chambers = {
                origin.add(2, 0, 0),
                origin.add(6, 0, 0),
                origin.add(10, 0, 0)
        };
        BaseCropCard[] cards = {CropRegistry.IRID_MOSS, CropRegistry.RADBLOOM, CropRegistry.BIOREED};

        // Dedicated Biogas Heat Burners (32 HU/t each) pointing SOUTH into Thermal Pipes (20 HU/t per chamber)
        for (int i = 0; i < chambers.length; i++) {
            BlockPos burnerPos = chambers[i].north(2);
            world.setBlockState(burnerPos, CropManager.BIOGAS_HEAT_BURNER.getDefaultState()
                    .withProperty(AgriculturalMachineBlock.FACING, EnumFacing.SOUTH), 3);
            BiogasHeatBurnerTile burner = (BiogasHeatBurnerTile) world.getTileEntity(burnerPos);
            burner.gas.fill(new FluidStack(FluidName.biogas.getInstance(), 4000), true);
        }

        for (int x = 2; x <= 10; x++) {
            world.setBlockState(origin.add(x, 0, -1), CropManager.THERMAL_PIPE.getDefaultState(), 3);
        }

        for (int i = 0; i < chambers.length; i++) {
            BlockPos cPos = chambers[i];
            IBlockState subsoil = cards[i] == CropRegistry.RADBLOOM
                    ? CropManager.RADIOACTIVE_SOIL.getDefaultState()
                    : Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7);
            world.setBlockState(cPos.down(), subsoil, 2);

            // 3x3 greenhouse_glass dome above each chamber (+25% speed)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setBlockState(cPos.add(dx, 1, dz), CropManager.GREENHOUSE_GLASS.getDefaultState(), 2);
                }
            }

            world.setBlockState(cPos, CropManager.HYDROPONIC_CHAMBER.getDefaultState()
                    .withProperty(ManagerBlock.FACING, EnumFacing.SOUTH), 3);
            HydroponicChamberTile chamber = (HydroponicChamberTile) world.getTileEntity(cPos);
            chamber.water.fill(new FluidStack(FluidRegistry.WATER, chamber.water.getCapacity()), true);
            chamber.energy.setEnergyStored(chamber.energy.getCapacity());
            chamber.inventory.setStackInSlot(HydroponicChamberTile.SLOT_SEED,
                    ItemCropSeed.generateItemStackFromValues(cards[i], 21, 31, 0, 4));
            chamber.inventory.setStackInSlot(HydroponicChamberTile.SLOT_CATALYST,
                    new ItemStack(CropManager.CATALYTIC_FERTILIZER, 64));
            chamber.inventory.setStackInSlot(HydroponicChamberTile.SLOT_UPG_START,
                    createWifiLinkedUpgrade(1, 1));
            chamber.refreshStructure(cards[i]);
            chamber.update();
        }

        placeSign(world, origin.add(6, 0, 3), EnumFacing.SOUTH,
                pt ? "6. Hidroponia +75%" : "6. Hydroponics +75%",
                "Domo 3x3 (+25%)",
                pt ? "Burner + ThermalPipe" : "Burner + ThermalPipe",
                "Irid / Rad / Bioreed");

        return chambers;
    }

    private static final class IrrigationResult {
        final BlockPos[] sprinklers;
        final BlockPos diagonalPos;

        IrrigationResult(BlockPos[] sprinklers, BlockPos diagonalPos) {
            this.sprinklers = sprinklers;
            this.diagonalPos = diagonalPos;
        }
    }

    private static IrrigationResult buildIrrigationSector(WorldServer world, BlockPos origin, boolean pt) {
        BlockPos mk1Pos = origin.add(0, 0, 0);
        BlockPos mk2Pos = origin.add(5, 0, 0);
        BlockPos mk3Pos = origin.add(10, 0, 0);
        BlockPos diagPos = origin.add(5, 0, -7);

        SprinklerBlock[] blocks = {CropManager.SPRINKLER_MK1, CropManager.SPRINKLER_MK2, CropManager.SPRINKLER_MK3};
        BlockPos[] positions = {mk1Pos, mk2Pos, mk3Pos};
        IBlockState farmland = Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7);

        for (int i = 0; i < positions.length; i++) {
            BlockPos sp = positions[i];
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        world.setBlockState(sp.down(), Blocks.WATER.getDefaultState(), 2);
                    } else {
                        world.setBlockState(sp.add(dx, -1, dz), farmland, 2);
                        plantCrop(world, sp.add(dx, 0, dz), CropRegistry.CRYOLOTUS, 3, 4, 15, 20, 0, false);
                    }
                }
            }
            world.setBlockState(sp, blocks[i].getDefaultState(), 3);
            SprinklerTile tile = (SprinklerTile) world.getTileEntity(sp);
            tile.redstoneMode = 0;
            tile.water.fill(new FluidStack(FluidRegistry.WATER, tile.water.getCapacity()), true);
            tile.refreshActiveState();
        }

        // Diagonal Sprinkler with dry cross walkways and 4 irrigated diagonal quadrants
        world.setBlockState(diagPos.down(), Blocks.WATER.getDefaultState(), 2);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 || dz == 0) {
                    if (!(dx == 0 && dz == 0)) {
                        world.setBlockState(diagPos.add(dx, -1, dz), Blocks.STONEBRICK.getDefaultState(), 2);
                    }
                } else {
                    world.setBlockState(diagPos.add(dx, -1, dz), farmland, 2);
                    plantCrop(world, diagPos.add(dx, 0, dz), CropRegistry.LAZULI_BLOOM, 3, 4, 18, 22, 0, true);
                }
            }
        }
        world.setBlockState(diagPos, CropManager.SPRINKLER_DIAGONAL.getDefaultState()
                .withProperty(AgriculturalMachineBlock.FACING, EnumFacing.SOUTH), 3);
        DiagonalSprinklerTile diag = (DiagonalSprinklerTile) world.getTileEntity(diagPos);
        NBTTagCompound q = new NBTTagCompound();
        q.setInteger("Quadrants", 15);
        q.setInteger("Redstone", 0);
        q.setInteger("Water", 4000);
        diag.acceptGuiState(q);
        diag.markDirty();

        placeSign(world, mk2Pos.south(3), EnumFacing.SOUTH,
                pt ? "7. Campo Irrigacao" : "7. Irrigation",
                "Sprinklers Mk1-Mk3",
                pt ? "Agua Infinita Abaixo" : "Infinite Water Below",
                pt ? "Regador Diagonal" : "Diagonal Sprinkler");

        return new IrrigationResult(positions, diagPos);
    }

    public static void verifyShowcaseIntegrity(WorldServer world, ShowcaseSummary summary) {
        TileEntity resTe = world.getTileEntity(summary.reservoirController);
        if (!(resTe instanceof AgriculturalReservoirTile) || !((AgriculturalReservoirTile) resTe).formed) {
            throw new AssertionError("Showcase Agricultural Reservoir failed to form");
        }
        TileEntity tankTe = world.getTileEntity(summary.tankController);
        if (!(tankTe instanceof IndustrialTankTile) || !((IndustrialTankTile) tankTe).formed) {
            throw new AssertionError("Showcase Industrial Tank failed to form");
        }
        TileEntity siloTe = world.getTileEntity(summary.siloController);
        if (!(siloTe instanceof AgriculturalSiloTile)) {
            throw new AssertionError("Showcase Agricultural Silo missing at " + summary.siloController);
        }
        AgriculturalSiloTile silo = (AgriculturalSiloTile) siloTe;
        if (!silo.formed || !silo.isLocked() || silo.getStoredCount() <= 0) {
            throw new AssertionError("Showcase Agricultural Silo failed to form or lock");
        }
        TileEntity genTe = world.getTileEntity(summary.biogasGenController);
        if (!(genTe instanceof BiogasGeneratorTile) || !((BiogasGeneratorTile) genTe).isFormed()) {
            throw new AssertionError("Showcase Biogas Generator (3x3x3) failed to form");
        }
        TileEntity monTe = world.getTileEntity(summary.monitorPos);
        if (!(monTe instanceof MonitorTile) || !"connected".equals(((MonitorTile) monTe).connection)) {
            throw new AssertionError("Showcase Monitor is not connected: "
                    + (monTe instanceof MonitorTile ? ((MonitorTile) monTe).connection : "null"));
        }
        TileEntity mgrTe = world.getTileEntity(summary.managerPos);
        if (!(mgrTe instanceof AdvancedManagerTile) || !((AdvancedManagerTile) mgrTe).report.hasKey("Survey")) {
            throw new AssertionError("Showcase Advanced Crop Manager did not complete initial survey");
        }
        TileEntity chestTe = world.getTileEntity(summary.databaseChestPos);
        if (!(chestTe instanceof TileCropDatabaseChest)) {
            throw new AssertionError("Showcase Database Chest missing at " + summary.databaseChestPos);
        }
        TileCropDatabaseChest chest = (TileCropDatabaseChest) chestTe;
        int occupied = 0;
        for (int s = 0; s < chest.seeds().getSlots(); s++) {
            if (!chest.seeds().getStackInSlot(s).isEmpty()) {
                occupied++;
            }
        }
        if (occupied < CropRegistry.ALL.size()) {
            throw new AssertionError("Showcase Database Chest missing custom crop seed bags: " + occupied);
        }
        for (BlockPos cPos : summary.hydroponicChambers) {
            TileEntity chTe = world.getTileEntity(cPos);
            if (!(chTe instanceof HydroponicChamberTile)) {
                throw new AssertionError("Hydroponic chamber missing at " + cPos);
            }
            HydroponicChamberTile chamber = (HydroponicChamberTile) chTe;
            if (!chamber.hasGlass || !chamber.hasSubsoil || !chamber.domeBonus || !chamber.heatBonus) {
                throw new AssertionError("Hydroponic chamber missing dome/subsoil/heat bonus at " + cPos);
            }
        }
        for (BlockPos cp : summary.greenhouseCrops) {
            TileEntity te = world.getTileEntity(cp);
            if (!(te instanceof TileEntityCrop) || ((TileEntityCrop) te).getCrop() == null
                    || ((TileEntityCrop) te).getCrop() == Crops.weed) {
                throw new AssertionError("Greenhouse crop missing or turned into weed at " + cp);
            }
        }
        double distToMainWalkway = Math.sqrt(summary.radbloomPos.distanceSq(summary.origin.add(-10, 0, 18)));
        if (distToMainWalkway < 5.0) {
            throw new AssertionError("Radbloom containment is closer than 5 blocks to walkway: " + distToMainWalkway);
        }
    }

    private static void buildShell(WorldServer world, BlockPos center, int height, IBlockState casing) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean isCenterCore = dy > 0 && dy < height - 1 && dx == 0 && dz == 0;
                    world.setBlockState(center.add(dx, dy, dz),
                            isCenterCore ? Blocks.AIR.getDefaultState() : casing, 2);
                }
            }
        }
    }

    private static void plantCrop(WorldServer world, BlockPos pos, CropCard card, int size, int scan,
                                  int growth, int gain, int resist, boolean catalyze) {
        world.setBlockState(pos, IC2Items.getItemAPI().getBlockState("te", "crop"), 2);
        TileEntityCrop crop = new TileEntityCrop();
        world.setTileEntity(pos, crop);
        crop.setCrop(card);
        crop.setCurrentSize(size);
        crop.setScanLevel(scan);
        crop.setStatGrowth(growth);
        crop.setStatGain(gain);
        crop.setStatResistance(resist);
        crop.setStorageNutrients(100);
        crop.setStorageWater(100);
        if (catalyze) {
            FertilizerItem.applyToCrop(new ItemStack(CropManager.CATALYTIC_FERTILIZER), crop, false);
        }
        crop.updateState();
    }

    private static void plantDoubleCrossing(WorldServer world, BlockPos pos) {
        world.setBlockState(pos, IC2Items.getItemAPI().getBlockState("te", "crop"), 2);
        TileEntityCrop crop = new TileEntityCrop();
        world.setTileEntity(pos, crop);
        crop.reset();
        crop.setCrossingBase(true);
        crop.updateState();
    }

    private static ItemStack createWifiLinkedUpgrade(int channel, int count) {
        ItemStack stack = new ItemStack(CropManager.WIRELESS_EU, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(WirelessEuUpgrade.WIFI_CHANNEL_TAG, channel);
        stack.setTagCompound(tag);
        return stack;
    }

    private static ItemStack createLinkedPortable(WorldServer world, BlockPos chestPos) {
        ItemStack portable = new ItemStack(CropManager.PORTABLE_DATABASE);
        ItemUpgradeLinker.setLink(portable, world.provider.getDimension(), chestPos);
        return portable;
    }

    private static ItemStack createLinkedProbe(WorldServer world, BlockPos managerPos) {
        ItemStack probe = new ItemStack(CropManager.PROBE);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Dim", world.provider.getDimension());
        tag.setInteger("X", managerPos.getX());
        tag.setInteger("Y", managerPos.getY());
        tag.setInteger("Z", managerPos.getZ());
        probe.setTagCompound(tag);
        return probe;
    }

    private static ItemStack createLoadedCrossingTool() {
        ItemStack tool = new ItemStack(CropManager.CROSSING_TOOL);
        CrossingTool.supplies(tool).insertItem(0, new ItemStack(ItemName.crop_stick.getInstance(), 64), false);
        return tool;
    }

    private static void placeSign(WorldServer world, BlockPos pos, EnumFacing facing,
                                  String l1, String l2, String l3, String l4) {
        int rot = 0;
        if (facing == EnumFacing.WEST) {
            rot = 4;
        } else if (facing == EnumFacing.NORTH) {
            rot = 8;
        } else if (facing == EnumFacing.EAST) {
            rot = 12;
        }
        world.setBlockState(pos, Blocks.STANDING_SIGN.getDefaultState()
                .withProperty(BlockStandingSign.ROTATION, rot), 3);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySign) {
            TileEntitySign sign = (TileEntitySign) te;
            sign.signText[0] = new TextComponentString(l1);
            sign.signText[1] = new TextComponentString(l2);
            sign.signText[2] = new TextComponentString(l3);
            sign.signText[3] = new TextComponentString(l4);
            sign.markDirty();
        }
    }

    public static String plankName() {
        return BlockPlanks.EnumType.OAK.getName();
    }
}
