package igentuman.mbtool.integration.jei;

import igentuman.mbtool.common.MultiblocksProvider;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return rl("jei_plugin");
    }
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MultiblockStructureCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    public void registerRecipes(IRecipeRegistration registration) {
        List<MultiblockStructureRecipe> multiblockRecipes = loadMultiblockStructures();
        registration.addRecipes(MultiblockStructureCategory.TYPE, multiblockRecipes);
    }

    private List<MultiblockStructureRecipe> loadMultiblockStructures() {
        return MultiblocksProvider.loadMultiblockStructures()
                .stream()
                .map(structure -> new MultiblockStructureRecipe(
                        structure.getId(),
                        structure.getStructureNbt(),
                        structure.getName()))
                .toList();
    }
}
