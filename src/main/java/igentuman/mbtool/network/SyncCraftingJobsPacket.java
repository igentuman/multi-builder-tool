package igentuman.mbtool.network;

import igentuman.mbtool.client.CraftingProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public record SyncCraftingJobsPacket(CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncCraftingJobsPacket> TYPE = new Type<>(rl("sync_crafting_jobs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCraftingJobsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SyncCraftingJobsPacket::data,
            SyncCraftingJobsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> CraftingProgress.update(data));
    }
}
