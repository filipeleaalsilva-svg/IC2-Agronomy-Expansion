package br.cropmanager;

import ic2.api.tile.IEnergyStorage;
import java.util.Collections;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public class EuDishAntennaTest {
    @org.junit.BeforeClass
    public static void boot() {
        Bootstrap.register();
        TestCapabilityHelper.ensureFluidCapabilities();
    }

    private static abstract class StorageTile extends TileEntity implements IEnergyStorage { }

    @Test
    public void radiusScalesWithWirelessRangeUpgrades() {
        EuDishAntennaTile dish = new EuDishAntennaTile();
        assertEquals(ManagerConfig.dishBaseRadius, dish.radius());
        dish.inventory.setStackInSlot(0, new ItemStack(CropManager.WIRELESS_RANGE));
        dish.inventory.setStackInSlot(1, new ItemStack(CropManager.WIRELESS_RANGE));
        assertEquals(ManagerConfig.dishBaseRadius + 2 * ManagerConfig.dishRadiusPerUpgrade, dish.radius());
    }

    @Test
    public void pullsFromAdjacentStorageAndPowersLinkedMachineInRange() throws Exception {
        World world = Mockito.mock(World.class);
        WorldProvider provider = Mockito.mock(WorldProvider.class);
        Mockito.when(provider.getDimension()).thenReturn(0);
        java.lang.reflect.Field pf = World.class.getDeclaredField("provider");
        pf.setAccessible(true);
        pf.set(world, provider);
        Mockito.when(world.getHeight()).thenReturn(256);
        Mockito.when(world.isBlockLoaded(Mockito.any(BlockPos.class), Mockito.eq(false))).thenReturn(true);

        BlockPos dishPos = new BlockPos(8, 64, 8);
        BlockPos storagePos = dishPos.north();
        BlockPos machinePos = new BlockPos(14, 64, 8); // 6 blocks away (within 16 radius)

        StorageTile storage = Mockito.mock(StorageTile.class);
        Mockito.when(storage.getStored()).thenReturn(1000);
        Mockito.when(world.getTileEntity(storagePos)).thenReturn(storage);

        ComposterTile composter = new ComposterTile();
        composter.setWorld(world);
        composter.setPos(machinePos);
        ItemStack linkedUpgrade = new ItemStack(CropManager.WIRELESS_EU);
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound dishTag = new NBTTagCompound();
        dishTag.setInteger("Dim", 0);
        dishTag.setInteger("X", dishPos.getX());
        dishTag.setInteger("Y", dishPos.getY());
        dishTag.setInteger("Z", dishPos.getZ());
        root.setTag(WirelessEuUpgrade.DISH_TAG, dishTag);
        linkedUpgrade.setTagCompound(root);
        composter.inventory.setStackInSlot(2, linkedUpgrade);
        Mockito.when(world.getTileEntity(machinePos)).thenReturn(composter);

        IChunkProvider chunkProvider = Mockito.mock(IChunkProvider.class);
        Chunk chunk = Mockito.mock(Chunk.class);
        Mockito.when(world.getChunkProvider()).thenReturn(chunkProvider);
        Mockito.when(chunkProvider.getLoadedChunk(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        Mockito.when(chunkProvider.getLoadedChunk(0, 0)).thenReturn(chunk);
        Mockito.when(chunk.getTileEntityMap()).thenReturn(Collections.singletonMap(machinePos, composter));

        EuDishAntennaTile dish = new EuDishAntennaTile();
        dish.setWorld(world);
        dish.setPos(dishPos);
        dish.update();

        Mockito.verify(storage).addEnergy(Mockito.anyInt());
        assertTrue("Linked machine inside dish radius should receive EU", composter.getEnergy() > 0);
        composter.energySink().setEnergyStored(0);
        composter.inventory.setStackInSlot(2, new ItemStack(CropManager.WIRELESS_EU));
        dish.update();
        assertTrue("Unlinked machine inside dish radius should receive EU", composter.getEnergy() > 0);
    }

    @Test
    public void onlyUnboundUpgradesOrUpgradesLinkedToThisDishAcceptIt() {
        ItemStack upgrade = new ItemStack(CropManager.WIRELESS_EU);
        assertTrue(WirelessEuUpgrade.acceptsDish(upgrade, 0, BlockPos.ORIGIN));
        NBTTagCompound tag = new NBTTagCompound();
        upgrade.setTagCompound(tag);
        tag.setString("display_test", "custom");
        assertTrue(WirelessEuUpgrade.acceptsDish(upgrade, 0, BlockPos.ORIGIN));
        tag.setInteger(WirelessEuUpgrade.WIFI_CHANNEL_TAG, 7);
        assertFalse(WirelessEuUpgrade.acceptsDish(upgrade, 0, BlockPos.ORIGIN));
        tag.removeTag(WirelessEuUpgrade.WIFI_CHANNEL_TAG);
        tag.setTag(WirelessEuUpgrade.LINK_TAG, new NBTTagCompound());
        assertFalse(WirelessEuUpgrade.acceptsDish(upgrade, 0, BlockPos.ORIGIN));
        tag.removeTag(WirelessEuUpgrade.LINK_TAG);
        NBTTagCompound dish = new NBTTagCompound();
        dish.setInteger("Dim", 1);
        dish.setInteger("X", 2);
        tag.setTag(WirelessEuUpgrade.DISH_TAG, dish);
        assertTrue(WirelessEuUpgrade.acceptsDish(upgrade, 1, new BlockPos(2, 0, 0)));
        assertFalse(WirelessEuUpgrade.acceptsDish(upgrade, 0, new BlockPos(2, 0, 0)));
        assertFalse(WirelessEuUpgrade.acceptsDish(upgrade, 1, BlockPos.ORIGIN));
    }

    @Test
    public void shiftRightClickLinksUpgradeToDishAndRedstonePausesBroadcast() throws Exception {
        World world = Mockito.mock(World.class);
        WorldProvider provider = Mockito.mock(WorldProvider.class);
        Mockito.when(provider.getDimension()).thenReturn(0);
        java.lang.reflect.Field pf = World.class.getDeclaredField("provider");
        pf.setAccessible(true);
        pf.set(world, provider);
        Mockito.when(world.isBlockLoaded(Mockito.any(BlockPos.class), Mockito.eq(false))).thenReturn(true);
        Mockito.when(world.isBlockPowered(BlockPos.ORIGIN)).thenReturn(true);

        EuDishAntennaTile dish = new EuDishAntennaTile();
        dish.setWorld(world);
        dish.setPos(BlockPos.ORIGIN);
        dish.energy.setEnergyStored(1000.0);
        Mockito.when(world.getTileEntity(BlockPos.ORIGIN)).thenReturn(dish);

        net.minecraft.entity.player.EntityPlayer player = Mockito.mock(net.minecraft.entity.player.EntityPlayer.class);
        Mockito.when(player.isSneaking()).thenReturn(true);
        ItemStack upgrade = new ItemStack(CropManager.WIRELESS_EU);
        Mockito.when(player.getHeldItem(net.minecraft.util.EnumHand.MAIN_HAND)).thenReturn(upgrade);

        assertEquals(net.minecraft.util.EnumActionResult.SUCCESS,
                CropManager.WIRELESS_EU.onItemUseFirst(player, world, BlockPos.ORIGIN,
                        net.minecraft.util.EnumFacing.UP, 0.5F, 0.5F, 0.5F, net.minecraft.util.EnumHand.MAIN_HAND));
        assertTrue(WirelessEuUpgrade.isLinkedToDish(upgrade, 0, BlockPos.ORIGIN));

        assertTrue(dish.control(1, player)); // mode 1 = active WITH signal
        assertTrue(dish.redstoneAllows());
        assertTrue(dish.control(1, player)); // mode 2 = active WITHOUT signal
        assertFalse(dish.redstoneAllows());
        dish.update();
        assertEquals(0.0, dish.lastSentRate, 1e-6);
    }

    @Test
    public void openMachineResolvesEuDishContainer() {
        EuDishAntennaTile dish = new EuDishAntennaTile();
        net.minecraft.entity.player.EntityPlayer player = Mockito.mock(net.minecraft.entity.player.EntityPlayer.class);
        player.inventory = new net.minecraft.entity.player.InventoryPlayer(player);
        player.openContainer = new EuDishContainer(player.inventory, dish);
        assertSame(dish, FarmTile.openMachine(player));
    }
}
