package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.PacketJeiRecipeTransfer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MultiblockRecipeHandler<T extends AbstractContainerMenu> implements StandardRecipeHandler<T> {

    @Override
    public List<Slot> getInputSources(T handler) {
        List<Slot> inputs = new ArrayList<>();
        if (!(handler instanceof MultibuilderContainer)) {
            return inputs;
        }
        for (int i = 0; i < handler.slots.size(); i++) {
            inputs.add(handler.getSlot(i));
        }
        return inputs;
    }

    @Override
    public List<Slot> getCraftingSlots(T handler) {
        return new ArrayList<>();
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof MultiblockStructureEmiRecipe;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return context.getScreenHandler() instanceof MultibuilderContainer;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (!(recipe instanceof MultiblockStructureEmiRecipe mbtoolRecipe)) {
            return false;
        }
        if (!(context.getScreenHandler() instanceof MultibuilderContainer)) {
            return false;
        }

        List<ItemStack> items = new ArrayList<>();
        for (EmiIngredient output : mbtoolRecipe.getInputs()) {
            ItemStack stack = output.getEmiStacks().get(0).getItemStack();
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        NetworkHandler.INSTANCE.sendToServer(new PacketJeiRecipeTransfer(items));
        return true;
    }
}
