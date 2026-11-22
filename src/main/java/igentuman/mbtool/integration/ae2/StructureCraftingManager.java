package igentuman.mbtool.integration.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.hooks.ticking.TickHandler;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.item.StructureRequestItem;
import igentuman.mbtool.util.BlockEquivalencyManager;
import igentuman.mbtool.util.BlockReplacementPreferences;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.nbt.CompoundTag;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StructureCraftingManager {
    private static final Map<UUID, StructureCraftingSession> SESSIONS = new HashMap<>();

    private StructureCraftingManager() {}

    public static void request(ServerPlayer player, ItemStack tool, MultiblockStructure structure) {
        request(player, tool, structure, BlockReplacementPreferences.EMPTY);
    }

    public static void request(ServerPlayer player, ItemStack tool, MultiblockStructure structure,
                               BlockReplacementPreferences preferences) {
        Map<Block, Integer> requiredBlocks = new LinkedHashMap<>();
        for (BlockState state : structure.getBlocks().values()) {
            if (!state.isAir()) requiredBlocks.merge(state.getBlock(), 1, Integer::sum);
        }
        request(player, tool, structure, requiredBlocks, preferences);
    }

    /** Requests confirmation for the blocks needed by this placement, excluding skipped positions. */
    public static void request(ServerPlayer player, ItemStack tool, MultiblockStructure structure,
                               Map<Block, Integer> requiredBlocks) {
        request(player, tool, structure, requiredBlocks, BlockReplacementPreferences.EMPTY);
    }

    public static void request(ServerPlayer player, ItemStack tool, MultiblockStructure structure,
                               Map<Block, Integer> requiredBlocks, BlockReplacementPreferences preferences) {
        preferences = preferences.validated(BlockEquivalencyManager.getGroups());
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
        CraftingJobLedger ledger = CraftingJobLedger.get(player.server);
        if (ledger.active(player.getUUID(), structure.getName()) != null) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.already_running",
                    StructureRequestItem.structureName(structure.getName())));
            return;
        }

        Component name = StructureRequestItem.structureName(structure.getName());
        int[] skipped = new int[1];
        List<StructureCraftingPattern.Input> inputs = collectInputs(item.getInventory(tool), requiredBlocks, skipped, preferences,
                block -> AE2Helper.getAvailableAmount(player, block));
        if (skipped[0] > 0) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.skipped", skipped[0]));
        }
        if (inputs.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.mbtool.structure_craft.nothing_needed", name));
            return;
        }
        CraftingJobLedger.Job job = new CraftingJobLedger.Job();
        job.owner = player.getUUID();
        ledger.associate(job, structure.getName(), job.id);
        job.amount = 1;
        job.grid = terminal.grid();
        job.network = CraftingJobTracker.networkLocation(terminal.grid());
        CompoundTag result = new CompoundTag();
        result.putString(StructureRequestItem.TAG_STRUCTURE, structure.getName());
        result.putUUID("owner", player.getUUID());
        result.putUUID("request", job.id);
        job.output = AEItemKey.of(Mbtool.STRUCTURE_REQUEST.get(), result);
        for (StructureCraftingPattern.Input input : inputs) {
            for (GenericStack key : input.getPossibleInputs()) {
                job.coverage.add(new GenericStack(key.what(), input.getMultiplier()));
            }
        }
        ledger.add(job);
        try {
            SESSIONS.put(job.id, new StructureCraftingSession(player, terminal.grid(), structure.getName(), inputs, job));
        } catch (RuntimeException e) {
            job.finish(CraftingJobLedger.Stage.FAILED);
            Mbtool.logger.error("Failed to start structure crafting request", e);
            player.sendSystemMessage(Component.translatable("message.mbtool.autocrafting_failed"));
        }
        CraftingJobTracker.sync(player);
    }

    static List<StructureCraftingPattern.Input> collectInputs(IItemHandler inventory, Map<Block, Integer> requiredBlocks,
                                                              int[] skipped, BlockReplacementPreferences preferences,
                                                              java.util.function.ToLongFunction<Block> storedAmount) {
        Map<Block, Integer> missing = new LinkedHashMap<>(requiredBlocks);
        Map<Block, Integer> available = new HashMap<>();
        for (int slot = 0; inventory != null && slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Block block = stack.isEmpty() ? Blocks.AIR : Block.byItem(stack.getItem());
            if (block != Blocks.AIR) available.merge(block, stack.getCount(), Integer::sum);
        }
        Map<Block, Integer> preferredStored = new HashMap<>();
        Map<Item, Long> reserved = new LinkedHashMap<>();
        // Reserve preferred ME stock before counting lower-priority tool inventory as coverage.
        missing.replaceAll((block, count) -> {
            Block preferred = preferences.preferred(block, BlockEquivalencyManager.getGroups());
            if (preferred == null) return count;
            count = take(available, preferred, count);
            int stored = preferredStored.computeIfAbsent(preferred,
                    candidate -> (int) Math.min(Integer.MAX_VALUE, Math.max(0, storedAmount.applyAsLong(candidate))));
            int used = Math.min(count, stored);
            if (used > 0) {
                preferredStored.put(preferred, stored - used);
                reserved.merge(preferred.asItem(), (long) used, Long::sum);
            }
            return count - used;
        });
        missing.replaceAll((block, count) -> take(available, block, count));
        missing.replaceAll((block, count) -> {
            for (Block equivalent : BlockEquivalencyManager.getEquivalentBlocks(block)) {
                if (count == 0) break;
                if (equivalent != block) count = take(available, equivalent, count);
            }
            return count;
        });

        // Candidate order is significant to AE2: preferred, original, remaining equivalents.
        Map<List<AEItemKey>, Long> amounts = new LinkedHashMap<>();
        reserved.forEach((item, amount) -> amounts.merge(List.of(AEItemKey.of(item)), amount, Long::sum));
        for (Map.Entry<Block, Integer> entry : missing.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (entry.getKey().asItem() == Items.AIR) {
                skipped[0]++;
                continue;
            }
            List<AEItemKey> options = BlockEquivalencyManager.orderedCandidates(entry.getKey(), preferences).stream()
                    .map(Block::asItem).filter(item -> item != Items.AIR).distinct().map(AEItemKey::of).toList();
            amounts.merge(options, (long) entry.getValue(), Long::sum);
        }
        List<StructureCraftingPattern.Input> inputs = new ArrayList<>();
        amounts.forEach((options, amount) -> inputs.add(new StructureCraftingPattern.Input(options, amount)));
        return inputs;
    }

    private static int take(Map<Block, Integer> available, Block block, int needed) {
        int stored = available.getOrDefault(block, 0);
        int used = Math.min(stored, needed);
        if (used > 0) available.put(block, stored - used);
        return needed - used;
    }

    public static boolean canSubmit(MinecraftServer server, ICraftingPlan plan) {
        if (!(plan.finalOutput().what() instanceof AEItemKey key) || key.getItem() != Mbtool.STRUCTURE_REQUEST.get()) return true;
        if (key.getTag() == null || !key.getTag().hasUUID("request")) return false;
        CraftingJobLedger.Job job = CraftingJobLedger.get(server).jobs.get(key.getTag().getUUID("request"));
        return job != null && job.output.equals(key) && job.active() && !job.cancelRequested
                && job.stage != CraftingJobLedger.Stage.CRAFTING;
    }

    public static void recordSubmitted(MinecraftServer server, ICraftingPlan plan,
                                       ICraftingSubmitResult result,
                                       ICraftingCPU cpu) {
        if (!result.successful() || !(plan.finalOutput().what() instanceof AEItemKey key)
                || key.getItem() != Mbtool.STRUCTURE_REQUEST.get() || key.getTag() == null
                || !key.getTag().hasUUID("request")) return;
        CraftingJobLedger ledger = CraftingJobLedger.get(server);
        CraftingJobLedger.Job job = ledger.jobs.get(key.getTag().getUUID("request"));
        if (job == null || !job.active() || !job.output.equals(key)) return;
        job.amount = plan.finalOutput().amount();
        job.link = result.link();
        job.craftingId = result.link().getCraftingID();
        job.stage = CraftingJobLedger.Stage.CRAFTING;
        job.display = "crafting";
        CraftingJobTracker.bind(job, cpu);
        ledger.setDirty();
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            for (IGrid grid : TickHandler.instance().getGridList()) {
                if (grid.getPivot() == null || grid.getPivot().getLevel().getServer() != server) continue;
                try {
                    StructureCraftingOutput.removeStoredTokens(grid, AEItemKey.of(Mbtool.STRUCTURE_REQUEST.get()));
                } catch (RuntimeException exception) {
                    if (server.getTickCount() % 200 == 0) {
                        Mbtool.logger.warn("Could not remove stored structure crafting tokens", exception);
                    }
                }
            }
        }
        CraftingJobLedger ledger = CraftingJobLedger.get(server);
        for (CraftingJobLedger.Job job : List.copyOf(ledger.jobs.values())) {
            if (job.session.isEmpty()) continue;
            StructureCraftingSession session = SESSIONS.get(job.id);
            ServerPlayer player = server.getPlayerList().getPlayer(job.owner);
            try {
                if (session == null) {
                    if (!job.active() && job.session.getList("returned", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty()) {
                        job.session = new CompoundTag();
                        ledger.setDirty();
                        continue;
                    }
                    IGrid grid = CraftingJobTracker.resolveGrid(server, job);
                    boolean recoverToPlayer = !job.active() && player != null;
                    if (!recoverToPlayer && (grid == null || CraftingJobTracker.loadedLevel(server, job.network) == null)) {
                        if (job.active()) job.display = job.cancelRequested ? "cancelling" : "disconnected";
                        if (job.active() && job.stage != CraftingJobLedger.Stage.CRAFTING) {
                            job.finish(CraftingJobLedger.Stage.CANCELLED);
                        }
                        continue;
                    }
                    session = StructureCraftingSession.restore(server, job, grid);
                    SESSIONS.put(job.id, session);
                }
                session.closeSubmittedConfirmation(player);
                if ((!job.active() || !session.tick(server, player)) && session.close(server, player)) {
                    SESSIONS.remove(job.id);
                }
            } catch (RuntimeException exception) {
                if (job.active()) job.display = "disconnected";
                if (server.getTickCount() % 200 == 0) {
                    Mbtool.logger.warn("Could not update structure crafting request {}", job.id, exception);
                }
            }
        }
    }

    public static void shutdown(MinecraftServer server) {
        for (StructureCraftingSession session : SESSIONS.values()) {
            try {
                session.suspend(server); // Save the provider and its inputs; do not cancel AE2 work.
            } catch (RuntimeException e) {
                Mbtool.logger.error("Failed to save structure crafting request", e);
            }
        }
        SESSIONS.clear();
    }
}
