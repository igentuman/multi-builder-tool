package igentuman.mbtool.network;

import igentuman.mbtool.util.BlockReplacementPreferences;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.integration.ae2.StructureCraftingManager;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestStructureCraftPacket(int containerId, int slot, int structureIndex,
                                          BlockReplacementPreferences preferences) {
    public RequestStructureCraftPacket(int containerId, int slot, int structureIndex) {
        this(containerId, slot, structureIndex, BlockReplacementPreferences.EMPTY);
    }
    public static void encode(RequestStructureCraftPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.containerId);
        buffer.writeInt(packet.slot);
        buffer.writeInt(packet.structureIndex);
        packet.preferences.write(buffer);
    }

    public static RequestStructureCraftPacket decode(FriendlyByteBuf buffer) {
        return new RequestStructureCraftPacket(buffer.readInt(), buffer.readInt(), buffer.readInt(), BlockReplacementPreferences.read(buffer));
    }

    public static void handle(RequestStructureCraftPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || player.containerMenu.containerId != packet.containerId) return;
            int menuSlot = player.containerMenu instanceof MultibuilderContainer menu ? menu.getPlayerSlot()
                    : player.containerMenu instanceof MultibuilderSelectStructureContainer menu ? menu.getPlayerSlot() : -1;
            if (menuSlot < 0 || packet.slot != menuSlot || packet.slot >= player.getInventory().getContainerSize()) return;
            if (!player.containerMenu.stillValid(player)) return;
            var structures = MultiblocksProvider.getStructures();
            if (packet.structureIndex < 0 || packet.structureIndex >= structures.size()
                    || structures.get(packet.structureIndex).isGroup()) return;
            ItemStack stack = player.getInventory().getItem(packet.slot);
            if (!(stack.getItem() instanceof MultibuilderItem)) return;
            StructureCraftingManager.request(player, stack, structures.get(packet.structureIndex),
                    packet.preferences);
        });
        context.setPacketHandled(true);
    }
}
