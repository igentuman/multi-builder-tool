package igentuman.mbtool.integration.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class CuriosHelper {

    /**
     * Returns the combined equipped curios inventory as an IItemHandler, or null if unavailable.
     */
    public static IItemHandler getEquippedCuriosHandler(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.getEquippedCurios())
                .orElse(null);
    }

    /**
     * Collects all non-empty ItemStacks from the player's equipped curios slots.
     */
    public static List<ItemStack> getEquippedCuriosItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        IItemHandler handler = getEquippedCuriosHandler(player);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }
        return items;
    }
}
