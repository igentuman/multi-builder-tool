package igentuman.mbtool.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class MbtoolConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Energy Configuration
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_ENERGY;
    public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TRANSFER_RATE;
    public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_PER_BLOCK;
    

    
    static {
        BUILDER.push("Energy Settings");
        
        MAX_ENERGY = BUILDER
                .comment("Maximum energy capacity of the Multibuilder Tool (in FE)")
                .defineInRange("maxEnergy", 1000000, 1000, 10000000);
        
        ENERGY_TRANSFER_RATE = BUILDER
                .comment("Energy transfer rate for charging/discharging (in FE/tick)")
                .defineInRange("energyTransferRate", 1000, 1, 100000);
        
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