package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import static igentuman.mbtool.Mbtool.MODID;

@EmiEntrypoint
public class MbtoolEmiPlugin implements EmiPlugin {
    public static final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath(MODID, "multiblock_structure");

    private static EmiRecipeCategory category;

    public static EmiRecipeCategory category() {
        if (category == null) {
            category = new EmiRecipeCategory(CATEGORY_ID, EmiStack.of(Mbtool.MBTOOL.get()));
        }
        return category;
    }

    @Override
    public void register(EmiRegistry registry) {
        EmiRecipeCategory cat = category();
        registry.addCategory(cat);
        registry.addWorkstation(cat, EmiStack.of(Mbtool.MBTOOL.get()));

        // The structure request item is an internal AE2 token, never a player-facing ingredient.
        registry.removeEmiStacks(EmiStack.of(Mbtool.STRUCTURE_REQUEST.get()));

        // EMI reloads on a worker thread while structure sync packets may still be arriving; iterate a snapshot.
        List<MultiblockStructure> structures = new ArrayList<>(MultiblocksProvider.getStructures());
        for (MultiblockStructure structure : structures) {
            registry.addRecipe(new MultiblockEmiRecipe(cat, structure));
        }

        registry.addRecipeHandler(Mbtool.MULTIBUILDER_CONTAINER.get(), new MultibuilderEmiRecipeHandler());
        if (ModUtil.isAe2Loaded()) {
            MultiblockAE2EmiRecipeHandler.register(registry);
        }
    }
}
