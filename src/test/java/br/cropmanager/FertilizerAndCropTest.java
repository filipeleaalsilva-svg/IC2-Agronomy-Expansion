package br.cropmanager;

import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.CropRegistry;
import ic2.api.crops.CropCard;
import ic2.api.crops.ICropTile;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public final class FertilizerAndCropTest {
    @BeforeClass
    public static void boot() {
        Bootstrap.register();
    }

    private static ICropTile fakeCrop(CropCard card, int size, int nutrients, int water, int light, NBTTagCompound custom) {
        ICropTile tile = Mockito.mock(ICropTile.class);
        int[] state = {nutrients, water, 0, size};
        Mockito.when(tile.getCrop()).thenReturn(card);
        Mockito.when(tile.getCurrentSize()).thenAnswer(inv -> state[3]);
        Mockito.when(tile.getLightLevel()).thenReturn(light);
        Mockito.when(tile.getStorageNutrients()).thenAnswer(inv -> state[0]);
        Mockito.doAnswer(inv -> { state[0] = (Integer) inv.getArguments()[0]; return null; }).when(tile).setStorageNutrients(Mockito.anyInt());
        Mockito.when(tile.getStorageWater()).thenAnswer(inv -> state[1]);
        Mockito.doAnswer(inv -> { state[1] = (Integer) inv.getArguments()[0]; return null; }).when(tile).setStorageWater(Mockito.anyInt());
        Mockito.when(tile.getGrowthPoints()).thenAnswer(inv -> state[2]);
        Mockito.doAnswer(inv -> { state[2] = (Integer) inv.getArguments()[0]; return null; }).when(tile).setGrowthPoints(Mockito.anyInt());
        Mockito.when(tile.getCustomData()).thenReturn(custom);
        return tile;
    }

    @Test
    public void managerRecognizesCustomFertilizersAndComposterAcceptsNewOrganics() {
        assertTrue(ManagerTile.isFertilizer(new ItemStack(CropManager.ORGANIC_COMPOST)));
        assertTrue(ManagerTile.isFertilizer(new ItemStack(CropManager.CATALYTIC_FERTILIZER)));
        assertTrue(ComposterTile.organic(new ItemStack(CropManager.BIO_FIBER)));
        assertTrue(ComposterTile.organic(new ItemStack(CropManager.NITRO_ESSENCE)));
    }

    @Test
    public void organicCompostRestoresNutrientsAndWaterCappedAt100() {
        NBTTagCompound custom = new NBTTagCompound();
        ICropTile crop = fakeCrop(CropRegistry.BIOREED, 1, 20, 50, 12, custom);
        ItemStack stack = new ItemStack(CropManager.ORGANIC_COMPOST);

        assertTrue(FertilizerItem.applyToCrop(stack, crop, true));
        assertEquals(80, crop.getStorageNutrients());
        assertEquals(90, crop.getStorageWater());

        // Second application caps at 100/100; third returns false
        assertTrue(FertilizerItem.applyToCrop(stack, crop, true));
        assertEquals(100, crop.getStorageNutrients());
        assertEquals(100, crop.getStorageWater());
        assertFalse(FertilizerItem.applyToCrop(stack, crop, true));
    }

    @Test
    public void catalyticFertilizerUnlocksCatalyzedCropsAndConsumesChargeOnHarvest() {
        NBTTagCompound custom = new NBTTagCompound();
        BaseCropCard lazuli = CropRegistry.LAZULI_BLOOM;
        assertTrue(lazuli.requiresCatalyst());

        // Stage 1 grows without catalyst
        ICropTile stage1 = fakeCrop(lazuli, 1, 50, 50, 12, custom);
        assertTrue(lazuli.canGrow(stage1));

        // Stage 2 is blocked until Catalytic Fertilizer is applied
        ICropTile stage2 = fakeCrop(lazuli, 2, 100, 100, 12, custom);
        assertFalse(lazuli.canGrow(stage2));

        assertTrue(FertilizerItem.applyToCrop(new ItemStack(CropManager.CATALYTIC_FERTILIZER), stage2, true));
        assertEquals(ManagerConfig.catalyticCharges, FertilizerItem.catalyzedCharges(stage2));
        assertEquals(ManagerConfig.catalyticGrowthBoost, stage2.getGrowthPoints());
        assertTrue(lazuli.canGrow(stage2));

        // Harvest consumes 1 catalytic charge
        ICropTile mature = fakeCrop(lazuli, 4, 100, 100, 12, custom);
        assertTrue(lazuli.canBeHarvested(mature));
        ItemStack drop = lazuli.getGain(mature);
        assertFalse(drop.isEmpty());
        assertEquals(ManagerConfig.catalyticCharges - 1, FertilizerItem.catalyzedCharges(mature));
    }

    @Test
    public void radbloomRequiresRadioactiveSoilAndCatalystAndEmitsLightWhenMature() {
        NBTTagCompound custom = new NBTTagCompound();
        br.cropmanager.crop.RadbloomCropCard rad = CropRegistry.RADBLOOM;
        assertTrue(rad.requiresCatalyst());

        // Without Radioactive Soil below, cannot grow even at stage 1
        ICropTile noSoil = fakeCrop(rad, 1, 100, 100, 12, custom);
        Mockito.when(noSoil.isBlockBelow(CropManager.RADIOACTIVE_SOIL)).thenReturn(false);
        assertFalse(rad.canGrow(noSoil));
        assertEquals(0, rad.getEmittedLight(noSoil));

        // With Radioactive Soil below, stage 1 grows, stage 2 waits for Catalytic Fertilizer
        ICropTile withSoil1 = fakeCrop(rad, 1, 100, 100, 12, custom);
        Mockito.when(withSoil1.isBlockBelow(CropManager.RADIOACTIVE_SOIL)).thenReturn(true);
        assertTrue(rad.canGrow(withSoil1));

        ICropTile withSoil2 = fakeCrop(rad, 2, 100, 100, 12, custom);
        Mockito.when(withSoil2.isBlockBelow(CropManager.RADIOACTIVE_SOIL)).thenReturn(true);
        assertFalse(rad.canGrow(withSoil2));

        assertTrue(FertilizerItem.applyToCrop(new ItemStack(CropManager.CATALYTIC_FERTILIZER), withSoil2, true));
        assertTrue(rad.canGrow(withSoil2));

        // Stage 3 emits dim glow (5), stage 4 emits bright glow (10) and drops item consuming 1 catalyst charge
        ICropTile stage3 = fakeCrop(rad, 3, 100, 100, 12, custom);
        assertEquals(5, rad.getEmittedLight(stage3));
        ICropTile mature = fakeCrop(rad, 4, 100, 100, 12, custom);
        assertEquals(10, rad.getEmittedLight(mature));
        assertFalse(rad.getGain(mature).isEmpty());
        assertEquals(ManagerConfig.catalyticCharges - 1, FertilizerItem.catalyzedCharges(mature));
    }

    @Test
    public void glowbloomEmitsLightWhenNearOrAtMaturity() {
        NBTTagCompound custom = new NBTTagCompound();
        BaseCropCard glow = CropRegistry.GLOWBLOOM;
        assertFalse(glow.requiresCatalyst());
        assertEquals(0, glow.getEmittedLight(fakeCrop(glow, 2, 100, 100, 12, custom)));
        assertEquals(6, glow.getEmittedLight(fakeCrop(glow, 3, 100, 100, 12, custom)));
        assertEquals(12, glow.getEmittedLight(fakeCrop(glow, 4, 100, 100, 12, custom)));
    }

    @Test
    public void allCustomCropsExposeExpectedMetadataAndFourTextures() {
        assertEquals(10, CropRegistry.ALL.size());
        for (BaseCropCard card : CropRegistry.ALL) {
            assertEquals(CropManager.ID, card.getOwner());
            assertEquals(4, card.getMaxSize());
            assertEquals(4, card.getTexturesLocation().size());
            assertEquals("cropmanager.crop." + card.getId(), card.getUnlocalizedName());
        }
    }
}
