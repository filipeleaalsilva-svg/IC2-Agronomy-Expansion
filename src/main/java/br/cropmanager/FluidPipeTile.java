package br.cropmanager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Shared TileEntity for Fluid Pipe, Redstone Fluid Restrictor, and 6-way Fluid Router. */
public final class FluidPipeTile extends TileEntity implements ITickable {
    private static final int MAX_NETWORK_NODES = 256;
    private static final ThreadLocal<Boolean> TRANSFERRING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final EnumFacing[][] SINGLE_DIR = new EnumFacing[EnumFacing.VALUES.length][1];

    static {
        for (EnumFacing face : EnumFacing.VALUES) {
            SINGLE_DIR[face.getIndex()][0] = face;
        }
    }

    public FluidPipeBlock.Kind kind = FluidPipeBlock.Kind.PIPE;
    public boolean powered;
    public EnumFacing outputSide = EnumFacing.NORTH;
    public int cooldown;
    private int rrCursor;

    private final IFluidHandler[] sidedHandlers = new IFluidHandler[6];
    private final IFluidHandler unsidedHandler = createHandler(null);

    public FluidPipeTile() {
        this(FluidPipeBlock.Kind.PIPE);
    }

    public FluidPipeTile(FluidPipeBlock.Kind kind) {
        this.kind = kind != null ? kind : FluidPipeBlock.Kind.PIPE;
        for (EnumFacing side : EnumFacing.VALUES) {
            sidedHandlers[side.getIndex()] = createHandler(side);
        }
    }

    public boolean isOpen() {
        if (kind != FluidPipeBlock.Kind.RESTRICTOR) return true;
        if (world != null && pos != null && world.isBlockLoaded(pos, false)) {
            powered = world.isBlockPowered(pos);
        }
        return powered;
    }

    public EnumFacing routerOutput() {
        if (kind == FluidPipeBlock.Kind.ROUTER && world != null && pos != null && world.isBlockLoaded(pos, false)) {
            IBlockState state = world.getBlockState(pos);
            if (state != null && state.getProperties().containsKey(FluidPipeBlock.FACING)) {
                outputSide = state.getValue(FluidPipeBlock.FACING);
            }
        }
        return outputSide != null ? outputSide : EnumFacing.NORTH;
    }

    public boolean allowsInputFrom(@Nullable EnumFacing side) {
        if (!isOpen()) return false;
        if (kind == FluidPipeBlock.Kind.ROUTER && side != null && side == routerOutput()) return false;
        return true;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || pos == null || !world.isBlockLoaded(pos, false)) return;
        if (kind == FluidPipeBlock.Kind.RESTRICTOR) {
            boolean nowPowered = world.isBlockPowered(pos);
            if (powered != nowPowered) {
                powered = nowPowered;
                markDirty();
            }
            IBlockState state = world.getBlockState(pos);
            if (state != null && state.getProperties().containsKey(FluidPipeBlock.POWERED)
                    && state.getValue(FluidPipeBlock.POWERED) != nowPowered) {
                world.setBlockState(pos, state.withProperty(FluidPipeBlock.POWERED, nowPowered), 3);
            }
            if (!nowPowered) return;
        }

        int interval = Math.max(1, ManagerConfig.fluidPipeIntervalTicks);
        if (++cooldown < interval) return;
        cooldown = 0;

