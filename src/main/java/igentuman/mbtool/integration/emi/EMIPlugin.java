package igentuman.mbtool.integration.emi;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import igentuman.mbtool.integration.ae2.MbtoolRecipeHandler;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

@EmiEntrypoint
public class EMIPlugin implements EmiPlugin {
    
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(MultiblockStructureEmiCategory.INSTANCE);
        registry.addRecipeHandler(PatternEncodingTermMenu.TYPE, new MbtoolRecipeHandler<>());
        
        List<MultiblockStructureEmiRecipe> multiblockRecipes = loadMultiblockStructures();
        for (MultiblockStructureEmiRecipe recipe : multiblockRecipes) {
            registry.addRecipe(recipe);
        }
    }
    
    private List<MultiblockStructureEmiRecipe> loadMultiblockStructures() {
        return MultiblocksProvider.getStructures()
                .stream()
                .map(structure -> new MultiblockStructureEmiRecipe(
                        rl("/"+structure.getId().getPath()),
                        structure.getStructureNbt(),
                        structure.getName()))
                .toList();
    }
}