package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public final class SprinklerTest {
    @BeforeClass
    public static void boot() {
        Bootstrap.register();
        TestCapabilityHelper.ensureFluidCapabilities();
        TileEntity.register("cropmanager:sprinkler", SprinklerTile.class);
    }

    @Test
    public void tiersExposeExpectedAreasSpeedsAndCapacities() {
        SprinklerTile mk1 = new SprinklerTile(1);
        SprinklerTile mk2 = new SprinklerTile(2);
        SprinklerTile mk3 = new SprinklerTile(3);

        assertEquals(4, mk1.radius());
        assertEquals(1, mk1.speedMultiplier());
        assertEquals(4000, mk1.water.getCapacity());

        assertEquals(4, mk2.radius());
        assertEquals(4, mk2.speedMultiplier());
        assertEquals(8000, mk2.water.getCapacity());

        assertEquals(8, mk3.radius());
        assertEquals(4, mk3.speedMultiplier());
        assertEquals(16000, mk3.water.getCapacity());
    }

    @Test
    public void hydratesCropAndAddsGrowthPoints() {
        TileEntityCrop crop = Mockito.mock(TileEntityCrop.class);
        int[] state = {30, 10};
        Mockito.when(crop.getStorageWater()).thenAnswer(inv -> state[0]);
        Mockito.doAnswer(inv -> { state[0] = (Integer) inv.getArguments()[0]; return null; }).when(crop).setStorageWater(Mockito.anyInt());
        Mockito.when(crop.getGrowthPoints()).thenAnswer(inv -> state[1]);
        Mockito.doAnswer(inv -> { state[1] = (Integer) inv.getArguments()[0]; return null; }).when(crop).setGrowthPoints(Mockito.anyInt());
        Mockito.when(crop.getCrop()).thenReturn(br.cropmanager.crop.CropRegistry.BIOREED);
        Mockito.when(crop.getCurrentSize()).thenReturn(2);

        SprinklerTile.hydrateCrop(crop, 25, 60);
        assertEquals(55, state[0]);
        assertEquals(70, state[1]);
    }

    @Test
    public void persistsTierWaterAndCooldownInNbt() {
        SprinklerTile original = new SprinklerTile(3);
        original.water.fill(new FluidStack(FluidRegistry.WATER, 5500), true);
        original.cooldown = 12;

        NBTTagCompound tag = original.writeToNBT(new NBTTagCompound());
        SprinklerTile loaded = new SprinklerTile();
        loaded.readFromNBT(tag);

        assertEquals(3, loaded.tier);
        assertEquals(16000, loaded.water.getCapacity());
        assertEquals(5500, loaded.water.getFluidAmount());
        assertEquals(12, loaded.cooldown);
        assertEquals(2, loaded.redstoneMode);
    }

    @Test
    public void redstoneControlDisablesOnSignalByDefaultAndCyclesModes() throws Exception {
        // Explicit truth table
        assertTrue(SprinklerTile.redstoneAllows(0, false));
        assertTrue(SprinklerTile.redstoneAllows(0, true));
        assertFalse(SprinklerTile.redstoneAllows(1, false));
        assertTrue(SprinklerTile.redstoneAllows(1, true));
        assertTrue(SprinklerTile.redstoneAllows(2, false));
        assertFalse(SprinklerTile.redstoneAllows(2, true));

        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(10, 64, 10);
        net.minecraft.world.World world = Mockito.mock(net.minecraft.world.World.class);
        Mockito.when(world.getHeight()).thenReturn(256);
        Mockito.when(world.isBlockLoaded(Mockito.any(net.minecraft.util.math.BlockPos.class), Mockito.eq(false))).thenReturn(true);
        boolean[] powered = {false};
        net.minecraft.block.state.IBlockState[] blockState = {
                CropManager.SPRINKLER_MK1.getDefaultState().withProperty(SprinklerBlock.ACTIVE, false)
        };
        Mockito.when(world.isBlockPowered(pos)).thenAnswer(inv -> powered[0]);
        Mockito.when(world.getBlockState(Mockito.any(net.minecraft.util.math.BlockPos.class)))
                .thenAnswer(inv -> pos.equals(inv.getArguments()[0]) ? blockState[0] : net.minecraft.init.Blocks.AIR.getDefaultState());
        Mockito.when(world.setBlockState(Mockito.eq(pos), Mockito.any(), Mockito.anyInt())).thenAnswer(inv -> {
            blockState[0] = (net.minecraft.block.state.IBlockState) inv.getArguments()[1];
            return true;
        });

        SprinklerTile tile = new SprinklerTile(1);
        tile.setWorld(world);
        tile.setPos(pos);
        Mockito.when(world.getTileEntity(pos)).thenReturn(tile);
        tile.water.fill(new FluidStack(FluidRegistry.WATER, 1000), true);

        // Default mode (2): unpowered runs and sets ACTIVE=true
        tile.update();
        assertTrue(blockState[0].getValue(SprinklerBlock.ACTIVE));
        assertEquals(1, tile.cooldown);

        // Powering with redstone immediately pauses sprinkler and sets ACTIVE=false without consuming water
        powered[0] = true;
        int waterBefore = tile.water.getFluidAmount();
        for (int i = 0; i < 100; i++) tile.update();
        assertFalse(blockState[0].getValue(SprinklerBlock.ACTIVE));
        assertEquals(waterBefore, tile.water.getFluidAmount());

        // Sneak + right-click cycles mode: 2 -> 0 (always) -> 1 (with signal) -> 2 (without signal)
        net.minecraft.entity.player.EntityPlayer player = Mockito.mock(net.minecraft.entity.player.EntityPlayer.class);
        player.capabilities = new net.minecraft.entity.player.PlayerCapabilities();
        Mockito.when(player.getHeldItem(net.minecraft.util.EnumHand.MAIN_HAND)).thenReturn(net.minecraft.item.ItemStack.EMPTY);
        Mockito.when(player.isSneaking()).thenReturn(true);
        Mockito.when(player.canPlayerEdit(Mockito.eq(pos), Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(world.isBlockModifiable(player, pos)).thenReturn(false);

        assertFalse(CropManager.SPRINKLER_MK1.onBlockActivated(world, pos, blockState[0], player,
                net.minecraft.util.EnumHand.MAIN_HAND, net.minecraft.util.EnumFacing.UP, 0.5F, 0.5F, 0.5F));
        assertEquals("Protected sprinkler retains its redstone mode", 2, tile.redstoneMode);

        Mockito.when(world.isBlockModifiable(player, pos)).thenReturn(true);
        CropManager.SPRINKLER_MK1.onBlockActivated(world, pos, blockState[0], player,
                net.minecraft.util.EnumHand.MAIN_HAND, net.minecraft.util.EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(0, tile.redstoneMode);
        assertTrue("Mode 0 (always) activates even while powered", blockState[0].getValue(SprinklerBlock.ACTIVE));

        CropManager.SPRINKLER_MK1.onBlockActivated(world, pos, blockState[0], player,
                net.minecraft.util.EnumHand.MAIN_HAND, net.minecraft.util.EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(1, tile.redstoneMode);
        assertTrue("Mode 1 (with signal) stays active while powered", blockState[0].getValue(SprinklerBlock.ACTIVE));
        powered[0] = false;
        CropManager.SPRINKLER_MK1.neighborChanged(blockState[0], world, pos, net.minecraft.init.Blocks.LEVER, pos.north());
        assertFalse("Mode 1 deactivates when signal turns off", blockState[0].getValue(SprinklerBlock.ACTIVE));

        // Client side guard, NBT clamping, shouldRefresh & capability checks (B2-B5)
        java.lang.reflect.Field remoteField = net.minecraft.world.World.class.getDeclaredField("isRemote");
        remoteField.setAccessible(true);
        remoteField.set(world, true);
        tile.redstoneMode = 0;
        int prevCooldown = tile.cooldown;
        tile.update();
        assertEquals(prevCooldown, tile.cooldown);

        NBTTagCompound corrupt = new NBTTagCompound();
        corrupt.setInteger("Tier", 99);
        corrupt.setInteger("Cooldown", -20);
        corrupt.setInteger("RedstoneMode", 42);
        SprinklerTile clamped = new SprinklerTile();
        clamped.readFromNBT(corrupt);
        assertEquals(3, clamped.tier);
        assertEquals(0, clamped.cooldown);
        assertEquals(2, clamped.redstoneMode);

        assertFalse(clamped.shouldRefresh(world, pos,
                CropManager.SPRINKLER_MK1.getStateFromMeta(0),
                CropManager.SPRINKLER_MK1.getStateFromMeta(1)));
        for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.values()) {
            assertTrue(clamped.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f));
        }
        assertTrue(clamped.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null));
        net.minecraftforge.fluids.capability.IFluidHandler fh = clamped.getCapability(
                net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        assertEquals(0, fh.fill(new FluidStack(FluidRegistry.LAVA, 1000), true));
        assertNull(fh.drain(1000, true));
    }

    @Test
    public void waterBucketRequiresBothPlayerAndWorldPermission() {
        BlockPos pos = new BlockPos(10, 64, 10);
        World world = Mockito.mock(World.class);
        SprinklerTile sprinkler = new SprinklerTile(1);
        sprinkler.setWorld(world);
        sprinkler.setPos(pos);
        Mockito.when(world.getTileEntity(pos)).thenReturn(sprinkler);
        SprinklerBlock block = CropManager.SPRINKLER_MK1;
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        player.world = world;
        player.capabilities = new PlayerCapabilities();
        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
        Mockito.when(player.getHeldItem(EnumHand.MAIN_HAND)).thenReturn(waterBucket);

        Mockito.when(world.isBlockModifiable(player, pos)).thenReturn(true);
        Mockito.when(player.canPlayerEdit(pos, EnumFacing.UP, waterBucket)).thenReturn(false);
        block.onBlockActivated(world, pos, block.getDefaultState(), player, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(0, sprinkler.water.getFluidAmount());

        Mockito.when(world.isBlockModifiable(player, pos)).thenReturn(false);
        Mockito.when(player.canPlayerEdit(pos, EnumFacing.UP, waterBucket)).thenReturn(true);
        block.onBlockActivated(world, pos, block.getDefaultState(), player, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(0, sprinkler.water.getFluidAmount());
        assertEquals(1, waterBucket.getCount());
        Mockito.verify(player, Mockito.never()).setHeldItem(Mockito.any(), Mockito.any());

        Mockito.when(world.isBlockModifiable(player, pos)).thenReturn(true);
        assertTrue(block.onBlockActivated(world, pos, block.getDefaultState(), player, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F));
        assertEquals(1000, sprinkler.water.getFluidAmount());
        Mockito.verify(player).setHeldItem(Mockito.eq(EnumHand.MAIN_HAND),
                Mockito.argThat(stack -> stack.getItem() == Items.BUCKET && stack.getCount() == 1));
    }

    @Test public void hydrateCropSkipsDirtyWhenMatureAndHydrated() {
        ic2.core.crop.TileEntityCrop crop = Mockito.mock(ic2.core.crop.TileEntityCrop.class);
        ic2.api.crops.CropCard card = Mockito.mock(ic2.api.crops.CropCard.class);
        Mockito.when(card.getMaxSize()).thenReturn(4);
        Mockito.when(crop.getCrop()).thenReturn(card);
        Mockito.when(crop.getCurrentSize()).thenReturn(4);
        Mockito.when(crop.getStorageWater()).thenReturn(100);

        assertFalse(SprinklerTile.hydrateCrop(crop, 25, 15));
        assertFalse(crop.dirty);
        Mockito.verify(crop, Mockito.never()).setStorageWater(Mockito.anyInt());
        Mockito.verify(crop, Mockito.never()).setGrowthPoints(Mockito.anyInt());
    }
}
