package igentuman.mbtool.integration.jei;

import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.network.PacketJeiRecipeTransfer;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultiblockRecipeHandler implements IRecipeTransferHandler<MultibuilderContainer, MultiblockStructureRecipe> {

    private final Class<MultibuilderContainer> containerClass = MultibuilderContainer.class;

    @Override
    public Class<MultibuilderContainer> getContainerClass() {
        return containerClass;
    }

    @Override
    public Optional<MenuType<MultibuilderContainer>> getMenuType() {
        return Optional.of(MULTIBUILDER_CONTAINER.get());
    }

    @Override
    public RecipeType<MultiblockStructureRecipe> getRecipeType() {
        return MultiblockStructureCategory.TYPE;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(MultibuilderContainer container, MultiblockStructureRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return checkIngredients(recipe, player);
        }
        return performTransfer(recipe);
    }

    private @Nullable IRecipeTransferError checkIngredients(MultiblockStructureRecipe recipe, Player player) {
        for (ItemStack required : recipe.outputs) {
            if (required.isEmpty()) continue;
            if (!hasItemInInventory(player, required)) {
                return new MultiblockRecipeTransferError();
            }
        }
        return null;
    }

    private @Nullable IRecipeTransferError performTransfer(MultiblockStructureRecipe recipe) {
        PacketDistributor.sendToServer(new PacketJeiRecipeTransfer(new ArrayList<>(recipe.outputs)));
        return null;
    }

    private boolean hasItemInInventory(Player player, ItemStack required) {
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, required)) {
                found += stack.getCount();
            }
        }
        return found >= required.getCount();
    }

    private static class MultiblockRecipeTransferError implements IRecipeTransferError {
        @Override
        public Type getType() {
            return Type.USER_FACING;
        }
    }
}
