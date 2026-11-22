package igentuman.mbtool.network;

import igentuman.mbtool.client.BlockReplacementState;
import igentuman.mbtool.util.BlockEquivalencyManager;
import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/** The client displays server policy, never its local common config. */
public record SyncBlockEquivalencyPacket(List<List<ResourceLocation>> groups) {
    public static SyncBlockEquivalencyPacket snapshot() {
        return new SyncBlockEquivalencyPacket(BlockEquivalencyManager.getGroups().stream()
                .map(group -> group.blocks().stream().map(ForgeRegistries.BLOCKS::getKey).toList()).toList());
    }

    public static void encode(SyncBlockEquivalencyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.groups, (buf, blocks) ->
                buf.writeCollection(blocks, FriendlyByteBuf::writeResourceLocation));
    }

    public static SyncBlockEquivalencyPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > BlockReplacementPreferences.MAX_SETS) throw new IllegalArgumentException("Too many equivalency sets");
        var groups = new java.util.ArrayList<List<ResourceLocation>>();
        for (int set = 0; set < count; set++) {
            int size = buffer.readVarInt();
            if (size < 0 || size > BlockReplacementPreferences.MAX_MEMBERS) throw new IllegalArgumentException("Too many equivalent blocks");
            var blocks = new java.util.ArrayList<ResourceLocation>();
            for (int i = 0; i < size; i++) blocks.add(buffer.readResourceLocation());
            groups.add(List.copyOf(blocks));
        }
        return new SyncBlockEquivalencyPacket(List.copyOf(groups));
    }

    public static void handle(SyncBlockEquivalencyPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> BlockReplacementState.update(packet.groups));
        context.get().setPacketHandled(true);
    }
}
