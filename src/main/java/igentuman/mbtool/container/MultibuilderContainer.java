package igentuman.mbtool.container;

import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.MultibuilderContainerSetContentPacket;
import igentuman.mbtool.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultibuilderContainer extends AbstractContainerMenu {
    
    private static final int INVENTORY_SIZE = 40;
    private final IItemHandler itemHandler;
    private final int playerSlot;
    private final UUID uuid;
    public Player serverPlayer;
    private ContainerSynchronizer containerSynchronizer;

    public MultibuilderContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
        this.itemHandler = null;
        this.playerSlot = -1;
        uuid = UUID.randomUUID();
    }



    public MultibuilderContainer(int pContainerId, BlockPos pos, Inventory pPlayerInventory, int slot) {
        super(MULTIBUILDER_CONTAINER.get(), pContainerId);
        this.playerSlot = slot;
        this.serverPlayer = pPlayerInventory.player;
        // Get the multibuilder item from the player's inventory
        ItemStack multibuilderStack = slot == 40 ? pPlayerInventory.offhand.get(0) : pPlayerInventory.items.get(slot);

        if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
            this.itemHandler = multibuilderItem.getInventory(multibuilderStack);
            uuid = multibuilderItem.getUUID(multibuilderStack);
        } else {
            this.itemHandler = null;
            uuid = UUID.randomUUID();
        }

        // Add multibuilder inventory slots (6x4 grid)
        if (itemHandler != null) {
            addMultibuilderInventory();
        }
        
        // Add player inventory slots
        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);
    }

    @Override
    public void setSynchronizer(ContainerSynchronizer pSynchronizer) {
        if(this.containerSynchronizer == null) {
            containerSynchronizer = new ContainerSynchronizer() {
                public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> p_143449_, ItemStack p_143450_, int[] p_143451_) {
                    sendPacket(new MultibuilderContainerSetContentPacket(menu.containerId, menu.incrementStateId(), p_143449_, p_143450_));

                    for(int i = 0; i < p_143451_.length; ++i) {
                        this.broadcastDataValue(menu, i, p_143451_[i]);
                    }

                }

                public void sendSlotChange(AbstractContainerMenu menu, int p_143442_, ItemStack p_143443_) {
                    sendPacket(new MultibuilderContainerSetContentPacket(menu.containerId, menu.incrementStateId(), p_143442_, p_143443_));
                }

                public void sendCarriedChange(AbstractContainerMenu menu, ItemStack p_143446_) {
                    sendPacket(new MultibuilderContainerSetContentPacket(-1, menu.incrementStateId(), -1, p_143446_));
                }

                public void sendDataChange(AbstractContainerMenu menu, int p_143438_, int p_143439_) {
                    this.broadcastDataValue(menu, p_143438_, p_143439_);
                }

                private void broadcastDataValue(AbstractContainerMenu menu, int p_143456_, int p_143457_) {
                    sendPacket(new MultibuilderContainerSetContentPacket(menu.containerId, p_143456_, p_143457_));
                }

                private void sendPacket(MultibuilderContainerSetContentPacket packet) {
                    if (serverPlayer instanceof ServerPlayer) {
                        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) serverPlayer), packet);
                    }
                }
            };
        }
        //it appeared, we don't need custom synchronizer anymore ¯\_(ツ)_/¯
        //super.setSynchronizer(containerSynchronizer);
    }
    
    private void addMultibuilderInventory() {
        // 8 columns, 5 rows = 40 slots
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 8; col++) {
                int index = row * 8 + col;
                this.addSlot(new CustomSlotHandler(getItemHandler(), index, 5 + col * 18, 13 + row * 18));
            }
        }
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        int yOffset = 107;
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 5 + l * 18, yOffset + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        int yOffset = 165;
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 5 + i * 18, yOffset));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            if(pPlayer.getItemInHand(InteractionHand.MAIN_HAND).equals(itemstack1)) {
                return ItemStack.EMPTY;
            }
            itemstack = itemstack1.copy();
            
            if (pIndex < INVENTORY_SIZE) {
                // Moving from multibuilder inventory to player inventory
                if (!this.moveItemStackTo(itemstack1, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
    protected boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
        boolean flag = false;
        int i = pStartIndex;
        if (pReverseDirection) {
            i = pEndIndex - 1;
        }

        if (pStack.isStackable()) {
            while(!pStack.isEmpty()) {
                if (pReverseDirection) {
                    if (i < pStartIndex) {
                        break;
                    }
                } else if (i >= pEndIndex) {
                    break;
                }

                Slot slot = this.slots.get(i);
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(pStack, itemstack)) {
                    int j = itemstack.getCount() + pStack.getCount();
                    // Always use the slot's max stack size (512) for our custom slots
                    int maxSize = slot.getMaxStackSize();
                    if (j <= maxSize) {
                        pStack.setCount(0);
                        itemstack.setCount(j);
                        slot.setByPlayer(itemstack);
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        pStack.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        slot.setByPlayer(itemstack);
                        slot.setChanged();
                        flag = true;
                    }
                }

                if (pReverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!pStack.isEmpty()) {
            if (pReverseDirection) {
                i = pEndIndex - 1;
            } else {
                i = pStartIndex;
            }

            while(true) {
                if (pReverseDirection) {
                    if (i < pStartIndex) {
                        break;
                    }
                } else if (i >= pEndIndex) {
                    break;
                }

                Slot slot1 = this.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(pStack)) {
                    // Use the slot's max stack size (512 for our custom slots)
                    if (pStack.getCount() > slot1.getMaxStackSize()) {
                        slot1.setByPlayer(pStack.split(slot1.getMaxStackSize()));
                    } else {
                        slot1.setByPlayer(pStack.split(pStack.getCount()));
                    }

                    slot1.setChanged();
                    flag = true;
                    break;
                }

                if (pReverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }
        return flag;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        ItemStack stack = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if(stack.getItem() instanceof MultibuilderItem multibuilderItem) {
            return multibuilderItem.getUUID(stack).equals(uuid);
        }
        return false;
    }
    
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
}
