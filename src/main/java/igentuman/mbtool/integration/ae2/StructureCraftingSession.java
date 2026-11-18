package igentuman.mbtool.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.StructureRequestItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

final class StructureCraftingSession implements IActionHost {
    private enum State { CONNECTING, CONFIRMING, CRAFTING }

    private static final int CONNECT_TIMEOUT_TICKS = 100;
    private static final int MENU_GRACE_TICKS = 20;
    private static final int MISSING_JOB_GRACE_TICKS = 40;
    private static final int SINK_PRIORITY = Integer.MAX_VALUE - 1;
    private static final IGridNodeListener<StructureCraftingSession> LISTENER = (owner, node) -> {};

    final String structureName;
    private final AEItemKey resultKey;
    private final StructureCraftingPattern pattern;
    private final IManagedGridNode node;
    private final KeyCounter returned = new KeyCounter();
    private long resultsOwed;
    private boolean delivering;
    private boolean finished;
    private State state = State.CONNECTING;
    private int stateTicks;
    private int missingJobTicks;

    StructureCraftingSession(ServerPlayer player, IGrid grid, String structureName,
                             List<StructureCraftingPattern.Input> inputs, int requestId) {
        this.structureName = structureName;
        UUID playerId = player.getUUID();
        CompoundTag tag = new CompoundTag();
        tag.putString(StructureRequestItem.TAG_STRUCTURE, structureName);
        tag.putUUID("owner", playerId);
        tag.putInt("request", requestId);
        resultKey = AEItemKey.of(Mbtool.STRUCTURE_REQUEST.get(), tag.copy());
        tag.putBoolean("pattern", true);
        pattern = new StructureCraftingPattern(AEItemKey.of(Mbtool.STRUCTURE_REQUEST.get(), tag), resultKey, inputs);

        ResultSink sink = new ResultSink();
        node = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(false)
                .setIdlePowerUsage(0)
                .setTagName("mbtool_structure_crafting")
                .setVisualRepresentation(Mbtool.MBTOOL.get())
                .addService(ICraftingProvider.class, new Provider())
                .addService(IStorageProvider.class, sink);
        node.setOwningPlayer(player);
        node.create(player.level(), null);
        if (!connect(grid)) {
            node.destroy();
            throw new IllegalStateException("Could not attach structure crafting node to the ME network");
        }
    }

    boolean isCrafting() {
        return state == State.CRAFTING;
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return node.getNode();
    }

    private @Nullable IGrid grid() {
        IGridNode own = node.getNode();
        return own == null ? null : own.getGrid();
    }

    private boolean connect(@Nullable IGrid target) {
        IGridNode own = node.getNode();
        IGridNode pivot = target == null ? null : target.getPivot();
        if (own == null || pivot == null || pivot == own) return false;
        if (own.getGrid() == target) return true;
        try {
            GridHelper.createConnection(own, pivot);
            return true;
        } catch (RuntimeException e) {
            Mbtool.logger.warn("Failed to connect structure crafting node", e);
            return false;
        }
    }

    boolean tick(ServerPlayer player) {
        stateTicks++;
        AE2Helper.GridContext terminal = AE2Helper.getPlayerGridContext(player);
        if (terminal != null && terminal.grid() != grid()) connect(terminal.grid());
        IGrid grid = grid();
        if (grid == null) return false;

        flushReturned(grid);
        deliverResults(grid);
        if (finished) {
            message(player, "message.mbtool.structure_craft.done");
            return false;
        }

        switch (state) {
            case CONNECTING -> {
                if (grid.getCraftingService().isCraftable(resultKey)) {
                    if (terminal == null || terminal.slot() < 0 || !openConfirmMenu(player, terminal.slot())) {
                        message(player, "message.mbtool.structure_craft.menu_failed");
                        return false;
                    }
                    enter(State.CONFIRMING);
                } else if (stateTicks > CONNECT_TIMEOUT_TICKS) {
                    message(player, "message.mbtool.structure_craft.pattern_timeout");
                    return false;
                }
            }
            case CONFIRMING -> {
                if (findCpu(grid) != null) {
                    enter(State.CRAFTING);
                } else if (!isCraftMenuOpen(player) && stateTicks > MENU_GRACE_TICKS) {
                    return false; // backed out of the confirm screen
                }
            }
            case CRAFTING -> {
                if (findCpu(grid) != null) {
                    missingJobTicks = 0;
                } else if (++missingJobTicks > MISSING_JOB_GRACE_TICKS) {
                    message(player, "message.mbtool.structure_craft.cancelled");
                    return false;
                }
            }
        }
        return true;
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
    }

