package br.cropmanager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
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
