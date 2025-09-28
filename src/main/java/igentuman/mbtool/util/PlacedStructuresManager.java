package igentuman.mbtool.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
     * Add a placed structure to the multibuilder tool's NBT data
     */
    public static void addPlacedStructure(ItemStack multibuilderStack, String structureId, AABB boundingBox, UUID placedBy, int rotation) {
        if(structureId == null) return;
        CompoundTag tag = multibuilderStack.getOrCreateTag();
        ListTag structuresList = tag.getList(NBT_KEY, Tag.TAG_COMPOUND);
        
        PlacedStructure structure = new PlacedStructure(structureId, boundingBox, placedBy, rotation);
        structuresList.add(structure.toNBT());
        
        tag.put(NBT_KEY, structuresList);
    }
    
    /**
     * Get all placed structures from the multibuilder tool's NBT data
     */
    public static List<PlacedStructure> getPlacedStructures(ItemStack multibuilderStack) {
        List<PlacedStructure> structures = new ArrayList<>();
        CompoundTag tag = multibuilderStack.getOrCreateTag();
        
        if (tag.contains(NBT_KEY)) {
            ListTag structuresList = tag.getList(NBT_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < structuresList.size(); i++) {
                CompoundTag structureTag = structuresList.getCompound(i);
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
     * Remove a placed structure from the multibuilder tool's NBT data
     */
    public static boolean removePlacedStructure(ItemStack multibuilderStack, PlacedStructure structureToRemove) {
        CompoundTag tag = multibuilderStack.getOrCreateTag();
        
        if (!tag.contains(NBT_KEY)) {
            return false;
        }
        
        ListTag structuresList = tag.getList(NBT_KEY, Tag.TAG_COMPOUND);
        ListTag newStructuresList = new ListTag();
        boolean removed = false;
        
        for (int i = 0; i < structuresList.size(); i++) {
            CompoundTag structureTag = structuresList.getCompound(i);
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
        return removed;
    }
    
    /**
     * Clear all placed structures from the multibuilder tool's NBT data
     */
    public static void clearPlacedStructures(ItemStack multibuilderStack) {
        CompoundTag tag = multibuilderStack.getOrCreateTag();
        tag.remove(NBT_KEY);
    }
}