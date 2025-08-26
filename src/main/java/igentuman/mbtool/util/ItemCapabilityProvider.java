package igentuman.mbtool.util;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class ItemCapabilityProvider implements ICapabilityProvider {
    
    private final ItemStack stack;
    private final CustomEnergyStorage energyStorage;
    private final ItemInventoryHandler inventoryHandler;
    private final LazyOptional<CustomEnergyStorage> energyOptional;
    private final LazyOptional<IItemHandler> inventoryOptional;
    
    public ItemCapabilityProvider(ItemStack stack, int energyCapacity, int energyTransferRate, int inventorySize, int stackSize) {
        this.stack = stack;
        this.energyStorage = new CustomEnergyStorage(energyCapacity, energyTransferRate, energyTransferRate) {
            @Override
            public int extractEnergy(int extract, boolean simulate) {
                int amount = super.extractEnergy(extract, simulate);
                if (!simulate) {
                    stack.getOrCreateTag().putInt("energy", this.energy);
                }
                return amount;
            }

            @Override
            public int receiveEnergy(int receive, boolean simulate) {
                int amount = super.receiveEnergy(receive, simulate);
                if (!simulate) {
                    stack.getOrCreateTag().putInt("energy", this.energy);
                }
                return amount;
            }
        };
        this.inventoryHandler = new ItemInventoryHandler(inventorySize, stackSize) {
            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, stack);
                saveToNBT();
            }
            
            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                ItemStack result = super.insertItem(slot, stack, simulate);
                if (!simulate) {
                    saveToNBT();
                }
                return result;
            }
            
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack result = super.extractItem(slot, amount, simulate);
                if (!simulate && !result.isEmpty()) {
                    saveToNBT();
                }
                return result;
            }
        };
        this.energyOptional = LazyOptional.of(() -> energyStorage);
        this.inventoryOptional = LazyOptional.of(() -> inventoryHandler);
        
        // Load existing data from NBT
        loadFromNBT();
    }
    
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryOptional.cast();
        }
        return LazyOptional.empty();
    }
    
    private void loadFromNBT() {
        CompoundTag tag = stack.getOrCreateTag();
        
        // Load energy data - use the simple "energy" key for compatibility
        if (tag.contains("energy")) {
            energyStorage.setEnergy(tag.getInt("energy"));
        }
        
        // Load inventory data
        if (tag.contains("Inventory")) {
            inventoryHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
    }
    
    public void saveToNBT() {
        CompoundTag tag = stack.getOrCreateTag();
        
        // Save energy data - use the simple "energy" key for compatibility
        tag.putInt("energy", energyStorage.getEnergyStored());
        
        // Save inventory data
        CompoundTag inventoryNBT = inventoryHandler.serializeNBT();
        tag.put("Inventory", inventoryNBT);
        
        // Force the stack to be marked as changed to ensure NBT persistence
        stack.setTag(tag);
    }
    
    public CustomEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public ItemInventoryHandler getInventoryHandler() {
        return inventoryHandler;
    }
    
    /**
     * Force save all data to NBT - useful for ensuring data persistence
     */
    public void forceSave() {
        saveToNBT();
    }
}