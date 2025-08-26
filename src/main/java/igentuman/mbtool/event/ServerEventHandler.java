package igentuman.mbtool.event;

import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.SyncStructuresPacket;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import static igentuman.mbtool.Mbtool.MODID;

@EventBusSubscriber(modid = MODID, bus = IModBusEvent)
public class ServerEventHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Send all server structures to the joining client
            SyncStructuresPacket packet = new SyncStructuresPacket(MultiblocksProvider.getStructures());
            NetworkHandler.sendToPlayer(serverPlayer, packet);
        }
    }
}