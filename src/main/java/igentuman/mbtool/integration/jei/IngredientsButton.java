package igentuman.mbtool.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    public void draw(GuiGraphicsExtractor guiGraphics, int x, int y, float partialTicks) {
        IDrawable icon = Internal.getTextures().getBookmarkButtonEnabledIcon();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float)x, (float)y);
        guiGraphics.pose().scale(0.5f, 0.5f);
        icon.draw(guiGraphics, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (bounds == null) return false;
        return mouseX >= bounds.getX() && mouseX <= bounds.getX() + bounds.getWidth() &&
                mouseY >= bounds.getY() && mouseY <= bounds.getY() + bounds.getHeight();
    }

    public void drawTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        for (ItemStack i : recipe.outputs) {
            tooltip.add(Component.literal(i.getCount() + "x ").append(i.getHoverName()));
        }
        guiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
    }
}
