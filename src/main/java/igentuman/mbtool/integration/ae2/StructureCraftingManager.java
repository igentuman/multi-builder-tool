package igentuman.mbtool.integration.ae2;

import appeng.api.stacks.AEItemKey;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.item.StructureRequestItem;
import igentuman.mbtool.util.BlockEquivalencyManager;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StructureCraftingManager {
    private static final Map<UUID, StructureCraftingSession> SESSIONS = new HashMap<>();
    private static int nextRequestId;

    private StructureCraftingManager() {}

    public static void request(ServerPlayer player, ItemStack tool, MultiblockStructure structure) {
        if (!(tool.getItem() instanceof MultibuilderItem item) || !item.isMeAccessAllowed(tool)) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.me_disabled"));
            return;
        }
        AE2Helper.GridContext terminal = AE2Helper.getPlayerGridContext(player);
        if (terminal == null) {
            player.sendSystemMessage(Component.translatable("message.mbtool.autocrafting_no_grid"));
            return;
        }
        if (terminal.slot() < 0) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.terminal_slot"));
            return;
        }
        UUID id = player.getUUID();
        StructureCraftingSession existing = SESSIONS.get(id);
        if (existing != null) {
            if (existing.isCrafting()) {
                player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.already_running",
                        StructureRequestItem.structureName(existing.structureName)));
                return;
            }
            SESSIONS.remove(id);
            existing.close(player);
        }

        Component name = StructureRequestItem.structureName(structure.getName());
        int[] skipped = new int[1];
        List<StructureCraftingPattern.Input> inputs = collectInputs(item.getInventory(tool), structure, skipped);
        if (skipped[0] > 0) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.skipped", skipped[0]));
        }
        if (inputs.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.nothing_needed", name));
            return;
        }
        try {
            SESSIONS.put(id, new StructureCraftingSession(player, terminal.grid(), structure.getName(), inputs, nextRequestId++));
        } catch (RuntimeException e) {
            Mbtool.logger.error("Failed to start structure crafting request", e);
            player.sendSystemMessage(Component.translatable("message.mbtool.autocrafting_failed"));
        }
    }

    private static List<StructureCraftingPattern.Input> collectInputs(IItemHandler inventory, MultiblockStructure structure,
                                                                     int[] skipped) {
        Map<Block, Integer> missing = new LinkedHashMap<>();
        for (BlockState state : structure.getBlocks().values()) {
            if (!state.isAir()) missing.merge(state.getBlock(), 1, Integer::sum);
        }
        Map<Block, Integer> available = new HashMap<>();
        for (int slot = 0; inventory != null && slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Block block = stack.isEmpty() ? Blocks.AIR : Block.byItem(stack.getItem());
            if (block != Blocks.AIR) available.merge(block, stack.getCount(), Integer::sum);
        }
        missing.replaceAll((block, count) -> take(available, block, count));
        missing.replaceAll((block, count) -> {
            for (Block equivalent : BlockEquivalencyManager.getEquivalentBlocks(block)) {
                if (count == 0) break;
                if (equivalent != block) count = take(available, equivalent, count);
            }
            return count;
        });

        Map<Item, Long> amounts = new LinkedHashMap<>();
        Map<Item, Set<Item>> candidates = new HashMap<>();
        for (Map.Entry<Block, Integer> entry : missing.entrySet()) {
            if (entry.getValue() <= 0) continue;
            Item item = entry.getKey().asItem();
            if (item == Items.AIR) {
                skipped[0]++;
                continue;
            }
            amounts.merge(item, (long) entry.getValue(), Long::sum);
            Set<Item> options = candidates.computeIfAbsent(item, first -> new LinkedHashSet<>(List.of(first)));
            for (Block equivalent : BlockEquivalencyManager.getEquivalentBlocks(entry.getKey())) {
                if (equivalent.asItem() != Items.AIR) options.add(equivalent.asItem());
            }
        }
        List<StructureCraftingPattern.Input> inputs = new ArrayList<>();
        amounts.forEach((item, amount) -> inputs.add(new StructureCraftingPattern.Input(
                candidates.get(item).stream().map(AEItemKey::of).toList(), amount)));
        return inputs;
    }

    private static int take(Map<Block, Integer> available, Block block, int needed) {
        int stored = available.getOrDefault(block, 0);
        int used = Math.min(stored, needed);
        if (used > 0) available.put(block, stored - used);
        return needed - used;
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        Iterator<Map.Entry<UUID, StructureCraftingSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, StructureCraftingSession> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            boolean keep;
            try {
                keep = player != null && entry.getValue().tick(player);
            } catch (RuntimeException e) {
                Mbtool.logger.error("Structure crafting request failed", e);
                keep = false;
            }
            if (!keep) {
                it.remove();
                try {
                    entry.getValue().close(player);
                } catch (RuntimeException e) {
                    Mbtool.logger.error("Failed to close structure crafting request", e);
                }
            }
        }
    }

    public static void onLogout(ServerPlayer player) {
        StructureCraftingSession session = SESSIONS.remove(player.getUUID());
        if (session != null) session.close(player);
    }

    public static void shutdown(MinecraftServer server) {
        for (Map.Entry<UUID, StructureCraftingSession> entry : SESSIONS.entrySet()) {
            try {
                entry.getValue().close(server.getPlayerList().getPlayer(entry.getKey()));
            } catch (RuntimeException e) {
                Mbtool.logger.error("Failed to close structure crafting request", e);
            }
        }
        SESSIONS.clear();
    }
}
