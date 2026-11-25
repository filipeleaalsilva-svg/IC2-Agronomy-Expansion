package br.cropmanager;

import br.cropmanager.crop.BaseCropCard;
import br.cropmanager.crop.RadbloomCropCard;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropSeed;
import ic2.api.energy.tile.IHeatSource;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Tier-2 EU Hydroponic Chamber that cultivates IC2 Seed Bags inside a controlled structural enclosure.
 *
 * <p>Requires a {@link GreenhouseGlassBlock} directly above and valid subsoil directly below
 * ({@code dirt}/{@code farmland}/{@code grass}, or {@link RadioactiveSoilBlock} for {@link RadbloomCropCard}).
 * Optional 3x3 greenhouse glass dome (+25% speed) and adjacent IC2 heat source (+50% speed) accelerate cycles.
 */
public final class HydroponicChamberTile extends PoweredMachineTile {
    static final int GUI = 25;
    static final int SLOT_SEED = 0;
    static final int SLOT_CATALYST = 1;
    static final int SLOT_OUT_START = 2;
    static final int SLOT_OUT_END = 6;
    static final int SLOT_UPG_START = 6;
    static final int SLOT_UPG_END = 10;
    static final int SLOT_BATTERY = 10;
    static final int PROGRESS_SCALE = 4;

    public int clientDuration = ManagerConfig.hydroponicDuration;
    public boolean hasGlass;
    public boolean hasSubsoil;
    public boolean domeBonus;
    public boolean heatBonus;
    public int catalystCharges;
    private int structureTicks;
    private Block cachedSubsoil;

