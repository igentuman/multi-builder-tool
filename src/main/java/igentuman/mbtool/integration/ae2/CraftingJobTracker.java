package igentuman.mbtool.integration.ae2;

import appeng.api.networking.*;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.SyncCraftingJobsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static igentuman.mbtool.integration.ae2.CraftingJobLedger.*;

public final class CraftingJobTracker {
    private CraftingJobTracker() {}

    static CompoundTag location(ServerLevel level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", level.dimension().location().toString());
        tag.putLong("pos", pos.asLong());
        return tag;
    }

    static CompoundTag networkLocation(IGrid grid) {
        for (IGridNode node : grid.getNodes()) {
            if (node.getOwner() instanceof BlockEntity entity && entity.getLevel() instanceof ServerLevel level) {
                return location(level, entity.getBlockPos());
            }
        }
        return new CompoundTag();
    }

    static ServerLevel loadedLevel(MinecraftServer server, CompoundTag location) {
        if (!location.contains("dimension")) return null;
        ResourceLocation id = ResourceLocation.tryParse(location.getString("dimension"));
        if (id == null) return null;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        return level != null && level.hasChunkAt(BlockPos.of(location.getLong("pos"))) ? level : null;
    }

    static IGrid resolveGrid(MinecraftServer server, Job job) {
        ServerLevel cpuLevel = loadedLevel(server, job.cpu);
        if (cpuLevel != null && cpuLevel.getBlockEntity(BlockPos.of(job.cpu.getLong("pos")))
                instanceof CraftingBlockEntity entity && entity.getCluster() != null) {
            return entity.getCluster().getGrid();
        }
        ServerLevel level = loadedLevel(server, job.network);
        if (level != null) {
            IInWorldGridNodeHost host = GridHelper.getNodeHost(level, BlockPos.of(job.network.getLong("pos")));
            if (host != null) for (Direction side : Direction.values()) {
                IGridNode node = host.getGridNode(side);
                if (node != null) return node.getGrid();
            }
        }
        return job.grid != null && !job.grid.isEmpty() ? job.grid : null;
    }

    public static @Nullable UUID beginBatch(ServerPlayer player, String structure) {
        return get(player.server).beginBatch(player.getUUID(), structure);
    }

    public static AE2Helper.AutocraftResult request(ServerPlayer player, Block block, int amount, String structure) {
        return request(player, block, amount, structure, beginBatch(player, structure));
    }

    public static AE2Helper.AutocraftResult request(ServerPlayer player, Block block, int amount, String structure, @Nullable UUID batch) {
        CraftingJobLedger ledger = get(player.server);
        if (!ledger.acceptsBatch(player.getUUID(), structure, batch)) {
            return AE2Helper.AutocraftResult.ALREADY_REQUESTING;
        }
        AEItemKey key = AEItemKey.of(block.asItem());
        Job existing = ledger.active(player.getUUID(), structure, key);
        if (existing != null) {
            ledger.associate(existing, structure, batch);
            return existing.stage == Stage.CRAFTING ? AE2Helper.AutocraftResult.ALREADY_REQUESTING
                    : AE2Helper.AutocraftResult.CALCULATING;
        }
        AE2Helper.GridContext ctx = AE2Helper.getPlayerGridContext(player);
        if (ctx == null) return AE2Helper.AutocraftResult.NO_GRID;
        if (!ctx.grid().getCraftingService().isCraftable(key)) return AE2Helper.AutocraftResult.NO_PATTERN;
        Job job = new Job();
        job.owner = player.getUUID();
        job.output = key;
        job.amount = amount;
        ledger.associate(job, structure, batch);
        job.grid = ctx.grid();
        job.network = networkLocation(ctx.grid());
        ledger.add(job);
        for (ICraftingCPU cpu : ctx.grid().getCraftingService().getCpus()) {
            CraftingJobStatus status = cpu.getJobStatus();
            ICraftingLink link = cpu instanceof CraftingCPUCluster cluster ? cluster.craftingLogic.getLastLink() : null;
            if (status != null && status.crafting().what().equals(key)
                    && !ledger.tracksCraftingId(link == null ? null : link.getCraftingID()) && bind(job, cpu)) {
                job.stage = Stage.CRAFTING;
                job.owned = false;
                return AE2Helper.AutocraftResult.ALREADY_REQUESTING;
            }
        }
        return calculate(player, job, ctx);
    }

