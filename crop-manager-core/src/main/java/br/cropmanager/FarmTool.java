package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public final class FarmTool extends Item {
    public FarmTool(String name) {
        setRegistryName(CropManager.ID, name); setTranslationKey("cropmanager." + name);
        setCreativeTab(CreativeTabs.TOOLS); setMaxStackSize(1);
    }
    @Override public void addInformation(ItemStack stack, World world, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(net.minecraft.client.resources.I18n.format("cropmanager.probe_link_help"));
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("Linked")) {
            BlockPos linked = BlockPos.fromLong(tag.getLong("Linked"));
            tooltip.add(net.minecraft.client.resources.I18n.format("cropmanager.probe_target_tip", linked.getX(), linked.getY(), linked.getZ(), tag.getInteger("Dimension")));
        }
    }
    private static void tell(EntityPlayer player, String key) { player.sendMessage(new TextComponentTranslation("cropmanager." + key)); }
    @Override public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float x, float y, float z, EnumHand hand) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(MonitorTile.supported(tile) || tile instanceof MonitorTile || tile instanceof TileEntityCrop)) return EnumActionResult.PASS;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        if (!player.canPlayerEdit(pos, side, player.getHeldItem(hand)) || !world.isBlockModifiable(player, pos)) return EnumActionResult.FAIL;
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        if (MonitorTile.supported(tile)) {
            if (player.isSneaking()) { tag.setLong("Linked", pos.toLong()); tag.setInteger("Dimension", world.provider.getDimension()); stack.setTagCompound(tag); tell(player, "probe_linked"); }
            else tell(player, "probe_link_help");
        } else if (tile instanceof MonitorTile) {
            if (!tag.hasKey("Linked")) tell(player, "probe_link_help");
            else { ((MonitorTile)tile).link(BlockPos.fromLong(tag.getLong("Linked")), tag.getInteger("Dimension")); tell(player, "monitor_linked"); }
        } else if (tile instanceof TileEntityCrop) {
            NBTTagCompound data = cropReadout((TileEntityCrop)tile);
            ManagerNetwork.CHANNEL.sendTo(new ManagerNetwork.Readout(data), (EntityPlayerMP)player);
        }
        return EnumActionResult.SUCCESS;
    }
    static NBTTagCompound cropReadout(TileEntityCrop crop) {
        NBTTagCompound data = new NBTTagCompound();
            boolean known = crop.getScanLevel() >= 4;
            data.setBoolean("Analyzed", known);
            data.setString("Species", crop.getCrop() != null && crop.getScanLevel() >= 1 ? crop.getCrop().getOwner() + ":" + crop.getCrop().getId() : "?");
            data.setInteger("Size", crop.getCurrentSize()); data.setInteger("MaxSize", crop.getCrop() == null ? 0 : crop.getCrop().getMaxSize());
            data.setInteger("Water", crop.getStorageWater()); data.setInteger("Nutrients", crop.getStorageNutrients()); data.setInteger("Weed", crop.getStorageWeedEX());
            data.setInteger("Light", crop.getLightLevel()); data.setInteger("Humidity", crop.getTerrainHumidity()); data.setInteger("TerrainNutrients", crop.getTerrainNutrients()); data.setInteger("Air", crop.getTerrainAirQuality());
            if (known) data.setIntArray("Stats", new int[] {crop.getStatGrowth(), crop.getStatGain(), crop.getStatResistance()});
        return data;
    }
}