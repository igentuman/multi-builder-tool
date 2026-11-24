package igentuman.mbtool.network;

import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public record SelectStructurePacket(int containerId, int slot, int structureIndex) implements CustomPacketPayload {
    public static final Type<SelectStructurePacket> TYPE = new Type<>(rl("select_structure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectStructurePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectStructurePacket::containerId,
            ByteBufCodecs.VAR_INT, SelectStructurePacket::slot,
            ByteBufCodecs.VAR_INT, SelectStructurePacket::structureIndex,
            SelectStructurePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void applyTo(ItemStack stack) {
        stack.set(MbtoolDataComponents.STRUCTURE_RECIPE.get(), structureIndex);
        stack.remove(MbtoolDataComponents.RUNTIME_STRUCTURE.get());
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack stack = MenuSession.toolStack(player, containerId, slot);
            if (stack == null) return;
            if (structureIndex < 0 || structureIndex >= MultiblocksProvider.getStructures().size()) return;
            applyTo(stack);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
    }
}
