package igentuman.mbtool.item;

import igentuman.mbtool.common.MultiblocksProvider;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import igentuman.mbtool.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.List;

import static igentuman.mbtool.Mbtool.MBTOOL;

public class MultibuilderItem extends Item {
    
    // Energy configuration - you can adjust these values as needed
    private static final int ENERGY_CAPACITY = 100000; // 100k FE
    private static final int ENERGY_TRANSFER_RATE = 1000; // 1k FE/t
    
    // Inventory configuration
    private static final int INVENTORY_SIZE = 24; // 24 slots
    private static final int STACK_SIZE = 64; // Standard stack size

    public MultibuilderItem(Properties pProperties) {
        super(pProperties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));
        ItemStack itemStack = player.getItemInHand(hand);

        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40; // 40 is offhand slot
        if(player.isSteppingCarefully()) {
            // Open GUI when sneaking
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
        } else {
            // Build multiblock when not sneaking
            if(hasRecipe(itemStack)) {
                MultiblockStructure structure = getSelectedStructure(itemStack);
                if (structure != null) {
                    BlockPos buildPos = getBuildPosition(player);
                    if (buildPos != null) {
                        int rotation = getRotation(itemStack);
                        MultiblockBuilder.BuildResult result = MultiblockBuilder.buildMultiblock(
                            level, player, itemStack, structure, buildPos, rotation);
                        
                        // Send result message to player
                        player.sendSystemMessage(result.getMessage());
                        
                        if (result.isSuccess()) {
                            return InteractionResultHolder.success(itemStack);
                        } else {
                            return InteractionResultHolder.fail(itemStack);
                        }
                    } else {
                        player.sendSystemMessage(Component.translatable("message.mbtool.no_build_position"));
                        return InteractionResultHolder.fail(itemStack);
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("message.mbtool.invalid_recipe"));
                    return InteractionResultHolder.fail(itemStack);
                }
            } else {
                player.sendSystemMessage(Component.translatable("message.mbtool.no_recipe"));
                return InteractionResultHolder.fail(itemStack);
            }
        }
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
    
    public static int getInventorySize() {
        return INVENTORY_SIZE;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new ItemCapabilityProvider(stack, getEnergyMaxStorage(), getEnergyTransferRate(), INVENTORY_SIZE, STACK_SIZE);
    }

    public CustomEnergyStorage getEnergy(ItemStack stack) {
        return (CustomEnergyStorage) CapabilityUtils.getPresentCapability(stack, ForgeCapabilities.ENERGY);
    }
    
    public IItemHandler getInventory(ItemStack stack) {
        return (IItemHandler) CapabilityUtils.getPresentCapability(stack, ForgeCapabilities.ITEM_HANDLER);
    }
    
    public ItemInventoryHandler getInventoryHandler(ItemStack stack) {
        IItemHandler handler = getInventory(stack);
        if (handler instanceof ItemInventoryHandler) {
            return (ItemInventoryHandler) handler;
        }
        return null;
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
        
        // Show inventory information
        IItemHandler inventory = getInventory(stack);
        if (inventory != null) {
            int usedSlots = 0;
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    usedSlots++;
                }
            }
            list.add(Component.translatable("tooltip.mbtool.inventory_slots", usedSlots, INVENTORY_SIZE)
                .withStyle(ChatFormatting.GRAY));
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
    
