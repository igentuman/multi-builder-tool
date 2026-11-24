package igentuman.mbtool.integration.jei;

import igentuman.mbtool.integration.common.StructurePreviewState;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.MODID;
import static igentuman.mbtool.util.TextUtils.__;

@SuppressWarnings("deprecation")
public class MultiblockStructureCategory implements mezz.jei.api.recipe.category.IRecipeCategory<MultiblockStructureRecipe> {
    public static final Identifier UID = Identifier.fromNamespaceAndPath(MODID, "multiblock_structure");
    public static final IRecipeType<MultiblockStructureRecipe> TYPE = IRecipeType.create(MODID, "multiblock_structure", MultiblockStructureRecipe.class);

    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;

    private final StructurePreviewState preview = new StructurePreviewState(80, 75, 50);
    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;
    private IngredientsButton ingredientsButton;

    public MultiblockStructureCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(MBTOOL.get()));
        this.title = Component.translatable("jei.category." + MODID + ".multiblock_structure");
    }

    @Override
    public IRecipeType<MultiblockStructureRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockStructureRecipe recipe, IFocusGroup focuses) {
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION).addItemLike(MBTOOL.get());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemLike(MBTOOL.get());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addIngredients(recipe.getIngredients());
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(recipe.getIngredients());
        ingredientsButton = IngredientsButton.create(recipe);
        ingredientsButton.updateBounds(new Rect2i(5, 15, 10, 9));
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, MultiblockStructureRecipe recipe,
                           IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (ingredientsButton != null && ingredientsButton.isMouseOver(mouseX, mouseY)) {
            ingredientsButton.recipe = recipe;
            tooltip.addAll(ingredientsButton.tooltipLines());
        }
    }

    @Override
    public void draw(MultiblockStructureRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        ingredientsButton.recipe = recipe;
        ingredientsButton.draw(graphics, mouseX, mouseY);
        graphics.text(font, __(recipe.getName()), 5, 2, 0xFFFFFFFF, false);

        float angle = preview.update(mouseX, mouseY);

        int screenX = (int) graphics.pose().m20() + 20;
        int screenY = (int) graphics.pose().m21() + 20;
        igentuman.mbtool.client.gui.MultiblockButton.renderStructureItems(
            graphics, recipe.getStructure(), screenX, screenY, WIDTH - 40, HEIGHT - 30, angle);
    }
}
