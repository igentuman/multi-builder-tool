package igentuman.mbtool.integration.emi;

import dev.emi.emi.runtime.EmiReloadManager;

/** Only touch this class when EMI is loaded. */
public final class EmiCompat {
    private EmiCompat() {}

    public static void reloadRecipes() {
        if (EmiReloadManager.getStatus() != 0) {
            EmiReloadManager.reload();
        }
    }
}
