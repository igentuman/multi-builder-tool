package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblockBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public class SyncMultibuilderParamsPacket implements CustomPacketPayload {
    public static final Type<SyncMultibuilderParamsPacket> TYPE = new Type<>(rl( "sync_multibuilder_params"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMultibuilderParamsPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, SyncMultibuilderParamsPacket::recipeIndex,
        ByteBufCodecs.INT, SyncMultibuilderParamsPacket::rotation,
        ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), SyncMultibuilderParamsPacket::hand,
        SyncMultibuilderParamsPacket::new
    );

    private final int recipeIndex;
    private final int rotation;
    private final InteractionHand hand;
    
    public SyncMultibuilderParamsPacket(int recipeIndex, int rotation, InteractionHand hand) {
        this.recipeIndex = recipeIndex;
        this.rotation = rotation;
        this.hand = hand;
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int recipeIndex() {
        return recipeIndex;
    }

    public int rotation() {
        return rotation;
    }

    public InteractionHand hand() {
        return hand;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            
            ItemStack itemStack = player.getItemInHand(this.hand);
            if (!(itemStack.getItem() instanceof MultibuilderItem multibuilderItem)) {
                return;
            }
            
            // Update the server-side ItemStack with client parameters
            itemStack.set(MbtoolDataComponents.STRUCTURE_RECIPE.get(), this.recipeIndex);
            itemStack.set(MbtoolDataComponents.STRUCTURE_ROTATION.get(), this.rotation);
            
            // Ensure structures are loaded on server
            if (MultiblocksProvider.structures.isEmpty()) {
                MultiblocksProvider.getStructures();
            }
            
            // Validate recipe index
            if (this.recipeIndex < 0 || this.recipeIndex >= MultiblocksProvider.structures.size()) {
                player.sendSystemMessage(Component.translatable("message.mbtool.invalid_recipe"));
                return;
            }
            
            // Get the structure
            MultiblockStructure structure = MultiblocksProvider.structures.get(this.recipeIndex);
            if (structure == null) {
                player.sendSystemMessage(Component.translatable("message.mbtool.invalid_recipe"));
                return;
            }
            
            // Get build position
            BlockPos buildPos = getBuildPosition(player, structure, this.rotation);
            if (buildPos == null) {
                player.sendSystemMessage(Component.translatable("message.mbtool.no_build_position"));
                return;
            }
            
            // Attempt to build the multiblock
            MultiblockBuilder.BuildResult result = MultiblockBuilder.buildMultiblock(
                player.level(), player, itemStack, structure, buildPos, this.rotation);
            
            // Send result message to player
            if (result.isShowAsTitle()) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.empty()));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(result.getMessage()));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
            } else {
                player.sendSystemMessage(result.getMessage());
            }
        });
    }
    
    /**
     * Get the build position based on player's look direction
     * Uses the same logic as MultibuilderItem#getBuildPosition
     */
    private static BlockPos getBuildPosition(ServerPlayer player, MultiblockStructure structure, int rotation) {
        if (player == null || player.level() == null) return null;
        
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(64.0));
        
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
        
        // Calculate placement position based on hit side with new pivot logic
        net.minecraft.core.Direction hitSide = rayTrace.getDirection();
        
        switch (hitSide) {
            case DOWN:
                // Looking at ground: center horizontally, bottom block at hit position
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, -structure.getHeight(), 0);
                }
                // Center horizontally based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
                }
                break;
            case UP:
                // Looking at top: center horizontally, top block at hit position
                hit = hit.offset(0, 1, 0);
                // Center horizontally based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
                }
                break;
            case EAST:
                // Looking at side: place structure adjacent to the hit face, center vertically and on Z axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(1, 0, 0);
                }
                // Center on Z axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(0, -structure.getHeight() / 2, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(0, -structure.getHeight() / 2, -structure.getWidth() / 2);
                }
                break;
            case WEST:
                // Looking at side: place structure adjacent to the hit face, center vertically and on Z axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(-1, 0, 0);
                }
                // Center on Z axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() + 1, -structure.getHeight() / 2, -structure.getDepth() / 2);
                } else {
                    hit = hit.offset(-structure.getDepth() + 1, -structure.getHeight() / 2, -structure.getWidth() / 2);
                }
                break;
            case NORTH:
                // Looking at side: place structure adjacent to the hit face, center vertically and on X axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 0, -1);
                }
                // Center on X axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, -structure.getHeight() / 2, -structure.getDepth() + 1);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, -structure.getHeight() / 2, -structure.getWidth() + 1);
                }
                break;
            case SOUTH:
                // Looking at side: place structure adjacent to the hit face, center vertically and on X axis
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 0, 1);
                }
                // Center on X axis based on rotation
                if (rotation == 0 || rotation == 2) {
                    hit = hit.offset(-structure.getWidth() / 2, -structure.getHeight() / 2, 0);
                } else {
                    hit = hit.offset(-structure.getDepth() / 2, -structure.getHeight() / 2, 0);
                }
                break;
        }

        return hit;
    }
}