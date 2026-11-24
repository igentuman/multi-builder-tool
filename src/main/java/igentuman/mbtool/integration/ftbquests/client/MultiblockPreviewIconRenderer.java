package igentuman.mbtool.integration.ftbquests.client;

import dev.ftb.mods.ftblibrary.client.icon.IconRenderer;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;

public enum MultiblockPreviewIconRenderer implements IconRenderer<MultiblockPreviewIcon> {
    INSTANCE;

    @Override
    public void render(MultiblockPreviewIcon icon, GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;

        Matrix3x2fStack pose = graphics.pose();
        double width = w * Math.hypot(pose.m00(), pose.m01());
        double height = h * Math.hypot(pose.m10(), pose.m11());
        double reduction = Math.max(1.0, Math.max(width / graphics.guiWidth(), height / graphics.guiHeight()));
        int previewWidth = Math.max(1, (int) Math.ceil(width / reduction));
        int previewHeight = Math.max(1, (int) Math.ceil(height / reduction));

        pose.pushMatrix();
        try {
            pose.translate(x, y).scale((float) w / previewWidth, (float) h / previewHeight);
            MultiblockStructure structure = icon.structure();
            if (structure != null) {
                MultiblockRenderer.renderRotating(structure, graphics, 0, 0, previewWidth, previewHeight,
                        angle(icon.tag().rotationSpeed()), icon.color().rgba());
            } else {
                drawMissing(icon, graphics, previewWidth, previewHeight);
            }
        } finally {
            pose.popMatrix();
        }
    }

    @Override
    public double aspectRatio(MultiblockPreviewIcon icon) {
        return (double) icon.tag().width() / icon.tag().height();
    }

    static float angle(double degreesPerSecond) {
        if (degreesPerSecond == 0) return 0f;
        double degrees = (Util.getMillis() / 1000.0 * degreesPerSecond) % 360.0;
        return (float) Math.toRadians(degrees);
    }

    private static void drawMissing(MultiblockPreviewIcon icon, GuiGraphicsExtractor graphics, int w, int h) {
        int alpha = icon.color().alphai();
        graphics.fill(0, 0, w, h, (Math.max(0, Math.min(255, alpha)) / 4) << 24);
        graphics.textWithWordWrap(Minecraft.getInstance().font,
                Component.translatable("gui.mbtool.ftbquests.missing", icon.tag().structure()),
                4, 4, Math.max(1, w - 8), 0xFFFF8080);
    }
}
