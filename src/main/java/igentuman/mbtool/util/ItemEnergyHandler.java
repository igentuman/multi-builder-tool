package igentuman.mbtool.util;

import igentuman.mbtool.registry.MbtoolDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Utility class for handling energy in item stacks
 * This replaces the old capability provider pattern with direct capability queries
 */
public class ItemEnergyHandler {

    private final int storage;
    private final int output;
    private final int input;
    public ItemStack stack;

    public ItemEnergyHandler(ItemStack stack, int storage, int output, int input) {
        this.stack = stack;
        this.storage = storage;
        this.output = output;
        this.input = input;
    }

    public int sendRate() {
        return output;
    }

    public int chargeRate() {
        return input;
    }

    public int capacity() {
        return storage;
    }

    public int getEnergyStored() {
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null) {
            return energyStorage.getEnergyStored();
        }
        return 0;
    }

    /**
     * Gets the energy storage capability for this item stack
     */
    public IEnergyStorage getEnergyStorage() {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    /**
     * Inner class that implements the actual energy storage behavior
     */
    public static class ItemEnergy extends CustomEnergyStorage {
        private ItemStack stack;
        
        public ItemEnergy(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
            this.stack = stack;
            Integer energyValue = stack.get(MbtoolDataComponents.ENERGY.get());
            energy = energyValue != null ? energyValue : 0;
        }

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
    }
}