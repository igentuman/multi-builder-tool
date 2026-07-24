package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.ModUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Mbtool.MODID)
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

        if(ModUtil.isAe2Loaded()) {
            registrar.playToServer(
                ToggleMeAccessPacket.TYPE,
                ToggleMeAccessPacket.STREAM_CODEC,
                ToggleMeAccessPacket::handle
            );

            registrar.playToServer(
                ToggleMeAutocraftPacket.TYPE,
                ToggleMeAutocraftPacket.STREAM_CODEC,
                ToggleMeAutocraftPacket::handle
            );

            registrar.playToServer(
                PacketAE2PatternTransfer.TYPE,
                PacketAE2PatternTransfer.STREAM_CODEC,
                PacketAE2PatternTransfer::handle
            );
        }
        if(ModUtil.isJEILoaded()) {
            registrar.playToServer(
                    PacketJeiRecipeTransfer.TYPE,
                    PacketJeiRecipeTransfer.STREAM_CODEC,
                    PacketJeiRecipeTransfer::handle
            );
        }
    }
}