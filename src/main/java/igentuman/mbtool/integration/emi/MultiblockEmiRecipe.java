package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.integration.common.StructureMaterials;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static igentuman.mbtool.Mbtool.MODID;

public class MultiblockEmiRecipe extends BasicEmiRecipe {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 120;

    private final MultiblockStructure structure;
    private final String name;
    private final List<ItemStack> materials;

    public MultiblockEmiRecipe(EmiRecipeCategory category, MultiblockStructure source) {
        super(category, recipeId(source.getId()), WIDTH, HEIGHT);
        this.structure = new MultiblockStructure(source.getStructureNbt());
        this.name = source.getName();
        this.materials = StructureMaterials.of(structure);

        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.catalysts = new ArrayList<>();
        this.catalysts.add(EmiStack.of(Mbtool.MBTOOL.get()));
        this.outputs.add(EmiStack.of(Mbtool.MBTOOL.get()));
        for (ItemStack stack : materials) {
            this.inputs.add(EmiStack.of(stack.copyWithCount(1), stack.getCount()));
            this.outputs.add(EmiStack.of(stack.copyWithCount(1), stack.getCount()));
        }
    }

    private static Identifier recipeId(Identifier structureId) {
        return Identifier.fromNamespaceAndPath(MODID,
                "/multiblock_structure/" + structureId.getNamespace() + "/" + structureId.getPath());
    }

    public MultiblockStructure getStructure() {
        return structure;
    }

    public String getName() {
        return name;
    }

    public List<ItemStack> getMaterials() {
        return materials;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.add(new StructurePreviewWidget(this, 0, 0, WIDTH, HEIGHT));
    }
}
