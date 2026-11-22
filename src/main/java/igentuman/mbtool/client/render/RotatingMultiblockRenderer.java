package igentuman.mbtool.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class RotatingMultiblockRenderer {
    private static final PreviewCache<MultiblockStructure, StructureMesh> MESHES =
            new PreviewCache<>(16, Long.MAX_VALUE, StructureMesh::close);
    private static final PreviewCache<StructureThumbnail.Size, StructureThumbnail> TARGETS =
            new PreviewCache<>(8, 16L * 1024 * 1024, StructureThumbnail::close);
    private static int revision = -1;

    private RotatingMultiblockRenderer() {}

    public static void render(MultiblockStructure structure, GuiGraphics graphics, int x, int y, int w, int h) {
        render(structure, graphics, x, y, w, h, 18.0, 0xFFFFFFFF);
    }

    public static void render(MultiblockStructure structure, GuiGraphics graphics, int x, int y, int w, int h,
                              double rotationSpeed, int color) {
        if (w <= 0 || h <= 0 || structure.getBlocks().isEmpty()) return;
        graphics.flush();
        if (revision != MultiblocksProvider.getRevision()) {
            invalidateCache();
            revision = MultiblocksProvider.getRevision();
        }
        StructureMesh mesh = MESHES.get(structure);
        if (mesh == null) {
            mesh = MultiblockRenderer.bakeMesh(structure);
            MESHES.put(structure, mesh, 0);
        }
        StructureMesh cached = mesh;
        StructureThumbnail.Size pixels = StructureThumbnail.Size.forGui(w, h, graphics.pose().last().pose(),
                Minecraft.getInstance().getWindow().getGuiScale());
        float yaw = (float) Math.toRadians((Util.getMillis() / 1000.0 * rotationSpeed) % 360.0);
        StructureThumbnail image = TARGETS.get(pixels);
        if (image == null) {
            image = StructureThumbnail.bake(pixels, target -> draw(cached, structure, target, pixels.width(), pixels.height(), yaw));
            TARGETS.put(pixels, image, image.allocationBytes());
        } else {
            image.redraw(target -> draw(cached, structure, target, pixels.width(), pixels.height(), yaw));
        }
        image.draw(graphics.pose(), x, y, w, h, color);
    }

    private static void draw(StructureMesh mesh, MultiblockStructure structure, RenderTarget target, int w, int h, float yaw) {
        PoseStack pose = new PoseStack();
        pose.translate(w / 2.0, h / 2.0, -1900);
        double diameter = Math.sqrt((double) structure.getWidth() * structure.getWidth()
                + (double) structure.getHeight() * structure.getHeight()
                + (double) structure.getDepth() * structure.getDepth());
        float scale = (float) (Math.min(w, h) * 0.9 / Math.max(1, diameter));
        pose.scale(scale, -scale, scale);
        pose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(25)));
        pose.mulPose(new Quaternionf().rotationY(yaw));
        pose.translate(-(structure.getMinX() + structure.getWidth() / 2.0),
                -(structure.getMinY() + structure.getHeight() / 2.0),
                -(structure.getMinZ() + structure.getDepth() / 2.0));
        mesh.draw(pose.last().pose(), new Matrix4f().setOrtho(0, w, h, 0, 1000, 3000), 1, true, target);
    }

    public static void invalidateCache() {
        MESHES.clear();
        TARGETS.clear();
        revision = -1;
    }
}
