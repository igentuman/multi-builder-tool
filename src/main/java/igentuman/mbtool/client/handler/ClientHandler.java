package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.render.PreviewRenderer;
import igentuman.mbtool.common.item.ItemMultiBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.awt.*;

public class ClientHandler
{
	public static boolean hasRecipe(ItemStack item)
	{
		try {
			return item.getOrCreateTag().contains("recipe");
		} catch (NullPointerException ignored) {
			return false;
		}
	}


/*	@SubscribeEvent
	public void renderLast(RenderWorldLastEvent event)
	{
		Minecraft mc = Minecraft.getMinecraft();
		ItemStack mainItem = mc.player.getHeldItemMainhand();
		ItemStack secondItem = mc.player.getHeldItemOffhand();

		boolean main = !mainItem.isEmpty() &&
				mainItem.getItem() instanceof ItemMultiBuilder &&
				hasRecipe(mainItem);
		boolean off = !secondItem.isEmpty() &&
				secondItem.getItem() instanceof ItemMultiBuilder  &&
				hasRecipe(secondItem);

		if(!main && !off) {
			return;
		}
		if(main && ((ItemMultiBuilder)mainItem.getItem()).afterPlaceDelay > 0) return;
		if(off && ((ItemMultiBuilder)secondItem.getItem()).afterPlaceDelay > 0) return;
		renderRays(event.getPartialTicks());
		PreviewRenderer.renderPreview(event.getPartialTicks());
	}*/
}
