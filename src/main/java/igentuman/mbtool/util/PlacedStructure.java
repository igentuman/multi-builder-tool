package igentuman.mbtool.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Represents a placed multiblock structure with its ID and bounding box
 */
public class PlacedStructure {
    private final String structureId;
    private final AABB boundingBox;
    private final UUID placedBy;
    private final long placedTime;
    private final int rotation;
    
    public PlacedStructure(String structureId, AABB boundingBox, UUID placedBy, int rotation) {
        this.structureId = structureId;
        this.boundingBox = boundingBox;
        this.placedBy = placedBy;
        this.placedTime = System.currentTimeMillis();
        this.rotation = rotation;
    }
    
    public PlacedStructure(CompoundTag tag) {
        this.structureId = tag.getString("structureId");
        this.boundingBox = new AABB(
            tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"),
            tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ")
        );
        this.placedBy = tag.getUUID("placedBy");
        this.placedTime = tag.getLong("placedTime");
        this.rotation = tag.getInt("rotation");
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("structureId", structureId);
        tag.putInt("minX", (int) boundingBox.minX);
        tag.putInt("minY", (int) boundingBox.minY);
        tag.putInt("minZ", (int) boundingBox.minZ);
        tag.putInt("maxX", (int) boundingBox.maxX);
        tag.putInt("maxY", (int) boundingBox.maxY);
        tag.putInt("maxZ", (int) boundingBox.maxZ);
        tag.putUUID("placedBy", placedBy);
        tag.putLong("placedTime", placedTime);
        tag.putInt("rotation", rotation);
        return tag;
    }
    
    public String getStructureId() {
        return structureId;
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public UUID getPlacedBy() {
        return placedBy;
    }
    
    public long getPlacedTime() {
        return placedTime;
    }
    
    public int getRotation() {
        return rotation;
    }
    
    public boolean contains(BlockPos pos) {
        return boundingBox.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}