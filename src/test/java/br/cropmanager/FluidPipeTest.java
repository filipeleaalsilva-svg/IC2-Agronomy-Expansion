package br.cropmanager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

/** Archetype B tests for FluidPipe (auto-transfer, anti-loop, bounds, NBT, capabilities). */
public final class FluidPipeTest {
    @BeforeClass
    public static void boot() {
        Bootstrap.register();
        TestCapabilityHelper.ensureFluidCapabilities();
        if (TileEntity.getKey(FluidPipeTile.class) == null) {
            TileEntity.register("cropmanager:fluid_pipe", FluidPipeTile.class);
        }
        if (TileEntity.getKey(IndustrialTankTile.class) == null) {
            TileEntity.register("cropmanager:fluid_tank", IndustrialTankTile.class);
        }
        if (TileEntity.getKey(StorageCasingTile.class) == null) {
            TileEntity.register("cropmanager:storage_casing", StorageCasingTile.class);
        }
        if (TileEntity.getKey(AgriculturalReservoirTile.class) == null) {
            TileEntity.register("cropmanager:agricultural_reservoir", AgriculturalReservoirTile.class);
        }
        if (TileEntity.getKey(ReservoirCasingTile.class) == null) {
            TileEntity.register("cropmanager:reservoir_casing", ReservoirCasingTile.class);
        }
        if (!ForgeRegistries.ITEMS.containsKey(CropManager.RESERVOIR_RESERVE.getRegistryName())) {
            ForgeRegistries.ITEMS.register(CropManager.RESERVOIR_RESERVE);
        }
    }

