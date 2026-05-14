package igentuman.mbtool.event;

import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.SyncSingleStructurePacket;
import igentuman.mbtool.util.AutocraftTracker;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

import static igentuman.mbtool.Mbtool.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Clear any stale autocrafting tracking state
            AutocraftTracker.clearPlayer(serverPlayer.getUUID());

            // Send server structures to the joining client one by one
            List<MultiblockStructure> structures = MultiblocksProvider.getStructures();
            int total = structures.size();

            for (int i = 0; i < total; i++) {
                MultiblockStructure structure = structures.get(i);
                SyncSingleStructurePacket packet = new SyncSingleStructurePacket(structure, i, total);
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AutocraftTracker.clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AutocraftTracker.clearAll();
    }
}