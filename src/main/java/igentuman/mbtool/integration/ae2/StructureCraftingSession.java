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
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.StructureRequestItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class StructureCraftingSession implements IActionHost {
    private enum State { CONNECTING, CONFIRMING, CRAFTING }

    private static final int CONNECT_TIMEOUT_TICKS = 100;
    private static final int MENU_GRACE_TICKS = 20;
    private static final IGridNodeListener<StructureCraftingSession> LISTENER = (owner, node) -> {};

    final String structureName;
    private final CraftingJobLedger.Job job;
    private final List<StructureCraftingPattern.Input> inputs;
    private final AEItemKey resultKey;
    private final StructureCraftingPattern pattern;
    private final IManagedGridNode node;
    private final KeyCounter returned = new KeyCounter();
    private long resultsOwed;
    private State state = State.CONNECTING;
    private net.minecraft.world.inventory.AbstractContainerMenu confirmationMenu;
    private int stateTicks;

    StructureCraftingSession(ServerPlayer player, IGrid grid, String structureName,
                             List<StructureCraftingPattern.Input> inputs, CraftingJobLedger.Job job) {
        this(player.server, job, structureName, inputs, grid, player);
    }

    private StructureCraftingSession(MinecraftServer server, CraftingJobLedger.Job job, String structureName,
                                     List<StructureCraftingPattern.Input> inputs, IGrid grid,
                                     @Nullable ServerPlayer player) {
        this.job = job;
        this.structureName = structureName;
        this.inputs = inputs;
        resultKey = (AEItemKey) job.output;
        CompoundTag definition = resultKey.getTag().copy();
        definition.putBoolean("pattern", true);
        pattern = new StructureCraftingPattern(AEItemKey.of(Mbtool.STRUCTURE_REQUEST.get(), definition), resultKey, inputs);
        node = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(false)
                .setIdlePowerUsage(0)
                .setTagName("mbtool_structure_crafting")
                .setVisualRepresentation(Mbtool.MBTOOL.get())
                .addService(ICraftingProvider.class, new Provider());
        if (job.session.contains("node")) node.loadFromNBT(job.session.getCompound("node"));
        if (player != null) node.setOwningPlayer(player);
        ServerLevel level = CraftingJobTracker.loadedLevel(server, job.network);
        if (level == null && player != null) level = player.serverLevel();
        if (level == null) throw new IllegalStateException("Original ME network is not loaded");
        node.create(level, null);
        if (job.session.contains("state")) {
            state = State.valueOf(job.session.getString("state"));
            resultsOwed = job.session.getLong("resultsOwed");
            for (Tag entry : job.session.getList("returned", Tag.TAG_COMPOUND)) {
                GenericStack stack = GenericStack.readTag((CompoundTag) entry);
                if (stack != null) returned.add(stack.what(), stack.amount());
            }
        }
        if (job.stage == CraftingJobLedger.Stage.CRAFTING) state = State.CRAFTING;
        connect(grid);
        checkpoint(server);
    }

    static StructureCraftingSession restore(MinecraftServer server, CraftingJobLedger.Job job, IGrid grid) {
        List<StructureCraftingPattern.Input> inputs = new ArrayList<>();
        for (Tag entry : job.session.getList("inputs", Tag.TAG_COMPOUND)) {
            CompoundTag input = (CompoundTag) entry;
            List<AEItemKey> candidates = new ArrayList<>();
            for (Tag key : input.getList("keys", Tag.TAG_COMPOUND)) {
                AEKey decoded = AEKey.fromTagGeneric((CompoundTag) key);
                if (decoded instanceof AEItemKey item) candidates.add(item);
            }
            if (!candidates.isEmpty()) inputs.add(new StructureCraftingPattern.Input(candidates, input.getLong("amount")));
        }
        return new StructureCraftingSession(server, job, job.structures.iterator().next(), inputs, grid,
                server.getPlayerList().getPlayer(job.owner));
    }

    private void checkpoint(MinecraftServer server) {
        CompoundTag tag = new CompoundTag();
        tag.putString("state", state.name());
        tag.putLong("resultsOwed", resultsOwed);
        CompoundTag nodeTag = new CompoundTag();
        node.saveToNBT(nodeTag);
        tag.put("node", nodeTag);
        ListTag pending = new ListTag();
        for (AEKey key : returned.keySet()) pending.add(GenericStack.writeTag(new GenericStack(key, returned.get(key))));
        tag.put("returned", pending);
        ListTag inputTags = new ListTag();
        for (StructureCraftingPattern.Input input : inputs) {
            CompoundTag inputTag = new CompoundTag();
            inputTag.putLong("amount", input.getMultiplier());
            ListTag keys = new ListTag();
            for (GenericStack key : input.getPossibleInputs()) keys.add(key.what().toTagGeneric());
            inputTag.put("keys", keys);
            inputTags.add(inputTag);
        }
        tag.put("inputs", inputTags);
        job.session = tag;
        CraftingJobLedger.get(server).setDirty();
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
        if (!own.getConnections().isEmpty()) return false;
        try {
            GridHelper.createConnection(own, pivot);
            return true;
        } catch (RuntimeException e) {
            Mbtool.logger.warn("Failed to connect structure crafting node", e);
            return false;
        }
    }

    boolean tick(MinecraftServer server, @Nullable ServerPlayer player) {
        stateTicks++;
        if (job.stage == CraftingJobLedger.Stage.CRAFTING && state != State.CRAFTING) enter(State.CRAFTING);
        if (job.stage != CraftingJobLedger.Stage.CRAFTING && (job.cancelRequested
                || state == State.CONFIRMING && stateTicks > MENU_GRACE_TICKS
                && (player == null || !isCraftMenuOpen(player)))) {
            if (player != null && isCraftMenuOpen(player)) player.closeContainer();
            job.finish(CraftingJobLedger.Stage.CANCELLED);
            checkpoint(server);
            return false;
        }
        if (state == State.CONNECTING && stateTicks > CONNECT_TIMEOUT_TICKS) {
            job.finish(CraftingJobLedger.Stage.FAILED);
            checkpoint(server);
            return false;
        }
        IGrid original = CraftingJobTracker.resolveGrid(server, job);
        if (original != null) connect(original);
        IGrid grid = grid();
        if (grid == null || original == null || grid != original || !node.isActive()) {
            job.display = job.cancelRequested ? "cancelling" : "disconnected";
            checkpoint(server);
            return job.active();
        }
        flushReturned(grid);
        deliverResults(grid);
        if (job.stage == CraftingJobLedger.Stage.COMPLETED) {
            if (player != null) message(player, "message.mbtool.structure_craft.done");
        }
        if (!job.active()) { checkpoint(server); return false; }

        ICraftingCPU cpu = findCpu(grid);
        if (cpu != null) {
            CraftingJobTracker.bind(job, cpu);
            if (state != State.CRAFTING) enter(State.CRAFTING);
            job.stage = CraftingJobLedger.Stage.CRAFTING;
        }
        if (job.cancelRequested && state != State.CRAFTING) {
            if (player != null && isCraftMenuOpen(player)) player.closeContainer();
            job.finish(CraftingJobLedger.Stage.CANCELLED);
        } else switch (state) {
            case CONNECTING -> {
                job.display = "connecting";
                if (player != null && grid.getCraftingService().isCraftable(resultKey)) {
                    AE2Helper.GridContext terminal = AE2Helper.getPlayerGridContext(player);
                    if (terminal == null || terminal.grid() != grid || terminal.slot() < 0
                            || !openConfirmMenu(player, terminal.slot())) {
                        message(player, "message.mbtool.structure_craft.menu_failed");
                        job.finish(CraftingJobLedger.Stage.FAILED);
                    } else {
                        enter(State.CONFIRMING);
                        job.stage = CraftingJobLedger.Stage.CONFIRMING;
                    }
                } else if (stateTicks > CONNECT_TIMEOUT_TICKS) job.finish(CraftingJobLedger.Stage.FAILED);
            }
            case CONFIRMING -> {
                job.display = "confirming";
                if ((player == null || !isCraftMenuOpen(player)) && stateTicks > MENU_GRACE_TICKS) {
                    job.finish(CraftingJobLedger.Stage.CANCELLED);
                }
            }
            case CRAFTING -> {} // The shared tracker polls the original CPU independently of this provider.
        }
        checkpoint(server);
        return job.active();
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
    }

    private boolean openConfirmMenu(ServerPlayer player, int terminalSlot) {
        CraftAmountMenu.open(player, MenuLocators.forInventorySlot(terminalSlot), resultKey, 1);
        if (!(player.containerMenu instanceof CraftAmountMenu amountMenu)) return false;
        amountMenu.confirm(1, false, false);
        if (!(player.containerMenu instanceof CraftConfirmMenu)) return false;
        confirmationMenu = player.containerMenu;
        return true;
    }

    private boolean isCraftMenuOpen(ServerPlayer player) {
        return confirmationMenu != null && player.containerMenu == confirmationMenu;
    }

    void closeSubmittedConfirmation(@Nullable ServerPlayer player) {
        if (job.craftingId != null && player != null && isCraftMenuOpen(player)) {
            player.closeContainer();
            confirmationMenu = null;
        }
    }

    private @Nullable ICraftingCPU findCpu(IGrid grid) {
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            CraftingJobStatus status = cpu.getJobStatus();
            if (status != null && resultKey.equals(status.crafting().what())
                    && CraftingJobTracker.bind(job, cpu)) return cpu;
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
        ICraftingCPU cpu = findCpu(grid);
        if (cpu instanceof CraftingCPUCluster cluster) {
            resultsOwed -= StructureCraftingOutput.deliver(cluster.craftingLogic, resultKey, resultsOwed);
        } else if (cpu == null) {
            resultsOwed = 0;
        }
    }

    boolean close(MinecraftServer server, @Nullable ServerPlayer player) {
        IGrid grid = grid();
        if (grid != null) flushReturned(grid);
        if (player != null) {
            for (AEKey key : List.copyOf(returned.keySet())) {
                if (!(key instanceof AEItemKey item)) continue;
                long amount = returned.get(key);
                while (amount > 0) {
                    int count = (int) Math.min(amount, item.getItem().getMaxStackSize());
                    player.getInventory().placeItemBackInInventory(item.toStack(count));
                    amount -= count;
                }
                returned.remove(key, returned.get(key));
            }
            returned.removeZeros();
        }
        checkpoint(server);
        if (!returned.isEmpty()) return false;
        node.destroy();
        job.session = new CompoundTag();
        CraftingJobLedger.get(server).setDirty();
        return true;
    }

    void suspend(MinecraftServer server) {
        checkpoint(server);
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
            checkpoint(node.getNode().getLevel().getServer());
            return true;
        }

        @Override
        public boolean isBusy() {
            return false;
        }
    }

}
