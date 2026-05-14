package igentuman.mbtool.network;

import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleMeAutocraftPacket {
    private final boolean meAutocrafting;
    private final InteractionHand hand;

    public ToggleMeAutocraftPacket(boolean meAutocrafting, InteractionHand hand) {
        this.meAutocrafting = meAutocrafting;
        this.hand = hand;
    }

    public static void encode(ToggleMeAutocraftPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meAutocrafting);
        buffer.writeEnum(packet.hand);
    }

    public static ToggleMeAutocraftPacket decode(FriendlyByteBuf buffer) {
        boolean meAutocrafting = buffer.readBoolean();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new ToggleMeAutocraftPacket(meAutocrafting, hand);
    }

    public static void handle(ToggleMeAutocraftPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack itemStack = player.getItemInHand(packet.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) {
                return;
            }

            MultibuilderItem.setMeAutocraftingEnabled(itemStack, packet.meAutocrafting);
        });
        context.setPacketHandled(true);
    }
}
