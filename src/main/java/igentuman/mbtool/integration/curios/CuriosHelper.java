package igentuman.mbtool.integration.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

// Curios not available for NeoForge 26.1 yet
public class CuriosHelper {

    public static IItemHandler getEquippedCuriosHandler(Player player) {
        return null;
    }

    public static List<ItemStack> getEquippedCuriosItems(Player player) {
        return new ArrayList<>();
    }
}
