package igentuman.mbtool.integration.jei;

import igentuman.mbtool.util.MultiblocksProvider;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    private static IIngredientManager ingredientManager;
    private static IJeiRuntime jeiRuntime;

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

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public void registerRecipes(IRecipeRegistration registration) {
        List<MultiblockStructureRecipe> multiblockRecipes = loadMultiblockStructures();
        registration.addRecipes(MultiblockStructureCategory.TYPE, multiblockRecipes);
    }

    public static void updateRecipes() {
        if (jeiRuntime == null || ingredientManager == null) return;
        List<MultiblockStructureRecipe> multiblockRecipes = loadMultiblockStructures();
        if (!multiblockRecipes.isEmpty()) {
            jeiRuntime.getRecipeManager().addRecipes(MultiblockStructureCategory.TYPE, multiblockRecipes);
        }
    }

    @Override
    public void registerRecipeTransferHandlers(@NotNull IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new MultiblockRecipeHandler(), MultiblockStructureCategory.TYPE);
        registration.addRecipeTransferHandler(new MultiblockAE2TransferHandler(), MultiblockStructureCategory.TYPE);
    }

    private static List<MultiblockStructureRecipe> loadMultiblockStructures() {
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
