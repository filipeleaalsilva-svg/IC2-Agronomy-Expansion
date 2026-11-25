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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/** Master TileEntity for the EU-free 3x5x3 single-item bulk Agricultural Grain Silo. */
public final class AgriculturalSiloTile extends FarmTile implements ITickable, StorageMultiblock {
    public static final int FIXED_HEIGHT = 5;
    public static final int TRANSFER_INTERVAL = 20;
    public static final int MAX_CAPACITY_UPGRADES = 8;
    static final int CLOSED = 0;
    static final int INPUT = 1;
    static final int OUTPUT = 2;

    private ItemStack storedTemplate = ItemStack.EMPTY;
    private int storedCount;
    private boolean locked;
    // EnumFacing indices: 0=DOWN, 1=UP, 2=NORTH(back), 3=SOUTH(front), 4=WEST(left), 5=EAST(right)
    private final int[] portModes = {OUTPUT, INPUT, CLOSED, CLOSED, CLOSED, CLOSED};
    private int redstoneMode;
    public boolean autoOutput = true;
    private int previousSignal = -1;
    private final IItemHandler[] sideHandlers = new IItemHandler[6];
    private final IItemHandler unsidedPort = port(null);

    public int baseY;
    public BlockPos center;
    public boolean formed;

    // Kept separate from external IItemHandler automation so pipes cannot extract upgrades.
    public final ItemStackHandler upgrades = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_CAPACITY_UPGRADES;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == CropManager.RESERVOIR_CAPACITY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };

    public AgriculturalSiloTile() {
        for (EnumFacing face : EnumFacing.values()) {
            sideHandlers[face.getIndex()] = port(face);
        }
    }

    public int upgradeCount() {
        ItemStack stack = upgrades.getStackInSlot(0);
        return stack.getItem() == CropManager.RESERVOIR_CAPACITY
                ? Math.min(MAX_CAPACITY_UPGRADES, Math.max(0, stack.getCount()))
                : 0;
    }

    public int getCapacity() {
        return ManagerConfig.siloBaseCapacity + upgradeCount() * ManagerConfig.siloCapacityPerUpgrade;
    }

    public ItemStack getStoredTemplate() {
        return storedTemplate;
    }

    public int getStoredCount() {
        return storedCount;
    }

    public boolean isLocked() {
        return locked;
    }

    public int portMode(int index) {
        return portModes[index];
    }

    public int redstoneMode() {
        return redstoneMode;
    }

    public boolean acceptsItem(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() <= 1) return false;
        return storedTemplate.isEmpty() || ItemHandlerHelper.canItemStacksStack(storedTemplate, stack);
    }

    public ItemStack insertBulk(ItemStack stack, boolean simulate) {
        if (!acceptsItem(stack)) return stack;
        int space = Math.max(0, getCapacity() - storedCount);
        int toInsert = Math.min(space, stack.getCount());
        if (toInsert <= 0) return stack;
        if (!simulate) {
            if (storedTemplate.isEmpty()) {
                storedTemplate = ItemHandlerHelper.copyStackWithSize(stack, 1);
            }
            storedCount += toInsert;
            changed();
        }
        return toInsert == stack.getCount()
                ? ItemStack.EMPTY
                : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - toInsert);
    }

    public ItemStack extractBulk(int amount, boolean simulate) {
        if (amount <= 0 || storedCount <= 0 || storedTemplate.isEmpty()) return ItemStack.EMPTY;
        int maxOut = Math.min(amount, Math.min(storedTemplate.getMaxStackSize(), storedCount));
        ItemStack out = ItemHandlerHelper.copyStackWithSize(storedTemplate, maxOut);
        if (!simulate) {
            storedCount -= maxOut;
            if (storedCount <= 0) {
                storedCount = 0;
                if (!locked) storedTemplate = ItemStack.EMPTY;
            }
            changed();
        }
        return out;
    }

    private IItemHandler port(@Nullable final EnumFacing face) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return formed && !isInvalid() ? 1 : 0;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (!formed || isInvalid() || slot != 0 || storedCount <= 0 || storedTemplate.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                return ItemHandlerHelper.copyStackWithSize(
                        storedTemplate, Math.min(storedCount, storedTemplate.getMaxStackSize()));
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!formed || isInvalid() || slot != 0 || !allows(face, INPUT)) return stack;
                return insertBulk(stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!formed || isInvalid() || slot != 0 || !allows(face, OUTPUT) || !redstoneAllows()) {
                    return ItemStack.EMPTY;
                }
                return extractBulk(amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? getCapacity() : 0;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return formed && !isInvalid() && slot == 0 && allows(face, INPUT) && acceptsItem(stack);
            }
        };
    }

    private EnumFacing relativeFace(EnumFacing face) {
        if (face.getAxis().isVertical() || world == null) return face;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.AGRICULTURAL_SILO) return face;
        EnumFacing front = state.getValue(AgriculturalSiloBlock.FACING);
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
        autoEjectItems();
    }

    public void tryForm() {
        if (world == null || world.isRemote || isInvalid()) return;
        if (formed) {
            StorageMultiblockSupport.Validation current = validateCurrent();
            if (current == StorageMultiblockSupport.Validation.VALID || current == StorageMultiblockSupport.Validation.DEFER) return;
            setFormed(false);
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != CropManager.AGRICULTURAL_SILO) return;
        EnumFacing facing = state.getValue(AgriculturalSiloBlock.FACING);
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
        if (state.getBlock() != CropManager.AGRICULTURAL_SILO) return StorageMultiblockSupport.Validation.INVALID;
        return validateStructure(center, baseY, state.getValue(AgriculturalSiloBlock.FACING));
    }

    private StorageMultiblockSupport.Validation validateStructure(BlockPos centerPos, int floorY, EnumFacing facing) {
        return StorageMultiblockSupport.validateStructure(
                world, pos, centerPos, floorY, FIXED_HEIGHT, facing,
                CropManager.AGRICULTURAL_SILO, CropManager.SILO_CASING);
    }

    private void form(BlockPos structureCenter, int structureBaseY) {
        center = structureCenter;
        baseY = structureBaseY;
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

    void autoEjectItems() {
        if (!autoOutput || !redstoneAllows() || storedCount <= 0 || storedTemplate.isEmpty() || center == null) return;
        int remainingBudget = ManagerConfig.siloAutoEjectRate;
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
                IItemHandler dest = target.getCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
                if (dest != null) {
                    while (remainingBudget > 0 && storedCount > 0 && !storedTemplate.isEmpty()) {
                        int step = Math.min(remainingBudget,
                                Math.min(storedTemplate.getMaxStackSize(), storedCount));
                        ItemStack batch = ItemHandlerHelper.copyStackWithSize(storedTemplate, step);
                        ItemStack remainder = ItemHandlerHelper.insertItemStacked(dest, batch, false);
                        int inserted = step - remainder.getCount();
                        if (inserted > 0) {
                            extractBulk(inserted, false);
                            remainingBudget -= inserted;
                        }
                        if (inserted < step) break;
                    }
                }
                if (remainingBudget <= 0 || storedCount <= 0) return;
            }
        }
    }

    List<BlockPos> shellPositions(BlockPos structureCenter, int floorY) {
        return StorageMultiblockSupport.shellPositions(structureCenter, floorY, FIXED_HEIGHT);
    }

    void changed() {
        markDirty();
        if (world != null && !world.isRemote && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            int signal = comparatorSignal();
            if (signal != previousSignal) {
                previousSignal = signal;
                if (state.getBlock() == CropManager.AGRICULTURAL_SILO) {
                    world.updateComparatorOutputLevel(pos, state.getBlock());
                }
                updateShellComparators();
            }
        }
    }

    private void updateShellComparators() {
        if (world != null && !world.isRemote) {
            StorageMultiblockSupport.updateShellComparators(world, pos, center, baseY, FIXED_HEIGHT, CropManager.SILO_CASING);
        }
    }

    @Override public boolean usable(EntityPlayer player) {
        return super.usable(player) || (formed && StorageMultiblockSupport.canUseFromShell(
                this, player, center, baseY, FIXED_HEIGHT, CropManager.SILO_CASING));
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public int comparatorSignal() {
        return formed ? StorageMultiblockSupport.comparatorSignal(storedCount, getCapacity()) : 0;
    }

    @Override
    public boolean openFromCasing(EntityPlayer player) {
        tryForm();
        if (!formed) return false;
        player.openGui(CropManager.instance, 17, world, pos.getX(), pos.getY(), pos.getZ());
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
        return formed && !isInvalid() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getPortCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (!formed || isInvalid() || capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return null;
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
        tag.setBoolean("SiloFormed", false);
        tag.removeTag("StructureCenter");
        tag.removeTag("StructureBaseY");
        return tag;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (!storedTemplate.isEmpty()) {
            tag.setTag("StoredItem", storedTemplate.writeToNBT(new NBTTagCompound()));
        }
        tag.setInteger("StoredCount", storedCount);
        tag.setBoolean("Locked", locked);
        tag.setTag("Upgrades", upgrades.serializeNBT());
        writeSettings(tag);
        tag.setInteger("StructureBaseY", baseY);
        tag.setBoolean("SiloFormed", formed);
        if (center != null) tag.setLong("StructureCenter", center.toLong());
        tag.setBoolean("AutoOutput", autoOutput);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readFixedInventory(upgrades, tag.getCompoundTag("Upgrades"), 1);
        readSettings(tag);
        storedTemplate = tag.hasKey("StoredItem", 10)
                ? new ItemStack(tag.getCompoundTag("StoredItem"))
                : ItemStack.EMPTY;
        if (!storedTemplate.isEmpty()) {
            if (storedTemplate.getMaxStackSize() <= 1) {
                storedTemplate = ItemStack.EMPTY;
            } else {
                storedTemplate.setCount(1);
            }
        }
        storedCount = storedTemplate.isEmpty() ? 0 : Math.max(0, tag.getInteger("StoredCount"));
        locked = !storedTemplate.isEmpty() && tag.getBoolean("Locked");
        if (storedCount <= 0 && !locked) {
            storedTemplate = ItemStack.EMPTY;
        }
        baseY = tag.getInteger("StructureBaseY");
        center = tag.hasKey("StructureCenter", 4) ? BlockPos.fromLong(tag.getLong("StructureCenter")) : null;
        autoOutput = !tag.hasKey("AutoOutput") || tag.getBoolean("AutoOutput");
        boolean worldSave = tag.hasKey("x") && tag.hasKey("y") && tag.hasKey("z");
        formed = worldSave && tag.getBoolean("SiloFormed") && center != null;
    }

    @Override
    public NBTTagCompound guiState() {
        NBTTagCompound tag = new NBTTagCompound();
        if (!storedTemplate.isEmpty()) {
            tag.setTag("StoredItem", storedTemplate.writeToNBT(new NBTTagCompound()));
        }
        tag.setInteger("StoredCount", storedCount);
        tag.setInteger("Capacity", getCapacity());
        tag.setBoolean("Locked", locked);
        tag.setBoolean("Formed", formed);
        tag.setBoolean("AutoOutput", autoOutput);
        writeSettings(tag);
        return tag;
    }

    @Override
    public void acceptGuiState(NBTTagCompound tag) {
        readSettings(tag);
        storedTemplate = tag.hasKey("StoredItem", 10)
                ? new ItemStack(tag.getCompoundTag("StoredItem"))
                : ItemStack.EMPTY;
        if (!storedTemplate.isEmpty()) storedTemplate.setCount(1);
        storedCount = storedTemplate.isEmpty() ? 0 : Math.max(0, tag.getInteger("StoredCount"));
        locked = !storedTemplate.isEmpty() && tag.getBoolean("Locked");
        formed = tag.getBoolean("Formed");
        autoOutput = tag.getBoolean("AutoOutput");
    }

    private void writeSettings(NBTTagCompound tag) {
        tag.setIntArray("SiloPorts", portModes);
        tag.setInteger("SiloRedstone", redstoneMode);
    }

    private void readSettings(NBTTagCompound tag) {
        int[] saved = tag.getIntArray("SiloPorts");
        if (saved.length == portModes.length) {
            for (int i = 0; i < portModes.length; i++) {
                portModes[i] = Math.max(CLOSED, Math.min(OUTPUT, saved[i]));
            }
        }
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("SiloRedstone")));
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
        } else if (action == 2) {
            ItemStack cursor = player.inventory.getItemStack();
            if (!cursor.isEmpty() && cursor.getMaxStackSize() > 1 && storedCount == 0) {
                storedTemplate = ItemHandlerHelper.copyStackWithSize(cursor, 1);
                locked = true;
            } else if (!storedTemplate.isEmpty()) {
                locked = !locked;
                if (!locked && storedCount == 0) {
                    storedTemplate = ItemStack.EMPTY;
                }
            } else {
                return false;
            }
        } else if (action == 3) {
            if (!player.inventory.getItemStack().isEmpty()) return false;
            locked = false;
            if (storedCount == 0) storedTemplate = ItemStack.EMPTY;
        } else if (action >= 10 && action < 16) {
            int i = action - 10;
            portModes[i] = (portModes[i] + 1) % 3;
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
                center.getX() + 2.25, baseY + FIXED_HEIGHT + 0.25, center.getZ() + 2.25);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return renderBounds();
    }
}
