package igentuman.mbtool.integration.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.items.tools.powered.WirelessTerminalItem;
import igentuman.mbtool.integration.curios.CuriosHelper;
import igentuman.mbtool.util.ModUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class AE2Helper {
    public static boolean hasAe2Terminal(ServerPlayer player) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof WirelessTerminalItem) {
                return true;
            }
        }
        if (ModUtil.isCuriosLoaded()) {
            List<ItemStack> items = CuriosHelper.getEquippedCuriosItems(player);
            for (ItemStack itemStack : items) {
                if (itemStack.getItem() instanceof WirelessTerminalItem) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasEnough(ServerPlayer player, Block requiredBlock, int required) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof WirelessTerminalItem terminal) {
                IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), player);
                if (grid == null) {
                    continue;
                }
                KeyCounter stacks = grid.getStorageService().getInventory().getAvailableStacks();
                AEItemKey key = AEItemKey.of(requiredBlock.asItem().getDefaultInstance());
                long stored = stacks.get(key);
                return stored >= required;
            }
        }
        if (ModUtil.isCuriosLoaded()) {
            List<ItemStack> items = CuriosHelper.getEquippedCuriosItems(player);
            for (ItemStack itemStack : items) {
                if (itemStack.getItem() instanceof WirelessTerminalItem terminal) {
                    IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), player);
                    if (grid == null) {
                        continue;
                    }
                    KeyCounter stacks = grid.getStorageService().getInventory().getAvailableStacks();
                    AEItemKey key = AEItemKey.of(requiredBlock.asItem().getDefaultInstance());
                    long stored = stacks.get(key);
                    return stored >= required;
                }
            }
        }
        return false;
    }

    /**
     * Extracts items from the ME network via the player's wireless terminal.
     * @param player The player with a wireless terminal
     * @param block The block type to extract
     * @param amount The number of items to extract
     * @return The number of items actually extracted
     */
    public static long extractItems(ServerPlayer player, Block block, int amount) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof WirelessTerminalItem terminal) {
                IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), player);
                if (grid == null) {
                    continue;
                }
                MEStorage storage = grid.getStorageService().getInventory();
                AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());
                return storage.extract(key, amount, appeng.api.config.Actionable.MODULATE, IActionSource.ofPlayer(player));
            }
        }
        if (ModUtil.isCuriosLoaded()) {
            List<ItemStack> items = CuriosHelper.getEquippedCuriosItems(player);
            for (ItemStack itemStack : items) {
                if (itemStack.getItem() instanceof WirelessTerminalItem terminal) {
                    IGrid grid = terminal.getLinkedGrid(itemStack, player.level(), player);
                    if (grid == null) {
                        continue;
                    }
                    MEStorage storage = grid.getStorageService().getInventory();
                    AEItemKey key = AEItemKey.of(block.asItem().getDefaultInstance());
                    return storage.extract(key, amount, appeng.api.config.Actionable.MODULATE, IActionSource.ofPlayer(player));
                }
            }
        }
        return 0;
    }
}
