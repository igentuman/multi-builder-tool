package igentuman.mbtool.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public class MbtoolConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Energy Configuration
    public static final ModConfigSpec.ConfigValue<Integer> MAX_ENERGY;
    public static final ModConfigSpec.ConfigValue<Integer> ENERGY_TRANSFER_RATE;
    public static final ModConfigSpec.ConfigValue<Integer> ENERGY_PER_BLOCK;
    //public static final ModConfigSpec.ConfigValue<Boolean> AUTOMATICALLY_ADD_MM_STRUCTURES;

    // Block Replacement Configuration
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BLOCK_EQUIVALENCY_SETS;
    

    
    static {
        BUILDER.push("Energy Settings");
        
        MAX_ENERGY = BUILDER
                .comment("Maximum energy capacity of the Multibuilder Tool (in FE)")
                .defineInRange("maxEnergy", 10000000, 100000, 100000000);
        
        ENERGY_TRANSFER_RATE = BUILDER
                .comment("Energy transfer rate for charging/discharging (in FE/tick)")
                .defineInRange("energyTransferRate", 100000, 10000, 1000000);
        
        ENERGY_PER_BLOCK = BUILDER
                .comment("Energy cost per block placed (in FE)")
                .defineInRange("energyPerBlock", 100, 1, 10000);
        
        BUILDER.pop();
        
        BUILDER.push("Block Replacement Settings");
        
        BLOCK_EQUIVALENCY_SETS = BUILDER
                .comment("Define sets of equivalent blocks that can be used as replacements.",
                        "Each line represents a set of equivalent blocks separated by commas.",
                        "Example: 'mekanism:basic_mechanical_pipe,mekanism:advanced_mechanical_pipe,mekanism:elite_mechanical_pipe'",
                        "If a structure requires 'elite_mechanical_pipe' but you only have 'basic_mechanical_pipe',",
                        "the builder will use the basic pipe as a replacement.")
                .defineList("blockEquivalencySets", 
                    java.util.Arrays.asList(
                        "mekanism:basic_mechanical_pipe,mekanism:advanced_mechanical_pipe,mekanism:elite_mechanical_pipe,mekanism:ultimate_mechanical_pipe"
                    ), 
                    obj -> obj instanceof String);
        
        BUILDER.pop();

       /* BUILDER.push("Integration Settings");

        AUTOMATICALLY_ADD_MM_STRUCTURES = BUILDER
                .comment("Add Masterful Machinery structures automatically if the mod is present")
                .define("add_masterful_machinery_structures", true);

        BUILDER.pop();*/

        SPEC = BUILDER.build();
    }
    
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "mbtool-common.toml");
    }
    
    // Convenience methods for getting config values
    public static int getMaxEnergy() {
        return MAX_ENERGY.get();
    }
    
    public static int getEnergyTransferRate() {
        return ENERGY_TRANSFER_RATE.get();
    }
    
    public static int getEnergyPerBlock() {
        return ENERGY_PER_BLOCK.get();
    }
    
    public static java.util.List<? extends String> getBlockEquivalencySets() {
        return BLOCK_EQUIVALENCY_SETS.get();
    }
}