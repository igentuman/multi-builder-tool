package igentuman.mbtool.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import static igentuman.mbtool.registration.MbtoolDataComponents.INVENTORY;

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
    public int getSlotLimit(int slot) {
        return 512;
    }

    @Override
    public void onContentsChanged(int slot) {
        if (stack != null) {
            stack.set(INVENTORY.get(), serializeNBT(provider));
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                int realCount = stack.getCount();
                stack.setCount(1);
                CompoundTag itemTag = (CompoundTag) stack.save(provider);
                stack.setCount(realCount);
                itemTag.putInt("count", realCount);
                itemTag.putByte("Slot", (byte) i);
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < stacks.size()) {
                int realCount = itemTag.getInt("count");
                itemTag.putInt("count", 1);
                ItemStack parsed = ItemStack.parseOptional(provider, itemTag);
                if (!parsed.isEmpty()) {
                    parsed.setCount(realCount);
                }
                stacks.set(slot, parsed);
            }
        }
        onContentsChanged(0);
    }
}
