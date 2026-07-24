package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public class ToggleMeAccessPacket implements CustomPacketPayload {
    public static final Type<ToggleMeAccessPacket> TYPE = new Type<>(rl("toggle_me_access"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMeAccessPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ToggleMeAccessPacket::meAccess,
        ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ToggleMeAccessPacket::hand,
        ToggleMeAccessPacket::new
    );

    private final boolean meAccess;
    private final InteractionHand hand;

    public ToggleMeAccessPacket(boolean meAccess, InteractionHand hand) {
        this.meAccess = meAccess;
        this.hand = hand;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public boolean meAccess() {
        return meAccess;
    }

    public InteractionHand hand() {
        return hand;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!MbtoolConfig.isAe2IntegrationEnabled()) return;

            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemStack itemStack = player.getItemInHand(this.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) {
                return;
            }

            itemStack.set(MbtoolDataComponents.ME_ACCESS.get(), this.meAccess);
        });
    }
}
