package igentuman.mbtool.event;

import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.SyncStructuresPacket;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import static igentuman.mbtool.Mbtool.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Send all server structures to the joining client
            SyncStructuresPacket packet = new SyncStructuresPacket(MultiblocksProvider.getStructures());
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
        }
    }
}