package igentuman.mbtool.event;

import igentuman.mbtool.integration.ae2.CraftingJobTracker;
import igentuman.mbtool.integration.ae2.StructureCraftingManager;
import igentuman.mbtool.network.SyncBlockEquivalencyPacket;
import igentuman.mbtool.network.SyncSingleStructurePacket;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static igentuman.mbtool.Mbtool.MODID;

@EventBusSubscriber(modid = MODID)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, SyncBlockEquivalencyPacket.snapshot());
            if (ModUtil.isAe2Loaded()) CraftingJobTracker.sync(serverPlayer);

            // Send server structures to the joining client one by one
            List<MultiblockStructure> structures = MultiblocksProvider.getStructures();
            int total = structures.size();

            for (int i = 0; i < total; i++) {
                MultiblockStructure structure = structures.get(i);
                SyncSingleStructurePacket packet = new SyncSingleStructurePacket(structure, i, total);
                PacketDistributor.sendToPlayer(serverPlayer, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ModUtil.isAe2Loaded()) {
            StructureCraftingManager.tick(event.getServer());
            CraftingJobTracker.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (ModUtil.isAe2Loaded()) StructureCraftingManager.shutdown(event.getServer());
    }
}
