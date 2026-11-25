package br.cropmanager;

import ic2.api.energy.tile.IHeatSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes de regressão para as correções e otimizações da revisão de código. */
public final class ReviewFixesRegressionTest {

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.WIRELESS_EU.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.WIRELESS_EU);
        }
    }

    private static final class StubHeatSource extends TileEntity implements IHeatSource {
        int available;
        int requestCalls;
        int drawCalls;

        StubHeatSource(int available) {
            this.available = available;
        }

        @Override public int maxrequestHeatTick(EnumFacing side) { return available; }

        @Override public int requestHeat(EnumFacing side, int request) {
            requestCalls++;
            return Math.min(available, request);
        }

        @Override public int drawHeat(EnumFacing side, int request, boolean simulate) {
            drawCalls++;
            int pulled = Math.min(available, request);
            if (!simulate) available -= pulled;
            return pulled;
        }
    }

    private static World worldWithTiles(Map<BlockPos, TileEntity> tiles) throws ReflectiveOperationException {
        World world = mock(World.class);
        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, new WorldProviderSurface());
        when(world.isBlockLoaded(any(BlockPos.class), anyBoolean())).thenReturn(true);
        when(world.getTileEntity(any(BlockPos.class))).thenAnswer(inv -> tiles.get(inv.getArgument(0)));
        return world;
    }

    @Test
    public void thermalPipeChainIsLinearAndDiamondDoesNotDoubleCountHeat() throws ReflectiveOperationException {
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World world = worldWithTiles(tiles);

        StubHeatSource source = new StubHeatSource(50);
        BlockPos sourcePos = new BlockPos(25, 64, 0);
        source.setWorld(world);
        source.setPos(sourcePos);
        tiles.put(sourcePos, source);

        ThermalPipeTile[] chain = new ThermalPipeTile[25];
        for (int i = 0; i < chain.length; i++) {
            BlockPos p = new BlockPos(i, 64, 0);
            chain[i] = new ThermalPipeTile();
            chain[i].setWorld(world);
            chain[i].setPos(p);
            tiles.put(p, chain[i]);
        }

        long startNs = System.nanoTime();
        int requested = chain[0].requestHeat(EnumFacing.WEST, 100);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertEquals(50, requested);
        assertEquals(0, source.requestCalls);
        assertEquals(1, source.drawCalls);
        assertTrue("Linha de 25 tubos deve executar em tempo linear (< 1000 ms), levou " + elapsedMs, elapsedMs < 1000L);

        // Malha em diamante: dois caminhos paralelos ligados à mesma fonte de 10 HU
        Map<BlockPos, TileEntity> diamondTiles = new HashMap<>();
        World diamondWorld = worldWithTiles(diamondTiles);
        StubHeatSource diamondSource = new StubHeatSource(10);
        BlockPos genPos = new BlockPos(0, 64, 0);
        diamondSource.setWorld(diamondWorld);
        diamondSource.setPos(genPos);
        diamondTiles.put(genPos, diamondSource);

        BlockPos[] pipePositions = {
                new BlockPos(1, 64, 0), new BlockPos(1, 64, 1), new BlockPos(1, 64, 2),
                new BlockPos(-1, 64, 0), new BlockPos(-1, 64, 1), new BlockPos(-1, 64, 2),
                new BlockPos(0, 64, 2)
        };
        ThermalPipeTile consumerPipe = null;
        for (BlockPos p : pipePositions) {
            ThermalPipeTile pipe = new ThermalPipeTile();
            pipe.setWorld(diamondWorld);
            pipe.setPos(p);
            diamondTiles.put(p, pipe);
            if (p.equals(new BlockPos(0, 64, 2))) consumerPipe = pipe;
        }

        assertEquals(10, consumerPipe.drawHeat(EnumFacing.SOUTH, 20, true));
        assertEquals(10, consumerPipe.requestHeat(EnumFacing.SOUTH, 20));
        assertEquals(0, diamondSource.available);
        assertEquals(0, consumerPipe.drawHeat(EnumFacing.SOUTH, 20, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fluidTanksHandleNullFluidTypeQueryWithoutNpe() throws ReflectiveOperationException {
        Capability<IFluidHandler> fluidCap = mock(Capability.class);
        when(fluidCap.cast(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        Field fluidField = CapabilityFluidHandler.class.getDeclaredField("FLUID_HANDLER_CAPABILITY");
        fluidField.setAccessible(true);
        fluidField.set(null, fluidCap);

        ManagerTile manager = new ManagerTile();
        MonitorTile monitor = new MonitorTile();
        WeederTile weeder = new WeederTile();
        GrinExtractorTile extractor = new GrinExtractorTile();
        WeedExFabricatorTile fabricator = new WeedExFabricatorTile();

        assertFalse(manager.water.canFillFluidType(null));
        assertFalse(manager.weed.canFillFluidType(null));
        assertFalse(monitor.water.canFillFluidType(null));
        assertFalse(monitor.weed.canFillFluidType(null));
        assertFalse(weeder.gas.canFillFluidType(null));
        assertFalse(extractor.water.canFillFluidType(null));
        assertFalse(extractor.extract.canFillFluidType(null));
        assertFalse(fabricator.extract.canFillFluidType(null));
        assertFalse(fabricator.weed.canFillFluidType(null));

        for (TileEntity tile : new TileEntity[] {manager, monitor, weeder, extractor, fabricator}) {
            IFluidHandler handler = tile.getCapability(fluidCap, EnumFacing.NORTH);
            assertNotNull(handler);
            for (IFluidTankProperties prop : handler.getTankProperties()) {
                assertFalse(prop.canFillFluidType(null));
            }
        }
    }

    @Test
    public void managerSettingsAlwaysPreservesCareFunctionBits() {
        ManagerTile tile = new ManagerTile();
        tile.settings(4, 7, 0);
        assertEquals(14, tile.functions);
        tile.settings(4, 7, 1);
        assertEquals(15, tile.functions);
        tile.settings(4, 7, 2);
        assertEquals(14, tile.functions);
    }

    @Test
    public void monitorWirelessSupportPowerUsesLoadedChunksOnly() throws ReflectiveOperationException {
        World world = mock(World.class);
        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, new WorldProviderSurface());
        // loadedTileEntityList permanece null: qualquer iteração sobre ela lançaria NullPointerException.
        assertNull(world.loadedTileEntityList);

        IChunkProvider chunkProvider = mock(IChunkProvider.class);
        when(world.getChunkProvider()).thenReturn(chunkProvider);

        ComposterTile composter = new ComposterTile();
        composter.setWorld(world);
        composter.setPos(new BlockPos(4, 64, 0));

        Chunk loadedChunk = mock(Chunk.class);
        when(loadedChunk.getTileEntityMap()).thenReturn(Collections.singletonMap(composter.getPos(), composter));
        when(chunkProvider.getLoadedChunk(0, 0)).thenReturn(loadedChunk);

        MonitorTile monitor = new MonitorTile();
        monitor.setWorld(world);
        monitor.setPos(new BlockPos(0, 64, 0));
        monitor.inventory.setStackInSlot(MonitorTile.WIRELESS_EU_SLOT, new ItemStack(CropManager.WIRELESS_EU, 1));
        monitor.recalculateWirelessEu();
        monitor.energy.setEnergyStored(1000.0);

        monitor.transferWirelessEnergy();

        assertEquals(256.0, composter.getEnergy(), 0.0);
        assertEquals(744.0, monitor.energy.getEnergyStored(), 0.0);
        verify(chunkProvider, never()).provideChunk(anyInt(), anyInt());
    }

    @Test
    public void managerContainerIgnoresReportAgeOnlyTicks() {
        NBTTagCompound a = new NBTTagCompound();
        a.setDouble("EU", 500.0);
        a.setLong("ReportAge", 10L);

        NBTTagCompound b = new NBTTagCompound();
        b.setDouble("EU", 500.0);
        b.setLong("ReportAge", 11L);

        assertTrue(ManagerContainer.sameVisibleState(a, b));

        b.setDouble("EU", 499.0);
        assertFalse(ManagerContainer.sameVisibleState(a, b));
    }

    @Test
    public void weederAndManagerThrottleMarkDirtyDuringIdleScans() throws ReflectiveOperationException {
        World weederWorld = mock(World.class);
        when(weederWorld.getHeight()).thenReturn(256);
        when(weederWorld.isBlockLoaded(any(BlockPos.class), anyBoolean())).thenReturn(true);

        WeederTile weeder = new WeederTile();
        weeder.setWorld(weederWorld);
        weeder.setPos(new BlockPos(0, 64, 0));
        weeder.gas.setFluid(new FluidStack(FluidRegistry.WATER, 1000));

        for (int i = 0; i < 19; i++) {
            assertFalse(weeder.scanOne());
        }
        verify(weederWorld, never()).markChunkDirty(any(BlockPos.class), any(TileEntity.class));
        assertFalse(weeder.scanOne());
        verify(weederWorld, times(1)).markChunkDirty(weeder.getPos(), weeder);

        World managerWorld = mock(World.class);
        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(managerWorld, new WorldProviderSurface());
        when(managerWorld.getHeight()).thenReturn(256);
        when(managerWorld.isBlockLoaded(any(BlockPos.class), anyBoolean())).thenReturn(true);
        when(managerWorld.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.getDefaultState());

        ManagerTile manager = new ManagerTile(true);
        manager.setWorld(managerWorld);
        manager.setPos(new BlockPos(0, 64, 0));
        manager.energy.setEnergyStored(10000.0);
        Field addedToEnet = ic2.api.energy.prefab.BasicSink.class.getSuperclass().getDeclaredField("addedToEnet");
        addedToEnet.setAccessible(true);
        addedToEnet.setBoolean(manager.energy, true);

        for (int i = 0; i < 19; i++) {
            manager.update();
        }
        verify(managerWorld, never()).markChunkDirty(any(BlockPos.class), any(TileEntity.class));
        manager.update();
        verify(managerWorld, times(1)).markChunkDirty(manager.getPos(), manager);
    }

    @Test
    public void jeiPluginUsesTopLevelCropStickAndCurrentModName() throws Exception {
        assertEquals("IC2: Agronomy Expansion", CropManager.NAME);

        java.util.Set<String> pluginStrings = readConstantPoolUtf8("/br/cropmanager/jei/CropManagerJeiPlugin.class");
        assertTrue("JEI plugin must query top-level IC2 item 'crop_stick'", pluginStrings.contains("crop_stick"));
        assertFalse("JEI plugin must not look up 'crop_res' for crop_stick", pluginStrings.contains("crop_res"));

        java.util.Set<String> categoryStrings = readConstantPoolUtf8("/br/cropmanager/jei/CropRecipeCategory.class");
        assertTrue("JEI category mod name must match current mod name",
                categoryStrings.contains("IC2: Agronomy Expansion"));
        assertFalse("JEI category must not hardcode legacy 'Crop Manager' name",
                categoryStrings.contains("Crop Manager"));
    }

    private static java.util.Set<String> readConstantPoolUtf8(String classResource) throws Exception {
        try (java.io.InputStream raw = ReviewFixesRegressionTest.class.getResourceAsStream(classResource)) {
            assertNotNull("Missing compiled class resource: " + classResource, raw);
            java.io.DataInputStream in = new java.io.DataInputStream(new java.io.BufferedInputStream(raw));
            assertEquals(0xCAFEBABE, in.readInt());
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major
            int cpCount = in.readUnsignedShort();
            java.util.Set<String> utf8 = new java.util.HashSet<>();
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1:
                        utf8.add(in.readUTF());
                        break;
                    case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                        in.readInt();
                        break;
                    case 5: case 6:
                        in.readLong();
                        i++;
                        break;
                    case 7: case 8: case 16: case 19: case 20:
                        in.readUnsignedShort();
                        break;
                    case 15:
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                        break;
                    default:
                        throw new IllegalStateException("Unsupported constant pool tag: " + tag);
                }
            }
            return utf8;
        }
    }
}
