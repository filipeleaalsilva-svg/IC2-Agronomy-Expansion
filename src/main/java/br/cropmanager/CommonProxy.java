package br.cropmanager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class CommonProxy implements IGuiHandler {
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == CrossingTool.GUI) return CrossingToolContainer.open(player, x);
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
            br.cropmanager.chest.inventory.PortableCropInventory portable =
                    br.cropmanager.chest.inventory.PortableCropInventory.open(player, x);
            if (remote == null || portable == null || !remote.isRemotelyUsableByPlayer(player)) return null;
            portable.transferSeedsTo(remote);
            return new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(player.inventory, remote, x);
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == 11 && tile instanceof br.cropmanager.chest.inventory.TileCropDatabaseChest
                && ((br.cropmanager.chest.inventory.TileCropDatabaseChest) tile).isUsableByPlayer(player)) {
            return new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(
                    player.inventory, (br.cropmanager.chest.inventory.TileCropDatabaseChest) tile);
        }
        if (id == DiagonalSprinklerTile.GUI && tile instanceof DiagonalSprinklerTile && ((FarmTile) tile).usable(player)) return new EuWifiContainer(player.inventory, (FarmTile) tile);
        if (id == BiogasSprayerTile.GUI && tile instanceof BiogasSprayerTile && ((FarmTile) tile).usable(player)) return new EuWifiContainer(player.inventory, (FarmTile) tile);
        if (id == BiogasGeneratorTile.GUI && tile instanceof BiogasGeneratorTile && ((FarmTile) tile).usable(player)) return new EuWifiContainer(player.inventory, (FarmTile) tile);
        if (id == 15 && tile instanceof AgriculturalReservoirTile
                && ((AgriculturalReservoirTile) tile).formed && ((AgriculturalReservoirTile) tile).usable(player))
            return new ReservoirContainer(player.inventory, (AgriculturalReservoirTile) tile);
        if (id == 16 && tile instanceof IndustrialTankTile
                && ((IndustrialTankTile) tile).formed && ((IndustrialTankTile) tile).usable(player))
            return new IndustrialTankContainer(player.inventory, (IndustrialTankTile) tile);
        if (id == 17 && tile instanceof AgriculturalSiloTile
                && ((AgriculturalSiloTile) tile).formed && ((AgriculturalSiloTile) tile).usable(player))
            return new AgriculturalSiloContainer(player.inventory, (AgriculturalSiloTile) tile);
        if ((id == 18 && tile instanceof EuWifiPlugTile || id == 19 && tile instanceof EuWifiPointTile)
                && ((FarmTile) tile).usable(player))
            return new EuWifiContainer(player.inventory, (FarmTile) tile);
        if (id == 20 && tile instanceof EuDishAntennaTile && ((EuDishAntennaTile) tile).usable(player))
            return new EuDishContainer(player.inventory, (EuDishAntennaTile) tile);
        if (id == 0 && tile instanceof ManagerTile) return new ManagerContainer(player.inventory, (ManagerTile)tile);
        if (id == 8 && tile instanceof WeedExFabricatorTile) return new EquipmentContainer(player.inventory, (FarmTile)tile);
        if (id == 9 && tile instanceof GrinGrinderTile || id == 10 && tile instanceof GrinExtractorTile || id == 14 && tile instanceof SeedAnalyzerTile || id == HydroponicChamberTile.GUI && tile instanceof HydroponicChamberTile) return new EquipmentContainer(player.inventory, (FarmTile)tile);
        return (id == 2 && tile instanceof MonitorTile || id == 3 && tile instanceof ComposterTile || id == 7 && tile instanceof WeederTile) ? new EquipmentContainer(player.inventory, (FarmTile)tile) : null;
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) { return null; }
    public void registerClientResources() {}
    public void receiveState(ManagerNetwork.State message) {}
    public void showReadout(net.minecraft.nbt.NBTTagCompound tag) {}
    public void receiveChestSettings(int window, net.minecraft.nbt.NBTTagCompound tag) {}
    public void receiveChestView(br.cropmanager.chest.network.ChestViewMessage message) {}
    public void openGuideBook() {}
}
