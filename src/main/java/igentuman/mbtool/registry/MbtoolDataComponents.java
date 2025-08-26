package igentuman.mbtool.registry;

import igentuman.mbtool.Mbtool;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MbtoolDataComponents {
    
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = 
        DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Mbtool.MODID);
    
    // Energy storage component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = 
        DATA_COMPONENTS.register("energy", () -> DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build());
    
    // Recipe index component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RECIPE = 
        DATA_COMPONENTS.register("recipe", () -> DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build());
    
    // Rotation component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROTATION = 
        DATA_COMPONENTS.register("rotation", () -> DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build());
}