    /**
     * Get an item from the inventory
     */
    public ItemStack getInventoryItem(ItemStack multibuilderStack, int slot) {
        IItemHandler inventory = getInventory(multibuilderStack);
        if (inventory != null && slot >= 0 && slot < inventory.getSlots()) {
            return inventory.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Insert an item into the inventory
     */
    public ItemStack insertInventoryItem(ItemStack multibuilderStack, ItemStack itemToInsert, boolean simulate) {
        IItemHandler inventory = getInventory(multibuilderStack);
        if (inventory != null) {
            // Try to insert into any available slot
            for (int i = 0; i < inventory.getSlots(); i++) {
                itemToInsert = inventory.insertItem(i, itemToInsert, simulate);
                if (itemToInsert.isEmpty()) {
                    break;
                }
            }
        }
        return itemToInsert;
    }
    
    /**
     * Extract an item from the inventory
     */
    public ItemStack extractInventoryItem(ItemStack multibuilderStack, int slot, int amount, boolean simulate) {
        IItemHandler inventory = getInventory(multibuilderStack);
        if (inventory != null && slot >= 0 && slot < inventory.getSlots()) {
            return inventory.extractItem(slot, amount, simulate);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 0 = no rotation, 1 = 90 degrees, 2 = 180 degrees, 3 = 270 degrees
     * @param stack
     * @return
     */
    public int getRotation(ItemStack stack) {
        try {
            if (!stack.getOrCreateTag().contains("rotation")) {
                return 0;
            }

            return stack.getOrCreateTag().getInt("rotation");
        } catch (Exception ignored) {
            return 0;
        }
    }

    public void rotate(ItemStack stack, int dir) {
        try {
            CompoundTag tag = stack.getOrCreateTag();
            int rotation = getRotation(stack);
            if(rotation + dir < 0) {
                rotation = 3;
            } else {
                rotation = (rotation + dir) % 4;
            }
            tag.putInt("rotation", rotation);
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Check if the item has a recipe stored
     */
    public boolean hasRecipe(ItemStack stack) {
        try {
            if (!stack.getOrCreateTag().contains("recipe")) {
                return false;
            }
            
            // Ensure structures are loaded
            if (MultiblocksProvider.structures.isEmpty()) {
                MultiblocksProvider.loadMultiblockStructures();
            }
            
            int recipeIndex = stack.getOrCreateTag().getInt("recipe");
            return recipeIndex >= 0 && recipeIndex < MultiblocksProvider.structures.size();
        } catch (Exception ignored) {
            return false;
        }
    }
    
    /**
     * Get the selected multiblock structure from the item
     */
    public MultiblockStructure getSelectedStructure(ItemStack stack) {
        try {
            if (!hasRecipe(stack)) {
                return null;
            }
            
            int recipeIndex = stack.getOrCreateTag().getInt("recipe");
            if (recipeIndex >= 0 && recipeIndex < MultiblocksProvider.structures.size()) {
                return MultiblocksProvider.structures.get(recipeIndex);
            }
        } catch (Exception ignored) {
            // Fall through to return null
        }
        return null;
    }
    
    /**
     * Get the build position based on player's look direction
     * Uses the same logic as PreviewRenderer#getRayTraceHit
     */
    private BlockPos getBuildPosition(Player player) {
        Level world = player.level();
        
        if (player == null || world == null) return null;
        
        // Perform raycast for 20 blocks
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(20.0));
        
        BlockHitResult rayTrace = world.clip(new net.minecraft.world.level.ClipContext(
            eyePos, endPos, 
            net.minecraft.world.level.ClipContext.Block.OUTLINE, 
            net.minecraft.world.level.ClipContext.Fluid.NONE, 
            player
        ));

        if (rayTrace.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offItem = player.getItemInHand(InteractionHand.OFF_HAND);

        boolean main = !mainItem.isEmpty() && mainItem.is(MBTOOL.get()) && hasRecipe(mainItem);
        boolean off = !offItem.isEmpty() && offItem.is(MBTOOL.get()) && hasRecipe(offItem);

        if (!main && !off) return null;

        BlockPos hit = rayTrace.getBlockPos();
        BlockState state = world.getBlockState(hit);

        // Get the selected structure
        ItemStack multibuilderStack = main ? mainItem : offItem;
        int recipeIndex = multibuilderStack.getOrCreateTag().getInt("recipe");
        
        // Ensure structures are loaded
        if (MultiblocksProvider.structures.isEmpty()) {
            MultiblocksProvider.loadMultiblockStructures();
        }
        
        if (recipeIndex < 0 || recipeIndex >= MultiblocksProvider.structures.size()) {
            return null;
        }
        
        MultiblockStructure structure = MultiblocksProvider.structures.get(recipeIndex);
        if (structure == null) return null;
        
        // Get rotation from item (if supported in the future)
        int rotation = multibuilderStack.getOrCreateTag().getInt("rotation");

        // Calculate placement position based on hit side
        Direction hitSide = rayTrace.getDirection();
        int maxSize = Math.max(structure.getWidth(), structure.getDepth()) - 1;
        
        switch (hitSide) {
            case DOWN:
                hit = hit.offset(0, -structure.getHeight(), 0);
                break;
            case UP:
                if (!state.canBeReplaced()) {
                    hit = hit.offset(0, 1, 0);
                }
                break;
            case EAST:
                hit = hit.offset(maxSize, 0, 0);
                break;
            case WEST:
                hit = hit.offset(-maxSize, 0, 0);
                break;
            case NORTH:
                hit = hit.offset(0, 0, -maxSize);
                break;
            case SOUTH:
                hit = hit.offset(0, 0, maxSize);
                break;
        }
        
        // Center the structure
        if (rotation == 0 || rotation == 2) {
            hit = hit.offset(-structure.getWidth() / 2, 0, -structure.getDepth() / 2);
        } else {
            hit = hit.offset(-structure.getDepth() / 2, 0, -structure.getWidth() / 2);
        }

        return hit;
    }
    
}
