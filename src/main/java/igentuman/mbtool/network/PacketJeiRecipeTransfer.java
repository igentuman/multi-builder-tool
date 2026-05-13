package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.container.MultibuilderContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class PacketJeiRecipeTransfer implements CustomPacketPayload {
    public static final Type<PacketJeiRecipeTransfer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mbtool.MODID, "jei_recipe_transfer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketJeiRecipeTransfer> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), PacketJeiRecipeTransfer::items,
        PacketJeiRecipeTransfer::new
    );

    private final List<ItemStack> items;

    public PacketJeiRecipeTransfer(List<ItemStack> items) {
        this.items = items;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public List<ItemStack> items() {
        return items;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            if (!(player.containerMenu instanceof MultibuilderContainer container)) return;

            IItemHandler itemHandler = container.getItemHandler();
            if (itemHandler == null) return;

            for (ItemStack required : items) {
                if (required.isEmpty()) continue;

                ItemStack remaining = required.copy();

                for (int i = 0; i < player.getInventory().getContainerSize() && !remaining.isEmpty(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.isEmpty() || !ItemStack.isSameItemSameComponents(invStack, remaining)) continue;

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
    }
}
