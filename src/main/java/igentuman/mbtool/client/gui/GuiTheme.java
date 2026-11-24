package igentuman.mbtool.client.gui;

import igentuman.mbtool.config.MbtoolClientConfig;

public final class GuiTheme {
    private GuiTheme() {}

    public static int color(int light, int dark) {
        return MbtoolClientConfig.isDarkMode() ? dark : light;
    }

    public static int labelColor() {
        return color(0xFF272727, 0xFFE0E0E0);
    }
}
