package br.cropmanager;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidTank;

/**
 * Shared geometry, validation, casing-binding, and comparator helpers for 3xHx3 storage multiblocks.
 */
final class StorageMultiblockSupport {
    enum Validation {
        VALID,
        INVALID,
        DEFER
    }

    private StorageMultiblockSupport() { }

    static BlockPos controllerPosition(BlockPos centerPos, int y, EnumFacing facing) {
        return centerPos.offset(facing).up(y - centerPos.getY());
    }

    static Validation validateStructure(World world, BlockPos controllerPos, BlockPos centerPos,
            int floorY, int height, EnumFacing facing, Block controllerBlock, Block casingBlock) {
        if (floorY < 0 || floorY + height > world.getHeight()) return Validation.INVALID;
        if (controllerPos.getY() <= floorY || controllerPos.getY() >= floorY + height - 1
                || !controllerPosition(centerPos, controllerPos.getY(), facing).equals(controllerPos)) {
            return Validation.INVALID;
        }
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        for (int y = 0; y < height; y++) {
            int worldY = floorY + y;
            boolean horizontalCap = y == 0 || y == height - 1;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    check.setPos(centerPos.getX() + dx, worldY, centerPos.getZ() + dz);
                    if (!world.isBlockLoaded(check, false)) return Validation.DEFER;
                    boolean interior = dx == 0 && dz == 0;
                    if (interior && !horizontalCap) {
                        if (world.getBlockState(check).getBlock() != Blocks.AIR) return Validation.INVALID;
                        continue;
                    }
                    Block block = world.getBlockState(check).getBlock();
                    Block expected = check.equals(controllerPos) ? controllerBlock : casingBlock;
                    if (block != expected) return Validation.INVALID;
                }
            }
        }
        return Validation.VALID;
    }

    static List<BlockPos> shellPositions(BlockPos structureCenter, int floorY, int height) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            boolean cap = y == 0 || y == height - 1;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (cap || Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                        positions.add(new BlockPos(structureCenter.getX() + dx, floorY + y, structureCenter.getZ() + dz));
                    }
                }
            }
        }
        return positions;
    }

    static boolean containsCasing(BlockPos controllerPos, @Nullable BlockPos center,
            int baseY, int height, @Nullable BlockPos candidate) {
        if (center == null || candidate == null || candidate.equals(controllerPos)) return false;
        int dx = Math.abs(candidate.getX() - center.getX());
        int dz = Math.abs(candidate.getZ() - center.getZ());
        int dy = candidate.getY() - baseY;
        if (dx > 1 || dz > 1 || dy < 0 || dy >= height) return false;
        return dy == 0 || dy == height - 1 || dx == 1 || dz == 1;
    }

    static boolean isExterior(@Nullable BlockPos center, int baseY, int height,
            BlockPos at, @Nullable EnumFacing side) {
        if (side == null || center == null) return side == null;
        return side == EnumFacing.DOWN && at.getY() == baseY
                || side == EnumFacing.UP && at.getY() == baseY + height - 1
                || side.getAxis().isHorizontal()
                && (at.getX() - center.getX()) * side.getXOffset()
                + (at.getZ() - center.getZ()) * side.getZOffset() == 1;
    }

    static void linkStorageCasings(World world, BlockPos controllerPos, @Nullable BlockPos center,
            int baseY, int height, @Nullable BlockPos masterPos) {
        if (world == null || center == null) return;
        for (BlockPos shell : shellPositions(center, baseY, height)) {
            if (shell.equals(controllerPos) || !world.isBlockLoaded(shell, false)) continue;
            TileEntity tile = world.getTileEntity(shell);
            if (tile instanceof StorageCasingTile) {
                ((StorageCasingTile) tile).setController(masterPos);
            }
        }
    }

    static void updateShellComparators(World world, BlockPos controllerPos, @Nullable BlockPos center,
            int baseY, int height, Block casingBlock) {
        if (world == null || center == null) return;
        for (BlockPos shell : shellPositions(center, baseY, height)) {
            if (!shell.equals(controllerPos) && world.isBlockLoaded(shell, false)) {
                world.updateComparatorOutputLevel(shell, casingBlock);
            }
        }
    }

    static boolean canUseFromShell(FarmTile controller, EntityPlayer player, @Nullable BlockPos center,
            int baseY, int height, Block casing) {
        World world = controller.getWorld();
        BlockPos master = controller.getPos();
        if (center == null || controller.isInvalid() || world == null || player.world != world
                || !world.isBlockLoaded(master, false) || world.getTileEntity(master) != controller) return false;
        for (BlockPos part : shellPositions(center, baseY, height)) {
            if (player.getDistanceSq(part) <= 64 && world.isBlockLoaded(part, false)
                    && world.getBlockState(part).getBlock() == casing) return true;
        }
        return false;
    }

    static int comparatorSignal(long current, long capacity) {
        if (capacity <= 0 || current <= 0) return 0;
        return Math.min(15, Math.max(1, (int) Math.floor(current * 14.0 / capacity) + 1));
    }

    static void readFluidTank(FluidTank tank, NBTTagCompound nbt) {
        // ItemBlock.setTileEntityNBT merges BlockEntityTag onto an empty tile's NBT, leaving "Empty" alongside "FluidName".
        if (nbt.hasKey("Empty") && nbt.hasKey("FluidName")) {
            NBTTagCompound clean = nbt.copy();
            clean.removeTag("Empty");
            tank.readFromNBT(clean);
        } else {
            tank.readFromNBT(nbt);
        }
    }
}
