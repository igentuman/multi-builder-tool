package igentuman.mbtool.recipe;

import igentuman.mbtool.util.*;
import igentuman.mbtool.integration.jei.MbtoolRecipeCategory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockRecipe {
    private String name;



    private MbtoolRecipeCategory category;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private String label;

    private String[][][] variantMap;
    private String[][][] map;
    private String[][][] map90;
    private String[][][] map180;
    private String[][][] map270;
    private List<BlockPos> mapAsBlockPos;
    private Map<String, IBlockState> reference;
    private Map<String, Integer> referenceCount;
    private Map<String, Boolean> referenceIgnoresMeta;
    private Map<String, NBTTagCompound> referenceTags;
    private Map<String, ItemStack> referenceStacks;
    private ItemStack targetStack;
    private BlockPos minPos;
    private BlockPos maxPos;
    private int count;

    public void setTargetStack(ItemStack targetStack) {
        this.targetStack = targetStack;
    }

    public MultiblockRecipe(String name) {
        this.name = name;
        this.reference = new HashMap<>();
        this.referenceCount = new HashMap<>();
        this.referenceIgnoresMeta = new HashMap<>();
        this.referenceTags = new HashMap<>();
        this.referenceStacks = new HashMap<>();
    }

    public boolean isValid()
    {
        for(ItemStack itemStack: getRequiredItemStacks()) {
            if(itemStack == null) {
                return false;
            }
        }
        return true;
    }

    public void addBlockReference(String ref, IBlockState state) {
        this.reference.put(ref, state);
    }

    public void addBlockVariation(String ref, NBTTagCompound tag) {
        this.referenceTags.put(ref, tag);
    }

    public void setIgnoreMeta(String ref, boolean value) {
        this.referenceIgnoresMeta.put(ref, value);
    }

    /*public void setIgnoreNBT(String ref, boolean value) {
        this.referenceIgnoresNBT.put(ref, value);
    }*/

    public void setReferenceStack(String ref, ItemStack stack) {
        this.referenceStacks.put(ref, stack);
    }


    public String getName() {
        return name;
    }

    public List<ItemStack> getRequiredItemStacks() {
        List<ItemStack> result = new ArrayList<>();
        for(String ref : reference.keySet()) {
            IBlockState state = reference.get(ref);
            int count = referenceCount.getOrDefault(ref, 0);
            if(count == 0) {
                continue;
            }

            if(referenceStacks.containsKey(ref)) {
                result.add(referenceStacks.get(ref).copy());
            } else if(state.getBlock() == Blocks.REDSTONE_WIRE) {
                result.add(new ItemStack(Items.REDSTONE, count));
            } else {
                if(referenceIgnoresMeta.getOrDefault(ref, false)) {
                    result.add(new ItemStack(state.getBlock(), count, 0));
                } else {

                    //result.add(new ItemStack(state.getBlock(), count, 0));

                    result.add(new ItemStack(state.getBlock(), count, state.getBlock().getMetaFromState(state)));
                }
            }
        }

        return result;
    }

    public void setVariantMap(String[][][] variantMap) {
        this.variantMap = variantMap;
    }

    public void setPositionMap(String[][][] map) {
        this.map = map;

        // Count blocks, we can use this to quickly skip this recipe if the crafting
        // area is of a different size.
        this.mapAsBlockPos = new ArrayList<>();
        this.count = 0;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int maxX = Integer.MIN_VALUE;
        for(int y = 0; y < map.length; y++) {
            for(int z = 0; z < map[y].length; z++) {
                for(int x = 0; x < map[y][z].length; x++) {
                    String content = map[y][z][x];
                    if(content.equals("_")) {
                        continue;
                    }

                    referenceCount.merge(content, 1, Integer::sum);

                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    minX = Math.min(minX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                    maxX = Math.max(maxX, x);
                    mapAsBlockPos.add(new BlockPos(x, y, z));
                    this.count++;
                }
            }
        }

        this.minPos = new BlockPos(minX, minY, minZ);
        this.maxPos = new BlockPos(maxX, maxY, maxZ);
        this.levels = map.length;
    }

    private boolean testRotation(World world, List<BlockPos> insideBlocks, int minX, int minY, int minZ, String[][][] map) {
        // Perform a few tests for each of the inside blocks
        for(BlockPos pos : insideBlocks) {
            BlockPos relativePos = pos.add(-minX, -minY, -minZ);
            int y = relativePos.getY();
            int z = relativePos.getZ();
            int x = relativePos.getX();

            // Test whether the position is outside of the recipe
            if(y < 0 || y >= map.length) {
                return false;
            }
            if(z < 0 || z >= map[y].length) {
                return false;
            }
            if(x < 0 || x >= map[y][z].length) {
                return false;
            }

            // Ignore "_", i.e. air blocks. These are air.
            IBlockState state = world.getBlockState(pos);
            if(map[map.length - y - 1][z][x].equals("_") && state.getBlock().isAir(state, world, pos)) {
                continue;
            }

            // Test whether the block is the type it should be
            IBlockState wanted = reference.get(map[y][z][x]);
            if(wanted == null || state.getBlock() != wanted.getBlock()) {
                return false;
            }

            if(!referenceIgnoresMeta.getOrDefault(map[y][z][x], false)) {
                if(state.getBlock().getMetaFromState(state) != wanted.getBlock().getMetaFromState(wanted)) {
                    return false;
                }
            }

        }

        return true;
    }

    private String[][][] rotateMapCW(String[][][] map) {
        String[][][] ret = new String[map.length][][];
        for(int y = 0; y < map.length; y++) {
            final int M = map[y].length;
            final int N = map[y][0].length;
            String[][] slice = new String[N][M];
            for (int r = 0; r < M; r++) {
                for (int c = 0; c < N; c++) {
                    slice[c][M - 1 - r] = map[y][r][c];
                }
            }
            ret[y] = slice;
        }

        return ret;

    }

    public BlockPos getMinPos() {
        return minPos;
    }

    public BlockPos getMaxPos() {
        return maxPos;
    }

    public String getDimensionsString() {
        return String.format("%dx%dx%d", getWidth(), getHeight(), getDepth());
    }

    public int getWidth() {
        return getMaxPos().getX() - getMinPos().getX() +1;
    }

    public int getHeight() {
        return getMaxPos().getY() - getMinPos().getY() +1;
    }

    public int getDepth() {
        return getMaxPos().getZ() - getMinPos().getZ() +1;
    }

    public ItemStack getTargetStack() {
        return targetStack;
    }

    public int levels;
    public void setLevels(int levels)
    {
        this.levels = levels;
    }

    public IBlockState getStateAtBlockPos(BlockPos pos) {
        if(pos.getY() < 0 || pos.getY() >= levels) {
            return Blocks.AIR.getDefaultState();
        }
        if(pos.getZ() < 0 || pos.getZ() >= this.map[pos.getY()].length) {
            return Blocks.AIR.getDefaultState();
        }
        if(pos.getX() < 0 || pos.getX() >= this.map[pos.getY()][pos.getZ()].length) {
            return Blocks.AIR.getDefaultState();
        }

        String ref = this.map[pos.getY()][pos.getZ()][pos.getX()];
        return reference.getOrDefault(ref, Blocks.AIR.getDefaultState());
    }
    public IBlockAccess getBlockAccess(ProxyWorld proxyWorld) {
        return new IBlockAccess() {
            @Nullable
            @Override
            public TileEntity getTileEntity(BlockPos pos) {
                IBlockState state = getBlockState(pos);
                if(state.getBlock().hasTileEntity(state)) {
                    TileEntity tileentity = state.getBlock().createTileEntity(proxyWorld, state);
                    if (tileentity != null) {
                        tileentity.setWorld(proxyWorld);
                        NBTTagCompound nbt = getVariantAtBlockPos(pos);
                        if(nbt != null) {
                            tileentity.readFromNBT(nbt);
                            for (AbstractExtraTileDataProvider provider : ExtraTileDataProviderRegistry.getDataProviders(tileentity)) {
                                String tagName = String.format("cm3_extra:%s", provider.getName());
                                if (nbt.hasKey(tagName)) {
                                    provider.readExtraData(tileentity, (NBTTagCompound) nbt.getTag(tagName));
                                }
                            }
                        }
                    }

                    return tileentity;
                }

                return null;
            }

            @Override
            public int getCombinedLight(BlockPos pos, int lightValue) {
                return 255;
            }

            @Override
            public IBlockState getBlockState(BlockPos pos) {
                return getStateAtBlockPos(pos);
            }

            @Override
            public boolean isAirBlock(BlockPos pos) {
                IBlockState blockState = this.getBlockState(pos);
                return blockState.getBlock().isAir(blockState, this, pos);
            }

            @Override
            public Biome getBiome(BlockPos pos) {
                return Biomes.PLAINS;
            }

            @Override
            public int getStrongPower(BlockPos pos, EnumFacing direction) {
                return 0;
            }

            @Override
            public WorldType getWorldType() {
                return WorldType.FLAT;
            }

            @Override
            public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
                return this.getBlockState(pos).isSideSolid(this, pos, side);
            }
        };
    }

    public NBTTagCompound getVariantAtBlockPos(BlockPos pos) {
        if(pos.getY() < 0 || pos.getY() >= this.map.length) {
            return null;
        }
        if(pos.getZ() < 0 || pos.getZ() >= this.map[pos.getY()].length) {
            return null;
        }
        if(pos.getX() < 0 || pos.getX() >= this.map[pos.getY()][pos.getZ()].length) {
            return null;
        }

        String variant = this.variantMap[pos.getY()][pos.getZ()][pos.getX()];
        return this.referenceTags.getOrDefault(variant, null);
    }

    public List<BlockPos> getShapeAsBlockPosList() {
        return mapAsBlockPos;
    }

    public void setCategory(MbtoolRecipeCategory cat)
    {
        category = cat;
    }

    public MbtoolRecipeCategory getCategory() {
        return category;
    }
}
