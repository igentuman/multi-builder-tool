package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import igentuman.mbtool.client.handler.ClientHandler;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Map;

import static igentuman.mbtool.Mbtool.MBTOOL;

public class PreviewRenderer {

    private static float interpolatedAlpha = 0.5F;
    private static MultiblockStructure structure = null;
    private static final Minecraft mc = Minecraft.getInstance();
    private static int height;
    private static int length;
    private static int width;
    private static int rotation = 0;
    private static BlockPos hit;
    private static float dir = 0.005f;

    private static final StructureMesh mesh = new StructureMesh();
    private static final FakeStructureLevel fakeLevel = new FakeStructureLevel();
    private static MultiblockStructure builtStructure = null;
    private static int builtRotation = -1;

    public static BlockPos getRayTraceHit() {
        Player player = mc.player;
        Level world = mc.level;

        if (player == null || world == null) return null;

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

        ItemStack multibuilderStack = main ? mainItem : offItem;
        structure = ((MultibuilderItem)multibuilderStack.getItem()).getCurrentStructure(multibuilderStack);

        if (structure == null) return null;

        rotation = multibuilderStack.getOrDefault(MbtoolDataComponents.STRUCTURE_ROTATION.get(), 0);

        Direction hitSide = rayTrace.getDirection();

        switch (hitSide) {
            case DOWN:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, -structure.getHeight(), 0);
                }
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
                }
                break;
            case UP:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 1, 0);
                }
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
                }
                break;
            case EAST:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(1, 0, 0);
                }
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(0, -structure.getHeight() / 2, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(0, -structure.getHeight() / 2, -structure.getWidth() / 2);
                }
                break;
            case WEST:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(-1, 0, 0);
                }
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() + 1, -structure.getHeight() / 2, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() + 1, -structure.getHeight() / 2, -structure.getWidth() / 2);
                }
                break;
            case NORTH:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 0, -1);
                }
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, -structure.getHeight() / 2, -structure.getDepth() + 1);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, -structure.getHeight() / 2, -structure.getWidth() + 1);
                }
                break;
            case SOUTH:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 0, 1);
                }
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

        if (structure != builtStructure || rotation != builtRotation) {
            rebuildMesh();
        }

        interpolatedAlpha += partialTicks * dir;
        if (interpolatedAlpha >= 0.8f) dir = -0.005f;
        if (interpolatedAlpha <= 0.5f) dir = 0.005f;

        poseStack.pushPose();

        height = structure.getHeight();
        length = structure.getDepth();
        width = structure.getWidth();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        poseStack.translate(hitPos.getX() - cameraPos.x, hitPos.getY() - cameraPos.y, hitPos.getZ() - cameraPos.z);

        // mesh.draw() calls RenderType.draw(MeshData) directly with pre-baked structure-local
        // vertex bytes; it ignores our PoseStack entirely and only respects RenderSystem's own
        // GL model-view matrix, so the hit-position translate must be pushed there too.
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.translate((float) (hitPos.getX() - cameraPos.x), (float) (hitPos.getY() - cameraPos.y), (float) (hitPos.getZ() - cameraPos.z));

        mesh.draw(interpolatedAlpha);

        modelViewStack.popMatrix();

        renderBoundaries(poseStack);

        poseStack.popPose();
        return true;
    }

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
            builtStructure = structure;
            builtRotation = rotation;
        } catch (Exception e) {
            mesh.close();
            // Leave builtStructure/builtRotation unset so the next frame retries
            // instead of permanently caching a failed (empty) mesh.
            builtStructure = null;
            builtRotation = -1;
        }
    }

    private static void renderBoundaries(PoseStack poseStack) {
        Level world = mc.level;
        if (world == null) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.LINES);
        Matrix4f matrix = poseStack.last().pose();

        int bWidth = length;
        int bLength = width;

        for (int h = 0; h < height; h++) {
            for (int l = 0; l < bLength; l++) {
                for (int w = 0; w < bWidth; w++) {
                    int xo = l;
                    int zo = w;

                    switch (rotation) {
                        case 1: zo = l; xo = (bWidth - w - 1); break;
                        case 2: xo = (bLength - l - 1); zo = (bWidth - w - 1); break;
                        case 3: zo = (bLength - l - 1); xo = w; break;
                    }

                    BlockPos actualPos = hit.offset(xo, h, zo);
                    boolean isEmpty = world.getBlockState(actualPos).canBeReplaced();

                    if (!isEmpty || ((w == 0 || w == bWidth - 1) && (l == 0 || l == bLength - 1) && (h == 0 || h == height - 1))) {
                        float r = isEmpty ? 0.0f : 1.0f;
                        float g = isEmpty ? 1.0f : 0.0f;
                        float b = 0.0f;
                        float alpha = 0.6f;
                        int color = ((int)(alpha*255)<<24)|((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);

                        float x = xo + 0.5f;
                        float y = h + 0.5f;
                        float z = zo + 0.5f;

                        if (!isEmpty || h == height - 1) {
                            addLineSegment(buffer, matrix, x-0.5f, y+0.5f, z-0.5f, x+0.5f, y+0.5f, z-0.5f, color);
                            addLineSegment(buffer, matrix, x+0.5f, y+0.5f, z-0.5f, x+0.5f, y+0.5f, z+0.5f, color);
                            addLineSegment(buffer, matrix, x+0.5f, y+0.5f, z+0.5f, x-0.5f, y+0.5f, z+0.5f, color);
                            addLineSegment(buffer, matrix, x-0.5f, y+0.5f, z+0.5f, x-0.5f, y+0.5f, z-0.5f, color);
                        }
                        if (!isEmpty || h == 0) {
                            addLineSegment(buffer, matrix, x-0.5f, y-0.5f, z-0.5f, x+0.5f, y-0.5f, z-0.5f, color);
                            addLineSegment(buffer, matrix, x+0.5f, y-0.5f, z-0.5f, x+0.5f, y-0.5f, z+0.5f, color);
                            addLineSegment(buffer, matrix, x+0.5f, y-0.5f, z+0.5f, x-0.5f, y-0.5f, z+0.5f, color);
                            addLineSegment(buffer, matrix, x-0.5f, y-0.5f, z+0.5f, x-0.5f, y-0.5f, z-0.5f, color);
                        }
                        if (!isEmpty) {
                            addLineSegment(buffer, matrix, x-0.5f, y+0.5f, z-0.5f, x-0.5f, y-0.5f, z-0.5f, color);
                            addLineSegment(buffer, matrix, x+0.5f, y+0.5f, z-0.5f, x+0.5f, y-0.5f, z-0.5f, color);
                            addLineSegment(buffer, matrix, x-0.5f, y+0.5f, z+0.5f, x-0.5f, y-0.5f, z+0.5f, color);
                            addLineSegment(buffer, matrix, x+0.5f, y+0.5f, z+0.5f, x+0.5f, y-0.5f, z+0.5f, color);
                        }
                    }
                }
            }
        }

        bufferSource.endBatch(RenderTypes.LINES);
    }

    private static void addLineSegment(VertexConsumer buffer, Matrix4f matrix,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2, int color) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        float nx = dx/len, ny = dy/len, nz = dz/len;
        buffer.addVertex(matrix, x1, y1, z1).setColor(color).setNormal(nx, ny, nz).setLineWidth(2.0f);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color).setNormal(nx, ny, nz).setLineWidth(2.0f);
    }

    private static BlockState rotateBlockState(BlockState blockState, int rotation) {
        if (rotation == 0) return blockState;

        BlockState rotatedState = blockState;

        for (Property<?> property : blockState.getProperties()) {
            if (property instanceof EnumProperty<?> ep && ep.getValueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                EnumProperty<Direction> dirProperty = (EnumProperty<Direction>) ep;
                Direction currentDirection = blockState.getValue(dirProperty);
                Direction rotatedDirection = rotateDirection(currentDirection, rotation, dirProperty);

                if (dirProperty.getPossibleValues().contains(rotatedDirection)) {
                    rotatedState = rotatedState.setValue(dirProperty, rotatedDirection);
                }
            }
        }

        return rotatedState;
    }

    private static Direction rotateDirection(Direction direction, int rotation, EnumProperty<Direction> property) {
        rotation = ((rotation % 4) + 4) % 4;

        boolean isHorizontalOnly = property.getPossibleValues().stream()
            .allMatch(dir -> dir.getAxis() != Direction.Axis.Y);

        if (isHorizontalOnly && (direction == Direction.UP || direction == Direction.DOWN)) {
            return direction;
        }

        Direction result = direction;
        for (int i = 0; i < rotation; i++) {
            result = rotateDirectionClockwise(result, isHorizontalOnly);
        }

        return result;
    }

    private static Direction rotateDirectionClockwise(Direction direction, boolean horizontalOnly) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> direction;
        };
    }
}
