package br.cropmanager.chest;

import br.cropmanager.chest.inventory.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.*;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChestSettingsTest {
    @BeforeClass public static void bootstrap() {
        SeedFixtures.bootstrap();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(LegacyModularChestTile.class,new net.minecraft.util.ResourceLocation("cropdatabasechest","modular_chest"));
    }
    private World world() { World w=mock(World.class);when(w.getBlockState(any(BlockPos.class))).thenReturn(Blocks.CHEST.getDefaultState());return w; }
    private TileCropDatabaseChest[] tiers() { return new TileCropDatabaseChest[]{new TileCropDatabaseChest(),new TileCropDatabaseChest125(),new TileCropDatabaseChest250()}; }
    @Test public void tierThreeAndLegacyChestsPersistIndividualFavoritesWithoutChangingSeedNbt() {
        for(TileCropDatabaseChest tile:tiers()){
            ItemStack seed=SeedFixtures.bag(7,4);NBTTagCompound before=seed.serializeNBT();tile.seeds().insertItem(0,seed,false);
            tile.toggleFavorite(0);tile.setCollectingEnabled(true);tile.setRedstoneMode(2);
            TileCropDatabaseChest copy=new TileCropDatabaseChest();
            copy.readFromNBT(tile.writeToNBT(new NBTTagCompound()));
            assertEquals(500,copy.getActiveCapacity());assertTrue(copy.isFavorite(SeedFixtures.bag(7,4)));assertFalse(copy.isFavorite(SeedFixtures.bag(9,4)));
            assertTrue(copy.collectingEnabled());assertEquals(2,copy.redstoneMode());assertEquals(before,copy.getStackInSlot(0).serializeNBT());
        }
    }
    @Test public void redstoneModesControlBothAutomationApisButNotManualAccess() {
        for(TileCropDatabaseChest tile:tiers()){
            World w=world();tile.setWorld(w);tile.setPos(BlockPos.ORIGIN);
            tile.setRedstoneMode(1);when(w.isBlockPowered(BlockPos.ORIGIN)).thenReturn(true);
            assertEquals(0,tile.getSlotsForFace(EnumFacing.UP).length);assertFalse(tile.automation().insertItem(0,SeedFixtures.bag(1,4),false).isEmpty());
            assertTrue(tile.seeds().insertItem(0,SeedFixtures.bag(1,4),false).isEmpty());assertTrue(tile.automation().extractItem(0,1,false).isEmpty());
            tile.setRedstoneMode(2);assertFalse(tile.automation().extractItem(0,1,false).isEmpty());
            when(w.isBlockPowered(BlockPos.ORIGIN)).thenReturn(false);assertEquals(0,tile.getSlotsForFace(EnumFacing.NORTH).length);
            tile.setRedstoneMode(0);assertEquals(tile.getSizeInventory(),tile.getSlotsForFace(EnumFacing.DOWN).length);
        }
    }
    @Test public void allTierContainersHaveTabsWithoutExtraSlotsAndSupportBulkDeposit() {
        for(TileCropDatabaseChest tile:tiers()){
            World w=world();EntityPlayer player=mock(EntityPlayer.class);player.world=w;player.inventory=new InventoryPlayer(player);tile.setWorld(w);tile.setPos(BlockPos.ORIGIN);when(w.getTileEntity(BlockPos.ORIGIN)).thenReturn(tile);
            player.inventory.setInventorySlotContents(0,SeedFixtures.bag(7,4));player.inventory.setInventorySlotContents(1,new ItemStack(net.minecraft.init.Items.DIAMOND));
            ContainerCropDatabaseChest container=new ContainerCropDatabaseChest(player.inventory,tile);
            assertEquals(tile.getSizeInventory()+36,container.inventorySlots.size());assertTrue(container.hasManagement());assertTrue(container.enchantItem(player,112));
            assertTrue(player.inventory.getStackInSlot(0).isEmpty());assertEquals(net.minecraft.init.Items.DIAMOND,player.inventory.getStackInSlot(1).getItem());
            assertTrue(container.enchantItem(player,111));assertEquals(-1,container.getPosition(0));tile.toggleFavorite(0);container.detectAndSendChanges();assertEquals(0,container.getPosition(0));
            assertTrue(container.enchantItem(player,113));assertTrue(container.collectingEnabled());assertTrue(container.enchantItem(player,114));assertEquals(1,container.redstoneMode());
        }
    }
    @Test public void collectorRespectsOwnerDelayAndCapacity() {
        TileCropDatabaseChest tile=new TileCropDatabaseChest125();World w=world();tile.setWorld(w);tile.setPos(BlockPos.ORIGIN);tile.setCollectingEnabled(true);
        EntityItem free=mock(EntityItem.class),owned=mock(EntityItem.class),delayed=mock(EntityItem.class);
        when(free.getItem()).thenReturn(SeedFixtures.bag(3,4));when(owned.getItem()).thenReturn(SeedFixtures.bag(4,4));when(owned.getOwner()).thenReturn("Someone");when(delayed.cannotPickup()).thenReturn(true);
        when(w.getEntitiesWithinAABB(eq(EntityItem.class),any(AxisAlignedBB.class))).thenReturn(java.util.Arrays.asList(free,owned,delayed));
        doAnswer(call->{free.isDead=true;return null;}).when(free).setDead();
        tile.update();assertEquals(3,tile.getStackInSlot(0).getTagCompound().getInteger("identity"));tile.update();assertTrue(tile.getStackInSlot(1).isEmpty());verify(owned,never()).setDead();verify(delayed,never()).setDead();
        for(int i=1;i<500;i++)tile.seeds().insertItem(i,SeedFixtures.bag(i,4),false);
        free.isDead=false;tile.update();assertFalse(tile.getStackInSlot(124).isEmpty());verify(free,times(1)).setDead();
    }
    @Test public void removedModularDataRetainsItsLastSeedAndFavorites() {
        LegacyModularChestTile tile=new LegacyModularChestTile();tile.seeds().insertItem(499,SeedFixtures.bag(499,4),false);tile.toggleFavorite(499);
        NBTTagCompound old=tile.writeToNBT(new NBTTagCompound());old.setTag("Favorites",old.getCompoundTag("Settings").getTagList("Favorites",8));old.removeTag("Settings");old.setTag("Upgrades",new NBTTagCompound());
        LegacyModularChestTile loaded=new LegacyModularChestTile();loaded.readFromNBT(old);
        assertEquals(500,loaded.getActiveCapacity());assertEquals(499,loaded.getStackInSlot(499).getTagCompound().getInteger("identity"));assertTrue(loaded.isFavorite(loaded.getStackInSlot(499)));
    }
}
