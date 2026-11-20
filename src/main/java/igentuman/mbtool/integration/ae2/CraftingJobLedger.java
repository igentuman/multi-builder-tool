package igentuman.mbtool.integration.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Future;

public final class CraftingJobLedger extends SavedData {
    private static final long OUTCOME_DISPLAY_MILLIS = 5_000;
    final Map<UUID, Job> jobs = new LinkedHashMap<>();

    public enum Stage {
        CALCULATING, WAITING_FOR_CPU, CONFIRMING, CRAFTING, COMPLETED, CANCELLED, REMOVED, FAILED;

        public boolean active() { return ordinal() < COMPLETED.ordinal(); }
    }

    static final class Job {
        UUID id = UUID.randomUUID();
        UUID owner;
        final Set<String> structures = new LinkedHashSet<>();
        final Map<String, UUID> batches = new LinkedHashMap<>();
        final Map<String, Long> batchStarts = new LinkedHashMap<>();
        final List<GenericStack> coverage = new ArrayList<>();
        AEKey output;
        long amount;
        Stage stage = Stage.CALCULATING;
        String display = "calculating";
        long started = System.currentTimeMillis();
        long lastProgressAt = started;
        long total, progress, elapsed;
        long ended;
        boolean cancelRequested;
        boolean owned = true;
        UUID craftingId;
        CompoundTag network = new CompoundTag();
        CompoundTag cpu = new CompoundTag();
        CompoundTag session = new CompoundTag();
        transient Future<ICraftingPlan> future;
        transient ICraftingLink link;
        transient IGrid grid;
        transient long calculationStarted = System.currentTimeMillis();
        transient int missingPolls;
        transient int errorPolls;

        boolean active() { return stage.active(); }

        boolean finishedCraft() { return !active() && (craftingId != null || stage == Stage.REMOVED); }

        boolean expired(long now) {
            return finishedCraft() || !active() && now - ended >= OUTCOME_DISPLAY_MILLIS;
        }

        boolean covers(AEKey key) {
            return output.equals(key) || coverage.stream().anyMatch(stack -> stack.what().equals(key));
        }

        void finish(Stage result) {
            stage = result;
            display = result.name().toLowerCase(Locale.ROOT);
            ended = System.currentTimeMillis();
            if (future != null) future.cancel(true);
            future = null;
            link = null;
            grid = null;
            if (result == Stage.COMPLETED) progress = total = Math.max(1, total);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putUUID("owner", owner);
            ListTag names = new ListTag();
            structures.forEach(name -> names.add(net.minecraft.nbt.StringTag.valueOf(name)));
            tag.put("structures", names);
            CompoundTag groups = new CompoundTag();
            for (String name : structures) {
                CompoundTag group = new CompoundTag();
                group.putUUID("id", batches.getOrDefault(name, id));
                group.putLong("started", batchStarts.getOrDefault(name, started));
                groups.put(name, group);
            }
            tag.put("batches", groups);
            ListTag inputs = new ListTag();
            coverage.forEach(stack -> inputs.add(GenericStack.writeTag(stack)));
            tag.put("coverage", inputs);
            tag.put("output", output.toTagGeneric());
            tag.putLong("amount", amount);
            tag.putString("stage", stage.name());
            tag.putString("display", display);
            tag.putLong("started", started);
            tag.putLong("lastProgressAt", lastProgressAt);
            tag.putLong("total", total);
            tag.putLong("progress", progress);
            tag.putLong("elapsed", elapsed);
            tag.putLong("ended", ended);
            tag.putBoolean("cancelRequested", cancelRequested);
            tag.putBoolean("owned", owned);
            if (craftingId != null) tag.putUUID("craftingId", craftingId);
            tag.put("network", network.copy());
            tag.put("cpu", cpu.copy());
            tag.put("session", session.copy());
            return tag;
        }

