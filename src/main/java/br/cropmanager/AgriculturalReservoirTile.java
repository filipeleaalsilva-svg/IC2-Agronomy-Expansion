package br.cropmanager;

import ic2.api.energy.prefab.BasicSink;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Master TileEntity for the 3x3xH two-fluid agricultural reservoir. */
public final class AgriculturalReservoirTile extends FarmTile implements ITickable {
    public static final int MIN_HEIGHT = 4;
    public static final int MAX_HEIGHT = 11;
    public static final int CAPACITY_PER_HEIGHT = 8000;
    public static final int ENERGY_CAPACITY = 20000;
    public static final int ENERGY_TIER = 1;
    public static final int TRANSFER_INTERVAL = 20;
    public static final int MAX_AUTO_TRANSFER = 1000;
    public static final int EQUALIZE_THRESHOLD = 10;

    private enum Validation { VALID, INVALID, DEFER }
    private static final String[] TANK_KEYS = {"Tank0", "Tank1"};
    private static final String[] LEGACY_TANK_KEYS = {"Water", "Weed"};
    public static final int FIXED_HEIGHT = 5;
    public static final int BASE_CAPACITY = 32000;
    static final int CAPACITY_SLOT = 4;
    static final int FIRST_MODULE_SLOT = 5;
    static final int INVENTORY_SLOTS = 7;
    static final int CLOSED = 0, INPUT = 1, OUTPUT = 2;
    private static final int BOTH = 2;
    private final FluidStack[] filters = new FluidStack[2];
    private final int[] portModes = {CLOSED, CLOSED, OUTPUT, INPUT, CLOSED, CLOSED};
    private final int[] portTanks = {BOTH, BOTH, BOTH, BOTH, BOTH, BOTH};
    private final int[] redstoneModes = new int[2];
    private final int[] reserveSteps = new int[2];
    private final ItemStackHandler pendingReturns = new ItemStackHandler(2);
    private final IFluidHandler[] sideHandlers = new IFluidHandler[6];
    private boolean legacyShape;
    private boolean legacyPorts;
    private int comparatorTank = BOTH;
    private int previousSignal = -1;

