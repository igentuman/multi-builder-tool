package igentuman.mbtool.network;

import igentuman.mbtool.integration.ae2.CraftingJobTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CancelCraftingJobsPacket(String structure) {
    public static void encode(CancelCraftingJobsPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.structure); }
    public static CancelCraftingJobsPacket decode(FriendlyByteBuf buffer) { return new CancelCraftingJobsPacket(buffer.readUtf()); }
    public static void handle(CancelCraftingJobsPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().enqueueWork(() -> {
            if (player != null) CraftingJobTracker.cancel(player, packet.structure);
        });
        context.get().setPacketHandled(true);
    }
}
