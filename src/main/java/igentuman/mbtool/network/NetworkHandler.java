package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.ModUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "3";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(Mbtool.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void registerPackets() {
        INSTANCE.messageBuilder(SyncBlockEquivalencyPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBlockEquivalencyPacket::encode).decoder(SyncBlockEquivalencyPacket::decode)
                .consumerMainThread(SyncBlockEquivalencyPacket::handle).add();
        INSTANCE.messageBuilder(SyncMultibuilderParamsPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SyncMultibuilderParamsPacket::encode)
            .decoder(SyncMultibuilderParamsPacket::decode)
            .consumerMainThread(SyncMultibuilderParamsPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(SyncStructuresPacket.class, packetId++)
            .encoder(SyncStructuresPacket::encode)
            .decoder(SyncStructuresPacket::decode)
            .consumerMainThread(SyncStructuresPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(SyncSingleStructurePacket.class, packetId++)
            .encoder(SyncSingleStructurePacket::encode)
            .decoder(SyncSingleStructurePacket::decode)
            .consumerMainThread(SyncSingleStructurePacket::handle)
            .add();
            
        INSTANCE.messageBuilder(SyncRuntimeStructurePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SyncRuntimeStructurePacket::encode)
            .decoder(SyncRuntimeStructurePacket::decode)
            .consumerMainThread(SyncRuntimeStructurePacket::handle)
            .add();
            
        INSTANCE.messageBuilder(DismantleStructurePacket.class, packetId++)
            .encoder(DismantleStructurePacket::encode)
            .decoder(DismantleStructurePacket::decode)
            .consumerMainThread(DismantleStructurePacket::handle)
            .add();
            
        INSTANCE.messageBuilder(MultibuilderContainerSetContentPacket.class, packetId++)
            .encoder(MultibuilderContainerSetContentPacket::encode)
            .decoder(MultibuilderContainerSetContentPacket::decode)
            .consumerMainThread(MultibuilderContainerSetContentPacket::handle)
            .add();

        INSTANCE.messageBuilder(ToggleIgnoreCollisionsPacket.class, packetId++)
            .encoder(ToggleIgnoreCollisionsPacket::encode)
            .decoder(ToggleIgnoreCollisionsPacket::decode)
            .consumerMainThread(ToggleIgnoreCollisionsPacket::handle)
            .add();

        if(ModUtil.isAe2Loaded()) {
            INSTANCE.messageBuilder(SyncCraftingJobsPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(SyncCraftingJobsPacket::encode).decoder(SyncCraftingJobsPacket::decode)
                    .consumerMainThread(SyncCraftingJobsPacket::handle).add();
            INSTANCE.messageBuilder(CancelCraftingJobsPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(CancelCraftingJobsPacket::encode).decoder(CancelCraftingJobsPacket::decode)
                    .consumerMainThread(CancelCraftingJobsPacket::handle).add();
            INSTANCE.messageBuilder(ToggleMeAccessPacket.class, packetId++)
                    .encoder(ToggleMeAccessPacket::encode)
                    .decoder(ToggleMeAccessPacket::decode)
                    .consumerMainThread(ToggleMeAccessPacket::handle)
                    .add();

            INSTANCE.messageBuilder(ToggleMeAutocraftPacket.class, packetId++)
                    .encoder(ToggleMeAutocraftPacket::encode)
                    .decoder(ToggleMeAutocraftPacket::decode)
                    .consumerMainThread(ToggleMeAutocraftPacket::handle)
                    .add();

            INSTANCE.messageBuilder(PacketAE2PatternTransfer.class, packetId++)
                    .encoder(PacketAE2PatternTransfer::encode)
                    .decoder(PacketAE2PatternTransfer::decode)
                    .consumerMainThread(PacketAE2PatternTransfer::handle)
                    .add();

            INSTANCE.messageBuilder(RequestStructureCraftPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(RequestStructureCraftPacket::encode)
                    .decoder(RequestStructureCraftPacket::decode)
                    .consumerMainThread(RequestStructureCraftPacket::handle)
                    .add();
        }
        if(ModUtil.isJEILoaded()) {
            INSTANCE.messageBuilder(PacketJeiRecipeTransfer.class, packetId++)
                    .encoder(PacketJeiRecipeTransfer::encode)
                    .decoder(PacketJeiRecipeTransfer::decode)
                    .consumerMainThread(PacketJeiRecipeTransfer::handle)
                    .add();
        }
        INSTANCE.messageBuilder(SelectStructurePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectStructurePacket::encode)
                .decoder(SelectStructurePacket::decode)
                .consumerMainThread(SelectStructurePacket::handle)
                .add();
    }
}