package igentuman.mbtool.network;

import igentuman.mbtool.client.CraftingProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCraftingJobsPacket(CompoundTag data) {
    public static void encode(SyncCraftingJobsPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.data); }
    public static SyncCraftingJobsPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new SyncCraftingJobsPacket(tag == null ? new CompoundTag() : tag);
    }
    public static void handle(SyncCraftingJobsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> CraftingProgress.update(packet.data));
        context.get().setPacketHandled(true);
    }
}
