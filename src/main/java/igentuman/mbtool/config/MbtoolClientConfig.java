package igentuman.mbtool.config;

import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local preferences shared by all tools and worlds; only request snapshots are sent to the server. */
public final class MbtoolClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DARK_MODE;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_REPLACEMENTS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DARK_MODE = builder.comment("Use a dark gray theme for the Multibuilder GUI.")
                .define("darkMode", false);
        BLOCK_REPLACEMENTS = builder.comment("Preferred block per server-defined equivalency set (set ID=block ID).",
                        "Saved for this client across tools, worlds and servers. Unmatched sets are ignored.")
                .defineListAllowEmpty("blockReplacements", List.of(), value -> value instanceof String);
        SPEC = builder.build();
    }

    private MbtoolClientConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "mbtool-client.toml");
    }

    public static BlockReplacementPreferences blockReplacements() {
        Map<String, ResourceLocation> choices = new LinkedHashMap<>();
        for (String entry : BLOCK_REPLACEMENTS.get()) {
            int separator = entry.indexOf('=');
            if (separator != 36) continue;
            var block = ResourceLocation.tryParse(entry.substring(separator + 1));
            if (block != null) choices.put(entry.substring(0, separator), block);
        }
        return new BlockReplacementPreferences(choices);
    }

    public static void selectReplacement(String group, ResourceLocation block) {
        var choices = new LinkedHashMap<>(blockReplacements().selections());
        if (block == null) choices.remove(group);
        else choices.put(group, block);
        saveReplacements(choices);
    }

    public static void clearReplacements(Collection<BlockReplacementPreferences.Group> groups) {
        var choices = new LinkedHashMap<>(blockReplacements().selections());
        groups.forEach(group -> choices.remove(group.id()));
        saveReplacements(choices);
    }

    private static void saveReplacements(Map<String, ResourceLocation> choices) {
        BLOCK_REPLACEMENTS.set(choices.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        SPEC.save();
    }

    public static boolean isDarkMode() {
        return DARK_MODE.get();
    }

    public static void toggleDarkMode() {
        DARK_MODE.set(!isDarkMode());
        SPEC.save();
    }
}
