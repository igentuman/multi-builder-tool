package igentuman.mbtool.integration.jei;

import igentuman.mbtool.util.MultiblockStructure;
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
import org.lwjgl.glfw.GLFW;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.MODID;
import static igentuman.mbtool.util.TextUtils.__;

@SuppressWarnings("deprecation")
public class MultiblockStructureCategory implements mezz.jei.api.recipe.category.IRecipeCategory<MultiblockStructureRecipe> {
    public static final Identifier UID = Identifier.fromNamespaceAndPath(MODID, "multiblock_structure");
    public static final IRecipeType<MultiblockStructureRecipe> TYPE = IRecipeType.create(MODID, "multiblock_structure", MultiblockStructureRecipe.class);

    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;

    private boolean isMouseDragging = false;
    private double lastMouseX = 0;
    private float manualRotationAngle = 0;
    private long mouseReleaseTime = 0;
    private float autoRotationSpeed = (float)(Math.PI * 0.1);
    private double lastMouseY = 0;
    private float manualTiltAmount = 0.20f;
    private boolean sliceMode = false;
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
        ingredientsButton.updateBounds(new Rect2i(5, 15, 10, 10));
    }

    @Override
    public void draw(MultiblockStructureRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        ingredientsButton.recipe = recipe;
        ingredientsButton.draw(graphics, 5, 15, 1);
        if(ingredientsButton.isMouseOver(mouseX, mouseY)) {
            ingredientsButton.drawTooltips(graphics, (int) mouseX, (int) mouseY);
        }
        graphics.text(font, __(recipe.getName()), 5, 2, 0xFFFFFFFF, false);

        long window = Minecraft.getInstance().getWindow().handle();
        boolean leftMouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        long currentTime = System.currentTimeMillis();
        if (mouseReleaseTime == 0) {
            mouseReleaseTime = currentTime;
        }

        float angle;
        if (isMouseDragging) {
            angle = manualRotationAngle;
        } else {
            float timeDiff = (currentTime - mouseReleaseTime) / 1000.0f;
            angle = manualRotationAngle + (timeDiff * autoRotationSpeed);
            if (angle > Math.PI * 2) {
                angle %= (2.0f * (float)Math.PI);
                manualRotationAngle = angle;
                mouseReleaseTime = currentTime;
            }
        }

        if (leftMouseDown && !isMouseDragging && isMouseInRotationArea(mouseX, mouseY)) {
            isMouseDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            manualRotationAngle = angle;
        }

        if (isMouseDragging && !leftMouseDown) {
            isMouseDragging = false;
            mouseReleaseTime = currentTime;
        }
        if(isMouseDragging) {
            float sensitivity = 0.02f;
            float delta = (float) (mouseX - lastMouseX) * sensitivity;
            manualRotationAngle += delta;

            float tiltSensitivity = 0.02f;
            float tiltDelta = (float) (mouseY - lastMouseY) * tiltSensitivity;
            manualTiltAmount += tiltDelta;
            manualTiltAmount = Math.max(-0.5f, Math.min(0.5f, manualTiltAmount));

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        if(sliceMode) {
            recipe.slice();
            sliceMode = false;
        }

        // GuiStructureRenderState is a PictureInPictureRenderState, which requires x0/y0/x1/y1 in
        // absolute screen coordinates — JEI only translates graphics.pose(), it doesn't shift PIP
        // states, so the recipe area's screen offset must be baked in manually here.
        int screenX = (int) graphics.pose().m20() + 20;
        int screenY = (int) graphics.pose().m21() + 20;
        igentuman.mbtool.client.gui.MultiblockButton.renderStructureItems(
            graphics, recipe.getStructure(), screenX, screenY, WIDTH - 40, HEIGHT - 30, angle);
    }

    private boolean isMouseInRotationArea(double mouseX, double mouseY) {
        double centerX = 80;
        double centerY = 75;
        double radius = 50;
        return Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2) <= Math.pow(radius, 2);
    }
}
