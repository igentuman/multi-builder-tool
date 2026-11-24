package igentuman.mbtool.client.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a {@link MultiblockStructure} as a real block-model mesh.
 *
 * <p>Rotating states (JEI, FTB widgets) render into the pooled per-renderer texture every frame,
 * as vanilla picture-in-picture renderers do. Static browser thumbnails are instead baked once
 * into their own texture, kept in a memory-bounded LRU {@link PreviewCache} keyed by geometry and
 * pixel size, and only blitted afterwards. Cold-cache baking is limited per frame so opening a
 * large catalogue does not tessellate every preview at once.</p>
 *
 * <p>Thumbnails contain static block models only; block-entity renderers are not invoked.</p>
 */
public class GuiStructureRenderer extends PictureInPictureRenderer<GuiStructureRenderState> {

    private static final int MIN_CACHED = 64;
    private static final long MAX_CACHE_BYTES = 64L * 1024 * 1024;
    private static final int BAKES_PER_FRAME = 2;
    private static final int MAX_LIVE_MESHES = 16;

    private static int remainingBakes = BAKES_PER_FRAME;
    private static final PreviewCache<ThumbnailKey, Thumbnail> THUMBNAILS =
        new PreviewCache<>(MIN_CACHED, MAX_CACHE_BYTES, Thumbnail::close);
    private static final Map<MultiblockStructure, GeometryKey> GEOMETRY_KEYS = new WeakHashMap<>();
    /** Meshes for per-frame (rotating) previews; small LRU since they hold CPU vertex copies. */
    private static final Map<MultiblockStructure, StructureMesh> LIVE_MESHES = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MultiblockStructure, StructureMesh> eldest) {
            if (size() > MAX_LIVE_MESHES) {
                eldest.getValue().close();
                return true;
            }
            return false;
        }
    };

    private static final java.lang.reflect.Field TEXTURE_VIEW = findTextureViewField();

    private static java.lang.reflect.Field findTextureViewField() {
        try {
            java.lang.reflect.Field field = PictureInPictureRenderer.class.getDeclaredField("textureView");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private GpuTextureView currentTextureView() {
        try {
            return TEXTURE_VIEW == null ? null : (GpuTextureView) TEXTURE_VIEW.get(this);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private final Projection bakeProjection = new Projection();
    private final ProjectionMatrixBuffer bakeProjectionBuffer = new ProjectionMatrixBuffer("PIP - mbtool thumbnail bake");

    public GuiStructureRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    public static void beginFrame() {
        remainingBakes = BAKES_PER_FRAME;
    }

    public static void setVisiblePreviewCount(int count) {
        THUMBNAILS.setCapacity(Math.max(MIN_CACHED, count));
    }

    public static void invalidateCache() {
        THUMBNAILS.clear();
        LIVE_MESHES.values().forEach(StructureMesh::close);
        LIVE_MESHES.clear();
        GEOMETRY_KEYS.clear();
    }

    @Override
    public Class<GuiStructureRenderState> getRenderStateClass() {
        return GuiStructureRenderState.class;
    }

    @Override
    public boolean canBeReusedFor(GuiStructureRenderState state, int textureWidth, int textureHeight) {
        // Cached states never touch this renderer's own texture.
        return state.cached() || super.canBeReusedFor(state, textureWidth, textureHeight);
    }

    @Override
    public void prepare(GuiStructureRenderState state, GuiRenderState guiRenderState, int guiScale) {
        if (!state.cached()) {
            super.prepare(state, guiRenderState, guiScale);
            return;
        }
        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;
        if (width <= 0 || height <= 0 || state.structure().getBlocks().isEmpty()) return;

        GeometryKey geometry = GEOMETRY_KEYS.computeIfAbsent(state.structure(), s -> new GeometryKey(s.getBlocks()));
        ThumbnailKey key = new ThumbnailKey(geometry, width, height,
            Float.floatToIntBits(state.scale() * guiScale), Float.floatToIntBits(state.angleY()));
        Thumbnail thumbnail = THUMBNAILS.get(key);
        if (thumbnail == null) {
            long bytes = (long) width * height * 4;
            if (remainingBakes <= 0 || !THUMBNAILS.fits(bytes)) return;
            remainingBakes--;
            thumbnail = bake(state, width, height, guiScale);
            THUMBNAILS.put(key, thumbnail, bytes);
        }
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(thumbnail.view(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            state.pose(),
            state.x0(), state.y0(), state.x1(), state.y1(),
            0.0F, 1.0F, 1.0F, 0.0F,
            state.tint(),
            state.scissorArea(),
            null
        ));
    }

    private Thumbnail bake(GuiStructureRenderState state, int width, int height, int guiScale) {
        GpuDevice device = RenderSystem.getDevice();
        GpuTexture color = device.createTexture(() -> "mbtool structure thumbnail",
            GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.RGBA8, width, height, 1, 1);
        GpuTextureView colorView = device.createTextureView(color);
        TextureFormat depthFormat = Minecraft.getInstance().getMainRenderTarget().getDepthTexture().getFormat();
        GpuTexture depth = device.createTexture(() -> "mbtool structure thumbnail depth",
            GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT, depthFormat, width, height, 1, 1);
        GpuTextureView depthView = device.createTextureView(depth);
        try {
            device.createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);
            bakeProjection.setupOrtho(-1000.0F, 1000.0F, width, height, true);
            RenderSystem.setProjectionMatrix(bakeProjectionBuffer.getBuffer(bakeProjection), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.outputColorTextureOverride = colorView;
            RenderSystem.outputDepthTextureOverride = depthView;
            PoseStack poseStack = new PoseStack();
            poseStack.translate(width / 2.0F, getTranslateY(height, guiScale), 0.0F);
            float scale = guiScale * state.scale();
            poseStack.scale(scale, scale, -scale);
            StructureMesh mesh = buildMesh(state.structure());
            try {
                drawMesh(mesh, state, poseStack);
            } finally {
                mesh.close();
            }
            bufferSource.endBatch();
        } catch (RuntimeException | Error ex) {
            colorView.close();
            color.close();
            throw ex;
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            depthView.close();
            depth.close();
        }
        return new Thumbnail(color, colorView);
    }

    @Override
    protected void blitTexture(GuiStructureRenderState state, GuiRenderState guiRenderState) {
        if (state.tint() == -1) {
            super.blitTexture(state, guiRenderState);
            return;
        }
        // Same blit as the base class, but carrying the state's tint (e.g. FTB chapter image color/alpha).
        GpuTextureView view = currentTextureView();
        if (view == null) {
            super.blitTexture(state, guiRenderState);
            return;
        }
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            state.pose(), state.x0(), state.y0(), state.x1(), state.y1(),
            0.0F, 1.0F, 1.0F, 0.0F, state.tint(), state.scissorArea(), null));
    }

    @Override
    protected void renderToTexture(GuiStructureRenderState state, PoseStack poseStack) {
        StructureMesh mesh = LIVE_MESHES.computeIfAbsent(state.structure(), GuiStructureRenderer::buildMesh);
        drawMesh(mesh, state, poseStack);
    }

    private static void drawMesh(StructureMesh mesh, GuiStructureRenderState state, PoseStack poseStack) {
        if (mesh.isEmpty()) return;
        MultiblockStructure structure = state.structure();

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        float w = structure.getWidth();
        float h = structure.getHeight();
        float d = structure.getDepth();

        // mesh.draw() ignores the PoseStack argument and reads RenderSystem's own model-view
        // stack instead (see StructureMesh/PreviewRenderer), so the offscreen translate+scale
        // must be folded in there too before applying our own rotation.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.mul(poseStack.last().pose());
            // Matches vanilla's own block-icon transform (assets/minecraft/models/block/block.json
            // "display.gui": rotation [30, 225, 0]) — no mirroring needed, just this exact rotation.
            modelView.rotateX((float) Math.toRadians(30));
            modelView.rotateY((float) Math.toRadians(225) + state.angleY());
            modelView.translate(-w / 2f, -h / 2f, -d / 2f);
            mesh.draw(1.0f);
        } finally {
            modelView.popMatrix();
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "mbtool_structure";
    }

    @Override
    public void close() {
        super.close();
        bakeProjectionBuffer.close();
    }

    static StructureMesh buildMesh(MultiblockStructure structure) {
        FakeStructureLevel level = new FakeStructureLevel();
        int width = structure.getWidth();
        int height = structure.getHeight();
        for (Map.Entry<BlockPos, BlockState> entry : structure.getBlocks().entrySet()) {
            BlockState blockState = entry.getValue();
            if (blockState.isAir()) continue;

            BlockPos pos = entry.getKey();
            int xo = pos.getX() - structure.getMinX();
            int yo = pos.getY() - structure.getMinY();
            // Flip X and Y here (mesh data) rather than in the view transform: a reflection in
            // the model-view matrix would invert triangle winding and break backface culling.
            level.put(new BlockPos(
                width - 1 - xo,
                height - 1 - yo,
                pos.getZ() - structure.getMinZ()
            ), blockState);
        }

        StructureMesh mesh = new StructureMesh();
        try {
            mesh.rebuild(level);
        } catch (Exception e) {
            // A bad model can throw during tessellation; keep the empty mesh so it is not retried every frame.
            mesh.close();
        }
        return mesh;
    }

    private record Thumbnail(GpuTexture texture, GpuTextureView view) {
        void close() {
            view.close();
            texture.close();
        }
    }

    private record ThumbnailKey(GeometryKey geometry, int width, int height, int scaleBits, int angleBits) {}

    private static final class GeometryKey {
        private final Map<BlockPos, BlockState> blocks;
        private final int hash;

        GeometryKey(Map<BlockPos, BlockState> blocks) {
            this.blocks = Map.copyOf(blocks);
            this.hash = this.blocks.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof GeometryKey key && hash == key.hash && blocks.equals(key.blocks);
        }
    }
}
