package br.cropmanager;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Defers block replacement until chunk/TileEntity loading has finished. */
@Mod.EventBusSubscriber(modid=CropManager.ID)
public final class LegacyMigration {
    private static final Set<net.minecraft.tileentity.TileEntity> pending=new LinkedHashSet<>();
    private LegacyMigration() {}
    static void request(net.minecraft.tileentity.TileEntity tile) { pending.add(tile); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        int count=0;
        for(net.minecraft.tileentity.TileEntity tile:pending.toArray(new net.minecraft.tileentity.TileEntity[0])) {
            if(count++==32)break; pending.remove(tile);
            if(!tile.isInvalid() && tile.getWorld()!=null && tile.getWorld().isBlockLoaded(tile.getPos(),false)
                    && tile.getWorld().getTileEntity(tile.getPos())==tile) {
                if(tile instanceof ManagerTile && !((ManagerTile)tile).advanced)((ManagerTile)tile).migrate();
                else if(tile instanceof LegacyRecoveryTile)((LegacyRecoveryTile)tile).retire();
            }
        }
    }
    @SubscribeEvent public static void unload(WorldEvent.Unload e) { if(!e.getWorld().isRemote)pending.removeIf(tile->tile.getWorld()==e.getWorld()); }
}
