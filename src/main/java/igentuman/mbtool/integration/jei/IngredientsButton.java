package igentuman.mbtool.integration.jei;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Small ingredients toggle drawn with a vanilla sprite. It owns its icon, bounds and hover detection and
 * does not extend or call JEI internals (whose classes change between JEI releases and crashed before).
 */
public class IngredientsButton {
    private static final Identifier SPRITE = Identifier.withDefaultNamespace("recipe_book/button");
    private static final Identifier SPRITE_HOVER = Identifier.withDefaultNamespace("recipe_book/button_highlighted");

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

    public void draw(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isMouseOver(mouseX, mouseY) ? SPRITE_HOVER : SPRITE,
                area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    /** Material lines (counts per block) for JEI's own tooltip, drawn above the structure preview. */
    public List<FormattedText> tooltipLines() {
        List<FormattedText> lines = new ArrayList<>();
        for (ItemStack i : recipe.outputs) {
            lines.add(Component.literal(i.getCount() + "x ").append(i.getHoverName()));
        }
        return lines;
    }
}
