package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

/** Archetype B gates for the passive Pollinator Hive. */
public final class PollinatorHiveTest {
    private static final BlockPos HIVE_POS = new BlockPos(10, 64, 10);
    private CropCard previousWeed;
    private CropCard weed;
    private int previousRadius;
    private int previousInterval;
    private int previousChargesPerPowder;
    private int previousMaxCharges;

    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
        if (TileEntity.getKey(PollinatorHiveTile.class) == null) {
            TileEntity.register("cropmanager:pollinator_hive", PollinatorHiveTile.class);
        }
    }

    @Before public void setUp() {
        previousWeed = Crops.weed;
        previousRadius = ManagerConfig.pollinatorRadius;
        previousInterval = ManagerConfig.pollinatorBaseInterval;
        previousChargesPerPowder = ManagerConfig.pollinatorChargesPerPowder;
        previousMaxCharges = ManagerConfig.pollinatorMaxCharges;
        ManagerConfig.pollinatorRadius = 2;
        ManagerConfig.pollinatorBaseInterval = 100;
        ManagerConfig.pollinatorChargesPerPowder = 16;
        ManagerConfig.pollinatorMaxCharges = 64;
        weed = Mockito.mock(CropCard.class);
        Crops.weed = weed;
    }

    @After public void tearDown() {
        Crops.weed = previousWeed;
        ManagerConfig.pollinatorRadius = previousRadius;
        ManagerConfig.pollinatorBaseInterval = previousInterval;
        ManagerConfig.pollinatorChargesPerPowder = previousChargesPerPowder;
        ManagerConfig.pollinatorMaxCharges = previousMaxCharges;
    }

    @Test public void passivePulseRunsOncePerBaseInterval() throws Exception {
        World world = mockWorld(false);
        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), world, HIVE_POS);
        for (int i = 0; i < 99; i++) hive.update();
        Mockito.verify(world, Mockito.never()).getTileEntity(Mockito.any(BlockPos.class));
        Mockito.verify(world, Mockito.times(4)).markChunkDirty(HIVE_POS, hive);
        assertEquals(99, hive.writeToNBT(new NBTTagCompound()).getInteger("Cooldown"));
        hive.update();
        Mockito.verify(world, Mockito.times(75)).getTileEntity(Mockito.any(BlockPos.class));
        Mockito.verify(world, Mockito.times(5)).markChunkDirty(HIVE_POS, hive);
    }

    @Test public void chargedModeRunsThreePulsesPerHundredTicksWithoutTargetCost() throws Exception {
        World world = mockWorld(false);
        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), world, HIVE_POS);
        assertTrue(hive.addPowderCharge());
        Mockito.clearInvocations(world);
        for (int i = 0; i < 100; i++) hive.update();
        Mockito.verify(world, Mockito.times(225)).getTileEntity(Mockito.any(BlockPos.class));
        Mockito.verify(world, Mockito.times(6)).markChunkDirty(HIVE_POS, hive);
        assertEquals("No valid target means no powder charge is spent", 16, hive.getCharges());
    }

    @Test public void onePulseAdvancesOnlyTheFirstEligibleLoadedCrossingSite() throws Exception {
        World world = mockWorld(false);
        Map<BlockPos, TileEntity> tiles = new HashMap<BlockPos, TileEntity>();
        Mockito.when(world.getTileEntity(Mockito.any(BlockPos.class))).thenAnswer(invocation ->
                tiles.get(invocation.getArgument(0)));

        BlockPos firstPos = HIVE_POS.add(-2, -1, -2);
        BlockPos secondPos = HIVE_POS.add(-2, 0, -2);
        TileEntityCrop first = crossingSite(firstPos);
        TileEntityCrop second = crossingSite(secondPos);
        tiles.put(firstPos, first);
        tiles.put(secondPos, second);
        addReadyParents(tiles, firstPos);
        addReadyParents(tiles, secondPos);

        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), world, HIVE_POS);
        assertTrue(hive.runOnePulse());
        Mockito.verify(first, Mockito.times(1)).performTick();
        Mockito.verify(second, Mockito.never()).performTick();
    }

    @Test public void ignoresAnUnloadedTargetWithoutReadingItsTileEntity() throws Exception {
        World world = mockWorld(false);
        BlockPos target = HIVE_POS.add(-2, -1, -2);
        Mockito.when(world.isBlockLoaded(target, false)).thenReturn(false);
        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), world, HIVE_POS);
        assertFalse(hive.runOnePulse());
        Mockito.verify(world, Mockito.never()).getTileEntity(target);
    }

    @Test public void clientTickDoesNotAdvanceCooldownOrScan() throws Exception {
        World client = mockWorld(true);
        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), client, HIVE_POS);
        hive.update();
        assertEquals(0, hive.getCooldown());
        assertFalse(hive.runOnePulse());
        Mockito.verifyZeroInteractions(client);
    }

    @Test public void consumesOneChargeForAnExecutedPulseEvenWithoutANewHybrid() throws Exception {
        World world = mockWorld(false);
        Map<BlockPos, TileEntity> tiles = new HashMap<BlockPos, TileEntity>();
        Mockito.when(world.getTileEntity(Mockito.any(BlockPos.class))).thenAnswer(invocation ->
                tiles.get(invocation.getArgument(0)));
        BlockPos target = HIVE_POS.add(-2, -1, -2);
        tiles.put(target, crossingSite(target));
        addReadyParents(tiles, target);
        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), world, HIVE_POS);
        assertTrue(hive.addPowderCharge());
        for (int i = 0; i < 34; i++) hive.update();
        assertEquals(15, hive.getCharges());
    }

    @Test public void powderFillsPartialRoomAndIsNotConsumedWhenHiveIsFull() throws Exception {
        World world = mockWorld(false);
        PollinatorHiveTile hive = new PollinatorHiveTile();
        Mockito.when(world.getTileEntity(HIVE_POS)).thenReturn(hive);
        setCharges(hive, 60);
        attach(hive, world, HIVE_POS);
        ItemStack powder = new ItemStack(CropManager.MUTAGENIC_POWDER);
        EntityPlayer player = mockPlayer(powder, false);

        assertTrue(CropManager.POLLINATOR_HIVE.onBlockActivated(world, HIVE_POS,
                CropManager.POLLINATOR_HIVE.getDefaultState(), player, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F));
        assertEquals(64, hive.getCharges());
        assertTrue("Accepted powder should be consumed", powder.isEmpty());

        ItemStack extraPowder = new ItemStack(CropManager.MUTAGENIC_POWDER);
        Mockito.when(player.getHeldItem(EnumHand.MAIN_HAND)).thenReturn(extraPowder);
        CropManager.POLLINATOR_HIVE.onBlockActivated(world, HIVE_POS,
                CropManager.POLLINATOR_HIVE.getDefaultState(), player, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(64, hive.getCharges());
        assertEquals("Full hive must not consume powder", 1, extraPowder.getCount());
    }

    @Test public void creativeAndClientInteractionsDoNotConsumePowderOrMutateCharges() throws Exception {
        World world = mockWorld(false);
        PollinatorHiveTile hive = attach(new PollinatorHiveTile(), world, HIVE_POS);
        Mockito.when(world.getTileEntity(HIVE_POS)).thenReturn(hive);
        ItemStack creativePowder = new ItemStack(CropManager.MUTAGENIC_POWDER);
        EntityPlayer creative = mockPlayer(creativePowder, true);
        CropManager.POLLINATOR_HIVE.onBlockActivated(world, HIVE_POS,
                CropManager.POLLINATOR_HIVE.getDefaultState(), creative, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(16, hive.getCharges());
        assertEquals("Creative players keep their Mutagenic Powder", 1, creativePowder.getCount());

        World client = mockWorld(true);
        PollinatorHiveTile clientHive = attach(new PollinatorHiveTile(), client, HIVE_POS);
        Mockito.when(client.getTileEntity(HIVE_POS)).thenReturn(clientHive);
        ItemStack clientPowder = new ItemStack(CropManager.MUTAGENIC_POWDER);
        EntityPlayer clientPlayer = mockPlayer(clientPowder, false);
        CropManager.POLLINATOR_HIVE.onBlockActivated(client, HIVE_POS,
                CropManager.POLLINATOR_HIVE.getDefaultState(), clientPlayer, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(0, clientHive.getCharges());
        assertEquals(1, clientPowder.getCount());

        ItemStack deniedPowder = new ItemStack(CropManager.MUTAGENIC_POWDER);
        EntityPlayer deniedPlayer = mockPlayer(deniedPowder, false);
        Mockito.when(deniedPlayer.canPlayerEdit(Mockito.any(BlockPos.class), Mockito.any(EnumFacing.class), Mockito.any(ItemStack.class)))
                .thenReturn(false);
        CropManager.POLLINATOR_HIVE.onBlockActivated(world, HIVE_POS,
                CropManager.POLLINATOR_HIVE.getDefaultState(), deniedPlayer, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        assertEquals(16, hive.getCharges());
        assertEquals(1, deniedPowder.getCount());
    }

    @Test public void chargedAndCooldownPersistAndCorruptNbtIsClamped() {
        PollinatorHiveTile source = new PollinatorHiveTile();
        NBTTagCompound saved = new NBTTagCompound();
        saved.setInteger("Charges", 37);
        saved.setInteger("Cooldown", 52);
        source.readFromNBT(saved);
        NBTTagCompound roundTrip = source.writeToNBT(new NBTTagCompound());
        PollinatorHiveTile restored = new PollinatorHiveTile();
        restored.readFromNBT(roundTrip);
        assertEquals(37, restored.getCharges());
        assertEquals(52, restored.getCooldown());

        NBTTagCompound invalid = new NBTTagCompound();
        invalid.setInteger("Charges", -9);
        invalid.setInteger("Cooldown", 10000);
        restored.readFromNBT(invalid);
        assertEquals(0, restored.getCharges());
        assertEquals(99, restored.getCooldown());
        invalid.setInteger("Charges", 10000);
        invalid.setInteger("Cooldown", -20);
        restored.readFromNBT(invalid);
        assertEquals(64, restored.getCharges());
        assertEquals(0, restored.getCooldown());
    }

    @Test public void chargedStateMetadataAndTileLifecycleAreStable() {
        PollinatorHiveBlock block = CropManager.POLLINATOR_HIVE;
        assertEquals(block.getDefaultState(), block.getStateFromMeta(block.getMetaFromState(block.getDefaultState())));
        assertEquals(block.getDefaultState().withProperty(PollinatorHiveBlock.CHARGED, true),
                block.getStateFromMeta(1));
        PollinatorHiveTile tile = new PollinatorHiveTile();
        assertFalse(tile.shouldRefresh(null, HIVE_POS, block.getDefaultState(),
                block.getDefaultState().withProperty(PollinatorHiveBlock.CHARGED, true)));
        assertTrue(tile.shouldRefresh(null, HIVE_POS, block.getDefaultState(), net.minecraft.init.Blocks.AIR.getDefaultState()));
    }

    private void addReadyParents(Map<BlockPos, TileEntity> tiles, BlockPos target) {
        CropCard parentCard = Mockito.mock(CropCard.class);
        Mockito.when(parentCard.canCross(Mockito.any())).thenReturn(true);
        int count = 0;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            TileEntityCrop parent = Mockito.mock(TileEntityCrop.class);
            Mockito.when(parent.getCrop()).thenReturn(parentCard);
            tiles.put(target.offset(facing), parent);
            if (++count == 2) break;
        }
    }

    private static TileEntityCrop crossingSite(BlockPos pos) {
        TileEntityCrop crop = Mockito.mock(TileEntityCrop.class);
        Mockito.when(crop.getPos()).thenReturn(pos);
        Mockito.when(crop.isCrossingBase()).thenReturn(true);
        Mockito.when(crop.getCrop()).thenReturn(null);
        return crop;
    }

    private static EntityPlayer mockPlayer(ItemStack held, boolean creative) {
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        PlayerCapabilities capabilities = new PlayerCapabilities();
        capabilities.isCreativeMode = creative;
        player.capabilities = capabilities;
        Mockito.when(player.getHeldItem(EnumHand.MAIN_HAND)).thenReturn(held);
        Mockito.when(player.canPlayerEdit(Mockito.any(BlockPos.class), Mockito.any(EnumFacing.class), Mockito.any(ItemStack.class)))
                .thenReturn(true);
        return player;
    }

    private static World mockWorld(boolean remote) throws Exception {
        World world = Mockito.mock(World.class);
        setField(world, World.class, "isRemote", remote);
        setField(world, World.class, "rand", new Random(1L));
        Mockito.when(world.getHeight()).thenReturn(256);
        Mockito.when(world.isBlockLoaded(Mockito.any(BlockPos.class), Mockito.eq(false))).thenReturn(true);
        Mockito.when(world.getBlockState(HIVE_POS)).thenReturn(CropManager.POLLINATOR_HIVE.getDefaultState());
        return world;
    }

    private static <T extends TileEntity> T attach(T tile, World world, BlockPos pos) {
        tile.setWorld(world);
        tile.setPos(pos);
        return tile;
    }

    private static void setCharges(PollinatorHiveTile tile, int charges) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Charges", charges);
        tile.readFromNBT(tag);
    }

    private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
