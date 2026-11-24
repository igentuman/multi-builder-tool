package igentuman.mbtool.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
    /** World positions that already held blocks at build time (ignored collisions); not owned by this structure. */
    private final Set<Long> sharedPositions = new HashSet<>();
    
    public PlacedStructure(String structureId, AABB boundingBox, UUID placedBy, int rotation) {
        this(structureId, boundingBox, placedBy, rotation, Set.of());
    }

    public PlacedStructure(String structureId, AABB boundingBox, UUID placedBy, int rotation, Collection<BlockPos> sharedPositions) {
        this.structureId = structureId;
        this.boundingBox = boundingBox;
        this.placedBy = placedBy;
        this.placedTime = System.currentTimeMillis();
        this.rotation = rotation;
        for (BlockPos pos : sharedPositions) this.sharedPositions.add(pos.asLong());
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
        tag.getLongArray("sharedPositions").ifPresent(positions -> {
            for (long pos : positions) this.sharedPositions.add(pos);
        });
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
        if (!sharedPositions.isEmpty()) {
            tag.putLongArray("sharedPositions", sharedPositions.stream().mapToLong(Long::longValue).toArray());
        }
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
    
    public boolean isShared(BlockPos pos) {
        return sharedPositions.contains(pos.asLong());
    }

    public boolean contains(BlockPos pos) {
        return boundingBox.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}