package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import igentuman.mbtool.Mbtool;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static igentuman.mbtool.Mbtool.MODID;

public class MultiblockStructureEmiCategory extends EmiRecipeCategory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "multiblock_structure");
    public static final MultiblockStructureEmiCategory INSTANCE = new MultiblockStructureEmiCategory();
    
    public MultiblockStructureEmiCategory() {
        super(ID, EmiStack.of(Mbtool.MBTOOL.get()), EmiTexture.EMPTY_ARROW);
    }
    
    @Override
    public Component getName() {
        return Component.translatable("emi.category." + MODID + ".multiblock_structure");
    }
}