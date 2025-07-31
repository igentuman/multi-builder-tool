package igentuman.mbtool.client.handler;

import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.world.item.ItemStack;

public class ClientHandler
{
	public static boolean hasRecipe(ItemStack item)
	{
		try {
			if (!item.getOrCreateTag().contains("recipe")) {
				return false;
			}
			
			// Ensure structures are loaded
			if (MultiblocksProvider.structures.isEmpty()) {
				MultiblocksProvider.loadMultiblockStructures();
			}
			
			int recipeIndex = item.getOrCreateTag().getInt("recipe");
			return recipeIndex >= 0 && recipeIndex < MultiblocksProvider.structures.size();
		} catch (Exception ignored) {
			return false;
		}
	}
}
