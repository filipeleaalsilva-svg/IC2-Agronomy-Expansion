package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.ItemName;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

public final class CrossingTool extends Item {
    static final int GUI = 21;
    static final int SLOTS = 3;
    private static final String LINK = "CropManagerCrossingLink";
    private static final String SUPPLIES = "CrossingSupplies";

    public CrossingTool() {
        setRegistryName(CropManager.ID, "crossing_tool");
        setTranslationKey("cropmanager.crossing_tool");
        setCreativeTab(CropManager.TAB);
        setMaxStackSize(1);
    }

    static boolean isCropStick(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ItemName.crop_stick.getInstance();
    }

    static ItemStackHandler supplies(ItemStack tool) {
        ItemStackHandler handler = new ItemStackHandler(SLOTS) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return isCropStick(stack);
            }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return isCropStick(stack) ? super.insertItem(slot, stack, simulate) : stack;
            }
            @Override protected void onContentsChanged(int slot) {
                tag(tool).setTag(SUPPLIES, serializeNBT());
            }
        };
        if (tool.hasTagCompound()) {
            NBTTagCompound data = tool.getTagCompound().getCompoundTag(SUPPLIES).copy();
            data.setInteger("Size", SLOTS);
            handler.deserializeNBT(data);
        }
        return handler;
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack tool = player.getHeldItem(hand);
        if (!player.isSneaking() || player.isSpectator() || !player.inventory.getItemStack().isEmpty()) {
            return new ActionResult<>(EnumActionResult.PASS, tool);
        }
        if (!world.isRemote) {
            player.openGui(CropManager.instance, GUI, world, hand == EnumHand.MAIN_HAND ? player.inventory.currentItem : 40, 0, 0);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, tool);
    }

    @Override public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos,
            EnumFacing side, float x, float y, float z, EnumHand hand) {
        if (!world.isBlockLoaded(pos, false)) return EnumActionResult.PASS;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityCrop)) return EnumActionResult.PASS;
        if (player.isSneaking()) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack tool = player.getHeldItem(hand);
        if (player.isSpectator() || !player.canPlayerEdit(pos, side, tool) || !world.isBlockModifiable(player, pos)) {
            return EnumActionResult.FAIL;
        }
        String result = useOnCrop(tool, (TileEntityCrop) tile);
        player.inventory.markDirty();
        player.sendStatusMessage(new TextComponentTranslation("cropmanager.crossing_tool." + result), true);
        return EnumActionResult.SUCCESS;
    }

    static String useOnCrop(ItemStack tool, TileEntityCrop crop) {
        NBTTagCompound cropData = crop.getTileData();
        CropCard card = crop.getCrop();
        if (card == null && crop.isCrossingBase()) {
            if (!cropData.hasUniqueId(LINK)) {
                cropData.setUniqueId(LINK, UUID.randomUUID());
                crop.markDirty();
            }
            tag(tool).setUniqueId(LINK, cropData.getUniqueId(LINK));
            return "linked";
        }
        NBTTagCompound toolData = tool.getTagCompound();
        if (toolData == null || !toolData.hasUniqueId(LINK) || !cropData.hasUniqueId(LINK)
                || !toolData.getUniqueId(LINK).equals(cropData.getUniqueId(LINK))) return "wrong_target";
        if (card == null || card == Crops.weed || !card.canBeHarvested(crop)) return "not_ready";

        ItemStackHandler stock = supplies(tool);
        int slot = 0;
        while (slot < SLOTS && !isCropStick(stock.getStackInSlot(slot))) slot++;
        if (slot == SLOTS) return "no_sticks";
        if (!crop.pick()) return "not_ready";
        crop.setCrossingBase(true);
        crop.dirty = true;
        crop.updateState();
        crop.markDirty();
        stock.extractItem(slot, 1, false);
        return "rearmed";
    }

    @SideOnly(Side.CLIENT)
    @Override public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        ItemStackHandler stock = supplies(stack);
        int count = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack supply = stock.getStackInSlot(slot);
            if (isCropStick(supply)) count += supply.getCount();
        }
        tooltip.add(I18n.format("cropmanager.crossing_tool.stock", count, count));
        tooltip.add(I18n.format("cropmanager.crossing_tool.help"));
    }
}
