package igentuman.mbtool.network;

import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.PlacedStructure;
import igentuman.mbtool.util.PlacedStructuresManager;
import igentuman.mbtool.util.StructureDismantler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DismantleStructurePacket {
    private final BlockPos targetPos;
    private final InteractionHand hand;
    
    public DismantleStructurePacket(BlockPos targetPos, InteractionHand hand) {
        this.targetPos = targetPos;
        this.hand = hand;
    }
    
    public static void encode(DismantleStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.targetPos);
        buffer.writeEnum(packet.hand);
    }
    
    public static DismantleStructurePacket decode(FriendlyByteBuf buffer) {
        BlockPos targetPos = buffer.readBlockPos();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new DismantleStructurePacket(targetPos, hand);
    }
    
    public static void handle(DismantleStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            ItemStack itemStack = player.getItemInHand(packet.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) {
                return;
            }
            
            // Find the structure at the target position
            PlacedStructure structure = PlacedStructuresManager.findStructureAt(itemStack, packet.targetPos);
            if (structure == null) {
                player.sendSystemMessage(Component.translatable("message.mbtool.no_structure_found"));
                return;
            }
            
            // Check if the player placed this structure (optional security check)
            if (!structure.getPlacedBy().equals(player.getUUID()) && !player.isCreative()) {
                player.sendSystemMessage(Component.translatable("message.mbtool.not_your_structure"));
                return;
            }
            
            // Attempt to dismantle the structure
            StructureDismantler.DismantleResult result = StructureDismantler.dismantleStructure(
                player.level(), player, itemStack, structure);
            
            // Send result message to player
            player.sendSystemMessage(result.getMessage());
            
            // If successful, remove from placed structures list
            if (result.isSuccess()) {
                PlacedStructuresManager.removePlacedStructure(itemStack, structure);
            }
        });
        context.setPacketHandled(true);
    }
}