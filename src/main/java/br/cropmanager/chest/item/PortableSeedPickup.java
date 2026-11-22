package br.cropmanager.chest.item;

import br.cropmanager.CropManager;
import br.cropmanager.chest.inventory.ContainerCropDatabaseChest;
import br.cropmanager.chest.inventory.PortableCropInventory;
import br.cropmanager.chest.inventory.SeedInventory;
import br.cropmanager.chest.inventory.TileCropDatabaseChest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CropManager.ID)
public final class PortableSeedPickup {
    private PortableSeedPickup() { }

    /** Copies input, tries enabled portables in slot order, then the offhand. Never changes the supplied stack. */
    public static ItemStack store(EntityPlayer player, ItemStack input) {
        if (player.world.isRemote || !SeedInventory.accepts(input)) return input.copy();
        ItemStack remaining = input.copy();
        for (int index = 0; index < 37 && !remaining.isEmpty(); index++) {
            int slot = index == 36 ? 40 : index;
            ItemStack carrier = player.inventory.getStackInSlot(slot);
            if (!PortableCropInventory.isPickupEnabled(carrier)) continue;
            if (ItemUpgradeLinker.hasLink(carrier)) {
                TileCropDatabaseChest linked = ItemPortableCropDatabase.resolveLinkedChest(player, carrier);
                if (linked != null) remaining = linked.seeds().insertAnywhere(remaining, false);
                continue;
            }
            PortableCropInventory storage = null;
            if (player.openContainer instanceof ContainerCropDatabaseChest) {
                storage = ((ContainerCropDatabaseChest) player.openContainer).getPortableInventory();
                if (storage != null && (!storage.isCarrierSlot(slot) || !storage.isUsableByPlayer(player))) storage = null;
            }
            if (storage == null) storage = PortableCropInventory.open(player, slot);
            if (storage != null) remaining = storage.seeds().insertAnywhere(remaining, false);
        }
        return remaining;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        EntityItem entity = event.getItem();
        if (event.isCanceled() || event.getResult() != Event.Result.DEFAULT || player.world.isRemote
                || player.isDead || entity.isDead || entity.cannotPickup()) return;
        String owner = entity.getOwner();
        if (owner != null && !owner.equals(player.getName()) && entity.lifespan - entity.getAge() > 200) return;
        ItemStack original = entity.getItem().copy();
        if (!SeedInventory.accepts(original)) return;
        ItemStack remaining = store(player, original);
        if (remaining.getCount() == original.getCount()) return; // Leave normal pickup completely untouched.
        event.setCanceled(true);
        if (!remaining.isEmpty()) player.inventory.addItemStackToInventory(remaining);
        int taken = original.getCount() - remaining.getCount();
        entity.setItem(remaining);
        ItemStack picked = original.copy();
        picked.setCount(taken);
        FMLCommonHandler.instance().firePlayerItemPickupEvent(player, entity, picked);
        StatBase statistic = StatList.getObjectsPickedUpStats(original.getItem());
        if (statistic != null) player.addStat(statistic, taken);
        if (remaining.isEmpty()) { player.onItemPickup(entity, taken); entity.setDead(); }
    }
}
