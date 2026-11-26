package br.cropmanager;

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
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

/** Master TileEntity for the EU-free 3x5x3 single-fluid Industrial Fluid Reservoir. */
public final class IndustrialTankTile extends FarmTile implements ITickable, StorageMultiblock {
    public static final int FIXED_HEIGHT = 5;
    public static final int TRANSFER_INTERVAL = 20;
    public static final int MAX_CAPACITY_UPGRADES = 8;
    static final int CLOSED = 0;
    static final int INPUT = 1;
    static final int OUTPUT = 2;
    static final int CONTAINER_IN_SLOT = 0;
    static final int CONTAINER_OUT_SLOT = 1;
    static final int CAPACITY_SLOT = 2;
    static final int INVENTORY_SLOTS = 3;

    @Nullable
    private FluidStack filter;
    // EnumFacing indices: 0=DOWN, 1=UP, 2=NORTH(back), 3=SOUTH(front), 4=WEST(left), 5=EAST(right)
    private final int[] portModes = {OUTPUT, INPUT, CLOSED, CLOSED, CLOSED, CLOSED};
    private int redstoneMode;
    public boolean autoOutput = true;
    private int previousSignal = -1;
    private final IFluidHandler[] sideHandlers = new IFluidHandler[6];
    private final IFluidHandler unsidedPort = port(null);

    public int baseY;
    public BlockPos center;
    public boolean formed;

