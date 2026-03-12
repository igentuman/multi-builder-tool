package igentuman.mbtool.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static igentuman.mbtool.Mbtool.rlFromString;
import static igentuman.mbtool.util.ModUtil.isGtLoaded;
import static igentuman.mbtool.util.ModUtil.isKubeJsLoaded;

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
            InitMbtoolStructuresEvent event = new InitMbtoolStructuresEvent(loadedStructures);
            NeoForge.EVENT_BUS.post(event);
/*            if(isKubeJsLoaded()) {
                MbtoolKubeJsEvents.onInitMbtoolStructures(event);
            }*/
            List<MultiblockStructure> structuresToAdd = new ArrayList<>();
            for (MultiblockStructure structure : event.structures) {
                if(!validateStructureBlocks(structure.getStructureNbt())) {
                    System.out.println("Structure " + structure.getName() + " contains invalid blocks and will be skipped.");
                } else {
                    structuresToAdd.add(structure);
                }
            }
            structures.clear();
            structures.addAll(structuresToAdd);
        }, gameExecutor);
    }

    private static List<MultiblockStructure> loadMultiblockStructures(ResourceManager resourceManager) {
        List<MultiblockStructure> loadedStructures = new ArrayList<>();
        loadedStructures.addAll(loadFromLocation(resourceManager, "mbtool_structures"));
        loadedStructures.addAll(loadFromLocation(resourceManager, "spatial_structures"));
        if(isGtLoaded()) {
            //loadGtStructures(loadedStructures); //todo fix BERs rendering, and get this back
        }
        /*if(isMMLoaded() && MbtoolConfig.AUTOMATICALLY_ADD_MM_STRUCTURES.get()) {
            MMUtil.loadMMStructures(loadedStructures);
        }*/
        return loadedStructures;
    }

    private static List<MultiblockStructure> loadFromLocation(ResourceManager resourceManager, String mbtoolStructures) {
        List<MultiblockStructure> tmp = new ArrayList<>();
        // Get all .nbt files from the structures directory
        Map<ResourceLocation, Resource> structureFiles = resourceManager.listResources(mbtoolStructures,
                location -> location.getPath().endsWith(".nbt"));

        for (Map.Entry<ResourceLocation, Resource> entry : structureFiles.entrySet()) {
            ResourceLocation location = entry.getKey();
            Resource resource = entry.getValue();

            try {
                CompoundTag nbt = NbtIo.readCompressed(resource.open(), NbtAccounter.unlimitedHeap());

                // Validate that all blocks in the structure exist
                if (validateStructureBlocks(nbt)) {
                    String fileName = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
                    tmp.add(new MultiblockStructure(location, nbt, fileName));
                } else {
                    System.out.println("Skipping structure " + location + " due to missing blocks");
                }

            } catch (IOException e) {
                System.err.println("Failed to load structure from " + location + ": " + e.getMessage());
            }
        }

        return tmp;
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
                if (!BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
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
