package igentuman.mbtool.network;

import igentuman.mbtool.container.MultibuilderContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenMultibuilderGuiPacket {
    private final int slot;
    
    public OpenMultibuilderGuiPacket(int slot) {
        this.slot = slot;
    }
    
    public static void encode(OpenMultibuilderGuiPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.slot);
    }
    
    public static OpenMultibuilderGuiPacket decode(FriendlyByteBuf buffer) {
        return new OpenMultibuilderGuiPacket(buffer.readInt());
    }
    
    public static void handle(OpenMultibuilderGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NetworkHooks.openScreen(player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("gui.mbtool.multibuilder");
                    }
                    
                    @Override
                    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                        return new MultibuilderContainer<>(containerId, BlockPos.ZERO, playerInventory);
                    }
                }, buf -> {
                    buf.writeBlockPos(BlockPos.ZERO);
                });
            }
        });
        context.setPacketHandled(true);
    }
}