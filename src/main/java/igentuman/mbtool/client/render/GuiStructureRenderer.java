package igentuman.mbtool.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws a {@link MultiblockStructure} as a real block-model mesh into the offscreen texture the
 * base class provides, instead of the flat item-icon approach previously used by MultiblockButton.
 * Reuses the same {@link StructureMesh}/{@link FakeStructureLevel} tessellation PreviewRenderer
 * uses for the in-world ghost preview; meshes are cached per structure since structures are stable,
 * shared objects (recipe/registry singletons), not rebuilt per frame or per button.
 */
public class GuiStructureRenderer extends PictureInPictureRenderer<GuiStructureRenderState> {

    private static final Map<MultiblockStructure, StructureMesh> MESH_CACHE = new IdentityHashMap<>();

    public GuiStructureRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<GuiStructureRenderState> getRenderStateClass() {
        return GuiStructureRenderState.class;
    }

    @Override
    protected void renderToTexture(GuiStructureRenderState state, PoseStack poseStack) {
        MultiblockStructure structure = state.structure();
        StructureMesh mesh = MESH_CACHE.computeIfAbsent(structure, GuiStructureRenderer::buildMesh);
        if (mesh.isEmpty()) return;

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        float w = structure.getWidth();
        float h = structure.getHeight();
        float d = structure.getDepth();

        // mesh.draw() ignores the PoseStack argument and reads RenderSystem's own model-view
        // stack instead (see StructureMesh/PreviewRenderer), so the base class's offscreen
        // translate+scale must be folded in there too before applying our own rotation.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(poseStack.last().pose());
        // Matches vanilla's own block-icon transform (assets/minecraft/models/block/block.json
        // "display.gui": rotation [30, 225, 0]) — no mirroring needed, just this exact rotation.
        modelView.rotateX((float) Math.toRadians(30));
        modelView.rotateY((float) Math.toRadians(225) + state.angleY());
        modelView.translate(-w / 2f, -h / 2f, -d / 2f);

        mesh.draw(1.0f);

        modelView.popMatrix();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "mbtool_structure";
    }

    private static StructureMesh buildMesh(MultiblockStructure structure) {
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
            mesh.close();
        }
        return mesh;
    }
}
