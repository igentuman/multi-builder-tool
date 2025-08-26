package igentuman.mbtool.util;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class CapabilityUtils {

    private CapabilityUtils() {
    }

    /**
     * Gets a capability from a block at the specified position
     */
    @Nullable
    public static <T> T getBlockCapability(Level level, BlockPos pos, BlockCapability<T, @Nullable Direction> capability, @Nullable Direction side) {
        return level.getCapability(capability, pos, side);
    }

    /**
     * Gets a capability from a block at the specified position (no side context)
     */
    @Nullable
    public static <T> T getBlockCapability(Level level, BlockPos pos, BlockCapability<T, Void> capability) {
        return level.getCapability(capability, pos, null);
    }

    /**
     * Gets a capability from an item stack
     */
    @Nullable
    public static <T> T getItemCapability(ItemStack stack, ItemCapability<T, Void> capability) {
        return stack.getCapability(capability);
    }

    /**
     * Gets a capability from an item stack with context
     */
    @Nullable
    public static <T, C> T getItemCapability(ItemStack stack, ItemCapability<T, C> capability, C context) {
        return stack.getCapability(capability, context);
    }

    /**
     * Gets a present capability from a block, throwing an exception if not available
     */
    @NotNull
    public static <T> T getPresentBlockCapability(Level level, BlockPos pos, BlockCapability<T, @Nullable Direction> capability, @Nullable Direction side) {
        return Objects.requireNonNull(getBlockCapability(level, pos, capability, side));
    }

    /**
     * Gets a present capability from an item stack, throwing an exception if not available
     */
    @NotNull
    public static <T> T getPresentItemCapability(ItemStack stack, ItemCapability<T, Void> capability) {
        return Objects.requireNonNull(getItemCapability(stack, capability));
    }
}