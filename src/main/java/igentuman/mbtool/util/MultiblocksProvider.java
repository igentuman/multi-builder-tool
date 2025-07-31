package igentuman.mbtool.util;

import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static igentuman.mbtool.Mbtool.rlFromString;

public class MultiblocksProvider implements PreparableReloadListener {

    public static List<MultiblockStructure> structures = new ArrayList<>();
    private static MultiblocksProvider INSTANCE = new MultiblocksProvider();

    public static MultiblocksProvider getInstance() {
        return INSTANCE;
    }

    public static List<MultiblockStructure> getStructures() {
        return structures;
    }
    
    /**
     * Sets the structures list. Used for client-side synchronization.
     * @param newStructures The new structures to set
     */
    public static void setStructures(List<MultiblockStructure> newStructures) {
        structures.clear();
        structures.addAll(newStructures);
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, 
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, 
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            return loadMultiblockStructures(resourceManager);
        }, backgroundExecutor).thenCompose(preparationBarrier::wait).thenAcceptAsync(loadedStructures -> {
            structures.clear();
            structures.addAll(loadedStructures);
        }, gameExecutor);
    }

    private static List<MultiblockStructure> loadMultiblockStructures(ResourceManager resourceManager) {
        List<MultiblockStructure> loadedStructures = new ArrayList<>();

        // Get all .nbt files from the structures directory
        Map<ResourceLocation, Resource> structureFiles = resourceManager.listResources("mbtool_structures",
                location -> location.getPath().endsWith(".nbt"));

        for (Map.Entry<ResourceLocation, Resource> entry : structureFiles.entrySet()) {
            ResourceLocation location = entry.getKey();
            Resource resource = entry.getValue();

            try {
                CompoundTag nbt = NbtIo.readCompressed(resource.open());

                // Validate that all blocks in the structure exist
                if (validateStructureBlocks(nbt)) {
                    String fileName = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
                    loadedStructures.add(new MultiblockStructure(location, nbt, fileName));
                } else {
                    System.out.println("Skipping structure " + location + " due to missing blocks");
                }

            } catch (IOException e) {
                System.err.println("Failed to load structure from " + location + ": " + e.getMessage());
            }
        }

        return loadedStructures;
    }

    /**
     * @deprecated Use getStructures() instead. This method is kept for backward compatibility.
     */
    @Deprecated
    public static List<MultiblockStructure> loadMultiblockStructures() {
        return getStructures();
    }

    /**
     * Validates that all blocks referenced in the structure NBT exist in the registry
     *
     * @param nbt The structure NBT data
     * @return true if all blocks exist, false otherwise
     */
    private static boolean validateStructureBlocks(CompoundTag nbt) {
        if (!nbt.contains("palette", Tag.TAG_LIST)) {
            return false; // No palette means no blocks to validate
        }

        ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);

        for (int i = 0; i < palette.size(); i++) {
            CompoundTag blockState = palette.getCompound(i);
            String blockId = blockState.getString("Name");

            if (blockId.isEmpty()) {
                continue; // Skip empty block names
            }

            try {
                ResourceLocation blockLocation = rlFromString(blockId);
                Block block = ForgeRegistries.BLOCKS.getValue(blockLocation);

                if (block == null || !blockLocation.getPath().equals(block.asItem().toString())) {
                    System.out.println("Missing block in structure: " + blockId);
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error validating block " + blockId + ": " + e.getMessage());
                return false;
            }
        }

        return true;
    }

}
