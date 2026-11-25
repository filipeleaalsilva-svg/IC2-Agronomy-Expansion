package br.cropmanager;

import ic2.api.energy.tile.IHeatSource;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** Passive thermal link that forwards IC2 heat requests through adjacent pipes. */
public final class ThermalPipeTile extends TileEntity implements IHeatSource {
    private static final ThreadLocal<Set<Long>> ACTIVE = ThreadLocal.withInitial(() -> new java.util.HashSet<Long>());
    @Override public int maxrequestHeatTick(EnumFacing side) { return ManagerConfig.thermalPipeMaxHeatPerTick; }
    @Override public int requestHeat(EnumFacing side, int request) { return transfer(side, request, false); }
    @Override public int drawHeat(EnumFacing side, int request, boolean simulate) { return transfer(side, request, simulate); }
    private int transfer(EnumFacing towardConsumer, int request, boolean simulate) {
        if (world == null || request <= 0) return 0;
        Set<Long> visited = ACTIVE.get();
        boolean root = visited.isEmpty();
        if (!visited.add(pos.toLong())) return 0;
        try {
            int remaining = Math.min(request, maxrequestHeatTick(towardConsumer));
            int moved = 0;
            for (EnumFacing direction : EnumFacing.values()) {
                BlockPos neighborPos = pos.offset(direction);
                if (direction == towardConsumer || !world.isBlockLoaded(neighborPos, false)) continue;
                TileEntity neighbor = world.getTileEntity(neighborPos);
                if (!(neighbor instanceof IHeatSource)) continue;
                if (!(neighbor instanceof ThermalPipeTile) && !visited.add(neighborPos.toLong())) continue;
                IHeatSource source = (IHeatSource) neighbor;
                EnumFacing sourceSide = direction.getOpposite();
                int received = Math.max(0, Math.min(remaining, source.drawHeat(sourceSide, remaining, simulate)));
                moved += received; remaining -= received;
                if (remaining == 0) break;
            }
            return moved;
        } finally {
            if (root) visited.clear();
        }
    }
    @Override public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) { return oldState.getBlock() != newState.getBlock(); }
}
