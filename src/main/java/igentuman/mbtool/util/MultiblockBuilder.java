package igentuman.mbtool.util;

import igentuman.mbtool.config.MbtoolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;

public class MultiblockBuilder {
    
    /**
     * Attempts to build a multiblock structure
     * @param level The world level
     * @param player The player building the structure
     * @param multibuilderStack The multibuilder item stack
     * @param structure The structure to build
     * @param centerPos The center position where to build the structure
     * @param rotation The rotation (0-3, representing 90-degree increments)
     * @return BuildResult containing success status and message
     */
    public static BuildResult buildMultiblock(Level level, Player player, ItemStack multibuilderStack, 
                                            MultiblockStructure structure, BlockPos centerPos, int rotation) {
        
        if (level.isClientSide) {
            return new BuildResult(false, Component.literal("Cannot build on client side"));
        }
        
        boolean isCreative = player.isCreative();
        
        // Calculate required materials
        Map<Block, Integer> requiredBlocks = calculateRequiredBlocks(structure);
        
        // Calculate total energy cost
        int totalEnergyCost = requiredBlocks.values().stream().mapToInt(Integer::intValue).sum() * MbtoolConfig.getEnergyPerBlock();
        
        // Check if we have enough energy (skip for creative mode)
        if (!isCreative) {
            CustomEnergyStorage energyStorage = getEnergyStorage(multibuilderStack);
            if (energyStorage == null || energyStorage.getEnergyStored() < totalEnergyCost) {
                return new BuildResult(false, Component.translatable("message.mbtool.insufficient_energy", 
                    TextUtils.formatEnergy(totalEnergyCost)));
            }
        }
        
        // Check if we have all required materials (skip for creative mode)
        Map<Block, Block> blockReplacements = new HashMap<>(); // Maps required block to replacement block
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack);
            if (inventory == null) {
                return new BuildResult(false, Component.translatable("message.mbtool.no_inventory"));
            }
            
            Map<Block, Integer> availableBlocks = countAvailableBlocks(inventory);
            
            for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
                Block requiredBlock = entry.getKey();
                int required = entry.getValue();
                
                // Try to find a replacement block (including the original block)
                Block replacementBlock = BlockEquivalencyManager.findReplacement(requiredBlock, availableBlocks);
                
                if (replacementBlock == null) {
                    return new BuildResult(false, Component.translatable("message.mbtool.insufficient_blocks", 
                        requiredBlock.getName(), required));
                }
                
                int available = availableBlocks.getOrDefault(replacementBlock, 0);
                if (available < required) {
                    return new BuildResult(false, Component.translatable("message.mbtool.insufficient_blocks", 
                        requiredBlock.getName(), required - available));
                }
                
