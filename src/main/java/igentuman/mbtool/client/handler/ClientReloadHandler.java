package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.render.MultiblockRenderer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import static igentuman.mbtool.Mbtool.MODID;
import static igentuman.mbtool.Mbtool.rl;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ClientReloadHandler {
    @SubscribeEvent
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(rl("structure_previews"),
            (ResourceManagerReloadListener) manager -> MultiblockRenderer.invalidateCache());
    }
}
