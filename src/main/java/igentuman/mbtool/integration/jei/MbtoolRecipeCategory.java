package igentuman.mbtool.integration.jei;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.client.render.RecipeRenderManager;
import igentuman.mbtool.recipe.MultiblockRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Vector3d;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.*;

public class MbtoolRecipeCategory implements IRecipeCategory<MbtoolRecipeCategory.Wrapper>, ITooltipCallback<ItemStack> {
    public static final String UID = Mbtool.MODID + "_multiblocks";
    private final String localizedName;
    private final IDrawableStatic background;
    private final IDrawableStatic slotDrawable;
    public static boolean zoom = false;
    public IRecipeLayout layout;

    public MbtoolRecipeCategory(IGuiHelper guiHelper) {
        localizedName = I18n.format(Mbtool.MODID+".jei.category.multiblocks");
        background = guiHelper.createBlankDrawable(174, 120);
        slotDrawable = guiHelper.getSlotDrawable();
    }


    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return localizedName;
    }

    /**
     * Return the name of the mod associated with this recipe category.
     * Used for the recipe category tab's tooltip.
     *
     * @since JEI 4.5.0
     */
    @Override
    public String getModName() {
        return Mbtool.MODID;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }


    @Nullable
    @Override
    public IDrawable getIcon() {
        return null;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        if(zoom) return;
        slotDrawable.draw(minecraft, 19 * 0, 0);
        slotDrawable.draw(minecraft, 19 * 1, 0);
        slotDrawable.draw(minecraft, 19 * 2, 0);
        slotDrawable.draw(minecraft, 19 * 3, 0);
        slotDrawable.draw(minecraft, 19 * 4, 0);
        slotDrawable.draw(minecraft, 19 * 5, 0);
        slotDrawable.draw(minecraft, 19 * 6, 0);
        slotDrawable.draw(minecraft, 19 * 7, 0);
        slotDrawable.draw(minecraft, 19 * 8, 0);

        slotDrawable.draw(minecraft, 19 * 0, 100);
        slotDrawable.draw(minecraft, 19 * 1, 100);
        slotDrawable.draw(minecraft, 19 * 2, 100);
        slotDrawable.draw(minecraft, 19 * 3, 100);
        slotDrawable.draw(minecraft, 19 * 4, 100);
        slotDrawable.draw(minecraft, 19 * 5, 100);
        slotDrawable.draw(minecraft, 19 * 6, 100);
        slotDrawable.draw(minecraft, 19 * 7, 100);
        slotDrawable.draw(minecraft, 19 * 8, 100);

    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, Wrapper recipeWrapper, IIngredients ingredients) {
        recipeWrapper.recipe.setCategory(this);
        layout = recipeLayout;
        if(zoom) {
            return;
        }
        recipeLayout.getItemStacks().init(0, true, 19 * 0, 0);
        recipeLayout.getItemStacks().init(1, true, 19 * 1, 0);
        recipeLayout.getItemStacks().init(2, true, 19 * 2, 0);
        recipeLayout.getItemStacks().init(3, true, 19 * 3, 0);
        recipeLayout.getItemStacks().init(4, true, 19 * 4, 0);
        recipeLayout.getItemStacks().init(5, true, 19 * 5, 0);
        recipeLayout.getItemStacks().init(6, true, 19 * 6, 0);
        recipeLayout.getItemStacks().init(7, true, 19 * 7, 0);
        recipeLayout.getItemStacks().init(8, true, 19 * 8, 0);

        recipeLayout.getItemStacks().init(9, true, 19 * 0, 100);
        recipeLayout.getItemStacks().init(10, true,19 * 1, 100);
        recipeLayout.getItemStacks().init(11, true,19 * 2, 100);
        recipeLayout.getItemStacks().init(12, true,19 * 3, 100);
        recipeLayout.getItemStacks().init(13, true,19 * 4, 100);
        recipeLayout.getItemStacks().init(14, true,19 * 5, 100);
        recipeLayout.getItemStacks().init(15, true,19 * 6, 100);
        recipeLayout.getItemStacks().init(16, true,19 * 7, 100);
        recipeLayout.getItemStacks().init(17, true,19 * 8, 100);

        recipeLayout.getItemStacks().addTooltipCallback(this);
        recipeLayout.getItemStacks().set(ingredients);
    }


    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return Collections.emptyList();
    }

    @Override
    public void onTooltip(int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) {
        String last = tooltip.get(tooltip.size()-1);
        tooltip.remove(tooltip.size()-1);
        if(slotIndex >= 0 && slotIndex <= 17) {
            tooltip.add(TextFormatting.YELLOW + I18n.format("tooltip.mbtool.jei.shape"));
        }
        tooltip.add(last);
    }

    public static class Wrapper implements IRecipeWrapper {
        public final MultiblockRecipe recipe;
        private final List<ItemStack> input = new ArrayList<>();
        public int layers;
        public IIngredients ingredients;

        public Wrapper(MultiblockRecipe recipe) {
            this.recipe = recipe;

            int added = 0;
            for(ItemStack stack : this.recipe.getRequiredItemStacks()) {
                this.input.add(stack);
                added++;
            }

            for(int emptySlot = 0; emptySlot < 17 - added; emptySlot++) {
                this.input.add(null);
            }
            layers = recipe.getHeight();
        }

        Map<Integer,? extends IGuiIngredient<ItemStack>> guiIngredients = new HashMap<>();

        @Override
        public void getIngredients(IIngredients ingredients) {
            this.ingredients = ingredients;
            ingredients.setInputs(ItemStack.class, input);
            ingredients.setOutput(ItemStack.class, this.recipe.getTargetStack());
        }

        public float td = 1f;
        public float ry = 0.5f;
        private int ticks = 90;

        private boolean isMouseOver(int mouseX, int mouseY, int width, int height){
            return 0 < mouseX && mouseX < width &&  0 < mouseY && mouseY < height;
        }

        public static int xMouse = 0;
        public static int yMouse = 0;
        public static float angle;
        public static float scale;
        public static Vector3d start = new Vector3d();

        @Override
        public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, 0F, 1.5F);
            GlStateManager.popMatrix();

            mc.fontRenderer.drawString(recipe.getLabel(), 170 - mc.fontRenderer.getStringWidth(recipe.getLabel()), 22, 0x444444);

            angle = ticks * 45.0f / 128.0f;
            ticks+=td;
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.disableFog();
            GlStateManager.disableLighting();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableAlpha();
            if (Minecraft.isAmbientOcclusionEnabled()) {
                GlStateManager.shadeModel(7425);
            } else {
                GlStateManager.shadeModel(7424);
            }

            GlStateManager.pushMatrix();

            // Center on recipe area
            GlStateManager.translate((float)(recipeWidth / 2), (float)(recipeHeight / 2), 255.0f);

            // Shift it a bit down so one can properly see 3d
            GlStateManager.rotate(-25.0f, 1.0f, 0.0f, 0.0f);

            // Rotate per our calculated time
            GlStateManager.rotate(angle, 0.0f, ry, 0.0f);

            // Scale down to gui scale
            GlStateManager.scale(16.0f, -16.0f, 16.0f);

            // Calculate the maximum size the shape has
            BlockPos mn = recipe.getMinPos();
            BlockPos mx = recipe.getMaxPos();
            int diffX = mx.getX() - mn.getX();
            int diffY = mx.getY() - mn.getY();
            int diffZ = mx.getZ() - mn.getZ();

            // We have big recipes, we need to adjust the size accordingly.
            int maxDiff = Math.max(Math.max(diffZ + 1, diffX), diffY+3) + 1;

            scale = 1.0f / ((float)maxDiff / 5.0f);
            recipe.getCategory().layout.getItemStacks().set(ingredients);
            recipe.getCategory().setRecipe(recipe.getCategory().layout, this, ingredients);

            GlStateManager.enableCull();
            GlStateManager.scale(scale, scale, scale);

            GlStateManager.translate(
                    (diffX + 1) / -2.0f,
                    (diffY + 1) / -2.0f,
                    (diffZ + 1) / -2.0f
            );


            recipe.setLevels(layers);
            RecipeRenderManager.instance.renderRecipe(recipe, 0.0f, layers);
            textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

            GlStateManager.popMatrix();
        }
    }
}
