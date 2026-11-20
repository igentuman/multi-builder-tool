package igentuman.mbtool.client.gui;

import igentuman.mbtool.config.MbtoolClientConfig;

/** Resolve colors while rendering so open views immediately reflect preference changes. */
public final class GuiTheme {
    private GuiTheme() {}

    public static int color(int light, int dark) {
        return MbtoolClientConfig.isDarkMode() ? dark : light;
    }

    public static int labelColor() {
        return color(0x272727, 0xE0E0E0);
    }
}
