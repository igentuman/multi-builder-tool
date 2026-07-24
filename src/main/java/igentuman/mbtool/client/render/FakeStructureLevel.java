package igentuman.mbtool.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal {@link net.minecraft.client.renderer.block.BlockAndTintGetter} backed by a plain map.
 *
 * Positions outside the structure resolve to AIR, so the block renderer culls internal
 * faces between structure blocks but keeps every boundary face. Passing this view to
 * {@link net.minecraft.client.renderer.block.ModelBlockRenderer#tesselateBlock} lets the
 * tessellator perform correct neighbour-based face culling.
 *
 * Light queries return full-bright so the preview is always visible.
 */
public class FakeStructureLevel implements net.minecraft.client.renderer.block.BlockAndTintGetter {

    private final Map<BlockPos, BlockState> blocks = new HashMap<>();

    public void clear() {
        blocks.clear();
    }

    public void put(BlockPos pos, BlockState state) {
        blocks.put(pos.immutable(), state);
    }

    public Map<BlockPos, BlockState> getBlocks() {
        return blocks;
    }

    // ---- BlockAndTintGetter (net.minecraft.client.renderer.block) ----

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return resolver.getColor(mc.level.getBiome(pos).value(), pos.getX(), pos.getZ());
        }
        return 0xFFFFFF;
    }

    // ---- BlockAndLightGetter (net.minecraft.world.level) ----

    @Override
    public LevelLightEngine getLightEngine() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getLightEngine() : null;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int minLight) {
        return 15;
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return false;
    }

    // ---- BlockGetter (net.minecraft.world.level) ----

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    // ---- LevelHeightAccessor ----

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
