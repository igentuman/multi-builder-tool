package igentuman.mbtool.integration.ae2;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import igentuman.mbtool.integration.emi.MultiblockStructureEmiRecipe;
import igentuman.mbtool.network.PacketAE2PatternTransfer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MbtoolRecipeHandler<T extends AbstractContainerMenu> implements StandardRecipeHandler<T> {

    @Override
    public List<Slot> getInputSources(T handler) {
        if (handler instanceof PatternEncodingTermMenu patternEncodingTermMenu) {
            return List.of(patternEncodingTermMenu.getProcessingInputSlots());
        }
        List<Slot> inputs = new ArrayList<>();
        for (int i = 0; i < handler.slots.size(); i++) {
            inputs.add(handler.getSlot(i));
        }
        return inputs;
    }

    @Override
    public List<Slot> getCraftingSlots(T handler) {
        if (handler instanceof PatternEncodingTermMenu patternEncodingTermMenu) {
            return List.of(patternEncodingTermMenu.getProcessingInputSlots());
        }
        return new ArrayList<>();
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof MultiblockStructureEmiRecipe;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return true;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (!(recipe instanceof MultiblockStructureEmiRecipe mbtoolRecipe)) {
            return false;
        }

        T handler = context.getScreenHandler();
        if (!(handler instanceof PatternEncodingTermMenu)) {
            return false;
        }

        try {
            List<ItemStack> inputItems = new ArrayList<>();

            for (EmiStack output : mbtoolRecipe.getOutputs()) {
                ItemStack stack = output.getItemStack();
                if (!stack.isEmpty()) {
                    inputItems.add(stack.copy());
                }
            }

            PacketDistributor.sendToServer(new PacketAE2PatternTransfer(inputItems, new ArrayList<>()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