    public final BasicSink energy = createSink(ENERGY_CAPACITY, ENERGY_TIER);
    public final FluidTank water = new LockedTank(0);
    public final FluidTank weed = new LockedTank(1);
    public final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SLOTS) {
        @Override public int getSlotLimit(int slot) { return slot == CAPACITY_SLOT ? 4 : 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot < 2) return FluidUtil.getFluidHandler(stack) != null || FluidUtil.getFluidContained(stack) != null;
            if (slot < CAPACITY_SLOT) return false;
            if (slot == CAPACITY_SLOT) return stack.getItem() == CropManager.RESERVOIR_CAPACITY;
            return moduleType(stack) != 0 && moduleType(stack) != moduleType(getStackInSlot(slot == 5 ? 6 : 5));
        }
        @Override protected void onContentsChanged(int slot) {
            inventoryChanged(slot);
        }
    };

    void inventoryChanged(int slot) {
        if (slot >= CAPACITY_SLOT) refreshUpgrades(true);
        changed();
    }

    private IFluidHandler port(@Nullable EnumFacing face) {
        return new IFluidHandler() {
            @Override public IFluidTankProperties[] getTankProperties() {
                if (!formed || isInvalid()) return new IFluidTankProperties[0];
                List<IFluidTankProperties> result = new ArrayList<>(2);
                for (int i = 0; i < 2; i++) {
                    boolean input = allows(face, i, INPUT);
                    boolean output = allows(face, i, OUTPUT);
                    if (input || output) result.add(properties(i, input, output));
                }
                return result.toArray(new IFluidTankProperties[result.size()]);
            }

            @Override public int fill(FluidStack resource, boolean execute) {
                if (!formed || isInvalid() || resource == null || resource.amount <= 0) return 0;
                boolean first = allows(face, 0, INPUT);
                boolean second = allows(face, 1, INPUT);
                boolean filteredFirst = first && resource.isFluidEqual(filterFluid(0));
                boolean filteredSecond = second && resource.isFluidEqual(filterFluid(1));
                if (filteredFirst || filteredSecond) {
                    first = filteredFirst;
                    second = filteredSecond;
                }
                int filled = first ? water.fill(resource, execute) : 0;
                return filled > 0 || !second ? filled : weed.fill(resource, execute);
            }

            @Override public FluidStack drain(FluidStack resource, boolean execute) {
                return resource == null ? null : drainMatching(resource, resource.amount, execute);
            }

            @Override public FluidStack drain(int amount, boolean execute) {
                return drainMatching(null, amount, execute);
            }

            private FluidStack drainMatching(@Nullable FluidStack fluid, int amount, boolean execute) {
                if (!formed || isInvalid() || amount <= 0) return null;
                for (int i = 0; i < 2; i++) {
                    FluidTank source = tank(i);
                    int drainable = Math.min(amount, available(i));
                    if (drainable > 0 && allows(face, i, OUTPUT)
                            && (fluid == null || fluid.isFluidEqual(source.getFluid()))) {
                        return source.drain(drainable, execute);
                    }
                }
                return null;
            }
        };
    }

    private final IFluidHandler unsidedPort = port(null);

    public int height = FIXED_HEIGHT;
    public int baseY;
    public BlockPos center;
    public boolean formed;
    public boolean autoWater = true;
    public boolean autoWeed = true;
    /** Sub-50mB EU charge carried independently by each auto-transfer tank. */
    public final int[] euRemainder = new int[2];

    public AgriculturalReservoirTile() {
        for (EnumFacing face : EnumFacing.values()) sideHandlers[face.getIndex()] = port(face);
    }

    public static int capacityForHeight(int height) {
        int clamped = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
        return CAPACITY_PER_HEIGHT * clamped;
    }

    private FluidTank tank(int index) { return index == 0 ? water : weed; }
    private boolean autoEnabled(int index) { return index == 0 ? autoWater : autoWeed; }
    @Nullable FluidStack filterFluid(int index) { return filters[index]; }

    int comparatorTank() { return comparatorTank; }
    boolean usesLegacyPorts() { return legacyPorts; }
    int portMode(int index) { return portModes[index]; }
    int portTank(int index) { return portTanks[index]; }
    int redstoneMode(int index) { return redstoneModes[index]; }
    int reserveStep(int index) { return reserveSteps[index]; }

    private EnumFacing relativeFace(EnumFacing face) {
        if (face.getAxis().isVertical() || world == null) return face;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.AGRICULTURAL_RESERVOIR) return face;
        EnumFacing front = state.getValue(AgriculturalReservoirBlock.FACING);
        if (face == front) return EnumFacing.SOUTH;
        if (face == front.getOpposite()) return EnumFacing.NORTH;
        return face == front.rotateYCCW() ? EnumFacing.EAST : EnumFacing.WEST;
    }

    private boolean allows(@Nullable EnumFacing side, int index, int mode) {
        if (legacyPorts) return true;
        if (side == null) {
            for (int i = 0; i < portModes.length; i++)
                if (portModes[i] == mode && (portTanks[i] == BOTH || portTanks[i] == index)) return true;
            return false;
        }
        int i = relativeFace(side).getIndex();
        return portModes[i] == mode && (portTanks[i] == BOTH || portTanks[i] == index);
    }

    boolean isExterior(BlockPos at, @Nullable EnumFacing side) {
        if (side == null || center == null) return side == null;
        return side == EnumFacing.DOWN && at.getY() == baseY
                || side == EnumFacing.UP && at.getY() == baseY + height - 1
                || side.getAxis().isHorizontal()
                && (at.getX() - center.getX()) * side.getXOffset()
                + (at.getZ() - center.getZ()) * side.getZOffset() == 1;
    }

    static int moduleType(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == CropManager.RESERVOIR_PUMP) return 1;
        if (stack.getItem() == CropManager.RESERVOIR_RESERVE) return 2;
        ItemStack transformer = ic2.api.item.IC2Items.getItem("upgrade", "transformer");
        return transformer != null && ItemStack.areItemsEqual(stack, transformer) ? 3 : 0;
    }

    boolean hasModule(int type) {
        return moduleType(inventory.getStackInSlot(5)) == type || moduleType(inventory.getStackInSlot(6)) == type;
    }

    private int available(int index) {
        FluidTank tank = tank(index);
        int reserve = hasModule(2) ? tank.getCapacity() * reserveSteps[index] / 4 : 0;
        return Math.max(0, tank.getFluidAmount() - reserve);
    }

    private void refreshUpgrades(boolean reconnect) {
        int capacity = (legacyShape ? capacityForHeight(height) : BASE_CAPACITY)
                + 16000 * Math.min(4, inventory.getStackInSlot(CAPACITY_SLOT).getCount());
        water.setCapacity(capacity);
        weed.setCapacity(capacity);
        int tier = hasModule(3) ? 2 : ENERGY_TIER;
        if (tier != energy.getSinkTier()) {
            energy.setSinkTier(tier);
            if (reconnect && world != null && !world.isRemote) {
                energy.invalidate();
                if (formed && center != null) for (BlockPos block : shellPositions(center, baseY, height)) {
                    if (!world.isBlockLoaded(block, false)) continue;
                    TileEntity casing = world.getTileEntity(block);
                    if (casing instanceof ReservoirCasingTile) ((ReservoirCasingTile) casing).refreshEnergyConnection();
                }
            }
        }
    }
    private boolean acceptsFluid(int index, FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) return false;
        FluidStack filter = filterFluid(index);
        if (filter != null) return filter.isFluidEqual(fluid);
        FluidStack current = tank(index).getFluid();
        return current == null || current.isFluidEqual(fluid);
    }

    private final class LockedTank extends FluidTank {
        private final int index;
        LockedTank(int index) { super(BASE_CAPACITY); this.index = index; }
        @Override public boolean canFillFluidType(FluidStack fluid) { return acceptsFluid(index, fluid); }
        @Override public int fill(FluidStack resource, boolean doFill) {
            if (getFluidAmount() >= getCapacity() || !canFillFluidType(resource)) return 0;
            return super.fill(resource, doFill);
        }
        @Override protected void onContentsChanged() { changed(); }
    }

    private IFluidTankProperties properties(final int index, boolean input, boolean output) {
        final FluidTank tank = tank(index);
        return new IFluidTankProperties() {
            @Override public FluidStack getContents() { return tank.getFluid() == null ? null : tank.getFluid().copy(); }
            @Override public int getCapacity() { return tank.getCapacity(); }
            @Override public boolean canFill() { return input && tank.getFluidAmount() < tank.getCapacity(); }
            @Override public boolean canDrain() { return output && available(index) > 0; }
            @Override public boolean canFillFluidType(FluidStack fluid) { return canFill() && acceptsFluid(index, fluid); }
            @Override public boolean canDrainFluidType(FluidStack fluid) {
                return canDrain() && fluid != null && fluid.isFluidEqual(tank.getFluid());
            }
        };
    }

    @Override public BasicSink energySink() { return energy; }
    @Override public void invalidate() { energy.invalidate(); super.invalidate(); }
    @Override public void onChunkUnload() { energy.onChunkUnload(); super.onChunkUnload(); }
    @Override public boolean isSupportMachine() { return true; }
    public double getEnergy() { return energy.getEnergyStored(); }
    public double injectCableEnergy(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return 0;
        if (!formed) return amount;
        double accepted = Math.min(amount, energy.getFreeCapacity());
        if (accepted <= 0) return amount;
        energy.setEnergyStored(energy.getEnergyStored() + accepted);
        markDirty();
        return amount - accepted;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        energy.update();
        if (world.getTotalWorldTime() % TRANSFER_INTERVAL != 0) return;
        if (!formed) {
            tryForm();
            return;
        }
        Validation validation = validateCurrent();
        if (validation == Validation.INVALID) {
            setFormed(false);
            return;
        }
        if (validation == Validation.DEFER) return;
        processContainers();
        transferToFarmTargets();
        if (legacyPorts) equalizeTouchingReservoirs();
    }

    /** Tries the three permitted controller levels without loading chunks. */
    public void tryForm() {
        if (world == null || world.isRemote || isInvalid()) return;
        if (formed) {
            Validation current = validateCurrent();
            if (current == Validation.VALID || current == Validation.DEFER) return;
            setFormed(false);
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.AGRICULTURAL_RESERVOIR) return;
        EnumFacing facing = state.getValue(AgriculturalReservoirBlock.FACING);
        BlockPos proposedCenter = pos.offset(facing.getOpposite());
        Structure found = null;
        boolean deferred = false;
        for (int relativeY = 1; relativeY < FIXED_HEIGHT - 1; relativeY++) {
            int candidateBase = pos.getY() - relativeY;
            Validation result = validateStructure(proposedCenter, candidateBase, FIXED_HEIGHT, facing);
            if (result == Validation.DEFER) deferred = true;
            else if (result == Validation.VALID) {
                if (found != null) return;
                found = new Structure(proposedCenter, candidateBase, FIXED_HEIGHT);
            }
        }
        if (found != null) {
            legacyShape = false;
            legacyPorts = false;
            form(found);
        } else if (!deferred) setFormed(false);
    }

    private Validation validateCurrent() {
        if (center == null || height < MIN_HEIGHT || height > MAX_HEIGHT) return Validation.INVALID;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.AGRICULTURAL_RESERVOIR) return Validation.INVALID;
        return validateStructure(center, baseY, height, state.getValue(AgriculturalReservoirBlock.FACING));
    }

    private Validation validateStructure(BlockPos centerPos, int floorY, int candidateHeight, EnumFacing facing) {
        if (candidateHeight < MIN_HEIGHT || candidateHeight > MAX_HEIGHT || floorY < 0
                || floorY + candidateHeight > world.getHeight()) return Validation.INVALID;
        if (pos.getY() <= floorY || pos.getY() >= floorY + candidateHeight - 1
                || !controllerPosition(centerPos, pos.getY(), facing).equals(pos)) return Validation.INVALID;
        for (int y = 0; y < candidateHeight; y++) {
            int worldY = floorY + y;
            boolean horizontalCap = y == 0 || y == candidateHeight - 1;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos check = new BlockPos(centerPos.getX() + dx, worldY, centerPos.getZ() + dz);
                if (!world.isBlockLoaded(check, false)) return Validation.DEFER;
                boolean interior = dx == 0 && dz == 0;
                if (interior && !horizontalCap) {
                    if (world.getBlockState(check).getBlock() != Blocks.AIR) return Validation.INVALID;
                    continue;
                }
                boolean perimeter = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if (!horizontalCap && !perimeter) return Validation.INVALID;
                Block block = world.getBlockState(check).getBlock();
                Block expected = check.equals(pos) ? CropManager.AGRICULTURAL_RESERVOIR : CropManager.RESERVOIR_CASING;
                if (block != expected) return Validation.INVALID;
            }
        }
        return Validation.VALID;
    }

    private BlockPos controllerPosition(BlockPos centerPos, int y, EnumFacing facing) {
        return centerPos.offset(facing).up(y - centerPos.getY());
    }

    private void form(Structure structure) {
        center = structure.center;
        baseY = structure.baseY;
        height = structure.height;
        refreshUpgrades(false);
        setFormed(true);
        for (BlockPos shell : shellPositions(center, baseY, height)) {
            if (!world.isBlockLoaded(shell, false) || shell.equals(pos)) continue;
            TileEntity tile = world.getTileEntity(shell);
            if (tile instanceof ReservoirCasingTile) ((ReservoirCasingTile) tile).setController(pos);
        }
        updateShellComparators();
    }

    ReservoirCasingBlock.Part renderPart(BlockPos casingPos) {
        if (usesTowerModel()) return ReservoirCasingBlock.Part.TOWER;
        if (!formed || center == null) return ReservoirCasingBlock.Part.CASING;
        if (casingPos.getY() == baseY + height - 1) return ReservoirCasingBlock.Part.RAILING;
        if (casingPos.getY() <= baseY || casingPos.getY() >= baseY + height - 1)
            return ReservoirCasingBlock.Part.CASING;
        IBlockState controllerState = world.getBlockState(pos);
        EnumFacing outward = controllerState.getValue(AgriculturalReservoirBlock.FACING);
        EnumFacing lateral = outward.rotateY();
        int depth = (casingPos.getX() - center.getX()) * outward.getXOffset()
                + (casingPos.getZ() - center.getZ()) * outward.getZOffset();
        int side = (casingPos.getX() - center.getX()) * lateral.getXOffset()
                + (casingPos.getZ() - center.getZ()) * lateral.getZOffset();
        if (depth == 1 && side == 1) return ReservoirCasingBlock.Part.LADDER;
        if (Math.abs(depth) == 1 && side == 0) return ReservoirCasingBlock.Part.GLASS;
        return ReservoirCasingBlock.Part.CASING;
    }

    private void setFormed(boolean value) {
        if (formed == value) return;
        formed = value;
        if (!value) unlinkCasings();
        changed();
    }

    void unformOnBreak() {
        if (!formed) return;
        formed = false;
        unlinkCasings();
        previousSignal = 0;
        updateShellComparators();
    }

    private void unlinkCasings() {
        if (world == null || world.isRemote || center == null) return;
        for (BlockPos shell : shellPositions(center, baseY, height)) {
            if (shell.equals(pos) || !world.isBlockLoaded(shell, false)) continue;
            TileEntity tile = world.getTileEntity(shell);
            if (tile instanceof ReservoirCasingTile) ((ReservoirCasingTile) tile).clearController(pos);
        }
    }

    void processContainers() {
        for (int i = 0; i < 2; i++) {
            int output = i + 2;
            if (!inventory.getStackInSlot(output).isEmpty()) continue;
            ItemStack returned = pendingReturns.getStackInSlot(i);
            if (!returned.isEmpty()) {
                pendingReturns.setStackInSlot(i, ItemStack.EMPTY);
                inventory.setStackInSlot(output, returned);
                continue;
            }
            ItemStack source = inventory.getStackInSlot(i);
            if (source.isEmpty()) continue;
            FluidTank tank = tank(i);
            FluidActionResult result = FluidUtil.getFluidContained(source) == null
                    ? FluidUtil.tryFillContainer(source.copy(), tank, Integer.MAX_VALUE, null, true)
                    : FluidUtil.tryEmptyContainer(source.copy(), tank, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                inventory.setStackInSlot(output, result.getResult());
            }
        }
    }

    private boolean redstoneAllows(int i) {
        int mode = redstoneModes[i];
        if (mode == 0) return true;
        if (world == null || center == null) return mode == 2;
        for (BlockPos casing : shellPositions(center, baseY, height))
            if (world.isBlockLoaded(casing, false) && world.isBlockPowered(casing)) return mode == 1;
        return mode == 2;
    }

    private void transferToFarmTargets() {
        for (int tankIndex = 0; tankIndex < 2; tankIndex++) {
            if (!autoEnabled(tankIndex) || available(tankIndex) <= 0 || !redstoneAllows(tankIndex)) continue;
            Set<BlockPos> visited = new HashSet<>();
            for (BlockPos shell : shellPositions(center, baseY, height)) {
                for (EnumFacing face : EnumFacing.values()) {
                    if (!isExterior(shell, face) || !allows(face, tankIndex, OUTPUT)) continue;
                    BlockPos targetPos = shell.offset(face);
                    if (!world.isBlockLoaded(targetPos, false) || !visited.add(targetPos)) continue;
                    TileEntity target = world.getTileEntity(targetPos);
                    if (!(target instanceof ManagerTile)
                            && !(target instanceof ic2.core.block.machine.tileentity.TileEntityCropmatron)) continue;
                    IFluidHandler handler = target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
                    if (handler != null) transferToHandler(tankIndex, handler);
                    if (tank(tankIndex).getFluidAmount() == 0) break;
                }
                if (tank(tankIndex).getFluidAmount() == 0) break;
            }
        }
    }

    void transferToHandler(int tankIndex, IFluidHandler destination) {
        FluidTank source = tank(tankIndex);
        FluidStack current = source.getFluid();
        if (current == null) return;
        int proposed = Math.min(hasModule(1) ? MAX_AUTO_TRANSFER * 2 : MAX_AUTO_TRANSFER, available(tankIndex));
        proposed = affordableTransfer(tankIndex, proposed);
        if (proposed <= 0) return;
        int affordable = proposed;
        int cost = energyCostFor(euRemainder[tankIndex], affordable);
        if (energy.getEnergyStored() < cost) return;
        int accepted = Math.min(affordable, destination.fill(new FluidStack(current, affordable), false));
        if (accepted <= 0) return;
        FluidStack drained = source.drain(accepted, true);
        if (drained == null || drained.amount != accepted) return;
        int inserted = Math.max(0, Math.min(accepted, destination.fill(drained.copy(), true)));
        if (inserted < accepted) {
            // Restore the drained volume even if a shorter shell left this tank above capacity.
            source.setFluid(new FluidStack(drained, source.getFluidAmount() + accepted - inserted));
            changed();
        }
        if (inserted <= 0) return;
        int actualCost = energyCostFor(euRemainder[tankIndex], inserted);
        if (actualCost > 0) energy.useEnergy(actualCost);
        euRemainder[tankIndex] = (euRemainder[tankIndex] + inserted) % 50;
        markDirty();
    }

    private int affordableTransfer(int tankIndex, int proposed) {
        int availableEu = (int) Math.floor(Math.max(0, energy.getEnergyStored()));
        int maximum = availableEu * 50 + 49 - euRemainder[tankIndex];
        return Math.max(0, Math.min(proposed, maximum));
    }

    public static int energyCostFor(int previousRemainder, int millibuckets) {
        int remainder = Math.max(0, Math.min(49, previousRemainder));
        return (remainder + Math.max(0, millibuckets)) / 50;
    }

    private void equalizeTouchingReservoirs() {
        if (center == null || height < MIN_HEIGHT) return;
        Set<BlockPos> neighbors = new HashSet<>();
        for (BlockPos shell : shellPositions(center, baseY, height)) {
            for (EnumFacing side : EnumFacing.values()) {
                BlockPos adjacent = shell.offset(side);
                if (!world.isBlockLoaded(adjacent, false)) continue;
                TileEntity tile = world.getTileEntity(adjacent);
                AgriculturalReservoirTile other = tile instanceof AgriculturalReservoirTile
                        ? (AgriculturalReservoirTile) tile
                        : tile instanceof ReservoirCasingTile ? ((ReservoirCasingTile) tile).controller() : null;
                if (other == null || other == this || !other.formed || !other.legacyPorts || other.pos.compareTo(pos) <= 0 || !neighbors.add(other.pos)) continue;
                equalizeWith(other);
            }
        }
    }

    void equalizeWith(AgriculturalReservoirTile other) {
        for (int first = 0; first < 2; first++) for (int second = 0; second < 2; second++) {
            FluidTank a = tank(first), b = other.tank(second);
            FluidStack fa = a.getFluid(), fb = b.getFluid();
            FluidStack fluid = fa != null ? fa : fb;
            if (fluid == null || (fa != null && fb != null && !fa.isFluidEqual(fb))) continue;
            if (fa == null && !fluid.isFluidEqual(filterFluid(first))
                    || fb == null && !fluid.isFluidEqual(other.filterFluid(second))) continue;
            int difference = a.getFluidAmount() - b.getFluidAmount();
            if (Math.abs(difference) <= EQUALIZE_THRESHOLD) continue;
            FluidTank from = difference > 0 ? a : b;
            FluidTank to = difference > 0 ? b : a;
            int amount = Math.min(Math.abs(difference) / 2,
                    Math.min(difference > 0 ? available(first) : other.available(second),
                            Math.max(0, to.getCapacity() - to.getFluidAmount())));
            if (amount <= 0) continue;
            FluidStack moved = from.drain(amount, false);
            if (moved == null || to.fill(moved, false) != amount) continue;
            from.drain(amount, true);
            to.fill(moved, true);
            markDirty(); other.markDirty();
        }
    }

    List<BlockPos> shellPositions(BlockPos structureCenter, int floorY, int structureHeight) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < structureHeight; y++) {
            boolean cap = y == 0 || y == structureHeight - 1;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (cap || Math.abs(dx) == 1 || Math.abs(dz) == 1)
                    positions.add(new BlockPos(structureCenter.getX() + dx, floorY + y, structureCenter.getZ() + dz));
            }
        }
        return positions;
    }

    private void changed() {
        markDirty();
        if (world != null && !world.isRemote && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            int signal = comparatorSignal();
            if (signal != previousSignal) {
                previousSignal = signal;
                if (state.getBlock() == CropManager.AGRICULTURAL_RESERVOIR) {
                    world.updateComparatorOutputLevel(pos, state.getBlock());
                }
                updateShellComparators();
            }
        }
    }

    private void updateShellComparators() {
        if (world != null && !world.isRemote) {
            StorageMultiblockSupport.updateShellComparators(
                    world, pos, center, baseY, height, CropManager.RESERVOIR_CASING);
        }
    }

    int comparatorSignal() {
        if (!formed) return 0;
        double ratio = comparatorTank == BOTH
                ? (water.getFluidAmount() + (double) weed.getFluidAmount()) / (water.getCapacity() + weed.getCapacity())
                : (double) tank(comparatorTank).getFluidAmount() / tank(comparatorTank).getCapacity();
        return ratio <= 0 ? 0 : Math.min(15, 1 + (int) Math.floor(ratio * 14));
    }

    void casingBroken(BlockPos casingPos) {
        if (formed && containsCasing(casingPos)) setFormed(false);
    }

    @Override public boolean usable(EntityPlayer player) {
        return super.usable(player) || (formed && StorageMultiblockSupport.canUseFromShell(
                this, player, center, baseY, height, CropManager.RESERVOIR_CASING));
    }

    boolean containsCasing(BlockPos casingPos) {
        return StorageMultiblockSupport.containsCasing(pos, center, baseY, height, casingPos);
    }

    public NBTTagCompound writeItemData() {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.removeTag("x"); tag.removeTag("y"); tag.removeTag("z");
        tag.setBoolean("ReservoirFormed", false);
        tag.removeTag("StructureCenter");
        tag.removeTag("StructureBaseY");
        tag.removeTag("StructureHeight");
        tag.setBoolean("LegacyShape", false);
        return tag;
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        energy.writeToNBT(tag);
        tag.setTag(TANK_KEYS[0], water.writeToNBT(new NBTTagCompound()));
        tag.setTag(TANK_KEYS[1], weed.writeToNBT(new NBTTagCompound()));
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setTag("PendingReturns", pendingReturns.serializeNBT());
        tag.setBoolean("LegacyShape", legacyShape);
        writeSettings(tag);
        tag.setInteger("StructureHeight", height);
        tag.setInteger("StructureBaseY", baseY);
        tag.setBoolean("ReservoirFormed", formed);
        if (center != null) tag.setLong("StructureCenter", center.toLong());
        tag.setBoolean("AutoWater", autoWater);
        tag.setBoolean("AutoWeed", autoWeed);
        tag.setInteger("EURemainder0", euRemainder[0]);
        tag.setInteger("EURemainder1", euRemainder[1]);
        return tag;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        boolean newData = tag.hasKey("ReservoirPorts", 11);
        if (newData) {
            readFixedInventory(inventory, tag.getCompoundTag("Inventory"), INVENTORY_SLOTS);
            readFixedInventory(pendingReturns, tag.getCompoundTag("PendingReturns"), 2);
            readSettings(tag);
        } else {
            ItemStackHandler old = new ItemStackHandler(4);
            readFixedInventory(old, tag.getCompoundTag("Inventory"), 4);
            for (int i = 0; i < 2; i++) {
                ItemStack oldFilter = old.getStackInSlot(i);
                filters[i] = FluidUtil.getFluidContained(oldFilter);
                pendingReturns.setStackInSlot(i, oldFilter);
                boolean processed = (tag.getInteger("ProcessedContainers") & (1 << (i + 2))) != 0;
                inventory.setStackInSlot(processed ? i + 2 : i, old.getStackInSlot(i + 2));
            }
            legacyPorts = tag.getBoolean("ReservoirFormed") || tag.hasKey("Inventory");
        }
        if (tag.hasKey(TANK_KEYS[0], 10) || tag.hasKey(TANK_KEYS[1], 10)) {
            StorageMultiblockSupport.readFluidTank(water, tag.getCompoundTag(TANK_KEYS[0]));
            StorageMultiblockSupport.readFluidTank(weed, tag.getCompoundTag(TANK_KEYS[1]));
        } else {
            StorageMultiblockSupport.readFluidTank(water, tag.getCompoundTag(LEGACY_TANK_KEYS[0]));
            StorageMultiblockSupport.readFluidTank(weed, tag.getCompoundTag(LEGACY_TANK_KEYS[1]));
        }
        energy.readFromNBT(tag);
        double eu = energy.getEnergyStored();
        energy.setCapacity(ENERGY_CAPACITY);
        energy.setSinkTier(ENERGY_TIER);
        energy.setEnergyStored(Double.isFinite(eu) ? Math.max(0, Math.min(ENERGY_CAPACITY, eu)) : 0);
        height = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, tag.getInteger("StructureHeight")));
        if (!tag.hasKey("StructureHeight")) height = FIXED_HEIGHT;
        baseY = tag.getInteger("StructureBaseY");
        center = tag.hasKey("StructureCenter", 4) ? BlockPos.fromLong(tag.getLong("StructureCenter")) : null;
        autoWater = !tag.hasKey("AutoWater") || tag.getBoolean("AutoWater");
        autoWeed = !tag.hasKey("AutoWeed") || tag.getBoolean("AutoWeed");
        euRemainder[0] = Math.max(0, Math.min(49, tag.getInteger("EURemainder0")));
        euRemainder[1] = Math.max(0, Math.min(49, tag.getInteger("EURemainder1")));
        boolean worldSave = tag.hasKey("x") && tag.hasKey("y") && tag.hasKey("z");
        formed = worldSave && tag.getBoolean("ReservoirFormed") && center != null;
        legacyShape = formed && (newData ? tag.getBoolean("LegacyShape") : true);
        refreshUpgrades(false);
    }

    @Override public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Water", water.getFluidAmount()); tag.setInteger("Weed", weed.getFluidAmount());
        tag.setInteger("TankCapacity", water.getCapacity()); tag.setDouble("EU", energy.getEnergyStored());
        tag.setDouble("EnergyCapacity", energy.getCapacity()); tag.setInteger("Tier", energy.getSinkTier());
        tag.setInteger("Height", height); tag.setBoolean("Formed", formed);
        tag.setBoolean("AutoWater", autoWater); tag.setBoolean("AutoWeed", autoWeed);
        FluidStack f0 = water.getFluid(), f1 = weed.getFluid();
        tag.setString("WaterFluid", f0 == null ? "" : f0.getFluid().getName());
        tag.setString("WeedFluid", f1 == null ? "" : f1.getFluid().getName());
        writeSettings(tag);
        return tag;
    }
    @Override public void acceptGuiState(NBTTagCompound tag) {
        height = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, tag.getInteger("Height")));
        int capacity = Math.max(BASE_CAPACITY, tag.getInteger("TankCapacity"));
        water.setCapacity(capacity); weed.setCapacity(capacity);
        readSettings(tag);
        setClientFluid(water, tag.getString("WaterFluid"), tag.getInteger("Water"));
        setClientFluid(weed, tag.getString("WeedFluid"), tag.getInteger("Weed"));
        energy.setCapacity(tag.getDouble("EnergyCapacity")); energy.setSinkTier(tag.getInteger("Tier"));
        energy.setEnergyStored(Math.max(0, Math.min(ENERGY_CAPACITY, tag.getDouble("EU"))));
        formed = tag.getBoolean("Formed"); autoWater = tag.getBoolean("AutoWater"); autoWeed = tag.getBoolean("AutoWeed");
    }
    private static void setClientFluid(FluidTank tank, String fluidName, int amount) {
        Fluid fluid = fluidName.isEmpty() ? null : FluidRegistry.getFluid(fluidName);
        tank.setFluid(fluid == null || amount <= 0 ? null : new FluidStack(fluid, amount));
    }

    private void writeSettings(NBTTagCompound tag) {
        tag.setIntArray("ReservoirPorts", portModes);
        tag.setIntArray("ReservoirPortTanks", portTanks);
        tag.setIntArray("ReservoirRedstone", redstoneModes);
        tag.setIntArray("ReservoirReserve", reserveSteps);
        tag.setInteger("ReservoirComparator", comparatorTank);
        tag.setBoolean("LegacyPorts", legacyPorts);
        for (int i = 0; i < 2; i++) {
            if (filters[i] != null) tag.setTag("Filter" + i, filters[i].writeToNBT(new NBTTagCompound()));
        }
    }

    private static void readFixedInventory(ItemStackHandler inventory, NBTTagCompound saved, int slots) {
        NBTTagCompound inventoryTag = saved.copy();
        inventoryTag.setInteger("Size", slots);
        inventory.deserializeNBT(inventoryTag);
    }

    private void readSettings(NBTTagCompound tag) {
        readModes(tag.getIntArray("ReservoirPorts"), portModes, 2);
        readModes(tag.getIntArray("ReservoirPortTanks"), portTanks, 2);
        readModes(tag.getIntArray("ReservoirRedstone"), redstoneModes, 2);
        readModes(tag.getIntArray("ReservoirReserve"), reserveSteps, 3);
        comparatorTank = Math.max(0, Math.min(BOTH, tag.getInteger("ReservoirComparator")));
        legacyPorts = tag.getBoolean("LegacyPorts");
        for (int i = 0; i < 2; i++) filters[i] = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("Filter" + i));
    }

    private static void readModes(int[] saved, int[] target, int max) {
        if (saved.length != target.length) return;
        for (int i = 0; i < target.length; i++)
            target[i] = Math.max(0, Math.min(max, saved[i]));
    }

    @Override public boolean control(int action, EntityPlayer player) {
        if (!formed || !usable(player)) return false;
        if (action == 0) autoWater = !autoWater;
        else if (action == 1) autoWeed = !autoWeed;
        else if (action == 2 || action == 3) redstoneModes[action - 2] = (redstoneModes[action - 2] + 1) % 3;
        else if (action == 4) comparatorTank = (comparatorTank + 1) % 3;
        else if (action >= 10 && action < 16) {
            legacyPorts = false;
            portModes[action - 10] = (portModes[action - 10] + 1) % 3;
        } else if (action >= 20 && action < 26) {
            legacyPorts = false;
            portTanks[action - 20] = (portTanks[action - 20] + 1) % 3;
        } else if (action == 30 || action == 31) {
            int i = action - 30;
            FluidStack sample = FluidUtil.getFluidContained(player.inventory.getItemStack());
            if (sample == null || tank(i).getFluid() != null && !sample.isFluidEqual(tank(i).getFluid())) return false;
            filters[i] = new FluidStack(sample, 1);
        } else if (action == 32 || action == 33) {
            if (!player.inventory.getItemStack().isEmpty()) return false;
            filters[action - 32] = null;
        } else if ((action == 40 || action == 41) && hasModule(2)) {
            reserveSteps[action - 40] = (reserveSteps[action - 40] + 1) % 4;
        } else return false;
        changed();
        return true;
    }

    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return (formed && isExterior(pos, facing) && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                || super.hasCapability(capability, facing);
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (isExterior(pos, facing) && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return (T) fluidAccess(facing);
        return super.getCapability(capability, facing);
    }

    @Nullable IFluidHandler fluidAccess(@Nullable EnumFacing side) {
        if (!formed || isInvalid()) return null;
        return side == null ? unsidedPort : sideHandlers[side.getIndex()];
    }

    boolean usesTowerModel() { return formed && !legacyShape; }

    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 1, getUpdateTag()); }
    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager, SPacketUpdateTileEntity packet) {
        boolean previousTower = usesTowerModel();
        BlockPos previousCenter = center;
        readFromNBT(packet.getNbtCompound());
        BlockPos redraw = center == null ? previousCenter : center;
        if (previousTower != usesTowerModel() && redraw != null && world != null)
            world.markBlockRangeForRenderUpdate(redraw.add(-1, -3, -1), redraw.add(1, 4, 1));
    }
    @Override public void handleUpdateTag(NBTTagCompound tag) { readFromNBT(tag); }

    public AxisAlignedBB renderBounds() {
        if (center == null || height < MIN_HEIGHT) return new AxisAlignedBB(pos);
        return new AxisAlignedBB(center.getX() - 1.25, baseY, center.getZ() - 1.25,
                center.getX() + 2.25, baseY + height, center.getZ() + 2.25);
    }

    @SideOnly(Side.CLIENT)
    @Override public AxisAlignedBB getRenderBoundingBox() { return renderBounds(); }

    private static final class Structure {
        final BlockPos center; final int baseY, height;
        Structure(BlockPos center, int baseY, int height) { this.center = center; this.baseY = baseY; this.height = height; }
    }
}
