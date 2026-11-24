package igentuman.mbtool.integration.ftbquests.client;

import dev.ftb.mods.ftblibrary.client.util.ImageComponent;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;

import java.util.Locale;

public final class MultiblockPreviewComponent extends ImageComponent {
    public String structureId;
    public double rotationSpeed;

    public MultiblockPreviewComponent(MultiblockPreviewTag tag) {
        structureId = tag.structure();
        rotationSpeed = tag.rotationSpeed();
        setWidth(tag.width());
        setHeight(tag.height());
        setAlign(ImageAlign.byName(tag.align()));
        setFit(tag.fit());
        setImage(new MultiblockPreviewIcon(tag));
    }

    public MultiblockPreviewTag toTag() {
        return new MultiblockPreviewTag(structureId, getWidth(), getHeight(),
                getAlign().getName().toLowerCase(Locale.ROOT), isFit(), rotationSpeed);
    }

    @Override
    public String toString() {
        return toTag().toString();
    }
}
