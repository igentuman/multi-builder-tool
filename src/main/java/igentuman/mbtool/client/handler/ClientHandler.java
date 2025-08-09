package igentuman.mbtool.client.handler;

import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
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

    public static boolean canShowPreview(ItemStack mainItem) {
        if(mainItem.isEmpty()) return false;
        Item holding = mainItem.getItem();
        if(holding instanceof MultibuilderItem multibuilderItem) {
            return multibuilderItem.delay < 1;
        }
        return false;
    }
}
