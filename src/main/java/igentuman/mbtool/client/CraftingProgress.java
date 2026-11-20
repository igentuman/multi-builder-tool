package igentuman.mbtool.client;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.item.StructureRequestItem;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static igentuman.mbtool.util.TextUtils.__;

@Mod.EventBusSubscriber(modid = Mbtool.MODID, value = Dist.CLIENT)
public final class CraftingProgress {
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
        for (Tag entry : data.getList("jobs", Tag.TAG_COMPOUND)) {
            CompoundTag job = (CompoundTag) entry;
            for (Tag name : job.getList("structures", Tag.TAG_STRING)) {
                grouped.computeIfAbsent(name.getAsString(), ignored -> new ArrayList<>()).add(job);
            }
        }
        Map<String, Summary> next = new HashMap<>();
        grouped.forEach((structure, matching) -> {
            CompoundTag latest = matching.stream()
                    .max(Comparator.comparingLong(job -> job.getLong("batchStarted"))).orElseThrow();
            UUID batch = latest.getUUID("batch");
            next.put(structure, summarize(matching.stream().filter(job -> batch.equals(job.getUUID("batch"))).toList()));
        });
        summaries = Map.copyOf(next);
    }

    public static Summary forStructure(String structure) { return summaries.get(structure); }

    static Summary summarize(List<CompoundTag> records) {
        boolean active = records.stream().anyMatch(job -> job.getBoolean("active"));
        String state = active ? "crafting" : "completed";
        int rank = -1;
        long total = 0, done = 0, elapsed = 0, eta = 0;
        boolean unknown = false;
        for (CompoundTag job : records) {
            long work = Math.max(1, job.getLong("total"));
            total += work;
            done += job.getString("state").equals("completed") ? work : Math.min(work, job.getLong("progress"));
            elapsed = Math.max(elapsed, job.getLong("elapsed"));
            if (job.getBoolean("active")) {
                long remaining = job.getLong("eta");
                unknown |= remaining < 0;
                eta = Math.max(eta, remaining);
            }
            if (job.getBoolean("active") == active) {
                int priority = priority(job.getString("state"));
                if (priority > rank) { rank = priority; state = job.getString("state"); }
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
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { summaries = Map.of(); }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui) return;
        Set<String> shown = new HashSet<>();
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 22;
        for (ItemStack stack : List.of(mc.player.getMainHandItem(), mc.player.getOffhandItem())) {
            if (!(stack.getItem() instanceof MultibuilderItem item)) continue;
            MultiblockStructure structure = item.getRuntimeStructure(stack);
            if (structure == null) structure = item.getSelectedStructure(stack);
            if (structure == null || !shown.add(structure.getName())) continue;
            Summary summary = forStructure(structure.getName());
            if (summary == null) continue;
            Component title = StructureRequestItem.structureName(structure.getName());
            List<Component> lines = List.of(title, summary.status(), summary.timing());
            int width = Math.min(mc.getWindow().getGuiScaledWidth() - 16,
                    lines.stream().mapToInt(mc.font::width).max().orElse(120) + 10);
            event.getGuiGraphics().fill(5, y - 4, 5 + width, y + 33, 0xB0000000);
            for (int i = 0; i < lines.size(); i++) {
                event.getGuiGraphics().drawString(mc.font, mc.font.plainSubstrByWidth(lines.get(i).getString(), width - 10),
                        10, y + i * 11, i == 1 ? 0xFFB5FFFF : 0xFFFFFFFF, true);
            }
            y += 44;
        }
    }
}
