package igentuman.mbtool.util;

import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureDismantler {
    
    /**
     * Dismantles a placed structure and adds blocks to the multibuilder inventory
     */
    public static DismantleResult dismantleStructure(Level level, Player player, ItemStack multibuilderStack, PlacedStructure structure) {
        if (level.isClientSide) {
            return new DismantleResult(false, Component.literal("Cannot dismantle on client side"));
        }
        
        boolean isCreative = player.isCreative();
        
        // Get the original structure definition
        MultiblockStructure originalStructure = getOriginalStructure(structure.getStructureId());
        if (originalStructure == null) {
            return new DismantleResult(false, Component.translatable("message.mbtool.structure_not_found"));
        }
        
        // Collect only blocks that are part of the original structure
        List<BlockPos> blocksToRemove = new ArrayList<>();
        List<ItemStack> collectedItems = new ArrayList<>();
        
        // Calculate the offset from the original structure to the placed structure
        AABB boundingBox = structure.getBoundingBox();
        BlockPos structureOrigin = new BlockPos((int) boundingBox.minX, (int) boundingBox.minY, (int) boundingBox.minZ);
        BlockPos originalOrigin = new BlockPos(originalStructure.getMinX(), originalStructure.getMinY(), originalStructure.getMinZ());
        BlockPos offset = structureOrigin.subtract(originalOrigin);
        
        // Apply rotation if needed
        Map<BlockPos, BlockState> rotatedBlocks = applyRotation(originalStructure.getBlocks(), structure.getRotation());
        
        // Check each block position from the original structure
        for (Map.Entry<BlockPos, BlockState> entry : rotatedBlocks.entrySet()) {
            BlockPos originalPos = entry.getKey();
            BlockState originalBlockState = entry.getValue();
            
            // Skip air blocks from the original structure
            if (originalBlockState.isAir()) {
                continue;
            }
            
            // Calculate the world position of this block
            BlockPos worldPos = originalPos.offset(offset);
            BlockState currentState = level.getBlockState(worldPos);
            
            // Skip if the current block is air (already removed or never placed)
            if (currentState.isAir()) {
                continue;
            }
            
            // Check if the player can break this block (respects claims)
            if (!canPlayerBreakBlockAt(level, player, worldPos)) {
                return new DismantleResult(false, Component.translatable("message.mbtool.cannot_break_protected", 
                    worldPos.getX(), worldPos.getY(), worldPos.getZ()));
            }
            
            // Only add blocks that match the original structure or are reasonable substitutes
            if (shouldDismantleBlock(originalBlockState, currentState)) {
                blocksToRemove.add(worldPos);
                
                // Use getDrops to handle blocks with NBT data correctly
                List<ItemStack> drops = Block.getDrops(currentState, (ServerLevel) level, worldPos, level.getBlockEntity(worldPos));
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        collectedItems.add(drop.copy());
                    }
                }
            }
        }
        
        if (blocksToRemove.isEmpty()) {
            return new DismantleResult(false, Component.translatable("message.mbtool.no_blocks_to_dismantle"));
        }
        
        // Calculate energy cost for dismantling
        int totalEnergyCost = blocksToRemove.size() * MbtoolConfig.getEnergyPerBlock() / 2; // Half cost for dismantling
        
        // Check if we have enough energy (skip for creative mode)
        if (!isCreative) {
            MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
            if (!multibuilderItem.hasEnergy(multibuilderStack, totalEnergyCost)) {
                return new DismantleResult(false, Component.translatable("message.mbtool.insufficient_energy", 
                    TextUtils.formatEnergy(totalEnergyCost)));
            }
        }
        
        // Check if we have enough inventory space (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack);
            if (inventory == null) {
                return new DismantleResult(false, Component.translatable("message.mbtool.no_inventory"));
            }
            
            // Simulate adding all items to check if we have space
            for (ItemStack itemStack : collectedItems) {
                // Try to insert (simulate)
                ItemStack remaining = insertItemIntoInventory(inventory, itemStack, true);
                if (!remaining.isEmpty()) {
                    return new DismantleResult(false, Component.translatable("message.mbtool.insufficient_inventory_space"));
                }
            }
        }
        
        // All checks passed, start dismantling
        int blocksRemoved = 0;
        
        // Remove blocks from world
        for (BlockPos pos : blocksToRemove) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                // Remove the block
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                blocksRemoved++;
            }
        }
        
        // Add items to inventory (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack);
            for (ItemStack itemStack : collectedItems) {
                // Insert into inventory
                insertItemIntoInventory(inventory, itemStack, false);
            }
        }
        
        // Consume energy (skip for creative mode)
        if (!isCreative) {
            MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
            multibuilderItem.consumeEnergy(multibuilderStack, totalEnergyCost);
        }
        
        // Play sound effect
        if (level instanceof ServerLevel) {
            level.playSound(null, new BlockPos((int) boundingBox.getCenter().x, (int) boundingBox.getCenter().y, (int) boundingBox.getCenter().z), 
                SoundEvents.NETHERITE_BLOCK_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        
        return new DismantleResult(true, Component.translatable("message.mbtool.structure_dismantled", blocksRemoved));
    }
    
    /**
     * Checks if a player can break a block at the given position (respects claim systems)
     */
    private static boolean canPlayerBreakBlockAt(Level level, Player player, BlockPos pos) {
        // Check if the player can interact with this position
        // This method respects claim systems like FTB Chunks, WorldGuard, etc.
        return level.mayInteract(player, pos);
    }
    
    /**
     * Get inventory from multibuilder item
     */
    private static IItemHandler getInventory(ItemStack stack) {
        return (IItemHandler) CapabilityUtils.getPresentCapability(stack, 
            net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
    }
    
    /**
     * Insert an item into the inventory
     */
    private static ItemStack insertItemIntoInventory(IItemHandler inventory, ItemStack itemToInsert, boolean simulate) {
        ItemStack remaining = itemToInsert.copy();
        
        // Try to insert into any available slot
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, simulate);
        }
        
        return remaining;
    }
    
    /**
     * Get the original structure definition by ID
     */
    private static MultiblockStructure getOriginalStructure(String structureId) {
        for (MultiblockStructure structure : MultiblocksProvider.getStructures()) {
            if (structure.getName().equals(structureId)) {
                return structure;
            }
        }
        return null;
    }
    
    /**
     * Apply rotation to the structure blocks
     */
    private static Map<BlockPos, BlockState> applyRotation(Map<BlockPos, BlockState> originalBlocks, int rotation) {
        if (rotation == 0) {
            return originalBlocks;
        }
        
        Map<BlockPos, BlockState> rotatedBlocks = new HashMap<>();
        
        // Find the center of the structure for rotation
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (BlockPos pos : originalBlocks.keySet()) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        
        double centerX = (minX + maxX) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;
        
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            BlockPos originalPos = entry.getKey();
            BlockState originalState = entry.getValue();
            
            // Apply rotation around the center
            BlockPos rotatedPos = rotateBlockPos(originalPos, centerX, centerZ, rotation);
            
            // Apply rotation to the block state if needed (for directional blocks)
            BlockState rotatedState = rotateBlockState(originalState, rotation);
            
            rotatedBlocks.put(rotatedPos, rotatedState);
        }
        
        return rotatedBlocks;
    }
    
    /**
     * Rotate a block position around a center point
     */
    private static BlockPos rotateBlockPos(BlockPos pos, double centerX, double centerZ, int rotation) {
        double x = pos.getX() - centerX;
        double z = pos.getZ() - centerZ;
        
        double rotatedX, rotatedZ;
        
        switch (rotation % 4) {
            case 1: // 90 degrees
                rotatedX = -z;
                rotatedZ = x;
                break;
            case 2: // 180 degrees
                rotatedX = -x;
                rotatedZ = -z;
                break;
            case 3: // 270 degrees
                rotatedX = z;
                rotatedZ = -x;
                break;
            default: // 0 degrees
                rotatedX = x;
                rotatedZ = z;
                break;
        }
        
        return new BlockPos((int) Math.round(rotatedX + centerX), pos.getY(), (int) Math.round(rotatedZ + centerZ));
    }
    
    /**
     * Rotate a block state (for directional blocks)
     */
    private static BlockState rotateBlockState(BlockState state, int rotation) {
        // For now, return the original state
        // TODO: Implement proper block state rotation for directional blocks
        return state;
    }
    
    /**
     * Determine if a block should be dismantled based on the original and current block states
     */
    private static boolean shouldDismantleBlock(BlockState originalState, BlockState currentState) {
        // If the blocks are exactly the same, definitely dismantle
        if (originalState.equals(currentState)) {
            return true;
        }
        
        // If the block types are the same but properties differ, still dismantle
        // (e.g., same block but different facing direction)
        if (originalState.getBlock() == currentState.getBlock()) {
            return true;
        }
        
        // For now, only dismantle exact matches or same block types
        // This could be extended to handle block substitutions if needed
        return false;
    }
    
    /**
     * Result of a dismantle operation
     */
    public static class DismantleResult {
        private final boolean success;
        private final Component message;
        
        public DismantleResult(boolean success, Component message) {
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