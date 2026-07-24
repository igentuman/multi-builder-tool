package igentuman.mbtool.network;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import igentuman.mbtool.Mbtool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Field;
import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

public class PacketAE2PatternTransfer implements CustomPacketPayload {
    public static final Type<PacketAE2PatternTransfer> TYPE = new Type<>(rl("ae2_pattern_transfer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAE2PatternTransfer> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), PacketAE2PatternTransfer::inputItems,
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), PacketAE2PatternTransfer::outputItems,
        PacketAE2PatternTransfer::new
    );

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public List<ItemStack> inputItems() {
        return inputItems;
    }

    public List<ItemStack> outputItems() {
        return outputItems;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
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
                for (ItemStack stack : inputItems) {
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
    }
}