    public final FluidTank tank = new LockedTank();
    public final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return slot == CAPACITY_SLOT ? MAX_CAPACITY_UPGRADES : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (slot == CONTAINER_IN_SLOT) {
                return FluidUtil.getFluidHandler(stack) != null || FluidUtil.getFluidContained(stack) != null;
            }
            if (slot == CAPACITY_SLOT) {
                return stack.getItem() == CropManager.RESERVOIR_CAPACITY;
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            inventoryChanged(slot);
        }
    };

    public IndustrialTankTile() {
        for (EnumFacing face : EnumFacing.values()) {
            sideHandlers[face.getIndex()] = port(face);
        }
    }

    void inventoryChanged(int slot) {
        if (slot == CAPACITY_SLOT) refreshCapacity();
        changed();
    }

    public int upgradeCount() {
        ItemStack stack = inventory.getStackInSlot(CAPACITY_SLOT);
        return stack.getItem() == CropManager.RESERVOIR_CAPACITY
                ? Math.min(MAX_CAPACITY_UPGRADES, Math.max(0, stack.getCount()))
                : 0;
    }

    public int computedCapacity() {
        return ManagerConfig.industrialTankBaseCapacity
                + upgradeCount() * ManagerConfig.industrialTankCapacityPerUpgrade;
    }

    private void refreshCapacity() {
        // Never discard excess fluid when capacity upgrades are removed.
        tank.setCapacity(Math.max(1000, computedCapacity()));
    }

    @Nullable
    public FluidStack filterFluid() {
        return filter;
    }

    public int portMode(int index) {
        return portModes[index];
    }

    public int redstoneMode() {
        return redstoneMode;
    }

    private boolean acceptsFluid(@Nullable FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) return false;
        if (filter != null) return filter.isFluidEqual(fluid);
        FluidStack current = tank.getFluid();
        return current == null || current.isFluidEqual(fluid);
    }

    private final class LockedTank extends FluidTank {
        LockedTank() {
            super(ManagerConfig.industrialTankBaseCapacity);
        }

        @Override
        public boolean canFillFluidType(@Nullable FluidStack fluid) {
            return acceptsFluid(fluid);
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || getFluidAmount() >= getCapacity() || !canFillFluidType(resource)) return 0;
            return super.fill(resource, doFill);
        }

        @Override
        protected void onContentsChanged() {
            changed();
        }
    }

    private IFluidHandler port(@Nullable final EnumFacing face) {
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                if (!formed || isInvalid()) return new IFluidTankProperties[0];
                final boolean input = allows(face, INPUT);
                final boolean output = allows(face, OUTPUT);
                if (!input && !output) return new IFluidTankProperties[0];
                return new IFluidTankProperties[] {
                    new IFluidTankProperties() {
                        @Override
                        public FluidStack getContents() {
                            return tank.getFluid() == null ? null : tank.getFluid().copy();
                        }

                        @Override
                        public int getCapacity() {
                            return tank.getCapacity();
                        }

                        @Override
                        public boolean canFill() {
                            return input && tank.getFluidAmount() < tank.getCapacity();
                        }

                        @Override
                        public boolean canDrain() {
                            return output && redstoneAllows() && tank.getFluidAmount() > 0;
                        }

                        @Override
                        public boolean canFillFluidType(@Nullable FluidStack fluid) {
                            return canFill() && acceptsFluid(fluid);
                        }

                        @Override
                        public boolean canDrainFluidType(@Nullable FluidStack fluid) {
                            return canDrain() && fluid != null && fluid.isFluidEqual(tank.getFluid());
                        }
                    }
                };
            }

            @Override
            public int fill(FluidStack resource, boolean execute) {
                if (!formed || isInvalid() || resource == null || resource.amount <= 0) return 0;
                return allows(face, INPUT) ? tank.fill(resource, execute) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, boolean execute) {
                if (!formed || isInvalid() || resource == null || resource.amount <= 0) return null;
                if (!allows(face, OUTPUT) || !redstoneAllows()) return null;
                FluidStack current = tank.getFluid();
                return current != null && resource.isFluidEqual(current)
                        ? tank.drain(resource.amount, execute)
                        : null;
            }

            @Override
            public FluidStack drain(int amount, boolean execute) {
                if (!formed || isInvalid() || amount <= 0) return null;
                return allows(face, OUTPUT) && redstoneAllows() ? tank.drain(amount, execute) : null;
            }
        };
    }

    private EnumFacing relativeFace(EnumFacing face) {
        if (face.getAxis().isVertical() || world == null) return face;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.FLUID_TANK) return face;
        EnumFacing front = state.getValue(IndustrialTankBlock.FACING);
        if (face == front) return EnumFacing.SOUTH;
        if (face == front.getOpposite()) return EnumFacing.NORTH;
        return face == front.rotateYCCW() ? EnumFacing.EAST : EnumFacing.WEST;
    }

    private boolean allows(@Nullable EnumFacing side, int mode) {
        if (side == null) {
            for (int portMode : portModes) {
                if (portMode == mode) return true;
            }
            return false;
        }
        return portModes[relativeFace(side).getIndex()] == mode;
    }

    public boolean redstoneAllows() {
        if (redstoneMode == 0) return true;
        if (world == null || center == null) return redstoneMode == 2;
        for (BlockPos shell : shellPositions(center, baseY)) {
            if (world.isBlockLoaded(shell, false) && world.isBlockPowered(shell)) {
                return redstoneMode == 1;
            }
        }
        return redstoneMode == 2;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        if (world.getTotalWorldTime() % TRANSFER_INTERVAL != 0) return;
        if (!formed) {
            tryForm();
            return;
        }
        StorageMultiblockSupport.Validation validation = validateCurrent();
        if (validation == StorageMultiblockSupport.Validation.INVALID) {
            setFormed(false);
            return;
        }
        if (validation == StorageMultiblockSupport.Validation.DEFER) return;
        processContainers();
        autoEjectFluids();
    }

    public void tryForm() {
        if (world == null || world.isRemote || isInvalid()) return;
        if (formed) {
            StorageMultiblockSupport.Validation current = validateCurrent();
            if (current == StorageMultiblockSupport.Validation.VALID || current == StorageMultiblockSupport.Validation.DEFER) return;
            setFormed(false);
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.FLUID_TANK) return;
        EnumFacing facing = state.getValue(IndustrialTankBlock.FACING);
        BlockPos proposedCenter = pos.offset(facing.getOpposite());
        BlockPos foundCenter = null;
        int foundBaseY = 0;
        boolean deferred = false;
        for (int relativeY = 1; relativeY < FIXED_HEIGHT - 1; relativeY++) {
            int candidateBase = pos.getY() - relativeY;
            StorageMultiblockSupport.Validation result = validateStructure(proposedCenter, candidateBase, facing);
            if (result == StorageMultiblockSupport.Validation.DEFER) {
                deferred = true;
            } else if (result == StorageMultiblockSupport.Validation.VALID) {
                if (foundCenter != null) return;
                foundCenter = proposedCenter;
                foundBaseY = candidateBase;
            }
        }
        if (foundCenter != null) {
            form(foundCenter, foundBaseY);
        } else if (!deferred) {
            setFormed(false);
        }
    }

    private StorageMultiblockSupport.Validation validateCurrent() {
        if (center == null) return StorageMultiblockSupport.Validation.INVALID;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.FLUID_TANK) return StorageMultiblockSupport.Validation.INVALID;
        return validateStructure(center, baseY, state.getValue(IndustrialTankBlock.FACING));
    }

    private StorageMultiblockSupport.Validation validateStructure(BlockPos centerPos, int floorY, EnumFacing facing) {
        return StorageMultiblockSupport.validateStructure(
                world, pos, centerPos, floorY, FIXED_HEIGHT, facing,
                CropManager.FLUID_TANK, CropManager.FLUID_TANK_CASING);
    }

    private void form(BlockPos structureCenter, int structureBaseY) {
        center = structureCenter;
        baseY = structureBaseY;
        refreshCapacity();
        setFormed(true);
        linkCasings(pos);
        updateShellComparators();
    }

    private void linkCasings(@Nullable BlockPos masterPos) {
        StorageMultiblockSupport.linkStorageCasings(world, pos, center, baseY, FIXED_HEIGHT, masterPos);
    }

    private void setFormed(boolean value) {
        if (formed == value) return;
        formed = value;
        if (!value) linkCasings(null);
        changed();
        if (world != null && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    void unformOnBreak() {
        if (!formed) return;
        formed = false;
        linkCasings(null);
        previousSignal = 0;
        updateShellComparators();
    }

    void processContainers() {
        if (!inventory.getStackInSlot(CONTAINER_OUT_SLOT).isEmpty()) return;
        ItemStack source = inventory.getStackInSlot(CONTAINER_IN_SLOT);
        if (source.isEmpty()) return;
        FluidActionResult result = FluidUtil.getFluidContained(source) == null
                ? FluidUtil.tryFillContainer(source.copy(), tank, Integer.MAX_VALUE, null, true)
                : FluidUtil.tryEmptyContainer(source.copy(), tank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            inventory.setStackInSlot(CONTAINER_IN_SLOT, ItemStack.EMPTY);
            inventory.setStackInSlot(CONTAINER_OUT_SLOT, result.getResult());
        }
    }

    void autoEjectFluids() {
        if (!autoOutput || !redstoneAllows() || tank.getFluidAmount() <= 0 || center == null) return;
        int remainingBudget = ManagerConfig.industrialTankAutoEjectRate;
        if (remainingBudget <= 0) return;
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos shell : shellPositions(center, baseY)) {
            for (EnumFacing face : EnumFacing.values()) {
                if (!isExterior(shell, face) || !allows(face, OUTPUT)) continue;
                BlockPos targetPos = shell.offset(face);
                if (!world.isBlockLoaded(targetPos, false) || !visited.add(targetPos)) continue;
                if (targetPos.equals(pos) || containsCasing(targetPos)) continue;
                TileEntity target = world.getTileEntity(targetPos);
                if (target == null || target.isInvalid()) continue;
                IFluidHandler handler = target.getCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
                if (handler != null) {
                    remainingBudget -= transferToHandler(handler, remainingBudget);
                }
                if (remainingBudget <= 0 || tank.getFluidAmount() <= 0) return;
            }
        }
    }

    int transferToHandler(IFluidHandler destination, int maxAmount) {
        FluidStack current = tank.getFluid();
        if (current == null || maxAmount <= 0) return 0;
        int proposed = Math.min(maxAmount, tank.getFluidAmount());
        if (proposed <= 0) return 0;
        int accepted = Math.min(proposed, destination.fill(new FluidStack(current, proposed), false));
        if (accepted <= 0) return 0;
        FluidStack drained = tank.drain(accepted, true);
        if (drained == null || drained.amount <= 0) return 0;
        int inserted = Math.max(0, Math.min(drained.amount, destination.fill(drained.copy(), true)));
        if (inserted < drained.amount) {
            tank.setFluid(new FluidStack(drained, tank.getFluidAmount() + drained.amount - inserted));
            changed();
        }
        if (inserted > 0) markDirty();
        return inserted;
    }

    List<BlockPos> shellPositions(BlockPos structureCenter, int floorY) {
        return StorageMultiblockSupport.shellPositions(structureCenter, floorY, FIXED_HEIGHT);
    }

    private void changed() {
        markDirty();
        if (world != null && !world.isRemote && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            int signal = comparatorSignal();
            if (signal != previousSignal) {
                previousSignal = signal;
                if (state.getBlock() == CropManager.FLUID_TANK) {
                    world.updateComparatorOutputLevel(pos, state.getBlock());
                }
                updateShellComparators();
            }
        }
    }

    private void updateShellComparators() {
        if (world != null && !world.isRemote) {
            StorageMultiblockSupport.updateShellComparators(world, pos, center, baseY, FIXED_HEIGHT, CropManager.FLUID_TANK_CASING);
        }
    }

    @Override public boolean usable(EntityPlayer player) {
        return super.usable(player) || (formed && StorageMultiblockSupport.canUseFromShell(
                this, player, center, baseY, FIXED_HEIGHT, CropManager.FLUID_TANK_CASING));
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public int comparatorSignal() {
        return formed ? StorageMultiblockSupport.comparatorSignal(tank.getFluidAmount(), tank.getCapacity()) : 0;
    }

    @Override
    public boolean openFromCasing(EntityPlayer player) {
        tryForm();
        if (!formed) return false;
        player.openGui(CropManager.instance, 16, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void casingBroken(BlockPos casingPos) {
        if (formed && containsCasing(casingPos)) setFormed(false);
    }

    @Override
    public boolean containsCasing(BlockPos casingPos) {
        return StorageMultiblockSupport.containsCasing(pos, center, baseY, FIXED_HEIGHT, casingPos);
    }

    @Override
    public boolean isExterior(BlockPos at, @Nullable EnumFacing side) {
        return StorageMultiblockSupport.isExterior(center, baseY, FIXED_HEIGHT, at, side);
    }

    @Override
    public boolean hasPortCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return formed && !isInvalid() && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getPortCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (!formed || isInvalid() || capability != CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return null;
        return (T) (side == null ? unsidedPort : sideHandlers[side.getIndex()]);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return (formed && isExterior(pos, facing) && hasPortCapability(capability, facing))
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (formed && isExterior(pos, facing) && hasPortCapability(capability, facing)) {
            return getPortCapability(capability, facing);
        }
        return super.getCapability(capability, facing);
    }

    public NBTTagCompound writeItemData() {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.removeTag("x");
        tag.removeTag("y");
        tag.removeTag("z");
        tag.setBoolean("TankFormed", false);
        tag.removeTag("StructureCenter");
        tag.removeTag("StructureBaseY");
        return tag;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
        tag.setTag("Inventory", inventory.serializeNBT());
        writeSettings(tag);
        tag.setInteger("StructureBaseY", baseY);
        tag.setBoolean("TankFormed", formed);
        if (center != null) tag.setLong("StructureCenter", center.toLong());
        tag.setBoolean("AutoOutput", autoOutput);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readFixedInventory(inventory, tag.getCompoundTag("Inventory"), INVENTORY_SLOTS);
        readSettings(tag);
        StorageMultiblockSupport.readFluidTank(tank, tag.getCompoundTag("Tank"));
        baseY = tag.getInteger("StructureBaseY");
        center = tag.hasKey("StructureCenter", 4) ? BlockPos.fromLong(tag.getLong("StructureCenter")) : null;
        autoOutput = !tag.hasKey("AutoOutput") || tag.getBoolean("AutoOutput");
        boolean worldSave = tag.hasKey("x") && tag.hasKey("y") && tag.hasKey("z");
        formed = worldSave && tag.getBoolean("TankFormed") && center != null;
        refreshCapacity();
    }

    @Override
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        FluidStack fluid = tank.getFluid();
        tag.setInteger("FluidAmount", tank.getFluidAmount());
        tag.setInteger("TankCapacity", tank.getCapacity());
        tag.setString("FluidName", fluid == null ? "" : fluid.getFluid().getName());
        tag.setBoolean("Formed", formed);
        tag.setBoolean("AutoOutput", autoOutput);
        tag.setInteger("Signal", comparatorSignal());
        writeSettings(tag);
        return tag;
    }

    @Override
    public void acceptGuiState(NBTTagCompound tag) {
        tank.setCapacity(Math.max(1000, tag.getInteger("TankCapacity")));
        readSettings(tag);
        String fluidName = tag.getString("FluidName");
        int amount = tag.getInteger("FluidAmount");
        Fluid fluid = fluidName.isEmpty() ? null : FluidRegistry.getFluid(fluidName);
        tank.setFluid(fluid == null || amount <= 0 ? null : new FluidStack(fluid, amount));
        formed = tag.getBoolean("Formed");
        autoOutput = tag.getBoolean("AutoOutput");
    }

    private void writeSettings(NBTTagCompound tag) {
        tag.setIntArray("TankPorts", portModes);
        tag.setInteger("TankRedstone", redstoneMode);
        if (filter != null) tag.setTag("Filter", filter.writeToNBT(new NBTTagCompound()));
    }

    private void readSettings(NBTTagCompound tag) {
        int[] saved = tag.getIntArray("TankPorts");
        if (saved.length == portModes.length) {
            for (int i = 0; i < portModes.length; i++) {
                portModes[i] = Math.max(CLOSED, Math.min(OUTPUT, saved[i]));
            }
        }
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("TankRedstone")));
        filter = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("Filter"));
    }

    private static void readFixedInventory(ItemStackHandler target, NBTTagCompound saved, int slots) {
        NBTTagCompound copy = saved.copy();
        copy.setInteger("Size", slots);
        target.deserializeNBT(copy);
    }

    @Override
    public boolean control(int action, EntityPlayer player) {
        if (!formed || !usable(player)) return false;
        if (action == 0) {
            autoOutput = !autoOutput;
        } else if (action == 1) {
            redstoneMode = (redstoneMode + 1) % 3;
        } else if (action >= 10 && action < 16) {
            int i = action - 10;
            portModes[i] = (portModes[i] + 1) % 3;
        } else if (action == 30) {
            FluidStack sample = FluidUtil.getFluidContained(player.inventory.getItemStack());
            if (sample == null || (tank.getFluid() != null && !sample.isFluidEqual(tank.getFluid()))) return false;
            filter = new FluidStack(sample, 1);
        } else if (action == 31) {
            if (!player.inventory.getItemStack().isEmpty()) return false;
            filter = null;
        } else {
            return false;
        }
        changed();
        return true;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
        boolean previousFormed = formed;
        BlockPos previousCenter = center;
        readFromNBT(packet.getNbtCompound());
        if (previousFormed != formed && world != null) {
            BlockPos redraw = center != null ? center : previousCenter;
            if (redraw != null) {
                world.markBlockRangeForRenderUpdate(
                        redraw.add(-1, 0, -1), redraw.add(1, FIXED_HEIGHT - 1, 1));
            } else {
                world.markBlockRangeForRenderUpdate(pos.add(-1, -3, -1), pos.add(1, 3, 1));
            }
        }
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos,
            IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    public AxisAlignedBB renderBounds() {
        if (center == null) return new AxisAlignedBB(pos);
        return new AxisAlignedBB(center.getX() - 1.25, baseY, center.getZ() - 1.25,
                center.getX() + 2.25, baseY + FIXED_HEIGHT, center.getZ() + 2.25);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return renderBounds();
    }
}
