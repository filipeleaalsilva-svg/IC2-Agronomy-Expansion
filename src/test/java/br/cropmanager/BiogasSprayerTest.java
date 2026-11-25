package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.FluidName;
import java.lang.reflect.Field;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BiogasSprayerTest {
    @Test public void spendsFuelOnlyForActualHydrationOrFertilization() throws Exception {
        Bootstrap.register();
        Fluid biogas = new Fluid("cropmanager_test_sprayer_gas",
                new ResourceLocation("minecraft:blocks/water_still"), new ResourceLocation("minecraft:blocks/water_flow"));
        FluidRegistry.registerFluid(biogas);
        Field instance = FluidName.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object old = instance.get(FluidName.biogas);
        instance.set(FluidName.biogas, biogas);
        try {
            BiogasSprayerTile sprayer = new BiogasSprayerTile();
            sprayer.setWorld(mock(World.class));
            sprayer.gas.fill(new FluidStack(biogas, 4), true);
            sprayer.water.fill(new FluidStack(FluidRegistry.WATER, 75), true);
            TileEntityCrop crop = mock(TileEntityCrop.class);
            CropCard card = mock(CropCard.class);
            when(crop.getCrop()).thenReturn(card);
            when(crop.getStorageWater()).thenReturn(95, 95, 100);
            sprayer.tend(crop);
            assertEquals(3, sprayer.gas.getFluidAmount());
            assertEquals(50, sprayer.water.getFluidAmount());
            verify(crop).setStorageWater(100);

            when(crop.getStorageWater()).thenReturn(100);
            when(crop.getStorageNutrients()).thenReturn(100);
            assertTrue(sprayer.fertilizer.insertItem(0, new ItemStack(CropManager.ORGANIC_COMPOST, 2), false).isEmpty());
            sprayer.tend(crop);
            assertEquals(3, sprayer.gas.getFluidAmount());
            assertEquals(2, sprayer.fertilizer.getStackInSlot(0).getCount());

            sprayer.water.setFluid(null);
            when(crop.getStorageNutrients()).thenReturn(60);
            sprayer.tend(crop);
            assertEquals(2, sprayer.gas.getFluidAmount());
            assertEquals(1, sprayer.fertilizer.getStackInSlot(0).getCount());
        } finally {
            instance.set(FluidName.biogas, old);
        }
    }

    @Test public void containerShiftClickMovesOnlyFertilizers() {
        Bootstrap.register();
        BiogasSprayerTile sprayer = new BiogasSprayerTile();
        net.minecraft.entity.player.EntityPlayer player = mock(net.minecraft.entity.player.EntityPlayer.class);
        player.inventory = new net.minecraft.entity.player.InventoryPlayer(player);
        EuWifiContainer container = new EuWifiContainer(player.inventory, sprayer);

        player.inventory.setInventorySlotContents(9, new ItemStack(net.minecraft.init.Blocks.COBBLESTONE, 4));
        assertTrue(container.transferStackInSlot(player, 1).isEmpty());
        assertTrue(sprayer.fertilizer.getStackInSlot(0).isEmpty());

        player.inventory.setInventorySlotContents(9, new ItemStack(CropManager.ORGANIC_COMPOST, 4));
        assertFalse(container.transferStackInSlot(player, 1).isEmpty());
        assertEquals(4, sprayer.fertilizer.getStackInSlot(0).getCount());
        assertTrue(player.inventory.getStackInSlot(9).isEmpty());

        assertFalse(container.transferStackInSlot(player, 0).isEmpty());
        assertTrue(sprayer.fertilizer.getStackInSlot(0).isEmpty());
        assertEquals(4, player.inventory.getStackInSlot(8).getCount());
    }
}
