package igentuman.mbtool.network;

import igentuman.mbtool.container.MultibuilderContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketJeiRecipeTransfer {
    private final List<ItemStack> items;

    public PacketJeiRecipeTransfer(List<ItemStack> items) {
        this.items = items;
    }

    public static void encode(PacketJeiRecipeTransfer packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.items.size());
        for (ItemStack stack : packet.items) {
            buffer.writeItem(stack);
        }
    }

    public static PacketJeiRecipeTransfer decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(buffer.readItem());
        }
        return new PacketJeiRecipeTransfer(items);
    }

    public static void handle(PacketJeiRecipeTransfer packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!(player.containerMenu instanceof MultibuilderContainer container)) return;

            IItemHandler itemHandler = container.getItemHandler();
            if (itemHandler == null) return;

            for (ItemStack required : packet.items) {
                if (required.isEmpty()) continue;

                ItemStack remaining = required.copy();

                for (int i = 0; i < player.getInventory().getContainerSize() && !remaining.isEmpty(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.isEmpty() || !ItemStack.isSameItemSameTags(invStack, remaining)) continue;

                    int toTransfer = Math.min(invStack.getCount(), remaining.getCount());
                    ItemStack toInsert = invStack.copyWithCount(toTransfer);

                    for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                        toInsert = itemHandler.insertItem(slot, toInsert, false);
                        if (toInsert.isEmpty()) break;
                    }

                    int transferred = toTransfer - toInsert.getCount();
                    invStack.shrink(transferred);
                    remaining.shrink(transferred);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
