package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.energy.prefab.BasicSink;
import ic2.api.item.IC2Items;
import ic2.api.upgrade.*;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.FluidName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.items.*;

/**
 * Entidade de bloco do Crop Manager (básico legado e {@link AdvancedManagerTile} avançado).
 *
 * <p>Executa varredura incremental em área configurável sobre {@link TileEntityCrop} do IC2 sem
 * forçar carregamento de chunks, aplicando fertilizante, hidratação, Weed-Ex, colheita e censo
 * ambiental/espécies no mesmo ciclo. Versões básicas legadas são migradas de forma adiada via
 * {@link LegacyMigration}.
 */
public class ManagerTile extends FarmTile implements ITickable, IUpgradableBlock {
    public static final int OUTPUT_START = 3, OUTPUT_END = 18, UPGRADE_START = 18, UPGRADE_END = 22;
    public static final int WATER_IN = 22, WEED_IN = 23, WATER_OUT = 24, WEED_OUT = 25;
    public static final int NO_ENERGY = 1, NO_FERTILIZER = 2, NO_WATER = 4, NO_WEED = 8, NO_SPACE = 16;
    private static final Set<UpgradableProperty> PROPERTIES = EnumSet.of(UpgradableProperty.Processing,
            UpgradableProperty.EnergyStorage, UpgradableProperty.Transformer);

    public boolean migrating;
    public final boolean advanced;
    public int redstoneMode;
    public boolean paused, enabled = true;
    public final AutomationRules automation = new AutomationRules();
    public NBTTagCompound report = new NBTTagCompound();
    private int[] counts = new int[6];
    private CropSurvey survey = new CropSurvey();
    private long reportTime = -1;
    public ManagerTile() { this(false); }
    protected ManagerTile(boolean advanced) {
        this.advanced = advanced;
        water.setCapacity(configuredTankCapacity()); weed.setCapacity(configuredTankCapacity());
        energy.setCapacity(baseEnergyCapacity()); energy.setSinkTier(baseSinkTier());
    }
    private int configuredTankCapacity() { return advanced ? ManagerConfig.managerAdvancedTankCapacity : ManagerConfig.managerBasicTankCapacity; }
    private int baseEnergyCapacity() { return advanced ? ManagerConfig.managerAdvancedEnergyCapacity : ManagerConfig.managerBasicEnergyCapacity; }
    private int baseSinkTier() { return advanced ? ManagerConfig.managerAdvancedSinkTier : ManagerConfig.managerBasicSinkTier; }
    public int tankCapacity() { return world != null && world.isRemote ? water.getCapacity() : configuredTankCapacity(); }

