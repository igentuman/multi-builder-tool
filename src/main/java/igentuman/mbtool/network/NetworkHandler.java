package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Mbtool.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Mbtool.MODID).versioned("1");
        
        registrar.playToServer(
            SyncMultibuilderParamsPacket.TYPE,
            SyncMultibuilderParamsPacket.STREAM_CODEC,
            SyncMultibuilderParamsPacket::handle
        );
            
        registrar.playToClient(
            SyncStructuresPacket.TYPE,
            SyncStructuresPacket.STREAM_CODEC,
            SyncStructuresPacket::handle
        );
            
        registrar.playToClient(
            SyncSingleStructurePacket.TYPE,
            SyncSingleStructurePacket.STREAM_CODEC,
            SyncSingleStructurePacket::handle
        );
            
        registrar.playToServer(
            SyncRuntimeStructurePacket.TYPE,
            SyncRuntimeStructurePacket.STREAM_CODEC,
            SyncRuntimeStructurePacket::handle
        );
            
        registrar.playToServer(
            DismantleStructurePacket.TYPE,
            DismantleStructurePacket.STREAM_CODEC,
            DismantleStructurePacket::handle
        );

        registrar.playToServer(
            ToggleMeAccessPacket.TYPE,
            ToggleMeAccessPacket.STREAM_CODEC,
            ToggleMeAccessPacket::handle
        );
    }
}