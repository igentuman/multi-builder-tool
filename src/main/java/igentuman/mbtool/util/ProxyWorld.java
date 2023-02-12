package igentuman.mbtool.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;

import javax.annotation.Nullable;

public class ProxyWorld extends World {
    private final World realWorld;
    private IBlockAccess fakeWorld;

    public ProxyWorld(WorldClient realWorld) {
        super((ISaveHandler)null, realWorld.getWorldInfo(), realWorld.provider, realWorld.profiler, true);
        this.realWorld = realWorld;
        this.chunkProvider = realWorld.getChunkProvider();
    }

    public ProxyWorld() {
        super((ISaveHandler)null, Minecraft.getMinecraft().world.getWorldInfo(), Minecraft.getMinecraft().world.provider, Minecraft.getMinecraft().world.profiler, true);
        this.realWorld = Minecraft.getMinecraft().world;
        this.chunkProvider = Minecraft.getMinecraft().world.getChunkProvider();
    }

    public void setFakeWorld(IBlockAccess fakeWorld) {
        this.fakeWorld = fakeWorld;
    }

    protected IChunkProvider createChunkProvider() {
        return this.realWorld.getChunkProvider();
    }

    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return x == 0 && z == 0;
    }

    private static BlockPos getFakePos(BlockPos pos) {
        return new BlockPos(pos.getX() % 1024, pos.getY(), pos.getZ());
    }

    public IBlockState getBlockState(BlockPos pos) {
        return this.fakeWorld == null ? super.getBlockState(pos) : this.fakeWorld.getBlockState(getFakePos(pos));
    }

    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        if (this.fakeWorld == null) {
            return super.getTileEntity(pos);
        } else {
            if (pos.getY() >= 40) {
                pos = pos.offset(EnumFacing.DOWN, 40);
            }

            return this.fakeWorld.getTileEntity(getFakePos(pos));
        }
    }

    public boolean isOutsideBuildHeight(BlockPos pos) {
        return super.isOutsideBuildHeight(getFakePos(pos));
    }

    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {
    }
}
