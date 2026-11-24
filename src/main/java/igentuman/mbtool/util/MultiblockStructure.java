package igentuman.mbtool.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static igentuman.mbtool.Mbtool.rlFromString;
import static net.minecraft.world.level.block.Blocks.AIR;

public class MultiblockStructure {
    public static final String DEFAULT_GROUP = "default";
    private static final String GROUP_TAG = "StructureGroup";
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private CompoundTag nbt;
    private String name;
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private Identifier location;

    public MultiblockStructure(CompoundTag nbt) {
        this.nbt = nbt;
        nbt.getString("StructureName").ifPresent(n -> name = n);
        nbt.getString("StructureLocation").ifPresent(l -> location = Identifier.tryParse(l));
        if (nbt.contains("blocks")) {
            ListTag blocksList = nbt.getListOrEmpty("blocks");
            ListTag palette = nbt.getListOrEmpty("palette");

            for (int i = 0; i < blocksList.size(); i++) {
                CompoundTag blockTag = blocksList.getCompoundOrEmpty(i);
                int stateIdx = blockTag.getIntOr("state", 0);
                CompoundTag state = palette.getCompoundOrEmpty(stateIdx);
                Tag posTag = blockTag.get("pos");
                int[] posArr = null;
                if (posTag instanceof ListTag posList && posList.size() == 3) {
                    posArr = new int[]{posList.getIntOr(0, 0), posList.getIntOr(1, 0), posList.getIntOr(2, 0)};
                } else if (posTag instanceof IntArrayTag intArrayTag && intArrayTag.getAsIntArray().length == 3) {
                    posArr = intArrayTag.getAsIntArray();
                }
                if (posArr != null && state.getString("Name").isPresent()) {
                    {
                        int x = posArr[0];
                        int y = posArr[1];
                        int z = posArr[2];

                        BlockPos pos = new BlockPos(x, y, z);
                        String blockId = state.getStringOr("Name", "");
                        Identifier rl = rlFromString(blockId);
                        if (rl != null && BuiltInRegistries.BLOCK.getOptional(rl).isPresent()) {
                            Block block = BuiltInRegistries.BLOCK.getOptional(rl).get();
                            BlockState bs = block.defaultBlockState();

                            if (state.contains("Properties")) {
                                CompoundTag properties = state.getCompoundOrEmpty("Properties");
                                for (String pKey : properties.keySet()) {
                                    for (net.minecraft.world.level.block.state.properties.Property<?> property : bs.getProperties()) {
                                        if (property.getName().equals(pKey)) {
                                            String valueStr = properties.getStringOr(pKey, "");
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

    public MultiblockStructure(Identifier rl, CompoundTag nbt, String file) {
        this(nbt);
        location = rl;
        name = file;
        nbt.putString("StructureName", file);
        if (rl != null) {
            nbt.putString("StructureLocation", rl.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static <S extends BlockState, T extends Comparable<T>> S setPropertyValue(S blockState,
          net.minecraft.world.level.block.state.properties.Property<T> property, Object value) {
        return (S) blockState.setValue(property, (T) value);
    }

    public BlockState getBlockAt(BlockPos pos) {
        return blocks.get(pos);
    }

    public Map<BlockPos, BlockState> getBlocks() {
        return blocks;
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getHeight() {
        return maxY - minY + 1;
    }

    public int getDepth() {
        return maxZ - minZ + 1;
    }

    public BlockPos getCenter() {
        return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public Identifier getId() {
        return location;
    }

    public String getName() {
        if (name != null && !name.isEmpty()) {
            if (!name.contains("mbtool")) {
                return "mbtool.structure." + name.replace(".nbt", "");
            }
            return name.replace(".nbt", "");
        }
        return name;
    }

    public String getGroup() {
        if (nbt != null) {
            String group = nbt.getStringOr(GROUP_TAG, "");
            if (!group.isBlank()) return group;
        }
        if (location != null) {
            String[] parts = location.getPath().split("/");
            if (parts.length > 2 && !parts[1].isBlank()) return parts[1];
        }
        return DEFAULT_GROUP;
    }

    public void setGroup(String group) {
        if (nbt == null) return;
        if (group == null || group.isBlank()) {
            nbt.remove(GROUP_TAG);
        } else {
            nbt.putString(GROUP_TAG, group);
        }
    }

    public CompoundTag getStructureNbt() {
        return nbt;
    }

    public void setStructureNbt(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public List<ItemStack> getNeededItems() {
        List<ItemStack> outputs = new ArrayList<>();
        List<Block> blockTypes = new ArrayList<>();
        for (BlockPos pos : getBlocks().keySet()) {
            if (getBlocks().get(pos).is(AIR)) {
                continue;
            }
            Block block = getBlocks().get(pos).getBlock();
            if (!blockTypes.contains(block)) {
                blockTypes.add(block);
                outputs.add(new ItemStack(block));
            }
        }
        for (ItemStack stackItem : outputs) {
            for (Map.Entry<BlockPos, BlockState> block : getBlocks().entrySet()) {
                if (stackItem.is(block.getValue().getBlock().asItem())) {
                    stackItem.setCount(stackItem.getCount() + 1);
                }
            }
            stackItem.setCount(stackItem.getCount() - 1);
        }
        return outputs;
    }

    /**
     * Filters out all air blocks from the structure NBT to reduce packet size.
     */
    public static CompoundTag filterAirBlocks(CompoundTag nbt) {
        if (nbt == null || !nbt.contains("blocks") || !nbt.contains("palette")) {
            return nbt;
        }

        CompoundTag filteredNbt = nbt.copy();
        ListTag blocksList = nbt.getListOrEmpty("blocks");
        ListTag palette = nbt.getListOrEmpty("palette");

        Set<Integer> airIndices = new HashSet<>();
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag paletteEntry = palette.getCompoundOrEmpty(i);
            String blockName = paletteEntry.getStringOr("Name", "");
            if (!blockName.isEmpty() && (blockName.equals("minecraft:air") ||
                                          blockName.equals("minecraft:cave_air") ||
                                          blockName.equals("minecraft:void_air"))) {
                airIndices.add(i);
            }
        }

        ListTag filteredBlocksList = new ListTag();
        for (int i = 0; i < blocksList.size(); i++) {
            CompoundTag blockTag = blocksList.getCompoundOrEmpty(i);
            int stateIndex = blockTag.getIntOr("state", 0);
            if (!airIndices.contains(stateIndex)) {
                filteredBlocksList.add(blockTag);
            }
        }

        filteredNbt.put("blocks", filteredBlocksList);
        return filteredNbt;
    }
}
