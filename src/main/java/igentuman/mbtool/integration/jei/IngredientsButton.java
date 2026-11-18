package igentuman.mbtool.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IngredientsButton {
	private static final ResourceLocation RECIPE_BUTTON = new ResourceLocation("textures/gui/recipe_button.png");
	private static final int TEX_W = 20;
	private static final int TEX_H = 18;
	private static final int HOVER_V = 19;

	public static IngredientsButton create(MultiblockStructureRecipe recipe) {
		return new IngredientsButton(recipe);
	}

	public MultiblockStructureRecipe recipe;
	private Rect2i area = new Rect2i(0, 0, 10, 10);

	private IngredientsButton(MultiblockStructureRecipe recipe) {
		this.recipe = recipe;
	}

	public void updateBounds(Rect2i area) {
		this.area = area;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
				&& mouseY >= area.getY() && mouseY < area.getY() + area.getHeight();
	}

	public void draw(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		int v = isMouseOver(mouseX, mouseY) ? HOVER_V : 0;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(area.getX(), area.getY(), 0);
		guiGraphics.pose().scale(area.getWidth() / (float) TEX_W, area.getHeight() / (float) TEX_H, 1f);
		guiGraphics.blit(RECIPE_BUTTON, 0, 0, 0, v, TEX_W, TEX_H, 256, 256);
		guiGraphics.pose().popPose();
	}

	public void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<Component> lines = new ArrayList<>();
		for (ItemStack i : recipe.getIngredients().getItems()) {
			lines.add(Component.literal(i.getCount() + "x " + i.getHoverName().getString()));
		}
		if (!lines.isEmpty()) {
			guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
		}
	}
}
