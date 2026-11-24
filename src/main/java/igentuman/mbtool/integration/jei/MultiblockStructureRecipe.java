package igentuman.mbtool.integration.jei;

import igentuman.mbtool.integration.common.StructureMaterials;
import igentuman.mbtool.util.MultiblockStructure;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class MultiblockStructureRecipe {
    private final Identifier id;
    private final CompoundTag structureNbt;
    private final String name;
    private final MultiblockStructure structure;
    public int currentLayer = 0;
    public List<ItemStack> outputs;
    private IIngredientManager ingredientManager;

    public MultiblockStructureRecipe(Identifier id, CompoundTag structureNbt, String name, IIngredientManager ingredientManager) {
        this.id = id;
        this.structureNbt = structureNbt;
        this.name =  name;
        this.structure = new MultiblockStructure(structureNbt);
        this.currentLayer = structure.getMaxY();
        this.outputs = StructureMaterials.of(structure);
        this.ingredientManager = ingredientManager;
    }
    
    public Identifier getId() {
        return id;
    }
    
    public CompoundTag getStructureNbt() {
        return structureNbt;
    }
    
    public String getName() {
        return name;
    }

    public MultiblockStructure getStructure() {
        return structure;
    }

    public void slice() {
        if(structure.getMaxY() < currentLayer) {
            currentLayer = structure.getMaxY();
        }
        currentLayer--;
        if (currentLayer < structure.getMinY()) {
            currentLayer = structure.getMaxY();
        }
    }

    public Ingredient getIngredients() {
        return Ingredient.of(outputs.stream().map(ItemStack::getItem));
    }

    public IIngredientManager getIngredientManager() {
        return ingredientManager;
    }
}