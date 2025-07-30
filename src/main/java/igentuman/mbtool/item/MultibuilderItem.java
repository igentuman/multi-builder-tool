package igentuman.mbtool.item;

import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.util.CapabilityUtils;
import igentuman.mbtool.util.CustomEnergyStorage;
import igentuman.mbtool.util.ItemEnergyHandler;
import igentuman.mbtool.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.List;

import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;

public class MultibuilderItem extends Item {
    
    // Energy configuration - you can adjust these values as needed
    private static final int ENERGY_CAPACITY = 100000; // 100k FE
    private static final int ENERGY_TRANSFER_RATE = 1000; // 1k FE/t
    
    public MultibuilderItem(Properties pProperties) {
        super(pProperties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));
        ItemStack itemStack = player.getItemInHand(hand);

        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40; // 40 is offhand slot
        
        NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.mbtool.multibuilder");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new MultibuilderContainer(containerId, player.blockPosition(), playerInventory, slot);
            }
        }, buf -> {
            buf.writeBlockPos(player.blockPosition());
            buf.writeInt(slot);
        });
        
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    
    @Override
    public boolean isRepairable(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        return false;
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return Mth.hsvToRgb(Math.max(0.0F, getBarWidth(pStack)/(float)MAX_BAR_WIDTH)/3.0F, 1.0F, 1.0F);
    }

    protected int getEnergyMaxStorage() {
        return ENERGY_CAPACITY;
    }
    
    protected int getEnergyTransferRate() {
        return ENERGY_TRANSFER_RATE;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new ItemEnergyHandler(stack, getEnergyMaxStorage(), getEnergyTransferRate(), getEnergyTransferRate());
    }

    public CustomEnergyStorage getEnergy(ItemStack stack) {
        return (CustomEnergyStorage) CapabilityUtils.getPresentCapability(stack, ForgeCapabilities.ENERGY);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CustomEnergyStorage energyStorage = getEnergy(stack);
        if (energyStorage == null) return 0;
        float chargeRatio = (float) energyStorage.getEnergyStored() / (float) getEnergyMaxStorage();
        return (int) Math.min(13, 13 * chargeRatio);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true; // Always show energy bar
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level world, List<Component> list, TooltipFlag flag) {
        CustomEnergyStorage energyStorage = getEnergy(stack);
        if (energyStorage != null) {
            list.add(TextUtils.__("tooltip.mbtool.energy_stored", 
                TextUtils.formatEnergy(energyStorage.getEnergyStored()), 
                TextUtils.formatEnergy(getEnergyMaxStorage())).withStyle(ChatFormatting.BLUE));
        }
    }
    
    /**
     * Check if the item has enough energy
     */
    public boolean hasEnergy(ItemStack stack, int amount) {
        CustomEnergyStorage energyStorage = getEnergy(stack);
        return energyStorage != null && energyStorage.getEnergyStored() >= amount;
    }
    
    /**
     * Consume energy from the item
     */
    public boolean consumeEnergy(ItemStack stack, int amount) {
        CustomEnergyStorage energyStorage = getEnergy(stack);
        if (energyStorage != null && energyStorage.getEnergyStored() >= amount) {
            energyStorage.extractEnergy(amount, false);
            return true;
        }
        return false;
    }
    
    /**
     * Add energy to the item
     */
    public int addEnergy(ItemStack stack, int amount) {
        CustomEnergyStorage energyStorage = getEnergy(stack);
        if (energyStorage != null) {
            return energyStorage.receiveEnergy(amount, false);
        }
        return 0;
    }
}
