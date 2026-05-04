package igentuman.mbtool.network;

import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleMeAccessPacket {
    private final boolean meAccess;
    private final InteractionHand hand;

    public ToggleMeAccessPacket(boolean meAccess, InteractionHand hand) {
        this.meAccess = meAccess;
        this.hand = hand;
    }

    public static void encode(ToggleMeAccessPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meAccess);
        buffer.writeEnum(packet.hand);
    }

    public static ToggleMeAccessPacket decode(FriendlyByteBuf buffer) {
        boolean meAccess = buffer.readBoolean();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new ToggleMeAccessPacket(meAccess, hand);
    }

    public static void handle(ToggleMeAccessPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack itemStack = player.getItemInHand(packet.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) {
                return;
            }

            itemStack.getOrCreateTag().putBoolean("meAccessAllowed", packet.meAccess);
        });
        context.setPacketHandled(true);
    }
}
