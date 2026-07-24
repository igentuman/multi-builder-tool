package igentuman.mbtool.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders a {@link MultiblockStructure} into a GUI.
 *
 * <p>Geometry is baked once per structure into a {@link StructureMesh} (GPU vertex buffers) and
 * drawn straight from GPU memory every frame. This replaces the old single-static-{@code fakeLevel}
 * path, which re-tessellated every block every frame and — with several structures on screen at
 * once (e.g. the structure-picker button list) — rebuilt the shared level for <em>every</em>
 * structure <em>every</em> frame because the single {@code builtStructure} guard could never
 * match more than one of them.</p>
 *
 * <p>Note: block-entity renderers are no longer invoked for GUI previews (the cached mesh only
 * holds static block models). Dynamic BE geometry — chest lids, sign text, etc. — is not shown.</p>
 */
public class MultiblockRenderer {

    public static MultiblockStructure structure;

    /** Bounded LRU cache of baked meshes keyed by a stable structure signature. */
    private static final int MAX_CACHED = 64;
    private static final LinkedHashMap<String, StructureMesh> MESH_CACHE =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, StructureMesh> eldest) {
                    if (size() > MAX_CACHED) {
                        eldest.getValue().close(); // free the evicted VBOs
                        return true;
                    }
                    return false;
                }
            };

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

        StructureMesh mesh = getOrBakeMesh(structure);
        if (mesh == null || mesh.isEmpty()) {
            return;
        }

        stack.pushPose();

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

        // Draw the baked mesh straight from the GPU. Solid (alpha 1) with depth test so front
        // blocks occlude back ones. The on-screen transform in a GUI is the RenderSystem
        // model-view combined with the GuiGraphics pose (unlike the in-world preview, where the
        // RenderSystem model-view is identity), so multiply both here.
        org.joml.Matrix4f modelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix());
        modelView.mul(stack.last().pose());
        mesh.draw(modelView, RenderSystem.getProjectionMatrix(), 1.0f, true);

        stack.popPose();
    }

    /** Returns the cached mesh for the structure, baking it on first use / cache miss. */
    private static StructureMesh getOrBakeMesh(MultiblockStructure structure) {
        String key = cacheKey(structure);
        StructureMesh cached = MESH_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

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
        MESH_CACHE.put(key, mesh);
        return mesh;
    }

    /**
     * Stable cache key. {@code getCurrentStructure} may hand back a fresh {@link MultiblockStructure}
     * instance each frame, so object identity is unusable — key on name plus dimensions and block
     * count instead.
     */
    private static String cacheKey(MultiblockStructure structure) {
        String name = structure.getName();
        return (name == null ? "?" : name)
                + '|' + structure.getWidth() + 'x' + structure.getHeight() + 'x' + structure.getDepth()
                + '|' + structure.getBlocks().size();
    }

    /** Drops all cached meshes and frees their VBOs (call on resource reload if wired up). */
    public static void invalidateCache() {
        for (Iterator<StructureMesh> it = MESH_CACHE.values().iterator(); it.hasNext(); ) {
            it.next().close();
        }
        MESH_CACHE.clear();
    }
}
