package igentuman.mbtool.util;

import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;

public class MultiblockBuilder {
    
    // Energy cost per block placed
    private static final int ENERGY_PER_BLOCK = 100;
    
    /**
     * Attempts to build a multiblock structure
     * @param level The world level
     * @param player The player building the structure
     * @param multibuilderStack The multibuilder item stack
     * @param structure The structure to build
     * @param centerPos The center position where to build the structure
     * @return BuildResult containing success status and message
     */
    public static BuildResult buildMultiblock(Level level, Player player, ItemStack multibuilderStack, 
                                            MultiblockStructure structure, BlockPos centerPos) {
        
        if (level.isClientSide) {
            return new BuildResult(false, Component.literal("Cannot build on client side"));
        }
        
        boolean isCreative = player.isCreative();
        
        // Calculate required materials
        Map<Block, Integer> requiredBlocks = calculateRequiredBlocks(structure);
        
        // Calculate total energy cost
        int totalEnergyCost = requiredBlocks.values().stream().mapToInt(Integer::intValue).sum() * ENERGY_PER_BLOCK;
        
        // Check if we have enough energy (skip for creative mode)
        if (!isCreative) {
            CustomEnergyStorage energyStorage = getEnergyStorage(multibuilderStack);
            if (energyStorage == null || energyStorage.getEnergyStored() < totalEnergyCost) {
                return new BuildResult(false, Component.translatable("message.mbtool.insufficient_energy", 
                    TextUtils.formatEnergy(totalEnergyCost)));
            }
        }
        
        // Check if we have all required materials (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack);
            if (inventory == null) {
                return new BuildResult(false, Component.translatable("message.mbtool.no_inventory"));
            }
            
            Map<Block, Integer> availableBlocks = countAvailableBlocks(inventory);
            
            for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
                Block block = entry.getKey();
                int required = entry.getValue();
                int available = availableBlocks.getOrDefault(block, 0);
                
                if (available < required) {
                    return new BuildResult(false, Component.translatable("message.mbtool.insufficient_blocks", 
                        block.getName(), required - available));
                }
            }
        }
        
        for (Map.Entry<BlockPos, BlockState> entry : structure.getBlocks().entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockPos worldPos = centerPos.offset(relativePos);
            BlockState currentState = level.getBlockState(worldPos);
            
            // Skip air blocks in the structure
            if (entry.getValue().isAir()) {
                continue;
            }
            
            // Check if we can place the block here
            if (!currentState.canBeReplaced()) {
                return new BuildResult(false, Component.translatable("message.mbtool.cannot_place_at", 
                    worldPos.getX(), worldPos.getY(), worldPos.getZ()));
            }
        }
        
        // All checks passed, start building
        int blocksPlaced = 0;
        
        // Consume materials from inventory (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack);
            for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
                Block block = entry.getKey();
                int needed = entry.getValue();
                
                // Remove items from inventory
                for (int slot = 0; slot < inventory.getSlots() && needed > 0; slot++) {
                    ItemStack slotStack = inventory.getStackInSlot(slot);
                    if (!slotStack.isEmpty() && Block.byItem(slotStack.getItem()) == block) {
                        int toExtract = Math.min(needed, slotStack.getCount());
                        inventory.extractItem(slot, toExtract, false);
                        needed -= toExtract;
                    }
                }
            }
        }
        
        // Place blocks
        for (Map.Entry<BlockPos, BlockState> entry : structure.getBlocks().entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockState blockState = entry.getValue();
            BlockPos worldPos = centerPos.offset(relativePos);
            
            // Skip air blocks
            if (blockState.isAir()) {
                continue;
            }
            
            // Place the block
            if (level.setBlock(worldPos, blockState, 3)) {
                blocksPlaced++;
                
                // Update block entity if needed
                if (blockState.hasBlockEntity()) {
                    level.getBlockEntity(worldPos);
                }
            }
        }
        
        // Consume energy (skip for creative mode)
        if (!isCreative) {
            CustomEnergyStorage energyStorage = getEnergyStorage(multibuilderStack);
            if (energyStorage != null) {
                energyStorage.extractEnergy(totalEnergyCost, false);
            }
        }
        
        return new BuildResult(true, Component.translatable("message.mbtool.multiblock_built", 
            blocksPlaced, structure.getName()));
    }
    
    /**
     * Calculate required blocks for the structure
     */
    private static Map<Block, Integer> calculateRequiredBlocks(MultiblockStructure structure) {
        Map<Block, Integer> requiredBlocks = new HashMap<>();
        
        for (BlockState blockState : structure.getBlocks().values()) {
            if (!blockState.isAir()) {
                Block block = blockState.getBlock();
                requiredBlocks.put(block, requiredBlocks.getOrDefault(block, 0) + 1);
            }
        }
        
        return requiredBlocks;
    }
    
    /**
     * Count available blocks in the inventory
     */
    private static Map<Block, Integer> countAvailableBlocks(IItemHandler inventory) {
        Map<Block, Integer> availableBlocks = new HashMap<>();
        
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block block = Block.byItem(stack.getItem());
                if (block != Blocks.AIR) {
                    availableBlocks.put(block, availableBlocks.getOrDefault(block, 0) + stack.getCount());
                }
            }
        }
        
        return availableBlocks;
    }
    
    /**
     * Get energy storage from multibuilder item
     */
    private static CustomEnergyStorage getEnergyStorage(ItemStack stack) {
        return (CustomEnergyStorage) CapabilityUtils.getPresentCapability(stack, 
            net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY);
    }
    
    /**
     * Get inventory from multibuilder item
     */
    private static IItemHandler getInventory(ItemStack stack) {
        return (IItemHandler) CapabilityUtils.getPresentCapability(stack, 
            net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
    }
    
    /**
     * Result of a build operation
     */
    public static class BuildResult {
        private final boolean success;
        private final Component message;
        
        public BuildResult(boolean success, Component message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Component getMessage() {
            return message;
        }
    }
}