    private static AE2Helper.AutocraftResult calculate(ServerPlayer player, Job job, AE2Helper.GridContext ctx) {
        try {
            job.stage = Stage.CALCULATING;
            job.calculationStarted = System.currentTimeMillis();
            IActionSource source = ctx.actionHost() != null ? IActionSource.ofPlayer(player, ctx.actionHost())
                    : IActionSource.ofPlayer(player);
            job.future = ctx.grid().getCraftingService().beginCraftingCalculation(player.level(), () -> source,
                    job.output, job.amount, CalculationStrategy.REPORT_MISSING_ITEMS);
            job.display = "calculating";
            return AE2Helper.AutocraftResult.CALCULATING;
        } catch (RuntimeException exception) {
            job.finish(Stage.FAILED);
            Mbtool.logger.warn("Failed to calculate MBTool crafting request {}", job.id, exception);
            return AE2Helper.AutocraftResult.FAILED;
        }
    }

    static boolean bind(Job job, ICraftingCPU cpu) {
        if (!(cpu instanceof CraftingCPUCluster cluster)) return false;
        ICraftingLink link = cluster.craftingLogic.getLastLink();
        if (link == null) return false;
        if (job.craftingId != null && !job.craftingId.equals(link.getCraftingID())) return false;
        job.craftingId = link.getCraftingID();
        job.link = link;
        job.grid = cluster.getGrid();
        var entities = cluster.getBlockEntities();
        if (entities.hasNext() && cluster.getLevel() instanceof ServerLevel level) {
            job.cpu = location(level, entities.next().getBlockPos());
            job.network = job.cpu.copy();
        }
        return true;
    }

    static void updateProgress(Job job, CraftingJobStatus status) {
        if (status.progress() != job.progress) job.lastProgressAt = System.currentTimeMillis();
        job.total = Math.max(0, status.totalItems());
        job.progress = Math.max(0, Math.min(job.total, status.progress()));
        job.elapsed = Math.max(0, status.elapsedTimeNanos() / 1_000_000);
        job.missingPolls = 0;
        job.display = job.cancelRequested ? "cancelling" :
                System.currentTimeMillis() - job.lastProgressAt > 60_000 ? "stalled" : "crafting";
    }

    public static void recordFinished(MinecraftServer server, CraftingCPUCluster cpu, boolean successful) {
        ICraftingLink link = cpu.craftingLogic.getLastLink();
        if (link == null) return;
        CraftingJobLedger ledger = get(server);
        for (Job job : ledger.jobs.values()) {
            if (job.stage != Stage.CRAFTING || !link.getCraftingID().equals(job.craftingId)) continue;
            CraftingJobStatus status = cpu.getJobStatus();
            if (status != null) updateProgress(job, status);
            job.finish(successful ? Stage.COMPLETED : Stage.CANCELLED);
            ledger.setDirty();
        }
    }

    static void pollCrafting(MinecraftServer server, Job job) {
        if (job.link != null && job.link.isDone()) { job.finish(Stage.COMPLETED); return; }
        if (job.link != null && job.link.isCanceled()) { job.finish(Stage.CANCELLED); return; }
        if (job.cancelRequested && !job.owned) { job.finish(Stage.CANCELLED); return; }
        if (pollGrid(job, resolveGrid(server, job))) return;
        ServerLevel level = loadedLevel(server, job.cpu);
        if (level == null) { pollCpu(job, false, null); return; }
        BlockEntity entity = level.getBlockEntity(BlockPos.of(job.cpu.getLong("pos")));
        CraftingCPUCluster cluster = entity instanceof CraftingBlockEntity crafting ? crafting.getCluster() : null;
        pollCpu(job, true, cluster);
    }

