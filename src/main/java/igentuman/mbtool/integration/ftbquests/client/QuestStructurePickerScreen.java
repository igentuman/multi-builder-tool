package igentuman.mbtool.integration.ftbquests.client;

import igentuman.mbtool.client.gui.BrowserScrollPanel;
import igentuman.mbtool.client.gui.GuiTheme;
import igentuman.mbtool.client.gui.ThemedButton;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.TextUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class QuestStructurePickerScreen extends Screen {
    private record Entry(MultiblockStructure structure, String id, String name, String group) {}

    private final String selected;
    private final Consumer<String> callback;
    private List<Entry> entries = List.of();
    private List<Entry> filtered = List.of();
    private BrowserScrollPanel grid;
    private String query = "";
    private int revision = -1;
    private boolean done;

    public QuestStructurePickerScreen(String selected, Consumer<String> callback) {
        super(Component.translatable("gui.mbtool.select_structure"));
        this.selected = selected == null ? "" : selected;
        this.callback = callback;
    }

    @Override
    protected void init() {
        EditBox search = new EditBox(font, 16, 28, width - 32, 16, Component.translatable("gui.mbtool.search_structures"));
        search.setMaxLength(128);
        search.setHint(Component.translatable("gui.mbtool.search_structures"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            filter();
        });
        addRenderableWidget(search);
        int columns = Math.max(1, (width - 32) / 110);
        grid = addRenderableWidget(new BrowserScrollPanel(16, 50, width - 32, Math.max(24, height - 86),
                columns, 104, 4, title) {
            @Override
            protected Component entryNarration(int index) {
                return Component.literal(filtered.get(index).name());
            }

            @Override
            protected void renderEntry(GuiGraphicsExtractor graphics, int index, int x, int y, int w, int h, boolean hovered) {
                Entry entry = filtered.get(index);
                boolean isSelected = selected.equals(entry.id());
                bevel(graphics, x, y, w, h, isSelected
                        ? GuiTheme.color(0xFF6F8F6F, 0xFF3F5A3F)
                        : GuiTheme.color(hovered ? 0xFFA0A0A0 : 0xFF8B8B8B, hovered ? 0xFF555555 : 0xFF3C3C3C), isSelected);
                int previewHeight = h - 29;
                graphics.fill(x + 3, y + 3, x + w - 3, y + previewHeight, GuiTheme.color(0xFF686868, 0xFF282828));
                graphics.enableScissor(x + 3, Math.max(getY(), y + 3), x + w - 3,
                        Math.min(getY() + getHeight(), y + previewHeight));
                try {
                    MultiblockRenderer.render(entry.structure(), graphics, x + 4, y + 4, w - 8, previewHeight - 6);
                } finally {
                    graphics.disableScissor();
                }
                drawFitted(graphics, entry.name(), x + 4, y + h - 24, w - 8, 0xFFFFFFFF);
                drawFitted(graphics, entry.group(), x + 4, y + h - 12, w - 8, 0xFFD0D0D0);
            }

            @Override
            protected void activateEntry(int index) {
                finish(filtered.get(index).id());
            }
        });
        MultiblockRenderer.setVisiblePreviewCount(grid.visibleCapacity());
        addRenderableWidget(new ThemedButton(width / 2 - 50, height - 28, 100, 20,
                Component.translatable("gui.cancel"), button -> onClose()));
        reload();
        setInitialFocus(search);
    }

    private void drawFitted(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        if (font.width(text) > maxWidth) text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
        graphics.text(font, text, x, y, color, false);
    }

    private static String displayName(String key) {
        if (key == null || key.isBlank()) return "";
        String translated = Component.translatable(key).getString();
        return translated.equals(key) ? TextUtils.convertToName(key.replace("mbtool.structure.", "")) : translated;
    }

    private static String groupName(String group) {
        if (group == null || group.isBlank() || MultiblockStructure.DEFAULT_GROUP.equals(group)) {
            return Component.translatable("gui.mbtool.ungrouped").getString();
        }
        String key = "gui.mbtool.folder." + group;
        String translated = Component.translatable(key).getString();
        return translated.equals(key) ? displayName(group) : translated;
    }

    private void reload() {
        revision = MultiblocksProvider.getRevision();
        entries = List.copyOf(MultiblocksProvider.getStructures()).stream()
                .filter(s -> s.getId() != null)
                .map(s -> new Entry(s, s.getId().toString(), displayName(s.getName()), groupName(s.getGroup())))
                .toList();
        filter();
    }

    private void filter() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        filtered = entries.stream()
                .filter(e -> needle.isEmpty()
                        || (e.name() + " " + e.id() + " " + e.group()).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        if (grid != null) grid.setItemCount(filtered.size());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (revision != MultiblocksProvider.getRevision()) reload();
        BrowserScrollPanel.bevel(graphics, 8, 6, width - 16, height - 12, GuiTheme.color(0xFFC6C6C6, 0xFF383838), false);
        graphics.centeredText(font, title, width / 2, 14, GuiTheme.labelColor());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (filtered.isEmpty()) {
            graphics.centeredText(font, Component.translatable("gui.mbtool.ftbquests.empty"),
                    grid.getX() + grid.getWidth() / 2, grid.getY() + grid.getHeight() / 2, 0xFFFFFFFF);
        }
        int hovered = grid.hoveredIndex(mouseX, mouseY);
        if (hovered >= 0) {
            Entry entry = filtered.get(hovered);
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(entry.name()),
                    Component.literal(entry.group()).withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal(entry.id()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        }
    }

    private void finish(String selection) {
        if (done) return;
        done = true;
        callback.accept(selection);
    }

    @Override
    public void onClose() {
        finish(null);
    }
}
