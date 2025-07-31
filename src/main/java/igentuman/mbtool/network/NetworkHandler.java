package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(Mbtool.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void registerPackets() {
        INSTANCE.messageBuilder(SyncMultibuilderParamsPacket.class, packetId++)
            .encoder(SyncMultibuilderParamsPacket::encode)
            .decoder(SyncMultibuilderParamsPacket::decode)
            .consumerMainThread(SyncMultibuilderParamsPacket::handle)
            .add();
    }
}