package igentuman.mbtool.common.item;

import igentuman.mbtool.util.CapabilityUtils;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.util.CustomEnergyStorage;
import igentuman.mbtool.util.ItemEnergyHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.ModList;


import javax.annotation.Nonnull;
import java.util.List;

import static igentuman.mbtool.util.GTUtils.formatEUEnergy;
import static igentuman.mbtool.util.TextUtils.__;
import static igentuman.mbtool.util.TextUtils.formatEnergy;

public class ItemMultiBuilder extends Item {

    public ItemMultiBuilder(Properties props)
    {
        super(props);
    }

    @Override
    public boolean isRepairable(@Nonnull ItemStack stack)
    {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book)
    {
        return false;
    }

    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity)
    {
        return false;
    }

    @Override
    public int getBarColor(ItemStack pStack)
    {
        return Mth.hsvToRgb(Math.max(0.0F, getBarWidth(pStack)/(float)MAX_BAR_WIDTH)/3.0F, 1.0F, 1.0F);
    }

    protected int getEnergyMaxStorage() {
        return MbtoolConfig.getMaxEnergy();
    }

    // TODO: Reimplement capabilities for NeoForge 1.21
    /*
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new ItemEnergyHandler(stack, getEnergyMaxStorage(), getEnergyMaxStorage(), getEnergyMaxStorage());
    }
    */

    public CustomEnergyStorage getEnergy(ItemStack stack)
    {
        return (CustomEnergyStorage) CapabilityUtils.getPresentCapability(stack, Capabilities.EnergyStorage.ITEM);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CustomEnergyStorage energyStorage = getEnergy(stack);
        float chargeRatio = (float) energyStorage.getEnergyStored() / (float) getEnergyMaxStorage();
        return (int) Math.min(13, 13*chargeRatio);
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level world, List<Component> list, TooltipFlag flag)
    {
        if(ModList.get().isLoaded("gtceu")) {
            list.add(__("tooltip.nc.eu_energy_stored", formatEUEnergy(getEnergy(stack).getEnergyStored()), formatEUEnergy(getEnergyMaxStorage())).withStyle(ChatFormatting.GOLD));
        }
        list.add(__("tooltip.nc.energy_stored", formatEnergy(getEnergy(stack).getEnergyStored()), formatEnergy(getEnergyMaxStorage())).withStyle(ChatFormatting.BLUE));
    }
}
