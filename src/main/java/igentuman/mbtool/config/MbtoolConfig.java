package igentuman.mbtool.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;

public class MbtoolConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Energy Configuration
    public static final ModConfigSpec.ConfigValue<Integer> MAX_ENERGY;
    public static final ModConfigSpec.ConfigValue<Integer> ENERGY_TRANSFER_RATE;
    public static final ModConfigSpec.ConfigValue<Integer> ENERGY_PER_BLOCK;
    

    
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
        
        SPEC = BUILDER.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "mbtool-common.toml");
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
}