    public int radius = ManagerConfig.defaultRadius, layers = ManagerConfig.defaultLayers, functions = 15;
    public int status, serverMaxRadius = ManagerConfig.maxRadius;
    private int cursor, activeTicks;
    private final EnergyWarning energyWarning = new EnergyWarning();
    private double progress, interval = ManagerConfig.interval, demandMultiplier = 1;
    private boolean upgradesDirty = true;
    private final List<ItemStack> pending = new ArrayList<>();
    public final BasicSink energy = createSink(20000, 1);
    @Override public BasicSink energySink() { return energy; }
    public final ItemStackHandler inventory = new ItemStackHandler(26) {
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && !isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot < OUTPUT_START) return isFertilizer(stack);
            if (slot >= UPGRADE_START && slot < UPGRADE_END) return isUpgrade(stack) && (advanced || !isWeedKiller(stack));
            if (slot == WATER_IN || slot == WEED_IN) {
                FluidStack fluid = FluidUtil.getFluidContained(stack);
                return fluid != null && fluid.getFluid() == (slot == WATER_IN ? FluidRegistry.WATER : FluidName.weed_ex.getInstance());
            }
            return true;
        }
        @Override protected void onContentsChanged(int slot) {
            inventoryChanged(slot);
        }
    };
    public final FluidTank water = new FluidTank(10000) {
        @Override public boolean canFillFluidType(FluidStack fluid) { return fluid != null && fluid.getFluid() == FluidRegistry.WATER; }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public final FluidTank weed = new FluidTank(10000) {
        @Override public boolean canFillFluidType(FluidStack fluid) { return fluid != null && fluid.getFluid() == FluidName.weed_ex.getInstance(); }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {water.getTankProperties()[0], weed.getTankProperties()[0]};
        }
        @Override public int fill(FluidStack resource, boolean execute) {
            return resource != null && resource.getFluid() == FluidRegistry.WATER
                    ? water.fill(resource, execute) : weed.fill(resource, execute);
        }
        @Override public FluidStack drain(FluidStack resource, boolean execute) {
            if (resource == null) return null;
            return resource.getFluid() == FluidRegistry.WATER ? water.drain(resource, execute) : weed.drain(resource, execute);
        }
        @Override public FluidStack drain(int amount, boolean execute) {
            return water.getFluidAmount() > 0 ? water.drain(amount, execute) : weed.drain(amount, execute);
        }
    };
    private final IItemHandler automationItems = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= OUTPUT_START && slot < OUTPUT_END || slot == WATER_OUT || slot == WEED_OUT) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= OUTPUT_START && slot < OUTPUT_END || slot == WATER_OUT || slot == WEED_OUT)
                return inventory.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
    };

    public static boolean isFertilizer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof FertilizerItem) return true;
        ItemStack fert = IC2Items.getItem("crop_res", "fertilizer");
        return fert != null && !fert.isEmpty() && ItemStack.areItemsEqual(stack, fert);
    }
    public static boolean isUpgrade(ItemStack stack) {
        return isWeedKiller(stack) || isIc2Upgrade(stack);
    }
    /** Native IC2 upgrades (and Wireless EU Upgrade); safe to cast to IUpgradeItem. */
    public static boolean isIc2Upgrade(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IUpgradeItem)) return false;
        if (stack.getItem() == CropManager.WIRELESS_EU) return true;
        for (String name : new String[] {"overclocker", "energy_storage", "transformer"}) {
            ItemStack upgrade = IC2Items.getItem("upgrade", name);
            if (upgrade != null && !upgrade.isEmpty() && ItemStack.areItemsEqual(stack, upgrade)) return true;
        }
        return false;
    }
    private static boolean isWeedKiller(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == CropManager.WEED_KILLER;
    }

    @Override public void onLoad() {
        if (!world.isRemote) {
            if (!advanced) {
                LegacyMigration.request(this);
                return;
            }
            recalculateUpgrades(); energy.onLoad();
        }
    }
    void migrate() {
        NBTTagCompound saved = writeToNBT(new NBTTagCompound());
        IBlockState old = world.getBlockState(pos); migrating = true;
        net.minecraft.util.EnumFacing facing = old.getBlock() instanceof ManagerBlock ? old.getValue(ManagerBlock.FACING) : EnumFacing.NORTH;
        world.setBlockState(pos, CropManager.ADVANCED.getDefaultState().withProperty(ManagerBlock.FACING, facing), 18);
        TileEntity next = world.getTileEntity(pos);
        if (!(next instanceof AdvancedManagerTile)) { next = new AdvancedManagerTile(); world.setTileEntity(pos, next); }
        if (next instanceof AdvancedManagerTile) { saved.setString("id", "cropmanager:advanced_crop_manager"); next.readFromNBT(saved); next.markDirty(); }
    }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }
    @Override public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
            IBlockState oldState, IBlockState newState) { return oldState.getBlock() != newState.getBlock(); }

    @Override public void update() {
        if (world.isRemote) return;
        if (upgradesDirty) recalculateUpgrades();
        tickUpgrades(this, inventory, UPGRADE_START, UPGRADE_END);
        energy.update();
        emptyFluidContainer(inventory, WATER_IN, WATER_OUT, water);
        emptyFluidContainer(inventory, WEED_IN, WEED_OUT, weed);
        flushPending();
        int fertilizerCount = inventory.getStackInSlot(0).getCount() + inventory.getStackInSlot(1).getCount() + inventory.getStackInSlot(2).getCount();
        boolean outputBlocked = (functions & 1) != 0 && (!pending.isEmpty() || !hasOutputRoom());
        if (automation.update(water.getFluidAmount(), water.getCapacity(), weed.getFluidAmount(), weed.getCapacity(),
                fertilizerCount, energy.getEnergyStored(), (int)energy.getCapacity(), outputBlocked)) markDirty();
        paused = !enabled || automation.stopped || !redstoneAllows(redstoneMode, redstoneMode != 0 && world.isAreaLoaded(pos.add(-2, -2, -2), pos.add(2, 2, 2), false) && world.isBlockPowered(pos));
        boolean scanning = !paused && (functions != 0 || advanced);
        status = energyWarning.isLow(getEnergy(), cost(ManagerConfig.scanEU), scanning) ? NO_ENERGY : 0;
        if ((functions & 1) != 0 && (!pending.isEmpty() || !hasOutputRoom())) status |= NO_SPACE;
        if ((functions & 2) != 0 && fertilizerSlot() < 0) status |= NO_FERTILIZER;
        if ((functions & 4) != 0 && water.getFluidAmount() == 0) status |= NO_WATER;
        if ((functions & 8) != 0 && weed.getFluidAmount() == 0) status |= NO_WEED;
        if (scanning) {
            progress = ScanLogic.credit(progress, interval, ManagerConfig.maxScans);
            markDirty();
            int attempts = 0;
            while (progress >= 1 && attempts++ < ManagerConfig.maxScans) {
                if (getEnergy() < cost(ManagerConfig.scanEU)) { status |= NO_ENERGY; progress = 0; break; }
                int[] offset = ScanLogic.offset(cursor, radius, layers);
                net.minecraft.util.math.BlockPos target = pos.add(offset[0], offset[1], offset[2]);
                cursor = (cursor + 1) % ScanLogic.volume(radius, layers);
                progress--;
                markDirty();
                if (target.getY() < 0 || target.getY() >= world.getHeight() || !world.isBlockLoaded(target, false)) {
                    if (advanced) counts[5]++;
                } else {
                    useEnergy(cost(ManagerConfig.scanEU));
                    TileEntity tile = world.getTileEntity(target);
                    if (tile instanceof TileEntityCrop) {
                        if (advanced) inspect((TileEntityCrop)tile, target);
                        visit((TileEntityCrop) tile);
                    }
                }
                if (advanced && cursor == 0) finishReport();
            }
        } else if (progress != 0) { progress = 0; markDirty(); }
        if (energyWarning.isLow(getEnergy(), cost(ManagerConfig.scanEU), scanning)) status |= NO_ENERGY;
        if (paused) activeTicks = 0;
        boolean active = activeTicks > 0;
        if (activeTicks > 0) { activeTicks--; markDirty(); }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ManagerBlock && state.getValue(ManagerBlock.ACTIVE) != active)
            setActive(active);
    }

    private void visit(TileEntityCrop crop) {
        CropCard card = crop.getCrop();
        if (card == null) return;
        if (advanced && hasWeedKiller() && card == ic2.api.crops.Crops.weed && weed.getFluidAmount() >= ManagerConfig.managerWeedKillerCostMB
                && action(ManagerConfig.careEU, () -> {
                    crop.reset();
                    crop.updateState();
                    if (ManagerConfig.managerWeedKillerCostMB > 0) weed.drain(ManagerConfig.managerWeedKillerCostMB, true);
                    world.markChunkDirty(crop.getPos(), crop);
                    return true;
                })) return;
        int fertilizer = fertilizerSlot();
        if (fertilizer >= 0 && action(ManagerConfig.careEU, () -> FertilizerItem.applyAny(inventory.getStackInSlot(fertilizer), crop, false))) {
            inventory.extractItem(fertilizer, 1, false);
            world.markChunkDirty(crop.getPos(), crop);
        }
        if (water.getFluidAmount() > 0
                && action(ManagerConfig.careEU, () -> crop.applyHydration(water))) world.markChunkDirty(crop.getPos(), crop);
        if (weed.getFluidAmount() > 0
                && action(ManagerConfig.careEU, () -> crop.applyWeedEx(weed, false))) world.markChunkDirty(crop.getPos(), crop);
        int size = crop.getCurrentSize();
        if ((functions & 1) != 0 && pending.isEmpty() && hasOutputRoom()
                && (size == card.getOptimalHarvestSize(crop) || size == card.getMaxSize())) {
            action(ManagerConfig.harvestEU, () -> {
                List<ItemStack> drops = crop.performHarvest();
                if (drops == null) return false;
                for (ItemStack stack : drops) if (!stack.isEmpty()) pending.add(stack.copy());
                world.markChunkDirty(crop.getPos(), crop);
                crop.updateState();
                markDirty();
                flushPending();
                return true;
            });
        }
    }

    private boolean hasWeedKiller() {
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++)
            if (isWeedKiller(inventory.getStackInSlot(slot))) return true;
        return false;
    }

    private double cost(double base) { return base * demandMultiplier; }
    private boolean action(double baseCost, BooleanSupplier operation) {
        double reserved = cost(baseCost);
        if (!useEnergy(reserved)) { energyWarning.shortage(reserved); status |= NO_ENERGY; return false; }
        if (!operation.getAsBoolean()) { energy.addEnergy(reserved); markDirty(); return false; }
        activeTicks = 10;
        markDirty();
        return true;
    }
    private int fertilizerSlot() {
        for (int i = 0; i < OUTPUT_START; i++) if (isFertilizer(inventory.getStackInSlot(i))) return i;
        return -1;
    }
    private boolean hasOutputRoom() {
        for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < Math.min(s.getMaxStackSize(), inventory.getSlotLimit(i))) return true;
        }
        return false;
    }
    private void flushPending() {
        int processed = 0;
        Iterator<ItemStack> it = pending.iterator();
        while (it.hasNext() && processed++ < ManagerConfig.managerFlushPerTick) {
            ItemStack original = it.next();
            ItemStack rest = original;
            int before = rest.getCount();
            for (int slot = OUTPUT_START; slot < OUTPUT_END && !rest.isEmpty(); slot++)
                rest = inventory.insertItem(slot, rest, false);
            if (rest.isEmpty()) it.remove();
            else {
                // Keep the original list entry; insertItem returns a separate remainder.
                original.setCount(rest.getCount());
            }
            if (rest.isEmpty() || rest.getCount() != before) markDirty();
            if (!rest.isEmpty()) break;
        }
    }
    private void recalculateUpgrades() {
        double[] multipliers = applyIc2Upgrades(this, inventory, UPGRADE_START, UPGRADE_END,
                energy, advanced ? ManagerConfig.managerAdvancedTimeMultiplier : 1, baseEnergyCapacity(), baseSinkTier());
        interval = Math.max(1e-100, ManagerConfig.interval * multipliers[0]);
        energyWarning.clear();
        demandMultiplier = multipliers[1];
        upgradesDirty = false;
    }
    @Override public double getEnergy() { return energy.getEnergyStored(); }
    @Override public boolean useEnergy(double amount) {
        boolean used = energy.useEnergy(amount);
        if (used) markDirty();
        return used;
    }
    @Override public Set<UpgradableProperty> getUpgradableProperties() { return EnumSet.copyOf(PROPERTIES); }
    /**
     * Aplica novas configurações de área e funções, zerando o relatório do censo quando as
     * dimensões da área mudam para evitar exibição de médias misturadas entre áreas distintas.
     */
    public void settings(int newRadius, int newLayers, int newFunctions) {
        newFunctions = (newFunctions & 1) | 14;
        if (!ScanLogic.validSettings(newRadius, newLayers, newFunctions, ManagerConfig.maxRadius)) return;
        if (radius != newRadius || layers != newLayers) { resetReport(); progress = 0; }
        if (functions != newFunctions) energyWarning.clear();
        radius = newRadius; layers = newLayers; functions = newFunctions;
        markDirty();
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing face) {
        // Ender IO 5.5.1's item conduit crashes while querying the Advanced
        // Crop Manager from below in the user's Cleanroom modpack. Keep that
        // face closed; item automation remains available from every other side.
        if (advanced && face == EnumFacing.DOWN && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return false;
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, face);
    }
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing face) {
        if (advanced && face == EnumFacing.DOWN && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return null;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemsForFace(face));
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluids);
        return super.getCapability(capability, face);
    }
    IItemHandler itemsForFace(@Nullable EnumFacing face) { return automationItems; }
    void inventoryChanged(int slot) {
        if (slot >= UPGRADE_START && slot < UPGRADE_END) upgradesDirty = true;
        markDirty();
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        energy.writeToNBT(tag);
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setTag("Water", water.writeToNBT(new NBTTagCompound()));
        tag.setTag("Weed", weed.writeToNBT(new NBTTagCompound()));
        tag.setInteger("Radius", radius); tag.setInteger("Layers", layers); tag.setInteger("Functions", functions);
        tag.setInteger("Redstone", redstoneMode);
        tag.setBoolean("Enabled", enabled); tag.setTag("Automation", automation.write());
        tag.setInteger("Cursor", cursor); tag.setDouble("Progress", progress); tag.setInteger("ActiveTicks", activeTicks);
        NBTTagList list = new NBTTagList();
        for (ItemStack s : pending) {
            NBTTagCompound entry = s.writeToNBT(new NBTTagCompound());
            entry.setByte("Count", (byte) 1);
            entry.setInteger("PendingCount", s.getCount()); list.appendTag(entry);
        }
        tag.setTag("Pending", list);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        enabled = !tag.hasKey("Enabled") || tag.getBoolean("Enabled"); automation.read(tag.getCompoundTag("Automation"));
        redstoneMode = MathHelper.clamp(tag.getInteger("Redstone"), 0, 2);
        inventory.deserializeNBT(tag.getCompoundTag("Inventory"));
        recalculateUpgrades();
        energy.readFromNBT(tag);
        double savedEnergy = energy.getEnergyStored();
        energy.setEnergyStored(Double.isFinite(savedEnergy)
                ? Math.max(0, Math.min(energy.getCapacity(), savedEnergy)) : 0);
        water.readFromNBT(tag.getCompoundTag("Water")); weed.readFromNBT(tag.getCompoundTag("Weed"));
        resizeTank(water, configuredTankCapacity()); resizeTank(weed, configuredTankCapacity());
        radius = MathHelper.clamp(tag.getInteger("Radius"), 1, ManagerConfig.maxRadius);
        layers = tag.getInteger("Layers") & 7; if (layers == 0) layers = ManagerConfig.defaultLayers;
        functions = (tag.getInteger("Functions") & 1) | 14;
        cursor = Math.floorMod(tag.getInteger("Cursor"), ScanLogic.volume(radius, layers));
        progress = tag.getDouble("Progress");
        if (!Double.isFinite(progress)) progress = 0;
        progress = Math.max(0, Math.min(ManagerConfig.maxScans, progress));
        activeTicks = MathHelper.clamp(tag.getInteger("ActiveTicks"), 0, 10);
        pending.clear();
        NBTTagList list = tag.getTagList("Pending", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            ItemStack s = new ItemStack(entry); s.setCount(entry.getInteger("PendingCount"));
            if (!s.isEmpty()) pending.add(s);
        }
        if (advanced) resetReport();
        upgradesDirty = true;
    }
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("EU", getEnergy()); tag.setDouble("Capacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier());
        tag.setInteger("Water", water.getFluidAmount()); tag.setInteger("Weed", weed.getFluidAmount());
        tag.setInteger("Radius", radius); tag.setInteger("Layers", layers); tag.setInteger("Functions", functions);
        tag.setBoolean("Enabled", enabled); tag.setTag("Automation", automation.write()); tag.setString("State", stateKey());
        tag.setInteger("Redstone", redstoneMode); tag.setBoolean("Paused", paused);
        tag.setInteger("TankCapacity", tankCapacity()); tag.setInteger("Cursor", world != null && world.isRemote ? clientCursor : cursor);
        tag.setTag("Report", report.copy()); tag.setLong("ReportAge", world != null && world.isRemote ? report.getLong("Age") : reportTime < 0 || world == null ? -1 : Math.max(0, world.getTotalWorldTime() - reportTime));
        tag.setBoolean("LowEnergy", (functions != 0 || advanced) && getEnergy() < cost(ManagerConfig.scanEU));
        tag.setInteger("Status", status); tag.setInteger("MaxRadius", ManagerConfig.maxRadius);
        return tag;
    }
    public void acceptGuiState(NBTTagCompound tag) {
        energy.setCapacity(tag.getDouble("Capacity")); energy.setEnergyStored(tag.getDouble("EU")); energy.setSinkTier(tag.getInteger("Tier"));
        int cap = tag.getInteger("TankCapacity");
        if (cap > 0) { water.setCapacity(cap); weed.setCapacity(cap); }
        water.setFluid(tag.getInteger("Water") > 0 ? new FluidStack(FluidRegistry.WATER, tag.getInteger("Water")) : null);
        weed.setFluid(tag.getInteger("Weed") > 0 ? new FluidStack(FluidName.weed_ex.getInstance(), tag.getInteger("Weed")) : null);
        radius = tag.getInteger("Radius"); layers = tag.getInteger("Layers"); functions = tag.getInteger("Functions");
        enabled = !tag.hasKey("Enabled") || tag.getBoolean("Enabled"); automation.read(tag.getCompoundTag("Automation"));
        redstoneMode = tag.getInteger("Redstone"); paused = tag.getBoolean("Paused");
        report = tag.getCompoundTag("Report"); report.setLong("Age", tag.getLong("ReportAge"));
        clientCursor = tag.getInteger("Cursor");
        status = tag.getInteger("Status"); serverMaxRadius = tag.getInteger("MaxRadius");
    }
    public int clientCursor;
    public static boolean redstoneAllows(int mode, boolean powered) { return mode == 0 || mode == 1 && powered || mode == 2 && !powered; }
    public String stateKey() {
        return !enabled ? "manual_off" : automation.stopped ? "automation_stop" : paused ? "paused" : (status & NO_ENERGY) != 0 ? "no_energy" : "running";
    }
    @Override public boolean control(int action, EntityPlayer player) {
        if (action == 200) { enabled = !enabled; markDirty(); return true; }
        if (action == 201) { settings(radius, layers, (functions ^ 1) | 14); return true; }
        if (automation.control(action)) { markDirty(); return true; }
        if (action >= 210 && action <= 214) {
            int control = action < 212 ? action - 210 + 4 : action - 212 + 6;
            int[] next = ManagerSettings.applyControl(control, radius, layers, functions, ManagerConfig.maxRadius);
            if (next == null) return false; settings(next[0], next[1], next[2]); return true;
        }
        if (action == 215) { settings(ManagerConfig.defaultRadius, ManagerConfig.defaultLayers, functions); return true; }
        if (action == 13) { redstoneMode = (redstoneMode + 1) % 3; progress = 0; markDirty(); return true; }
        return false;
    }
    void resetReport() { cursor = 0; counts = new int[6]; survey = new CropSurvey(); report = new NBTTagCompound(); reportTime = -1; }
    private void inspect(TileEntityCrop crop, net.minecraft.util.math.BlockPos position) {
        CropCard card = crop.getCrop(); if (card == null) return;
        survey.add(card.getOwner(), card.getId(), crop.getScanLevel() >= 1, card == ic2.api.crops.Crops.weed,
                new int[] {crop.getTerrainNutrients(), crop.getTerrainHumidity(), crop.getTerrainAirQuality(), crop.getLightLevel()});
        counts[0]++;
        if (card.canBeHarvested(crop)) counts[1]++;
        if (card == ic2.api.crops.Crops.weed) counts[2]++;
    }
    private void finishReport() {
        report = new NBTTagCompound(); report.setIntArray("Counts", counts);
        report.setTag("Survey", survey.write());
        reportTime = world.getTotalWorldTime(); counts = new int[6]; survey = new CropSurvey();
    }
    @Override public void dropContents() {
        dropHandler(inventory);
        for (ItemStack s : pending) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), s);
        pending.clear(); markDirty();
    }
}
