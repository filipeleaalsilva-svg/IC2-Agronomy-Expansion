package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.ItemName;
import java.lang.reflect.Field;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CrossingToolTest {
    @Test public void linkingReloadingAndHarvestingPreservesStockAndProtectsOtherCrops() throws Exception {
        Bootstrap.register();
        Item sticks = new Item().setRegistryName("cropmanager_test", "crossing_sticks");
        ForgeRegistries.ITEMS.register(sticks);
        Field instance = ItemName.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(ItemName.crop_stick);
        instance.set(ItemName.crop_stick, sticks);
        try {
            ItemStack tool = new ItemStack(CropManager.CROSSING_TOOL);
            TileEntityCrop crop = mock(TileEntityCrop.class);
            NBTTagCompound cropData = new NBTTagCompound();
            when(crop.getTileData()).thenReturn(cropData);
            when(crop.isCrossingBase()).thenReturn(true);
            assertEquals("linked", CrossingTool.useOnCrop(tool, crop));

            TileEntityCrop reloaded = mock(TileEntityCrop.class);
            when(reloaded.getTileData()).thenReturn(cropData.copy());
            CropCard plant = mock(CropCard.class);
            when(reloaded.getCrop()).thenReturn(plant);
            tool = tool.copy();
            assertEquals("not_ready", CrossingTool.useOnCrop(tool, reloaded));
            when(plant.canBeHarvested(reloaded)).thenReturn(true);
            assertEquals("no_sticks", CrossingTool.useOnCrop(tool, reloaded));

            for (int slot = 0; slot < 3; slot++) {
                assertTrue(CrossingTool.supplies(tool).insertItem(slot, new ItemStack(sticks, slot + 2), false).isEmpty());
            }
            assertFalse(CrossingTool.supplies(tool).insertItem(0, new ItemStack(Items.STICK), false).isEmpty());
            TileEntityCrop replaced = mock(TileEntityCrop.class);
            when(replaced.getTileData()).thenReturn(new NBTTagCompound());
            when(replaced.getCrop()).thenReturn(plant);
            assertEquals("wrong_target", CrossingTool.useOnCrop(tool, replaced));
            verify(replaced, never()).pick();

            assertEquals("not_ready", CrossingTool.useOnCrop(tool, reloaded));
            assertEquals(2, CrossingTool.supplies(tool).getStackInSlot(0).getCount());
            when(reloaded.pick()).thenReturn(true);
            assertEquals("rearmed", CrossingTool.useOnCrop(tool, reloaded));
            verify(reloaded).setCrossingBase(true);
            assertEquals(1, CrossingTool.supplies(tool).getStackInSlot(0).getCount());
            assertEquals(3, CrossingTool.supplies(tool).getStackInSlot(1).getCount());
            assertEquals(4, CrossingTool.supplies(tool).getStackInSlot(2).getCount());
            assertEquals("rearmed", CrossingTool.useOnCrop(tool, reloaded));

            EntityPlayer player = mock(EntityPlayer.class);
            player.world = mock(World.class);
            player.inventory = new InventoryPlayer(player);
            player.inventory.setInventorySlotContents(0, tool);
            CrossingToolContainer container = CrossingToolContainer.open(player, 0);
            assertNotNull(container);
            assertTrue(container.slotClick(0, 0, ClickType.SWAP, player).isEmpty());
            assertTrue(container.transferStackInSlot(player, 30).isEmpty());
            assertSame(tool, player.inventory.getStackInSlot(0));
            player.inventory.setInventorySlotContents(9, new ItemStack(sticks, 5));
            assertFalse(container.transferStackInSlot(player, 3).isEmpty());
            assertTrue(CrossingTool.supplies(tool).getStackInSlot(0).isEmpty());
            assertEquals(8, CrossingTool.supplies(tool).getStackInSlot(1).getCount());
        } finally {
            instance.set(ItemName.crop_stick, previous);
        }
    }
}
