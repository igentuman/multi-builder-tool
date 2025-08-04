package igentuman.mbtool.client.handler;

import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.world.item.ItemStack;

public class ClientHandler
{
	public static boolean hasRecipe(ItemStack item)
	{
		try {
			MultibuilderItem multibuilderItem = (MultibuilderItem)item.getItem();
			if (!item.getOrCreateTag().contains("recipe") && multibuilderItem.runtimeStructure == null) {
				return false;
			}
			
			// Ensure structures are loaded
			if (MultiblocksProvider.structures.isEmpty()) {
				MultiblocksProvider.getStructures();
			}
			
			int recipeIndex = item.getOrCreateTag().getInt("recipe");
			return recipeIndex >= 0 && recipeIndex < MultiblocksProvider.structures.size() || multibuilderItem.runtimeStructure != null;
		} catch (Exception ignored) {
			return false;
		}
	}
}
