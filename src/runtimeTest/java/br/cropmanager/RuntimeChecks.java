package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.item.IC2Items;
import ic2.api.energy.tile.IHeatSource;
import ic2.api.upgrade.IProcessingUpgrade;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.FluidName;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import ic2.core.block.wiring.TileEntityElectricBatBox;

/** Optional separate test addon. Runs only in its explicitly named disposable world. */
@Mod(modid = "cropmanager_tests", name = "Crop Manager Runtime Tests", version = "1",
        dependencies = "required-after:cropmanager")
public final class RuntimeChecks {
    private int checks;
    private WorldServer tickWorld;
    private BlockPos tickPos;
    private int tickStage, elapsed;
    private double previousEnergy;
    private MonitorTile timedMonitor;
    private void check(boolean result, String description) {
        if (!result) throw new AssertionError(description);
        checks++;
        CropManager.log.info("RUNTIME PASS: {}", description);
    }
    private void pulse(ManagerTile tile, int count) { for (int i = 0; i < count; i++) tile.update(); }
    private int countItem(ManagerTile tile, net.minecraft.item.Item item) {
        int total = 0;
        for (int i = 3; i < 18; i++) {
            ItemStack stack = tile.inventory.getStackInSlot(i);
            if (stack.getItem() == item) total += stack.getCount();
        }
        NBTTagList pending = tile.writeToNBT(new NBTTagCompound()).getTagList("Pending", 10);
        for (int i = 0; i < pending.tagCount(); i++) {
            NBTTagCompound tag = pending.getCompoundTagAt(i);
            if (new ItemStack(tag).getItem() == item) total += tag.getInteger("PendingCount");
        }
        return total;
    }

    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("cropmanager.testWorld")) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (!"cropmanager-test-world".equals(server.getFolderName()))
            throw new IllegalStateException("Runtime tests require the disposable cropmanager-test-world");
        try {
            run(server.getWorld(0));
            revision04(server.getWorld(0));
            revision05(server.getWorld(0));
            revision052Survey(server.getWorld(0));
            weedExProductionLine(server.getWorld(0));
            chestAndLinkerIntegration(server.getWorld(0));
            beginTickedEnergyTest(server.getWorld(0));
        } catch (Throwable error) {
            CropManager.log.error("CROP_MANAGER_RUNTIME_TESTS_FAILED", error);
            server.initiateShutdown();
            throw new RuntimeException("Crop Manager runtime test failure", error);
        }
    }

    private void weedExProductionLine(WorldServer world) {
        BlockPos base = new BlockPos(100008, 210, 100008);
        for (int i = 0; i <= 4; i++) world.setBlockToAir(base.east(i));
        BlockPos sourcePos = base.east(3);
        world.setBlockState(base, CropManager.GRIN_GRINDER.getDefaultState(), 18);
        world.setBlockState(base.east(), CropManager.GRIN_EXTRACTOR.getDefaultState(), 18);
        world.setBlockState(base.east(2), CropManager.THERMAL_PIPE.getDefaultState(), 18);
        world.setBlockState(sourcePos, CropManager.ADVANCED.getDefaultState(), 18);
        world.setBlockState(base.east(4), CropManager.WEED_EX_FABRICATOR.getDefaultState(), 18);
        GrinGrinderTile grinder = (GrinGrinderTile)world.getTileEntity(base);
        GrinExtractorTile extractor = (GrinExtractorTile)world.getTileEntity(base.east());
        WeedExFabricatorTile mixer = (WeedExFabricatorTile)world.getTileEntity(base.east(4));
        TestHeatSource heat = new TestHeatSource(4000);
        world.setTileEntity(sourcePos, heat);
        grinder.energy.setEnergyStored(1000);
        grinder.inventory.setStackInSlot(0, new ItemStack(Items.SPIDER_EYE));
        grinder.inventory.setStackInSlot(1, new ItemStack(CropManager.GRINDING_BLADE));
        for (int i = 0; i < GrinGrinderTile.DURATION; i++) grinder.update();
        ItemStack powder = grinder.inventory.extractItem(2, 1, false);
        check(powder.getCount() == 1 && grinder.inventory.getStackInSlot(1).getItemDamage() == 1,
                "Grin Grinder converts one spider eye and wears its reusable blade once");
        extractor.energy.setEnergyStored(1000);
        extractor.inventory.setStackInSlot(0, powder);
        extractor.water.fill(new FluidStack(FluidRegistry.WATER, GrinExtractorTile.WATER_PER_RECIPE), true);
        for (int i = 0; i < GrinExtractorTile.DURATION; i++) extractor.update();
        check(extractor.extract.getFluidAmount() == GrinExtractorTile.EXTRACT_PER_RECIPE
                        && extractor.water.getFluidAmount() == 0 && heat.heat == 2000,
                "Grin Extractor consumes water and IC2 heat through a connected thermal pipe");
        FluidStack extracted = extractor.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN)
                .drain(GrinExtractorTile.EXTRACT_PER_RECIPE, true);
        int inserted = mixer.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.UP).fill(extracted, true);
        mixer.inventory.setStackInSlot(WeedExFabricatorTile.REDSTONE, new ItemStack(Items.REDSTONE)); mixer.energy.setEnergyStored(1000);
        for (int i = 0; i < WeedExFabricatorTile.DURATION; i++) mixer.update();
        check(inserted == GrinExtractorTile.EXTRACT_PER_RECIPE && mixer.weed.getFluidAmount() == WeedExFabricatorTile.WEED_PER_RECIPE
                        && mixer.extract.getFluidAmount() == 0 && mixer.inventory.getStackInSlot(WeedExFabricatorTile.REDSTONE).isEmpty(),
                "Weed-Ex Mixer consumes transferred Grin Extract and redstone into Weed-Ex");
        ItemStack oc = IC2Items.getItem("upgrade", "overclocker");
        grinder.inventory.setStackInSlot(3, oc.copy()); grinder.update();
        extractor.inventory.setStackInSlot(4, oc.copy()); extractor.update();
        mixer.inventory.setStackInSlot(4, oc.copy()); mixer.update();
        check(grinder.workTicks < GrinGrinderTile.DURATION && extractor.workTicks < GrinExtractorTile.DURATION && mixer.workTicks < WeedExFabricatorTile.DURATION,
                "Overclocker upgrades accelerate Grin Grinder, Grin Extractor, and Weed-Ex Mixer");
        GrinExtractorTile restored = new GrinExtractorTile();
        extractor.extract.fill(new FluidStack(CropManager.GRIN_EXTRACT, 250), true); extractor.progress = 37;
        restored.readFromNBT(extractor.writeToNBT(new NBTTagCompound()));
        check(restored.extract.getFluidAmount() == 250 && restored.progress == 37,
                "Grin Extractor fluid and work state survive NBT reload");
        world.setBlockToAir(base); world.setBlockToAir(base.east()); world.setBlockToAir(base.east(2));
        world.setBlockToAir(sourcePos); world.setBlockToAir(base.east(4));
    }

    private void chestAndLinkerIntegration(WorldServer world) {
        net.minecraftforge.common.util.FakePlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        BlockPos chestPos = new BlockPos(100024, 180, 100024);
        BlockPos grinderPos = chestPos.east(2);
        BlockPos pipePos = chestPos.east(4);
        world.setBlockToAir(chestPos);
        world.setBlockToAir(grinderPos);
        world.setBlockToAir(grinderPos.up());
        world.setBlockToAir(pipePos);

        // Verify right-clicking two-high / pipe blocks without a wrench never crashes (regression test for isWrench NPE).
        world.setBlockState(grinderPos, CropManager.GRIN_GRINDER.getDefaultState(), 18);
        world.setBlockState(pipePos, CropManager.THERMAL_PIPE.getDefaultState(), 18);
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, ItemStack.EMPTY);
        check(CropManager.GRIN_GRINDER.onBlockActivated(world, grinderPos, world.getBlockState(grinderPos), player, net.minecraft.util.EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5f, 0.5f, 0.5f),
                "Grin Grinder right-click without wrench succeeds without NPE");
        check(CropManager.THERMAL_PIPE.onBlockActivated(world, pipePos, world.getBlockState(pipePos), player, net.minecraft.util.EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5f, 0.5f, 0.5f),
                "Thermal Pipe right-click without wrench succeeds without NPE");

        // Place 500-slot Crop Database Chest and insert an analyzed IC2 Seed Bag.
        world.setBlockState(chestPos, CropManager.DATABASE_CHEST.getDefaultState(), 18);
        br.cropmanager.chest.inventory.TileCropDatabaseChest chest =
                (br.cropmanager.chest.inventory.TileCropDatabaseChest) world.getTileEntity(chestPos);
        check(chest != null && chest.getSizeInventory() == 500, "Crop Database Chest tile exposes 500 slots");

        CropCard wheat = Crops.instance.getCropCard("ic2", "wheat");
        ItemStack seedBag = ic2.core.item.ItemCropSeed.generateItemStackFromValues(wheat, 20, 21, 5, 4);
        br.cropmanager.chest.integration.CropInformation info = br.cropmanager.chest.integration.CropInformation.read(seedBag);
        check(info.scanLevel == 4 && info.tier == wheat.getProperties().getTier() && info.maxSize == wheat.getMaxSize() && !info.discoveredBy.isEmpty(),
                "CropInformation exposes full Cropnalyzer metadata");

        ItemStack rem = chest.seeds().insertAnywhere(seedBag.copy(), false);
        check(rem.isEmpty() && chest.getStackInSlot(0).getCount() == 1, "Crop Database Chest stores analyzed IC2 seed bag");

        // Bind Upgrade Linker to the chest and apply it to Portable Crop Database.
        ItemStack linker = new ItemStack(CropManager.UPGRADE_LINKER);
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, linker);
        player.setSneaking(true);
        check(CropManager.UPGRADE_LINKER.onItemUse(player, world, chestPos, net.minecraft.util.EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f) == net.minecraft.util.EnumActionResult.SUCCESS,
                "Upgrade Linker binds to Crop Database Chest");
        player.setSneaking(false);

        ItemStack portable = new ItemStack(CropManager.PORTABLE_DATABASE);
        player.setHeldItem(net.minecraft.util.EnumHand.OFF_HAND, portable);
        CropManager.UPGRADE_LINKER.onItemRightClick(world, player, net.minecraft.util.EnumHand.MAIN_HAND);
        check(br.cropmanager.chest.item.ItemUpgradeLinker.hasLink(portable),
                "Upgrade Linker transfers Chest coordinates to Portable Crop Database");

        br.cropmanager.chest.inventory.TileCropDatabaseChest resolved =
                br.cropmanager.chest.item.ItemPortableCropDatabase.resolveLinkedChest(player, portable);
        check(resolved == chest, "Linked Portable Crop Database resolves remote Chest tile entity");

        player.inventory.setInventorySlotContents(0, portable);
        Object remoteContainer = CropManager.proxy.getServerGuiElement(13, player, world, 0, 0, 0);
        check(remoteContainer instanceof br.cropmanager.chest.inventory.ContainerCropDatabaseChest
                && ((br.cropmanager.chest.inventory.ContainerCropDatabaseChest) remoteContainer).canInteractWith(player),
                "Remote linked chest container opens and stays usable via server GUI handler 13");

        // Verify PortableSeedPickup routes to the linked chest when pickup is enabled.
        br.cropmanager.chest.inventory.PortableCropInventory.open(player, 0).setPickupEnabled(true);
        ItemStack secondBag = ic2.core.item.ItemCropSeed.generateItemStackFromValues(wheat, 18, 19, 4, 4);
        ItemStack leftover = br.cropmanager.chest.item.PortableSeedPickup.store(player, secondBag);
        check(leftover.isEmpty() && chest.getStackInSlot(1).getCount() == 1,
                "Portable automatic pickup stores seeds directly into the linked Crop Database Chest");

        player.inventory.clear();
        player.setHeldItem(net.minecraft.util.EnumHand.OFF_HAND, ItemStack.EMPTY);
        world.setBlockToAir(chestPos);
        world.setBlockToAir(grinderPos);
        world.setBlockToAir(pipePos);
    }

    private static final class TestHeatSource extends net.minecraft.tileentity.TileEntity implements IHeatSource {
        int heat;
        TestHeatSource(int heat) { this.heat = heat; }
        @Override public int maxrequestHeatTick(EnumFacing side) { return 100; }
        @Override public int requestHeat(EnumFacing side, int request) { return Math.min(heat, request); }
        @Override public int drawHeat(EnumFacing side, int request, boolean simulate) { int amount = Math.min(heat, request); if (!simulate) heat -= amount; return amount; }
    }

    private void beginTickedEnergyTest(WorldServer world) {
        tickWorld = world;
        BlockPos sourcePos = new BlockPos(100008, 106, 100008);
        tickPos = sourcePos.east();
        world.setBlockToAir(sourcePos); world.setBlockToAir(tickPos);
        world.setBlockState(sourcePos, IC2Items.getItemAPI().getBlockState("te", "batbox"), 2);
        TileEntityElectricBatBox source = new TileEntityElectricBatBox();
        world.setTileEntity(sourcePos, source);
        source.setFacing(EnumFacing.EAST);
        source.energy.addEnergy(20000);
        world.setBlockState(tickPos, CropManager.ADVANCED.getDefaultState(), 2);
        ((ManagerTile) world.getTileEntity(tickPos)).settings(1, 2, 0);
        BlockPos pulsePos = sourcePos.up(4); world.setBlockToAir(pulsePos); world.setBlockState(pulsePos, CropManager.MONITOR.getDefaultState(), 2);
        timedMonitor = (MonitorTile)world.getTileEntity(pulsePos); timedMonitor.outputMode = 0; timedMonitor.control(147, null);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || tickWorld == null) return;
        if (++elapsed < (tickStage == 2 ? 50 : 30)) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            if (tickStage == 3) {
                check(tickWorld.getChunkProvider().getLoadedChunk(tickPos.getX() >> 4, tickPos.getZ() >> 4) == null,
                        "Real server ticks unload the test chunk");
                tickWorld.getChunk(tickPos);
                tickStage++; elapsed = 0;
                return;
            }
            ManagerTile tile = (ManagerTile) tickWorld.getTileEntity(tickPos);
            check(tile != null && tile.getEnergy() > previousEnergy,
                    "EU arrives from real BatBox during server ticks, stage " + tickStage);
            if (tickStage == 0) {
                check(timedMonitor.signal == 15, "Monitor test pulse remains active after 30 real server ticks");
                tile.inventory.setStackInSlot(18, IC2Items.getItem("upgrade", "transformer"));
                tile.inventory.setStackInSlot(19, IC2Items.getItem("upgrade", "energy_storage"));
            } else if (tickStage == 1) {
                check(tile.energy.getSinkTier() == 3 && tile.energy.getCapacity() == 110000,
                        "EU reception continues after live tier and storage upgrade");
                NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
                tickWorld.removeTileEntity(tickPos);
                ManagerTile reloaded = new AdvancedManagerTile(); reloaded.readFromNBT(saved);
                tickWorld.setTileEntity(tickPos, reloaded);
            } else if (tickStage == 2) {
                check(timedMonitor.signal == 0, "Monitor test pulse expires after 100 real server ticks");
                previousEnergy = tile.getEnergy();
                tickWorld.getChunkProvider().queueUnload(tickWorld.getChunk(tickPos));
                tickStage++; elapsed = 0;
                return;
            } else {
                check(tile.energy.getSinkTier() == 3 && tile.energy.getCapacity() == 110000,
                        "Saved upgrades survive real chunk unload and reload");
                CropManager.log.info("CROP_MANAGER_RUNTIME_TESTS_PASSED checks={}", checks);
                tickWorld = null;
                MinecraftForge.EVENT_BUS.unregister(this);
                server.initiateShutdown();
                return;
            }
            previousEnergy = tile.getEnergy();
            tickStage++; elapsed = 0;
        } catch (Throwable error) {
            CropManager.log.error("CROP_MANAGER_RUNTIME_TESTS_FAILED", error);
            tickWorld = null;
            MinecraftForge.EVENT_BUS.unregister(this);
            server.initiateShutdown();
        }
    }

    private void run(WorldServer world) {
        BlockPos base = new BlockPos(100008, 100, 100008);
        world.getChunk(base);
        world.setBlockToAir(new BlockPos(100015, 110, 100008));
        world.setBlockToAir(new BlockPos(100014, 110, 100008));
        world.setBlockToAir(new BlockPos(100008, 110, 100008));
        net.minecraft.world.chunk.Chunk leftover = world.getChunkProvider().getLoadedChunk(6251, 6250);
        if (leftover != null) { world.getChunkProvider().queueUnload(leftover); world.getChunkProvider().tick(); }
        world.setBlockToAir(base);
        placeBasicFixture(world,base,2);
        ManagerTile tile = (ManagerTile) world.getTileEntity(base);
        tile.settings(1, 2, 15);
        BlockPos plant = base.add(1, 0, 0);
        world.setBlockState(plant.down(), Blocks.FARMLAND.getDefaultState(), 2);
        net.minecraft.block.state.IBlockState cropState = IC2Items.getItemAPI().getBlockState("te", "crop");
        check(cropState != null && cropState.getBlock() != Blocks.AIR, "IC2 crop block resolves");
        world.setBlockState(plant, cropState, 2);
        TileEntityCrop crop = new TileEntityCrop();
        world.setTileEntity(plant, crop);
        CropCard wheat = Crops.instance.getCropCard("ic2", "wheat");
        check(wheat != null, "Real IC2 wheat card resolves");
        crop.setCrop(wheat); crop.setCurrentSize(wheat.getMaxSize());
        crop.setStorageNutrients(0); crop.setStorageWater(0); crop.setStorageWeedEX(0);
        tile.inventory.setStackInSlot(0, IC2Items.getItem("crop_res", "fertilizer"));
        tile.inventory.getStackInSlot(0).setCount(10);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        tile.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(), 1000), true);

        boolean steadyWarning = true;
        for (int tick = 0; tick < 18; tick++) {
            tile.update();
            steadyWarning &= (tile.status & ManagerTile.NO_ENERGY) != 0;
        }
        check(steadyWarning, "Low EU remains visible on processing and idle ticks");
        check(crop.getCurrentSize() == wheat.getMaxSize() && crop.getStorageWater() == 0,
                "No crop action without EU");
        check(tile.inventory.getStackInSlot(0).getCount() == 10 && tile.water.getFluidAmount() == 1000,
                "No input consumption without EU");
        tile.energy.setEnergyStored(20000);
        pulse(tile, 1);
        check((tile.status & ManagerTile.NO_ENERGY) == 0, "Low EU clears after recharging");
        pulse(tile, 17);
        check(crop.getCurrentSize() < wheat.getMaxSize() && crop.getCrop() == wheat,
                "Native harvest preserves and resets real crop");
        check(crop.getStorageNutrients() == 90 && tile.inventory.getStackInSlot(0).getCount() == 9,
                "Native fertilizer consumes exactly one item");
        check(crop.getStorageWater() == 200 && tile.water.getFluidAmount() == 800,
                "Native hydration drains exactly 200 mB once");
        check(crop.getStorageWeedEX() == 150 && tile.weed.getFluidAmount() == 850,
                "Native Weed-Ex drains exactly 150 mB once");
        check(Math.abs(tile.getEnergy() - 19941) < 1e-6, "Combined pass costs 9 + 30 + 20 EU");
        pulse(tile, 18);
        check(tile.water.getFluidAmount() == 800 && tile.weed.getFluidAmount() == 850,
                "Satisfied crop consumes no further fluids");

        crop.setCurrentSize(wheat.getMaxSize());
        tile.settings(1, 2, 1);
        for (int i = 3; i < 18; i++) tile.inventory.setStackInSlot(i, new ItemStack(Items.WHEAT_SEEDS, 64));
        pulse(tile, 18);
        check(crop.getCurrentSize() == wheat.getMaxSize(), "Full output blocks harvesting before plant mutation");
        IItemHandler bottom = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        IItemHandler top = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        check(bottom.insertItem(0, new ItemStack(Items.WHEAT), false).getCount() == 1,
                "Output capability rejects insertion");
        check(top.extractItem(0, 1, false).isEmpty(), "Input capability rejects extraction");
        check(top.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1,
                "Input capability rejects invalid fertilizer");

        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        NBTTagCompound product = new ItemStack(Items.WHEAT).writeToNBT(new NBTTagCompound());
        product.setInteger("PendingCount", 200);
        NBTTagList pending = new NBTTagList(); pending.appendTag(product); saved.setTag("Pending", pending);
        ManagerTile restored = new ManagerTile(); restored.readFromNBT(saved);
        check(restored.getEnergy() == tile.getEnergy() && restored.water.getFluidAmount() == 800
                && restored.weed.getFluidAmount() == 850, "EU and both fluid tanks survive NBT reload");
        check(countItem(restored, Items.WHEAT) == 200, "Pending count above signed byte survives NBT reload");
        tile.invalidate(); world.removeTileEntity(base); world.setTileEntity(base, restored);
        restored.settings(1, 2, 0);
        restored.inventory.setStackInSlot(3, ItemStack.EMPTY);
        pulse(restored, 1);
        check(restored.inventory.getStackInSlot(3).getCount() == 64 && countItem(restored, Items.WHEAT) == 200,
                "Partial insertion conserves all pending products");
        double stoppedEU = restored.getEnergy(); pulse(restored, 20);
        check(restored.getEnergy() == stoppedEU, "Disabled machine consumes no scan EU");

        for (int i = 4; i < 18; i++) restored.inventory.setStackInSlot(i, ItemStack.EMPTY);
        pulse(restored, 1);
        check(countItem(restored, Items.WHEAT) == 200, "Pending queue flush conserves products across several slots");
        crop.setCurrentSize(1);
        ItemStack overclocker = IC2Items.getItem("upgrade", "overclocker");
        overclocker.setCount(2); restored.inventory.setStackInSlot(18, overclocker);
        restored.inventory.setStackInSlot(19, IC2Items.getItem("upgrade", "energy_storage"));
        restored.inventory.setStackInSlot(20, IC2Items.getItem("upgrade", "transformer"));
        restored.settings(2, 2, 0); restored.settings(1, 2, 1);
        restored.energy.setEnergyStored(20000);
        pulse(restored, 18);
        IProcessingUpgrade upgrade = (IProcessingUpgrade) overclocker.getItem();
        double time = Math.pow(upgrade.getProcessTimeMultiplier(overclocker, restored), 2);
        double demand = Math.pow(upgrade.getEnergyDemandMultiplier(overclocker, restored), 2);
        int scans = (int) Math.floor(18 * Math.min(8, 1.0 / (2 * time)));
        check(Math.abs(restored.getEnergy() - (20000 - scans * demand)) < 1e-5,
                "Overclockers apply native timing and demand multipliers");
        check(restored.energy.getCapacity() == 30000 && restored.energy.getSinkTier() == 2,
                "Storage and transformer upgrades have real effects");

        int neighborX = (base.getX() >> 4) + 1, neighborZ = base.getZ() >> 4;
        check(world.getChunkProvider().getLoadedChunk(neighborX, neighborZ) == null,
                "Neighbor chunk starts unloaded");
        restored.inventory.setStackInSlot(18, ItemStack.EMPTY);
        restored.settings(8, 2, 1); restored.energy.setEnergyStored(20000);
        pulse(restored, 578);
        check(world.getChunkProvider().getLoadedChunk(neighborX, neighborZ) == null,
                "Full radius-eight scan does not load neighbor chunk");

        restored.settings(1, 2, 0);
        restored.inventory.setStackInSlot(22, new ItemStack(Items.WATER_BUCKET));
        restored.inventory.setStackInSlot(24, new ItemStack(Items.BUCKET, 16));
        int waterBefore = restored.water.getFluidAmount();
        pulse(restored, 1);
        check(restored.water.getFluidAmount() == waterBefore && !restored.inventory.getStackInSlot(22).isEmpty(),
                "Full container output prevents input consumption and fluid transfer");
        restored.inventory.setStackInSlot(24, ItemStack.EMPTY);
        pulse(restored, 1);
        check(restored.water.getFluidAmount() == waterBefore + 1000 && restored.inventory.getStackInSlot(22).isEmpty()
                && restored.inventory.getStackInSlot(24).getItem() == Items.BUCKET,
                "Water bucket transfers once and preserves empty container");

        int wheatBefore = countItem(restored, Items.WHEAT);
        net.minecraft.util.math.AxisAlignedBB region = new net.minecraft.util.math.AxisAlignedBB(base).grow(2);
        // Remove fixture leftovers from earlier runs before checking this break.
        for (net.minecraft.entity.item.EntityItem entity : world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, region))
            entity.setDead();
        world.destroyBlock(base, true);
        int wheatDropped = 0;
        for (net.minecraft.entity.item.EntityItem entity : world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, region))
            if (!entity.isDead && entity.getItem().getItem() == Items.WHEAT) wheatDropped += entity.getItem().getCount();
        check(wheatDropped == wheatBefore, "Breaking machine drops harvested items exactly once");

        BlockPos edge = new BlockPos(100014, 110, 100008);
        // Flag 16 suppresses vanilla observer notifications while building this fixture.
        world.setBlockState(edge.east(), Blocks.STONE.getDefaultState(), 18);
        placeBasicFixture(world,edge,18);
        ManagerTile edgeTile = (ManagerTile) world.getTileEntity(edge);
        edgeTile.settings(1, 2, 0);
        edgeTile.markDirty();
        pulse(edgeTile, 2);
        check(world.getChunkProvider().getLoadedChunk(neighborX, neighborZ) == null,
                "Marking saves beside solid block at chunk edge does not load neighbor");
        world.setBlockToAir(edge);
        world.setBlockState(edge.down(), Blocks.FARMLAND.getDefaultState(), 18);
        world.setBlockState(edge, cropState, 18);
        TileEntityCrop edgeCrop = new TileEntityCrop(); world.setTileEntity(edge, edgeCrop);
        edgeCrop.setCrop(wheat); edgeCrop.setCurrentSize(1); edgeCrop.setStorageNutrients(0);
        BlockPos controllerPos = new BlockPos(100008, 110, 100008);
        placeBasicFixture(world,controllerPos,18);
        ManagerTile controller = (ManagerTile) world.getTileEntity(controllerPos);
        controller.settings(8, 2, 2); controller.energy.setEnergyStored(20000);
        controller.inventory.setStackInSlot(0, IC2Items.getItem("crop_res", "fertilizer"));
        // Removing the preceding fixture's tile invokes vanilla comparator notifications.
        // Start the operation assertion with its neighbor explicitly unloaded again.
        net.minecraft.world.chunk.Chunk setupNeighbor = world.getChunkProvider().getLoadedChunk(neighborX, neighborZ);
        if (setupNeighbor != null) { world.getChunkProvider().queueUnload(setupNeighbor); world.getChunkProvider().tick(); }
        pulse(controller, 578);
        check(edgeCrop.getStorageNutrients() == 90
                        && world.getChunkProvider().getLoadedChunk(neighborX, neighborZ) == null,
                "Saving a treated crop at chunk edge does not load neighbor");
        world.setBlockToAir(controllerPos); world.setBlockToAir(edge); world.setBlockToAir(edge.east());
    }
    private void revision04(WorldServer world) {
        BlockPos p = new BlockPos(100008, 145, 100008);
        net.minecraft.entity.player.EntityPlayerMP player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        player.setPosition(p.getX(), p.getY(), p.getZ());
        world.setBlockToAir(p); placeBasicFixture(world,p,2); world.setBlockState(p,world.getBlockState(p).withProperty(ManagerBlock.FACING,EnumFacing.EAST),18);
        ManagerTile old = (ManagerTile)world.getTileEntity(p); old.energy.setEnergyStored(1234); old.functions = 5; old.radius = 2; old.layers = 2; old.redstoneMode = 2;
        old.inventory.setStackInSlot(3, new ItemStack(Items.WHEAT, 11)); old.water.fill(new FluidStack(FluidRegistry.WATER, 1600), true);
        int beforeEntities = world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(p).grow(1)).size();
        old.migrate(); ManagerTile manager = (ManagerTile)world.getTileEntity(p);
        check(manager instanceof AdvancedManagerTile && world.getBlockState(p).getBlock() == CropManager.ADVANCED && world.getBlockState(p).getValue(ManagerBlock.FACING) == EnumFacing.EAST, "Basic Manager migrates to advanced and preserves orientation");
        check(manager.getEnergy() == 1234 && manager.inventory.getStackInSlot(3).getCount() == 11 && manager.water.getFluidAmount() == 1600 && manager.functions == 15 && manager.radius == 2 && manager.layers == 2 && manager.redstoneMode == 2, "Migration preserves EU inventory tanks and area controls");
        check(world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(p).grow(1)).size() == beforeEntities, "Migration never drops or duplicates the old inventory");
        for(String name:new String[]{"crop_manager","seed_classifier","seed_workbench","configuration_card","agricultural_notebook","compost"})check(net.minecraft.item.Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(CropManager.ID,name))==null || !new net.minecraft.util.ResourceLocation(CropManager.ID,name).equals(net.minecraft.item.Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(CropManager.ID,name)).getRegistryName()),"Obsolete item registry removed: "+name);
        for (String name : new String[]{"crop_manager", "seed_classifier", "seed_workbench", "configuration_card", "agricultural_notebook", "compost_fertilizer", "agricultural_reservoir", "agricultural_collector"}) check(net.minecraft.item.crafting.CraftingManager.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(CropManager.ID, name)) == null, "Removed recipe absent: " + name);
        for (String name : new String[]{"advanced_crop_manager", "agricultural_monitor", "electric_composter", "agricultural_probe", "wireless_eu_upgrade"}) check(net.minecraft.item.crafting.CraftingManager.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(CropManager.ID, name)) != null, "Active recipe present: " + name);
        BlockPos cp = p.east(2); world.setBlockToAir(cp); world.setBlockState(cp, CropManager.COMPOSTER.getDefaultState(), 2);
        ComposterTile c = (ComposterTile)world.getTileEntity(cp); c.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, 4));
        check(!c.processTick() && c.progress == 0 && c.inventory.getStackInSlot(0).getCount() == 4, "Dry composter preserves its inputs");
        c.water.fill(new FluidStack(FluidRegistry.WATER, 2000), true);
        check(!c.processTick() && c.progress == 0 && c.inventory.getStackInSlot(0).getCount() == 4, "Unpowered composter preserves its inputs"); c.energy.setEnergyStored(1000);
        for (int i = 0; i < 100; i++) c.update();
        check(world.getTileEntity(cp) == c && c.progress == 100 && c.getEnergy() == 500, "Composter retains tile while active and uses exactly 5 EU per tick");
        ComposterTile restored = new ComposterTile(); restored.setWorld(world); restored.setPos(cp); restored.readFromNBT(c.writeToNBT(new NBTTagCompound()));
        for (int i = 0; i < 100; i++) restored.processTick();
        check(restored.inventory.getStackInSlot(0).isEmpty() && restored.water.getFluidAmount() == 1000 && ManagerTile.isFertilizer(restored.inventory.getStackInSlot(1)) && restored.inventory.getStackInSlot(1).getCount() == 1 && restored.getEnergy() == 0, "Four plants, 1000 mB water and 1000 EU make one genuine IC2 fertilizer across save/load");
        c.inventory.setStackInSlot(0, new ItemStack(Items.WHEAT, 16)); c.inventory.setStackInSlot(1, ItemStack.EMPTY); c.energy.setEnergyStored(20000); c.progress = 100;
        ItemStack overclock = IC2Items.getItem("upgrade", "overclocker"); c.inventory.setStackInSlot(2, overclock);
        c.processTick(); int duration = Math.max(1, (int)Math.ceil(200 * ((IProcessingUpgrade)overclock.getItem()).getProcessTimeMultiplier(overclock, c)));
        check(c.workTicks == duration && c.progress == (int)(100L * duration / 200) + 1 && c.euPerTick == Math.ceil(5 * ((IProcessingUpgrade)overclock.getItem()).getEnergyDemandMultiplier(overclock, c)), "Overclocker applies actual IC2 timing demand and preserves completed work ratio");
        c.inventory.setStackInSlot(3, IC2Items.getItem("upgrade", "energy_storage")); c.inventory.setStackInSlot(4, IC2Items.getItem("upgrade", "transformer")); c.processTick();
        check(c.energy.getCapacity() == 30000 && c.energy.getSinkTier() == 2, "Composter storage and transformer upgrades have actual effects");
        ComposterTile reload = new ComposterTile(); reload.setWorld(world); reload.setPos(cp); reload.readFromNBT(c.writeToNBT(new NBTTagCompound()));
        check(reload.workTicks == c.workTicks && reload.progress == c.progress && reload.getEnergy() == c.getEnergy() && reload.energy.getSinkTier() == 2 && reload.inventory.getSlots() == 7, "All composter upgrade state survives reload");
        NBTTagCompound oldSave = c.writeToNBT(new NBTTagCompound()); oldSave.getCompoundTag("Inventory").setInteger("Size", 2); oldSave.getCompoundTag("Inventory").setTag("Items", new NBTTagList()); oldSave.removeTag("Duration");
        reload.readFromNBT(oldSave); check(reload.inventory.getSlots() == 7 && reload.workTicks == 200, "Old two-slot composter saves expand safely to seven slots");
        c.inventory.setStackInSlot(1, IC2Items.getItem("crop_res", "fertilizer").copy()); c.inventory.getStackInSlot(1).setCount(64); double eu = c.getEnergy(); int progress = c.progress;
        check(!c.processTick() && c.getEnergy() == eu && c.progress == progress, "Full fertilizer output consumes no energy and preserves progress");
        check(!c.itemsForFace(EnumFacing.UP).insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty() && c.itemsForFace(EnumFacing.UP).extractItem(0, 1, false).isEmpty() && c.itemsForFace(EnumFacing.DOWN).extractItem(1, 1, true).getCount() == 1, "Composter automation validates organics and exposes only output extraction");
        BlockPos mp = p.north(2); world.setBlockToAir(mp); world.setBlockState(mp, CropManager.MONITOR.getDefaultState(), 2);
        MonitorTile monitor = (MonitorTile)world.getTileEntity(mp); manager.energy.setEnergyStored(0); manager.update(); monitor.link(p, 0);
        check(monitor.alarmActive && monitor.signal == 15 && world.getTileEntity(mp) == monitor, "Monitor alarms on Manager low EU and retains tile identity");
        monitor.control(146, player); check(monitor.outputMode == 2 && monitor.signal == 0, "Normal output mode stays off during a selected fault");
        monitor.control(146, player); check(monitor.outputMode == 0 && monitor.signal == 0 && monitor.alarmActive, "Disabled output leaves visual diagnosis active");
        monitor.control(147, player); check(monitor.signal == 15 && world.getRedstonePower(mp, EnumFacing.UP) == 15, "Test button forces real redstone power even with disabled output");
        check(monitor.guiState().getInteger("Test") == 100, "Test pulse is configured for exactly 100 ticks");
        monitor.control(148, player); check(monitor.signal == 0, "Unlink cancels a running signal test");
        BlockPos matronPos = p.west(2); world.setBlockToAir(matronPos); 
        ic2.core.block.machine.tileentity.TileEntityCropmatron matron = new ic2.core.block.machine.tileentity.TileEntityCropmatron(); check(ic2.core.item.block.ItemBlockTileEntity.placeTeBlock(IC2Items.getItem("te", "cropmatron"), player, world, matronPos, EnumFacing.UP, matron), "Crop-Matron uses the original IC2 placement and network initialization");
        matron.getComponent(ic2.core.block.comp.Energy.class).addEnergy(1000); matron.getWaterTank().fill(new FluidStack(FluidRegistry.WATER, 1000), true); matron.getExTank().fill(new FluidStack(FluidName.weed_ex.getInstance(), 1000), true); matron.fertilizerSlot.put(IC2Items.getItem("crop_res", "fertilizer"));
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(CropManager.PROBE)); player.setSneaking(true);
        CropManager.PROBE.onItemUseFirst(player, world, matronPos, EnumFacing.UP, 0.5f,0.5f,0.5f,net.minecraft.util.EnumHand.MAIN_HAND); player.setSneaking(false);
        CropManager.PROBE.onItemUseFirst(player, world, mp, EnumFacing.UP,0.5f,0.5f,0.5f,net.minecraft.util.EnumHand.MAIN_HAND);
        monitor.control(146, player); monitor.control(146, player);
        check("connected".equals(monitor.connection) && "cropmatron".equals(monitor.snapshot.getString("Target")) && monitor.snapshot.getInteger("Fertilizer") == 1 && monitor.snapshot.getDouble("EU") == matron.getEnergy() && !monitor.alarmActive && monitor.signal == 15, "Probe links native Crop-Matron with real resources and normal-mode signal");
        matron.getWaterTank().drain(1000, true); monitor.refresh();
        check(monitor.alarmActive && monitor.signal == 0, "Native Crop-Matron water fault obeys selected category and normal mode"); monitor.outputMode = 1; monitor.refresh();
        check(monitor.signal == 15, "Native Crop-Matron fault drives alarm-mode redstone");
        BlockPos lamp = mp.up(); world.setBlockState(lamp, Blocks.REDSTONE_LAMP.getDefaultState(), 3); world.neighborChanged(lamp, CropManager.MONITOR, mp);
        check(world.getBlockState(lamp).getBlock() == Blocks.LIT_REDSTONE_LAMP, "Monitor output lights an actual adjacent redstone lamp");
        MonitorTile loadedMonitor = new MonitorTile(); loadedMonitor.setWorld(world); loadedMonitor.setPos(mp); loadedMonitor.readFromNBT(monitor.writeToNBT(new NBTTagCompound())); loadedMonitor.refresh();
        check(loadedMonitor.outputMode == 1 && "connected".equals(loadedMonitor.connection), "Monitor link mask and output mode survive reload");
        monitor.control(148, player); check("unlinked".equals(monitor.connection) && monitor.signal == 0 && !monitor.alarmActive, "Unlink clears both target data and signal");
        check(!monitor.control(999, player), "Monitor rejects unknown controls");
        monitor.link(p, 1); check(monitor.signal == 15 && "dimension".equals(monitor.connection), "Dimension fault is visible and alarms without loading another world");
        BlockPos unloaded = new BlockPos(100024,145,100008); net.minecraft.world.chunk.Chunk chunk = world.getChunkProvider().getLoadedChunk(unloaded.getX()>>4,unloaded.getZ()>>4); if (chunk != null) { world.getChunkProvider().queueUnload(chunk); world.getChunkProvider().tick(); }
        monitor.link(unloaded, 0); check("unloaded".equals(monitor.connection) && world.getChunkProvider().getLoadedChunk(unloaded.getX()>>4,unloaded.getZ()>>4) == null, "Alarm handling does not load an unavailable target chunk");
        net.minecraft.inventory.Container dummy=new net.minecraft.inventory.Container(){public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p){return true;}};
        net.minecraft.inventory.InventoryCrafting grid=new net.minecraft.inventory.InventoryCrafting(dummy,3,3);grid.setInventorySlotContents(0,IC2Items.getItem("te","crop_harvester"));grid.setInventorySlotContents(1,IC2Items.getItem("crafting","iridium"));grid.setInventorySlotContents(2,IC2Items.getItem("crafting","advanced_circuit"));grid.setInventorySlotContents(3,IC2Items.getItem("crafting","advanced_circuit"));grid.setInventorySlotContents(4,IC2Items.getItem("te","cropmatron"));grid.setInventorySlotContents(5,IC2Items.getItem("crafting","advanced_circuit"));grid.setInventorySlotContents(6,IC2Items.getItem("crafting","iridium"));grid.setInventorySlotContents(7,IC2Items.getItem("crafting","iridium"));grid.setInventorySlotContents(8,IC2Items.getItem("crafting","iridium"));
        net.minecraft.item.crafting.IRecipe recipe=net.minecraft.item.crafting.CraftingManager.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(CropManager.ID,"advanced_crop_manager"));check(recipe.matches(grid,world) && recipe.getCraftingResult(grid).getItem()==net.minecraft.item.Item.getItemFromBlock(CropManager.ADVANCED),"Advanced recipe no longer depends on removed basic Manager");
        BlockPos edge = new BlockPos(100015,155,100008), receiver = edge.west();
        net.minecraft.world.chunk.Chunk neighbor = world.getChunkProvider().getLoadedChunk(6251,6250); if(neighbor!=null){world.getChunkProvider().queueUnload(neighbor);world.getChunkProvider().tick();}
        world.setBlockState(edge,CropManager.COMPOSTER.getDefaultState(),18); ComposterTile edgeCompost=(ComposterTile)world.getTileEntity(edge);edgeCompost.energy.setEnergyStored(100);edgeCompost.inventory.setStackInSlot(0,new ItemStack(Items.WHEAT,8));edgeCompost.update();
        check(world.getChunkProvider().getLoadedChunk(6251,6250)==null,"Composter activity animation does not load a neighboring chunk");
        world.setBlockState(receiver,CropManager.ADVANCED.getDefaultState(),18);
        BlockPos edgeMonitorPos=edge.up(6);world.setBlockState(edgeMonitorPos,CropManager.MONITOR.getDefaultState(),18);MonitorTile edgeMonitor=(MonitorTile)world.getTileEntity(edgeMonitorPos);edgeMonitor.link(receiver,0);
        check(edgeMonitor.signal==15 && world.getChunkProvider().getLoadedChunk(6251,6250)==null,"Monitor alarm and redstone notifications respect unloaded neighbor chunks");
        world.setBlockState(edge,Blocks.AIR.getDefaultState(),18);world.setBlockState(receiver,Blocks.AIR.getDefaultState(),18);world.setBlockState(edgeMonitorPos,Blocks.AIR.getDefaultState(),18);
        world.setBlockToAir(lamp);world.setBlockToAir(cp);world.setBlockToAir(mp);world.setBlockToAir(matronPos);world.setBlockToAir(p);
    }
    private void placeBasicFixture(WorldServer world,BlockPos pos,int flags){world.setBlockState(pos,CropManager.ADVANCED.getDefaultState(),flags);world.setTileEntity(pos,new ManagerTile());}
    private void revision05(WorldServer world){
        BlockPos p=new BlockPos(100008,165,100008);world.setBlockToAir(p);world.setBlockState(p,CropManager.ADVANCED.getDefaultState(),18);ManagerTile manager=(ManagerTile)world.getTileEntity(p);manager.energy.setEnergyStored(10000);manager.functions=0;manager.automation.mode=1;manager.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(),3999),true);manager.update();
        check(manager.automation.stopped && manager.paused && manager.getEnergy()==10000,"Automation stops scanning below 20% Weed-Ex without consuming scan EU");manager.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(),16000),true);manager.update();check(manager.paused,"Manager remains stopped at 19999 mB even after original threshold clears");
        ManagerTile saved=new AdvancedManagerTile();saved.setWorld(world);saved.setPos(p);saved.readFromNBT(manager.writeToNBT(new NBTTagCompound()));saved.update();check(saved.automation.stopped,"Automatic stop latch persists across save/load without monitor");
        manager.weed.fill(new FluidStack(FluidName.weed_ex.getInstance(),1),true);manager.update();check(!manager.paused && !manager.automation.stopped && manager.getEnergy()<10000,"Manager resumes only when Weed-Ex is completely full");manager.enabled=false;double eu=manager.getEnergy();manager.update();check(manager.paused && manager.getEnergy()==eu,"Manual off overrides automatic return and halts EU-consuming scans");
        BlockPos mp=p.north(2);world.setBlockToAir(mp);world.setBlockState(mp,CropManager.MONITOR.getDefaultState(),18);MonitorTile monitor=(MonitorTile)world.getTileEntity(mp);monitor.link(p,0);
        net.minecraft.entity.player.EntityPlayerMP player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);player.setPosition(mp.getX(),mp.getY(),mp.getZ());
        check(monitor.control(200,player) && manager.enabled,"Monitor can switch a linked Manager on");int previous=manager.functions;check(monitor.control(201,player) && manager.functions==(previous^1),"Monitor changes the source harvest function rather than a local copy");
        monitor.control(220,player);check(manager.automation.mode==2,"Monitor edits rules stored on the Manager");check(!monitor.control(140,player),"Old selectable fault controls are removed");check(!monitor.control(210,player),"Remote monitor does not change local-only area settings");
        monitor.link(p,1);previous=manager.functions;check(!monitor.control(202,player) && manager.functions==previous,"Remote control rejects a different-dimension target");monitor.link(p,0);
        manager.control(211,player);check(manager.radius==5,"Advanced area radius can be increased");manager.control(212,player);check(manager.layers==6,"Advanced area layers can be selected");manager.layers=1;check(!manager.control(212,player) && manager.layers==1,"Area controls cannot disable every layer");manager.radius=ManagerConfig.maxRadius;check(!manager.control(211,player),"Area controls respect the server radius limit");manager.control(215,player);check(manager.radius==ManagerConfig.defaultRadius && manager.layers==ManagerConfig.defaultLayers,"Area reset restores configured defaults");
        BlockPos wp=p.east(3);world.setBlockToAir(wp);world.setBlockState(wp,CropManager.WEEDER.getDefaultState(),18);WeederTile weeder=(WeederTile)world.getTileEntity(wp);weeder.radius=1;weeder.layers=2;
        BlockPos weedPos=wp.west();world.setBlockToAir(weedPos);world.setBlockState(weedPos.down(),Blocks.FARMLAND.getDefaultState(),18);world.setBlockState(weedPos,IC2Items.getItemAPI().getBlockState("te","crop"),18);TileEntityCrop crop=new TileEntityCrop();world.setTileEntity(weedPos,crop);crop.setCrop(Crops.weed);crop.setCurrentSize(2);
        weeder.cursor=3;check(!weeder.scanOne() && crop.getCrop()==Crops.weed,"Unfuelled weeder never removes crops");weeder.gas.fill(new FluidStack(FluidName.biogas.getInstance(),100),true);weeder.cursor=3;
        check(weeder.scanOne() && crop.getCrop()==null && world.getTileEntity(weedPos)==crop && weeder.removed==1 && weeder.gas.getFluidAmount()==79,"Biogas weeder removes true weeds for 1+20 mB and preserves crop sticks");
        CropCard wheat=Crops.instance.getCropCard("ic2","wheat");crop.setCrop(wheat);crop.setCurrentSize(7);crop.setStatGrowth(31);weeder.cursor=3;check(!weeder.scanOne() && crop.getCrop()==wheat && crop.getStatGrowth()==31,"High-growth valuable crops are never mistaken for removable weeds");
        check(weeder.gas.fill(new FluidStack(FluidRegistry.WATER,1000),true)==0,"Weeder rejects water as fuel");check(weeder.inventory.getSlots()==2,"Weeder uses fuel container slots");
        WeederTile savedWeeder=new WeederTile();savedWeeder.readFromNBT(weeder.writeToNBT(new NBTTagCompound()));check(savedWeeder.gas.getFluidAmount()==weeder.gas.getFluidAmount() && savedWeeder.removed==1 && savedWeeder.radius==1 && savedWeeder.layers==2,"Weeder fuel counter and area persist");weeder.enabled=false;int gas=weeder.gas.getFluidAmount();weeder.update();check(weeder.gas.getFluidAmount()==gas && weeder.paused,"Weeder manual off stops fuel consumption");
        check(!CropManager.WEEDER.isFullCube(world.getBlockState(wp)) && !CropManager.WEEDER.isOpaqueCube(world.getBlockState(wp)),"Weeder is a non-cubic 3D block");
        monitor.link(wp,0);check("weeder".equals(monitor.snapshot.getString("Target")) && monitor.snapshot.getInteger("Gas")==weeder.gas.getFluidAmount(),"Monitor reads the biogas weeder fuel and removal counter");check(monitor.control(200,player) && weeder.enabled,"Monitor switches the linked biogas weeder on");check(!monitor.control(201,player),"Weeder does not accept Manager-only harvest controls");
        world.setBlockToAir(weedPos);world.setBlockToAir(wp);world.setBlockToAir(mp);world.setBlockToAir(p);
    }

    private void revision052Survey(WorldServer world) {
        BlockPos p = new BlockPos(100008, 180, 100008);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) world.setBlockToAir(p.add(x,0,z));
        world.setBlockState(p, CropManager.ADVANCED.getDefaultState(), 18);
        ManagerTile manager = (ManagerTile)world.getTileEntity(p);
        manager.settings(1,2,0); manager.energy.setEnergyStored(10000);
        CropCard wheat = Crops.instance.getCropCard("ic2","wheat");
        BlockPos[] positions = {p.west(),p.north(),p.east(),p.south()};
        int[] sums = new int[4], minimum = new int[4], maximum = new int[4];
        for (int i = 0; i < positions.length; i++) {
            BlockPos cp = positions[i];
            world.setBlockState(cp.down(),Blocks.FARMLAND.getDefaultState(),18);
            world.setBlockState(cp,IC2Items.getItemAPI().getBlockState("te","crop"),18);
            TileEntityCrop crop = new TileEntityCrop(); world.setTileEntity(cp,crop);
            crop.setCrop(i == 3 ? Crops.weed : wheat); crop.setCurrentSize(2); crop.setScanLevel(i < 2 ? 1 : 0);
            if (i < 3) {
                int[] values = {crop.getTerrainNutrients(),crop.getTerrainHumidity(),crop.getTerrainAirQuality(),crop.getLightLevel()};
                for (int k = 0; k < 4; k++) {
                    sums[k] += values[k];
                    if (i == 0 || values[k] < minimum[k]) minimum[k] = values[k];
                    if (i == 0 || values[k] > maximum[k]) maximum[k] = values[k];
                }
            }
        }
        pulse(manager,1);
        check(!manager.report.hasKey("Survey"),"Survey stays unpublished until the existing scan completes a full cycle");
        for (int i = 0; i < 100 && !manager.report.hasKey("Survey"); i++) pulse(manager,1);
        NBTTagCompound survey = manager.report.getCompoundTag("Survey");
        NBTTagList species = survey.getTagList("Species",10);
        check(survey.getInteger("Count") == 3 && species.tagCount() == 2,
                "Completed real crop scan groups two identified plants and one unknown while excluding true weeds");
        check(species.getCompoundTagAt(0).getInteger("Count") == 2 && "wheat".equals(species.getCompoundTagAt(0).getString("Id"))
                        && species.getCompoundTagAt(1).getInteger("Count") == 1 && species.getCompoundTagAt(1).getString("Id").isEmpty()
                        && species.getCompoundTagAt(1).getString("Owner").isEmpty() && !species.getCompoundTagAt(0).hasKey("Stats"),
                "Level-one analysis reveals species identity but survey publishes no unknown identity or seed genetics");
        check(java.util.Arrays.equals(sums,survey.getIntArray("Sum")) && java.util.Arrays.equals(minimum,survey.getIntArray("Min"))
                        && java.util.Arrays.equals(maximum,survey.getIntArray("Max")),
                "Survey environment aggregates match actual IC2 terrain nutrients humidity air and light readings");
        NBTTagCompound snapshot = MonitorTile.readTarget(manager);
        check(snapshot.getCompoundTag("Report").getCompoundTag("Survey").equals(survey),
                "Monitor receives the same completed species and environment survey as the linked Manager");
        AdvancedManagerTile restored = new AdvancedManagerTile();
        restored.readFromNBT(manager.writeToNBT(new NBTTagCompound()));
        check(!restored.report.hasKey("Survey") && restored.guiState().getLong("ReportAge") == -1,
                "Reloading old or current Manager NBT waits for a fresh survey instead of fabricating saved environment data");
        manager.control(211,null);
        check(!manager.report.hasKey("Survey") && manager.guiState().getLong("ReportAge") == -1,
                "Changing the configured area immediately invalidates the previous crop survey");
        for (BlockPos cp : positions) world.setBlockToAir(cp);
        for (int i = 0; i < 150 && !manager.report.hasKey("Survey"); i++) pulse(manager,1);
        survey = manager.report.getCompoundTag("Survey");
        check(survey.hasKey("Count") && survey.getInteger("Count") == 0 && survey.getTagList("Species",10).tagCount() == 0,
                "An empty planting area publishes an empty survey with no imaginary crop or environment sample");
        for (BlockPos cp : positions) world.setBlockToAir(cp.down());
        world.setBlockToAir(p);
    }

}