        int maxMb = Math.max(1, ManagerConfig.fluidPipeTransferMb);
        for (EnumFacing side : EnumFacing.VALUES) {
            if (!allowsInputFrom(side)) continue;
            BlockPos adjPos = pos.offset(side);
            if (adjPos.getY() < 0 || adjPos.getY() >= world.getHeight() || !world.isBlockLoaded(adjPos, false)) continue;
            TileEntity adj = world.getTileEntity(adjPos);
            if (adj == null || adj.isInvalid() || adj instanceof FluidPipeTile) continue;
            IFluidHandler src = adj.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
            if (src == null) continue;

            FluidStack available = src.drain(maxMb, false);
            if (available == null || available.amount <= 0) continue;

            int simAccepted = distributeToNetwork(adjPos, available, false, true, src);
            if (simAccepted <= 0) continue;

            FluidStack simDrained = src.drain(new FluidStack(available, simAccepted), false);
            if (simDrained == null || simDrained.amount <= 0) continue;

            int verifiedAccept = distributeToNetwork(adjPos, simDrained, false, true, src);
            if (verifiedAccept <= 0) continue;

            FluidStack drained = src.drain(new FluidStack(simDrained, verifiedAccept), true);
            if (drained != null && drained.amount > 0) {
                distributeToNetwork(adjPos, drained, true, true, src);
            }
        }
    }

    int distributeToNetwork(@Nullable BlockPos sourceBlockPos, FluidStack resource, boolean doFill,
            boolean autoPull, @Nullable IFluidHandler autoPullSource) {
        if (resource == null || resource.amount <= 0 || world == null || pos == null || !isOpen()) return 0;
        if (Boolean.TRUE.equals(TRANSFERRING.get())) return 0;

        TRANSFERRING.set(Boolean.TRUE);
        try {
            List<Endpoint> endpoints = discoverEndpoints(sourceBlockPos, autoPullSource);
            if (endpoints.isEmpty()) return 0;

            boolean srcCanFill = autoPull && autoPullSource != null && canFillFluid(autoPullSource, resource);
            int srcAmount = srcCanFill ? (fluidAmount(autoPullSource, resource) + (doFill ? resource.amount : 0)) : 0;

            int remaining = resource.amount;
            int moved = 0;
            int count = endpoints.size();
            int start = Math.floorMod(rrCursor, count);
            for (int i = 0; i < count && remaining > 0; i++) {
                Endpoint ep = endpoints.get((start + i) % count);
                int limit = remaining;
                if (srcCanFill && !ep.routed && canDrainFluid(ep.handler, resource)) {
                    int dstAmount = fluidAmount(ep.handler, resource);
                    if (srcAmount <= dstAmount + 1) continue;
                    limit = Math.min(limit, (srcAmount - dstAmount) / 2);
                    if (limit <= 0) continue;
                }
                int accepted = Math.max(0, Math.min(limit, ep.handler.fill(new FluidStack(resource, limit), doFill)));
                if (accepted > 0) {
                    moved += accepted;
                    remaining -= accepted;
                    if (srcCanFill) srcAmount -= accepted;
                    if (doFill) rrCursor = (start + i + 1) % count;
                }
            }
            if (doFill && moved < resource.amount && autoPullSource != null) {
                int leftover = resource.amount - moved;
                autoPullSource.fill(new FluidStack(resource, leftover), true);
            }
            return moved;
        } finally {
            TRANSFERRING.set(Boolean.FALSE);
        }
    }

    private List<Endpoint> discoverEndpoints(@Nullable BlockPos sourceBlockPos, @Nullable IFluidHandler excludeHandler) {
        Map<Long, Boolean> visitedPipes = new HashMap<>();
        Map<Long, Endpoint[]> foundByPos = new HashMap<>();
        List<Endpoint> found = new ArrayList<>();
        Queue<PipeStep> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
        int maxY = world.getHeight();

        boolean startRouted = (kind == FluidPipeBlock.Kind.ROUTER);
        visitedPipes.put(pos.toLong(), startRouted);
        queue.add(new PipeStep(this, startRouted));

        while (!queue.isEmpty()) {
            PipeStep step = queue.poll();
            FluidPipeTile cur = step.pipe;
            EnumFacing[] exits = cur.kind == FluidPipeBlock.Kind.ROUTER
                    ? SINGLE_DIR[cur.routerOutput().getIndex()]
                    : EnumFacing.VALUES;

            for (EnumFacing dir : exits) {
                nextPos.setPos(cur.pos).move(dir);
                if (nextPos.getY() < 0 || nextPos.getY() >= maxY || !world.isBlockLoaded(nextPos, false)) continue;
                if (sourceBlockPos != null && nextPos.equals(sourceBlockPos)) continue;
                TileEntity te = world.getTileEntity(nextPos);
                if (te == null || te.isInvalid()) continue;

                EnumFacing targetFace = dir.getOpposite();
                long key = nextPos.toLong();
                if (te instanceof FluidPipeTile) {
                    FluidPipeTile nextPipe = (FluidPipeTile) te;
                    if (!nextPipe.allowsInputFrom(targetFace)) continue;
                    boolean nextRouted = step.routed || nextPipe.kind == FluidPipeBlock.Kind.ROUTER;
                    Boolean prev = visitedPipes.get(key);
                    if (prev == null) {
                        if (visitedPipes.size() >= MAX_NETWORK_NODES) continue;
                        visitedPipes.put(key, nextRouted);
                        queue.add(new PipeStep(nextPipe, nextRouted));
                    } else if (!prev && nextRouted) {
                        visitedPipes.put(key, Boolean.TRUE);
                        queue.add(new PipeStep(nextPipe, true));
                    }
                } else {
                    IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, targetFace);
                    if (handler != null && handler != excludeHandler) {
                        Endpoint[] slots = foundByPos.get(key);
                        if (slots == null) {
                            slots = new Endpoint[EnumFacing.VALUES.length];
                            foundByPos.put(key, slots);
                        }
                        int idx = targetFace.getIndex();
                        Endpoint existing = slots[idx];
                        if (existing == null) {
                            Endpoint created = new Endpoint(handler, step.routed);
                            slots[idx] = created;
                            found.add(created);
                        } else if (!existing.routed && step.routed) {
                            int listIdx = found.indexOf(existing);
                            Endpoint updated = new Endpoint(handler, true);
                            slots[idx] = updated;
                            if (listIdx >= 0) found.set(listIdx, updated);
                        }
                    }
                }
            }
        }
        return found;
    }

    private static boolean canFillFluid(IFluidHandler handler, FluidStack sample) {
        IFluidTankProperties[] props = handler.getTankProperties();
        if (props == null) return false;
        for (IFluidTankProperties p : props) {
            if (p != null && (p.canFill() || p.canFillFluidType(sample))) return true;
        }
        return false;
    }

    private static boolean canDrainFluid(IFluidHandler handler, FluidStack sample) {
        IFluidTankProperties[] props = handler.getTankProperties();
        if (props == null) return false;
        for (IFluidTankProperties p : props) {
            if (p != null && (p.canDrain() || p.canDrainFluidType(sample))) return true;
        }
        return false;
    }

    private static int fluidAmount(IFluidHandler handler, FluidStack target) {
        IFluidTankProperties[] props = handler.getTankProperties();
        if (props == null) return 0;
        int sum = 0;
        for (IFluidTankProperties p : props) {
            if (p == null) continue;
            FluidStack c = p.getContents();
            if (c != null && c.isFluidEqual(target)) sum += Math.max(0, c.amount);
        }
        return sum;
    }

    private IFluidHandler createHandler(@Nullable final EnumFacing side) {
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                final boolean canReceive = allowsInputFrom(side);
                return new IFluidTankProperties[] {
                    new IFluidTankProperties() {
                        @Override public FluidStack getContents() { return null; }
                        @Override public int getCapacity() { return Math.max(1, ManagerConfig.fluidPipeTransferMb); }
                        @Override public boolean canFill() { return canReceive; }
                        @Override public boolean canDrain() { return false; }
                        @Override public boolean canFillFluidType(FluidStack fluid) { return canReceive && fluid != null; }
                        @Override public boolean canDrainFluidType(FluidStack fluid) { return false; }
                    }
                };
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                if (resource == null || resource.amount <= 0 || !allowsInputFrom(side)) return 0;
                BlockPos origin = (pos != null && side != null) ? pos.offset(side) : null;
                return distributeToNetwork(origin, resource, doFill, false, null);
            }

            @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
            @Override public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
        };
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(
                    facing == null ? unsidedHandler : sidedHandlers[facing.getIndex()]);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void markDirty() {
        if (world != null && !world.isRemote && pos != null && world.isBlockLoaded(pos, false)) {
            world.markChunkDirty(pos, this);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Kind", kind.ordinal());
        tag.setBoolean("Powered", powered);
        tag.setInteger("OutputSide", (outputSide != null ? outputSide : EnumFacing.NORTH).getIndex());
        tag.setInteger("Cooldown", cooldown);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        FluidPipeBlock.Kind[] kinds = FluidPipeBlock.Kind.values();
        int k = tag.getInteger("Kind");
        kind = kinds[Math.max(0, Math.min(kinds.length - 1, k))];
        powered = tag.getBoolean("Powered");
        int sideIdx = tag.hasKey("OutputSide") ? tag.getInteger("OutputSide") : EnumFacing.NORTH.getIndex();
        if (sideIdx < 0 || sideIdx > 5) sideIdx = EnumFacing.NORTH.getIndex();
        outputSide = EnumFacing.byIndex(sideIdx);
        cooldown = Math.max(0, tag.getInteger("Cooldown"));
    }

    private static final class PipeStep {
        final FluidPipeTile pipe;
        final boolean routed;
        PipeStep(FluidPipeTile pipe, boolean routed) {
            this.pipe = pipe;
            this.routed = routed;
        }
    }

    private static final class Endpoint {
        final IFluidHandler handler;
        final boolean routed;
        Endpoint(IFluidHandler handler, boolean routed) {
            this.handler = handler;
            this.routed = routed;
        }
    }
}
