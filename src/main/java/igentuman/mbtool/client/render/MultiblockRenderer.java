package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderTypeHelper;
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

        stack.pushPose();
        
        // Set up proper GUI rendering state
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Center within the provided x, y, w, h bounds with proper Z positioning for GUI
        stack.translate(x + w / 2.0f, y + h / 2.0f, 100.0f);
        
        // Calculate appropriate scale to fit within the provided dimensions
        float maxDimension = Math.max(Math.max(structure.getWidth(), structure.getHeight()), structure.getDepth());
        float baseScale = Math.min(w, h) * 0.9f; // Use 90% of available space
        float scale = baseScale / maxDimension;
        stack.scale(scale, -scale, scale); // Negative Y scale to match GUI coordinates

        // Apply isometric-style rotation for better viewing angle
        stack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(30)));
        stack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-45)));

        // Use the correct implementation from JEI category
        renderStructure(structure, stack);

        // Restore render state
        RenderSystem.disableBlend();

        stack.popPose();
    }

    private static void renderStructure(MultiblockStructure structure, PoseStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        ModelBlockRenderer modelBlockRenderer = blockRenderer.getModelRenderer();
        BlockEntityRenderDispatcher blockEntityRenderer = minecraft.getBlockEntityRenderDispatcher();

        // Get all blocks and calculate structure center for better positioning
        Map<BlockPos, BlockState> blocks = structure.getBlocks();
        if (blocks.isEmpty()) return;

        // Calculate structure dimensions for scaling
        int width = structure.getWidth();
        int height = structure.getHeight();
        int depth = structure.getDepth();
        float scale = (float) (1.2f / (Math.log10(Math.max(Math.max(width, height), depth)+105)));

        // Apply scaling to fit the structure in view
        stack.scale(scale, scale, scale);

        // Center the structure
        float centerX = structure.getMinX() + width / 2.0f;
        float centerY = structure.getMinY() + height / 2.0f;
        float centerZ = structure.getMinZ() + depth / 2.0f;
        stack.translate(-centerX, -centerY, -centerZ);

        // Set up rendering
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        // Render each block
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());

            try {
                try {
                    // Try to get the block entity
                    BlockEntity entity = null;
                    if (state.getBlock() instanceof EntityBlock entityBlock)
                        entity = entityBlock.newBlockEntity(pos, state);

                    // If block entity exist, try to get its renderer
                    BlockEntityRenderer<BlockEntity> renderer = null;
                    if(entity != null)
                        renderer = blockEntityRenderer.getRenderer(entity);

                    // If the block entity has a renderer, then render the block
                    if(renderer != null) {
                        entity.setLevel(minecraft.level);
                        renderer.render(
                                entity,
                                minecraft.getPartialTick(),
                                stack,
                                bufferSource,
                                15728880,
                                OverlayTexture.NO_OVERLAY
                        );
                    }
                } catch (Exception ignored) {
                    // Fall back to the block model render if something goes wrong here
                }

                // Get BakedModel from the block state to render it
                BakedModel model = blockRenderer.getBlockModel(state);

                // Get ModelData from the block state for proper rendering of complex blocks like GTCEU controllers/ports
                ModelData modelData;
                try {
                    // Try to get model data from the block if it supports it
                    modelData = model.getModelData(minecraft.level, pos, state, ModelData.EMPTY);
                } catch (Exception ignored) {
                    // Fall back to empty model data if there's any issue
                    modelData = ModelData.EMPTY;
                }

                // Avoid fog color blending on MODEL shapes
                RenderShape shape = state.getRenderShape();
                if(shape == RenderShape.MODEL) {
                    for (RenderType rt : model.getRenderTypes(state, RandomSource.create(42), modelData)) {
                        modelBlockRenderer.renderModel(
                                stack.last(),
                                bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(rt, false)),
                                state,
                                model,
                                1.0f,
                                1.0f,
                                1.0f,
                                15728880,
                                OverlayTexture.NO_OVERLAY,
                                modelData,
                                rt
                        );
                    }
                } else {
                    // Render like usual for any other shapes
                    blockRenderer.renderSingleBlock(
                            state,
                            stack,
                            bufferSource,
                            15728880,
                            OverlayTexture.NO_OVERLAY,
                            modelData,
                            null
                    );
                }
            } catch (Exception e) {
                System.err.print(e.getMessage());
                // Skip problematic blocks to prevent crashes
            }

            stack.popPose();
        }

        // Finish rendering
        bufferSource.endBatch();
    }

}

