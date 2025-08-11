package igentuman.mbtool.integration.nc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static igentuman.mbtool.Mbtool.rl;

public class ReactorDesignParser {

    public static String resolvePath(String input) throws Exception {
        if (input.startsWith("file://")) {
            return Paths.get(new URI(input)).toAbsolutePath().toString();
        } else {
            return Paths.get(input).toAbsolutePath().toString();
        }
    }
    
    private static JsonElement loadFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set request properties
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(30000); // 30 seconds
        connection.setRequestProperty("User-Agent", "MultiBuilderTool/1.0");
        
        // Check response code
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + responseCode + ": " + connection.getResponseMessage());
        }
        
        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
        }
        
        connection.disconnect();
        
        // Parse and return JSON
        return JsonParser.parseString(response.toString());
    }

    public static MultiblockStructure parseNuclearCraftReactorDesign(String input) {
        JsonElement jsonElement = null;
        
        // Try to load from URL first
        if (input.startsWith("http://") || input.startsWith("https://")) {
            try {
                jsonElement = loadFromUrl(input);
            } catch (Exception e) {
                System.err.println("Failed to load from URL: " + e.getMessage());
            }
        } else {
            // Try to load from file path
            String resolvedPath = input;
            
            try {
                resolvedPath = resolvePath(input);
            } catch (Exception ignored) {
            }
            
            try {
                jsonElement = JsonParser.parseReader(new FileReader(resolvedPath));
            } catch (FileNotFoundException ignored) {
            }
        }
        
        try {
            if(jsonElement == null) {
                jsonElement = JsonParser.parseString(input);
            }
            
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (!jsonObject.has("CompressedReactor")) {
                return null;
            }

            JsonObject compressedReactor = jsonObject.getAsJsonObject("CompressedReactor");

            // Create NBT structure for MultiblockStructure
            CompoundTag nbt = new CompoundTag();
            ListTag blocksList = new ListTag();
            ListTag palette = new ListTag();
            Map<String, Integer> paletteMap = new HashMap<>();
            AtomicInteger paletteIndex = new AtomicInteger(0);

            // First, find the bounds of the reactor structure and check for existing casing
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            boolean hasCasing = false;

            // Parse each component type and its positions to find bounds and check for casing
            for (Map.Entry<String, JsonElement> entry : compressedReactor.entrySet()) {
                String componentType = entry.getKey();
                JsonElement positionsElement = entry.getValue();

                if (!positionsElement.isJsonArray()) continue;

                // Check if this component type is casing
                if (componentType.toLowerCase().contains("casing") || componentType.toLowerCase().contains("glass")) {
                    //hasCasing = true;
                }

                for (JsonElement posObj : positionsElement.getAsJsonArray()) {
                    JsonObject positionObject = posObj.getAsJsonObject();
                    int x = positionObject.get("X").getAsInt();
                    int y = positionObject.get("Y").getAsInt();
                    int z = positionObject.get("Z").getAsInt();

                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }
            }

            // Only add reactor casing and glass wrapper if structure doesn't already have casing
            if (!hasCasing) {
                // Expand bounds by 1 block in each direction for the casing
                minX--; maxX++;
                minY--; maxY++;
                minZ--; maxZ++;

                // Add reactor casing and glass wrapper
                addReactorWrapper(blocksList, palette, paletteMap, paletteIndex, minX, maxX, minY, maxY, minZ, maxZ);
            }

            // Parse each component type and its positions (original reactor components)
            for (Map.Entry<String, JsonElement> entry : compressedReactor.entrySet()) {
                String componentType = entry.getKey();
                JsonElement positionsElement = entry.getValue();

                if (!positionsElement.isJsonArray()) continue;

                for (JsonElement posObj : positionsElement.getAsJsonArray()) {
                    JsonObject positionObject = posObj.getAsJsonObject();
                    int x = positionObject.get("X").getAsInt();
                    int y = positionObject.get("Y").getAsInt();
                    int z = positionObject.get("Z").getAsInt();

                    String blockName = getBlockNameFromComponent(componentType.toLowerCase());
                    addBlockToStructure(blocksList, palette, paletteMap, x, y, z, blockName, paletteIndex);
                }
            }

            nbt.put("blocks", blocksList);
            nbt.put("palette", palette);

            return new MultiblockStructure(rl("runtime_reactor"), nbt, "nuclearcraft_reactor");

        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON syntax in reactor design: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing reactor design: " + e.getMessage());
            return null;
        }
    }


    private static String getBlockNameFromComponent(String componentType) {
        // Map NuclearCraft component types to block names
        switch (componentType) {
            case "transparentcasing":
                return "nuclearcraft:fission_reactor_glass";
            case "fuelcell":
                return "nuclearcraft:fission_reactor_solid_fuel_cell";
            case "water":
                return "nuclearcraft:water_heat_sink";
            case "redstone":
                return "nuclearcraft:redstone_heat_sink";
            case "quartz":
                return "nuclearcraft:quartz_heat_sink";
            case "obsidian":
                return "nuclearcraft:obsidian_heat_sink";
            case "nether_brick":
                return "nuclearcraft:nether_brick_heat_sink";
            case "glowstone":
                return "nuclearcraft:glowstone_heat_sink";
            case "lapis":
                return "nuclearcraft:lapis_heat_sink";
            case "gold":
                return "nuclearcraft:gold_heat_sink";
            case "prismarine":
                return "nuclearcraft:prismarine_heat_sink";
            case "slime":
                return "nuclearcraft:slime_heat_sink";
            case "end_stone":
                return "nuclearcraft:end_stone_heat_sink";
            case "purpur":
                return "nuclearcraft:purpur_heat_sink";
            case "diamond":
                return "nuclearcraft:diamond_heat_sink";
            case "emerald":
                return "nuclearcraft:emerald_heat_sink";
            case "copper":
                return "nuclearcraft:copper_heat_sink";
            case "tin":
                return "nuclearcraft:tin_heat_sink";
            case "lead":
                return "nuclearcraft:lead_heat_sink";
            case "boron":
                return "nuclearcraft:boron_heat_sink";
            case "lithium":
                return "nuclearcraft:lithium_heat_sink";
            case "magnesium":
                return "nuclearcraft:magnesium_heat_sink";
            case "manganese":
                return "nuclearcraft:manganese_heat_sink";
            case "aluminum":
                return "nuclearcraft:aluminum_heat_sink";
            case "silver":
                return "nuclearcraft:silver_heat_sink";
            case "fluorite":
                return "nuclearcraft:fluorite_heat_sink";
            case "villiaumite":
                return "nuclearcraft:villiaumite_heat_sink";
            case "carobbiite":
                return "nuclearcraft:carobbiite_heat_sink";
            case "arsenic":
                return "nuclearcraft:arsenic_heat_sink";
            case "liquid_helium":
                return "nuclearcraft:liquid_helium_heat_sink";
            case "liquid_nitrogen":
                return "nuclearcraft:liquid_nitrogen_heat_sink";
            case "liquid_neon":
                return "nuclearcraft:liquid_neon_heat_sink";
            case "liquid_argon":
                return "nuclearcraft:liquid_argon_heat_sink";
            case "liquid_krypton":
                return "nuclearcraft:liquid_krypton_heat_sink";
            case "liquid_xenon":
                return "nuclearcraft:liquid_xenon_heat_sink";
            case "liquid_radon":
                return "nuclearcraft:liquid_radon_heat_sink";
            case "enderium":
                return "nuclearcraft:enderium_heat_sink";
            case "cryotheum":
                return "nuclearcraft:cryotheum_heat_sink";
            case "iron":
                return "nuclearcraft:iron_heat_sink";
            case "moderator":
                return "nuclearcraft:fission_reactor_moderator";
            case "reflector":
                return "nuclearcraft:fission_reactor_reflector";
            default:
                return "minecraft:air";
        }
    }

    private static void addReactorWrapper(ListTag blocksList, ListTag palette, Map<String, Integer> paletteMap,
                                          AtomicInteger paletteIndex, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Add reactor casing at corners and glass on walls
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Check if this position is on the boundary
                    boolean isOnBoundary = (x == minX || x == maxX) || (y == minY || y == maxY) || (z == minZ || z == maxZ);
                    
                    if (isOnBoundary) {
                        // Check if this is a corner (intersection of 3 faces) or edge (intersection of 2 faces)
                        int faceCount = 0;
                        if (x == minX || x == maxX) faceCount++;
                        if (y == minY || y == maxY) faceCount++;
                        if (z == minZ || z == maxZ) faceCount++;
                        
                        String blockName;
                        if (faceCount >= 2) {
                            // Corner or edge - use casing
                            blockName = "nuclearcraft:fission_reactor_casing";
                        } else {
                            // Wall center - use glass
                            blockName = "nuclearcraft:fission_reactor_glass";
                        }
                        
                        addBlockToStructure(blocksList, palette, paletteMap, x, y, z, blockName, paletteIndex);
                    }
                }
            }
        }
    }

    private static void addBlockToStructure(ListTag blocksList, ListTag palette, Map<String, Integer> paletteMap,
                                     int x, int y, int z, String blockName, AtomicInteger paletteIndex) {
        // Add to palette if new
        if (!paletteMap.containsKey(blockName)) {
            CompoundTag paletteEntry = new CompoundTag();
            paletteEntry.putString("Name", blockName);
            palette.add(paletteEntry);
            paletteMap.put(blockName, paletteIndex.getAndIncrement());
        }

        // Create block entry
        CompoundTag blockEntry = new CompoundTag();
        ListTag pos = new ListTag();
        pos.add(IntTag.valueOf(x));
        pos.add(IntTag.valueOf(y));
        pos.add(IntTag.valueOf(z));

        blockEntry.put("pos", pos);
        blockEntry.putInt("state", paletteMap.get(blockName));

        blocksList.add(blockEntry);
    }
}
