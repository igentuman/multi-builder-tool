package igentuman.mbtool.integration.jei;

import igentuman.mbtool.util.MultiblocksProvider;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    private IIngredientManager ingredientManager;
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
        List<MultiblockStructureRecipe> multiblockRecipes = loadMultiblockStructures();
        registration.addRecipes(MultiblockStructureCategory.TYPE, multiblockRecipes);
    }

    private List<MultiblockStructureRecipe> loadMultiblockStructures() {
        return MultiblocksProvider.getStructures()
                .stream()
                .map(structure -> new MultiblockStructureRecipe(
                        structure.getId(),
                        structure.getStructureNbt(),
                        structure.getName(),
                        ingredientManager))
                .toList();
    }
}
