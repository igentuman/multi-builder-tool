package igentuman.mbtool.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IngredientsButton {
    public static IngredientsButton create(MultiblockStructureRecipe recipe) {
        return new IngredientsButton(recipe);
    }

    public MultiblockStructureRecipe recipe;
    private Rect2i bounds;

    private IngredientsButton(MultiblockStructureRecipe recipe) {
        this.recipe = recipe;
    }

    public void updateBounds(Rect2i bounds) {
        this.bounds = bounds;
    }

    public void draw(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        IDrawable icon = Internal.getTextures().getBookmarkButtonEnabledIcon();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1f);
        icon.draw(guiGraphics, 0, 0);
        guiGraphics.pose().popPose();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (bounds == null) return false;
        return mouseX >= bounds.getX() && mouseX <= bounds.getX() + bounds.getWidth() &&
                mouseY >= bounds.getY() && mouseY <= bounds.getY() + bounds.getHeight();
    }

    public void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        for (ItemStack i : recipe.getIngredients().getItems()) {
            tooltip.add(Component.literal(i.getCount() + "x ").append(i.getHoverName()));
        }
        guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }
}
