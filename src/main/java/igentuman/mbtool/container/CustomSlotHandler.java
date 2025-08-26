package igentuman.mbtool.container;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CustomSlotHandler extends SlotItemHandler {
    public CustomSlotHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        // Return the custom stack size of 512 for all items in this slot
        // This allows stacks larger than the default 64
        return 512;
    }
}
