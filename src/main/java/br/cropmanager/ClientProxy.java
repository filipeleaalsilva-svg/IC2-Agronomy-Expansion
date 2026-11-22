package br.cropmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = CropManager.ID, value = Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @Override public void registerClientResources() { FarmGuiStyle.registerPalette(); }
    @SubscribeEvent public static void stitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(CropManager.GRIN_EXTRACT.getStill());
        event.getMap().registerSprite(CropManager.GRIN_EXTRACT.getFlowing());
    }
    @SubscribeEvent public static void models(ModelRegistryEvent event) {
        net.minecraft.client.renderer.block.statemap.StateMap machineMap =
                new net.minecraft.client.renderer.block.statemap.StateMap.Builder()
                        .ignore(ManagerBlock.HALF, ManagerBlock.NORTH, ManagerBlock.EAST, ManagerBlock.SOUTH, ManagerBlock.WEST, ManagerBlock.UP, ManagerBlock.DOWN).build();
        net.minecraft.client.renderer.block.statemap.StateMap pipeMap =
                new net.minecraft.client.renderer.block.statemap.StateMap.Builder()
                        .ignore(ManagerBlock.HALF, ManagerBlock.FACING, ManagerBlock.ACTIVE).build();
        for (ManagerBlock block : CropManager.BLOCKS) {
            ModelLoader.setCustomStateMapper(block, block == CropManager.THERMAL_PIPE ? pipeMap : machineMap);
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.MANUAL_COMPOSTER), 0, new ModelResourceLocation(CropManager.MANUAL_COMPOSTER.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.RADIOACTIVE_SOIL), 0, new ModelResourceLocation(CropManager.RADIOACTIVE_SOIL.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SPRINKLER_MK1), 0, new ModelResourceLocation(CropManager.SPRINKLER_MK1.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SPRINKLER_MK2), 0, new ModelResourceLocation(CropManager.SPRINKLER_MK2.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.SPRINKLER_MK3), 0, new ModelResourceLocation(CropManager.SPRINKLER_MK3.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CropManager.DATABASE_CHEST), 0, new ModelResourceLocation(CropManager.DATABASE_CHEST.getRegistryName(), "inventory"));
        for (Item item : new Item[] {CropManager.PROBE, CropManager.WEED_KILLER, CropManager.WIRELESS_RANGE, CropManager.HARVEST_TRANSFER, CropManager.WIRELESS_EU, CropManager.GRINDING_BLADE, CropManager.ORGANIC_COMPOST, CropManager.CATALYTIC_FERTILIZER, CropManager.MUTAGENIC_POWDER, CropManager.NITRO_ESSENCE, CropManager.BIO_FIBER, CropManager.PORTABLE_DATABASE, CropManager.UPGRADE_LINKER}) ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(MonitorTile.class, new MonitorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(br.cropmanager.chest.inventory.TileCropDatabaseChest.class, new br.cropmanager.chest.client.CropChestRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(br.cropmanager.chest.inventory.TileCropDatabaseChest250.class, new br.cropmanager.chest.client.CropChestRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(br.cropmanager.chest.inventory.TileCropDatabaseChest125.class, new br.cropmanager.chest.client.CropChestRenderer());
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == 12) {
            br.cropmanager.chest.inventory.PortableCropInventory portable =
                    br.cropmanager.chest.inventory.PortableCropInventory.open(player, x);
            return portable == null ? null : new br.cropmanager.chest.client.GuiCropDatabaseChest(
                    new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(player.inventory, portable));
        }
        if (id == 13) {
            return new br.cropmanager.chest.client.GuiCropDatabaseChest(
                    new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(
                            player.inventory, new br.cropmanager.chest.inventory.TileCropDatabaseChest(), x));
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == 11 && tile instanceof br.cropmanager.chest.inventory.TileCropDatabaseChest) {
            return new br.cropmanager.chest.client.GuiCropDatabaseChest(
                    new br.cropmanager.chest.inventory.ContainerCropDatabaseChest(
                            player.inventory, (br.cropmanager.chest.inventory.TileCropDatabaseChest) tile));
        }
        if (id == 0 && tile instanceof ManagerTile) return new ManagerGui(player.inventory, (ManagerTile)tile);
        if (id == 8 && tile instanceof WeedExFabricatorTile) return new EquipmentGui(player.inventory, (FarmTile)tile);
        if (id == 9 && tile instanceof GrinGrinderTile || id == 10 && tile instanceof GrinExtractorTile || id == 14 && tile instanceof SeedAnalyzerTile) return new EquipmentGui(player.inventory, (FarmTile)tile);
        return (id == 2 && tile instanceof MonitorTile || id == 3 && tile instanceof ComposterTile || id == 7 && tile instanceof WeederTile) ? new EquipmentGui(player.inventory, (FarmTile)tile) : null;
    }
    @Override public void receiveState(ManagerNetwork.State message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.player == null || message.tag == null) return;
            FarmTile tile = FarmTile.openMachine(mc.player);
            if (tile != null && mc.player.openContainer.windowId == message.window && tile.getPos().equals(message.pos)) {
                tile.acceptGuiState(message.tag);
                if (tile instanceof ManagerTile || tile instanceof WeederTile) AreaPreview.refresh(tile);
            }
        });
    }
    @Override public void showReadout(net.minecraft.nbt.NBTTagCompound tag) {
        Minecraft.getMinecraft().addScheduledTask(() -> { if (tag != null) Minecraft.getMinecraft().displayGuiScreen(new ProbeGui(tag)); });
    }
    @Override public void receiveChestSettings(int window, net.minecraft.nbt.NBTTagCompound tag) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null && tag != null && player.openContainer.windowId == window
                    && player.openContainer instanceof br.cropmanager.chest.inventory.ContainerCropDatabaseChest) {
                ((br.cropmanager.chest.inventory.ContainerCropDatabaseChest) player.openContainer).readManagement(tag);
            }
        });
    }
}
