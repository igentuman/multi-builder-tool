package igentuman.mbtool.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tessellates a {@link FakeStructureLevel} into per-layer geometry once, then draws it every
 * frame. Replaces the old per-frame CPU tessellation and the 1.21.1 VertexBuffer approach
 * (which is unavailable in 26.1).
 *
 * {@link RenderType#draw(MeshData)} always closes the {@link MeshData} it's given (it's a
 * one-shot immediate-draw call, re-uploading to the GPU every time) so a {@link MeshData} can't
 * be cached and drawn across multiple frames. Instead the raw vertex bytes and {@link
 * MeshData.DrawState} are cached here, and a throwaway {@link MeshData} is rebuilt from them on
 * every draw call.
 *
 * Because tessellation goes through {@link ModelBlockRenderer#tesselateBlock} with a full
 * {@link BlockAndTintGetter} view, face culling and connected textures work correctly.
 */
public class StructureMesh {

    private final Map<ChunkSectionLayer, byte[]> vertexData = new EnumMap<>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, MeshData.DrawState> drawStates = new EnumMap<>(ChunkSectionLayer.class);

    /** Rebuilds all layer meshes from the supplied fake level. */
    public void rebuild(FakeStructureLevel level) {
        close();

        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer modelRenderer = new ModelBlockRenderer(true, true, mc.getBlockColors());
        RandomSource random = RandomSource.create();

        Map<ChunkSectionLayer, BufferBuilder> builders = new EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, ByteBufferBuilder> tessellationPools = new EnumMap<>(ChunkSectionLayer.class);

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            ByteBufferBuilder pool = new ByteBufferBuilder(layer.bufferSize());
            tessellationPools.put(layer, pool);
            builders.put(layer, new BufferBuilder(pool, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
        }

        for (Map.Entry<BlockPos, BlockState> entry : level.getBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (state.isAir()) continue;

            BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
            long seed = state.getSeed(pos);

            BlockQuadOutput output = (x, y, z, quad, instance) -> {
                ChunkSectionLayer layer = quad.materialInfo().layer();
                BufferBuilder builder = builders.get(layer);
                if (builder != null) {
                    builder.putBlockBakedQuad(x + pos.getX(), y + pos.getY(), z + pos.getZ(), quad, instance);
                }
            };

            try {
                modelRenderer.tesselateBlock(output, 0f, 0f, 0f, level, pos, state, model, seed);
            } catch (Exception ignored) {
                // Skip blocks whose models throw during tessellation
            }
        }

        for (Map.Entry<ChunkSectionLayer, BufferBuilder> entry : builders.entrySet()) {
            ChunkSectionLayer layer = entry.getKey();
            MeshData mesh = entry.getValue().build();
            if (mesh != null) {
                ByteBuffer buffer = mesh.vertexBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                vertexData.put(layer, bytes);
                drawStates.put(layer, mesh.drawState());
                mesh.close();
            }
            tessellationPools.remove(layer).close();
        }
    }

    public boolean isEmpty() {
        return vertexData.isEmpty();
    }

    /** Draws all layers. Alpha < 1 gives a translucent ghost effect. */
    public void draw(float alpha) {
        for (Map.Entry<ChunkSectionLayer, byte[]> entry : vertexData.entrySet()) {
            ChunkSectionLayer layer = entry.getKey();
            RenderType renderType = layerToRenderType(layer);
            if (renderType == null) continue;

            byte[] bytes = entry.getValue();
            MeshData.DrawState drawState = drawStates.get(layer);
            ByteBufferBuilder scratch = ByteBufferBuilder.exactlySized(bytes.length);
            try {
                long pointer = scratch.reserve(bytes.length);
                MemoryUtil.memByteBuffer(pointer, bytes.length).put(bytes);
                ByteBufferBuilder.Result result = scratch.build();
                renderType.draw(new MeshData(result, drawState));
            } finally {
                scratch.close();
            }
        }
    }

    public void close() {
        vertexData.clear();
        drawStates.clear();
    }

    private static RenderType layerToRenderType(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }
}
