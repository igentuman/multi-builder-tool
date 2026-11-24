package igentuman.mbtool.integration.ftbquests;

import dev.ftb.mods.ftblibrary.client.icon.IconRenderer;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.neoforged.fml.loading.FMLEnvironment;

public final class MultiblockPreviewIcon extends Icon<MultiblockPreviewIcon> {
    private final MultiblockPreviewTag tag;
    private final Color4I color;
    private int revision = -1;
    private MultiblockStructure structure;

    public MultiblockPreviewIcon(MultiblockPreviewTag tag) {
        this(tag, Color4I.WHITE);
    }

    private MultiblockPreviewIcon(MultiblockPreviewTag tag, Color4I color) {
        this.tag = tag;
        this.color = color;
    }

    public MultiblockPreviewTag tag() {
        return tag;
    }

    public Color4I color() {
        return color;
    }

    public MultiblockStructure structure() {
        int current = MultiblocksProvider.getRevision();
        if (revision != current) {
            revision = current;
            structure = findStructure(tag.structure());
        }
        return structure;
    }

    public static MultiblockStructure findStructure(String id) {
        if (id == null) return null;
        // Prefer the full resource ID. Short names are supported for hand-written markers.
        for (MultiblockStructure structure : MultiblocksProvider.getStructures()) {
            if (structure.getId() != null && structure.getId().toString().equals(id)) return structure;
        }
        for (MultiblockStructure structure : MultiblocksProvider.getStructures()) {
            String name = structure.getName();
            if (id.equals(name) || ("mbtool.structure." + id).equals(name)) return structure;
        }
        return null;
    }

    @Override
    public MultiblockPreviewIcon copy() {
        MultiblockPreviewIcon copy = new MultiblockPreviewIcon(tag, color);
        copy.revision = revision;
        copy.structure = structure;
        return copy;
    }

    @Override
    public MultiblockPreviewIcon withColor(Color4I color) {
        MultiblockPreviewIcon copy = new MultiblockPreviewIcon(tag, color);
        copy.revision = revision;
        copy.structure = structure;
        return copy;
    }

    @Override
    public IconRenderer<MultiblockPreviewIcon> getRenderer() {
        // Never touch the client renderer class on a dedicated server.
        return FMLEnvironment.getDist().isClient() ? ClientRendererHolder.get() : NoopRenderer.INSTANCE;
    }

    @Override
    public String toString() {
        return tag.toString();
    }

    private enum NoopRenderer implements IconRenderer<MultiblockPreviewIcon> {
        INSTANCE;

        @Override
        public void render(MultiblockPreviewIcon icon, net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                           int x, int y, int w, int h) {
        }

        @Override
        public double aspectRatio(MultiblockPreviewIcon icon) {
            return (double) icon.tag.width() / icon.tag.height();
        }
    }

    private static final class ClientRendererHolder {
        static IconRenderer<MultiblockPreviewIcon> get() {
            return igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewIconRenderer.INSTANCE;
        }
    }
}
