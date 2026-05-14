package igentuman.mbtool.integration.jei;

import igentuman.mbtool.util.MultiblocksProvider;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    private IIngredientManager ingredientManager;
    private static IJeiRuntime jeiRuntime;
    private static List<MultiblockStructureRecipe> currentRecipes = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return rl("jei_plugin");
    }

    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MultiblockStructureCategory(registration.getJeiHelpers().getGuiHelper())
        );
        ingredientManager = registration.getJeiHelpers().getIngredientManager();
    }

    public void registerRecipes(IRecipeRegistration registration) {
        currentRecipes = buildRecipes(registration.getJeiHelpers().getIngredientManager());
        registration.addRecipes(MultiblockStructureCategory.TYPE, currentRecipes);
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        jeiRuntime = runtime;
        MultiblocksProvider.setOnStructuresChangedCallback(JEIPlugin::reloadRecipes);
    }

    @Override
    public void registerRecipeTransferHandlers(@NotNull IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new MultiblockRecipeHandler(), MultiblockStructureCategory.TYPE);
        registration.addRecipeTransferHandler(new MultiblockAE2TransferHandler(), MultiblockStructureCategory.TYPE);
    }

    private static List<MultiblockStructureRecipe> buildRecipes(IIngredientManager manager) {
        return MultiblocksProvider.getStructures()
                .stream()
                .map(structure -> new MultiblockStructureRecipe(
                        structure.getId(),
                        structure.getStructureNbt(),
                        structure.getName(),
                        manager))
                .toList();
    }

    private static void reloadRecipes() {
        if (jeiRuntime == null) return;
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        if (!currentRecipes.isEmpty()) {
            recipeManager.hideRecipes(MultiblockStructureCategory.TYPE, currentRecipes);
        }
        currentRecipes = buildRecipes(jeiRuntime.getIngredientManager());
        recipeManager.addRecipes(MultiblockStructureCategory.TYPE, currentRecipes);
        recipeManager.unhideRecipeCategory(MultiblockStructureCategory.TYPE);
    }
}
