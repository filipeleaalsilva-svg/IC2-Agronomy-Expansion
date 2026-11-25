package br.cropmanager;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Shared contract for EU-free 3x5x3 storage multiblock controllers exposed via {@link StorageCasingTile}.
 */
interface StorageMultiblock {
    /** @return {@code true} when the 3x5x3 shell is currently valid and formed. */
    boolean isFormed();

    /** @return {@code true} if {@code casingPos} belongs to this controller's formed shell. */
    boolean containsCasing(BlockPos casingPos);

    /** @return {@code true} if {@code side} of {@code at} faces outside the formed shell. */
    boolean isExterior(BlockPos at, @Nullable EnumFacing side);

    /** Notifies the controller that a bound shell casing at {@code casingPos} was broken. */
    void casingBroken(BlockPos casingPos);

    /** @return redstone comparator level (0-15) representing current fill ratio. */
    int comparatorSignal();

    /** Opens the controller GUI for {@code player} when interacting with a shell casing. */
    boolean openFromCasing(EntityPlayer player);

    /** Queries whether this controller exposes {@code capability} on exterior {@code side}. */
    boolean hasPortCapability(Capability<?> capability, @Nullable EnumFacing side);

    /** Returns the capability instance for exterior {@code side}, or {@code null} if unavailable. */
    @Nullable
    <T> T getPortCapability(Capability<T> capability, @Nullable EnumFacing side);
}
