package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.PlacedStructure;
import igentuman.mbtool.util.PlacedStructuresManager;
import igentuman.mbtool.util.StructureDismantler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public class DismantleStructurePacket implements CustomPacketPayload {
    public static final Type<DismantleStructurePacket> TYPE = new Type<>(rl("dismantle_structure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DismantleStructurePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DismantleStructurePacket::targetPos,
        ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), DismantleStructurePacket::hand,
        DismantleStructurePacket::new
    );

    private final BlockPos targetPos;
    private final InteractionHand hand;
    
    public DismantleStructurePacket(BlockPos targetPos, InteractionHand hand) {
        this.targetPos = targetPos;
        this.hand = hand;
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public BlockPos targetPos() {
        return targetPos;
    }

    public InteractionHand hand() {
        return hand;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            
            ItemStack itemStack = player.getItemInHand(this.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem)) {
                return;
            }
            
            // Find the structure at the target position
            PlacedStructure structure = PlacedStructuresManager.findStructureAt(itemStack, this.targetPos);
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
    }
}