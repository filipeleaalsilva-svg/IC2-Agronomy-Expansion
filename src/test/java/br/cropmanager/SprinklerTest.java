package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
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
    }
}
