package igentuman.mbtool.integration.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import igentuman.mbtool.Mbtool;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Future;

public final class CraftingJobLedger extends SavedData {
    private static final long OUTCOME_DISPLAY_MILLIS = 5_000;

    static final SavedDataType<CraftingJobLedger> TYPE = new SavedDataType<>(
            Mbtool.rl("crafting_jobs"), CraftingJobLedger::new,
            CompoundTag.CODEC.xmap(CraftingJobLedger::load, CraftingJobLedger::saveTag));

    final Map<UUID, Job> jobs = new LinkedHashMap<>();

    public enum Stage {
        CALCULATING, WAITING_FOR_CPU, CONFIRMING, CRAFTING, COMPLETED, CANCELLED, REMOVED, FAILED;

        public boolean active() { return ordinal() < COMPLETED.ordinal(); }
    }

    static RegistryOps<Tag> ops() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("No server for crafting ledger serialization");
        HolderLookup.Provider registries = server.registryAccess();
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }

    static Tag encodeKey(AEKey key) {
        return AEKey.CODEC.encodeStart(ops(), key).getOrThrow();
    }

    static @Nullable AEKey decodeKey(Tag tag) {
        return AEKey.CODEC.parse(ops(), tag).result().orElse(null);
    }

    static Tag encodeStack(GenericStack stack) {
        return GenericStack.CODEC.encodeStart(ops(), stack).getOrThrow();
    }

    static @Nullable GenericStack decodeStack(Tag tag) {
        return GenericStack.CODEC.parse(ops(), tag).result().orElse(null);
    }

    private static void putUUID(CompoundTag tag, String key, UUID id) {
        tag.store(key, UUIDUtil.CODEC, id);
    }

    private static @Nullable UUID getUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElse(null);
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
            putUUID(tag, "id", id);
            putUUID(tag, "owner", owner);
            ListTag names = new ListTag();
            structures.forEach(name -> names.add(StringTag.valueOf(name)));
            tag.put("structures", names);
            CompoundTag groups = new CompoundTag();
            for (String name : structures) {
                CompoundTag group = new CompoundTag();
                putUUID(group, "id", batches.getOrDefault(name, id));
                group.putLong("started", batchStarts.getOrDefault(name, started));
                groups.put(name, group);
            }
            tag.put("batches", groups);
            ListTag inputs = new ListTag();
            coverage.forEach(stack -> inputs.add(encodeStack(stack)));
            tag.put("coverage", inputs);
            tag.put("output", encodeKey(output));
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
            if (craftingId != null) putUUID(tag, "craftingId", craftingId);
            tag.put("network", network.copy());
            tag.put("cpu", cpu.copy());
            tag.put("session", session.copy());
            return tag;
        }

        static Job load(CompoundTag tag) {
            Job job = new Job();
            job.id = Objects.requireNonNull(getUUID(tag, "id"), "id");
            job.owner = Objects.requireNonNull(getUUID(tag, "owner"), "owner");
            tag.getListOrEmpty("structures").forEach(name -> name.asString().ifPresent(job.structures::add));
            tag.getListOrEmpty("coverage").forEach(input -> {
                GenericStack stack = decodeStack(input);
                if (stack != null) job.coverage.add(stack);
            });
            CompoundTag groups = tag.getCompoundOrEmpty("batches");
            for (String name : job.structures) {
                CompoundTag group = groups.getCompoundOrEmpty(name);
                UUID batch = getUUID(group, "id");
                job.batches.put(name, batch != null ? batch : job.id);
                job.batchStarts.put(name, group.getLongOr("started", tag.getLongOr("started", 0L)));
            }
            Tag output = tag.get("output");
            job.output = output == null ? null : decodeKey(output);
            job.amount = tag.getLongOr("amount", 0L);
            job.stage = Stage.valueOf(tag.getStringOr("stage", Stage.FAILED.name()));
            job.display = tag.getStringOr("display", "failed");
            job.started = tag.getLongOr("started", 0L);
            job.lastProgressAt = tag.getLongOr("lastProgressAt", job.started);
            job.total = tag.getLongOr("total", 0L);
            job.progress = tag.getLongOr("progress", 0L);
            job.elapsed = tag.getLongOr("elapsed", 0L);
            job.ended = tag.getLongOr("ended", 0L);
            job.cancelRequested = tag.getBooleanOr("cancelRequested", false);
            job.owned = tag.getBooleanOr("owned", true);
            job.craftingId = getUUID(tag, "craftingId");
            job.network = tag.getCompoundOrEmpty("network").copy();
            job.cpu = tag.getCompoundOrEmpty("cpu").copy();
            job.session = tag.getCompoundOrEmpty("session").copy();
            return job;
        }
    }

    static CraftingJobLedger get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private static CraftingJobLedger load(CompoundTag tag) {
        CraftingJobLedger data = new CraftingJobLedger();
        for (Tag entry : tag.getListOrEmpty("jobs")) {
            if (!(entry instanceof CompoundTag compound)) continue;
            try {
                Job job = Job.load(compound);
                if (job.output != null) data.jobs.put(job.id, job);
            } catch (RuntimeException exception) {
                Mbtool.logger.warn("Skipping invalid saved crafting record", exception);
            }
        }
        return data;
    }

    private CompoundTag saveTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        jobs.values().forEach(job -> list.add(job.save()));
        tag.put("jobs", list);
        return tag;
    }

    Job active(UUID owner, String structure) {
        return jobs.values().stream().filter(job -> job.owner.equals(owner) && job.active()
                        && job.structures.contains(structure))
                .findFirst().orElse(null);
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