                // Store the replacement mapping
                blockReplacements.put(requiredBlock, replacementBlock);
            }
        }
        
        for (Map.Entry<BlockPos, BlockState> entry : structure.getBlocks().entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockState blockState = entry.getValue();
            
            // Skip air blocks in the structure
            if (blockState.isAir()) {
                continue;
            }
            
            // Apply rotation to position and block state
            BlockPos rotatedRelativePos = rotateBlockPos(relativePos, structure, rotation);
            BlockPos worldPos = centerPos.offset(rotatedRelativePos);
            BlockState currentState = level.getBlockState(worldPos);
            BlockState rotatedBlockState = rotateBlockState(blockState, rotation);
            
            // Check if we can place the block here
            if (!currentState.canBeReplaced() || !canPlayerPlaceBlockAt(level, player, worldPos, blockState)) {
                return new BuildResult(false, Component.translatable("message.mbtool.cannot_place_at", 
                    worldPos.getX(), worldPos.getY(), worldPos.getZ()));
            }
            
            // Check if the player can place blocks at this position (respects claims)
            if (!canPlayerPlaceBlockAt(level, player, worldPos, rotatedBlockState)) {
                return new BuildResult(false, Component.translatable("message.mbtool.cannot_place_protected", 
                    worldPos.getX(), worldPos.getY(), worldPos.getZ()));
            }
        }
        
        // All checks passed, start building
        int blocksPlaced = 0;
        
        // Consume materials from inventory (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack);
            for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
                Block requiredBlock = entry.getKey();
                Block replacementBlock = blockReplacements.get(requiredBlock);
                int needed = entry.getValue();
                
                // Remove replacement items from inventory
                for (int slot = 0; slot < inventory.getSlots() && needed > 0; slot++) {
                    ItemStack slotStack = inventory.getStackInSlot(slot);
                    if (!slotStack.isEmpty() && Block.byItem(slotStack.getItem()) == replacementBlock) {
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
            
            // Skip air blocks
            if (blockState.isAir()) {
                continue;
            }
            
            // Apply rotation to position and block state
            BlockPos rotatedRelativePos = rotateBlockPos(relativePos, structure, rotation);
            BlockPos worldPos = centerPos.offset(rotatedRelativePos);
            BlockState rotatedBlockState = rotateBlockState(blockState, rotation);
            
            // Apply block replacement if needed (skip for creative mode)
            BlockState finalBlockState = rotatedBlockState;
            if (!isCreative) {
                Block originalBlock = rotatedBlockState.getBlock();
                Block replacementBlock = blockReplacements.get(originalBlock);
                if (replacementBlock != null && replacementBlock != originalBlock) {
                    // Create a new BlockState with the replacement block but try to preserve properties
                    finalBlockState = createReplacementBlockState(rotatedBlockState, replacementBlock);
                }
            }
            
            // Place the block using player-respecting method
            if (placeBlockAsPlayer(level, player, worldPos, finalBlockState)) {
                blocksPlaced++;
                
                // Update block entity if needed
                if (finalBlockState.hasBlockEntity()) {
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
        
        // Store the placed structure for dismantling
        if (blocksPlaced > 0) {
            AABB structureBounds = calculateStructureBounds(structure, centerPos, rotation);
            PlacedStructuresManager.addPlacedStructure(multibuilderStack, structure.getName(), 
                structureBounds, player.getUUID(), rotation);
        }
        
        // Spawn smoke particles around the built structure
        if (level instanceof ServerLevel serverLevel) {
            spawnSmokeParticles(serverLevel, structure, centerPos, rotation);
            sendPlacementSoundEvent(level, centerPos);
        }
        
        return new BuildResult(true, Component.translatable("message.mbtool.multiblock_built", 
            blocksPlaced, Component.translatable(structure.getName())));
    }

    private static void sendPlacementSoundEvent(Level level, BlockPos centerPos) {
        // Play a block placement sound at the center position
        level.playSound(null, centerPos, 
            SoundEvents.NETHERITE_BLOCK_PLACE,
            net.minecraft.sounds.SoundSource.BLOCKS, 
            1.0f, 1.0f);
    }

    /**
     * Checks if a player can place a block at the given position (respects claim systems)
     * @param level The world level
     * @param player The player attempting to place the block
     * @param pos The position to check
     * @param blockState The block state to place
     * @return true if the player can place the block, false otherwise
     */
    private static boolean canPlayerPlaceBlockAt(Level level, Player player, BlockPos pos, BlockState blockState) {
        // Check if the player can interact with this position
        // This method respects claim systems like FTB Chunks, WorldGuard, etc.
        return level.mayInteract(player, pos);
    }
    
    /**
     * Places a block as if the player placed it, respecting claim systems and protection mods
     * @param level The world level
     * @param player The player placing the block
     * @param pos The position to place the block
     * @param blockState The block state to place
     * @return true if the block was successfully placed, false otherwise
     */
    private static boolean placeBlockAsPlayer(Level level, Player player, BlockPos pos, BlockState blockState) {
        // First check if the player can place the block at this position
        if (!canPlayerPlaceBlockAt(level, player, pos, blockState)) {
            return false;
        }
        
        // Place the block with proper flags (3 = update neighbors and clients)
        boolean success = level.setBlock(pos, blockState, 3);
        
        if (success && level instanceof ServerLevel serverLevel) {
            // Notify the block that it was placed by a player
            Block block = blockState.getBlock();
            block.setPlacedBy(level, pos, blockState, player, new ItemStack(block.asItem()));
        }
        
        return success;
    }
    
    /**
     * Rotates a block state's directional properties based on the given rotation (0-3, representing 90-degree increments)
     */
    private static BlockState rotateBlockState(BlockState blockState, int rotation) {
        if (rotation == 0) return blockState;
        
        BlockState rotatedState = blockState;
        
        // Check all properties of the block state
        for (Property<?> property : blockState.getProperties()) {
            if (property instanceof DirectionProperty) {
                DirectionProperty dirProperty = (DirectionProperty) property;
                Direction currentDirection = blockState.getValue(dirProperty);
                Direction rotatedDirection = rotateDirection(currentDirection, rotation, dirProperty);
                
                // Only update if the rotated direction is valid for this property
                if (dirProperty.getPossibleValues().contains(rotatedDirection)) {
                    rotatedState = rotatedState.setValue(dirProperty, rotatedDirection);
                }
            }
        }
        
        return rotatedState;
    }
    
    /**
     * Rotates a direction based on the rotation amount and property constraints
     */
    private static Direction rotateDirection(Direction direction, int rotation, DirectionProperty property) {
        // Normalize rotation to 0-3 range
        rotation = ((rotation % 4) + 4) % 4;
        
        // For horizontal-only properties, only rotate around Y-axis
        boolean isHorizontalOnly = property.getPossibleValues().stream()
            .allMatch(dir -> dir.getAxis() != Direction.Axis.Y);
        
        if (isHorizontalOnly && (direction == Direction.UP || direction == Direction.DOWN)) {
            return direction; // Don't rotate vertical directions for horizontal-only properties
        }
        
        Direction result = direction;
        for (int i = 0; i < rotation; i++) {
            result = rotateDirectionClockwise(result, isHorizontalOnly);
        }
        
        return result;
    }
    
    /**
     * Rotates a direction 90 degrees clockwise around the Y-axis
     */
    private static Direction rotateDirectionClockwise(Direction direction, boolean horizontalOnly) {
        switch (direction) {
            case NORTH: return Direction.EAST;
            case EAST: return Direction.SOUTH;
            case SOUTH: return Direction.WEST;
            case WEST: return Direction.NORTH;
            case UP: return horizontalOnly ? Direction.UP : Direction.UP; // Keep UP as UP for horizontal-only
            case DOWN: return horizontalOnly ? Direction.DOWN : Direction.DOWN; // Keep DOWN as DOWN for horizontal-only
            default: return direction;
        }
    }
    
    /**
     * Applies rotation to a block position relative to structure origin
     */
    private static BlockPos rotateBlockPos(BlockPos relativePos, MultiblockStructure structure, int rotation) {
        if (rotation == 0) return relativePos;
        
        // Convert to structure-relative coordinates (same as PreviewRenderer)
        int xo = relativePos.getX() - structure.getMinX();
        int yo = relativePos.getY() - structure.getMinY();
        int zo = relativePos.getZ() - structure.getMinZ();
        
        // Apply rotation (matching boundary rendering coordinate system)
        int rotatedX = xo;
        int rotatedZ = zo;
        int bWidth = structure.getDepth();   // same as boundary rendering
        int bLength = structure.getWidth();  // same as boundary rendering
        
        switch (rotation) {
            case 1:
                rotatedZ = xo;
                rotatedX = (bWidth - zo - 1);
                break;
            case 2:
                rotatedX = (bLength - xo - 1);
                rotatedZ = (bWidth - zo - 1);
                break;
            case 3:
                rotatedZ = (bLength - xo - 1);
                rotatedX = zo;
                break;
        }
        
        // Return the rotated offset (add back the structure's min coordinates to match original relative position format) format)
        return new BlockPos(rotatedX + structure.getMinX(), yo + structure.getMinY(), rotatedZ + structure.getMinZ());
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
     * Spawns smoke particles around the built multiblock structure
     * @param serverLevel The server level
     * @param structure The built structure
     * @param centerPos The center position of the structure
     * @param rotation The rotation applied to the structure
     */
    private static void spawnSmokeParticles(ServerLevel serverLevel, MultiblockStructure structure, 
                                          BlockPos centerPos, int rotation) {
        RandomSource random = serverLevel.getRandom();
        
        // Calculate structure bounds after rotation
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        
        // Find the actual bounds of the rotated structure
        for (BlockPos relativePos : structure.getBlocks().keySet()) {
            BlockState blockState = structure.getBlocks().get(relativePos);
            if (!blockState.isAir()) {
                BlockPos rotatedRelativePos = rotateBlockPos(relativePos, structure, rotation);
                BlockPos worldPos = centerPos.offset(rotatedRelativePos);
                
                minX = Math.min(minX, worldPos.getX());
                maxX = Math.max(maxX, worldPos.getX());
                minY = Math.min(minY, worldPos.getY());
                maxY = Math.max(maxY, worldPos.getY());
                minZ = Math.min(minZ, worldPos.getZ());
                maxZ = Math.max(maxZ, worldPos.getZ());
            }
        }
        
        // Spawn particles around the structure perimeter
        int particleCount = Math.max(20, (maxX - minX + maxZ - minZ) * 2); // Scale with structure size
        
        for (int i = 0; i < particleCount; i++) {
            double x, y, z;
            
            // Choose a random side of the structure to spawn particles on
            int side = random.nextInt(4);
            switch (side) {
                case 0: // North side
                    x = minX + random.nextDouble() * (maxX - minX + 1);
                    z = minZ - 0.5 + random.nextDouble() * 0.5;
                    break;
                case 1: // South side
                    x = minX + random.nextDouble() * (maxX - minX + 1);
                    z = maxZ + 0.5 + random.nextDouble() * 0.5;
                    break;
                case 2: // West side
                    x = minX - 0.5 + random.nextDouble() * 0.5;
                    z = minZ + random.nextDouble() * (maxZ - minZ + 1);
                    break;
                default: // East side
                    x = maxX + 0.5 + random.nextDouble() * 0.5;
                    z = minZ + random.nextDouble() * (maxZ - minZ + 1);
                    break;
            }
            
            // Random height within structure bounds, slightly above ground
            y = minY + random.nextDouble() * (maxY - minY + 2) + 0.5;
            
            double velocityX = (random.nextDouble() - 0.5) * 0.1;
            double velocityY = random.nextDouble() * 0.1 + 0.05; // Upward motion
            double velocityZ = (random.nextDouble() - 0.5) * 0.1;
            
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 1, velocityX, velocityY, velocityZ, 0.02);
            
            if (random.nextFloat() < 0.3f) {
                serverLevel.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 1, velocityX, velocityY, velocityZ, 0.02);
            }
        }
        
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2;
            double offsetY = random.nextDouble() * 2;
            double offsetZ = (random.nextDouble() - 0.5) * 2;
            
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                centerPos.getX() + 0.5 + offsetX, 
                centerPos.getY() + 1 + offsetY, 
                centerPos.getZ() + 0.5 + offsetZ, 
                3, 0.1, 0.1, 0.1, 0.05);
        }
    }
    
    /**
     * Creates a replacement BlockState using the replacement block while trying to preserve compatible properties
     * @param originalState The original block state
     * @param replacementBlock The replacement block to use
     * @return A new BlockState with the replacement block
     */
    private static BlockState createReplacementBlockState(BlockState originalState, Block replacementBlock) {
        BlockState replacementState = replacementBlock.defaultBlockState();
        
        // Try to preserve compatible properties
        for (Property<?> property : originalState.getProperties()) {
            if (replacementState.hasProperty(property)) {
                try {
                    // Use a helper method to handle the generic type casting safely
                    replacementState = copyPropertyValue(originalState, replacementState, property);
                } catch (Exception e) {
                    // If there's any issue with property transfer, just use the default value
                    // This can happen if the property types don't match exactly
                }
            }
        }
        
        return replacementState;
    }
    
    /**
     * Helper method to safely copy a property value from one BlockState to another
     */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyPropertyValue(
            BlockState sourceState, BlockState targetState, Property<?> property) {
        Property<T> typedProperty = (Property<T>) property;
        T value = sourceState.getValue(typedProperty);
        
        // Check if the target block supports this property value
        if (typedProperty.getPossibleValues().contains(value)) {
            return targetState.setValue(typedProperty, value);
        }
        
        return targetState;
    }
    
    /**
     * Calculate the bounding box of a placed structure
     */
    private static AABB calculateStructureBounds(MultiblockStructure structure, BlockPos centerPos, int rotation) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        
        // Calculate bounds by checking all block positions in the structure
        for (BlockPos relativePos : structure.getBlocks().keySet()) {
            BlockState blockState = structure.getBlocks().get(relativePos);
            
            // Skip air blocks
            if (blockState.isAir()) {
                continue;
            }
            
            // Apply rotation to position
            BlockPos rotatedRelativePos = rotateBlockPos(relativePos, structure, rotation);
            BlockPos worldPos = centerPos.offset(rotatedRelativePos);
            
            // Update bounds
            minX = Math.min(minX, worldPos.getX());
            maxX = Math.max(maxX, worldPos.getX());
            minY = Math.min(minY, worldPos.getY());
            maxY = Math.max(maxY, worldPos.getY());
            minZ = Math.min(minZ, worldPos.getZ());
            maxZ = Math.max(maxZ, worldPos.getZ());
        }
        
        return new AABB(minX, minY, minZ, maxX, maxY , maxZ);
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