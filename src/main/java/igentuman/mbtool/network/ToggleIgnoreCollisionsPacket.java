package igentuman.mbtool.network;

import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleIgnoreCollisionsPacket {
    private final boolean ignoreCollisions;
    private final InteractionHand hand;

    public ToggleIgnoreCollisionsPacket(boolean ignoreCollisions, InteractionHand hand) {
        this.ignoreCollisions = ignoreCollisions;
        this.hand = hand;
    }

    public static void encode(ToggleIgnoreCollisionsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.ignoreCollisions);
        buffer.writeEnum(packet.hand);
    }

    public static ToggleIgnoreCollisionsPacket decode(FriendlyByteBuf buffer) {
        boolean ignoreCollisions = buffer.readBoolean();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new ToggleIgnoreCollisionsPacket(ignoreCollisions, hand);
    }

    public static void handle(ToggleIgnoreCollisionsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack itemStack = player.getItemInHand(packet.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) {
                return;
            }

            MultibuilderItem.setIgnoreCollisions(itemStack, packet.ignoreCollisions);
        });
        context.setPacketHandled(true);
    }
}