    private boolean openConfirmMenu(ServerPlayer player, int terminalSlot) {
        CraftAmountMenu.open(player, MenuLocators.forInventorySlot(terminalSlot), resultKey, 1);
        if (!(player.containerMenu instanceof CraftAmountMenu amountMenu)) return false;
        amountMenu.confirm(1, false, false);
        return player.containerMenu instanceof CraftConfirmMenu;
    }

    private static boolean isCraftMenuOpen(ServerPlayer player) {
        return player.containerMenu instanceof CraftConfirmMenu || player.containerMenu instanceof CraftAmountMenu;
    }

    private @Nullable ICraftingCPU findCpu(IGrid grid) {
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            CraftingJobStatus status = cpu.getJobStatus();
            if (status != null && resultKey.equals(status.crafting().what())) return cpu;
        }
        return null;
    }

    private void flushReturned(IGrid grid) {
        if (returned.isEmpty()) return;
        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = IActionSource.ofMachine(this);
        for (AEKey key : List.copyOf(returned.keySet())) {
            long amount = returned.get(key);
            if (amount <= 0) continue;
            returned.remove(key, storage.insert(key, amount, Actionable.MODULATE, source));
        }
        returned.removeZeros();
    }

    private void deliverResults(IGrid grid) {
        if (resultsOwed <= 0 || !returned.isEmpty()) return;
        delivering = true;
        try {
            resultsOwed -= grid.getStorageService().getInventory()
                    .insert(resultKey, resultsOwed, Actionable.MODULATE, IActionSource.ofMachine(this));
        } finally {
            delivering = false;
        }
        if (resultsOwed > 0 && findCpu(grid) == null) resultsOwed = 0; // job is gone, nobody to deliver to
    }

    void close(@Nullable ServerPlayer player) {
        IGrid grid = grid();
        if (grid != null) {
            ICraftingCPU cpu = findCpu(grid);
            if (cpu != null) cpu.cancelJob();
            flushReturned(grid);
        }
        if (player != null) {
            for (AEKey key : returned.keySet()) {
                if (!(key instanceof AEItemKey item)) continue;
                long amount = returned.get(key);
                while (amount > 0) {
                    int count = (int) Math.min(amount, item.getItem().getMaxStackSize());
                    player.getInventory().placeItemBackInInventory(item.toStack(count));
                    amount -= count;
                }
            }
        }
        returned.clear();
        node.destroy();
    }

    private void message(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key, StructureRequestItem.structureName(structureName)));
    }

    private final class Provider implements ICraftingProvider {
        @Override
        public List<IPatternDetails> getAvailablePatterns() {
            return List.of(pattern);
        }

        @Override
        public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
            if (!pattern.equals(details)) return false;
            for (KeyCounter input : inputs) returned.addAll(input);
            resultsOwed++;
            return true;
        }

        @Override
        public boolean isBusy() {
            return false;
        }
    }

    private final class ResultSink implements IStorageProvider, MEStorage {
        @Override
        public void mountInventories(IStorageMounts mounts) {
            mounts.mount(this, SINK_PRIORITY);
        }

        @Override
        public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
            return resultKey.equals(what);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (delivering || !resultKey.equals(what)) return 0;
            if (mode == Actionable.MODULATE) finished = true;
            return amount;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("item.mbtool.structure_request");
        }
    }
}
