package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Mbtool.MODID);
        
        // Register packets using the new payload system
        registrar.playToServer(SyncMultibuilderParamsPacket.TYPE, SyncMultibuilderParamsPacket.STREAM_CODEC, SyncMultibuilderParamsPacket::handle);
        registrar.playToClient(SyncStructuresPacket.TYPE, SyncStructuresPacket.STREAM_CODEC, SyncStructuresPacket::handle);
        registrar.playToServer(SyncRuntimeStructurePacket.TYPE, SyncRuntimeStructurePacket.STREAM_CODEC, SyncRuntimeStructurePacket::handle);
    }
    
    public static void sendToServer(Object packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    public static void sendToAllPlayers(Object packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}