        static Job load(CompoundTag tag) {
            Job job = new Job();
            job.id = tag.getUUID("id");
            job.owner = tag.getUUID("owner");
            tag.getList("structures", Tag.TAG_STRING).forEach(name -> job.structures.add(name.getAsString()));
            tag.getList("coverage", Tag.TAG_COMPOUND).forEach(input -> {
                GenericStack stack = GenericStack.readTag((CompoundTag) input);
                if (stack != null) job.coverage.add(stack);
            });
            for (String name : job.structures) {
                CompoundTag group = tag.getCompound("batches").getCompound(name);
                job.batches.put(name, group.hasUUID("id") ? group.getUUID("id") : job.id);
                job.batchStarts.put(name, group.contains("started") ? group.getLong("started") : tag.getLong("started"));
            }
            job.output = AEKey.fromTagGeneric(tag.getCompound("output"));
            job.amount = tag.getLong("amount");
            job.stage = Stage.valueOf(tag.getString("stage"));
            job.display = tag.getString("display");
            job.started = tag.getLong("started");
            job.lastProgressAt = tag.getLong("lastProgressAt");
            job.total = tag.getLong("total");
            job.progress = tag.getLong("progress");
            job.elapsed = tag.getLong("elapsed");
            job.ended = tag.getLong("ended");
            job.cancelRequested = tag.getBoolean("cancelRequested");
            job.owned = !tag.contains("owned") || tag.getBoolean("owned");
            if (tag.hasUUID("craftingId")) job.craftingId = tag.getUUID("craftingId");
            job.network = tag.getCompound("network").copy();
            job.cpu = tag.getCompound("cpu").copy();
            job.session = tag.getCompound("session").copy();
            return job;
        }
    }

    static CraftingJobLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CraftingJobLedger::load,
                CraftingJobLedger::new, "mbtool_crafting_jobs");
    }

    private static CraftingJobLedger load(CompoundTag tag) {
        CraftingJobLedger data = new CraftingJobLedger();
        for (Tag entry : tag.getList("jobs", Tag.TAG_COMPOUND)) {
            try {
                Job job = Job.load((CompoundTag) entry);
                if (job.output != null) data.jobs.put(job.id, job);
            } catch (IllegalArgumentException exception) {
                igentuman.mbtool.Mbtool.logger.warn("Skipping invalid saved crafting record", exception);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        jobs.values().forEach(job -> list.add(job.save()));
        tag.put("jobs", list);
        return tag;
    }

    @Nullable UUID beginBatch(UUID owner, String structure) {
        return active(owner, structure) == null ? UUID.randomUUID() : null;
    }

    boolean acceptsBatch(UUID owner, String structure, @Nullable UUID batch) {
        Job active = active(owner, structure);
        return batch != null && (active == null || batch.equals(active.batches.get(structure)));
    }

    Job active(UUID owner, String structure) {
        return jobs.values().stream().filter(job -> job.owner.equals(owner) && job.active()
                        && job.structures.contains(structure))
                .findFirst().orElse(null);
    }

    Job active(UUID owner, String structure, AEKey key) {
        return jobs.values().stream().filter(job -> job.owner.equals(owner) && job.active()
                        && job.structures.contains(structure) && job.covers(key))
                .findFirst().orElse(null);
    }

    boolean tracksCraftingId(UUID craftingId) {
        return craftingId != null && jobs.values().stream()
                .anyMatch(job -> job.active() && craftingId.equals(job.craftingId));
    }

    void associate(Job job, String structure, UUID batch) {
        long previous = jobs.values().stream().filter(other -> other.owner.equals(job.owner) && other.structures.contains(structure))
                .mapToLong(other -> other.batchStarts.getOrDefault(structure, other.started)).max().orElse(0);
        long started = jobs.values().stream().filter(other -> batch.equals(other.batches.get(structure)))
                .mapToLong(other -> other.batchStarts.getOrDefault(structure, other.started))
                .min().orElse(Math.max(System.currentTimeMillis(), previous + 1));
        job.structures.add(structure);
        job.batches.put(structure, batch);
        job.batchStarts.put(structure, started);
        setDirty();
    }

    Job active(UUID owner, AEKey key) {
        return jobs.values().stream().filter(job -> job.owner.equals(owner) && job.active() && job.covers(key))
                .findFirst().orElse(null);
    }

    void removeFinishedCrafts() {
        long now = System.currentTimeMillis();
        if (jobs.values().removeIf(job -> job.expired(now) && job.session.isEmpty())) setDirty();
    }

    void add(Job job) {
        List<Job> history = jobs.values().stream().filter(j -> j.owner.equals(job.owner) && !j.active() && j.session.isEmpty()).toList();
        for (int i = 0; i < history.size() - 127; i++) jobs.remove(history.get(i).id);
        jobs.put(job.id, job);
        setDirty();
    }
}
