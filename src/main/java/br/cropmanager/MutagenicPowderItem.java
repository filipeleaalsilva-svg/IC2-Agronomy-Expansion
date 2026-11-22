package br.cropmanager;

import ic2.core.crop.TileEntityCrop;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Consumable powder that accelerates IC2 cross-breeding on double crop sticks while suppressing weeds. */
public final class MutagenicPowderItem extends Item {
    public MutagenicPowderItem() {
        setRegistryName(CropManager.ID, "mutagenic_powder");
        setTranslationKey("cropmanager.mutagenic_powder");
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager.mutagenic_powder_help"));
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || stack.getItem() != this) return EnumActionResult.PASS;
        if (pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos, false)) return EnumActionResult.PASS;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityCrop)) return EnumActionResult.PASS;
        TileEntityCrop crop = (TileEntityCrop) tile;
        if (world.isRemote) {
            return CrossBreedingHelper.canAccelerate(world, crop) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }
        if (!player.canPlayerEdit(pos, side, stack) || !world.isBlockModifiable(player, pos)) {
            return EnumActionResult.FAIL;
        }
        if (!CrossBreedingHelper.accelerateCrossing(world, crop, ManagerConfig.mutagenicPowderBurstTicks, true, world.rand)) {
            return EnumActionResult.PASS;
        }
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        world.playEvent(2005, pos, 0);
        return EnumActionResult.SUCCESS;
    }
}
