package igentuman.mbtool.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static igentuman.mbtool.Mbtool.rlFromString;
import static net.minecraft.world.level.block.Blocks.AIR;
import static net.minecraft.world.level.block.state.StateHolder.PROPERTIES_TAG;

public class MultiblockStructure {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private CompoundTag nbt;
    private String name;
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private ResourceLocation location;
    private String group = "default";
    private boolean isGroup = false;
    private List<MultiblockStructure> groupStructures = new ArrayList<>();

    public MultiblockStructure(String groupName) {
        this.group = groupName;
        this.isGroup = true;
        this.name = groupName;
    }

    public MultiblockStructure(CompoundTag nbt) {
        this.nbt = nbt;
        if (nbt.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocksList = nbt.getList("blocks", Tag.TAG_COMPOUND);
            ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);

            for (int i = 0; i < blocksList.size(); i++) {
                CompoundTag blockTag = blocksList.getCompound(i);
                CompoundTag state = palette.getCompound(blockTag.getInt("state"));
                if (blockTag.get("pos") instanceof ListTag posList && state.getString("Name") != null) {

                    if (posList.size() == 3) {
                        int x = posList.getInt(0);
                        int y = posList.getInt(1);
                        int z = posList.getInt(2);
                        
                        BlockPos pos = new BlockPos(x, y, z);
                        String blockId = state.getString("Name");
                        Block block = ForgeRegistries.BLOCKS.getValue(rlFromString(blockId));
                        
                        if (block != null) {
                            BlockState bs = block.defaultBlockState();
                            
                            // Handle block state properties if they exist
                            if (state.contains(PROPERTIES_TAG, Tag.TAG_COMPOUND)) {
                                CompoundTag properties = state.getCompound("Properties");
                                for(String pKey: state.getCompound("Properties").getAllKeys()) {
                                    for (net.minecraft.world.level.block.state.properties.Property<?> property : bs.getProperties()) {
                                        if (property.getName().equals(pKey)) {
                                            // Parse the string value to the appropriate property value
                                            String valueStr = properties.getString(pKey);
                                            Optional<?> value = property.getValue(valueStr);

                                            if (value.isPresent()) {
                                                bs = setPropertyValue(bs, property, value.get());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            blocks.put(pos, bs);
                            
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            minZ = Math.min(minZ, z);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                            maxZ = Math.max(maxZ, z);
                        }
                    }
                }
            }
        }
    }

    public MultiblockStructure(ResourceLocation rl, CompoundTag nbt, String file, String group) {
        this(nbt);
        location = rl;
        name = file;
        this.group = group;
    }

    @SuppressWarnings("unchecked")
    private static <S extends BlockState, T extends Comparable<T>> S setPropertyValue(S blockState,
          net.minecraft.world.level.block.state.properties.Property<T> property, Object value) {
        return (S) blockState.setValue(property, (T) value);
    }

    public BlockState getBlockAt(BlockPos pos) {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getBlockAt(pos) : null;
        }
        return blocks.get(pos);
    }
    
    public Map<BlockPos, BlockState> getBlocks() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getBlocks() : blocks;
        }
        return blocks;
    }
    
    public int getWidth() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getWidth() : 0;
        }
        return maxX - minX + 1;
    }
    
    public int getHeight() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getHeight() : 0;
        }
        return maxY - minY + 1;
    }
    
    public int getDepth() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getDepth() : 0;
        }
        return maxZ - minZ + 1;
    }
    
    public BlockPos getCenter() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getCenter() : BlockPos.ZERO;
        }
        return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }
    
    public int getMinX() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getMinX() : minX;
        }
        return minX;
    }
    public int getMinY() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getMinY() : minY;
        }
        return minY;
    }
    public int getMinZ() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getMinZ() : minZ;
        }
        return minZ;
    }
    public int getMaxX() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getMaxX() : maxX;
        }
        return maxX;
    }
    public int getMaxY() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getMaxY() : maxY;
        }
        return maxY;
    }
    public int getMaxZ() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getMaxZ() : maxZ;
        }
        return maxZ;
    }

    public ResourceLocation getId() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getId() : location;
        }
        return location;
    }

    public String getName() {
        if(name != null && !name.isEmpty()) {
            if(!name.contains("mbtool")) {
                return "mbtool.structure." + name.replace(".nbt", "");
            }
            return name.replace(".nbt", "");
        }
        return name;
    }

    public CompoundTag getStructureNbt() {
        if (isGroup) {
            MultiblockStructure rep = getRepresentativeStructure();
            return rep != null ? rep.getStructureNbt() : nbt;
        }
        return nbt;
    }

    public void setStructureNbt(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    public void addGroupStructure(MultiblockStructure s) {
        this.groupStructures.add(s);
    }

    public List<MultiblockStructure> getGroupStructures() {
        return groupStructures;
    }

    public MultiblockStructure getRepresentativeStructure() {
        if (groupStructures == null || groupStructures.isEmpty()) {
            return null;
        }
        return groupStructures.stream()
                .min(Comparator.comparing(s -> s.getName() != null ? s.getName() : ""))
                .orElse(null);
    }

    public List<ItemStack> getNeededItems() {
        if (isGroup) {
            return new ArrayList<>();
        }
        List<ItemStack> outputs = new ArrayList<>();
        List<Block> blockTypes = new ArrayList<>();
        for(BlockPos pos : getBlocks().keySet()) {
            if(getBlocks().get(pos).is(AIR)) {
                continue;
            }
            Block block = getBlocks().get(pos).getBlock();
            if (!blockTypes.contains(block)) {
                blockTypes.add(block);
                outputs.add(new ItemStack(block));
            }
        }
        for(ItemStack stackItem :outputs) {
            for(Map.Entry<BlockPos, BlockState> block : getBlocks().entrySet()) {
                if(stackItem.is(block.getValue().getBlock().asItem())) {
                    stackItem.setCount(stackItem.getCount() + 1);
                }
            }
            stackItem.setCount(stackItem.getCount() - 1);
        }
        return outputs;
    }

    /**
     * Filters out all air blocks from the structure NBT.
     * This is useful for reducing packet size and ensuring air blocks are not placed.
     * 
     * @param nbt The structure NBT to filter
     * @return A new CompoundTag with air blocks removed
     */
    public static CompoundTag filterAirBlocks(CompoundTag nbt) {
        if (nbt == null || !nbt.contains("blocks", Tag.TAG_LIST) || !nbt.contains("palette", Tag.TAG_LIST)) {
            return nbt;
        }

        CompoundTag filteredNbt = nbt.copy();
        ListTag blocksList = nbt.getList("blocks", Tag.TAG_COMPOUND);
        ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        
        // Find air block indices in the palette
        Set<Integer> airIndices = new HashSet<>();
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag paletteEntry = palette.getCompound(i);
            String blockName = paletteEntry.getString("Name");
            if (blockName != null && (blockName.equals("minecraft:air") || 
                                      blockName.equals("minecraft:cave_air") || 
                                      blockName.equals("minecraft:void_air"))) {
                airIndices.add(i);
            }
        }
        
        // Filter out blocks that reference air palette entries
        ListTag filteredBlocksList = new ListTag();
        for (int i = 0; i < blocksList.size(); i++) {
            CompoundTag blockTag = blocksList.getCompound(i);
            int stateIndex = blockTag.getInt("state");
            if (!airIndices.contains(stateIndex)) {
                filteredBlocksList.add(blockTag);
            }
        }
        
        filteredNbt.put("blocks", filteredBlocksList);
        return filteredNbt;
    }
}