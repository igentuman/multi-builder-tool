package igentuman.mbtool.integration.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.PacketAE2PatternTransfer;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiblockAE2TransferHandler implements IRecipeTransferHandler<PatternEncodingTermMenu, MultiblockStructureRecipe> {

    @Override
    public Class<PatternEncodingTermMenu> getContainerClass() {
        return PatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
        return Optional.of(PatternEncodingTermMenu.TYPE);
    }

    @Override
    public RecipeType<MultiblockStructureRecipe> getRecipeType() {
        return MultiblockStructureCategory.TYPE;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(PatternEncodingTermMenu container, MultiblockStructureRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }

        List<ItemStack> inputItems = new ArrayList<>();
        for (ItemStack stack : recipe.outputs) {
            if (!stack.isEmpty()) {
                inputItems.add(stack.copy());
            }
        }

        NetworkHandler.INSTANCE.sendToServer(new PacketAE2PatternTransfer(inputItems, new ArrayList<>()));
        return null;
    }
}
