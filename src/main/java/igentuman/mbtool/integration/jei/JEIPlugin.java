package igentuman.mbtool.integration.jei;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.ModConfig;
import igentuman.mbtool.RegistryHandler;
import igentuman.mbtool.common.item.ItemMultiBuilder;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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
        NBTTagCompound tag = new NBTTagCompound();

        ItemStack cat = new ItemStack(RegistryHandler.MBTOOL, 1, 0).copy();
        registry.handleRecipes(MultiblockRecipe.class, MbtoolRecipeCategory.Wrapper::new, MbtoolRecipeCategory.UID);
        registry.addRecipeCatalyst(cat, MbtoolRecipeCategory.UID);

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
