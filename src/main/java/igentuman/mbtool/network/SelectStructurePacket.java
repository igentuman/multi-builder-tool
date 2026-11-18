package igentuman.mbtool.network;

import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Saves a browser selection without triggering the build action. */
public record SelectStructurePacket(int containerId, int slot, int structureIndex) {
    public void applyTo(CompoundTag tag) {
        tag.putInt("recipe", structureIndex);
        tag.remove("runtimeStructure");
    }

    public static void encode(SelectStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.containerId);
        buffer.writeInt(packet.slot);
        buffer.writeInt(packet.structureIndex);
    }

    public static SelectStructurePacket decode(FriendlyByteBuf buffer) {
        return new SelectStructurePacket(buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(SelectStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || player.containerMenu.containerId != packet.containerId) return;
            int menuSlot = player.containerMenu instanceof MultibuilderContainer menu ? menu.getPlayerSlot()
                    : player.containerMenu instanceof MultibuilderSelectStructureContainer menu ? menu.getPlayerSlot() : -1;
            if (menuSlot < 0 || packet.slot != menuSlot || packet.slot >= player.getInventory().getContainerSize()) return;
            var structures = MultiblocksProvider.getStructures();
            if (packet.structureIndex < 0 || packet.structureIndex >= structures.size()
                    || structures.get(packet.structureIndex).isGroup()) return;
            ItemStack stack = player.getInventory().getItem(packet.slot);
            if (!(stack.getItem() instanceof MultibuilderItem)) return;
            if (!player.containerMenu.stillValid(player)) return;
            packet.applyTo(stack.getOrCreateTag());
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
