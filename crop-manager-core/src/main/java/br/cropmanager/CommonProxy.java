package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class CommonProxy implements IGuiHandler {
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == 12) {
            br.cropmanager.chest.inventory.PortableCropInventory portable =
                    br.cropmanager.chest.inventory.PortableCropInventory.open(player, x);
            return portable != null && portable.isUsableByPlayer(player)
                    ? new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(player.inventory, portable) : null;
        }
        if (id == 13) {
            net.minecraft.item.ItemStack carrier = player.inventory.getStackInSlot(x);
            br.cropmanager.chest.inventory.TileCropDatabaseChest remote =
                    br.cropmanager.chest.item.ItemPortableCropDatabase.resolveLinkedChest(player, carrier);
            return remote != null && remote.isRemotelyUsableByPlayer(player)
                    ? new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(player.inventory, remote, x) : null;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == 11 && tile instanceof br.cropmanager.chest.inventory.TileCropDatabaseChest
                && ((br.cropmanager.chest.inventory.TileCropDatabaseChest) tile).isUsableByPlayer(player)) {
            return new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(
                    player.inventory, (br.cropmanager.chest.inventory.TileCropDatabaseChest) tile);
        }
        if (id == 0 && tile instanceof ManagerTile) return new ManagerContainer(player.inventory, (ManagerTile) tile);
        if (id == 2 && tile instanceof FarmTile) return new EquipmentContainer(player.inventory, (FarmTile) tile);
        return null;
    }

    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) { return null; }
    public void registerClientResources() {}
    public void receiveState(ManagerNetwork.State message) {}
    public void showReadout(net.minecraft.nbt.NBTTagCompound tag) {}
    public void receiveChestSettings(int window, net.minecraft.nbt.NBTTagCompound tag) {}
}
