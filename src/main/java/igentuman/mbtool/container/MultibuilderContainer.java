package igentuman.mbtool.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultibuilderContainer<T extends AbstractContainerMenu> extends AbstractContainerMenu {
    public MultibuilderContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public MultibuilderContainer(int pContainerId, BlockPos pos, Inventory pPlayerInventory) {
        super(MULTIBUILDER_CONTAINER.get(), pContainerId);
        
        // Add player inventory slots
        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // If clicking on player inventory, try to move to container
            if (pIndex >= 0 && pIndex < 36) {
                // For now, just return the original stack since we don't have container slots yet
                return ItemStack.EMPTY;
            }
            
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(pPlayer, itemstack1);
        }
        
        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true; // Allow the container to stay open
    }
}
