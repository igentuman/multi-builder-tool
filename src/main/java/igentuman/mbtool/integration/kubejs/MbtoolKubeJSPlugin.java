package igentuman.mbtool.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;

public class MbtoolKubeJSPlugin  extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        MbtoolKubeJsEvents.GROUP.register();
    }
}
