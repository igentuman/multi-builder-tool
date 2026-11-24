package igentuman.mbtool.network;

import igentuman.mbtool.client.BlockReplacementState;
import igentuman.mbtool.util.BlockEquivalencyManager;
import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

public record SyncBlockEquivalencyPacket(List<List<Identifier>> groups) implements CustomPacketPayload {
    public static final Type<SyncBlockEquivalencyPacket> TYPE = new Type<>(rl("sync_block_equivalency"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBlockEquivalencyPacket> STREAM_CODEC =
        StreamCodec.of((buffer, packet) -> packet.write(buffer), SyncBlockEquivalencyPacket::read);

    public static SyncBlockEquivalencyPacket snapshot() {
        return new SyncBlockEquivalencyPacket(BlockEquivalencyManager.getGroups().stream()
                .map(group -> group.blocks().stream().map(BuiltInRegistries.BLOCK::getKey).toList()).toList());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeCollection(groups, (buf, blocks) -> buf.writeCollection(blocks, FriendlyByteBuf::writeIdentifier));
    }

    private static SyncBlockEquivalencyPacket read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > BlockReplacementPreferences.MAX_SETS) throw new IllegalArgumentException("Too many equivalency sets");
        List<List<Identifier>> groups = new ArrayList<>();
        for (int set = 0; set < count; set++) {
            int size = buffer.readVarInt();
            if (size < 0 || size > BlockReplacementPreferences.MAX_MEMBERS) throw new IllegalArgumentException("Too many equivalent blocks");
            List<Identifier> blocks = new ArrayList<>();
            for (int i = 0; i < size; i++) blocks.add(buffer.readIdentifier());
            groups.add(List.copyOf(blocks));
        }
        return new SyncBlockEquivalencyPacket(List.copyOf(groups));
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> BlockReplacementState.update(groups));
    }
}
