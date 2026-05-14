package igentuman.mbtool.network;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketAE2PatternTransfer {
    private static Field encodingLogicField;

    static {
        try {
            encodingLogicField = PatternEncodingTermMenu.class.getDeclaredField("encodingLogic");
            encodingLogicField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to find encodingLogic field in PatternEncodingTermMenu", e);
        }
    }

    private final List<ItemStack> inputItems;
    private final List<ItemStack> outputItems;

    public PacketAE2PatternTransfer(List<ItemStack> inputItems, List<ItemStack> outputItems) {
        this.inputItems = inputItems;
        this.outputItems = outputItems;
    }

    public static void encode(PacketAE2PatternTransfer packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.inputItems.size());
        for (ItemStack stack : packet.inputItems) {
            writeItemStack(buffer, stack, true);
        }
        buffer.writeInt(packet.outputItems.size());
        for (ItemStack stack : packet.outputItems) {
            writeItemStack(buffer, stack, true);
        }
    }

    public static FriendlyByteBuf writeItemStack(FriendlyByteBuf buffer, ItemStack pStack, boolean limitedTag) {
        if (pStack.isEmpty()) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            Item item = pStack.getItem();
            buffer.writeId(BuiltInRegistries.ITEM, item);
            buffer.writeInt(pStack.getCount());
            CompoundTag compoundtag = null;
            if (item.isDamageable(pStack) || item.shouldOverrideMultiplayerNbt()) {
                compoundtag = limitedTag ? pStack.getShareTag() : pStack.getTag();
            }

            buffer.writeNbt(compoundtag);
        }

        return buffer;
    }

    public static ItemStack readItem(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            Item item = buffer.readById(BuiltInRegistries.ITEM);
            int i = buffer.readInt();
            ItemStack itemstack = new ItemStack(item, i);
            itemstack.readShareTag(buffer.readNbt());
            return itemstack;
        }
    }

    public static PacketAE2PatternTransfer decode(FriendlyByteBuf buffer) {
        int inputSize = buffer.readInt();
        List<ItemStack> inputItems = new ArrayList<>(inputSize);
        for (int i = 0; i < inputSize; i++) {
            inputItems.add(readItem(buffer));
        }
        int outputSize = buffer.readInt();
        List<ItemStack> outputItems = new ArrayList<>(outputSize);
        for (int i = 0; i < outputSize; i++) {
            outputItems.add(readItem(buffer));
        }
        return new PacketAE2PatternTransfer(inputItems, outputItems);
    }

    public static void handle(PacketAE2PatternTransfer packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!(player.containerMenu instanceof PatternEncodingTermMenu patternEncodingTermMenu)) return;

            try {
                patternEncodingTermMenu.setMode(EncodingMode.PROCESSING);

                PatternEncodingLogic encodingLogic = (PatternEncodingLogic) encodingLogicField.get(patternEncodingTermMenu);

                ConfigInventory encodedInputInv = encodingLogic.getEncodedInputInv();
                ConfigInventory encodedOutputInv = encodingLogic.getEncodedOutputInv();

                encodedInputInv.clear();
                encodedOutputInv.clear();

                int inputSlot = 0;
                for (ItemStack stack : packet.inputItems) {
                    stack.setCount(Math.abs(stack.getCount()));
                    if (inputSlot >= encodedInputInv.size()) break;
                    if (!stack.isEmpty()) {
                        AEItemKey itemKey = AEItemKey.of(stack);
                        if (itemKey != null) {
                            encodedInputInv.setStack(inputSlot++, new GenericStack(itemKey, stack.getCount()));
                        }
                    }
                }

                encodedOutputInv.clear();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        context.setPacketHandled(true);
    }
}
