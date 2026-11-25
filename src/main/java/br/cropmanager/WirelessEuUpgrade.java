package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.tile.IEnergyStorage;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.Energy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IC2-compatible upgrade that wirelessly draws EU from a linked IC2 energy storage
 * (BatBox, CESU, MFE, MFSU, etc.) into any IUpgradableBlock machine.
 *
 * <p><b>Wireless Link Rules:</b></p>
 * <ul>
 *   <li><b>Dimension:</b> Stored in NBT tag {@code WirelessSource.Dim}. Transfer only occurs
 *       if the host machine and the source storage are in the exact same dimension. Cross-dimension
 *       transfer is blocked.</li>
 *   <li><b>Range:</b> Unlimited distance within the same dimension.</li>
 *   <li><b>Chunk Loading:</b> Requires the source chunk to be already loaded ({@code world.isBlockLoaded(pos, false)}).
 *       Does not keep chunks loaded and will never force-load chunks.</li>
 *   <li><b>Target & Storage:</b> Linked by Shift + Right Clicking an {@link ic2.api.tile.IEnergyStorage}.
 *       Supplies energy to any IC2 {@link ic2.api.upgrade.IUpgradableBlock} with an {@code Energy} component
 *       or Crop Manager machine with a registered sink. If the source block is broken or missing,
 *       energy transfer safely no-ops without throwing exceptions.</li>
 * </ul>
 */
public final class WirelessEuUpgrade extends Item implements IUpgradeItem {
    public static final String LINK_TAG = "WirelessSource";
    public static final String WIFI_CHANNEL_TAG = "WifiChannel";
    public static final String WIFI_OWNER_TAG = "WifiOwner";
    public static final String DISH_TAG = "DishSource";

