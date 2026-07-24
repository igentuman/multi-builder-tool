package igentuman.mbtool.util;

import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.energy.EnergyStorage;

public class CustomEnergyStorage extends EnergyStorage {

    private ItemStack stack;
    public boolean wasUpdated = true;
    private int receivedEnergy = 0;
    private boolean limit = false;
    private long outputAmperage;
    private long outputVoltage;
    private long inputAmperage;
    private long inputVoltage;

    public static final long[] V = new long[] { 8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152, 8388608,
            33554432, 134217728, 536870912, 2147483648L };
    public CustomEnergyStorage(ItemStack stack, int capacity, int maxTransfer, int maxExtract) {
        this(capacity, maxTransfer, maxExtract);
        this.stack = stack;
    }

    public CustomEnergyStorage(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, 0);
    }

    public CustomEnergyStorage(int capacity, int maxTransfer, int maxExtract, long outputAmerage, long outputVoltage, long inputAmerage, long inputVoltage) {
        super(capacity, maxTransfer, maxExtract);
        this.outputAmperage = outputAmerage;
        this.outputVoltage = V[(int) outputVoltage];;
        this.inputAmperage = inputAmerage*2;
        this.inputVoltage = V[(int) inputVoltage];
    }

    public CustomEnergyStorage(int capacity, int maxTransfer, int maxExtract) {
        this(capacity, maxTransfer, maxExtract, 0, 0, 2, 2);
    }

    public CustomEnergyStorage(int capacity, int maxTransfer, int maxExtract, boolean limit) {
        this(capacity, maxTransfer, maxExtract);
        this.limit = limit;
    }

    public CustomEnergyStorage setInputAmperage(long value) {
        this.inputAmperage = value;
        return this;
    }

    public CustomEnergyStorage setInputEnergyTier(long value) {
        this.inputAmperage = value*2;
        this.inputVoltage = V[(int) value];
        return this;
    }

    public CustomEnergyStorage setOutputEnergyTier(long value) {
        this.outputAmperage = value;
        this.outputVoltage = V[(int) value];
        return this;
    }

    protected void onEnergyChanged() {
        wasUpdated = true;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (limit && receivedEnergy >= maxReceive) {
            return 0;
        }
        int rc;
        if (stack != null) {
            int stored = getEnergyStored();
            rc = Math.min(getMaxEnergyStored() - stored, Math.min(maxReceive, maxReceive)); // simplifying for now
            // actually EnergyStorage uses maxReceive and capacity
            rc = Math.min(getMaxEnergyStored() - stored, maxReceive);
            if (!simulate && rc > 0) {
                setEnergy(stored + rc);
            }
        } else {
            rc = super.receiveEnergy(maxReceive, simulate);
        }
        
        if (rc > 0 && !simulate) {
            receivedEnergy += rc;
            onEnergyChanged();
        }
        return rc;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int rc;
        if (stack != null) {
            int stored = getEnergyStored();
            rc = Math.min(stored, maxExtract);
            if (!simulate && rc > 0) {
                setEnergy(stored - rc);
            }
        } else {
            rc = super.extractEnergy(maxExtract, simulate);
        }
        
        if (rc > 0 && !simulate) {
            onEnergyChanged();
        }
        return rc;
    }

    @Override
    public int getEnergyStored() {
        if (stack != null) {
            return stack.getOrDefault(MbtoolDataComponents.ENERGY.get(), 0);
        }
        return super.getEnergyStored();
    }

    public void setEnergy(int energy) {
        int wasEnergy = getEnergyStored();
        int newEnergy = Math.max(0, Math.min(energy, getMaxEnergyStored()));
        if (stack != null) {
            stack.set(MbtoolDataComponents.ENERGY.get(), newEnergy);
        } else {
            this.energy = newEnergy;
        }
        
        if(newEnergy != wasEnergy) {
            onEnergyChanged();
        }
    }

    public void addEnergy(int energy) {
        this.energy += energy;
        if (this.energy > getMaxEnergyStored()) {
            this.energy = getEnergyStored();
        }
        this.energy = Math.max(this.energy, 0);
        this.energy = Math.min(this.energy, getMaxEnergyStored());
        if(energy != 0) {
            onEnergyChanged();
        }
    }

    public void consumeEnergy(int energy) {
        this.energy -= energy;
        if (this.energy < 0) {
            this.energy = 0;
        }
        if(energy != 0) {
            onEnergyChanged();
        }
    }

    public void setMaxCapacity(int cap) {
        if(cap != capacity) {
            onEnergyChanged();
        }
        capacity = cap;
    }

    public void setMaxExtract(int i) {
        if(i != maxExtract) {
            onEnergyChanged();
        }
        maxExtract = i;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("energy", this.getEnergyStored());
        output.putInt("capacity", capacity);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.energy = input.getIntOr("energy", 0);
        capacity = input.getIntOr("capacity", capacity);
    }

    public void tick() {
        receivedEnergy = 0;
    }

    public long getGTOutputAmperage() {
        return outputAmperage;
    }

    public long getGTOuputVoltage() {
        return outputVoltage;
    }

    public long getGTInputAmperage() {
        return inputAmperage;
    }

    public long getGTInputVoltage() {
        return inputVoltage;
    }

    public int getMaxExtract() {
        return maxExtract;
    }
}