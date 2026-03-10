package igentuman.mbtool.util;

import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MMUtil {
    public static List<MultiblockStructure> loadMMStructures(List<MultiblockStructure> loadedStructures) {
        for(ResourceLocation id: StructureManager.STRUCTURES.keySet()) {
            MultiblockStructure structure = convertMMStructure(id);
            if(structure != null) {
                loadedStructures.add(structure);
            }
        }
        return loadedStructures;
    }

    private static MultiblockStructure convertMMStructure(ResourceLocation structureId) {
        try {
            StructureModel model = StructureManager.STRUCTURES.get(structureId);
            if (model == null) {
                return null;
            }

            CompoundTag structureNbt = convertModelToNBT(model);
            if (structureNbt == null) {
                return null;
            }

            String structureName = "mm_" + structureId.getPath();
            return new MultiblockStructure(structureId, structureNbt, structureName);
        } catch (Exception e) {
            System.err.println("Failed to convert MasterfulMachinery structure: " + structureId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a MasterfulMachinery StructureModel to NBT format compatible with MultiblockStructure
     */
    private static CompoundTag convertModelToNBT(StructureModel model) {
        try {
            CompoundTag nbt = new CompoundTag();
            ListTag blocksList = new ListTag();
            ListTag palette = new ListTag();
            Map<String, Integer> paletteMap = new HashMap<>();
            AtomicInteger paletteIndex = new AtomicInteger(0);

            // Get the structure layout from the model
            // MasterfulMachinery structures are typically stored as a map of BlockPos to BlockState
            Map<BlockPos, BlockState> structureBlocks = getStructureBlocks(model);
            
            if (structureBlocks == null || structureBlocks.isEmpty()) {
                return null;
            }

            // Iterate through all blocks in the structure
            for (Map.Entry<BlockPos, BlockState> entry : structureBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState blockState = entry.getValue();

                // Skip air blocks
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
                            String propertyValue = getPropertyValueString(blockState, property);
                            properties.putString(propertyName, propertyValue);
                        }
                        paletteEntry.put("Properties", properties);
                    }

                    palette.add(paletteEntry);
                    paletteMap.put(blockStateString, paletteIndex.getAndIncrement());
                }

                // Create block entry
                CompoundTag blockEntry = new CompoundTag();
                ListTag posTag = new ListTag();
                posTag.add(IntTag.valueOf(pos.getX()+1));
                posTag.add(IntTag.valueOf(pos.getY()+1));
                posTag.add(IntTag.valueOf(pos.getZ()));
                blockEntry.put("pos", posTag);
                blockEntry.putInt("state", paletteMap.get(blockStateString));

                blocksList.add(blockEntry);
            }
            //add controller
            BlockPos controllerPos = model.layout().getCharGrid().getControllerPos();
            String controllerState = model.controllerIds().getIds().get(0).toString();
            paletteMap.put(controllerState, paletteIndex.getAndIncrement());
            CompoundTag paletteEntry = new CompoundTag();
            paletteEntry.putString("Name", controllerState);
            palette.add(paletteEntry);
            CompoundTag blockEntry = new CompoundTag();
            ListTag posTag = new ListTag();
            posTag.add(IntTag.valueOf(controllerPos.getX()));
            posTag.add(IntTag.valueOf(controllerPos.getY()));
            posTag.add(IntTag.valueOf(controllerPos.getZ()));
            blockEntry.put("pos", posTag);
            blockEntry.putInt("state", paletteMap.get(controllerState));
            blocksList.add(blockEntry);

            nbt.put("blocks", blocksList);
            nbt.put("palette", palette);

            return nbt;

        } catch (Exception e) {
            System.err.println("Failed to convert StructureModel to NBT: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the block map from a MasterfulMachinery StructureModel
     * Converts the StructureLayout into a Map of BlockPos to BlockState
     */
    private static Map<BlockPos, BlockState> getStructureBlocks(StructureModel model) {
        try {
            StructureLayout layout = model.layout();
            
            if (layout == null) {
                System.err.println("StructureModel layout is null");
                return null;
            }
            
            Map<BlockPos, BlockState> blockMap = new HashMap<>();
            
            // Get the positioned pieces from the layout (using NONE rotation for base structure)
            List<io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece> pieces = 
                layout.getPositionedPieces();
            
            if (pieces == null || pieces.isEmpty()) {
                System.err.println("StructureModel has no positioned pieces");
                return null;
            }
            
            // Iterate through each positioned piece
            for (io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece positionedPiece : pieces) {
                BlockPos pos = positionedPiece.pos();
                
                // Get the blocks that this piece can represent
                // Each piece has a supplier that provides the list of valid blocks
                List<Block> validBlocks = positionedPiece.piece().getGuiPiece().getBlocks();
                
                if (validBlocks != null && !validBlocks.isEmpty()) {
                    // Use the first valid block as the representative block for this position
                    Block block = validBlocks.get(0);
                    BlockState blockState = block.defaultBlockState();
                    blockMap.put(pos, blockState);
                }
            }


            if (blockMap.isEmpty()) {
                System.err.println("No blocks extracted from StructureModel");
                return null;
            }
            
            return blockMap;
            
        } catch (Exception e) {
            System.err.println("Failed to extract structure blocks from StructureModel: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
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
                if (!first) {
                    sb.append(",");
                }
                sb.append(property.getName()).append("=").append(getPropertyValueString(blockState, property));
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
}
