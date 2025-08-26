package igentuman.mbtool.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.gui.elements.GuiIconToggleButton;
import mezz.jei.gui.input.UserInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IngredientsButton extends GuiIconToggleButton {
	public static IngredientsButton create(MultiblockStructureRecipe recipe) {
		Textures textures = Internal.getTextures();
		return new IngredientsButton(recipe);
	}

	public MultiblockStructureRecipe recipe;
	private IngredientsButton(MultiblockStructureRecipe recipe) {
		super(Internal.getTextures().getBookmarkButtonEnabledIcon(), Internal.getTextures().getBookmarkButtonEnabledIcon());
		this.recipe = recipe;
	}

	@Override
	protected void getTooltips(JeiTooltip tooltip) {
		for(Holder<Item> i : recipe.getIngredients())
			tooltip.add(FormattedText.of(i.getCount() + "x " + i.getHoverName().getString()));
	}

	public void draw(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(8, 8, 0);
		guiGraphics.pose().scale(0.5f, 0.5f, 1f);
		super.draw(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.pose().popPose();
	}

	@Override
	protected boolean isIconToggledOn() {
		return false;
	}

	@Override
	protected boolean onMouseClicked(UserInput input) {
		return false;
	}
}
