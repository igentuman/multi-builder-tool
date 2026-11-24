package igentuman.mbtool.config;

import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MbtoolClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DARK_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_REPLACEMENTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DARK_MODE = builder.comment("Use a dark gray theme for the Multibuilder GUI.")
                .define("darkMode", false);
        BLOCK_REPLACEMENTS = builder.comment("Preferred block per server-defined equivalency set (set ID=block ID).",
                        "Saved for this client across tools, worlds and servers. Unmatched sets are ignored.")
                .defineListAllowEmpty("blockReplacements", List.of(), () -> "", value -> value instanceof String);
        SPEC = builder.build();
    }

    private MbtoolClientConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "mbtool-client.toml");
    }

    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static BlockReplacementPreferences blockReplacements() {
        if (!loaded()) return BlockReplacementPreferences.EMPTY;
        Map<String, Identifier> choices = new LinkedHashMap<>();
        for (String entry : BLOCK_REPLACEMENTS.get()) {
            int separator = entry.indexOf('=');
            if (separator != 36) continue;
            Identifier block = Identifier.tryParse(entry.substring(separator + 1));
            if (block != null) choices.put(entry.substring(0, separator), block);
        }
        return new BlockReplacementPreferences(choices);
    }

    public static void selectReplacement(String group, Identifier block) {
        Map<String, Identifier> choices = new LinkedHashMap<>(blockReplacements().selections());
        if (block == null) choices.remove(group);
        else choices.put(group, block);
        saveReplacements(choices);
    }

    public static void clearReplacements(Collection<BlockReplacementPreferences.Group> groups) {
        Map<String, Identifier> choices = new LinkedHashMap<>(blockReplacements().selections());
        groups.forEach(group -> choices.remove(group.id()));
        saveReplacements(choices);
    }

    private static void saveReplacements(Map<String, Identifier> choices) {
        if (!loaded()) return;
        BLOCK_REPLACEMENTS.set(choices.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        SPEC.save();
    }

    public static boolean isDarkMode() {
        return loaded() && DARK_MODE.get();
    }

    public static void toggleDarkMode() {
        if (!loaded()) return;
        DARK_MODE.set(!isDarkMode());
        SPEC.save();
    }
}
