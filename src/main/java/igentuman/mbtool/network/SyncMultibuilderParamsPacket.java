package igentuman.mbtool.network;

import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblockBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncMultibuilderParamsPacket {
    private final int recipeIndex;
    private final int rotation;
    private final InteractionHand hand;
    
    public SyncMultibuilderParamsPacket(int recipeIndex, int rotation, InteractionHand hand) {
        this.recipeIndex = recipeIndex;
        this.rotation = rotation;
        this.hand = hand;
    }
    
    public static void encode(SyncMultibuilderParamsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.recipeIndex);
        buffer.writeInt(packet.rotation);
        buffer.writeEnum(packet.hand);
    }
    
    public static SyncMultibuilderParamsPacket decode(FriendlyByteBuf buffer) {
        int recipeIndex = buffer.readInt();
        int rotation = buffer.readInt();
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new SyncMultibuilderParamsPacket(recipeIndex, rotation, hand);
    }
    
    public static void handle(SyncMultibuilderParamsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            ItemStack itemStack = player.getItemInHand(packet.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem multibuilderItem)) {
                return;
            }
            
            // Update the server-side ItemStack with client parameters
            itemStack.getOrCreateTag().putInt("recipe", packet.recipeIndex);
            itemStack.getOrCreateTag().putInt("rotation", packet.rotation);
            
            // Ensure structures are loaded on server
            if (MultiblocksProvider.structures.isEmpty()) {
                MultiblocksProvider.getStructures();
            }
            
            // Validate recipe index
            if (packet.recipeIndex < 0 || packet.recipeIndex >= MultiblocksProvider.structures.size()) {
                player.sendSystemMessage(Component.translatable("message.mbtool.invalid_recipe"));
                return;
            }
            
            // Get the structure
            MultiblockStructure structure = MultiblocksProvider.structures.get(packet.recipeIndex);
            if (structure == null) {
                player.sendSystemMessage(Component.translatable("message.mbtool.invalid_recipe"));
                return;
            }
            
            // Get build position
            BlockPos buildPos = getBuildPosition(player, structure, packet.rotation);
            if (buildPos == null) {
                player.sendSystemMessage(Component.translatable("message.mbtool.no_build_position"));
                return;
            }
            
            // Attempt to build the multiblock
            MultiblockBuilder.BuildResult result = MultiblockBuilder.buildMultiblock(
                player.level(), player, itemStack, structure, buildPos, packet.rotation);
            
            // Send result message to player
            player.sendSystemMessage(result.getMessage());
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Get the build position based on player's look direction
     * Uses the same logic as MultibuilderItem#getBuildPosition
     */
    private static BlockPos getBuildPosition(ServerPlayer player, MultiblockStructure structure, int rotation) {
        if (player == null || player.level() == null) return null;
        
        // Perform raycast for 20 blocks
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(20.0));
        
        BlockHitResult rayTrace = player.level().clip(new net.minecraft.world.level.ClipContext(
            eyePos, endPos, 
            net.minecraft.world.level.ClipContext.Block.OUTLINE, 
            net.minecraft.world.level.ClipContext.Fluid.NONE, 
            player
        ));

        if (rayTrace.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos hit = rayTrace.getBlockPos();
        net.minecraft.world.level.block.state.BlockState state = player.level().getBlockState(hit);
        
        // Calculate placement position based on hit side
        net.minecraft.core.Direction hitSide = rayTrace.getDirection();
        int maxSize = Math.max(structure.getWidth(), structure.getDepth()) - 1;
        
        switch (hitSide) {
            case DOWN:
                hit = hit.offset(0, -structure.getHeight(), 0);
                break;
            case UP:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 1, 0);
                }
                break;
            case EAST:
                hit = hit.offset(maxSize, 0, 0);
                break;
            case WEST:
                hit = hit.offset(-maxSize, 0, 0);
                break;
            case NORTH:
                hit = hit.offset(0, 0, -maxSize);
                break;
            case SOUTH:
                hit = hit.offset(0, 0, maxSize);
                break;
        }
        
        // Center the structure
        if (rotation == 0 || rotation == 2) {
            hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
        } else {
            hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
        }

        return hit;
    }
}