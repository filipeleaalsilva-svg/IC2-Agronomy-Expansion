package br.cropmanager;

import br.cropmanager.crop.BaseCropCard;
import ic2.api.crops.CropCard;
import ic2.api.crops.ICropSeed;
import ic2.api.energy.prefab.BasicSink;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Máquina de suporte Tier 1 que analisa sacos de sementes do IC2 em lote até o nível 4.
 *
 * <p>Sementes já analisadas avançam direto para a saída sem custo de EU apenas para atualizar a
 * telemetria exibida na GUI; a automação externa só consegue inserir sementes que ainda precisem
 * de análise para evitar loops com baús de armazenamento.
 */
public final class SeedAnalyzerTile extends PoweredMachineTile {
    public int clientDuration = ManagerConfig.analyzerDuration;
    public SeedAnalyzerTile() {
        super(ManagerConfig.analyzerEnergyCapacity, ManagerConfig.analyzerSinkTier,
                ManagerConfig.analyzerDuration, ManagerConfig.analyzerEuPerTick, "analyzer_input");
    }
    @Override protected ItemStackHandler machineInventory() { return inventory; }
    @Override protected int upgradeStartSlot() { return 2; }
    @Override protected int upgradeEndSlot() { return 6; }
    @Override protected int batterySlot() { return 6; }
    @Override protected int baseDuration() { return ManagerConfig.analyzerDuration; }
    @Override protected double baseEuPerTick() { return ManagerConfig.analyzerEuPerTick; }
    @Override protected int baseEnergyCapacity() { return ManagerConfig.analyzerEnergyCapacity; }
    @Override protected int baseSinkTier() { return ManagerConfig.analyzerSinkTier; }
    @Override protected boolean processMachineTick() { return processTick(); }

    // Last analyzed seed telemetry (synced to GUI and persisted)
    public String lastSpecies = "";
    public int lastTier, lastGr, lastGa, lastRe;
    public boolean lastCatalyzed;
    public String lastAttributes = "";

    public static boolean isSeedBag(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ICropSeed;
    }

    public static boolean needsScan(ItemStack stack) {
        return isSeedBag(stack) && ((ICropSeed) stack.getItem()).getScannedFromStack(stack) < 4;
    }

