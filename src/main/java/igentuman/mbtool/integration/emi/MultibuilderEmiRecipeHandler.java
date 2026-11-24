package igentuman.mbtool.integration.emi;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.network.PacketJeiRecipeTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MultibuilderEmiRecipeHandler implements EmiRecipeHandler<MultibuilderContainer> {

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<MultibuilderContainer> screen) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return new EmiPlayerInventory(List.of());
        }
        return new EmiPlayerInventory(player.getInventory().getNonEquipmentItems().stream()
                .map(EmiStack::of)
                .toList());
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof MultiblockEmiRecipe;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<MultibuilderContainer> context) {
        return context.getInventory().canCraft(recipe);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<MultibuilderContainer> context) {
        if (!(recipe instanceof MultiblockEmiRecipe multiblock)) {
            return false;
        }
        ArrayList<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : multiblock.getMaterials()) {
            items.add(stack.copy());
        }
        ClientPacketDistributor.sendToServer(new PacketJeiRecipeTransfer(items));
        return true;
    }
}
