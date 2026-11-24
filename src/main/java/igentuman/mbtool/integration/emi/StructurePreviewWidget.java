package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import igentuman.mbtool.client.gui.MultiblockButton;
import igentuman.mbtool.integration.common.StructurePreviewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static igentuman.mbtool.util.TextUtils.__;

public class StructurePreviewWidget extends Widget {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("recipe_book/button");
    private static final Identifier BUTTON_HOVER = Identifier.withDefaultNamespace("recipe_book/button_highlighted");
    private static final int BTN_X = 5, BTN_Y = 15, BTN_W = 10, BTN_H = 9;

    private final MultiblockEmiRecipe recipe;
    private final Bounds bounds;
    private final StructurePreviewState preview;

    public StructurePreviewWidget(MultiblockEmiRecipe recipe, int x, int y, int width, int height) {
        this.recipe = recipe;
        this.bounds = new Bounds(x, y, width, height);
        this.preview = new StructurePreviewState(x + width / 2.0, y + 75, 50);
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    private boolean isOverButton(int mouseX, int mouseY) {
        return mouseX >= bounds.x() + BTN_X && mouseX < bounds.x() + BTN_X + BTN_W
                && mouseY >= bounds.y() + BTN_Y && mouseY < bounds.y() + BTN_Y + BTN_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = bounds.x();
        int y = bounds.y();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isOverButton(mouseX, mouseY) ? BUTTON_HOVER : BUTTON,
                x + BTN_X, y + BTN_Y, BTN_W, BTN_H);
        graphics.text(Minecraft.getInstance().font, __(recipe.getName()), x + 5, y + 2, 0xFFFFFFFF, false);

        float angle = preview.update(mouseX, mouseY);

        int screenX = (int) graphics.pose().m20() + x + 20;
        int screenY = (int) graphics.pose().m21() + y + 20;
        MultiblockButton.renderStructureItems(graphics, recipe.getStructure(), screenX, screenY,
                bounds.width() - 40, bounds.height() - 30, angle);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        if (!isOverButton(mouseX, mouseY)) {
            return List.of();
        }
        List<ClientTooltipComponent> lines = new ArrayList<>();
        for (ItemStack stack : recipe.getMaterials()) {
            Component line = Component.literal(stack.getCount() + "x ").append(stack.getHoverName());
            lines.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return button == 0 && preview.isInRotationArea(mouseX, mouseY);
    }
}
