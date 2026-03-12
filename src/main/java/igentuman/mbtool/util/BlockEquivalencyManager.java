package igentuman.mbtool.util;

import igentuman.mbtool.config.MbtoolConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages block equivalency sets for replacement during multiblock building
 */
public class BlockEquivalencyManager {
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static Map<Block, Set<Block>> equivalencyMap = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * Initialize the equivalency map from config
     */
    public static void initialize() {
        equivalencyMap.clear();
        
        List<? extends String> equivalencySets = MbtoolConfig.getBlockEquivalencySets();
        
        for (String setString : equivalencySets) {
            if (setString == null || setString.trim().isEmpty()) {
                continue;
            }
            
            String[] blockIds = setString.split(",");
            Set<Block> blockSet = new HashSet<>();
            
            // Parse all blocks in this equivalency set
            for (String blockId : blockIds) {
                blockId = blockId.trim();
                if (!blockId.isEmpty()) {
                    try {
                        ResourceLocation resourceLocation = ResourceLocation.tryParse(blockId);
                        if (resourceLocation == null) {
                            LOGGER.warn("Invalid block ID format in equivalency set: {}", blockId);
                            continue;
                        }
                        Block block = BuiltInRegistries.BLOCK.get(resourceLocation);
                        if (block != Blocks.AIR) {
                            blockSet.add(block);
                        }
                    } catch (Exception e) {
                        // Invalid block ID, skip it
                        LOGGER.warn("Invalid block ID in equivalency set: {}", blockId);
                    }
                }
            }
            
            // Add bidirectional mappings for all blocks in this set
            if (blockSet.size() > 1) {
                for (Block block : blockSet) {
                    equivalencyMap.computeIfAbsent(block, k -> new HashSet<>()).addAll(blockSet);
                    // Remove self-reference
                    equivalencyMap.get(block).remove(block);
                }
            }
        }
        
        initialized = true;
        LOGGER.info("Loaded {} block equivalency sets with {} total block mappings", 
                   equivalencySets.size(), equivalencyMap.size());
    }
    
    /**
     * Get all blocks that are equivalent to the given block (including the block itself)
     */
    public static Set<Block> getEquivalentBlocks(Block block) {
        if (!initialized) {
            initialize();
        }
        
        Set<Block> equivalents = new HashSet<>();
        equivalents.add(block); // Always include the original block
        
        Set<Block> configuredEquivalents = equivalencyMap.get(block);
        if (configuredEquivalents != null) {
            equivalents.addAll(configuredEquivalents);
        }
        
        return equivalents;
    }
    
    /**
     * Check if two blocks are equivalent
     */
    public static boolean areBlocksEquivalent(Block block1, Block block2) {
        if (block1 == block2) {
            return true;
        }
        
        if (!initialized) {
            initialize();
        }
        
        Set<Block> equivalents = equivalencyMap.get(block1);
        return equivalents != null && equivalents.contains(block2);
    }
    
    /**
     * Find a replacement block from available blocks for the required block
     * @param requiredBlock The block that is needed
     * @param availableBlocks Map of available blocks and their counts
     * @return The replacement block to use, or null if no suitable replacement is found
     */
    public static Block findReplacement(Block requiredBlock, Map<Block, Integer> availableBlocks) {
        if (!initialized) {
            initialize();
        }
        
        // First check if we have the exact block
        if (availableBlocks.getOrDefault(requiredBlock, 0) > 0) {
            return requiredBlock;
        }
        
        // Look for equivalent blocks
        Set<Block> equivalents = getEquivalentBlocks(requiredBlock);
        for (Block equivalent : equivalents) {
            if (availableBlocks.getOrDefault(equivalent, 0) > 0) {
                return equivalent;
            }
        }
        
        return null; // No replacement found
    }
    
    /**
     * Force re-initialization of the equivalency map (useful when config changes)
     */
    public static void reinitialize() {
        initialized = false;
        initialize();
    }
}