    public WirelessEuUpgrade() {
        setRegistryName(CropManager.ID, "wireless_eu_upgrade");
        setTranslationKey(CropManager.ID + ".wireless_eu_upgrade");
        setCreativeTab(CropManager.TAB);
        setMaxStackSize(MonitorTile.MAX_WIRELESS_EU_UPGRADES);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!player.isSneaking()) return EnumActionResult.PASS;
        TileEntity tile = world.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);
        if (tile instanceof EuWifiPlugTile || tile instanceof EuWifiPointTile) {
            if (world.isRemote) return EnumActionResult.SUCCESS;
            int channel = tile instanceof EuWifiPlugTile
                    ? ((EuWifiPlugTile) tile).channel
                    : ((EuWifiPointTile) tile).channel;
            if (!WirelessEuGridData.get(world).canAccess(channel, player.getUniqueID())) {
                return EnumActionResult.FAIL;
            }
            NBTTagCompound root = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
            root.removeTag(LINK_TAG);
            root.removeTag(DISH_TAG);
            root.setInteger(WIFI_CHANNEL_TAG, channel);
            root.setUniqueId(WIFI_OWNER_TAG, player.getUniqueID());
            stack.setTagCompound(root);
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.wireless_eu_linked_wifi", channel), true);
            return EnumActionResult.SUCCESS;
        }
        if (tile instanceof EuDishAntennaTile) {
            if (world.isRemote) return EnumActionResult.SUCCESS;
            NBTTagCompound root = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
            root.removeTag(LINK_TAG);
            root.removeTag(WIFI_CHANNEL_TAG);
            NBTTagCompound dish = new NBTTagCompound();
            dish.setInteger("Dim", world.provider.getDimension());
            dish.setInteger("X", pos.getX());
            dish.setInteger("Y", pos.getY());
            dish.setInteger("Z", pos.getZ());
            root.setTag(DISH_TAG, dish);
            stack.setTagCompound(root);
            player.sendStatusMessage(new TextComponentTranslation("cropmanager.wireless_eu_linked_dish",
                    pos.getX(), pos.getY(), pos.getZ()), true);
            return EnumActionResult.SUCCESS;
        }
        if (!(tile instanceof IEnergyStorage)) return EnumActionResult.PASS;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        NBTTagCompound root = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        root.removeTag(WIFI_CHANNEL_TAG);
        root.removeTag(DISH_TAG);
        NBTTagCompound link = new NBTTagCompound();
        link.setInteger("Dim", world.provider.getDimension());
        link.setInteger("X", pos.getX());
        link.setInteger("Y", pos.getY());
        link.setInteger("Z", pos.getZ());
        root.setTag(LINK_TAG, link);
        stack.setTagCompound(root);
        player.sendStatusMessage(new TextComponentTranslation("cropmanager.wireless_eu_linked",
                pos.getX(), pos.getY(), pos.getZ()), true);
        return EnumActionResult.SUCCESS;
    }

    static boolean acceptsDish(ItemStack stack, int dim, BlockPos dishPos) {
        if (stack.isEmpty() || !(stack.getItem() instanceof WirelessEuUpgrade)) return false;
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null || (!tag.hasKey(LINK_TAG) && !tag.hasKey(WIFI_CHANNEL_TAG) && !tag.hasKey(DISH_TAG))
                || isLinkedToDish(stack, dim, dishPos);
    }

    public static boolean isLinkedToDish(ItemStack stack, int dim, BlockPos dishPos) {
        if (stack.isEmpty() || !stack.hasTagCompound() || !stack.getTagCompound().hasKey(DISH_TAG, 10)) return false;
        NBTTagCompound dish = stack.getTagCompound().getCompoundTag(DISH_TAG);
        return dish.getInteger("Dim") == dim
                && dish.getInteger("X") == dishPos.getX()
                && dish.getInteger("Y") == dishPos.getY()
                && dish.getInteger("Z") == dishPos.getZ();
    }

    @Override
    public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> types) {
        return types != null && !types.isEmpty();
    }

    @Override
    public boolean onTick(ItemStack stack, IUpgradableBlock parent) {
        if (!(parent instanceof TileEntity)) return false;
        TileEntity target = (TileEntity) parent;
        World world = target.getWorld();
        if (world == null || world.isRemote || !stack.hasTagCompound()) return false;
        double free = freeEnergy(parent);
        if (free <= 0) return false;
        NBTTagCompound root = stack.getTagCompound();
        int maxRate = Math.max(1, ManagerConfig.monitorWirelessEuRatePerUpgrade * Math.max(1, stack.getCount()));
        if (root.hasKey(WIFI_CHANNEL_TAG, 3)) {
            int channel = WirelessEuGridData.clampChannel(root.getInteger(WIFI_CHANNEL_TAG));
            java.util.UUID owner = root.hasUniqueId(WIFI_OWNER_TAG) ? root.getUniqueId(WIFI_OWNER_TAG) : null;
            double want = Math.min(maxRate, free);
            WirelessEuGridData grid = WirelessEuGridData.get(world);
            double available = grid.extractEu(channel, want, world.provider.getDimension(), owner);
            if (available <= 0) return false;
            double accepted = injectEnergy(parent, available);
            if (accepted < available) {
                grid.refundEu(channel, available - accepted);
            }
            return accepted > 0;
        }
        if (!root.hasKey(LINK_TAG, 10)) return false;
        NBTTagCompound link = root.getCompoundTag(LINK_TAG);
        if (link.getInteger("Dim") != world.provider.getDimension()) return false;
        BlockPos sourcePos = new BlockPos(link.getInteger("X"), link.getInteger("Y"), link.getInteger("Z"));
        if (!world.isBlockLoaded(sourcePos, false)) return false;
        TileEntity sourceTile = world.getTileEntity(sourcePos);
        if (!(sourceTile instanceof IEnergyStorage) || sourceTile == target) return false;
        IEnergyStorage storage = (IEnergyStorage) sourceTile;
        int want = Math.min(Math.min(maxRate, storage.getStored()), (int) Math.floor(free));
        if (want <= 0) return false;
        double accepted = injectEnergy(parent, want);
        if (accepted <= 0) return false;
        storage.addEnergy(-(int) Math.round(accepted));
        return true;
    }

    static double freeEnergy(IUpgradableBlock parent) {
        if (parent instanceof TileEntityBlock) {
            Energy energy = ((TileEntityBlock) parent).getComponent(Energy.class);
            return energy == null ? 0 : Math.max(0, energy.getFreeEnergy());
        }
        BasicSink sink = sinkOf(parent);
        return sink == null ? 0 : Math.max(0, sink.getFreeCapacity());
    }

    static double injectEnergy(IUpgradableBlock parent, double amount) {
        if (parent instanceof TileEntityBlock) {
            Energy energy = ((TileEntityBlock) parent).getComponent(Energy.class);
            if (energy == null) return 0;
            double free = energy.getFreeEnergy();
            double give = Math.min(amount, free);
            if (give > 0) energy.addEnergy(give);
            return give;
        }
        BasicSink sink = sinkOf(parent);
        if (sink == null) return 0;
        double give = Math.min(amount, sink.getFreeCapacity());
        if (give > 0) {
            sink.setEnergyStored(sink.getEnergyStored() + give);
            ((TileEntity) parent).markDirty();
        }
        return give;
    }

    private static BasicSink sinkOf(IUpgradableBlock parent) {
        return parent instanceof FarmTile ? ((FarmTile) parent).energySink() : null;
    }

    @Override
    public Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock parent, Collection<ItemStack> output) {
        return Collections.emptyList();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager.wireless_eu_help"));
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(WIFI_CHANNEL_TAG, 3)) {
            tooltip.add(I18n.format("cropmanager.wireless_eu_target_wifi", stack.getTagCompound().getInteger(WIFI_CHANNEL_TAG)));
        } else if (stack.hasTagCompound() && stack.getTagCompound().hasKey(DISH_TAG, 10)) {
            NBTTagCompound dish = stack.getTagCompound().getCompoundTag(DISH_TAG);
            tooltip.add(I18n.format("cropmanager.wireless_eu_target_dish",
                    dish.getInteger("X"), dish.getInteger("Y"), dish.getInteger("Z")));
        } else if (stack.hasTagCompound() && stack.getTagCompound().hasKey(LINK_TAG, 10)) {
            NBTTagCompound link = stack.getTagCompound().getCompoundTag(LINK_TAG);
            tooltip.add(I18n.format("cropmanager.wireless_eu_target",
                    link.getInteger("X"), link.getInteger("Y"), link.getInteger("Z")));
        } else {
            tooltip.add(I18n.format("cropmanager.wireless_eu_unlinked"));
        }
    }
}
