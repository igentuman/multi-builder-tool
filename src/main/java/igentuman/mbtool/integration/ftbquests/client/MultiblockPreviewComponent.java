package igentuman.mbtool.integration.ftbquests.client;

import dev.ftb.mods.ftblibrary.util.client.ImageComponent;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;

public final class MultiblockPreviewComponent extends ImageComponent {
    public String structureId;
    public double rotationSpeed;

    public MultiblockPreviewComponent(MultiblockPreviewTag tag) {
        structureId = tag.structure();
        width = tag.width();
        height = tag.height();
        align = ImageAlign.fromString(tag.align());
        fit = tag.fit();
        rotationSpeed = tag.rotationSpeed();
        image = new MultiblockPreviewIcon(tag);
    }

    public static MultiblockStructure findStructure(String id) {
        // Prefer the full resource ID. Short names are supported for hand-written markers.
        for (MultiblockStructure structure : MultiblocksProvider.getStructures()) {
            if (!structure.isGroup() && structure.getId() != null && structure.getId().toString().equals(id)) return structure;
        }
        for (MultiblockStructure structure : MultiblocksProvider.getStructures()) {
            if (!structure.isGroup() && (id.equals(structure.getName()) || ("mbtool.structure." + id).equals(structure.getName()))) return structure;
        }
        return null;
    }

    public MultiblockPreviewTag toTag() {
        return new MultiblockPreviewTag(structureId, width, height, align.name().toLowerCase(java.util.Locale.ROOT), fit, rotationSpeed);
    }

    @Override
    public String toString() {
        return toTag().toString();
    }
}
