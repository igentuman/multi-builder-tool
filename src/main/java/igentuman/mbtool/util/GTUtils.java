package igentuman.mbtool.util;

import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static igentuman.mbtool.Mbtool.rl;


public class GTUtils {

    public static void loadGtStructures(List<MultiblockStructure> loadedStructures) {
        List<MultiblockMachineDefinition> GT_MACHINES =  GTRegistries.MACHINES.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .filter(MultiblockMachineDefinition::isRenderXEIPreview)
                .toList();
        for(MultiblockMachineDefinition definition : GT_MACHINES){
            List<MultiblockShapeInfo> shapeInfos = definition.getMatchingShapes();
            if(shapeInfos.isEmpty()) continue;
            MultiblockShapeInfo info = shapeInfos.get(0);
            
            CompoundTag structureNbt = convertShapeInfoToNBT(info);
            if(structureNbt != null) {
                String machineName = definition.getId().getPath();
                MultiblockStructure structure = new MultiblockStructure(
                    definition.getId(), 
                    structureNbt, 
                    "gt_" + machineName
                );
                loadedStructures.add(structure);
            }
        }
    }
    
    /**
     * Converts a MultiblockShapeInfo to NBT format compatible with MultiblockStructure
     */
    public static CompoundTag convertShapeInfoToNBT(MultiblockShapeInfo shapeInfo) {
        try {
            CompoundTag nbt = new CompoundTag();
            ListTag blocksList = new ListTag();
            ListTag palette = new ListTag();
            Map<String, Integer> paletteMap = new HashMap<>();
            AtomicInteger paletteIndex = new AtomicInteger(0);
            
            // Get the blocks array from MultiblockShapeInfo
            // The array is structured as [z][y][x]
            var blocks = shapeInfo.getBlocks();
            int depth = blocks.length;
            int height = depth > 0 ? blocks[0].length : 0;
            int width = height > 0 ? blocks[0][0].length : 0;
            
            // Iterate through all positions in the structure
            for (int z = 0; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        var blockInfo = blocks[z][y][x];
                        
                        // Skip empty/air blocks
                        if (blockInfo == null) {
                            continue;
                        }
                        
                        BlockState blockState = blockInfo.getBlockState();
                        if (blockState.isAir()) {
                            continue;
                        }
                        
                        // Get block registry name
                        Block block = blockState.getBlock();
                        String blockName = ForgeRegistries.BLOCKS.getKey(block).toString();
                        
                        // Create block state string with properties
                        String blockStateString = getBlockStateString(blockState);
                        
                        // Add to palette if not already present
                        if (!paletteMap.containsKey(blockStateString)) {
                            CompoundTag paletteEntry = new CompoundTag();
                            paletteEntry.putString("Name", blockName);
                            
                            // Add properties if they exist
                            if (!blockState.getProperties().isEmpty()) {
                                CompoundTag properties = new CompoundTag();
                                for (Property<?> property : blockState.getProperties()) {
                                    String propertyName = property.getName();
                                    if(!propertyName.equals("facing")) continue;
                                    String propertyValue = getPropertyValueString(blockState, property);
                                    // Invert facing direction
                                    if (propertyName.equals("facing")) {
                                        propertyValue = invertFacingDirection(propertyValue);
                                    }
                                    properties.putString(propertyName, propertyValue);
                                }
                                paletteEntry.put("Properties", properties);
                            }
                            
                            palette.add(paletteEntry);
                            paletteMap.put(blockStateString, paletteIndex.getAndIncrement());
                        }
                        
                        // Create block entry
                        CompoundTag blockEntry = new CompoundTag();
                        ListTag pos = new ListTag();
                        pos.add(IntTag.valueOf(x));
                        pos.add(IntTag.valueOf(y));
                        pos.add(IntTag.valueOf(z));
                        blockEntry.put("pos", pos);
                        blockEntry.putInt("state", paletteMap.get(blockStateString));
                        
                        blocksList.add(blockEntry);
                    }
                }
            }
            
            nbt.put("blocks", blocksList);
            nbt.put("palette", palette);
            
            return nbt;
            
        } catch (Exception e) {
            // Log error and return null if conversion fails
            System.err.println("Failed to convert MultiblockShapeInfo to NBT: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Inverts a facing direction string to its opposite
     */
    private static String invertFacingDirection(String facing) {
        switch (facing.toLowerCase()) {
            case "north": return "south";
            case "south": return "north";
            case "east": return "west";
            case "west": return "east";
            case "up": return "down";
            case "down": return "up";
            default: return facing; // Return original if not a recognized direction
        }
    }
    
    /**
     * Creates a unique string representation of a BlockState including its properties
     */
    private static String getBlockStateString(BlockState blockState) {
        StringBuilder sb = new StringBuilder();
        Block block = blockState.getBlock();
        sb.append(ForgeRegistries.BLOCKS.getKey(block).toString());
        
        if (!blockState.getProperties().isEmpty()) {
            sb.append("[");
            boolean first = true;
            for (Property<?> property : blockState.getProperties()) {
                if(!property.getName().equals("facing")) continue;
                if (!first) {
                    sb.append(",");
                }
                String propertyValue = getPropertyValueString(blockState, property);
                // Invert facing direction
                if (property.getName().equals("facing")) {
                    propertyValue = invertFacingDirection(propertyValue);
                }
                sb.append(property.getName()).append("=").append(propertyValue);
                first = false;
            }
            sb.append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the string representation of a property value from a BlockState
     */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyValueString(BlockState blockState, Property<T> property) {
        T value = blockState.getValue(property);
        return property.getName(value);
    }

    public static String formatEUEnergy(int energy)
    {
        energy = energy / FE2EURatio();
        if(energy >= 1000000) {
            return TextUtils.numberFormat(energy/1000000d)+" MEU";
        }
        if(energy >= 1000) {
            return TextUtils.numberFormat(energy/1000d)+" kEU";
        }
        return TextUtils.numberFormat(energy)+" EU";
    }

    public static int convert2FE(long eu) {
        long converted = eu * FE2EURatio();
        if(converted > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if(converted < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) converted;
    }
    public static int convert2EU(int fe) {
        return (int) (fe / FE2EURatio());
    }

    public static int FE2EURatio() {
        return FeCompat.ratio(true);
    }

    public static int EU2FERatio() {
        return FeCompat.ratio(false);
    }

}
