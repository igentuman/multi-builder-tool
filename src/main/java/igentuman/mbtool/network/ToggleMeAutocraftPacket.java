package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ToggleMeAutocraftPacket implements CustomPacketPayload {
    public static final Type<ToggleMeAutocraftPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mbtool.MODID, "toggle_me_autocraft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMeAutocraftPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ToggleMeAutocraftPacket::meAutocrafting,
        ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ToggleMeAutocraftPacket::hand,
        ToggleMeAutocraftPacket::new
    );

    private final boolean meAutocrafting;
    private final InteractionHand hand;

    public ToggleMeAutocraftPacket(boolean meAutocrafting, InteractionHand hand) {
        this.meAutocrafting = meAutocrafting;
        this.hand = hand;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public boolean meAutocrafting() {
        return meAutocrafting;
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

            itemStack.set(MbtoolDataComponents.ME_AUTOCRAFTING.get(), this.meAutocrafting);
        });
    }
}
