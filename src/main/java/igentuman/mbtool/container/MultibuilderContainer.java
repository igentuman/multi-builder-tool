package igentuman.mbtool.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultibuilderContainer<T extends AbstractContainerMenu> extends AbstractContainerMenu {
    public MultibuilderContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public MultibuilderContainer(int pContainerId, BlockPos pos, Inventory pPlayerInventory) {
        super(MULTIBUILDER_CONTAINER.get(), pContainerId);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return false;
    }
}
