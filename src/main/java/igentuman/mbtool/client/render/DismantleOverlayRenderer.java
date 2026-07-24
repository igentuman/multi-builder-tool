package igentuman.mbtool.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import igentuman.mbtool.util.PlacedStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class DismantleOverlayRenderer {

    public static void renderDismantleOverlay(PoseStack poseStack, PlacedStructure structure, float partialTicks) {
        if (structure == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        AABB boundingBox = structure.getBoundingBox();

        poseStack.pushPose();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        long time = System.currentTimeMillis();
        float pulse = (float) (0.15f + 0.1f * Math.sin(time * 0.005f));
        float alpha = Math.max(0.1f, pulse);

        float minX = (float) boundingBox.minX - 0.0001f;
        float minY = (float) boundingBox.minY - 0.0001f;
        float minZ = (float) boundingBox.minZ - 0.0001f;
        float maxX = (float) boundingBox.maxX + 1.0001f;
        float maxY = (float) boundingBox.maxY + 1.0001f;
        float maxZ = (float) boundingBox.maxZ + 1.0001f;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.LINES);
        Matrix4f matrix = poseStack.last().pose();

        float r = 1.0f, g = 0.0f, b = 0.0f;
        // Draw 12 edges of the AABB
        addLineBox(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);

        bufferSource.endBatch(RenderTypes.LINES);

        poseStack.popPose();
    }

    private static void addLineBox(VertexConsumer buffer, Matrix4f matrix,
                                    float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ,
                                    float r, float g, float b, float a) {
        int color = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        // Bottom face edges
        addLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        addLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        addLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        // Top face edges
        addLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        addLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        addLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        addLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        // Vertical edges
        addLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        addLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        addLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private static void addLine(VertexConsumer buffer, Matrix4f matrix,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2, int color) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        buffer.addVertex(matrix, x1, y1, z1).setColor(color).setNormal(nx, ny, nz).setLineWidth(2.0f);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color).setNormal(nx, ny, nz).setLineWidth(2.0f);
    }
}
