package igentuman.mbtool.item;

import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.SyncMultibuilderParamsPacket;
import igentuman.mbtool.network.SyncRuntimeStructurePacket;
import igentuman.mbtool.registry.MbtoolDataComponents;
import igentuman.mbtool.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.util.ModUtil.isGtLoaded;

public class MultibuilderItem extends Item {
    
    // Inventory configuration
    public static final int INVENTORY_SIZE = 40; // 40 slots
    private static final int STACK_SIZE = 64; // Standard stack size
    private static final int MAX_BAR_WIDTH = 13; // Maximum width for energy bar
    public int delay = 0;

    public MultibuilderItem(Properties pProperties) {
        super(pProperties);
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40; // 40 is offhand slot
        
        if(player.isSteppingCarefully()) {
            // Open GUI when sneaking - only on server side
            if (!level.isClientSide) {
                player.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("gui.mbtool.multibuilder");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                        return new MultibuilderContainer(containerId, player.blockPosition(), playerInventory, slot);
                    }
                });
            }
            return InteractionResult.SUCCESS;
        } else {
            if(delay > 0) return InteractionResult.PASS;
            delay = 40;
            // Build multiblock when not sneaking
            if (level.isClientSide) {
                int rotation = getRotation(itemStack);

                MultiblockStructure runtimeStructure = getRuntimeStructure(itemStack);
                if(runtimeStructure != null) {
                    // Send runtimeStructure to server to build
                    NetworkHandler.sendToServer(
                            new SyncRuntimeStructurePacket(runtimeStructure.getStructureNbt(), rotation, hand));
                    Integer recipe = itemStack.get(MbtoolDataComponents.RECIPE.get());
                    if(recipe != null && recipe > 0) {
                        setRuntimeStructure(itemStack, null);
                    }
                    return InteractionResult.SUCCESS;
                }
                // Client side: validate and send packet to server
                if(hasRecipe(itemStack)) {
                    Integer recipeIndex = itemStack.get(MbtoolDataComponents.RECIPE.get());
                    if (recipeIndex == null) recipeIndex = 0;
                    // Ensure structures are loaded on client
                    if (MultiblocksProvider.structures.isEmpty()) {
                        MultiblocksProvider.getStructures();
                    }
                    
                    // Validate recipe index on client
                    if (recipeIndex >= 0 && recipeIndex < MultiblocksProvider.structures.size()) {
                        // Send packet to server with current parameters
                        setRuntimeStructure(itemStack, null);
                        NetworkHandler.sendToServer(
                            new SyncMultibuilderParamsPacket(recipeIndex, rotation, hand));
                        return InteractionResult.SUCCESS;
                    } else {
                        player.displayClientMessage(Component.translatable("message.mbtool.invalid_recipe"), true);
                        return InteractionResult.FAIL;
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.mbtool.no_recipe"), true);
                    return InteractionResult.FAIL;
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return Mth.hsvToRgb(Math.max(0.0F, getBarWidth(pStack)/(float)MAX_BAR_WIDTH)/3.0F, 1.0F, 1.0F);
    }

    protected int getEnergyMaxStorage() {
        return MbtoolConfig.getMaxEnergy();
    }
    
    protected int getEnergyTransferRate() {
        return MbtoolConfig.getEnergyTransferRate();
    }
    
    public static int getInventorySize() {
        return INVENTORY_SIZE;
    }

    // TODO: Reimplement capabilities for NeoForge 1.21
    /*
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new ItemCapabilityProvider(stack, getEnergyMaxStorage(), getEnergyTransferRate(), getInventorySize(), STACK_SIZE);
    }
    */

    public CustomEnergyStorage getEnergy(ItemStack stack) {
        // Create energy storage from NBT data stored in DataComponents
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CustomEnergyStorage energyStorage = new CustomEnergyStorage(getEnergyMaxStorage(), getEnergyTransferRate(), getEnergyTransferRate());
        
        if (tag.contains("energy")) {
            energyStorage.setEnergy(tag.getInt("energy").orElseGet(() -> 0));
        }
        
        // Override methods to save back to ItemStack
        return new CustomEnergyStorage(getEnergyMaxStorage(), getEnergyTransferRate(), getEnergyTransferRate()) {
            {
                if (tag.contains("energy")) {
                    setEnergy(tag.getInt("energy").orElseGet(() -> 0));
                }
            }
            
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = super.extractEnergy(maxExtract, simulate);
                if (!simulate && extracted > 0) {
                    saveEnergyToStack(stack, getEnergyStored());
                }
                return extracted;
            }
            
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (!simulate && received > 0) {
                    saveEnergyToStack(stack, getEnergyStored());
                }
                return received;
            }
        };
    }
    
    private void saveEnergyToStack(ItemStack stack, int energy) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("energy", energy);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    public IItemHandler getInventory(ItemStack stack) {
        // Create inventory handler from NBT data stored in DataComponents
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ItemInventoryHandler inventoryHandler = new ItemInventoryHandler(getInventorySize(), STACK_SIZE) {
            @Override
            public void setStackInSlot(int slot, net.minecraft.world.item.ItemStack itemStack) {
                super.setStackInSlot(slot, itemStack);
                saveInventoryToStack(stack, this);
            }
            
            @Override
            public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack itemStack, boolean simulate) {
                net.minecraft.world.item.ItemStack result = super.insertItem(slot, itemStack, simulate);
                if (!simulate) {
                    saveInventoryToStack(stack, this);
                }
                return result;
            }
            
            @Override
            public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
                net.minecraft.world.item.ItemStack result = super.extractItem(slot, amount, simulate);
                if (!simulate && !result.isEmpty()) {
                    saveInventoryToStack(stack, this);
                }
                return result;
            }
        };
        
        // Load existing inventory data
        if (tag.contains("Inventory")) {
            inventoryHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        
        return inventoryHandler;
    }
    
    private void saveInventoryToStack(ItemStack stack, ItemInventoryHandler inventoryHandler) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put("Inventory", inventoryHandler.serializeNBT());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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
            if(isGtLoaded()) {
                list.add(TextUtils.__("tooltip.mbtool.energy_stored",
                        GTUtils.formatEUEnergy(energyStorage.getEnergyStored()),
                        GTUtils.formatEUEnergy(getEnergyMaxStorage())).withStyle(ChatFormatting.BLUE));
            } else {
                list.add(TextUtils.__("tooltip.mbtool.energy_stored",
                        TextUtils.formatEnergy(energyStorage.getEnergyStored()),
                        TextUtils.formatEnergy(getEnergyMaxStorage())).withStyle(ChatFormatting.BLUE));
            }
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
            list.add(Component.translatable("tooltip.mbtool.inventory_slots", usedSlots, getInventorySize())
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
            Integer rotation = stack.get(MbtoolDataComponents.ROTATION.get());
            return rotation != null ? rotation : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    public void rotate(ItemStack stack, int dir) {
        try {
            int rotation = getRotation(stack);
            if(rotation + dir < 0) {
                rotation = 3;
            } else {
                rotation = (rotation + dir) % 4;
            }
            stack.set(MbtoolDataComponents.ROTATION.get(), rotation);
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Check if the item has a recipe stored
     */
    public boolean hasRecipe(ItemStack stack) {
        try {
            MultibuilderItem multibuilderItem = (MultibuilderItem)stack.getItem();
            if (multibuilderItem.getRuntimeStructure(stack) != null) {
                return true;
            }
            Integer recipeIndex = stack.get(MbtoolDataComponents.RECIPE.get());
            if (recipeIndex == null) {
                return false;
            }
            
            // Ensure structures are loaded
            if (MultiblocksProvider.structures.isEmpty()) {
                MultiblocksProvider.getStructures();
            }
            
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

            if(stack.getItem() instanceof  MultibuilderItem multibuilderItem) {
                Integer recipeIndex = stack.get(MbtoolDataComponents.RECIPE.get());
                if (recipeIndex == null) {
                    return null;
                }

                // Ensure structures are loaded
                if (MultiblocksProvider.structures.isEmpty()) {
                    MultiblocksProvider.getStructures();
                }

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
        CompoundTag multibuilderTag = multibuilderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int recipeIndex = multibuilderTag.getInt("recipe");
        
        // Ensure structures are loaded
        if (MultiblocksProvider.structures.isEmpty()) {
            MultiblocksProvider.getStructures();
        }
        
        if (recipeIndex < 0 || recipeIndex >= MultiblocksProvider.structures.size()) {
            return null;
        }
        
        MultiblockStructure structure = MultiblocksProvider.structures.get(recipeIndex);
        if (structure == null) return null;
        
        // Get rotation from item (if supported in the future)
        int rotation = multibuilderTag.getInt("rotation");

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

    /**
     * Set the runtime structure in the ItemStack's NBT
     */
    public void setRuntimeStructure(ItemStack stack, MultiblockStructure structure) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (structure != null) {
            tag.remove("recipe");
            tag.put("runtimeStructure", structure.getStructureNbt());
        } else {
            tag.remove("runtimeStructure");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if(delay > 0) {
            delay--;
        }
    }
    
    /**
     * Get the runtime structure from the ItemStack's NBT
     */
    public MultiblockStructure getRuntimeStructure(ItemStack stack) {
        try {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag != null && tag.contains("runtimeStructure")) {
                CompoundTag structureNbt = tag.getCompound("runtimeStructure").orElseGet(CompoundTag::new);
                return new MultiblockStructure(structureNbt);
            }
        } catch (Exception ignored) {
            // Fall through to return null
        }
        return null;
    }

    public MultiblockStructure getCurrentStructure(ItemStack stack) {
        MultiblockStructure structure = getRuntimeStructure(stack);
        if(structure == null) {
            structure = getSelectedStructure(stack);
        }
        return  structure;
    }

    public int getSelectedStructureId(ItemStack multibuilderStack) {
        CompoundTag tag = multibuilderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if(tag.contains("recipe")) {
            return tag.getInt("recipe").orElseGet(() -> -1);
        }
        return -1;
    }

    public UUID getUUID(ItemStack multibuilderStack) {
        try {
            CompoundTag tag = multibuilderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if(!tag.contains("uuid")) {
                tag.putUUID("uuid", UUID.randomUUID());
                multibuilderStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            return tag.getUUID("uuid");
        } catch(Exception e) {
            return null;
        }
    }
}
