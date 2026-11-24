package igentuman.mbtool.network;

import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

final class MenuSession {
    private MenuSession() {}

    static int playerSlot(AbstractContainerMenu menu) {
        if (menu instanceof MultibuilderContainer container) return container.getPlayerSlot();
        if (menu instanceof MultibuilderSelectStructureContainer container) return container.getPlayerSlot();
        return -1;
    }

    static ItemStack toolStack(ServerPlayer player, int containerId, int slot) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId) return null;
        int menuSlot = playerSlot(menu);
        if (menuSlot < 0 || slot != menuSlot || slot >= player.getInventory().getContainerSize()) return null;
        if (!menu.stillValid(player)) return null;
        ItemStack stack = player.getInventory().getItem(slot);
        return stack.getItem() instanceof MultibuilderItem ? stack : null;
    }
}