    public final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 1 || isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == 0) return isSeedBag(stack);
            if (slot == 6) return isBattery(stack);
            return slot >= 2 && slot < 6 && ManagerTile.isIc2Upgrade(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot >= 2 && slot < 6) upgradesDirty = true;
            markDirty();
        }
    };

    boolean processTick() {
        if (upgradesDirty) recalculateUpgrades();
        ItemStack input = inventory.getStackInSlot(0);
        if (!isSeedBag(input)) {
            if (progress != 0) { progress = 0; markDirty(); }
            condition = "analyzer_input";
            return false;
        }
        ICropSeed seed = (ICropSeed) input.getItem();
        ItemStack result = input.copy();
        result.setCount(1);
        boolean alreadyScanned = seed.getScannedFromStack(result) >= 4;
        if (!alreadyScanned) seed.setScannedFromStack(result, 4);

        if (!inventory.insertItem(1, result, true).isEmpty()) {
            condition = "no_space";
            return false;
        }

        // Already analyzed seeds pass straight through for free and refresh the telemetry display.
        if (alreadyScanned) {
            inventory.extractItem(0, 1, false);
            inventory.insertItem(1, result, false);
            recordAnalysis(result);
            progress = 0;
            condition = "analyzer_ready";
            markDirty();
            return false;
        }

        if (!energy.useEnergy(euPerTick)) {
            condition = "no_energy";
            return false;
        }

        condition = "analyzer_working";
        progress++;
        markDirty();
        if (progress >= workTicks) {
            inventory.extractItem(0, 1, false);
            inventory.insertItem(1, result, false);
            recordAnalysis(result);
            progress = 0;
            condition = "analyzer_ready";
            markDirty();
        }
        return true;
    }

    private void recordAnalysis(ItemStack analyzed) {
        if (!isSeedBag(analyzed)) return;
        ICropSeed seed = (ICropSeed) analyzed.getItem();
        CropCard card = seed.getCropFromStack(analyzed);
        if (card == null) {
            lastSpecies = "";
            lastTier = 0;
            lastGr = lastGa = lastRe = 0;
            lastCatalyzed = false;
            lastAttributes = "";
            return;
        }
        lastSpecies = card.getOwner() + ":" + card.getId();
        lastTier = card.getProperties() == null ? 0 : card.getProperties().getTier();
        lastGr = seed.getGrowthFromStack(analyzed);
        lastGa = seed.getGainFromStack(analyzed);
        lastRe = seed.getResistanceFromStack(analyzed);
        lastCatalyzed = card instanceof BaseCropCard && ((BaseCropCard) card).requiresCatalyst();
        String[] attrs = card.getAttributes();
        lastAttributes = attrs == null ? "" : String.join(", ", attrs);
    }

    public IItemHandler itemsForFace(@Nullable EnumFacing face) {
        return new IItemHandler() {
            @Override public int getSlots() { return inventory.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                // Automation only feeds unscanned seeds to prevent loops with storage chests.
                if (slot != 0 || !needsScan(stack)) return stack;
                return inventory.insertItem(slot, stack, simulate);
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 1 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        };
    }

    @Override public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing face) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(cap, face);
    }

    @Override public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing face) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemsForFace(face));
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
        tag.setString("LastSpecies", lastSpecies);
        tag.setInteger("LastTier", lastTier);
        tag.setInteger("LastGr", lastGr);
        tag.setInteger("LastGa", lastGa);
        tag.setInteger("LastRe", lastRe);
        tag.setBoolean("LastCatalyzed", lastCatalyzed);
        tag.setString("LastAttributes", lastAttributes);
        return tag;
    }

    @Override
    public void acceptGuiState(NBTTagCompound tag) {
        energy.setCapacity(tag.getDouble("Capacity"));
        energy.setSinkTier(Math.max(1, tag.getInteger("Tier")));
        energy.setEnergyStored(tag.getDouble("EU"));
        clientDuration = Math.max(1, tag.getInteger("Duration"));
        workTicks = clientDuration;
        progress = tag.getInteger("Work");
        euPerTick = tag.getDouble("Demand");
        condition = tag.getString("Condition");
        lastSpecies = tag.getString("LastSpecies");
        lastTier = tag.getInteger("LastTier");
        lastGr = tag.getInteger("LastGr");
        lastGa = tag.getInteger("LastGa");
        lastRe = tag.getInteger("LastRe");
        lastCatalyzed = tag.getBoolean("LastCatalyzed");
        lastAttributes = tag.getString("LastAttributes");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writePoweredNBT(tag);
        tag.setString("LastSpecies", lastSpecies);
        tag.setInteger("LastTier", lastTier);
        tag.setInteger("LastGr", lastGr);
        tag.setInteger("LastGa", lastGa);
        tag.setInteger("LastRe", lastRe);
        tag.setBoolean("LastCatalyzed", lastCatalyzed);
        tag.setString("LastAttributes", lastAttributes);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        lastSpecies = tag.getString("LastSpecies");
        lastTier = tag.getInteger("LastTier");
        lastGr = tag.getInteger("LastGr");
        lastGa = tag.getInteger("LastGa");
        lastRe = tag.getInteger("LastRe");
        lastCatalyzed = tag.getBoolean("LastCatalyzed");
        lastAttributes = tag.getString("LastAttributes");
        readPoweredNBT(tag);
    }

    @Override EquipmentLayout equipmentLayout() {
        return new EquipmentLayout(inventory, 126,
                new int[]{34, 110, 148, 148, 148, 148, 8},
                new int[]{24, 24, 22, 40, 58, 76, 62},
                1, false,
                s -> isBattery(s) ? new int[]{6, 7} : ManagerTile.isIc2Upgrade(s) ? new int[]{2, 6} : new int[]{0, 1});
    }
}
