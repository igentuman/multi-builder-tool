package igentuman.mbtool.container;

import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultibuilderSelectStructureContainer extends AbstractContainerMenu {
    private final int playerSlot;
    private final Inventory playerInventory;
    private IItemHandler inventory;
    
    public MultibuilderSelectStructureContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
        this.playerSlot = -1;
        this.playerInventory = null;
    }

    public MultibuilderSelectStructureContainer(int pContainerId, BlockPos pos, Inventory pPlayerInventory, int slot) {
        super(MULTIBUILDER_CONTAINER.get(), pContainerId);
        this.playerSlot = slot;
        this.playerInventory = pPlayerInventory;
        ItemStack item = playerInventory.player.getItemInHand(InteractionHand.MAIN_HAND);
        if(item.getItem() instanceof MultibuilderItem multibuilderItem) {
            inventory = multibuilderItem.getInventory(item);
        }
    }
    
    public int getPlayerSlot() {
        return playerSlot;
    }
    
    public Inventory getPlayerInventory() {
        return playerInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.getItemInHand(InteractionHand.MAIN_HAND).is(MBTOOL.get());
    }

    public boolean hasEnough(ItemStack stack) {
        if(playerInventory.player.isCreative()) {
            return true;
        }
        if(inventory == null) {
            return false;
        }
        for (int id = 0; id < inventory.getSlots(); id++) {
            if(inventory.getStackInSlot(id).is(stack.getItem()) && inventory.getStackInSlot(id).getCount() >= stack.getCount()) {
                return true;
            }
        }
        return false;
    }
}
