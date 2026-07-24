package igentuman.mbtool.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.registration.MbtoolDataComponents.INVENTORY;

@SuppressWarnings("deprecation")
public class ItemInventoryHandler extends ItemStackHandler {

    protected ItemStack stack;
    protected HolderLookup.Provider provider;

    public ItemInventoryHandler(int slots) {
        super(slots);
    }

    public ItemInventoryHandler(ItemStack stack, int slots, HolderLookup.Provider provider) {
        super(slots);
        this.stack = stack;
        this.provider = provider;
        load();
    }

    private void load() {
        if (stack != null && stack.has(INVENTORY.get())) {
            CompoundTag tag = stack.get(INVENTORY.get());
            if (tag != null) {
                deserializeNBT(provider, tag);
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !stack.is(MBTOOL.get());
    }

    @Override
    public int getSlotLimit(int slot) {
        return 512;
    }

    @Override
    public void onContentsChanged(int slot) {
        if (stack != null) {
            stack.set(INVENTORY.get(), serializeNBT(provider));
        }
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack itemStack = stacks.get(i);
            if (!itemStack.isEmpty()) {
                int realCount = itemStack.getCount();
                ItemStack copy = itemStack.copy();
                copy.setCount(1);
                Tag savedTag = ItemStack.CODEC.encodeStart(
                        provider.createSerializationContext(NbtOps.INSTANCE), copy
                ).getOrThrow();
                if (savedTag instanceof CompoundTag itemTag) {
                    itemTag.putInt("count", realCount);
                    itemTag.putByte("Slot", (byte) i);
                    nbtTagList.add(itemTag);
                }
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        setSize(nbt.contains("Size") ? nbt.getIntOr("Size", stacks.size()) : stacks.size());
        ListTag tagList = nbt.getListOrEmpty("Items");
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompoundOrEmpty(i);
            int slot = itemTag.getByteOr("Slot", (byte) 0) & 255;
            if (slot < stacks.size()) {
                int realCount = itemTag.getIntOr("count", 1);
                ItemStack parsed = ItemStack.OPTIONAL_CODEC.parse(
                        provider.createSerializationContext(NbtOps.INSTANCE), itemTag
                ).result().orElse(ItemStack.EMPTY);
                if (!parsed.isEmpty()) {
                    parsed.setCount(realCount);
                }
                stacks.set(slot, parsed);
            }
        }
        onContentsChanged(0);
    }
}
