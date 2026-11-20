package igentuman.mbtool.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Local preferences shared by all tools and worlds; never synchronized with the server. */
public final class MbtoolClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DARK_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DARK_MODE = builder.comment("Use a dark gray theme for the Multibuilder GUI.")
                .define("darkMode", false);
        SPEC = builder.build();
    }

    private MbtoolClientConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "mbtool-client.toml");
    }

    public static boolean isDarkMode() {
        return DARK_MODE.get();
    }

    public static void toggleDarkMode() {
        DARK_MODE.set(!isDarkMode());
        SPEC.save();
    }
}
