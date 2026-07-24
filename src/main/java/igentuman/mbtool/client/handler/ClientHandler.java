package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.DismantleHandler;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ClientHandler
{
	public static boolean hasStructure(ItemStack item)
	{
		try {
			MultibuilderItem multibuilderItem = (MultibuilderItem)item.getItem();
			return multibuilderItem.getCurrentStructure(item) != null;
		} catch (Exception ignored) {
			return false;
		}
	}

    public static boolean canShowPreview(Player player, ItemStack mainItem) {
        if(mainItem.isEmpty()) return false;

        // Hide preview when dismantling
        if(DismantleHandler.isDismantling()) {
            return false;
        }

        Item holding = mainItem.getItem();
        if(holding instanceof MultibuilderItem) {
            return !player.getCooldowns().isOnCooldown(mainItem);
        }
        return false;
    }
}
