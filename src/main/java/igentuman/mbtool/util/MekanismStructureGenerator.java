package igentuman.mbtool.util;

import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static igentuman.mbtool.Mbtool.rl;

public class MekanismStructureGenerator {

    public static MultiblockStructure generate(List<ItemStack> blocks, int height, int width, int length) {
        if(isSetForTurbine(blocks)) {
            return generateTurbine(blocks, height, width, length);
        }

        if(isSetForFissionReactor(blocks)) {
            return generateFissionReactor(blocks, height, width, length);
        }

        if(isSetForBoiler(blocks)) {
            return generateBoiler(blocks, height, width, length);
        }
        return null;
    }

    private static MultiblockStructure generateBoiler(List<ItemStack> blocks, int height, int width, int length) {
        MultiblockStructure structure = new MultiblockStructure(rl("runtime"), new CompoundTag(), "mekanism_boiler");
        return structure;
    }

    private static MultiblockStructure generateFissionReactor(List<ItemStack> blocks, int height, int width, int length) {
        // Validate dimensions
        if (!validateDimensions(height, width, length)) {
            return null;
        }

        // Create block availability map with stack counts
        Map<String, Integer> availableBlocks = createBlockAvailabilityMap(blocks);
        Map<String, Integer> usedBlocks = new HashMap<>();

        CompoundTag nbt = new CompoundTag();
        ListTag blocksList = new ListTag();
        ListTag palette = new ListTag();
        Map<String, Integer> paletteMap = new HashMap<>();
        AtomicInteger paletteIndex = new AtomicInteger(0);

        // Collect wall positions for port placement (excluding corners and edges)
        List<BlockPosition> wallPositions = new ArrayList<>();

        // Generate structure
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    String blockType = determineBlockType(x, y, z, width, height, length, availableBlocks, usedBlocks);
                    if (blockType != null) {
                        // Check if we have enough blocks
                        if (canUseBlock(blockType, availableBlocks, usedBlocks)) {
                            addBlockToStructure(blocksList, palette, paletteMap, x, y, z, blockType, paletteIndex);
                            usedBlocks.put(blockType, usedBlocks.getOrDefault(blockType, 0) + 1);
                        }
                    }
                    
                    // Collect suitable wall positions for port placement (not corners, not edges, not floor/ceiling)
                    if (isWallPosition(x, y, z, width, height, length) && 
                        !isCornerPosition(x, y, z, width, height, length) && 
                        !isEdgePosition(x, y, z, width, height, length) &&
                        y > 0 && y < height - 1) {
                        wallPositions.add(new BlockPosition(x, y, z));
                    }
                }
            }
        }

        // Add random ports
        addRandomPorts(wallPositions, availableBlocks, usedBlocks, blocksList, palette, paletteMap, paletteIndex, width, height, length);

        nbt.put("blocks", blocksList);
        nbt.put("palette", palette);
        MultiblockStructure structure = new MultiblockStructure(rl("runtime"), nbt, "mekanism_fission_reactor");

        return structure;
    }

    private static MultiblockStructure generateTurbine(List<ItemStack> blocks, int height, int width, int length) {
        MultiblockStructure structure = new MultiblockStructure(rl("runtime"), new CompoundTag(), "mekanism_turbine");
        return structure;
    }

    private static boolean isSetForBoiler(List<ItemStack> blocks) {
        for(ItemStack block : blocks) {
            if(block.getItem().toString().contains("boiler")) {
                return true;
            }
        }
        return false;
    }


    private static boolean isSetForFissionReactor(List<ItemStack> blocks) {
        for(ItemStack block : blocks) {
            if(block.getItem().toString().contains("fission_reactor")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSetForTurbine(List<ItemStack> blocks) {
        for(ItemStack block : blocks) {
            if(block.getItem().toString().contains("turbine")) {
                return true;
            }
        }
        return false;
    }

    // Helper classes and methods for fission reactor generation

    private static class BlockPosition {
        final int x, y, z;

        BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static boolean validateDimensions(int height, int width, int length) {
        // Minimum size check (3x4x3)
        if (width < 3 || height < 4 || length < 3) {
            return false;
        }
        
        // Maximum size check (18x18x18)
        if (width > 18 || height > 18 || length > 18) {
            return false;
        }
        
        return true;
    }

    private static Map<String, Integer> createBlockAvailabilityMap(List<ItemStack> blocks) {
        Map<String, Integer> availableBlocks = new HashMap<>();
        int ports = howManyPorts(blocks);
        boolean hasGlass = hasGlass(blocks);
        for (ItemStack stack : blocks) {
            String blockName = getBlockName(stack);
            int count = stack.getCount();
            if(stack.getItem().toString().contains("glass")) {
                count += ports;
            }
            if(!hasGlass && stack.getItem().toString().contains("casing")) {
                count += ports;
            }
            availableBlocks.put(blockName, availableBlocks.getOrDefault(blockName, 0) + count);
        }
        
        return availableBlocks;
    }

    private static boolean hasGlass(List<ItemStack> blocks) {
        for (ItemStack stack : blocks) {
            if(stack.getItem().toString().contains("glass")) {
                return true;
            }
        }
        return false;
    }

    private static int howManyPorts(List<ItemStack> blocks) {
        int ports = 0;
        for (ItemStack stack : blocks) {
            if(stack.getItem().toString().contains("port")) {
                ports += stack.getCount();
            }
        }
        return  ports;
    }

    private static String getBlockName(ItemStack stack) {
        // Get the registry name which is in format "modid:item_id"
        // For items that correspond to blocks, we need to get the block's registry name
        String registryName = stack.getItem().builtInRegistryHolder().key().location().toString();
        
        // Convert item registry name to block registry name if needed
        // Most items have the same registry name as their corresponding blocks
        // but some might have "_item" suffix that needs to be removed
        if (registryName.endsWith("_item")) {
            registryName = registryName.substring(0, registryName.length() - 5);
        }
        
        return registryName;
    }

    private static boolean canUseBlock(String blockType, Map<String, Integer> availableBlocks, Map<String, Integer> usedBlocks) {
        int available = availableBlocks.getOrDefault(blockType, 0);
        int used = usedBlocks.getOrDefault(blockType, 0);
        return available > used;
    }

    private static String determineBlockType(int x, int y, int z, int width, int height, int length, 
                                           Map<String, Integer> availableBlocks, Map<String, Integer> usedBlocks) {
        boolean isCorner = isCornerPosition(x, y, z, width, height, length);
        boolean isEdge = isEdgePosition(x, y, z, width, height, length);
        boolean isWall = isWallPosition(x, y, z, width, height, length);
        boolean isInterior = !isWall;
        boolean isFloor = (y == 0);
        boolean isCeiling = (y == height - 1);
        
        // Structure logic
        if (isCorner || isEdge) {
            return "mekanismgenerators:fission_reactor_casing";
        }
        
        if (isWall || isCeiling || isFloor) {
            // Use glass if available, otherwise casing (for walls, ceiling, and floor)
            if (availableBlocks.containsKey("mekanismgenerators:reactor_glass") && 
                canUseBlock("mekanismgenerators:reactor_glass", availableBlocks, usedBlocks)) {
                return "mekanismgenerators:reactor_glass";
            }
            return "mekanismgenerators:fission_reactor_casing";
        }
        
        if (isInterior) {
            return determineInteriorBlock(x, y, z, width, height, length, availableBlocks, usedBlocks);
        }
        
        return null; // Air/empty space
    }

    private static String determineInteriorBlock(int x, int y, int z, int width, int height, int length,
                                               Map<String, Integer> availableBlocks, Map<String, Integer> usedBlocks) {
        // Interior coordinates (excluding walls)
        int interiorX = x - 1;
        int interiorZ = z - 1;
        
        // Checkerboard pattern for fuel assemblies - works for both odd and even dimensions
        boolean isFuelPosition = (interiorX + interiorZ) % 2 == 0;
        
        if (!isFuelPosition) {
            return null; // Air space
        }
        
        // Bottom layer: fuel assembly
        if (y == 1) {
            if (availableBlocks.containsKey("mekanismgenerators:fission_fuel_assembly") &&
                canUseBlock("mekanismgenerators:fission_fuel_assembly", availableBlocks, usedBlocks)) {
                return "mekanismgenerators:fission_fuel_assembly";
            }
        }
        
        // Middle layers: continue fuel assembly tower
        if (y > 1 && y < height - 2) {
            if (availableBlocks.containsKey("mekanismgenerators:fission_fuel_assembly") &&
                canUseBlock("mekanismgenerators:fission_fuel_assembly", availableBlocks, usedBlocks)) {
                return "mekanismgenerators:fission_fuel_assembly";
            }
        }
        
        // Top of tower: control rod
        if (y == height - 2) {
            if (availableBlocks.containsKey("mekanismgenerators:control_rod_assembly") &&
                canUseBlock("mekanismgenerators:control_rod_assembly", availableBlocks, usedBlocks)) {
                return "mekanismgenerators:control_rod_assembly";
            }
            // Fallback to fuel assembly if no control rods
            if (availableBlocks.containsKey("mekanismgenerators:fission_fuel_assembly") &&
                canUseBlock("mekanismgenerators:fission_fuel_assembly", availableBlocks, usedBlocks)) {
                return "mekanismgenerators:fission_fuel_assembly";
            }
        }
        
        return null;
    }

    private static boolean isCornerPosition(int x, int y, int z, int width, int height, int length) {
        boolean isXEdge = (x == 0 || x == width - 1);
        boolean isYEdge = (y == 0 || y == height - 1);
        boolean isZEdge = (z == 0 || z == length - 1);
        
        // Corner if on 3 edges, or on 2 edges including Y
        return (isXEdge && isYEdge && isZEdge) || 
               (isYEdge && ((isXEdge && isZEdge)));
    }

    private static boolean isEdgePosition(int x, int y, int z, int width, int height, int length) {
        boolean isXEdge = (x == 0 || x == width - 1);
        boolean isYEdge = (y == 0 || y == height - 1);
        boolean isZEdge = (z == 0 || z == length - 1);
        
        // Edge if on exactly 2 faces
        int edgeCount = (isXEdge ? 1 : 0) + (isYEdge ? 1 : 0) + (isZEdge ? 1 : 0);
        return edgeCount == 2 && !isCornerPosition(x, y, z, width, height, length);
    }

    private static boolean isWallPosition(int x, int y, int z, int width, int height, int length) {
        return (x == 0 || x == width - 1 || y == 0 || y == height - 1 || z == 0 || z == length - 1);
    }

    private static void addBlockToStructure(ListTag blocksList, ListTag palette, 
                                          Map<String, Integer> paletteMap,
                                          int x, int y, int z, String blockType, 
                                          AtomicInteger paletteIndex) {
        // Add to palette if new
        if (!paletteMap.containsKey(blockType)) {
            CompoundTag paletteEntry = new CompoundTag();
            paletteEntry.putString("Name", blockType);
            palette.add(paletteEntry);
            paletteMap.put(blockType, paletteIndex.getAndIncrement());
        }
        
        // Create block entry
        CompoundTag blockEntry = new CompoundTag();
        ListTag pos = new ListTag();
        pos.add(IntTag.valueOf(x));
        pos.add(IntTag.valueOf(y));
        pos.add(IntTag.valueOf(z));
        
        blockEntry.put("pos", pos);
        blockEntry.putInt("state", paletteMap.get(blockType));
        
        blocksList.add(blockEntry);
    }

    private static void addRandomPorts(List<BlockPosition> wallPositions, 
                                     Map<String, Integer> availableBlocks,
                                     Map<String, Integer> usedBlocks,
                                     ListTag blocksList, ListTag palette, 
                                     Map<String, Integer> paletteMap,
                                     AtomicInteger paletteIndex, 
                                     int width, int height, int length) {
        List<String> portTypes = Arrays.asList(
            "mekanismgenerators:fission_reactor_port",
            "mekanismgenerators:fission_reactor_logic_adapter"
        );
        
        // Filter available port types
        List<String> availablePorts = new ArrayList<>();
        for (String portType : portTypes) {
            if (availableBlocks.containsKey(portType) && 
                canUseBlock(portType, availableBlocks, usedBlocks)) {
                availablePorts.add(portType);
            }
        }
        
        if (availablePorts.isEmpty()) return;
        
        // Place minimum 4 ports, maximum 25% of wall positions
        int maxPorts = Math.min(wallPositions.size() / 4, availablePorts.size() * 4);
        int minPorts = Math.min(4, maxPorts);
        
        Collections.shuffle(wallPositions);
        
        // Track what block types are being replaced by ports
        Map<String, Integer> replacedBlockCounts = new HashMap<>();
        
        int portsPlaced = 0;
        for (int i = 0; i < wallPositions.size() && portsPlaced < minPorts; i++) {
            BlockPosition pos = wallPositions.get(i);
            String portType = availablePorts.get(portsPlaced % availablePorts.size());
            
            if (canUseBlock(portType, availableBlocks, usedBlocks)) {
                // Determine what block type would have been placed at this position
                String replacedBlockType = determineReplacedBlockType(pos.x, pos.y, pos.z, width, height, length, availableBlocks, usedBlocks);
                
                // Track the replaced block type
                if (replacedBlockType != null) {
                    replacedBlockCounts.put(replacedBlockType, replacedBlockCounts.getOrDefault(replacedBlockType, 0) + 1);
                }
                
                // Add port block to structure
                addBlockToStructure(blocksList, palette, paletteMap, pos.x, pos.y, pos.z, portType, paletteIndex);
                usedBlocks.put(portType, usedBlocks.getOrDefault(portType, 0) + 1);
                portsPlaced++;
            }
        }
        
        // Adjust the counts of replaced block types by the total number of ports that replaced them
        for (Map.Entry<String, Integer> entry : replacedBlockCounts.entrySet()) {
            String blockType = entry.getKey();
            int replacedCount = entry.getValue();
            int currentUsed = usedBlocks.getOrDefault(blockType, 0);
            if (currentUsed >= replacedCount) {
                usedBlocks.put(blockType, currentUsed - replacedCount);
            }
        }
    }

    private static String determineReplacedBlockType(int x, int y, int z, int width, int height, int length,
                                                   Map<String, Integer> availableBlocks, Map<String, Integer> usedBlocks) {
        boolean isCorner = isCornerPosition(x, y, z, width, height, length);
        boolean isEdge = isEdgePosition(x, y, z, width, height, length);
        boolean isWall = isWallPosition(x, y, z, width, height, length);
        boolean isFloor = (y == 0);
        boolean isCeiling = (y == height - 1);
        
        // Same logic as determineBlockType but for determining what would have been placed
        if (isCorner || isEdge) {
            return "mekanismgenerators:fission_reactor_casing";
        }
        
        if (isWall || isCeiling || isFloor) {
            // Check if glass would have been used
            if (availableBlocks.containsKey("mekanismgenerators:reactor_glass")) {
                return "mekanismgenerators:reactor_glass";
            }
            return "mekanismgenerators:fission_reactor_casing";
        }
        
        return null; // Should not happen for wall positions
    }
}
