package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Renders a {@link MultiblockStructure} into a GUI.
 *
 * <p>Static geometry is baked into a transparent GPU texture on first display. Subsequent frames
 * draw a single textured quad; temporary mesh buffers are freed immediately after baking.
 * Snapshots survive screen changes and are invalidated on resource reload or provider revision.</p>
 *
 * <p>Note: block-entity renderers are no longer invoked for GUI previews (the cached mesh only
 * holds static block models). Dynamic BE geometry — chest lids, sign text, etc. — is not shown.</p>
 */
public class MultiblockRenderer {

    public static MultiblockStructure structure;

    private static final int MIN_CACHED = 64;
    private static final long MAX_CACHE_BYTES = 64L * 1024 * 1024;
    private static final int BAKES_PER_FRAME = 2;
    private static int remainingBakes = BAKES_PER_FRAME;
    private static final PreviewCache<ImageKey, StructureThumbnail> IMAGE_CACHE =
            new PreviewCache<>(MIN_CACHED, MAX_CACHE_BYTES, StructureThumbnail::close);

    record ImageKey(MeshKey<BlockPos, BlockState> geometry, int width, int height, StructureThumbnail.Size pixels) {}

    private static final Map<MultiblockStructure, MeshKey<BlockPos, BlockState>> STRUCTURE_KEYS = new WeakHashMap<>();
    private static int sourceRevision = -1;

    /** Immutable geometry snapshot, with its hash computed only once for repeated render calls. */
    static final class MeshKey<P, S> {
        private final Map<P, S> blocks;
        private final int hash;

        MeshKey(Map<P, S> blocks) {
            this.blocks = Map.copyOf(blocks);
            hash = this.blocks.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof MeshKey<?, ?> key && blocks.equals(key.blocks);
        }
    }

    /** Spread cold-cache work across frames instead of baking the entire visible grid at once. */
    public static void beginFrame() {
        remainingBakes = BAKES_PER_FRAME;
    }

    /** Allow all visible cards to remain resident, subject to the GPU memory limit. */
    public static void setVisiblePreviewCount(int count) {
        IMAGE_CACHE.setCapacity(Math.max(MIN_CACHED, count));
    }

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

    public static void render(MultiblockStructure blocksMap, GuiGraphics graphics, int x, int y, int w, int h) {
        // Finish queued card backgrounds before switching framebuffers or drawing the snapshot.
        graphics.flush();
        render(blocksMap, graphics.pose(), x, y, w, h);
    }

    public static void render(PoseStack stack, int x, int y, int w, int h) {
        if (structure == null || structure.getBlocks().isEmpty() || w <= 0 || h <= 0) {
            return;
        }
        if (sourceRevision != MultiblocksProvider.getRevision()) {
            MultiblockStructure current = structure;
            invalidateCache();
            structure = current;
            sourceRevision = MultiblocksProvider.getRevision();
        }
        MeshKey<BlockPos, BlockState> geometry = STRUCTURE_KEYS.computeIfAbsent(structure, value -> new MeshKey<>(value.getBlocks()));
        StructureThumbnail.Size pixels = StructureThumbnail.Size.forGui(w, h, Minecraft.getInstance().getWindow().getGuiScale());
        ImageKey key = new ImageKey(geometry, w, h, pixels);
        StructureThumbnail image = IMAGE_CACHE.get(key);
        if (image == null) {
            if (remainingBakes <= 0) return;
            remainingBakes--;
            StructureMesh mesh = bakeMesh(structure);
            try {
                image = StructureThumbnail.bake(pixels, target -> drawMesh(mesh, target, w, h));
            } finally {
                mesh.close();
            }
            IMAGE_CACHE.put(key, image, image.allocationBytes());
        }
        image.draw(stack, x, y, w, h);
    }

    private static void drawMesh(StructureMesh mesh, RenderTarget target, int w, int h) {
        if (mesh.isEmpty()) return;
        PoseStack stack = new PoseStack();

        // Use local GUI coordinates, independent of the card's position, scroll, and clipping.
        stack.translate(w / 2.0f, h / 2.0f, -1900.0f);

        // Calculate appropriate scale to fit within the provided dimensions
        float maxDimension = Math.max(Math.max(structure.getWidth(), structure.getHeight()), structure.getDepth());
        float baseScale = Math.min(w, h) * 0.9f; // Use 90% of available space
        float scale = baseScale / maxDimension;
        stack.scale(scale, -scale, scale); // Negative Y scale to match GUI coordinates

        // Apply isometric-style rotation for better viewing angle
        stack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(30)));
        stack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-45)));

        // Inner structure-fit scale (matches the old log-based factor) and centering. The mesh is
        // baked at absolute structure coords, so shift by the structure center here.
        int width = structure.getWidth();
        int height = structure.getHeight();
        int depth = structure.getDepth();
        float fitScale = (float) (1.2f / (Math.log10(Math.max(Math.max(width, height), depth) + 105)));
        stack.scale(fitScale, fitScale, fitScale);
        stack.translate(
                -(structure.getMinX() + width / 2.0f),
                -(structure.getMinY() + height / 2.0f),
                -(structure.getMinZ() + depth / 2.0f)
        );

        Matrix4f projection = new Matrix4f().setOrtho(0, w, h, 0, 1000, 3000);
        mesh.draw(stack.last().pose(), projection, 1.0f, true, target);
    }

    /** Temporary geometry, used only while rendering a new snapshot. */
    private static StructureMesh bakeMesh(MultiblockStructure structure) {
        // Build a fake level from the structure's absolute positions so tesselateBlock can resolve
        // neighbours for CTM and internal face culling.
        FakeStructureLevel level = new FakeStructureLevel();
        for (Map.Entry<BlockPos, BlockState> e : structure.getBlocks().entrySet()) {
            if (e.getValue().isAir()) continue;
            level.put(e.getKey(), e.getValue());
        }

        StructureMesh mesh = new StructureMesh();
        try {
            mesh.rebuild(level);
        } catch (Exception ex) {
            // A bad model can throw during tessellation; drop it and cache the empty mesh so we
            // don't re-bake every frame.
            mesh.close();
        }
        return mesh;
    }

    /** Drops all snapshots and frees their GPU textures on resource reload. */
    public static void invalidateCache() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(MultiblockRenderer::invalidateCache);
            return;
        }
        IMAGE_CACHE.clear();
        STRUCTURE_KEYS.clear();
        structure = null;
    }
}
