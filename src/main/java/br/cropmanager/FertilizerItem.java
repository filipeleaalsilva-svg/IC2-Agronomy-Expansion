package br.cropmanager;

import ic2.api.crops.ICropTile;
import ic2.core.crop.TileEntityCrop;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Custom fertilizers compatible with IC2 crops and the Advanced Crop Manager. */
public final class FertilizerItem extends Item {
    public static final String CATALYZED_KEY = "cm_catalyzed";

    public enum Kind {
        ORGANIC("cropmanager.organic_compost_help"),
        CATALYTIC("cropmanager.catalytic_fertilizer_help");

        public final String tooltipKey;
        Kind(String tooltipKey) { this.tooltipKey = tooltipKey; }
    }

    public final Kind kind;

    public FertilizerItem(String name, Kind kind) {
        this.kind = kind;
        setRegistryName(CropManager.ID, name);
        setTranslationKey("cropmanager." + name);
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format(kind.tooltipKey));
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityCrop)) return EnumActionResult.PASS;
        ItemStack stack = player.getHeldItem(hand);
        TileEntityCrop crop = (TileEntityCrop) tile;
        if (world.isRemote) return canApply(stack, crop) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        if (!player.canPlayerEdit(pos, side, stack) || !world.isBlockModifiable(player, pos)) return EnumActionResult.FAIL;
        if (!applyToCrop(stack, crop, true)) return EnumActionResult.PASS;
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        crop.dirty = true;
        crop.updateState();
        world.markChunkDirty(pos, crop);
        world.playEvent(2005, pos, 0);
        return EnumActionResult.SUCCESS;
    }

    public static boolean canApply(ItemStack stack, ICropTile crop) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FertilizerItem) || crop == null) return false;
        Kind k = ((FertilizerItem) stack.getItem()).kind;
        if (k == Kind.ORGANIC) {
            return crop.getStorageNutrients() < 100 || crop.getStorageWater() < 100;
        }
        NBTTagCompound custom = crop.getCustomData();
        int charges = custom == null ? 0 : custom.getInteger(CATALYZED_KEY);
        return crop.getStorageNutrients() < 100 || charges < ManagerConfig.catalyticCharges;
    }

    public static boolean applyToCrop(ItemStack stack, ICropTile crop, boolean manual) {
        if (!canApply(stack, crop)) return false;
        Kind k = ((FertilizerItem) stack.getItem()).kind;
        if (k == Kind.ORGANIC) {
            crop.setStorageNutrients(Math.min(100, crop.getStorageNutrients() + ManagerConfig.organicNutrients));
            crop.setStorageWater(Math.min(100, crop.getStorageWater() + ManagerConfig.organicWater));
            if (crop instanceof TileEntityCrop) ((TileEntityCrop) crop).dirty = true;
            return true;
        }
        crop.setStorageNutrients(Math.min(100, crop.getStorageNutrients() + ManagerConfig.catalyticNutrients));
        if (crop.getCrop() != null && crop.getCurrentSize() < crop.getCrop().getMaxSize()) {
            crop.setGrowthPoints(crop.getGrowthPoints() + ManagerConfig.catalyticGrowthBoost);
        }
        NBTTagCompound custom = crop.getCustomData();
        if (custom != null) custom.setInteger(CATALYZED_KEY, ManagerConfig.catalyticCharges);
        if (crop instanceof TileEntityCrop) ((TileEntityCrop) crop).dirty = true;
        return true;
    }

    /** Applies either a custom FertilizerItem or native IC2 fertilizer to a TileEntityCrop. */
    public static boolean applyAny(ItemStack stack, TileEntityCrop crop, boolean manual) {
        if (stack.isEmpty() || crop == null) return false;
        if (stack.getItem() instanceof FertilizerItem) return applyToCrop(stack, crop, manual);
        return crop.applyFertilizer(manual);
    }

    public static int catalyzedCharges(ICropTile crop) {
        if (crop == null || crop.getCustomData() == null) return 0;
        return Math.max(0, crop.getCustomData().getInteger(CATALYZED_KEY));
    }

    public static void consumeCatalystCharge(ICropTile crop) {
        if (crop == null || crop.getCustomData() == null) return;
        int current = crop.getCustomData().getInteger(CATALYZED_KEY);
        if (current > 0) crop.getCustomData().setInteger(CATALYZED_KEY, current - 1);
    }
}
