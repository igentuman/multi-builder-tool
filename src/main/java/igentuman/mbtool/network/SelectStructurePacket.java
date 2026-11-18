package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Saves a browser selection without triggering the build action. */
public record SelectStructurePacket(int containerId, int slot, int structureIndex) implements CustomPacketPayload {
    public static final Type<SelectStructurePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mbtool.MODID, "select_structure"));

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

    /** Selecting a predefined structure replaces any imported runtime structure; other settings are kept. */
    public void applyTo(ItemStack stack) {
        stack.set(MbtoolDataComponents.STRUCTURE_RECIPE.get(), structureIndex);
        stack.remove(MbtoolDataComponents.RUNTIME_STRUCTURE.get());
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var menu = player.containerMenu;
            if (menu.containerId != containerId) return;
            int menuSlot = menu instanceof MultibuilderContainer container ? container.getPlayerSlot()
                    : menu instanceof MultibuilderSelectStructureContainer container ? container.getPlayerSlot() : -1;
            if (menuSlot < 0 || slot != menuSlot || slot >= player.getInventory().getContainerSize()) return;
            if (structureIndex < 0 || structureIndex >= MultiblocksProvider.getStructures().size()) return;
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof MultibuilderItem)) return;
            if (!menu.stillValid(player)) return;
            applyTo(stack);
            player.getInventory().setChanged();
            menu.broadcastChanges();
        });
    }
}
