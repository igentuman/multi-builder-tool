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
        BufferBuilder buffer = tessellator.getBuilder();
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
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
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
        
        // Top face (Y+)
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
        
        // North face (Z-)
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
        
        // South face (Z+)
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
        
        // West face (X-)
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        
        // East face (X+)
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
        
        tessellator.end();
        
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        poseStack.popPose();
    }
}