package igentuman.mbtool.util;

import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.integration.ae2.AE2Helper;
import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        
        if (level.isClientSide()) {
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
        
        Map<BlockPos, Block> plannedReplacements = new HashMap<>();
        Map<Block, Integer> blocksFromAE2ToExtract = new HashMap<>();
        int[] tempSlotCounts = null;

        // Check if we have all required materials (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack, level.registryAccess());
            if (inventory == null) {
                return new BuildResult(false, Component.translatable("message.mbtool.no_inventory"));
            }

            Map<Block, Integer> missingBlocks = new HashMap<>();

            // Per-slot simulation
            tempSlotCounts = new int[inventory.getSlots()];
            Block[] tempSlotBlocks = new Block[inventory.getSlots()];
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    tempSlotBlocks[i] = Block.byItem(stack.getItem());
                    tempSlotCounts[i] = stack.getCount();
                } else {
                    tempSlotBlocks[i] = Blocks.AIR;
                    tempSlotCounts[i] = 0;
                }
            }

            // AE2 simulation
            Map<Block, Integer> tempAe2Counts = new HashMap<>();
            boolean hasME = isAllowedMEAccess(multibuilderStack) && AE2Helper.hasAe2Terminal((ServerPlayer) player);
            if (hasME) {
                for (Block requiredBlock : requiredBlocks.keySet()) {
                    if (!tempAe2Counts.containsKey(requiredBlock)) {
                        long stored = AE2Helper.getAvailableAmount((ServerPlayer) player, requiredBlock);
                        tempAe2Counts.put(requiredBlock, (int) Math.min(stored, Integer.MAX_VALUE));
                    }
                    Set<Block> equivalents = BlockEquivalencyManager.getEquivalentBlocks(requiredBlock);
                    for (Block equivalent : equivalents) {
                        if (!tempAe2Counts.containsKey(equivalent)) {
                            long stored = AE2Helper.getAvailableAmount((ServerPlayer) player, equivalent);
                            tempAe2Counts.put(equivalent, (int) Math.min(stored, Integer.MAX_VALUE));
                        }
                    }
                }
            }

            for (Map.Entry<BlockPos, BlockState> entry : structure.getBlocks().entrySet()) {
                BlockPos relativePos = entry.getKey();
                BlockState blockState = entry.getValue();

                if (blockState.isAir()) {
                    continue;
                }

                Block requiredBlock = blockState.getBlock();
                Block chosenBlock = null;

                // 1. Try exact block in inventory
                for (int i = 0; i < tempSlotCounts.length; i++) {
                    if (tempSlotBlocks[i] == requiredBlock && tempSlotCounts[i] > 0) {
                        tempSlotCounts[i]--;
                        chosenBlock = requiredBlock;
                        break;
                    }
                }

                // 2. Try equivalent block in inventory
                if (chosenBlock == null) {
                    Set<Block> equivalents = BlockEquivalencyManager.getEquivalentBlocks(requiredBlock);
                    for (int i = 0; i < tempSlotCounts.length; i++) {
                        Block slotBlock = tempSlotBlocks[i];
                        if (slotBlock != Blocks.AIR && slotBlock != requiredBlock && equivalents.contains(slotBlock) && tempSlotCounts[i] > 0) {
                            tempSlotCounts[i]--;
                            chosenBlock = slotBlock;
                            break;
                        }
                    }
                }

                // 3. Try exact block from AE2
                if (chosenBlock == null && hasME) {
                    int ae2Count = tempAe2Counts.getOrDefault(requiredBlock, 0);
                    if (ae2Count > 0) {
                        tempAe2Counts.put(requiredBlock, ae2Count - 1);
                        blocksFromAE2ToExtract.put(requiredBlock, blocksFromAE2ToExtract.getOrDefault(requiredBlock, 0) + 1);
                        chosenBlock = requiredBlock;
                    }
                }

                // 3b. Try equivalent block from AE2
                if (chosenBlock == null && hasME) {
                    Set<Block> equivalents = BlockEquivalencyManager.getEquivalentBlocks(requiredBlock);
                    for (Block equivalent : equivalents) {
                        if (equivalent != requiredBlock) {
                            int ae2Count = tempAe2Counts.getOrDefault(equivalent, 0);
                            if (ae2Count > 0) {
                                tempAe2Counts.put(equivalent, ae2Count - 1);
                                blocksFromAE2ToExtract.put(equivalent, blocksFromAE2ToExtract.getOrDefault(equivalent, 0) + 1);
                                chosenBlock = equivalent;
                                break;
                            }
                        }
                    }
                }

                // 4. Missing
                if (chosenBlock == null) {
                    missingBlocks.put(requiredBlock, missingBlocks.getOrDefault(requiredBlock, 0) + 1);
                } else {
                    plannedReplacements.put(relativePos, chosenBlock);
                }
            }

            // If there are missing blocks, attempt autocrafting if enabled
            if (!missingBlocks.isEmpty()) {
                if (isMeAutocraftingEnabled(multibuilderStack)) {
                    return attemptAutocrafting((ServerPlayer) player, missingBlocks);
                } else {
                    Map.Entry<Block, Integer> first = missingBlocks.entrySet().iterator().next();
                    return new BuildResult(false, Component.translatable("message.mbtool.insufficient_blocks",
                            first.getValue(), first.getKey().getName()));
                }
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
        
        // Consume materials from inventory and AE2 (skip for creative mode)
        if (!isCreative) {
            IItemHandler inventory = getInventory(multibuilderStack, level.registryAccess());
            if (tempSlotCounts != null) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        int originalCount = stack.getCount();
                        int plannedCount = tempSlotCounts[i];
                        int toExtract = originalCount - plannedCount;
                        if (toExtract > 0) {
                            inventory.extractItem(i, toExtract, false);
                        }
                    }
                }
            }
            if (!blocksFromAE2ToExtract.isEmpty()) {
                for (Map.Entry<Block, Integer> ae2Entry : blocksFromAE2ToExtract.entrySet()) {
                    if (isAllowedMEAccess(multibuilderStack) && AE2Helper.hasAe2Terminal((ServerPlayer) player)) {
                        AE2Helper.extractItems((ServerPlayer) player, ae2Entry.getKey(), ae2Entry.getValue());
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
                Block replacementBlock = plannedReplacements.get(relativePos);
                if (replacementBlock != null && replacementBlock != rotatedBlockState.getBlock()) {
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
        
        // Sync inventory to client after building
        if (player instanceof ServerPlayer && blocksPlaced > 0) {
            MultibuilderItem.syncInventoryToClient((ServerPlayer) player, multibuilderStack, InteractionHand.MAIN_HAND);
        }
        
        return new BuildResult(true, Component.translatable("message.mbtool.multiblock_built", 
            blocksPlaced, Component.translatable(structure.getName())));
    }

    private static boolean isMeAutocraftingEnabled(ItemStack multibuilderStack) {
        if (!ModUtil.isAe2Loaded()) return false;
        return MultibuilderItem.isMeAutocraftingEnabled(multibuilderStack);
    }

    /**
     * Attempts to request autocrafting for all missing blocks via the ME network.
     * This method is fully non-blocking — it starts async calculations and polls
     * for results on subsequent build attempts.
     */
    private static BuildResult attemptAutocrafting(ServerPlayer player, Map<Block, Integer> missingBlocks) {
        boolean anyCalculating = false;
        boolean anySubmitted = false;
        boolean anyNewlyStarted = false;
        java.util.List<Block> unrequestable = new ArrayList<>();

        for (Map.Entry<Block, Integer> missing : missingBlocks.entrySet()) {
            Block block = missing.getKey();
            int amount = missing.getValue();

            // First, check if there's already a tracked request for this block
            if (AutocraftTracker.hasActiveRequest(player.getUUID(), block)) {
                anyCalculating = true;
                continue;
            }

            // No existing request — start a new async calculation
            AE2Helper.AutocraftResult result = AE2Helper.startCraftingCalculation(player, block, amount);
            if (result == AE2Helper.AutocraftResult.CALCULATING) {
                anyNewlyStarted = true;
            } else if (result == AE2Helper.AutocraftResult.ALREADY_REQUESTING) {
                anySubmitted = true; // ME network is already crafting this
            } else if (result == AE2Helper.AutocraftResult.NO_PATTERN) {
                unrequestable.add(block);
            }
            // NO_GRID, FAILED — silently skip, will be reported below
        }

        if (!unrequestable.isEmpty()) {
            return new BuildResult(false, Component.translatable("message.mbtool.autocrafting_no_pattern",
                    unrequestable.get(0).getName()));
        } else if (anyCalculating || anyNewlyStarted) {
            return new BuildResult(false, Component.translatable("message.mbtool.crafting_in_progress"), true);
        } else if (anySubmitted) {
            return new BuildResult(false, Component.translatable("message.mbtool.autocrafting_requested"));
        }

        return new BuildResult(false, Component.translatable("message.mbtool.autocrafting_failed"));
    }

    private static boolean isAllowedMEAccess(ItemStack multibuilderStack) {
        if (!ModUtil.isAe2Loaded()) {
            return false;
        }
        if (multibuilderStack.getItem() instanceof MultibuilderItem item) {
            return item.isMeAccessAllowed(multibuilderStack);
        }
        return false;
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
            if (property instanceof EnumProperty<?> ep && ep.getValueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                EnumProperty<Direction> dirProperty = (EnumProperty<Direction>) ep;
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
    private static Direction rotateDirection(Direction direction, int rotation, EnumProperty<Direction> property) {
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
     * Get energy storage from multibuilder item
     */
    private static CustomEnergyStorage getEnergyStorage(ItemStack stack) {
        if (stack.getItem() instanceof MultibuilderItem item) {
            return item.getEnergy(stack);
        }
        return null;
    }
    
    /**
     * Get inventory from multibuilder item
     */
    private static IItemHandler getInventory(ItemStack stack, net.minecraft.core.HolderLookup.Provider provider) {
        if (stack.getItem() instanceof MultibuilderItem item) {
            return item.getInventory(stack, provider);
        }
        return null;
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
        private final boolean showAsTitle;

        public BuildResult(boolean success, Component message) {
            this(success, message, false);
        }

        public BuildResult(boolean success, Component message, boolean showAsTitle) {
            this.success = success;
            this.message = message;
            this.showAsTitle = showAsTitle;
        }

        public boolean isSuccess() {
            return success;
        }

        public Component getMessage() {
            return message;
        }

        public boolean isShowAsTitle() {
            return showAsTitle;
        }
    }
}