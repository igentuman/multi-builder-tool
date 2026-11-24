package igentuman.mbtool.network;

import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public record ToggleIgnoreCollisionsPacket(boolean ignoreCollisions, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ToggleIgnoreCollisionsPacket> TYPE = new Type<>(rl("toggle_ignore_collisions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleIgnoreCollisionsPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ToggleIgnoreCollisionsPacket::ignoreCollisions,
        ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[Math.floorMod(i, InteractionHand.values().length)], InteractionHand::ordinal),
        ToggleIgnoreCollisionsPacket::hand,
        ToggleIgnoreCollisionsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack itemStack = player.getItemInHand(hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) return;
            MultibuilderItem.setIgnoreCollisions(itemStack, ignoreCollisions);
        });
    }
}
