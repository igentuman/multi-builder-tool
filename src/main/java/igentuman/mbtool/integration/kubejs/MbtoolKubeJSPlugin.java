package igentuman.mbtool.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import net.neoforged.neoforge.common.NeoForge;

public class MbtoolKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void init() {
        NeoForge.EVENT_BUS.addListener(MbtoolKubeJsEvents::onInitMbtoolStructures);
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(MbtoolKubeJsEvents.GROUP);
    }
}
