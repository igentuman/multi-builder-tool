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

    /**
     * Holds a reference to both the ME grid and the IActionHost (from the wireless terminal).
     * The IActionHost is needed to create a proper action source that has grid node context,
     * which is required for crafting calculations to access network storage.
     */
    private record GridContext(IGrid grid, IActionHost actionHost) {}

    /**
     * Finds the ME grid accessible by the player via a wireless terminal
     * in their inventory or curios slots. Returns both the grid and the
     * IActionHost so callers can build a proper IActionSource.
     */
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

    /**
     * Extracts items from the ME network via the player's wireless terminal.
     */
    public static long extractItems(ServerPlayer player, Block block, int amount) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return 0;
        MEStorage storage = grid.getStorageService().getInventory();
        AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());
        return storage.extract(key, amount, Actionable.MODULATE, IActionSource.ofPlayer(player));
    }

    /**
     * Quick check if a block is craftable via the ME network.
     */
    public static boolean isCraftable(ServerPlayer player, Block block) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return false;
        AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());
        return grid.getCraftingService().isCraftable(key);
    }

    /**
     * Non-blocking: starts an async crafting calculation and stores the Future in AutocraftTracker.
     * Returns immediately without blocking the server thread.
     *
     * @return CALCULATING if the calculation was started, NO_PATTERN if no pattern exists, NO_GRID if no grid.
     */
    public static AutocraftResult startCraftingCalculation(ServerPlayer player, Block block, int amount) {
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
            // Use IActionSource.ofPlayer(player, actionHost) to provide grid node context.
            // Without the IActionHost, the crafting calculation cannot access network storage
            // and reports all items as missing (plan.simulation() == true).
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
            AutocraftResult result = trySubmitCraftingJob(player, block);
            if (result == null) continue;
            switch (result) {
                case MISSING_INGREDIENTS -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_missing_ingredients", block.getName()));
                case NO_GRID -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_no_grid"));
                case CPU_BUSY -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_cpu_busy", block.getName()));
                case FAILED -> player.sendSystemMessage(
                        Component.translatable("message.mbtool.autocrafting_failed"));
                default -> {} // CALCULATING, SUBMITTED, etc. — no message needed
            }
        }
    }

    /**
     * Non-blocking: checks if a previously started crafting calculation has completed.
     * If done, submits the crafting job. Never blocks the server thread.
     *
     * @return the current status, or null if there is no pending request for this block.
     */
    public static AutocraftResult trySubmitCraftingJob(ServerPlayer player, Block block) {
        AutocraftTracker.CraftRequest request = AutocraftTracker.getRequest(player.getUUID(), block);
        if (request == null) return null;

        // Already submitted successfully on a previous call
        if (request.status == AutocraftTracker.RequestStatus.SUBMITTED) {
            if(request.planFuture == null) {
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

            if (submitResult.successful()) {
                AutocraftTracker.markSubmitted(player.getUUID(), block);
                return AutocraftResult.SUBMITTED;
            }

            // Map error codes
            AutocraftTracker.markFailed(player.getUUID(), block);
            CraftingSubmitErrorCode errorCode = submitResult.errorCode();
            if (errorCode == CraftingSubmitErrorCode.CPU_BUSY
                    || errorCode == CraftingSubmitErrorCode.NO_CPU_FOUND
                    || errorCode == CraftingSubmitErrorCode.NO_SUITABLE_CPU_FOUND
                    || errorCode == CraftingSubmitErrorCode.CPU_TOO_SMALL) {
                return AutocraftResult.CPU_BUSY;
            }
            if (errorCode == CraftingSubmitErrorCode.MISSING_INGREDIENT
                    || errorCode == CraftingSubmitErrorCode.INCOMPLETE_PLAN) {
                return AutocraftResult.MISSING_INGREDIENTS;
            }

            return AutocraftResult.FAILED;

        } catch (Exception e) {
            AutocraftTracker.markFailed(player.getUUID(), block);
            return AutocraftResult.FAILED;
        }
    }
}
