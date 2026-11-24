package igentuman.mbtool.client;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.item.StructureRequestItem;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.*;

import static igentuman.mbtool.util.TextUtils.__;

@EventBusSubscriber(modid = Mbtool.MODID, value = Dist.CLIENT)
public final class CraftingProgress {
    private static final UUID NO_BATCH = new UUID(0, 0);
    private static Map<String, Summary> summaries = Map.of();

    public record Summary(String state, boolean active, int percent, long elapsed, long eta, int jobs) {
        public Component status() {
            return __("gui.mbtool.crafting." + state, percent);
        }

        public Component timing() {
            return __("gui.mbtool.crafting.timing", duration(elapsed), eta < 0 ? "?" : duration(eta));
        }
    }

    public static void update(CompoundTag data) {
        Map<String, List<CompoundTag>> grouped = new HashMap<>();
        for (Tag entry : data.getListOrEmpty("jobs")) {
            if (!(entry instanceof CompoundTag job)) continue;
            for (Tag name : job.getListOrEmpty("structures")) {
                name.asString().ifPresent(structure ->
                        grouped.computeIfAbsent(structure, ignored -> new ArrayList<>()).add(job));
            }
        }
        Map<String, Summary> next = new HashMap<>();
        grouped.forEach((structure, matching) -> {
            // Aggregate only the latest batch so old and new outcomes are never mixed.
            CompoundTag latest = matching.stream()
                    .max(Comparator.comparingLong(job -> job.getLongOr("batchStarted", 0L))).orElseThrow();
            UUID batch = batch(latest);
            next.put(structure, summarize(matching.stream().filter(job -> batch.equals(batch(job))).toList()));
        });
        summaries = Map.copyOf(next);
    }

    private static UUID batch(CompoundTag job) {
        return job.read("batch", UUIDUtil.CODEC).orElse(NO_BATCH);
    }

    public static Summary forStructure(String structure) {
        return structure == null ? null : summaries.get(structure);
    }

    static Summary summarize(List<CompoundTag> records) {
        boolean active = records.stream().anyMatch(job -> job.getBooleanOr("active", false));
        String state = active ? "crafting" : "completed";
        int rank = -1;
        long total = 0, done = 0, elapsed = 0, eta = 0;
        boolean unknown = false;
        for (CompoundTag job : records) {
            long work = Math.max(1, job.getLongOr("total", 0L));
            String jobState = job.getStringOr("state", "");
            boolean jobActive = job.getBooleanOr("active", false);
            total += work;
            done += jobState.equals("completed") ? work : Math.min(work, job.getLongOr("progress", 0L));
            elapsed = Math.max(elapsed, job.getLongOr("elapsed", 0L));
            if (jobActive) {
                long remaining = job.getLongOr("eta", -1L);
                unknown |= remaining < 0;
                eta = Math.max(eta, remaining);
            }
            if (jobActive == active) {
                int priority = priority(jobState);
                if (priority > rank) { rank = priority; state = jobState; }
            }
        }
        return new Summary(state, active, total == 0 ? 0 : (int) Math.min(100, done * 100.0 / total),
                elapsed, unknown ? -1 : eta, records.size());
    }

    private static int priority(String state) {
        return switch (state) {
            case "cancelling" -> 10;
            case "disconnected" -> 9;
            case "stalled" -> 8;
            case "failed", "removed", "cancelled" -> 7;
            case "confirming", "connecting", "calculating" -> 6;
            case "waiting_for_cpu" -> 5;
            default -> 0;
        };
    }

    public static String duration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        if (seconds >= 3600) return "%dh %02dm".formatted(seconds / 3600, seconds / 60 % 60);
        return "%dm %02ds".formatted(seconds / 60, seconds % 60);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        summaries = Map.of();
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui || summaries.isEmpty()) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Set<String> shown = new HashSet<>();
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 22;
        for (ItemStack stack : List.of(mc.player.getMainHandItem(), mc.player.getOffhandItem())) {
            if (!(stack.getItem() instanceof MultibuilderItem item)) continue;
            MultiblockStructure structure = item.getCurrentStructure(stack);
            if (structure == null || structure.getName() == null || !shown.add(structure.getName())) continue;
            Summary summary = forStructure(structure.getName());
            if (summary == null) continue;
            Component title = StructureRequestItem.structureName(structure.getName());
            List<Component> lines = List.of(title, summary.status(), summary.timing());
            int width = Math.min(mc.getWindow().getGuiScaledWidth() - 16,
                    lines.stream().mapToInt(mc.font::width).max().orElse(120) + 10);
            graphics.fill(5, y - 4, 5 + width, y + 33, 0xB0000000);
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(mc.font, mc.font.plainSubstrByWidth(lines.get(i).getString(), width - 10),
                        10, y + i * 11, i == 1 ? 0xFFB5FFFF : 0xFFFFFFFF, true);
            }
            y += 44;
        }
    }
}
