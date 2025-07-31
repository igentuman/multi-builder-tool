package igentuman.mbtool.container;

import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultibuilderContainer extends AbstractContainerMenu {
    
    private static final int INVENTORY_SIZE = 24;
    private final IItemHandler itemHandler;
    private final int playerSlot;
    
    public MultibuilderContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
        this.itemHandler = null;
        this.playerSlot = -1;
    }

    public MultibuilderContainer(int pContainerId, BlockPos pos, Inventory pPlayerInventory, int slot) {
        super(MULTIBUILDER_CONTAINER.get(), pContainerId);
        this.playerSlot = slot;
        
        // Get the multibuilder item from the player's inventory
        ItemStack multibuilderStack = slot == 40 ? pPlayerInventory.offhand.get(0) : pPlayerInventory.items.get(slot);
        
        if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
            this.itemHandler = multibuilderItem.getInventory(multibuilderStack);
        } else {
            this.itemHandler = null;
        }
        
        // Add multibuilder inventory slots (6x4 grid)
        if (itemHandler != null) {
            addMultibuilderInventory();
        }
        
        // Add player inventory slots
        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);
    }
    
    private void addMultibuilderInventory() {
        // 6 columns, 4 rows = 24 slots
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 6; col++) {
                int index = row * 6 + col;
                this.addSlot(new SlotItemHandler(itemHandler, index, 10 + col * 18, 13 + row * 18));
            }
        }
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        int yOffset = 96;
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 10 + l * 18, yOffset + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        int yOffset = 154;
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 10 + i * 18, yOffset));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // Multibuilder inventory slots: 0-23
            // Player inventory slots: 24-59 (24-50 main inventory, 51-59 hotbar)
            
            if (pIndex < INVENTORY_SIZE) {
                // Moving from multibuilder inventory to player inventory
                if (!this.moveItemStackTo(itemstack1, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to multibuilder inventory
                // Don't allow moving the multibuilder item itself
                if (pIndex - INVENTORY_SIZE == playerSlot || (playerSlot == 40 && pIndex == this.slots.size() - 1)) {
                    return ItemStack.EMPTY;
                }
                
                if (!this.moveItemStackTo(itemstack1, 0, INVENTORY_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
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
        // Check if the multibuilder item is still in the correct slot
        if (playerSlot == 40) {
            // Offhand slot
            return pPlayer.getItemInHand(InteractionHand.OFF_HAND).is(MBTOOL.get());
        } else {
            // Main hand or other inventory slot
            return pPlayer.getInventory().getItem(playerSlot).is(MBTOOL.get());
        }
    }
    
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
}
