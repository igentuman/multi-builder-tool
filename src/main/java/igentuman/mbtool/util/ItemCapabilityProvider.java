package igentuman.mbtool.util;

import igentuman.mbtool.registry.MbtoolDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ItemCapabilityProvider {
    
    // Cache for energy storage instances per item stack
    private static final Map<ItemStack, CustomEnergyStorage> energyStorageCache = new ConcurrentHashMap<>();
    // Cache for item handler instances per item stack
    private static final Map<ItemStack, ItemInventoryHandler> itemHandlerCache = new ConcurrentHashMap<>();
    
    /**
     * Gets or creates an energy storage capability for the given item stack
     */
    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        return energyStorageCache.computeIfAbsent(stack, s -> createEnergyStorage(s));
    }
    
    /**
     * Gets or creates an item handler capability for the given item stack
     */
    public static IItemHandler getItemHandler(ItemStack stack) {
        return itemHandlerCache.computeIfAbsent(stack, s -> createItemHandler(s));
    }
    
    /**
     * Creates a new energy storage instance for the given item stack
     */
    private static CustomEnergyStorage createEnergyStorage(ItemStack stack) {
        // Default values - these should be configurable or based on item properties
        int energyCapacity = 100000;
        int energyTransferRate = 1000;
        
        CustomEnergyStorage energyStorage = new CustomEnergyStorage(energyCapacity, energyTransferRate, energyTransferRate) {
            @Override
            public int extractEnergy(int extract, boolean simulate) {
                int amount = super.extractEnergy(extract, simulate);
                if (!simulate) {
                    stack.set(MbtoolDataComponents.ENERGY.get(), this.energy);
                }
                return amount;
            }

            @Override
            public int receiveEnergy(int receive, boolean simulate) {
                int amount = super.receiveEnergy(receive, simulate);
                if (!simulate) {
                    stack.set(MbtoolDataComponents.ENERGY.get(), this.energy);
                }
                return amount;
            }
        };
        
        // Load existing energy data from DataComponent
        Integer energy = stack.get(MbtoolDataComponents.ENERGY.get());
        if (energy != null) {
            energyStorage.setEnergy(energy);
        }
        
        return energyStorage;
    }
    
    /**
     * Creates a new item handler instance for the given item stack
     */
    private static ItemInventoryHandler createItemHandler(ItemStack stack) {
        // Default values - these should be configurable or based on item properties
        int inventorySize = 27;
        int stackSize = 64;
        
        ItemInventoryHandler inventoryHandler = new ItemInventoryHandler(inventorySize, stackSize) {
            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, stack);
                saveInventoryToNBT();
            }
            
            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                ItemStack result = super.insertItem(slot, stack, simulate);
                if (!simulate) {
                    saveInventoryToNBT();
                }
                return result;
            }
            
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack result = super.extractItem(slot, amount, simulate);
                if (!simulate && !result.isEmpty()) {
                    saveInventoryToNBT();
                }
                return result;
            }
            
            private void saveInventoryToNBT() {
                // Save inventory data to CustomData (still using NBT for complex data)
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.put("Inventory", this.serializeNBT());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        };
        
        // Load existing inventory data from CustomData
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("Inventory")) {
            inventoryHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        
        return inventoryHandler;
    }
    
    /**
     * Clears cached capabilities for the given item stack
     * Should be called when the item stack is no longer valid
     */
    public static void clearCache(ItemStack stack) {
        energyStorageCache.remove(stack);
        itemHandlerCache.remove(stack);
    }
    
    /**
     * Force save all data for the given item stack
     */
    public static void forceSave(ItemStack stack) {
        CustomEnergyStorage energyStorage = energyStorageCache.get(stack);
        if (energyStorage != null) {
            stack.set(MbtoolDataComponents.ENERGY.get(), energyStorage.getEnergyStored());
        }
        
        ItemInventoryHandler itemHandler = itemHandlerCache.get(stack);
        if (itemHandler != null) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.put("Inventory", itemHandler.serializeNBT());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}