    public final FluidTank water = new FluidTank(ManagerConfig.hydroponicTankCapacity) {
        { setCanDrain(false); }
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && fluid.getFluid() == FluidRegistry.WATER;
        }
        @Override protected void onContentsChanged() { markDirty(); }
    };

    public final ItemStackHandler inventory = new ItemStackHandler(11) {
        @Override public int getSlotLimit(int slot) {
            return slot == SLOT_SEED ? 1 : super.getSlotLimit(slot);
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return (slot >= SLOT_OUT_START && slot < SLOT_OUT_END) || isItemValid(slot, stack)
                    ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == SLOT_SEED) return isValidSeedBag(stack);
            if (slot == SLOT_CATALYST) return stack.getItem() == CropManager.CATALYTIC_FERTILIZER;
            if (slot == SLOT_BATTERY) return isBattery(stack);
            return slot >= SLOT_UPG_START && slot < SLOT_UPG_END && ManagerTile.isIc2Upgrade(stack);
        }

        @Override protected void onContentsChanged(int slot) {
            if (slot == SLOT_SEED) {
                progress = 0;
                catalystCharges = 0;
                structureTicks = 0;
            } else if (slot >= SLOT_UPG_START && slot < SLOT_UPG_END) {
                upgradesDirty = true;
            }
            markDirty();
        }
    };

    private final IItemHandler automationItems = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != SLOT_CATALYST || !inventory.isItemValid(slot, stack)) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return (slot >= SLOT_OUT_START && slot < SLOT_OUT_END)
                    ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
    };

    public HydroponicChamberTile() {
        super(ManagerConfig.hydroponicEnergyCapacity, ManagerConfig.hydroponicSinkTier,
                ManagerConfig.hydroponicDuration, ManagerConfig.hydroponicEuPerTick, "hydroponic_no_seed");
    }

    @Override protected ItemStackHandler machineInventory() { return inventory; }
    @Override protected int upgradeStartSlot() { return SLOT_UPG_START; }
    @Override protected int upgradeEndSlot() { return SLOT_UPG_END; }
    @Override protected int batterySlot() { return SLOT_BATTERY; }
    @Override protected int baseDuration() { return ManagerConfig.hydroponicDuration; }
    @Override protected double baseEuPerTick() { return ManagerConfig.hydroponicEuPerTick; }
    @Override protected int baseEnergyCapacity() { return ManagerConfig.hydroponicEnergyCapacity; }
    @Override protected int baseSinkTier() { return ManagerConfig.hydroponicSinkTier; }
    @Override protected boolean processMachineTick() { return processTick(); }

    public static boolean isValidSeedBag(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ICropSeed)) return false;
        CropCard card = ((ICropSeed) stack.getItem()).getCropFromStack(stack);
        return card != null && card != Crops.weed;
    }

    @Override
    protected void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, machineInventory(), upgradeStartSlot(), upgradeEndSlot(),
                energy, 1, baseEnergyCapacity(), baseSinkTier());
        int prevScaled = Math.max(1, workTicks * PROGRESS_SCALE);
        int next = Math.max(1, (int) Math.ceil(baseDuration() * multipliers[0]));
        int nextScaled = next * PROGRESS_SCALE;
        progress = Math.min(nextScaled - 1, (int) ((long) progress * nextScaled / prevScaled));
        workTicks = next;
        euPerTick = Math.ceil(baseEuPerTick() * multipliers[1]);
        upgradesDirty = false;
    }

    void refreshStructure(CropCard card) {
        if (world == null || pos == null) {
            hasGlass = false;
            hasSubsoil = false;
            domeBonus = false;
            heatBonus = false;
            cachedSubsoil = null;
            return;
        }
        BlockPos above = pos.up();
        hasGlass = above.getY() < world.getHeight() && world.isBlockLoaded(above, false)
                && world.getBlockState(above).getBlock() == CropManager.GREENHOUSE_GLASS;

        if (hasGlass) {
            boolean fullDome = true;
            for (int dx = -1; dx <= 1 && fullDome; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = above.add(dx, 0, dz);
                    if (!world.isBlockLoaded(p, false)
                            || world.getBlockState(p).getBlock() != CropManager.GREENHOUSE_GLASS) {
                        fullDome = false;
                        break;
                    }
                }
            }
            domeBonus = fullDome;
        } else {
            domeBonus = false;
        }

        BlockPos below = pos.down();
        if (below.getY() >= 0 && world.isBlockLoaded(below, false)) {
            cachedSubsoil = world.getBlockState(below).getBlock();
            hasSubsoil = isValidSubsoil(cachedSubsoil, card);
        } else {
            cachedSubsoil = null;
            hasSubsoil = false;
        }

        heatBonus = findHeatSource(ManagerConfig.hydroponicHeatPerTick) != null;
    }

    static boolean isValidSubsoil(@Nullable Block block, @Nullable CropCard card) {
        if (block == null) return false;
        if (card instanceof RadbloomCropCard) return block == CropManager.RADIOACTIVE_SOIL;
        return block == Blocks.DIRT || block == Blocks.FARMLAND || block == Blocks.GRASS
                || block == CropManager.RADIOACTIVE_SOIL;
    }

    @Nullable
    private EnumFacing findHeatSource(int heatNeeded) {
        if (world == null || pos == null || heatNeeded <= 0) return null;
        for (EnumFacing side : EnumFacing.values()) {
            BlockPos neighborPos = pos.offset(side);
            if (!world.isBlockLoaded(neighborPos, false)) continue;
            TileEntity te = world.getTileEntity(neighborPos);
            if (!(te instanceof IHeatSource)) continue;
            int available = ((IHeatSource) te).drawHeat(side.getOpposite(), heatNeeded, true);
            if (available >= heatNeeded) return side;
        }
        return null;
    }

    private boolean consumeHeat(int heatNeeded) {
        EnumFacing side = findHeatSource(heatNeeded);
        if (side == null) return false;
        TileEntity te = world.getTileEntity(pos.offset(side));
        if (!(te instanceof IHeatSource)) return false;
        return ((IHeatSource) te).drawHeat(side.getOpposite(), heatNeeded, false) >= heatNeeded;
    }

    boolean processTick() {
        if (upgradesDirty) recalculateUpgrades();
        ItemStack seedStack = inventory.getStackInSlot(SLOT_SEED);
        if (!isValidSeedBag(seedStack)) {
            if (progress != 0) { progress = 0; markDirty(); }
            if (structureTicks <= 0) {
                refreshStructure(null);
                structureTicks = 20;
            } else {
                structureTicks--;
            }
            condition = "hydroponic_no_seed";
            return false;
        }

        ICropSeed seed = (ICropSeed) seedStack.getItem();
        CropCard card = seed.getCropFromStack(seedStack);
        if (structureTicks <= 0) {
            refreshStructure(card);
            structureTicks = 20;
        } else {
            structureTicks--;
        }

        if (!hasGlass) {
            condition = "hydroponic_no_glass";
            return false;
        }
        if (!hasSubsoil) {
            condition = "hydroponic_no_subsoil";
            return false;
        }

        int waterCost = ManagerConfig.hydroponicWaterPerCycle;
        if (water.getFluidAmount() < waterCost) {
            condition = "hydroponic_no_water";
            return false;
        }

        boolean needsCatalyst = card instanceof BaseCropCard && ((BaseCropCard) card).requiresCatalyst();
        if (needsCatalyst && catalystCharges <= 0 && inventory.getStackInSlot(SLOT_CATALYST).isEmpty()) {
            condition = "hydroponic_no_catalyst";
            return false;
        }

        if (!hasOutputRoomForTick(card)) {
            condition = "no_space";
            return false;
        }

        int target = Math.max(1, workTicks) * PROGRESS_SCALE;
        if (progress < target) {
            if (!energy.useEnergy(euPerTick)) {
                condition = "no_energy";
                return false;
            }
            boolean heatActive = consumeHeat(ManagerConfig.hydroponicHeatPerTick);
            heatBonus = heatActive;
            int step = PROGRESS_SCALE + (heatActive ? 2 : 0) + (domeBonus ? 1 : 0);
            progress = Math.min(target, progress + step);
            condition = "hydroponic_working";
            if (progress % 20 == 0 || progress >= target) markDirty();
        }

        if (progress >= target) {
            ItemStack drop = computeDrop(card, seed, seedStack);
            if (!drop.isEmpty() && !canFitOutput(drop)) {
                condition = "no_space";
                return false;
            }
            water.drainInternal(waterCost, true);
            if (needsCatalyst) {
                if (catalystCharges <= 0) {
                    inventory.extractItem(SLOT_CATALYST, 1, false);
                    catalystCharges = ManagerConfig.catalyticCharges;
                }
                catalystCharges = Math.max(0, catalystCharges - 1);
            }
            if (!drop.isEmpty()) insertOutput(drop);
            progress = 0;
            markDirty();
        }
        return true;
    }

    private boolean hasOutputRoomForTick(CropCard card) {
        if (card instanceof BaseCropCard) {
            ItemStack preview = ((BaseCropCard) card).previewGain();
            if (!preview.isEmpty()) return canFitOutput(preview);
        }
        for (int slot = SLOT_OUT_START; slot < SLOT_OUT_END; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private ItemStack computeDrop(CropCard card, ICropSeed seed, ItemStack seedStack) {
        if (card instanceof BaseCropCard) {
            ItemStack preview = ((BaseCropCard) card).previewGain();
            if (!preview.isEmpty()) return preview;
        }
        VirtualCropTile virtual = new VirtualCropTile(
                world, pos, card,
                seed.getGrowthFromStack(seedStack),
                seed.getGainFromStack(seedStack),
                seed.getResistanceFromStack(seedStack),
                cachedSubsoil);
        ItemStack drop = card.getGain(virtual);
        return drop == null || drop.isEmpty() ? ItemStack.EMPTY : drop.copy();
    }

    private boolean canFitOutput(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = SLOT_OUT_START; slot < SLOT_OUT_END && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, true);
        }
        return remaining.isEmpty();
    }

    private void insertOutput(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = SLOT_OUT_START; slot < SLOT_OUT_END && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, false);
        }
    }

    public IItemHandler itemsForFace(@Nullable EnumFacing face) {
        return automationItems;
    }

    @Override
    public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing face) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(cap, face);
    }

    @Override
    public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing face) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automationItems);
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(water);
        return super.getCapability(cap, face);
    }

    @Override
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("EU", energy.getEnergyStored());
        tag.setDouble("Capacity", energy.getCapacity());
        tag.setInteger("Tier", energy.getSinkTier());
        tag.setInteger("Work", progress);
        tag.setInteger("Duration", workTicks);
        tag.setDouble("Demand", euPerTick);
        tag.setString("Condition", condition);
        tag.setInteger("Water", water.getFluidAmount());
        tag.setInteger("WaterCap", water.getCapacity());
        tag.setBoolean("HasGlass", hasGlass);
        tag.setBoolean("HasSubsoil", hasSubsoil);
        tag.setBoolean("DomeBonus", domeBonus);
        tag.setBoolean("HeatBonus", heatBonus);
        tag.setInteger("CatalystCharges", catalystCharges);
        return tag;
    }

    @Override
    public void acceptGuiState(NBTTagCompound tag) {
        energy.setCapacity(tag.getDouble("Capacity"));
        energy.setSinkTier(Math.max(1, tag.getInteger("Tier")));
        energy.setEnergyStored(tag.getDouble("EU"));
        clientDuration = Math.max(1, tag.getInteger("Duration"));
        workTicks = clientDuration;
        progress = Math.max(0, tag.getInteger("Work"));
        euPerTick = tag.getDouble("Demand");
        condition = tag.getString("Condition");
        water.setCapacity(Math.max(1, tag.getInteger("WaterCap")));
        int amt = Math.max(0, Math.min(water.getCapacity(), tag.getInteger("Water")));
        water.setFluid(amt > 0 ? new FluidStack(FluidRegistry.WATER, amt) : null);
        hasGlass = tag.getBoolean("HasGlass");
        hasSubsoil = tag.getBoolean("HasSubsoil");
        domeBonus = tag.getBoolean("DomeBonus");
        heatBonus = tag.getBoolean("HeatBonus");
        catalystCharges = Math.max(0, tag.getInteger("CatalystCharges"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writePoweredNBT(tag);
        tag.setTag("WaterTank", water.writeToNBT(new NBTTagCompound()));
        tag.setInteger("CatalystCharges", catalystCharges);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        water.setCapacity(ManagerConfig.hydroponicTankCapacity);
        water.readFromNBT(tag.getCompoundTag("WaterTank"));
        FluidStack w = water.getFluid();
        if (!water.canFillFluidType(w)) water.setFluid(null);
        else w.amount = Math.min(water.getCapacity(), w.amount);
        readPoweredNBT(tag);
        catalystCharges = Math.max(0, tag.getInteger("CatalystCharges"));
        int maxProgress = Math.max(1, workTicks) * PROGRESS_SCALE;
        progress = Math.max(0, Math.min(maxProgress, tag.getInteger("Work")));
        structureTicks = 0;
    }

    @Override
    EquipmentLayout equipmentLayout() {
        return new EquipmentLayout(inventory, 126,
                new int[]{54, 54, 104, 122, 104, 122, 148, 148, 148, 148, 8},
                new int[]{24, 54, 24, 24, 42, 42, 22, 40, 58, 76, 62},
                slot -> slot < SLOT_OUT_START || slot >= SLOT_OUT_END,
                s -> isBattery(s) ? new int[]{SLOT_BATTERY, SLOT_BATTERY + 1}
                        : ManagerTile.isIc2Upgrade(s) ? new int[]{SLOT_UPG_START, SLOT_UPG_END}
                        : s.getItem() == CropManager.CATALYTIC_FERTILIZER ? new int[]{SLOT_CATALYST, SLOT_CATALYST + 1}
                        : new int[]{SLOT_SEED, SLOT_SEED + 1});
    }
}