    static boolean pollGrid(Job job, IGrid grid) {
        if (grid == null || grid.getPivot() == null || !grid.getPivot().isActive()) return false;
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            if (cpu.getJobStatus() != null && bind(job, cpu)) {
                pollCpu(job, true, (CraftingCPUCluster) cpu);
                return true;
            }
        }
        job.finish(Stage.REMOVED);
        return true;
    }

    static void pollCpu(Job job, boolean loaded, CraftingCPUCluster cluster) {
        if (!loaded) { job.display = job.cancelRequested ? "cancelling" : "disconnected"; return; }
        if (cluster != null && bind(job, cluster)) {
            if (job.link.isDone()) { job.finish(Stage.COMPLETED); return; }
            if (job.link.isCanceled()) { job.finish(Stage.CANCELLED); return; }
            if (job.cancelRequested) {
                cluster.cancelJob();
                job.finish(Stage.CANCELLED);
                return;
            }
            if (!cluster.isActive()) { job.display = "disconnected"; return; }
            CraftingJobStatus status = cluster.getJobStatus();
            if (status != null) { updateProgress(job, status); return; }
        }
        job.display = "disconnected";
        if (++job.missingPolls >= 10) job.finish(Stage.REMOVED);
    }

    private static void tickCalculation(MinecraftServer server, Job job) throws Exception {
        ServerPlayer player = server.getPlayerList().getPlayer(job.owner);
        if (job.cancelRequested) { job.finish(Stage.CANCELLED); return; }
        if (job.stage == Stage.CALCULATING && System.currentTimeMillis() - job.calculationStarted > 120_000) { job.finish(Stage.FAILED); return; }
        IGrid original = resolveGrid(server, job);
        AE2Helper.GridContext ctx = player == null ? null : AE2Helper.getPlayerGridContext(player);
        if (ctx == null || ctx.grid() != original || ctx.grid().getPivot() == null
                || !ctx.grid().getPivot().isActive()) {
            job.display = "disconnected";
            return;
        }
        if (job.future == null) { calculate(player, job, ctx); return; }
        if (!job.future.isDone()) { job.display = "calculating"; return; }
        for (ICraftingCPU cpu : original.getCraftingService().getCpus()) {
            CraftingJobStatus status = cpu.getJobStatus();
            if (status != null && status.crafting().what().equals(job.output) && bind(job, cpu)) {
                job.stage = Stage.CRAFTING;
                job.owned = false;
                job.future.cancel(true);
                job.future = null;
                return;
            }
        }
        ICraftingPlan plan = job.future.get();
        if (plan.simulation()) { job.finish(Stage.FAILED); return; }
        IActionSource source = ctx.actionHost() == null ? IActionSource.ofPlayer(player)
                : IActionSource.ofPlayer(player, ctx.actionHost());
        ICraftingSubmitResult result = original.getCraftingService().submitJob(plan, null, null, false, source);
        if (result.successful()) {
            job.link = result.link();
            job.craftingId = job.link.getCraftingID();
            job.stage = Stage.CRAFTING;
            job.display = "crafting";
            job.future = null;
            for (ICraftingCPU cpu : original.getCraftingService().getCpus()) if (bind(job, cpu)) break;
        } else if (AE2Helper.isCpuUnavailable(result.errorCode())) {
            job.stage = Stage.WAITING_FOR_CPU;
            job.display = "waiting_for_cpu";
        } else job.finish(Stage.FAILED);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        CraftingJobLedger ledger = get(server);
        for (Job job : List.copyOf(ledger.jobs.values())) {
            if (!job.active()) continue;
            try {
                if (job.stage == Stage.CRAFTING) pollCrafting(server, job);
                else if (job.session.isEmpty()) tickCalculation(server, job);
                job.errorPolls = 0;
            } catch (Exception exception) {
                job.display = "disconnected";
                if (++job.errorPolls == 1) Mbtool.logger.warn("Cannot poll crafting request {}", job.id, exception);
                if (job.stage != Stage.CRAFTING && job.errorPolls >= 5) job.finish(Stage.FAILED);
            }
        }
        ledger.removeFinishedCrafts();
        ledger.setDirty();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }

    public static void cancel(ServerPlayer player, String structure) {
        CraftingJobLedger ledger = get(player.server);
        for (Job job : ledger.jobs.values()) {
            if (job.owner.equals(player.getUUID()) && job.active() && job.structures.contains(structure)) {
                job.cancelRequested = true;
                job.display = "cancelling";
                if (job.session.isEmpty() && job.stage != Stage.CRAFTING) job.finish(Stage.CANCELLED);
            }
        }
        ledger.setDirty();
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ListTag jobs = new ListTag();
        long now = System.currentTimeMillis();
        get(player.server).jobs.values().stream()
                .filter(job -> job.owner.equals(player.getUUID()) && !job.expired(now)).forEach(job -> {
            for (String structure : job.structures) {
                CompoundTag tag = new CompoundTag();
                ListTag structures = new ListTag();
                structures.add(net.minecraft.nbt.StringTag.valueOf(structure));
                tag.put("structures", structures);
                tag.putString("state", job.display);
                tag.putBoolean("active", job.active());
                tag.putLong("total", job.total);
                tag.putLong("progress", job.progress);
                tag.putLong("elapsed", Math.max(0, (job.active() ? System.currentTimeMillis() : job.ended) - job.started));
                long eta = job.progress > 0 && job.display.equals("crafting")
                        ? (long) ((double) job.elapsed * (job.total - job.progress) / job.progress) : -1;
                tag.putLong("eta", eta);
                tag.putLong("started", job.started);
                tag.putUUID("batch", job.batches.getOrDefault(structure, job.id));
                tag.putLong("batchStarted", job.batchStarts.getOrDefault(structure, job.started));
                jobs.add(tag);
            }
        });
        CompoundTag payload = new CompoundTag();
        payload.put("jobs", jobs);
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncCraftingJobsPacket(payload));
    }
}