    @Test
    public void transfersFromSourceToConsumerAndEqualizesWithoutPingPongLoop() throws Exception {
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World world = mockWorld(tiles, false);

        BlockPos srcPos = new BlockPos(0, 64, 0);
        BlockPos p1Pos = new BlockPos(1, 64, 0);
        BlockPos p2Pos = new BlockPos(2, 64, 0);
        BlockPos dstPos = new BlockPos(3, 64, 0);

        FluidTank srcTank = new FluidTank(10000);
        srcTank.setCanFill(false); // dedicated output source
        srcTank.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        tiles.put(srcPos, tankTile(srcTank));

        FluidTank dstTank = new FluidTank(10000);
        dstTank.setCanDrain(false); // dedicated consumer (like Sprinkler)
        tiles.put(dstPos, tankTile(dstTank));

        FluidPipeTile p1 = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p1Pos, CropManager.FLUID_PIPE.getDefaultState());
        FluidPipeTile p2 = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p2Pos, CropManager.FLUID_PIPE.getDefaultState());
        tiles.put(p1Pos, p1);
        tiles.put(p2Pos, p2);

        // Pulse interval = 5 ticks -> transfers 250 mB on 5th tick
        for (int t = 0; t < 5; t++) p1.update();
        assertEquals(750, srcTank.getFluidAmount());
        assertEquals(250, dstTank.getFluidAmount());

        // Bidirectional tanks equalize safely without oscillating forever
        srcTank.setCanFill(true);
        dstTank.setCanDrain(true);
        srcTank.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        dstTank.setFluid(new FluidStack(FluidRegistry.WATER, 0));
        for (int t = 0; t < 20; t++) {
            p1.update();
            p2.update();
        }
        assertEquals(500, srcTank.getFluidAmount());
        assertEquals(500, dstTank.getFluidAmount());
    }

    @Test
    public void respectsClientSideGuardUnloadedChunksWorldHeightAndCapabilities() throws Exception {
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World clientWorld = mockWorld(tiles, true);

        BlockPos pPos = new BlockPos(5, 0, 5);
        FluidTank src = new FluidTank(4000);
        src.setFluid(new FluidStack(FluidRegistry.WATER, 2000));
        tiles.put(pPos.east(), tankTile(src));
        // Below world (y = -1) must be ignored cleanly
        tiles.put(pPos.down(), tankTile(new FluidTank(4000)));

        FluidPipeTile clientPipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), clientWorld, pPos, CropManager.FLUID_PIPE.getDefaultState());
        tiles.put(pPos, clientPipe);
        for (int t = 0; t < 10; t++) clientPipe.update();
        assertEquals("Client world must be a no-op", 0, clientPipe.cooldown);
        assertEquals(2000, src.getFluidAmount());

        // Unloaded chunk & y<0 guard on server
        World serverWorld = mockWorld(tiles, false);
        Mockito.when(serverWorld.isBlockLoaded(pPos.east(), false)).thenReturn(false);
        FluidPipeTile serverPipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), serverWorld, pPos, CropManager.FLUID_PIPE.getDefaultState());
        for (int t = 0; t < 5; t++) serverPipe.update();
        assertEquals(2000, src.getFluidAmount());

        // Capabilities on all 6 faces + null + external passive fill
        World fillWorld = mockWorld(tiles, false);
        FluidTank dst = new FluidTank(4000);
        tiles.put(pPos.west(), tankTile(dst));
        FluidPipeTile activePipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), fillWorld, pPos, CropManager.FLUID_PIPE.getDefaultState());
        for (EnumFacing face : EnumFacing.VALUES) {
            assertTrue(activePipe.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face));
            assertNotNull(activePipe.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face));
        }
        assertTrue(activePipe.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null));
        IFluidHandler westIn = activePipe.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.EAST);
        assertEquals(0, westIn.fill(null, true));
        assertNull(westIn.drain(100, true));
        assertEquals(300, westIn.fill(new FluidStack(FluidRegistry.WATER, 300), true));
        assertEquals(300, dst.getFluidAmount());
    }

    @Test
    public void nbtClampingShouldRefreshAndDynamicBoundingBoxHold() throws Exception {
        NBTTagCompound corrupt = new NBTTagCompound();
        corrupt.setInteger("Kind", 99);
        corrupt.setInteger("OutputSide", -3);
        corrupt.setInteger("Cooldown", -50);
        FluidPipeTile loaded = new FluidPipeTile();
        loaded.readFromNBT(corrupt);
        assertEquals(FluidPipeBlock.Kind.ROUTER, loaded.kind);
        assertEquals(EnumFacing.NORTH, loaded.outputSide);
        assertEquals(0, loaded.cooldown);

        FluidPipeTile original = new FluidPipeTile(FluidPipeBlock.Kind.PIPE);
        original.cooldown = 3;
        NBTTagCompound saved = original.writeToNBT(new NBTTagCompound());
        FluidPipeTile roundTrip = new FluidPipeTile();
        roundTrip.readFromNBT(saved);
        assertEquals(FluidPipeBlock.Kind.PIPE, roundTrip.kind);
        assertEquals(3, roundTrip.cooldown);

        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World world = mockWorld(tiles, false);
        BlockPos pos = new BlockPos(2, 64, 2);
        assertFalse(roundTrip.shouldRefresh(world, pos, CropManager.FLUID_PIPE.getDefaultState(), CropManager.FLUID_PIPE.getDefaultState()));
        assertTrue(roundTrip.shouldRefresh(world, pos, CropManager.FLUID_PIPE.getDefaultState(), Blocks.AIR.getDefaultState()));

        // Bounding box connects toward north neighbor
        tiles.put(pos.north(), tankTile(new FluidTank(1000)));
        AxisAlignedBB box = CropManager.FLUID_PIPE.getBoundingBox(CropManager.FLUID_PIPE.getDefaultState(), world, pos);
        assertEquals(0.0, box.minZ, 1e-6);
        assertEquals(0.6875, box.maxZ, 1e-6);
    }

    @Test
    public void fourPipeRingEqualizesWithoutInfiniteLoopAndGuardsReentrancy() throws Exception {
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World world = mockWorld(tiles, false);

        BlockPos p00 = new BlockPos(0, 64, 0);
        BlockPos p10 = new BlockPos(1, 64, 0);
        BlockPos p11 = new BlockPos(1, 64, 1);
        BlockPos p01 = new BlockPos(0, 64, 1);

        FluidPipeTile[] ring = {
                attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p00, CropManager.FLUID_PIPE.getDefaultState()),
                attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p10, CropManager.FLUID_PIPE.getDefaultState()),
                attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p11, CropManager.FLUID_PIPE.getDefaultState()),
                attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p01, CropManager.FLUID_PIPE.getDefaultState())
        };
        tiles.put(p00, ring[0]);
        tiles.put(p10, ring[1]);
        tiles.put(p11, ring[2]);
        tiles.put(p01, ring[3]);

        FluidTank t1 = new FluidTank(10000);
        t1.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        FluidTank t2 = new FluidTank(10000);
        tiles.put(p00.west(), tankTile(t1));
        tiles.put(p11.east(), tankTile(t2));

        for (int t = 0; t < 40; t++) {
            for (FluidPipeTile pipe : ring) pipe.update();
        }
        assertEquals(500, t1.getFluidAmount());
        assertEquals(500, t2.getFluidAmount());

        // Further ticking remains stable at 500 / 500 with conservation
        for (int t = 0; t < 40; t++) {
            for (FluidPipeTile pipe : ring) pipe.update();
        }
        assertEquals(500, t1.getFluidAmount());
        assertEquals(500, t2.getFluidAmount());

        // Re-entrancy guard (TRANSFERRING): re-entering pipe.fill() during fill() returns 0 without stack overflow
        AtomicInteger reentrantFillResult = new AtomicInteger(-1);
        FluidTank src = new FluidTank(10000);
        src.setCanFill(false);
        src.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        FluidTank reentrantDst = new FluidTank(10000) {
            @Override public int fill(FluidStack resource, boolean doFill) {
                int accepted = super.fill(resource, doFill);
                if (doFill && accepted > 0) {
                    IFluidHandler cap = ring[0].getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.NORTH);
                    reentrantFillResult.set(cap.fill(new FluidStack(FluidRegistry.WATER, 100), true));
                }
                return accepted;
            }
        };
        reentrantDst.setCanDrain(false);
        tiles.put(p00.west(), tankTile(src));
        tiles.put(p11.east(), tankTile(reentrantDst));
        for (int t = 0; t < 5; t++) ring[0].update();
        assertEquals(0, reentrantFillResult.get());
        assertEquals(750, src.getFluidAmount());
        assertEquals(250, reentrantDst.getFluidAmount());
    }

    @Test
    public void threeTankEqualizationConvergesRespectsDeadbandAndBypassesFullTank() throws Exception {
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World world = mockWorld(tiles, false);

        BlockPos p1Pos = new BlockPos(1, 64, 0);
        BlockPos p2Pos = new BlockPos(2, 64, 0);
        BlockPos p3Pos = new BlockPos(3, 64, 0);
        FluidPipeTile p1 = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p1Pos, CropManager.FLUID_PIPE.getDefaultState());
        FluidPipeTile p2 = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p2Pos, CropManager.FLUID_PIPE.getDefaultState());
        FluidPipeTile p3 = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), world, p3Pos, CropManager.FLUID_PIPE.getDefaultState());
        tiles.put(p1Pos, p1);
        tiles.put(p2Pos, p2);
        tiles.put(p3Pos, p3);

        FluidTank t1 = new FluidTank(10000);
        FluidTank t2 = new FluidTank(10000);
        FluidTank t3 = new FluidTank(10000);
        tiles.put(p1Pos.west(), tankTile(t1));
        tiles.put(p2Pos.south(), tankTile(t2));
        tiles.put(p3Pos.east(), tankTile(t3));

        // 3000 / 0 / 0 -> 1000 / 1000 / 1000
        t1.setFluid(new FluidStack(FluidRegistry.WATER, 3000));
        for (int t = 0; t < 100; t++) {
            p1.update();
            p2.update();
            p3.update();
        }
        assertEquals(1000, t1.getFluidAmount());
        assertEquals(1000, t2.getFluidAmount());
        assertEquals(1000, t3.getFluidAmount());

        // <= 1 mB deadband holds at (1001, 1000, 1000)
        t1.setFluid(new FluidStack(FluidRegistry.WATER, 1001));
        t2.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        t3.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        for (int t = 0; t < 20; t++) {
            p1.update();
            p2.update();
            p3.update();
        }
        assertEquals(1001, t1.getFluidAmount());
        assertEquals(1000, t2.getFluidAmount());
        assertEquals(1000, t3.getFluidAmount());

        // Difference of 2 mB (1002, 1000, 1000) moves 1 mB and stops within <= 1 mB deadband
        t1.setFluid(new FluidStack(FluidRegistry.WATER, 1002));
        for (int t = 0; t < 20; t++) {
            p1.update();
            p2.update();
            p3.update();
        }
        assertEquals(3002, t1.getFluidAmount() + t2.getFluidAmount() + t3.getFluidAmount());
        assertEquals(1001, t1.getFluidAmount());
        int min = Math.min(t1.getFluidAmount(), Math.min(t2.getFluidAmount(), t3.getFluidAmount()));
        int max = Math.max(t1.getFluidAmount(), Math.max(t2.getFluidAmount(), t3.getFluidAmount()));
        assertTrue("Levels must stabilize within 1 mB deadband", max - min <= 1);

        // Full tank bypass: T2 full at 200 mB does not block T1 from delivering to T3
        FluidTank smallFull = new FluidTank(200);
        smallFull.setFluid(new FluidStack(FluidRegistry.WATER, 200));
        tiles.put(p2Pos.south(), tankTile(smallFull));
        t1.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        t3.setFluid(null);
        for (int t = 0; t < 20; t++) {
            p1.update();
            p2.update();
            p3.update();
        }
        assertEquals(200, smallFull.getFluidAmount());
        assertEquals(500, t1.getFluidAmount());
        assertEquals(500, t3.getFluidAmount());
    }

    @Test
    public void restrictorGatesFlowByRedstoneAndRouterDirectsOutputOnly() throws Exception {
        Map<BlockPos, TileEntity> tiles = new HashMap<>();
        World world = mockWorld(tiles, false);
        final boolean[] powered = {false};
        Mockito.when(world.isBlockPowered(Mockito.any(BlockPos.class))).thenAnswer(inv -> powered[0]);

        BlockPos rPos = new BlockPos(1, 64, 0);
        FluidPipeBlock restrictorBlock = FluidPipeBlock.restrictor();
        IBlockState unpoweredState = restrictorBlock.getDefaultState().withProperty(FluidPipeBlock.POWERED, false);
        FluidPipeTile restrictor = attach(new FluidPipeTile(FluidPipeBlock.Kind.RESTRICTOR), world, rPos, unpoweredState);
        tiles.put(rPos, restrictor);

        FluidTank src = new FluidTank(4000);
        src.setCanFill(false);
        src.setFluid(new FluidStack(FluidRegistry.WATER, 1000));
        FluidTank dst = new FluidTank(4000);
        dst.setCanDrain(false);
        tiles.put(rPos.west(), tankTile(src));
        tiles.put(rPos.east(), tankTile(dst));

        // Unpowered restrictor blocks auto-pull and external fill
        assertFalse(restrictor.allowsInputFrom(EnumFacing.WEST));
        for (int t = 0; t < 10; t++) restrictor.update();
        assertEquals(1000, src.getFluidAmount());
        assertEquals(0, dst.getFluidAmount());
        assertEquals(0, restrictor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.WEST)
                .fill(new FluidStack(FluidRegistry.WATER, 100), true));

        // neighborChanged updates powered state and blockstate; powered restrictor allows transfer
        powered[0] = true;
        restrictorBlock.neighborChanged(unpoweredState, world, rPos, Blocks.REDSTONE_BLOCK, rPos.north());
        assertTrue(restrictor.powered);
        Mockito.verify(world).setBlockState(rPos,
                restrictorBlock.getDefaultState().withProperty(FluidPipeBlock.POWERED, true), 3);
        Mockito.when(world.getBlockState(rPos)).thenReturn(
                restrictorBlock.getDefaultState().withProperty(FluidPipeBlock.POWERED, true));
        for (int t = 0; t < 5; t++) restrictor.update();
        assertEquals(750, src.getFluidAmount());
        assertEquals(250, dst.getFluidAmount());

        // Router with outputSide = EAST refuses input from EAST and pushes only to EAST (even when dst > src)
        BlockPos routerPos = new BlockPos(10, 64, 10);
        FluidPipeBlock routerBlock = FluidPipeBlock.router();
        IBlockState routerState = routerBlock.getDefaultState().withProperty(FluidPipeBlock.FACING, EnumFacing.EAST);
        FluidPipeTile router = attach(new FluidPipeTile(FluidPipeBlock.Kind.ROUTER), world, routerPos, routerState);
        tiles.put(routerPos, router);

        assertFalse(router.allowsInputFrom(EnumFacing.EAST));
        assertTrue(router.allowsInputFrom(EnumFacing.WEST));
        FluidTank rSrc = new FluidTank(4000);
        rSrc.setFluid(new FluidStack(FluidRegistry.WATER, 500));
        FluidTank rEastDst = new FluidTank(4000);
        rEastDst.setFluid(new FluidStack(FluidRegistry.WATER, 600));
        FluidTank rSouthDst = new FluidTank(4000);
        rSouthDst.setCanDrain(false);
        tiles.put(routerPos.west(), tankTile(rSrc));
        tiles.put(routerPos.east(), tankTile(rEastDst));
        tiles.put(routerPos.south(), tankTile(rSouthDst));

        for (int t = 0; t < 5; t++) router.update();
        assertEquals(250, rSrc.getFluidAmount());
        assertEquals(850, rEastDst.getFluidAmount());
        assertEquals("Router must not push to non-output faces", 0, rSouthDst.getFluidAmount());
    }

    @Test
    public void fluidPipeRespectsIndustrialTankRedstoneValveAndReservoirReserve() throws Exception {
        // 1. IndustrialTank redstone valve: HIGH mode blocks OUTPUT pipe pull when unpowered, INPUT pipe still fills
        Map<BlockPos, TileEntity> tankTiles = new HashMap<>();
        World tankWorld = mockWorld(tankTiles, false);
        final boolean[] tankPowered = {false};
        Mockito.when(tankWorld.isBlockPowered(Mockito.any(BlockPos.class))).thenAnswer(inv -> tankPowered[0]);
        Map<BlockPos, IBlockState> tankStates = new HashMap<>();
        Mockito.when(tankWorld.getBlockState(Mockito.any(BlockPos.class))).thenAnswer(inv ->
                tankStates.getOrDefault(inv.getArguments()[0], Blocks.AIR.getDefaultState()));

        BlockPos tankCenter = new BlockPos(0, 64, 0);
        BlockPos tankCtrlPos = tankCenter.north().up();
        IndustrialTankTile tankCtrl = new IndustrialTankTile();
        tankCtrl.setWorld(tankWorld);
        tankCtrl.setPos(tankCtrlPos);
        tankTiles.put(tankCtrlPos, tankCtrl);
        tankStates.put(tankCtrlPos, CropManager.FLUID_TANK.getDefaultState().withProperty(IndustrialTankBlock.FACING, EnumFacing.NORTH));
        for (int y = 0; y < IndustrialTankTile.FIXED_HEIGHT; y++) {
            boolean cap = y == 0 || y == IndustrialTankTile.FIXED_HEIGHT - 1;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!cap && dx == 0 && dz == 0) continue;
                    BlockPos p = new BlockPos(tankCenter.getX() + dx, tankCenter.getY() + y, tankCenter.getZ() + dz);
                    if (!p.equals(tankCtrlPos)) {
                        StorageCasingTile ct = new StorageCasingTile();
                        ct.setWorld(tankWorld);
                        ct.setPos(p);
                        tankTiles.put(p, ct);
                        tankStates.put(p, CropManager.FLUID_TANK_CASING.getDefaultState());
                    }
                }
            }
        }
        tankCtrl.tryForm();
        assertTrue(tankCtrl.formed);

        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        player.world = tankWorld;
        Mockito.when(player.getDistanceSq(tankCtrlPos)).thenReturn(4.0);
        assertTrue(tankCtrl.control(1, player)); // redstoneMode = 1 (HIGH)
        tankCtrl.tank.fill(new FluidStack(FluidRegistry.WATER, 5000), true);

        // INPUT pipe pushing into top UP casing + OUTPUT pipe pulling from bottom DOWN casing
        BlockPos inPipePos = tankCenter.up(5);
        BlockPos inSrcPos = inPipePos.up();
        FluidTank inSrc = new FluidTank(10000);
        inSrc.setCanFill(false);
        inSrc.setFluid(new FluidStack(FluidRegistry.WATER, 2000));
        tankTiles.put(inSrcPos, tankTile(inSrc));
        FluidPipeTile inPipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), tankWorld, inPipePos, CropManager.FLUID_PIPE.getDefaultState());
        tankTiles.put(inPipePos, inPipe);
        tankStates.put(inPipePos, CropManager.FLUID_PIPE.getDefaultState());

        BlockPos outPipePos = tankCenter.down();
        BlockPos outDstPos = outPipePos.down();
        FluidTank outDst = new FluidTank(10000);
        outDst.setCanDrain(false);
        tankTiles.put(outDstPos, tankTile(outDst));
        FluidPipeTile outPipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), tankWorld, outPipePos, CropManager.FLUID_PIPE.getDefaultState());
        tankTiles.put(outPipePos, outPipe);
        tankStates.put(outPipePos, CropManager.FLUID_PIPE.getDefaultState());

        // Unpowered: OUTPUT pipe cannot drain tank, while INPUT pipe fills 250 mB
        for (int t = 0; t < 5; t++) {
            outPipe.update();
            inPipe.update();
        }
        assertEquals(0, outDst.getFluidAmount());
        assertEquals(5250, tankCtrl.tank.getFluidAmount());

        // Powered: OUTPUT pipe drains 250 mB per 5-tick pulse
        tankPowered[0] = true;
        for (int t = 0; t < 5; t++) outPipe.update();
        assertEquals(250, outDst.getFluidAmount());
        assertEquals(5000, tankCtrl.tank.getFluidAmount());

        // 2. AgriculturalReservoir RESERVE (25%, 50%, 75%) stops OUTPUT pipe at 8000 / 16000 / 24000 mB while INPUT pipe stays open
        Map<BlockPos, TileEntity> resTiles = new HashMap<>();
        World resWorld = mockWorld(resTiles, false);
        Map<BlockPos, IBlockState> resStates = new HashMap<>();
        Mockito.when(resWorld.getBlockState(Mockito.any(BlockPos.class))).thenAnswer(inv ->
                resStates.getOrDefault(inv.getArguments()[0], Blocks.AIR.getDefaultState()));

        BlockPos resCenter = new BlockPos(0, 64, 0);
        BlockPos resCtrlPos = resCenter.north().up();
        AgriculturalReservoirTile resCtrl = new AgriculturalReservoirTile();
        resCtrl.setWorld(resWorld);
        resCtrl.setPos(resCtrlPos);
        resTiles.put(resCtrlPos, resCtrl);
        resStates.put(resCtrlPos, CropManager.AGRICULTURAL_RESERVOIR.getDefaultState()
                .withProperty(AgriculturalReservoirBlock.FACING, EnumFacing.NORTH));
        for (int y = 0; y < AgriculturalReservoirTile.FIXED_HEIGHT; y++) {
            boolean cap = y == 0 || y == AgriculturalReservoirTile.FIXED_HEIGHT - 1;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!cap && dx == 0 && dz == 0) continue;
                    BlockPos p = new BlockPos(resCenter.getX() + dx, resCenter.getY() + y, resCenter.getZ() + dz);
                    if (!p.equals(resCtrlPos)) {
                        ReservoirCasingTile ct = new ReservoirCasingTile();
                        ct.setWorld(resWorld);
                        ct.setPos(p);
                        resTiles.put(p, ct);
                        resStates.put(p, CropManager.RESERVOIR_CASING.getDefaultState());
                    }
                }
            }
        }
        resCtrl.tryForm();
        assertTrue(resCtrl.formed);

        EntityPlayer resPlayer = Mockito.mock(EntityPlayer.class);
        resPlayer.world = resWorld;
        Mockito.when(resPlayer.getDistanceSq(resCtrlPos)).thenReturn(4.0);
        resCtrl.inventory.setStackInSlot(AgriculturalReservoirTile.FIRST_MODULE_SLOT,
                new ItemStack(CropManager.RESERVOIR_RESERVE, 1));

        BlockPos resOutPipePos = resCenter.south().up().south();
        BlockPos resOutDstPos = resOutPipePos.south();
        FluidTank resOutDst = new FluidTank(64000);
        resOutDst.setCanDrain(false);
        resTiles.put(resOutDstPos, tankTile(resOutDst));
        FluidPipeTile resOutPipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), resWorld, resOutPipePos, CropManager.FLUID_PIPE.getDefaultState());
        resTiles.put(resOutPipePos, resOutPipe);
        resStates.put(resOutPipePos, CropManager.FLUID_PIPE.getDefaultState());

        BlockPos resInPipePos = resCenter.north().up(2).north();
        BlockPos resInSrcPos = resInPipePos.north();
        FluidTank resInSrc = new FluidTank(64000);
        resInSrc.setCanFill(false);
        resInSrc.setFluid(new FluidStack(FluidRegistry.WATER, 4000));
        resTiles.put(resInSrcPos, tankTile(resInSrc));
        FluidPipeTile resInPipe = attach(new FluidPipeTile(FluidPipeBlock.Kind.PIPE), resWorld, resInPipePos, CropManager.FLUID_PIPE.getDefaultState());
        resTiles.put(resInPipePos, resInPipe);
        resStates.put(resInPipePos, CropManager.FLUID_PIPE.getDefaultState());

        int[] expectedReserves = {8000, 16000, 24000};
        for (int step = 1; step <= 3; step++) {
            assertTrue(resCtrl.control(40, resPlayer));
            resCtrl.water.setFluid(new FluidStack(FluidRegistry.WATER, 30000));
            resOutDst.setFluid(null);
            for (int t = 0; t < 500; t++) resOutPipe.update();
            assertEquals(expectedReserves[step - 1], resCtrl.water.getFluidAmount());
            assertEquals(30000 - expectedReserves[step - 1], resOutDst.getFluidAmount());

            // INPUT pipe remains unrestricted even at reserve threshold
            for (int t = 0; t < 5; t++) resInPipe.update();
            assertEquals(expectedReserves[step - 1] + 250, resCtrl.water.getFluidAmount());
        }
    }

    static World mockWorld(Map<BlockPos, TileEntity> tiles, boolean remote) throws Exception {
        World world = Mockito.mock(World.class);
        Field rf = World.class.getDeclaredField("isRemote");
        rf.setAccessible(true);
        rf.set(world, remote);
        Mockito.when(world.getHeight()).thenReturn(256);
        Mockito.when(world.isBlockLoaded(Mockito.any(BlockPos.class), Mockito.eq(false))).thenAnswer(inv -> {
            BlockPos p = (BlockPos) inv.getArguments()[0];
            return p != null && p.getY() >= 0 && p.getY() < 256;
        });
        Mockito.when(world.getTileEntity(Mockito.any(BlockPos.class))).thenAnswer(inv -> tiles.get(inv.getArguments()[0]));
        return world;
    }

    static <T extends TileEntity> T attach(T tile, World world, BlockPos pos, IBlockState state) {
        tile.setWorld(world);
        tile.setPos(pos);
        Mockito.when(world.getBlockState(pos)).thenReturn(state);
        return tile;
    }

    static TileEntity tankTile(final FluidTank tank) {
        return new TileEntity() {
            @Override
            public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
                return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(cap, facing);
            }
            @Override
            public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
                if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                    return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
                }
                return super.getCapability(cap, facing);
            }
        };
    }
}
