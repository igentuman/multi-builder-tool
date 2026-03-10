package igentuman.mbtool.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemStackFriendlyByteBuf extends FriendlyByteBuf {
    public ItemStackFriendlyByteBuf(ByteBuf pSource) {
        super(pSource);
    }

    @Override
    public FriendlyByteBuf writeItemStack(ItemStack pStack, boolean limitedTag) {
        if (pStack.isEmpty()) {
            this.writeBoolean(false);
        } else {
            this.writeBoolean(true);
            Item item = pStack.getItem();
            this.writeId(BuiltInRegistries.ITEM, item);
            this.writeInt(pStack.getCount());
            CompoundTag compoundtag = null;
            if (item.isDamageable(pStack) || item.shouldOverrideMultiplayerNbt()) {
                compoundtag = limitedTag ? pStack.getShareTag() : pStack.getTag();
            }

            this.writeNbt(compoundtag);
        }

        return this;
    }

    /**
     * Reads an ItemStack from this buffer.
     *
     * @see #writeItem
     */
    @Override
    public ItemStack readItem() {
        if (!this.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            Item item = this.readById(BuiltInRegistries.ITEM);
            int i = this.readInt();
            ItemStack itemstack = new ItemStack(item, i);
            itemstack.readShareTag(this.readNbt());
            return itemstack;
        }
    }
}
