package igentuman.mbtool.integration.jei;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.RegistryHandler;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

@mezz.jei.api.JEIPlugin
public class JEIPlugin implements IModPlugin {
    public static JEIPlugin INSTANCE;
    public JEIPlugin() {
        super();
    }
    private IJeiRuntime runtime;

    @Override
    public void register(IModRegistry registry) {
        INSTANCE = this;
        //multiblocks
        registry.handleRecipes(MultiblockRecipe.class, MbtoolRecipeCategory.Wrapper::new, MbtoolRecipeCategory.UID);
        registry.addRecipeCatalyst(new ItemStack(RegistryHandler.MBTOOL), MbtoolRecipeCategory.UID);

        registry.addRecipes(MultiblockRecipes.getAvaliableRecipes(), MbtoolRecipeCategory.UID);

    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        final IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        //multiblocks
        registry.addRecipeCategories(new MbtoolRecipeCategory(guiHelper));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
    }

    public IJeiRuntime getRuntime() {
        return runtime;
    }
}
