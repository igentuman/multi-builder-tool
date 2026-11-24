package igentuman.mbtool.integration.ae2;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.network.SyncCraftingJobsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static igentuman.mbtool.integration.ae2.CraftingJobLedger.Job;
import static igentuman.mbtool.integration.ae2.CraftingJobLedger.Stage;
import static igentuman.mbtool.integration.ae2.CraftingJobLedger.get;

public final class CraftingJobTracker {
    private CraftingJobTracker() {}

    static CompoundTag location(ServerLevel level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", level.dimension().identifier().toString());
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
        Identifier id = Identifier.tryParse(location.getStringOr("dimension", ""));
        if (id == null) return null;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        return level != null && level.hasChunkAt(BlockPos.of(location.getLongOr("pos", 0L))) ? level : null;
    }

    static IGrid resolveGrid(MinecraftServer server, Job job) {
        ServerLevel cpuLevel = loadedLevel(server, job.cpu);
        if (cpuLevel != null && cpuLevel.getBlockEntity(BlockPos.of(job.cpu.getLongOr("pos", 0L)))
                instanceof CraftingBlockEntity entity && entity.getCluster() != null) {
            return entity.getCluster().getGrid();
        }
        ServerLevel level = loadedLevel(server, job.network);
        if (level != null) {
            IInWorldGridNodeHost host = GridHelper.getNodeHost(level, BlockPos.of(job.network.getLongOr("pos", 0L)));
            if (host != null) for (Direction side : Direction.values()) {
                IGridNode node = host.getGridNode(side);
                if (node != null) return node.getGrid();
            }
        }
        return job.grid != null && !job.grid.isEmpty() ? job.grid : null;
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
        BlockEntity entity = level.getBlockEntity(BlockPos.of(job.cpu.getLongOr("pos", 0L)));
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
        // An unloaded or stalled network must never be mistaken for a successful completion.
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

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        CraftingJobLedger ledger = get(server);
        for (Job job : List.copyOf(ledger.jobs.values())) {
            if (!job.active()) continue;
            try {
                if (job.stage == Stage.CRAFTING) pollCrafting(server, job);
                else if (job.session.isEmpty()) job.finish(Stage.CANCELLED); // pre-submit request lost its provider
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
        CraftingJobLedger ledger = get(player.level().getServer());
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
        get(player.level().getServer()).jobs.values().stream()
                .filter(job -> job.owner.equals(player.getUUID()) && !job.expired(now)).forEach(job -> {
            for (String structure : job.structures) {
                CompoundTag tag = new CompoundTag();
                ListTag structures = new ListTag();
                structures.add(StringTag.valueOf(structure));
                tag.put("structures", structures);
                tag.putString("state", job.display);
                tag.putBoolean("active", job.active());
                tag.putLong("total", job.total);
                tag.putLong("progress", job.progress);
                tag.putLong("elapsed", Math.max(0, (job.active() ? now : job.ended) - job.started));
                long eta = job.progress > 0 && job.display.equals("crafting")
                        ? (long) ((double) job.elapsed * (job.total - job.progress) / job.progress) : -1;
                tag.putLong("eta", eta);
                tag.putLong("started", job.started);
                tag.store("batch", UUIDUtil.CODEC, job.batches.getOrDefault(structure, job.id));
                tag.putLong("batchStarted", job.batchStarts.getOrDefault(structure, job.started));
                jobs.add(tag);
            }
        });
        CompoundTag payload = new CompoundTag();
        payload.put("jobs", jobs);
        PacketDistributor.sendToPlayer(player, new SyncCraftingJobsPacket(payload));
    }
}
