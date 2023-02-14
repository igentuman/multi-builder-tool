package igentuman.mbtool.common.item;

import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public class IC2ElectricManager implements IElectricItemManager
{

    @Override
    public double charge(ItemStack itemStack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate)
    {
        if (itemStack.getItem() instanceof ItemMultiBuilder)
        {
            ItemMultiBuilder item = (ItemMultiBuilder) itemStack.getItem();
            if (amount > ((ItemMultiBuilder) item).getMaxCharge(itemStack))
            {
                amount = ((ItemMultiBuilder) item).getMaxCharge(itemStack);
            }
            float energy = (float) amount * 0.25f;
            float rejectedElectricity = Math.max(item.getElectricityStored(itemStack) + energy - item.getMaxElectricityStored(itemStack), 0);
            float energyToReceive = energy - rejectedElectricity;
            if (!ignoreTransferLimit && energyToReceive > item.getMaxTransfer(itemStack))
            {
                energyToReceive = (float) item.getMaxTransfer(itemStack);
            }

            if (!simulate)
            {
                item.setElectricity(itemStack, item.getElectricityStored(itemStack) + energyToReceive);
            }

            return energyToReceive / 0.25f;
        }
        return 0D;
    }

    @Override
    public double discharge(ItemStack itemStack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate)
    {
        if (itemStack.getItem() instanceof ItemMultiBuilder)
        {
            ItemMultiBuilder item = (ItemMultiBuilder) itemStack.getItem();
            float energy = (float) amount / 4f;
            float energyToTransfer = Math.min(item.getElectricityStored(itemStack), energy);
            if (!ignoreTransferLimit)
            {
                energyToTransfer = (float) Math.min(energyToTransfer, item.getMaxTransfer(itemStack));
            }

            if (!simulate)
            {
                item.setElectricity(itemStack, item.getElectricityStored(itemStack) - energyToTransfer);
            }

            return energyToTransfer * 4f;
        }
        return 0D;
    }

    @Override
    public double getCharge(ItemStack itemStack)
    {
        if (itemStack.getItem() instanceof ItemMultiBuilder)
        {
            ItemMultiBuilder item = (ItemMultiBuilder) itemStack.getItem();
            return item.getElectricityStored(itemStack) * 4f;
        }
        return 0D;
    }

    @Override
    public boolean canUse(ItemStack itemStack, double amount)
    {
        if (itemStack.getItem() instanceof IElectricItem)
        {
            return this.getCharge(itemStack) >= amount;
        }
        return false;
    }

    @Override
    public boolean use(ItemStack itemStack, double amount, EntityLivingBase entity)
    {
        if (itemStack.getItem() instanceof IElectricItem)
        {
            return this.discharge(itemStack, amount, 1, true, false, false) >= amount - 1;
        }
        return false;
    }

    @Override
    public void chargeFromArmor(ItemStack itemStack, EntityLivingBase entity)
    {
    }

    @Override
    public String getToolTip(ItemStack itemStack)
    {
        return null;
    }

    @Override
    public double getMaxCharge(ItemStack stack)
    {
        if (stack.getItem() instanceof IElectricItem)
        {
            return ((IElectricItem) stack.getItem()).getMaxCharge(stack);
        }
        return 1;
    }

    @Override
    public int getTier(ItemStack stack)
    {
        if (stack.getItem() instanceof IElectricItem)
        {
            return ((IElectricItem) stack.getItem()).getTier(stack);
        }

        return 1;
    }
}
