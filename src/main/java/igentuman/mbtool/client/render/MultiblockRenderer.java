package igentuman.mbtool.client.render;

import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MultiblockRenderer {
    private static int sourceRevision = -1;

    private MultiblockRenderer() {}

    public static void beginFrame() {
        if (sourceRevision != MultiblocksProvider.getRevision()) {
            sourceRevision = MultiblocksProvider.getRevision();
            GuiStructureRenderer.invalidateCache();
        }
        GuiStructureRenderer.beginFrame();
    }

    public static void setVisiblePreviewCount(int count) {
        GuiStructureRenderer.setVisiblePreviewCount(count);
    }

    /** Must be called on the render thread. */
    public static void invalidateCache() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(MultiblockRenderer::invalidateCache);
            return;
        }
        GuiStructureRenderer.invalidateCache();
    }

    public static void render(MultiblockStructure structure, GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        render(structure, graphics, x, y, w, h, -1);
    }

    public static void render(MultiblockStructure structure, GuiGraphicsExtractor graphics, int x, int y, int w, int h, int tint) {
        if (structure == null || structure.getBlocks().isEmpty() || w <= 0 || h <= 0) return;
        float scale = fitScale(structure, 0f, w, h);
        if (scale <= 0f) return;
        graphics.submitPictureInPictureRenderState(new GuiStructureRenderState(structure, 0f, x, y, x + w, y + h,
            scale, true, tint, new Matrix3x2f(graphics.pose()), graphics.peekScissorStack()));
    }

    public static void renderRotating(MultiblockStructure structure, GuiGraphicsExtractor graphics,
                                      int x, int y, int w, int h, float angleY) {
        renderRotating(structure, graphics, x, y, w, h, angleY, -1);
    }

    public static void renderRotating(MultiblockStructure structure, GuiGraphicsExtractor graphics,
                                      int x, int y, int w, int h, float angleY, int tint) {
        if (structure == null || structure.getBlocks().isEmpty() || w <= 0 || h <= 0) return;
        float scale = fitScale(structure, angleY, w, h);
        if (scale <= 0f) return;
        graphics.submitPictureInPictureRenderState(new GuiStructureRenderState(structure, angleY, x, y, x + w, y + h,
            scale, false, tint, new Matrix3x2f(graphics.pose()), graphics.peekScissorStack()));
    }

    public static float fitScale(MultiblockStructure structure, float angleY, int w, int h) {
        int width = structure.getWidth();
        int height = structure.getHeight();
        int depth = structure.getDepth();
        Matrix4f rotation = new Matrix4f()
            .rotateX((float) Math.toRadians(30))
            .rotateY((float) Math.toRadians(225) + angleY);

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector3f corner = new Vector3f();
        for (int cx = 0; cx <= 1; cx++) {
            for (int cy = 0; cy <= 1; cy++) {
                for (int cz = 0; cz <= 1; cz++) {
                    corner.set(cx * width - width / 2f, cy * height - height / 2f, cz * depth - depth / 2f);
                    rotation.transformPosition(corner);
                    minX = Math.min(minX, corner.x);
                    maxX = Math.max(maxX, corner.x);
                    minY = Math.min(minY, corner.y);
                    maxY = Math.max(maxY, corner.y);
                }
            }
        }
        float projW = maxX - minX;
        float projH = maxY - minY;
        float scale = Math.min((w - 4f) / Math.max(projW, 0.01f), (h - 4f) / Math.max(projH, 0.01f));
        return Math.min(scale, 24f);
    }
}
