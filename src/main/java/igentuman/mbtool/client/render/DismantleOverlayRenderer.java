package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import igentuman.mbtool.util.PlacedStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class DismantleOverlayRenderer {
    
    /**
     * Renders a transparent red lit box around the structure being dismantled
     */
    public static void renderDismantleOverlay(PoseStack poseStack, PlacedStructure structure, float partialTicks) {
        if (structure == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        AABB boundingBox = structure.getBoundingBox();
        
        poseStack.pushPose();
        
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        // Calculate time-based effects
        long time = System.currentTimeMillis();
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        // Red color with pulsing transparency for lit box effect
        float r = 1.0f;
        float g = 0.0f;
        float b = 0.0f;
        // Create a pulsing effect based on time - more transparent for lit box
        float pulse = (float) (0.15f + 0.1f * Math.sin(time * 0.005f));
        float alpha = Math.max(0.1f, pulse);
        
        // Get bounding box coordinates
        float minX = (float) boundingBox.minX-0.0001f;
        float minY = (float) boundingBox.minY-0.0001f;
        float minZ = (float) boundingBox.minZ-0.0001f;
        float maxX = (float) boundingBox.maxX + 1.0001f; // Add 1 to include the last block
        float maxY = (float) boundingBox.maxY + 1.0001f;
        float maxZ = (float) boundingBox.maxZ + 1.0001f;
        
        // Draw filled faces of the box
        // Bottom face (Y-)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, alpha);
        
        // Top face (Y+)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, alpha);
        
        // North face (Z-)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, alpha);
        
        // South face (Z+)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
        
        // West face (X-)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, alpha);
        
        // East face (X+)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, alpha);
        
        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        poseStack.popPose();
    }
}