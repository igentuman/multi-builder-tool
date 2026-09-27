package igentuman.mbtool.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.items.tools.powered.WirelessTerminalItem;
import igentuman.mbtool.integration.curios.CuriosHelper;
import igentuman.mbtool.util.AutocraftTracker;
import igentuman.mbtool.util.ModUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class AE2Helper {

    /**
     * Result of an autocrafting request.
     */
    public enum AutocraftResult {
        CALCULATING,         // Crafting plan is being calculated (non-blocking)
        SUBMITTED,           // Crafting job submitted successfully
        NO_PATTERN,          // No crafting pattern exists for this item
        MISSING_INGREDIENTS, // Pattern exists but ingredients are missing
        NO_GRID,             // Cannot connect to ME grid
        CPU_BUSY,            // All crafting CPUs are busy
        ALREADY_REQUESTING,  // Already being crafted by the ME network
        FAILED               // Generic failure
    }

    private record GridContext(IGrid grid, IActionHost actionHost) {}

    private static GridContext getPlayerGridContext(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack itemStack = player.getInventory().items.get(i);
            if (itemStack.getItem() instanceof WirelessTerminalItem terminal) {
                IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), player);
                if (grid != null) {
                    ItemMenuHost menuHost = terminal.getMenuHost(player, i, itemStack, null);
                    IActionHost actionHost = menuHost instanceof IActionHost ah ? ah : null;
                    return new GridContext(grid, actionHost);
                }
            }
        }
        if (ModUtil.isCuriosLoaded()) {
            for (ItemStack itemStack : CuriosHelper.getEquippedCuriosItems(player)) {
                if (itemStack.getItem() instanceof WirelessTerminalItem terminal) {
                    IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), player);
                    if (grid != null) {
                        // Curios items don't have a standard inventory slot; fall back to grid pivot node
                        IActionHost actionHost = grid.getPivot() != null
                                && grid.getPivot().getOwner() instanceof IActionHost ah ? ah : null;
                        return new GridContext(grid, actionHost);
                    }
                }
            }
        }
        return null;
    }

    private static IGrid getPlayerGrid(ServerPlayer player) {
        GridContext ctx = getPlayerGridContext(player);
        return ctx != null ? ctx.grid : null;
    }

    public static boolean hasAe2Terminal(ServerPlayer player) {
        return getPlayerGrid(player) != null;
    }

    public static boolean hasEnough(ServerPlayer player, Block requiredBlock, int required) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return false;
        KeyCounter stacks = grid.getStorageService().getInventory().getAvailableStacks();
        AEItemKey key = AEItemKey.of(requiredBlock.asItem().getDefaultInstance());
        long stored = stacks.get(key);
        return stored >= required;
    }

    public static long getAvailableAmount(ServerPlayer player, Block requiredBlock) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return 0;
        KeyCounter stacks = grid.getStorageService().getInventory().getAvailableStacks();
        AEItemKey key = AEItemKey.of(requiredBlock.asItem().getDefaultInstance());
        return stacks.get(key);
    }

    public static long extractItems(ServerPlayer player, Block block, int amount) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return 0;
        MEStorage storage = grid.getStorageService().getInventory();
        AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());
        return storage.extract(key, amount, Actionable.MODULATE, IActionSource.ofPlayer(player));
    }

    public static boolean isCraftable(ServerPlayer player, Block block) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return false;
        AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());
        return grid.getCraftingService().isCraftable(key);
    }

    public static AutocraftResult startCraftingCalculation(ServerPlayer player, Block block, int amount) {
        AutocraftTracker.CraftRequest existing = AutocraftTracker.getRequest(player.getUUID(), block);
        if (existing != null && AutocraftTracker.hasActiveRequest(player.getUUID(), block)) {
            return existing.status == AutocraftTracker.RequestStatus.SUBMITTED
                    ? AutocraftResult.ALREADY_REQUESTING : AutocraftResult.CALCULATING;
        }

        GridContext ctx = getPlayerGridContext(player);
        if (ctx == null) return AutocraftResult.NO_GRID;

        ICraftingService craftingService = ctx.grid.getCraftingService();
        AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());

        if (!craftingService.isCraftable(key)) {
            return AutocraftResult.NO_PATTERN;
        }

        // If already being crafted by the ME network, no need to request again
        if (craftingService.isRequesting(key)) {
            return AutocraftResult.ALREADY_REQUESTING;
        }

        try {
            IActionSource actionSource = ctx.actionHost != null
                    ? IActionSource.ofPlayer(player, ctx.actionHost)
                    : IActionSource.ofPlayer(player);
            ICraftingSimulationRequester simRequester = new ICraftingSimulationRequester() {
                @Override
                public IActionSource getActionSource() {
                    return actionSource;
                }
            };

            Future<ICraftingPlan> future = craftingService.beginCraftingCalculation(
                    player.level(), simRequester, key, amount,
                    CalculationStrategy.REPORT_MISSING_ITEMS);

            // Store the future for later polling
            AutocraftTracker.trackCalculation(player.getUUID(), block, future, amount);

            return AutocraftResult.CALCULATING;
        } catch (Exception e) {
            return AutocraftResult.FAILED;
        }
    }

    public static void tickJobs(ServerPlayer player) {
        Map<Block, AutocraftTracker.CraftRequest> requests = AutocraftTracker.activeRequests.get(player.getUUID());
        if (requests == null) return;
        // Iterate over a snapshot to avoid ConcurrentModificationException (markFailed removes entries)
        Set<Map.Entry<Block, AutocraftTracker.CraftRequest>> snapshot = Set.copyOf(requests.entrySet());
        for (Map.Entry<Block, AutocraftTracker.CraftRequest> entry : snapshot) {
            Block block = entry.getKey();
            boolean wasWaitingForCpu = entry.getValue().status == AutocraftTracker.RequestStatus.WAITING_FOR_CPU;
            AutocraftResult result = trySubmitCraftingJob(player, block);
            if (result == null) continue;
            switch (result) {
                case MISSING_INGREDIENTS -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_missing_ingredients", block.getName()));
                case NO_GRID -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_no_grid"));
                case CPU_BUSY -> {
                    if (!wasWaitingForCpu) {
                        player.sendSystemMessage(Component.translatable(
                                "message.mbtool.autocrafting_cpu_busy", block.getName()));
                    }
                }
                case FAILED -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_failed"));
                default -> {} // CALCULATING, SUBMITTED, etc. — no message needed
            }
        }
    }

    public static AutocraftResult trySubmitCraftingJob(ServerPlayer player, Block block) {
        AutocraftTracker.CraftRequest request = AutocraftTracker.getRequest(player.getUUID(), block);
        if (request == null) return null;

        // Keep submitted requests tracked while the ME network is crafting them.
        if (request.status == AutocraftTracker.RequestStatus.SUBMITTED) {
            GridContext ctx = getPlayerGridContext(player);
            if (ctx != null && !ctx.grid.getCraftingService().isRequesting(
                    AEItemKey.of(block.asItem().getDefaultInstance()))) {
                AutocraftTracker.markFailed(player.getUUID(), block);
                return null;
            }
            return AutocraftResult.SUBMITTED;
        }

        // Still calculating — check if future is done (non-blocking)
        if (request.planFuture == null || !request.planFuture.isDone()) {
            return AutocraftResult.CALCULATING;
        }

        // Future is done — retrieve result and submit job
        try {
            ICraftingPlan plan = (ICraftingPlan) request.planFuture.get(); // already done, won't block

            if (plan.simulation()) {
                AutocraftTracker.markFailed(player.getUUID(), block);
                return AutocraftResult.MISSING_INGREDIENTS;
            }

            GridContext ctx = getPlayerGridContext(player);
            if (ctx == null) {
                AutocraftTracker.markFailed(player.getUUID(), block);
                return AutocraftResult.NO_GRID;
            }

            IActionSource actionSource = ctx.actionHost != null
                    ? IActionSource.ofPlayer(player, ctx.actionHost)
                    : IActionSource.ofPlayer(player);
            ICraftingSubmitResult submitResult = ctx.grid.getCraftingService().submitJob(
                    plan, null, null, false, actionSource);
            return recordSubmitResult(player.getUUID(), block, submitResult);

        } catch (Exception e) {
            AutocraftTracker.markFailed(player.getUUID(), block);
            return AutocraftResult.FAILED;
        }
    }

    static AutocraftResult recordSubmitResult(UUID playerId, Block block, ICraftingSubmitResult submitResult) {
        if (submitResult.successful()) {
            AutocraftTracker.markSubmitted(playerId, block);
            return AutocraftResult.SUBMITTED;
        }

        // Retain the completed plan and its original amount until a CPU is available.
        CraftingSubmitErrorCode errorCode = submitResult.errorCode();
        if (isCpuUnavailable(errorCode)) {
            AutocraftTracker.markWaitingForCpu(playerId, block);
            return AutocraftResult.CPU_BUSY;
        }
        AutocraftTracker.markFailed(playerId, block);
        if (errorCode == CraftingSubmitErrorCode.MISSING_INGREDIENT
                || errorCode == CraftingSubmitErrorCode.INCOMPLETE_PLAN) {
            return AutocraftResult.MISSING_INGREDIENTS;
        }

        return AutocraftResult.FAILED;
    }

    static boolean isCpuUnavailable(CraftingSubmitErrorCode errorCode) {
        return errorCode == CraftingSubmitErrorCode.CPU_BUSY
                || errorCode == CraftingSubmitErrorCode.NO_CPU_FOUND
                || errorCode == CraftingSubmitErrorCode.NO_SUITABLE_CPU_FOUND
                || errorCode == CraftingSubmitErrorCode.CPU_TOO_SMALL;
    }
}
