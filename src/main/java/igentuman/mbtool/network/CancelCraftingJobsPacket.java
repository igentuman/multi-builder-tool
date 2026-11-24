package igentuman.mbtool.network;

import igentuman.mbtool.integration.ae2.CraftingJobTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public record CancelCraftingJobsPacket(String structure) implements CustomPacketPayload {
    public static final Type<CancelCraftingJobsPacket> TYPE = new Type<>(rl("cancel_crafting_jobs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelCraftingJobsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(512), CancelCraftingJobsPacket::structure,
            CancelCraftingJobsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) CraftingJobTracker.cancel(player, structure);
        });
    }
}
