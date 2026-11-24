package igentuman.mbtool.item;

import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.TextUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StructureRequestItem extends Item {
    public static final String TAG_STRUCTURE = "structure";

    public StructureRequestItem(Properties properties) {
        super(properties);
    }

    public static Component structureName(String key) {
        if (key == null || key.isBlank()) return Component.empty();
        return Component.translatableWithFallback(key, TextUtils.convertToName(key.replace("mbtool.structure.", "")));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.get(MbtoolDataComponents.STRUCTURE_REQUEST.get());
        if (tag != null && tag.contains(TAG_STRUCTURE)) {
            return Component.translatable("item.mbtool.structure_request.named",
                    structureName(tag.getStringOr(TAG_STRUCTURE, "")));
        }
        return super.getName(stack);
    }
}
