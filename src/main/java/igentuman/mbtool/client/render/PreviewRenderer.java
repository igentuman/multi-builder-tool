package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import igentuman.mbtool.client.handler.ClientHandler;
import igentuman.mbtool.common.MultiblocksProvider;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;

import static igentuman.mbtool.Mbtool.MBTOOL;

public class PreviewRenderer {
    
    // Interpolate alpha based on partialTicks
    private static float interpolatedAlpha = 0.5F;
    private static MultiblockStructure structure = null;
    private static final Minecraft mc = Minecraft.getInstance();
    private static int height;
    private static int length;
    private static int width;
    private static int rotation = 0;
    private static BlockPos hit;
    private static float dir = 0.005f;

    public static BlockPos getRayTraceHit() {
        Player player = mc.player;
        Level world = mc.level;
        
        if (player == null || world == null) return null;
        
        // Perform raycast for 20 blocks
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(20.0));
        
        BlockHitResult rayTrace = world.clip(new net.minecraft.world.level.ClipContext(
            eyePos, endPos, 
            net.minecraft.world.level.ClipContext.Block.OUTLINE, 
            net.minecraft.world.level.ClipContext.Fluid.NONE, 
            player
        ));

        if (rayTrace.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offItem = player.getItemInHand(InteractionHand.OFF_HAND);

        boolean main = !mainItem.isEmpty() && mainItem.is(MBTOOL.get()) && ClientHandler.hasRecipe(mainItem);
        boolean off = !offItem.isEmpty() && offItem.is(MBTOOL.get()) && ClientHandler.hasRecipe(offItem);

        if (!main && !off) return null;

        hit = rayTrace.getBlockPos();
        BlockState state = world.getBlockState(hit);

        // Get the selected structure
        ItemStack multibuilderStack = main ? mainItem : offItem;
        int recipeIndex = multibuilderStack.getOrCreateTag().getInt("recipe");
        
        // Ensure structures are loaded
        if (MultiblocksProvider.structures.isEmpty()) {
            MultiblocksProvider.loadMultiblockStructures();
        }
        
        if (recipeIndex < 0 || recipeIndex >= MultiblocksProvider.structures.size()) {
            return null;
        }
        
        structure = MultiblocksProvider.structures.get(recipeIndex);
        if (structure == null) return null;
        
        // Get rotation from item (if supported in the future)
        rotation = multibuilderStack.getOrCreateTag().getInt("rotation");

        // Calculate placement position based on hit side
        Direction hitSide = rayTrace.getDirection();
        int maxSize = Math.max(structure.getWidth(), structure.getDepth()) - 1;
        
        switch (hitSide) {
            case DOWN:
                hit = hit.offset(0, -structure.getHeight(), 0);
                break;
            case UP:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 1, 0);
                }
                break;
            case EAST:
                hit = hit.offset(maxSize, 0, 0);
                break;
            case WEST:
                hit = hit.offset(-maxSize, 0, 0);
                break;
            case NORTH:
                hit = hit.offset(0, 0, -maxSize);
                break;
            case SOUTH:
                hit = hit.offset(0, 0, maxSize);
                break;
        }
        
        // Center the structure
        if (rotation == 0 || rotation == 2) {
            hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
        } else {
            hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
        }

        return hit;
    }

    public static boolean renderPreview(PoseStack poseStack, float partialTicks) {
        BlockPos hitPos = getRayTraceHit();
        if (hitPos == null || structure == null) return false;

        poseStack.pushPose();

        height = structure.getHeight();
        length = structure.getDepth();
        width = structure.getWidth();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(hitPos.getX() - cameraPos.x, hitPos.getY() - cameraPos.y, hitPos.getZ() - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        
        renderPreviewBlocks(poseStack, partialTicks);
        renderBoundaries(poseStack);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
        return true;
    }

    private static void renderBoundaries(PoseStack poseStack) {
        Level world = mc.level;
        if (world == null) return;
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);
        
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        int bWidth = length;
        int bLength = width;
        
        for (int h = 0; h < height; h++) {
            for (int l = 0; l < bLength; l++) {
                for (int w = 0; w < bWidth; w++) {
                    int xo = l;
                    int zo = w;
                    
                    // Apply rotation
                    switch (rotation) {
                        case 1:
                            zo = l;
                            xo = (bWidth - w - 1);
                            break;
                        case 2:
                            xo = (bLength - l - 1);
                            zo = (bWidth - w - 1);
                            break;
                        case 3:
                            zo = (bLength - l - 1);
                            xo = w;
                            break;
                    }
                    
                    BlockPos actualPos = hit.offset(xo, h, zo);
                    boolean isEmpty = world.getBlockState(actualPos).canBeReplaced();
                    
                    if (!isEmpty || ((w == 0 || w == bWidth-1) && (l == 0 || l == bLength-1) && (h == 0 || h == height-1))) {
                        float r = isEmpty ? 0.0f : 1.0f;
                        float g = isEmpty ? 1.0f : 0.0f;
                        float b = 0.0f;
                        float alpha = 0.4f;
                        
                        float x = xo + 0.5f;
                        float y = h + 0.5f;
                        float z = zo + 0.5f;
                        
                        // Draw wireframe cube
                        if (!isEmpty || h == height-1) { // top face
                            buffer.vertex(matrix, x - 0.5f, y + 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y + 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y + 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y + 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y + 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y + 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y + 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y + 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                        }
                        
                        if (!isEmpty) { // vertical edges
                            buffer.vertex(matrix, x - 0.5f, y + 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y - 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y + 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y - 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y + 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y - 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y + 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y - 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                        }
                        
                        if (!isEmpty || h == 0) { // bottom face
                            buffer.vertex(matrix, x - 0.5f, y - 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y - 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y - 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y - 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x + 0.5f, y - 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y - 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y - 0.5f, z + 0.5f).color(r, g, b, alpha).endVertex();
                            buffer.vertex(matrix, x - 0.5f, y - 0.5f, z - 0.5f).color(r, g, b, alpha).endVertex();
                        }
                    }
                }
            }
        }
        
        tessellator.end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderPreviewBlocks(PoseStack poseStack, float partialTicks) {
        Level world = mc.level;
        if (world == null || structure == null) return;
        
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        
        // Update alpha animation
        interpolatedAlpha = interpolatedAlpha + partialTicks * dir;
        if (interpolatedAlpha >= 0.8f) {
            dir = -0.005f;
        }
        if (interpolatedAlpha <= 0.2f) {
            dir = 0.005f;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, interpolatedAlpha);
        
        // Get the buffer source for rendering
        var bufferSource = mc.renderBuffers().bufferSource();
        
        Map<BlockPos, BlockState> blocks = structure.getBlocks();
        
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos structurePos = entry.getKey();
            BlockState blockState = entry.getValue();
            
            if (blockState.isAir()) continue;
            
            // Calculate world position with rotation
            int xo = structurePos.getX() - structure.getMinX();
            int yo = structurePos.getY() - structure.getMinY();
            int zo = structurePos.getZ() - structure.getMinZ();
            
            // Apply rotation
            int rotatedX = xo;
            int rotatedZ = zo;
            switch (rotation) {
                case 1:
                    rotatedZ = xo;
                    rotatedX = (structure.getDepth() - zo - 1);
                    break;
                case 2:
                    rotatedX = (structure.getWidth() - xo - 1);
                    rotatedZ = (structure.getDepth() - zo - 1);
                    break;
                case 3:
                    rotatedZ = (structure.getWidth() - xo - 1);
                    rotatedX = zo;
                    break;
            }
            
            BlockPos worldPos = hit.offset(rotatedX, yo, rotatedZ);
            
            // Only render if the position is replaceable
            if (world.getBlockState(worldPos).canBeReplaced()) {
                poseStack.pushPose();
                poseStack.translate(rotatedX, yo, rotatedZ);
                
                try {
                    // Render the block with full brightness
                    blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
                } catch (Exception e) {
                    // Fallback: render a simple colored cube if block rendering fails
                    renderSimpleCube(poseStack, bufferSource);
                }
                
                poseStack.popPose();
            }
        }
        
        // Flush the buffer to ensure all blocks are rendered
        bufferSource.endBatch();
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    private static void renderSimpleCube(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource) {
        // Fallback method to render a simple translucent cube
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        float alpha = interpolatedAlpha;
        float r = 0.8f, g = 0.8f, b = 0.8f; // Light gray color
        
        // Render all 6 faces of the cube
        // Bottom face
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, alpha).endVertex();
        
        // Top face
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, alpha).endVertex();
        
        // North face
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, alpha).endVertex();
        
        // South face
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, alpha).endVertex();
        
        // West face
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha).endVertex();
        
        // East face
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, alpha).endVertex();
        
        tessellator.end();
    }
}
