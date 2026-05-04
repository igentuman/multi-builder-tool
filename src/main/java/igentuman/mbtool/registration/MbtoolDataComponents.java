package igentuman.mbtool.registration;

import igentuman.mbtool.Mbtool;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.util.ExtraCodecs;

import java.util.function.UnaryOperator;

public class MbtoolDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Mbtool.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STRUCTURE_RECIPE = register("structure_recipe", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STRUCTURE_ROTATION = register("structure_rotation", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> RUNTIME_STRUCTURE = register("runtime_structure", builder -> builder.persistent(CompoundTag.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> UUID = register("uuid", builder -> builder.persistent(net.minecraft.core.UUIDUtil.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = register("energy", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> INVENTORY = register("inventory", builder -> builder.persistent(CompoundTag.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> PLACED_STRUCTURES = register("placed_structures", builder -> builder.persistent(CompoundTag.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ME_ACCESS = register("me_access", builder -> builder.persistent(com.mojang.serialization.Codec.BOOL));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return DATA_COMPONENT_TYPES.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }
}
