package igentuman.mbtool.util;

import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages placed structures stored in the multibuilder tool's NBT data
 */
public class PlacedStructuresManager {
    private static final String NBT_KEY = "placedStructures";
    
    /**
     * Add a placed structure to the multibuilder tool's Data Components
     */
    public static void addPlacedStructure(ItemStack multibuilderStack, String structureId, AABB boundingBox, UUID placedBy, int rotation) {
        if(structureId == null) return;
        CompoundTag tag = multibuilderStack.getOrDefault(MbtoolDataComponents.PLACED_STRUCTURES.get(), new CompoundTag());
        ListTag structuresList = tag.getListOrEmpty(NBT_KEY);
        
        PlacedStructure structure = new PlacedStructure(structureId, boundingBox, placedBy, rotation);
        structuresList.add(structure.toNBT());
        
        tag.put(NBT_KEY, structuresList);
        multibuilderStack.set(MbtoolDataComponents.PLACED_STRUCTURES.get(), tag);
    }
    
    /**
     * Get all placed structures from the multibuilder tool's Data Components
     */
    public static List<PlacedStructure> getPlacedStructures(ItemStack multibuilderStack) {
        List<PlacedStructure> structures = new ArrayList<>();
        CompoundTag tag = multibuilderStack.get(MbtoolDataComponents.PLACED_STRUCTURES.get());
        
        if (tag != null && tag.contains(NBT_KEY)) {
            ListTag structuresList = tag.getListOrEmpty(NBT_KEY);
            for (int i = 0; i < structuresList.size(); i++) {
                CompoundTag structureTag = structuresList.getCompound(i).orElse(new CompoundTag());
                structures.add(new PlacedStructure(structureTag));
            }
        }
        
        return structures;
    }
    
    /**
     * Find a placed structure that contains the given position
     */
    public static PlacedStructure findStructureAt(ItemStack multibuilderStack, BlockPos pos) {
        List<PlacedStructure> structures = getPlacedStructures(multibuilderStack);
        
        for (PlacedStructure structure : structures) {
            if (structure.contains(pos)) {
                return structure;
            }
        }
        
        return null;
    }
    
    /**
     * Remove a placed structure from the multibuilder tool's Data Components
     */
    public static boolean removePlacedStructure(ItemStack multibuilderStack, PlacedStructure structureToRemove) {
        CompoundTag tag = multibuilderStack.get(MbtoolDataComponents.PLACED_STRUCTURES.get());
        
        if (tag == null || !tag.contains(NBT_KEY)) {
            return false;
        }
        
        ListTag structuresList = tag.getListOrEmpty(NBT_KEY);
        ListTag newStructuresList = new ListTag();
        boolean removed = false;
        
        for (int i = 0; i < structuresList.size(); i++) {
            CompoundTag structureTag = structuresList.getCompound(i).orElse(new CompoundTag());
            PlacedStructure structure = new PlacedStructure(structureTag);
            
            // Compare by bounding box and structure ID
            if (!structure.getBoundingBox().equals(structureToRemove.getBoundingBox()) ||
                !structure.getStructureId().equals(structureToRemove.getStructureId())) {
                newStructuresList.add(structureTag);
            } else {
                removed = true;
            }
        }
        
        tag.put(NBT_KEY, newStructuresList);
        multibuilderStack.set(MbtoolDataComponents.PLACED_STRUCTURES.get(), tag);
        return removed;
    }
    
    /**
     * Clear all placed structures from the multibuilder tool's Data Components
     */
    public static void clearPlacedStructures(ItemStack multibuilderStack) {
        multibuilderStack.remove(MbtoolDataComponents.PLACED_STRUCTURES.get());
    }
}