package igentuman.mbtool.integration.ftbquests;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import igentuman.mbtool.client.render.RotatingMultiblockRenderer;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewComponent;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class MultiblockPreviewIcon extends Icon {
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

    @Override
    public double aspectRatio() {
        return (double) tag.width() / tag.height();
    }

    @Override
    public Icon withColor(Color4I color) {
        MultiblockPreviewIcon copy = new MultiblockPreviewIcon(tag, color);
        copy.revision = revision;
        copy.structure = structure;
        return copy;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
        if (revision != MultiblocksProvider.getRevision()) {
            revision = MultiblocksProvider.getRevision();
            structure = MultiblockPreviewComponent.findStructure(tag.structure());
        }
        if (structure != null) {
            RotatingMultiblockRenderer.render(structure, graphics, x, y, w, h, tag.rotationSpeed(), color.rgba());
        } else {
            var transform = graphics.pose().last().pose();
            int textWidth = Math.max(1, (int) Math.ceil(w * Math.hypot(transform.m00(), transform.m01())));
            int textHeight = Math.max(1, (int) Math.ceil(h * Math.hypot(transform.m10(), transform.m11())));
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale((float) w / textWidth, (float) h / textHeight, 1);
            graphics.drawWordWrap(Minecraft.getInstance().font,
                    Component.translatable("gui.mbtool.ftbquests.missing", tag.structure()),
                    4, 4, Math.max(1, textWidth - 8), 0xFF8080);
            graphics.pose().popPose();
        }
    }

    @Override
    public String toString() {
        return tag.toString();
    }
}
