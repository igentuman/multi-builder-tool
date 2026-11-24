package igentuman.mbtool.network;

import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.integration.ae2.StructureCraftingManager;
import igentuman.mbtool.util.BlockReplacementPreferences;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public record RequestStructureCraftPacket(int containerId, int slot, int structureIndex,
                                          BlockReplacementPreferences preferences) implements CustomPacketPayload {
    public static final Type<RequestStructureCraftPacket> TYPE = new Type<>(rl("request_structure_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestStructureCraftPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestStructureCraftPacket::containerId,
            ByteBufCodecs.VAR_INT, RequestStructureCraftPacket::slot,
            ByteBufCodecs.VAR_INT, RequestStructureCraftPacket::structureIndex,
            BlockReplacementPreferences.STREAM_CODEC, RequestStructureCraftPacket::preferences,
            RequestStructureCraftPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!MbtoolConfig.isAe2IntegrationEnabled()) return;
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack stack = MenuSession.toolStack(player, containerId, slot);
            if (stack == null) return;
            var structures = MultiblocksProvider.getStructures();
            if (structureIndex < 0 || structureIndex >= structures.size()) return;
            StructureCraftingManager.request(player, stack, structures.get(structureIndex), preferences);
        });
    }
}
