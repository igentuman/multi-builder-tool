package igentuman.mbtool.container;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CustomSlotHandler extends SlotItemHandler {
    public CustomSlotHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public int getMaxStackSize() {
        return 512;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 512;
    }
}
