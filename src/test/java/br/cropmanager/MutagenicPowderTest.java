package br.cropmanager;

import br.cropmanager.crop.CropRegistry;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import java.lang.reflect.Field;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import static org.junit.Assert.*;

/**
 * Archetype A gate tests for Mutagenic Powder ({@link MutagenicPowderItem}) and
 * {@link CrossBreedingHelper}.
 */
public final class MutagenicPowderTest {
    private static final BlockPos CENTER = new BlockPos(10, 64, 10);

    private CropCard previousWeed;
    private int previousBurstTicks;
    private double previousBonusChance;
    private CropCard mockWeed;

    @BeforeClass
    public static void bootstrapVanilla() {
        Bootstrap.register();
    }

    @Before
    public void setUp() {
        previousWeed = Crops.weed;
        previousBurstTicks = ManagerConfig.mutagenicPowderBurstTicks;
        previousBonusChance = ManagerConfig.mutagenicStatBonusChance;

        mockWeed = Mockito.mock(CropCard.class);
        Crops.weed = mockWeed;
        ManagerConfig.mutagenicPowderBurstTicks = 12;
        ManagerConfig.mutagenicStatBonusChance = 0.5;
    }

    @After
    public void tearDown() {
        Crops.weed = previousWeed;
        ManagerConfig.mutagenicPowderBurstTicks = previousBurstTicks;
        ManagerConfig.mutagenicStatBonusChance = previousBonusChance;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final class CropState {
        CropCard card;
        boolean crossingBase;
        int weedEx;
        int growth;
        int gain;
        int resistance;
        int tickCalls;

        CropState(CropCard card, boolean crossingBase, int weedEx, int growth, int gain, int resistance) {
            this.card = card;
            this.crossingBase = crossingBase;
            this.weedEx = weedEx;
            this.growth = growth;
            this.gain = gain;
            this.resistance = resistance;
        }
    }

    private static void setField(Object target, Class<?> owner, String name, Object value) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new AssertionError("Failed to set field " + owner.getName() + "." + name, e);
        }
    }

    private static World mockWorld(boolean isRemote, Random rand) {
        World world = Mockito.mock(World.class);
        setField(world, World.class, "isRemote", isRemote);
        setField(world, World.class, "rand", rand);
        Mockito.when(world.isBlockLoaded(Mockito.any(BlockPos.class), Mockito.eq(false))).thenReturn(true);
        Mockito.when(world.isBlockModifiable(Mockito.any(EntityPlayer.class), Mockito.any(BlockPos.class))).thenReturn(true);
        return world;
    }

    private static EntityPlayer mockPlayer(ItemStack held, boolean creative, boolean canEdit) {
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        PlayerCapabilities caps = new PlayerCapabilities();
        caps.isCreativeMode = creative;
        player.capabilities = caps;
        Mockito.when(player.getHeldItem(Mockito.any(EnumHand.class))).thenReturn(held);
        Mockito.when(player.canPlayerEdit(Mockito.any(BlockPos.class), Mockito.any(EnumFacing.class), Mockito.any(ItemStack.class)))
                .thenReturn(canEdit);
        return player;
    }

    private static TileEntityCrop mockCropTile(BlockPos pos, CropState state, Runnable onPerformTick) {
        TileEntityCrop tile = Mockito.mock(TileEntityCrop.class);
        Mockito.when(tile.getPos()).thenReturn(pos);
        Mockito.when(tile.getCrop()).thenAnswer(inv -> state.card);
        Mockito.when(tile.isCrossingBase()).thenAnswer(inv -> state.crossingBase);
        Mockito.doAnswer(inv -> { state.crossingBase = (Boolean) inv.getArguments()[0]; return null; })
                .when(tile).setCrossingBase(Mockito.anyBoolean());
        Mockito.when(tile.getStorageWeedEX()).thenAnswer(inv -> state.weedEx);
        Mockito.doAnswer(inv -> { state.weedEx = (Integer) inv.getArguments()[0]; return null; })
                .when(tile).setStorageWeedEX(Mockito.anyInt());
        Mockito.when(tile.getStatGrowth()).thenAnswer(inv -> state.growth);
        Mockito.doAnswer(inv -> { state.growth = (Integer) inv.getArguments()[0]; return null; })
                .when(tile).setStatGrowth(Mockito.anyInt());
        Mockito.when(tile.getStatGain()).thenAnswer(inv -> state.gain);
        Mockito.doAnswer(inv -> { state.gain = (Integer) inv.getArguments()[0]; return null; })
                .when(tile).setStatGain(Mockito.anyInt());
        Mockito.when(tile.getStatResistance()).thenAnswer(inv -> state.resistance);
        Mockito.doAnswer(inv -> { state.resistance = (Integer) inv.getArguments()[0]; return null; })
                .when(tile).setStatResistance(Mockito.anyInt());
        Mockito.doAnswer(inv -> {
            state.card = null;
            state.crossingBase = false;
            return null;
        }).when(tile).reset();
        Mockito.doAnswer(inv -> {
            state.tickCalls++;
            if (onPerformTick != null) onPerformTick.run();
            return null;
        }).when(tile).performTick();
        return tile;
    }

    private static void attachParent(World world, BlockPos pos, CropCard card, boolean canCross) {
        TileEntityCrop parent = Mockito.mock(TileEntityCrop.class);
        Mockito.when(parent.getPos()).thenReturn(pos);
        Mockito.when(parent.getCrop()).thenReturn(card);
        if (card != null) {
            Mockito.when(card.canCross(parent)).thenReturn(canCross);
        }
        Mockito.when(world.getTileEntity(pos)).thenReturn(parent);
    }

    private static void attachTwoReadyParents(World world, BlockPos center) {
        CropCard parentCard1 = Mockito.mock(CropCard.class);
        CropCard parentCard2 = Mockito.mock(CropCard.class);
        BlockPos north = center.offset(EnumFacing.NORTH);
        BlockPos east = center.offset(EnumFacing.EAST);
        attachParent(world, north, parentCard1, true);
        attachParent(world, east, parentCard2, true);
    }

    private static long findSeedMatching(boolean wantBelowHalf) {
        for (long seed = 1L; seed < 10000L; seed++) {
            double v = new Random(seed).nextDouble();
            if (wantBelowHalf ? (v < 0.5) : (v >= 0.5)) {
                return seed;
            }
        }
        throw new AssertionError("No seed found");
    }

    // -------------------------------------------------------------------------
    // A1: Happy Path
    // -------------------------------------------------------------------------

    @Test
    public void a1_happyPathSpawnsHybridStopsBurstEarlyRestoresWeedExAndAppliesSafeStatBonus() {
        long bonusSeed = findSeedMatching(true);
        World world = mockWorld(false, new Random(bonusSeed));
        attachTwoReadyParents(world, CENTER);

        CropState state = new CropState(null, true, 45, 10, 12, 8);
        CropCard hybrid = CropRegistry.BIOREED;
        TileEntityCrop crop = mockCropTile(CENTER, state, () -> {
            state.weedEx = Math.max(0, state.weedEx - 10);
            if (state.tickCalls == 3) {
                state.card = hybrid;
                state.crossingBase = false;
            }
        });
        Mockito.when(world.getTileEntity(CENTER)).thenReturn(crop);

        assertTrue(CrossBreedingHelper.hasReadyParents(world, CENTER));
        assertTrue(CrossBreedingHelper.canAccelerate(world, crop));

        assertTrue(CrossBreedingHelper.accelerateCrossing(world, crop, 12, true, new Random(bonusSeed)));

        assertEquals("Burst should stop early once offspring spawns", 3, state.tickCalls);
        Mockito.verify(crop, Mockito.times(3)).performTick();
        assertSame(hybrid, state.card);
        assertEquals("Weed-Ex storage must be restored", 45, state.weedEx);
        assertTrue("Crop tile must be marked dirty", crop.dirty);
        Mockito.verify(crop).updateState();
        Mockito.verify(world).markChunkDirty(CENTER, crop);

        int totalStats = state.growth + state.gain + state.resistance;
        assertEquals("Safe +1 stat bonus should apply when RNG triggers", 10 + 12 + 8 + 1, totalStats);
        assertTrue(state.growth <= CrossBreedingHelper.SAFE_MAX_GROWTH);
        assertTrue(state.gain <= CrossBreedingHelper.MAX_STAT);
        assertTrue(state.resistance <= CrossBreedingHelper.MAX_STAT);
    }

    // -------------------------------------------------------------------------
    // A2: Guards & Caps
    // -------------------------------------------------------------------------

    @Test
    public void a2_guardsRejectNullsInvalidSticksFewParentsAndUnloadedChunks() {
        World world = mockWorld(false, new Random(1L));
        CropState validState = new CropState(null, true, 10, 5, 5, 5);
        TileEntityCrop validCrop = mockCropTile(CENTER, validState, null);

        // Null world / pos / crop and out-of-bounds Y
        assertFalse(CrossBreedingHelper.hasReadyParents(null, CENTER));
        assertFalse(CrossBreedingHelper.hasReadyParents(world, null));
        assertFalse(CrossBreedingHelper.hasReadyParents(world, new BlockPos(10, -1, 10)));
        assertFalse(CrossBreedingHelper.hasReadyParents(world, new BlockPos(10, 256, 10)));
        assertFalse(CrossBreedingHelper.canAccelerate(null, validCrop));
        assertFalse(CrossBreedingHelper.canAccelerate(world, null));
        assertFalse(CrossBreedingHelper.accelerateCrossing(null, validCrop, 12, true, new Random(1L)));
        assertFalse(CrossBreedingHelper.accelerateCrossing(world, null, 12, true, new Random(1L)));

        // < 2 ready parents (0 parents)
        assertFalse(CrossBreedingHelper.hasReadyParents(world, CENTER));
        assertFalse(CrossBreedingHelper.canAccelerate(world, validCrop));
        assertFalse(CrossBreedingHelper.accelerateCrossing(world, validCrop, 12, true, new Random(1L)));
        Mockito.verify(validCrop, Mockito.never()).performTick();

        // 1 ready parent + 1 immature parent + 1 weed parent + 1 non-crop TE -> still < 2 ready parents
        CropCard readyCard = Mockito.mock(CropCard.class);
        CropCard immatureCard = Mockito.mock(CropCard.class);
        attachParent(world, CENTER.north(), readyCard, true);
        attachParent(world, CENTER.south(), immatureCard, false);
        attachParent(world, CENTER.east(), mockWeed, true);
        TileEntity unrelatedTile = Mockito.mock(TileEntity.class);
        Mockito.when(world.getTileEntity(CENTER.west())).thenReturn(unrelatedTile);

        assertFalse(CrossBreedingHelper.hasReadyParents(world, CENTER));
        assertFalse(CrossBreedingHelper.canAccelerate(world, validCrop));

        // Now attach 2 ready parents, but unload one neighbor chunk
        attachTwoReadyParents(world, CENTER);
        Mockito.when(world.isBlockLoaded(CENTER.offset(EnumFacing.EAST), false)).thenReturn(false);
        assertFalse("Unloaded neighbor must not count as a ready parent", CrossBreedingHelper.hasReadyParents(world, CENTER));
        assertFalse(CrossBreedingHelper.canAccelerate(world, validCrop));

        // Re-load neighbor chunk -> now 2 ready parents exist
        Mockito.when(world.isBlockLoaded(CENTER.offset(EnumFacing.EAST), false)).thenReturn(true);
        assertTrue(CrossBreedingHelper.hasReadyParents(world, CENTER));

        // Reject !isCrossingBase (single crop stick)
        CropState singleStickState = new CropState(null, false, 10, 5, 5, 5);
        TileEntityCrop singleStick = mockCropTile(CENTER, singleStickState, null);
        assertFalse(CrossBreedingHelper.canAccelerate(world, singleStick));
        assertFalse(CrossBreedingHelper.accelerateCrossing(world, singleStick, 12, true, new Random(1L)));
        Mockito.verify(singleStick, Mockito.never()).performTick();

        // Reject already planted stick
        CropState plantedState = new CropState(CropRegistry.BIOREED, true, 10, 5, 5, 5);
        TileEntityCrop plantedStick = mockCropTile(CENTER, plantedState, null);
        assertFalse(CrossBreedingHelper.canAccelerate(world, plantedStick));
        assertFalse(CrossBreedingHelper.accelerateCrossing(world, plantedStick, 12, true, new Random(1L)));
        Mockito.verify(plantedStick, Mockito.never()).performTick();
    }

    @Test
    public void a2_chunkBorderRequiresEveryNeighborBeforeCallingIc2() {
        World world = mockWorld(false, new Random(1L));
        BlockPos center = new BlockPos(0, 64, 10);
        BlockPos unloadedNeighbor = center.west();
        attachTwoReadyParents(world, center);
        TileEntityCrop crop = mockCropTile(center, new CropState(null, true, 10, 0, 0, 0), null);
        Mockito.when(world.isBlockLoaded(unloadedNeighbor, false)).thenReturn(false);

        assertFalse(CrossBreedingHelper.accelerateCrossing(world, crop, 1, false, world.rand));
        Mockito.verify(crop, Mockito.never()).performTick();
        Mockito.verify(world, Mockito.never()).getTileEntity(unloadedNeighbor);

        Mockito.when(world.isBlockLoaded(unloadedNeighbor, false)).thenReturn(true);
        Mockito.when(world.isBlockLoaded(center, false)).thenReturn(false);
        assertFalse(CrossBreedingHelper.accelerateCrossing(world, crop, 1, false, world.rand));
        Mockito.verify(crop, Mockito.never()).performTick();

        Mockito.when(world.isBlockLoaded(center, false)).thenReturn(true);
        assertTrue(CrossBreedingHelper.accelerateCrossing(world, crop, 1, false, world.rand));
        Mockito.verify(crop).performTick();
    }

    @Test
    public void a2_safeStatBonusNeverExceedsGrowth23AndCapsGainAndResistanceAt31() {
        assertFalse(CrossBreedingHelper.applySafeStatBonus(null, new Random(1L)));

        // GR at 22, GA and RE already at 31: only GR can advance to 23, never 24
        CropState nearGrowthCap = new CropState(CropRegistry.BIOREED, false, 0, 22, 31, 31);
        TileEntityCrop growthTile = mockCropTile(CENTER, nearGrowthCap, null);
        assertFalse(CrossBreedingHelper.applySafeStatBonus(growthTile, null));

        assertTrue(CrossBreedingHelper.applySafeStatBonus(growthTile, new Random(42L)));
        assertEquals(CrossBreedingHelper.SAFE_MAX_GROWTH, nearGrowthCap.growth);
        assertEquals(31, nearGrowthCap.gain);
        assertEquals(31, nearGrowthCap.resistance);

        // Further attempts must return false and NEVER push GR to 24
        for (int i = 0; i < 20; i++) {
            assertFalse(CrossBreedingHelper.applySafeStatBonus(growthTile, new Random(i)));
            assertEquals("GR must never exceed SAFE_MAX_GROWTH (23)", 23, nearGrowthCap.growth);
            assertEquals(31, nearGrowthCap.gain);
            assertEquals(31, nearGrowthCap.resistance);
        }

        // GR already at 23, GA=30, RE=30: exactly 2 bonuses apply (one to GA, one to RE), GR stays 23
        CropState nearAllCaps = new CropState(CropRegistry.BIOREED, false, 0, 23, 30, 30);
        TileEntityCrop allCapsTile = mockCropTile(CENTER, nearAllCaps, null);
        Random rand = new Random(123L);
        assertTrue(CrossBreedingHelper.applySafeStatBonus(allCapsTile, rand));
        assertTrue(CrossBreedingHelper.applySafeStatBonus(allCapsTile, rand));
        assertEquals(23, nearAllCaps.growth);
        assertEquals(31, nearAllCaps.gain);
        assertEquals(31, nearAllCaps.resistance);
        assertFalse(CrossBreedingHelper.applySafeStatBonus(allCapsTile, rand));
        assertEquals(23, nearAllCaps.growth);
        assertEquals(31, nearAllCaps.gain);
        assertEquals(31, nearAllCaps.resistance);
    }

    @Test
    public void a2_weedSpawnDuringBurstInvokesResetAndRestoresCrossingBase() {
        World world = mockWorld(false, new Random(1L));
        attachTwoReadyParents(world, CENTER);

        // Case 1: Ticks 1 & 2 spawn IC2 weed, tick 3 spawns a real hybrid
        CropState state = new CropState(null, true, 60, 4, 4, 4);
        CropCard hybrid = CropRegistry.GLOWBLOOM;
        TileEntityCrop crop = mockCropTile(CENTER, state, () -> {
            if (state.tickCalls <= 2) {
                state.card = mockWeed;
            } else {
                state.card = hybrid;
                state.crossingBase = false;
            }
        });

        assertTrue(CrossBreedingHelper.accelerateCrossing(world, crop, 12, false, new Random(1L)));

        assertEquals(3, state.tickCalls);
        assertSame(hybrid, state.card);
        assertEquals(60, state.weedEx);
        InOrder order = Mockito.inOrder(crop);
        order.verify(crop).performTick();
        order.verify(crop).reset();
        order.verify(crop).setCrossingBase(true);
        order.verify(crop).performTick();
        order.verify(crop).reset();
        order.verify(crop).setCrossingBase(true);
        order.verify(crop).performTick();

        // Case 2: Every tick spawns weed -> double crop stick stays intact and empty
        CropState allWeedState = new CropState(null, true, 30, 0, 0, 0);
        TileEntityCrop allWeedCrop = mockCropTile(CENTER, allWeedState, () -> allWeedState.card = mockWeed);

        assertTrue(CrossBreedingHelper.accelerateCrossing(world, allWeedCrop, 4, true, new Random(1L)));
        assertEquals(4, allWeedState.tickCalls);
        Mockito.verify(allWeedCrop, Mockito.times(4)).reset();
        Mockito.verify(allWeedCrop, Mockito.times(4)).setCrossingBase(true);
        assertNull(allWeedState.card);
        assertTrue("Double crop stick must remain a crossing base after weed suppression", allWeedState.crossingBase);
        assertEquals(30, allWeedState.weedEx);
    }

    // -------------------------------------------------------------------------
    // A3: Consumption & Side Guard
    // -------------------------------------------------------------------------

    @Test
    public void a3_itemUseGuardsNonCropClientSideAndEditPermissions() {
        MutagenicPowderItem item = CropManager.MUTAGENIC_POWDER;
        assertEquals("cropmanager:mutagenic_powder", item.getRegistryName().toString());
        assertEquals("item.cropmanager.mutagenic_powder", item.getTranslationKey());

        // 1. Non-crop block -> PASS, no consumption
        World serverWorld = mockWorld(false, new Random(1L));
        Mockito.when(serverWorld.getTileEntity(CENTER)).thenReturn(null);
        ItemStack stack = new ItemStack(item, 5);
        EntityPlayer survival = mockPlayer(stack, false, true);

        assertEquals(EnumActionResult.PASS,
                item.onItemUseFirst(survival, serverWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals(5, stack.getCount());

        // Out-of-bounds Y and unloaded chunk -> PASS
        assertEquals(EnumActionResult.PASS,
                item.onItemUseFirst(survival, serverWorld, new BlockPos(10, -1, 10), EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals(EnumActionResult.PASS,
                item.onItemUseFirst(survival, serverWorld, new BlockPos(10, 256, 10), EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        Mockito.when(serverWorld.isBlockLoaded(CENTER, false)).thenReturn(false);
        assertEquals(EnumActionResult.PASS,
                item.onItemUseFirst(survival, serverWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        Mockito.when(serverWorld.isBlockLoaded(CENTER, false)).thenReturn(true);

        // 2. Client side (world.isRemote == true) -> SUCCESS without mutating state or consuming item
        World clientWorld = mockWorld(true, new Random(1L));
        attachTwoReadyParents(clientWorld, CENTER);
        CropState clientState = new CropState(null, true, 25, 5, 5, 5);
        TileEntityCrop clientCrop = mockCropTile(CENTER, clientState, () -> clientState.card = CropRegistry.BIOREED);
        Mockito.when(clientWorld.getTileEntity(CENTER)).thenReturn(clientCrop);

        assertEquals(EnumActionResult.SUCCESS,
                item.onItemUseFirst(survival, clientWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals("Client side must not consume item", 5, stack.getCount());
        assertEquals("Client side must not run performTick", 0, clientState.tickCalls);
        assertNull("Client side must not mutate crop", clientState.card);
        Mockito.verify(clientWorld, Mockito.never()).playEvent(Mockito.anyInt(), Mockito.any(BlockPos.class), Mockito.anyInt());

        // Client side on non-acceleratable crop -> PASS
        clientState.crossingBase = false;
        assertEquals(EnumActionResult.PASS,
                item.onItemUseFirst(survival, clientWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));

        // 3. Permission denied (!canPlayerEdit) -> FAIL without mutating or consuming
        attachTwoReadyParents(serverWorld, CENTER);
        CropState protectedState = new CropState(null, true, 25, 5, 5, 5);
        TileEntityCrop protectedCrop = mockCropTile(CENTER, protectedState, () -> protectedState.card = CropRegistry.BIOREED);
        Mockito.when(serverWorld.getTileEntity(CENTER)).thenReturn(protectedCrop);

        EntityPlayer emptyHand = mockPlayer(ItemStack.EMPTY, false, true);
        assertEquals(EnumActionResult.PASS,
                item.onItemUseFirst(emptyHand, serverWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        Mockito.verify(protectedCrop, Mockito.never()).performTick();

        EntityPlayer noEditPlayer = mockPlayer(stack, false, false);
        assertEquals(EnumActionResult.FAIL,
                item.onItemUseFirst(noEditPlayer, serverWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals(5, stack.getCount());
        assertEquals(0, protectedState.tickCalls);

        // Permission denied (!world.isBlockModifiable) -> FAIL
        Mockito.when(serverWorld.isBlockModifiable(survival, CENTER)).thenReturn(false);
        assertEquals(EnumActionResult.FAIL,
                item.onItemUseFirst(survival, serverWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals(5, stack.getCount());
        assertEquals(0, protectedState.tickCalls);
    }

    @Test
    public void a3_itemUseConsumesOneInSurvivalAndZeroInCreative() {
        MutagenicPowderItem item = CropManager.MUTAGENIC_POWDER;
        long seed = findSeedMatching(true);

        // Survival mode consumes 1 item on success
        World survivalWorld = mockWorld(false, new Random(seed));
        attachTwoReadyParents(survivalWorld, CENTER);
        CropState survivalState = new CropState(null, true, 20, 6, 6, 6);
        TileEntityCrop survivalCrop = mockCropTile(CENTER, survivalState, () -> {
            survivalState.card = CropRegistry.BIOREED;
            survivalState.crossingBase = false;
        });
        Mockito.when(survivalWorld.getTileEntity(CENTER)).thenReturn(survivalCrop);

        ItemStack survivalStack = new ItemStack(item, 5);
        EntityPlayer survivalPlayer = mockPlayer(survivalStack, false, true);

        assertEquals(EnumActionResult.SUCCESS,
                item.onItemUseFirst(survivalPlayer, survivalWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals("Survival mode must consume 1 item", 4, survivalStack.getCount());
        assertSame(CropRegistry.BIOREED, survivalState.card);
        Mockito.verify(survivalWorld).playEvent(2005, CENTER, 0);

        // Creative mode does NOT consume item on success
        World creativeWorld = mockWorld(false, new Random(seed));
        attachTwoReadyParents(creativeWorld, CENTER);
        CropState creativeState = new CropState(null, true, 20, 6, 6, 6);
        TileEntityCrop creativeCrop = mockCropTile(CENTER, creativeState, () -> {
            creativeState.card = CropRegistry.BIOREED;
            creativeState.crossingBase = false;
        });
        Mockito.when(creativeWorld.getTileEntity(CENTER)).thenReturn(creativeCrop);

        ItemStack creativeStack = new ItemStack(item, 5);
        EntityPlayer creativePlayer = mockPlayer(creativeStack, true, true);

        assertEquals(EnumActionResult.SUCCESS,
                item.onItemUseFirst(creativePlayer, creativeWorld, CENTER, EnumFacing.UP, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND));
        assertEquals("Creative mode must not consume item", 5, creativeStack.getCount());
        assertSame(CropRegistry.BIOREED, creativeState.card);
        Mockito.verify(creativeWorld).playEvent(2005, CENTER, 0);
    }

    // -------------------------------------------------------------------------
    // A4: Deterministic RNG
    // -------------------------------------------------------------------------

    @Test
    public void a4_deterministicSeededRandomCoversBonusAppliedAndBonusSkippedPaths() {
        long bonusSeed = findSeedMatching(true);
        long skipSeed = findSeedMatching(false);

        Random probeBonus = new Random(bonusSeed);
        assertTrue(probeBonus.nextDouble() < ManagerConfig.mutagenicStatBonusChance);
        int expectedStatIndex = probeBonus.nextInt(3);

        Random probeSkip = new Random(skipSeed);
        assertTrue(probeSkip.nextDouble() >= ManagerConfig.mutagenicStatBonusChance);

        // Path 1: Bonus applied deterministically to the exact chosen stat
        World world1 = mockWorld(false, new Random(bonusSeed));
        attachTwoReadyParents(world1, CENTER);
        CropState state1 = new CropState(null, true, 15, 10, 10, 10);
        TileEntityCrop crop1 = mockCropTile(CENTER, state1, () -> {
            state1.card = CropRegistry.NITROBULB;
            state1.crossingBase = false;
        });

        assertTrue(CrossBreedingHelper.accelerateCrossing(world1, crop1, 12, true, new Random(bonusSeed)));
        assertEquals(10 + (expectedStatIndex == 0 ? 1 : 0), state1.growth);
        assertEquals(10 + (expectedStatIndex == 1 ? 1 : 0), state1.gain);
        assertEquals(10 + (expectedStatIndex == 2 ? 1 : 0), state1.resistance);
        assertSame(CropRegistry.NITROBULB, state1.card);
        assertEquals(15, state1.weedEx);

        // Path 2: Bonus skipped deterministically without corrupting spawned hybrid
        World world2 = mockWorld(false, new Random(skipSeed));
        attachTwoReadyParents(world2, CENTER);
        CropState state2 = new CropState(null, true, 15, 10, 10, 10);
        TileEntityCrop crop2 = mockCropTile(CENTER, state2, () -> {
            state2.card = CropRegistry.NITROBULB;
            state2.crossingBase = false;
        });

        assertTrue(CrossBreedingHelper.accelerateCrossing(world2, crop2, 12, true, new Random(skipSeed)));
        assertEquals(10, state2.growth);
        assertEquals(10, state2.gain);
        assertEquals(10, state2.resistance);
        assertSame(CropRegistry.NITROBULB, state2.card);
        assertEquals(15, state2.weedEx);

        // Path 3: allowStatBonus == false suppresses bonus even with bonusSeed
        World world3 = mockWorld(false, new Random(bonusSeed));
        attachTwoReadyParents(world3, CENTER);
        CropState state3 = new CropState(null, true, 15, 10, 10, 10);
        TileEntityCrop crop3 = mockCropTile(CENTER, state3, () -> {
            state3.card = CropRegistry.NITROBULB;
            state3.crossingBase = false;
        });

        assertTrue(CrossBreedingHelper.accelerateCrossing(world3, crop3, 12, false, new Random(bonusSeed)));
        assertEquals(10, state3.growth);
        assertEquals(10, state3.gain);
        assertEquals(10, state3.resistance);
    }
}
