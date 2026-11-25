package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.upgrade.*;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Entidade de bloco da Composteira Elétrica (Tier 1).
 *
 * <p>Converte itens orgânicos (vanilla, OreDictionary e materiais do mod) e água em fertilizante IC2,
 * suportando upgrades nativos IC2, Wireless EU Upgrade e bateria de descarga direta.
 */
public final class ComposterTile extends PoweredMachineTile {
    public static final int REQUIRED = 4, WATER_PER_RECIPE = 1000, CAPACITY = 16000, DURATION = 200, EU_PER_TICK = 5;
    public ComposterTile() {
        super(ManagerConfig.composterEnergyCapacity, ManagerConfig.composterSinkTier,
                ManagerConfig.composterDuration, ManagerConfig.composterEuPerTick, "compost_input");
    }
    @Override protected ItemStackHandler machineInventory() { return inventory; }
    @Override protected int upgradeStartSlot() { return 2; }
    @Override protected int upgradeEndSlot() { return 6; }
    @Override protected int batterySlot() { return 6; }
    @Override protected int baseDuration() { return ManagerConfig.composterDuration; }
    @Override protected double baseEuPerTick() { return ManagerConfig.composterEuPerTick; }
    @Override protected int baseEnergyCapacity() { return ManagerConfig.composterEnergyCapacity; }
    @Override protected int baseSinkTier() { return ManagerConfig.composterSinkTier; }
    @Override protected boolean processMachineTick() { return processTick(); }
    public final FluidTank water = new FluidTank(ManagerConfig.composterTankCapacity) {
        { setCanDrain(false); }
        @Override public boolean canFillFluidType(FluidStack stack) { return stack != null && stack.getFluid() == FluidRegistry.WATER; }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 ? organic(stack) : slot == 6 ? isBattery(stack) : slot >= 2 ? ManagerTile.isIc2Upgrade(stack) : ManagerTile.isFertilizer(stack); }
        @Override protected void onContentsChanged(int slot) { if (slot >= 2 && slot < 6) upgradesDirty = true; markDirty(); }
    };
    public static boolean organic(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // IC2 Crop Resources: Weed (ic2:crop_res, variant "weed", metadata 5).
        // Use the IC2 item API instead of a hard-coded metadata value so this
        // remains tied to the configured IC2 resource item.
        ItemStack weed = ic2.api.item.IC2Items.getItem("crop_res", "weed");
        if (weed != null && !weed.isEmpty() && ItemStack.areItemsEqual(stack, weed)) return true;
        Item item = stack.getItem();
        if (item == CropManager.BIO_FIBER || item == CropManager.NITRO_ESSENCE) return true;
        if (item == Items.WHEAT || item == Items.WHEAT_SEEDS || item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS
                || item == Items.CARROT || item == Items.POTATO || item == Items.POISONOUS_POTATO || item == Items.BEETROOT
                || item == Items.BEETROOT_SEEDS || item == Items.REEDS || item == Items.APPLE || item == Items.MELON
                || item == Item.getItemFromBlock(Blocks.LEAVES) || item == Item.getItemFromBlock(Blocks.LEAVES2)
                || item == Item.getItemFromBlock(Blocks.SAPLING) || item == Item.getItemFromBlock(Blocks.VINE)
                || item == Item.getItemFromBlock(Blocks.TALLGRASS) || item == Item.getItemFromBlock(Blocks.CACTUS)) return true;
        for (int id : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
            String name = net.minecraftforge.oredict.OreDictionary.getOreName(id);
            if (name.equals("treeSapling") || name.equals("treeLeaves") || name.equals("listAllseed")
                    || name.equals("listAllveggie") || name.equals("listAllfruit") || name.equals("cropWheat")
                    || name.equals("cropCarrot") || name.equals("cropPotato") || name.equals("cropBeetroot")) return true;
        }
        return false;
    }
    boolean processTick() {
        if (upgradesDirty) recalculateUpgrades();
        ItemStack input = inventory.getStackInSlot(0), output = ic2.api.item.IC2Items.getItem("crop_res", "fertilizer").copy();
        if (!organic(input) || input.getCount() < ManagerConfig.composterRequiredItems) { condition = "compost_input"; return false; }
        if (water.getFluidAmount() < ManagerConfig.composterWaterPerRecipe) { condition = "no_water"; return false; }
        if (!inventory.insertItem(1, output, true).isEmpty()) { condition = "no_space"; return false; }
        if (!energy.useEnergy(euPerTick)) { condition = "no_energy"; return false; }
        condition = "working"; progress++; markDirty();
        if (progress >= workTicks) {
            inventory.extractItem(0, ManagerConfig.composterRequiredItems, false);
            water.drainInternal(ManagerConfig.composterWaterPerRecipe, true);
            inventory.insertItem(1, output, false);
            progress = 0;
        }
        return true;
    }
    public IItemHandler itemsForFace(@Nullable EnumFacing face) {
        return exposedItems(inventory, 1, 1);
    }
    @Override public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing face) { return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(cap, face); }
    @Override public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing face) { if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemsForFace(face)); if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(water); return super.getCapability(cap, face); }
    @Override public NBTTagCompound guiState() { NBTTagCompound t = new NBTTagCompound(); t.setDouble("EU", energy.getEnergyStored()); t.setInteger("Water", water.getFluidAmount()); t.setInteger("TankCapacity", water.getCapacity()); t.setInteger("Work", progress); t.setInteger("Duration", workTicks); t.setDouble("Demand", euPerTick); t.setDouble("Capacity", energy.getCapacity()); t.setInteger("Tier", energy.getSinkTier()); t.setString("Condition", condition); return t; }
    @Override public void acceptGuiState(NBTTagCompound t) { energy.setCapacity(t.getDouble("Capacity")); energy.setSinkTier(t.getInteger("Tier")); energy.setEnergyStored(t.getDouble("EU")); if (t.getInteger("TankCapacity") > 0) water.setCapacity(t.getInteger("TankCapacity")); water.setFluid(t.getInteger("Water") > 0 ? new FluidStack(FluidRegistry.WATER, t.getInteger("Water")) : null); workTicks = Math.max(1, t.getInteger("Duration")); euPerTick = t.getDouble("Demand"); progress = t.getInteger("Work"); condition = t.getString("Condition"); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound t) { super.writeToNBT(t); writePoweredNBT(t); t.setTag("Water", water.writeToNBT(new NBTTagCompound())); return t; }
    @Override public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        water.readFromNBT(t.getCompoundTag("Water")); resizeTank(water, ManagerConfig.composterTankCapacity);
        readPoweredNBT(t);
    }
    @Override EquipmentLayout equipmentLayout() {
        return new EquipmentLayout(inventory, 126,
                new int[]{54, 112, 148, 148, 148, 148, 8},
                new int[]{42, 42, 22, 40, 58, 76, 62},
                1, false,
                s -> isBattery(s) ? new int[]{6, 7} : ManagerTile.isIc2Upgrade(s) ? new int[]{2, 6} : new int[]{0, 1});
    }
}
