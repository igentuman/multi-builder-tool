package igentuman.mbtool.integration.ftbquests.client;

import igentuman.mbtool.client.gui.BrowserScrollPanel;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class QuestStructurePickerScreen extends Screen {
    private final String selected;
    private final Consumer<String> callback;
    private List<MultiblockStructure> filtered = List.of();
    private BrowserScrollPanel grid;
    private String query = "";
    private int revision = -1;

    public QuestStructurePickerScreen(String selected, Consumer<String> callback) {
        super(Component.translatable("gui.mbtool.select_structure"));
        this.selected = selected;
        this.callback = callback;
    }

    @Override
    protected void init() {
        EditBox search = new EditBox(font, 12, 28, width - 24, 20, Component.translatable("gui.mbtool.search_structures"));
        search.setMaxLength(128);
        search.setHint(Component.translatable("gui.mbtool.search_structures"));
        search.setValue(query);
        search.setResponder(value -> { query = value; filter(); });
        addRenderableWidget(search);
        int columns = Math.max(1, (width - 24) / 110);
        grid = addRenderableWidget(new BrowserScrollPanel(12, 54, width - 24, Math.max(24, height - 90),
                columns, 104, 4, title) {
            @Override
            protected Component entryNarration(int index) { return label(filtered.get(index)); }

            @Override
            protected void renderEntry(GuiGraphics graphics, int index, int x, int y, int w, int h, boolean hovered) {
                MultiblockStructure structure = filtered.get(index);
                bevel(graphics, x, y, w, h, hovered ? 0xFF606878 : 0xFF383E4A, selected.equals(structure.getId().toString()));
                MultiblockRenderer.render(structure, graphics, x + 4, y + 4, w - 8, h - 28);
                graphics.drawString(font, font.plainSubstrByWidth(label(structure).getString(), w - 8), x + 4, y + h - 22, 0xFFFFFF);
                graphics.drawString(font, font.plainSubstrByWidth(String.valueOf(structure.getGroup()), w - 8), x + 4, y + h - 11, 0xBBBBBB);
            }

            @Override
            protected void activateEntry(int index) { callback.accept(filtered.get(index).getId().toString()); }
        });
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        filter();
    }

    private static Component label(MultiblockStructure structure) { return Component.translatable(structure.getName()); }

    private void filter() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        filtered = MultiblocksProvider.getStructures().stream().filter(s -> !s.isGroup() && s.getId() != null)
                .filter(s -> (label(s).getString() + " " + s.getId() + " " + s.getGroup()).toLowerCase(Locale.ROOT).contains(needle)).toList();
        revision = MultiblocksProvider.getRevision();
        if (grid != null) grid.setItemCount(filtered.size());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (revision != MultiblocksProvider.getRevision()) filter();
        MultiblockRenderer.beginFrame();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        if (filtered.isEmpty()) graphics.drawCenteredString(font, Component.translatable("gui.mbtool.ftbquests.empty"), width / 2, 64, 0xFFFFFF);
    }

    @Override
    public void onClose() { callback.accept(null); }
}
