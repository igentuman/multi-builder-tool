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
        this.structureId = tag.getStringOr("structureId", "");
        this.boundingBox = new AABB(
            tag.getIntOr("minX", 0), tag.getIntOr("minY", 0), tag.getIntOr("minZ", 0),
            tag.getIntOr("maxX", 0), tag.getIntOr("maxY", 0), tag.getIntOr("maxZ", 0)
        );
        this.placedBy = UUID.fromString(tag.getStringOr("placedBy", new UUID(0,0).toString()));
        this.placedTime = tag.getLongOr("placedTime", 0L);
        this.rotation = tag.getIntOr("rotation", 0);
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
        tag.putString("placedBy", placedBy.toString());
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