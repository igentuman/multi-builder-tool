package igentuman.mbtool.integration.emi;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import igentuman.mbtool.network.PacketAE2PatternTransfer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MultiblockAE2EmiRecipeHandler implements EmiRecipeHandler<PatternEncodingTermMenu> {

    static void register(EmiRegistry registry) {
        registry.addRecipeHandler(PatternEncodingTermMenu.TYPE, new MultiblockAE2EmiRecipeHandler());
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<PatternEncodingTermMenu> screen) {
        return new EmiPlayerInventory(List.of());
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof MultiblockEmiRecipe;
    }

    @Override
    public boolean alwaysDisplaySupport(EmiRecipe recipe) {
        return true;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<PatternEncodingTermMenu> context) {
        return true;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<PatternEncodingTermMenu> context) {
        if (!(recipe instanceof MultiblockEmiRecipe multiblock)) {
            return false;
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack stack : multiblock.getMaterials()) {
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        ClientPacketDistributor.sendToServer(new PacketAE2PatternTransfer(inputs, new ArrayList<>()));
        return true;
    }
}
