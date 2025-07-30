package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Quaternionf;

import java.util.Map;

public class MultiblockRenderer {

    public static MultiblockStructure structure;

    public static Vec3i getSize(MultiblockStructure structure) {
        if (structure == null || structure.getBlocks().isEmpty()) {
            return new Vec3i(1, 1, 1);
        }
        return new Vec3i(structure.getWidth(), structure.getHeight(), structure.getDepth());
    }

    public static void render(MultiblockStructure blocksMap, PoseStack stack, int x, int y, int w, int h) {
        structure = blocksMap;
        render(stack, x, y, w, h);
    }
    public static void render(PoseStack stack, int x, int y, int w, int h) {
        if (structure == null || structure.getBlocks().isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        Vec3i size = getSize(structure);
        int width = size.getX();
        int height = size.getY();
        int depth = size.getZ();

        // Calculate appropriate scale to fit within the provided dimensions
        float maxDimension = Math.max(Math.max(width, height), depth);
        float scaleFactor = 0.8f; // Allow some margin around the structure
        float scale = (scaleFactor * Math.min(w, h)) / maxDimension;

        stack.pushPose();
        
        // Center within the provided x, y, w, h bounds
        stack.translate(x + w / 2.0f, y + h / 2.0f, 100);
        
        // Apply isometric-style rotation for better viewing angle
        stack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(30)));
        stack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-135)));

        // Scale to fit within bounds
        stack.scale(scale, scale, scale);

        // Center the structure based on its actual bounds
        stack.translate(-width / 2.0f, -height / 2.0f, -depth / 2.0f);

        // Enable depth testing for proper 3D rendering
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        
        // Get the minimum coordinates to normalize positions
        int minX = structure.getMinX();
        int minY = structure.getMinY();
        int minZ = structure.getMinZ();

        for (Map.Entry<BlockPos, BlockState> entry : structure.getBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            stack.pushPose();
            
            // Translate to the block's position relative to the structure's minimum bounds
            stack.translate(
                pos.getX() - minX,
                pos.getY() - minY,
                pos.getZ() - minZ
            );
            
            // Render the block with full brightness and no overlay
            blockRenderer.renderSingleBlock(
                    state,
                    stack,
                    bufferSource,
                    15728880, // Full brightness
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    null
            );
            
            stack.popPose();
        }
        
        bufferSource.endBatch();
        stack.popPose();
    }

}

