package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import igentuman.mbtool.client.handler.ClientHandler;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
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

    // --- Baked geometry cache ------------------------------------------------
    private static final StructureMesh mesh = new StructureMesh();
    private static final FakeStructureLevel fakeLevel = new FakeStructureLevel();
    private static MultiblockStructure builtStructure = null;
    private static int builtRotation = -1;

    public static BlockPos getRayTraceHit() {
        Player player = mc.player;
        Level world = mc.level;
        
        if (player == null || world == null) return null;
        
        // Perform raycast for 64 blocks
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(64.0));
        
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

        boolean main = !mainItem.isEmpty() && mainItem.is(MBTOOL.get()) && ClientHandler.hasStructure(mainItem);
        boolean off = !offItem.isEmpty() && offItem.is(MBTOOL.get()) && ClientHandler.hasStructure(offItem);

        if (!main && !off) return null;

        hit = rayTrace.getBlockPos();
        BlockState state = world.getBlockState(hit);

        // Get the selected structure
        ItemStack multibuilderStack = main ? mainItem : offItem;
        structure = ((MultibuilderItem)multibuilderStack.getItem()).getCurrentStructure(multibuilderStack);

        if (structure == null) return null;
        
        // Get rotation from item (if supported in the future)
        rotation = multibuilderStack.getOrDefault(MbtoolDataComponents.STRUCTURE_ROTATION.get(), 0);

        // Calculate placement position based on hit side with new pivot logic
        Direction hitSide = rayTrace.getDirection();
        
        switch (hitSide) {
            case DOWN:
                // Looking at ground: center horizontally, bottom block at hit position
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, -structure.getHeight(), 0);
                }
                // Center horizontally based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
                }
                break;
            case UP:
                // Looking at top: center horizontally, top block at hit position
                hit = hit.offset(0, 1, 0);
                // Center horizontally based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
                }
                break;
            case EAST:
                // Looking at side: place structure adjacent to the hit face, center vertically and on Z axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(1, 0, 0);
                }
                // Center on Z axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(0, -structure.getHeight() / 2, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(0, -structure.getHeight() / 2, -structure.getWidth() / 2);
                }
                break;
            case WEST:
                // Looking at side: place structure adjacent to the hit face, center vertically and on Z axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(-1, 0, 0);
                }

                // Center on Z axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() + 1,-structure.getHeight() / 2, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() + 1, -structure.getHeight() / 2, -structure.getWidth() / 2);
                }
                break;
            case NORTH:
                // Looking at side: place structure adjacent to the hit face, center vertically and on X axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 0, -1);
                }
                // Center on X axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, -structure.getHeight() / 2, -structure.getDepth() + 1);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, -structure.getHeight() / 2, -structure.getWidth()+1);
                }
                break;
            case SOUTH:
                // Looking at side: place structure adjacent to the hit face, center vertically and on X axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 0, 1);
                }
                // Center on X axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, -structure.getHeight() / 2, 0);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, -structure.getHeight() / 2, 0);
                }
                break;
        }

        return hit;
    }

    public static boolean renderPreview(PoseStack poseStack, float partialTicks) {
        BlockPos hitPos = getRayTraceHit();
        Player player = mc.player;

        if (hitPos == null || structure == null || player == null) return false;

        if (player.distanceToSqr(hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5) < 8.0) return false;

        // Rebuild baked geometry only when the selected structure or its rotation changes.
        if (structure != builtStructure || rotation != builtRotation) {
            rebuildMesh();
        }

        // Advance the pulsing-alpha animation.
        interpolatedAlpha += partialTicks * dir;
        if (interpolatedAlpha >= 0.8f) dir = -0.005f;
        if (interpolatedAlpha <= 0.5f) dir = 0.005f;

        poseStack.pushPose();

        height = structure.getHeight();
        length = structure.getDepth();
        width = structure.getWidth();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(hitPos.getX() - cameraPos.x, hitPos.getY() - cameraPos.y, hitPos.getZ() - cameraPos.z);

        // Combine GL model-view (camera rotation) with poseStack (translation offset) so the
        // VBO drawWithShader call gets the full transform — BufferUploader.drawWithShader applies
        // GL state automatically, but vbo.drawWithShader needs explicit matrices.
        org.joml.Matrix4f mv = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());

        // Baked blocks (VBOs) first, boundary wireframe on top.
        mesh.draw(mv, RenderSystem.getProjectionMatrix(), interpolatedAlpha);
        renderBoundaries(poseStack);

        poseStack.popPose();
        return true;
    }

    /**
     * Populates the fake level with rotated structure blocks (local coords, origin at 0,0,0)
     * and re-bakes the mesh. Rotation of positions and directional block states reuses the
     * same logic the block-by-block renderer used, so placement matches {@code getRayTraceHit}.
     */
    private static void rebuildMesh() {
        fakeLevel.clear();

        Map<BlockPos, BlockState> blocks = structure.getBlocks();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockState blockState = entry.getValue();
            if (blockState.isAir()) continue;

            BlockPos structurePos = entry.getKey();
            int xo = structurePos.getX() - structure.getMinX();
            int yo = structurePos.getY() - structure.getMinY();
            int zo = structurePos.getZ() - structure.getMinZ();

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

            fakeLevel.put(new BlockPos(rotatedX, yo, rotatedZ), rotateBlockState(blockState, rotation));
        }

        try {
            mesh.rebuild(fakeLevel);
        } catch (Exception e) {
            // A bad model can throw during tessellation; drop the mesh and skip drawing it.
            mesh.close();
        }
        // Mark as built regardless so a persistent failure doesn't re-bake every frame.
        builtStructure = structure;
        builtRotation = rotation;
    }

    private static void renderBoundaries(PoseStack poseStack) {
        Level world = mc.level;
        if (world == null) return;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
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

                    if (!isEmpty || ((w == 0 || w == bWidth - 1) && (l == 0 || l == bLength - 1) && (h == 0 || h == height - 1))) {
                        float r = isEmpty ? 0.0f : 1.0f;
                        float g = isEmpty ? 1.0f : 0.0f;
                        float b = 0.0f;
                        float alpha = 0.4f;

                        float x = xo + 0.5f;
                        float y = h + 0.5f;
                        float z = zo + 0.5f;

                        // Draw wireframe cube
                        if (!isEmpty || h == height - 1) { // top face
                            buffer.addVertex(matrix, x - 0.5f, y + 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y + 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y + 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y + 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y + 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y + 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y + 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y + 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                        }

                        if (!isEmpty) { // vertical edges
                            buffer.addVertex(matrix, x - 0.5f, y + 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y - 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y + 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y - 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y + 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y - 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y + 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y - 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                        }

                        if (!isEmpty || h == 0) { // bottom face
                            buffer.addVertex(matrix, x - 0.5f, y - 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y - 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y - 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y - 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x + 0.5f, y - 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y - 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y - 0.5f, z + 0.5f).setColor(r, g, b, alpha);
                            buffer.addVertex(matrix, x - 0.5f, y - 0.5f, z - 0.5f).setColor(r, g, b, alpha);
                        }
                    }
                }
            }
        }

        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
    }

    /**
     * Rotates a block state's directional properties based on the given rotation (0-3, representing 90-degree increments)
     */
    private static BlockState rotateBlockState(BlockState blockState, int rotation) {
        if (rotation == 0) return blockState;

        BlockState rotatedState = blockState;

        // Check all properties of the block state
        for (Property<?> property : blockState.getProperties()) {
            if (property instanceof DirectionProperty) {
                DirectionProperty dirProperty = (DirectionProperty) property;
                Direction currentDirection = blockState.getValue(dirProperty);
                Direction rotatedDirection = rotateDirection(currentDirection, rotation, dirProperty);

                // Only update if the rotated direction is valid for this property
                if (dirProperty.getPossibleValues().contains(rotatedDirection)) {
                    rotatedState = rotatedState.setValue(dirProperty, rotatedDirection);
                }
            }
        }

        return rotatedState;
    }

    /**
     * Rotates a direction based on the rotation amount and property constraints
     */
    private static Direction rotateDirection(Direction direction, int rotation, DirectionProperty property) {
        // Normalize rotation to 0-3 range
        rotation = ((rotation % 4) + 4) % 4;

        // For horizontal-only properties, only rotate around Y-axis
        boolean isHorizontalOnly = property.getPossibleValues().stream()
                .allMatch(dir -> dir.getAxis() != Direction.Axis.Y);

        if (isHorizontalOnly && (direction == Direction.UP || direction == Direction.DOWN)) {
            return direction; // Don't rotate vertical directions for horizontal-only properties
        }

        Direction result = direction;
        for (int i = 0; i < rotation; i++) {
            result = rotateDirectionClockwise(result);
        }

        return result;
    }

    /**
     * Rotates a direction 90 degrees clockwise around the Y-axis
     */
    private static Direction rotateDirectionClockwise(Direction direction) {
        switch (direction) {
            case NORTH: return Direction.EAST;
            case EAST: return Direction.SOUTH;
            case SOUTH: return Direction.WEST;
            case WEST: return Direction.NORTH;
            default: return direction; // UP / DOWN unchanged
        }
    }
}
