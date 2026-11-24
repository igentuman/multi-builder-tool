package igentuman.mbtool.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.items.tools.powered.WirelessTerminalItem;
import igentuman.mbtool.integration.curios.CuriosHelper;
import igentuman.mbtool.util.ModUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;


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
     * The player's ME grid via a powered wireless terminal, with the terminal's IActionHost (needed so
     * crafting calculations can see network storage). {@code slot} is the terminal's player inventory
     * slot, or -1 when it is equipped as a curio (AE2 cannot open its confirmation menu from there).
     */
    record GridContext(IGrid grid, IActionHost actionHost, int slot) {}

    static GridContext getPlayerGridContext(ServerPlayer player) {
        // Main inventory, armor and offhand (0..40): slots AE2 can open a terminal menu from.
        int slots = Math.min(Inventory.SLOT_OFFHAND + 1, player.getInventory().getContainerSize());
        for (int i = 0; i < slots; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack.getItem() instanceof WirelessTerminalItem terminal && terminal.hasPower(player, 1, itemStack)) {
                IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), component -> {});
                if (grid != null) {
                    ItemMenuHostLocator locator = MenuLocators.forInventorySlot(i);
                    ItemMenuHost menuHost = terminal.getMenuHost(player, locator, null);
                    if (menuHost instanceof IActionHost actionHost) {
                        var node = actionHost.getActionableNode();
                        if (node != null && node.isActive()) return new GridContext(grid, actionHost, i);
                    }
                }
            }
        }
        if (ModUtil.isCuriosLoaded()) {
            for (ItemStack itemStack : CuriosHelper.getEquippedCuriosItems(player)) {
                if (itemStack.getItem() instanceof WirelessTerminalItem terminal && terminal.hasPower(player, 1, itemStack)) {
                    IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), component -> {});
                    if (grid != null) {
                        // Curios items don't have a standard inventory slot; fall back to grid pivot node
                        IActionHost actionHost = grid.getPivot() != null
                                && grid.getPivot().getOwner() instanceof IActionHost ah ? ah : null;
                        return new GridContext(grid, actionHost, -1);
                    }
                }
            }
        }
        return null;
    }

    public static long getAvailableAmount(ServerPlayer player, Block requiredBlock) {
        IGrid grid = getPlayerGrid(player);
        if (grid == null) return 0;
        KeyCounter stacks = grid.getStorageService().getInventory().getAvailableStacks();
        AEItemKey key = AEItemKey.of(requiredBlock.asItem().getDefaultInstance());
        return stacks.get(key);
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

    static boolean isCpuUnavailable(CraftingSubmitErrorCode errorCode) {
        return errorCode == CraftingSubmitErrorCode.CPU_BUSY
                || errorCode == CraftingSubmitErrorCode.CPU_OFFLINE
                || errorCode == CraftingSubmitErrorCode.NO_CPU_FOUND
                || errorCode == CraftingSubmitErrorCode.NO_SUITABLE_CPU_FOUND
                || errorCode == CraftingSubmitErrorCode.CPU_TOO_SMALL;
    }
}
