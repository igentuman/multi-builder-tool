package igentuman.mbtool.recipe;

import com.google.gson.*;
import igentuman.mbtool.Mbtool;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

public class MultiblockRecipeSerializer implements JsonSerializer<MultiblockRecipe>, JsonDeserializer<MultiblockRecipe> {
    public MultiblockRecipeSerializer() {
    }

    public MultiblockRecipe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            return null;
        } else {
            JsonObject jsonRoot = json.getAsJsonObject();
            if (jsonRoot.has("input-types") && jsonRoot.has("shape")) {
                if (!jsonRoot.has("name")) {
                    return null;
                } else {
                    String name = jsonRoot.get("name").getAsString();
                    String label = jsonRoot.get("label").getAsString();
                    boolean canRotate = true;
                    if(jsonRoot.has("allow-rotate") ) {
                        canRotate = jsonRoot.get("allow-rotate").getAsBoolean();
                    }
                    if (MultiblockRecipes.getRecipeByName(name) != null) {
                        return null;
                    } else if (jsonRoot.has("disabled") && jsonRoot.get("disabled").getAsBoolean()) {
                        return null;
                    } else {
                        ItemStack targetStack = ItemStack.EMPTY;
                        String nbtRaw;
                        if (jsonRoot.has("target-item")) {
                            nbtRaw = jsonRoot.get("target-item").getAsString();
                            Item targetItem = (Item)Item.REGISTRY.getObject(new ResourceLocation(nbtRaw));
                            if (targetItem != null) {
                                int meta = 0;
                                if(jsonRoot.has("target-item-meta")) {
                                    meta = jsonRoot.get("target-item-meta").getAsInt();
                                }
                                targetStack = new ItemStack(targetItem,1, meta);
                            }
                        }


                            MultiblockRecipe result = new MultiblockRecipe(name);
                            result.allowRotate = canRotate;
                                result.setTargetStack(targetStack);
                            JsonObject jsonReferenceMap = jsonRoot.get("input-types").getAsJsonObject();
                            Iterator var16 = jsonReferenceMap.entrySet().iterator();

                            int x;
                            while(var16.hasNext()) {
                                Map.Entry<String, JsonElement> entry = (Map.Entry)var16.next();
                                JsonObject data = ((JsonElement)entry.getValue()).getAsJsonObject();
                                if (!data.has("id")) {
                                    return null;
                                }

                                String blockId = data.get("id").getAsString();
                                ResourceLocation res = new ResourceLocation(blockId);
                                Block sourceBlock = (Block)Block.REGISTRY.getObject(res);
                                if (sourceBlock == null || !sourceBlock.getRegistryName().equals(res)) {
                                    if(Item.REGISTRY.getObject(res) == null || !Item.REGISTRY.getObject(res).getRegistryName().equals(res)) {
                                        return null;
                                    }
                                }



                                int meta = data.has("meta") ? data.get("meta").getAsInt() : 0;
                                IBlockState state = sourceBlock.getStateFromMeta(meta);
                                if (state == null) {
                                    state = sourceBlock.getDefaultState();
                                }
                                if(Mbtool.hooks.IC2Loaded) {
                                    if (blockId.equals("ic2:te")) {
                                        ic2.core.block.ITeBlock te = ic2.core.block.TeBlockRegistry.get(res, meta);
                                        state = ((ic2.core.block.BlockTileEntity) sourceBlock).getState(te);
                                    }
                                }

                                result.addBlockReference((String)entry.getKey(), state);
                                result.addMetaReference((String)entry.getKey(), meta);
                                boolean ignoreMeta = false;
                                if (data.has("ignore-meta")) {
                                    ignoreMeta = data.get("ignore-meta").getAsBoolean();
                                }

                                result.setIgnoreMeta((String)entry.getKey(), ignoreMeta);
                                if (data.has("item")) {
                                    JsonObject stackData = data.get("item").getAsJsonObject();
                                    if (stackData.has("id")) {
                                        String id = stackData.get("id").getAsString();
                                        Item stackItem = Item.getByNameOrId(id);
                                        if (stackItem != null) {
                                            int stackMeta = stackData.has("meta") ? stackData.get("meta").getAsInt() : 0;
                                            x = stackData.has("count") ? stackData.get("count").getAsInt() : 1;
                                            ItemStack stackStack = new ItemStack(stackItem, x, stackMeta);
                                            if (stackData.has("nbt")) {
                                                nbtRaw = stackData.get("nbt").getAsString();

                                                try {
                                                    NBTTagCompound stackNbt = JsonToNBT.getTagFromJson(nbtRaw);
                                                    stackStack.setTagCompound(stackNbt);
                                                } catch (NBTException var33) {
                                                }
                                            }

                                            result.setReferenceStack((String)entry.getKey(), stackStack);
                                        }
                                    }
                                }
                            }

                            if (jsonRoot.has("input-nbt")) {
                                JsonObject jsonVariantMap = jsonRoot.get("input-nbt").getAsJsonObject();
                                Iterator var44 = jsonVariantMap.entrySet().iterator();

                                while(var44.hasNext()) {
                                    Map.Entry<String, JsonElement> entry = (Map.Entry)var44.next();
                                    JsonObject data = ((JsonElement)entry.getValue()).getAsJsonObject();
                                    if (!data.has("nbt")) {
                                        return null;
                                    }

                                    String rawNbtJson = data.get("nbt").getAsString();

                                    try {
                                        NBTTagCompound variantNBT = JsonToNBT.getTagFromJson(rawNbtJson);
                                        result.addBlockVariation((String)entry.getKey(), variantNBT);
                                    } catch (NBTException var32) {
                                        return null;
                                    }
                                }
                            }

                            JsonArray jsonYPosArray = jsonRoot.get("shape").getAsJsonArray();
                            int height = jsonYPosArray.size();
                            int width = 0;
                            int depth = 0;
                            Iterator var51 = jsonYPosArray.iterator();

                            Iterator var58;
                            JsonElement jsonYElement;
                            while(var51.hasNext()) {
                                jsonYElement = (JsonElement)var51.next();
                                JsonArray jsonZPosArray = jsonYElement.getAsJsonArray();
                                depth = Math.max(depth, jsonZPosArray.size());

                                JsonArray jsonXPosArray;
                                for(var58 = jsonZPosArray.iterator(); var58.hasNext(); width = Math.max(width, jsonXPosArray.size())) {
                                    jsonYElement = (JsonElement)var58.next();
                                    jsonXPosArray = jsonYElement.getAsJsonArray();
                                }
                            }

                            String[][][] positionMap = new String[height][depth][width];
                            String[][][] variantMap = new String[height][depth][width];
                            int y = 0;

                            for(var58 = jsonYPosArray.iterator(); var58.hasNext(); ++y) {
                                jsonYElement = (JsonElement)var58.next();
                                int z = 0;

                                for(Iterator var62 = jsonYElement.getAsJsonArray().iterator(); var62.hasNext(); ++z) {
                                    JsonElement jsonZElement = (JsonElement)var62.next();
                                    x = 0;

                                    for(Iterator var64 = jsonZElement.getAsJsonArray().iterator(); var64.hasNext(); ++x) {
                                        JsonElement jsonXElement = (JsonElement)var64.next();
                                        String ref = jsonXElement.getAsString();
                                        if (ref.contains(":")) {
                                            positionMap[height - 1 - y][z][x] = ref.substring(0, ref.indexOf(58));
                                            variantMap[height - 1 - y][z][x] = ref;
                                        } else {
                                            positionMap[height - 1 - y][z][x] = ref;
                                        }
                                    }
                                }
                            }
                            result.setLabel(label);
                            result.setPositionMap(positionMap);
                            result.setVariantMap(variantMap);
                            return result;

                    }
                }
            } else {
                return null;
            }
        }
    }

    public JsonElement serialize(MultiblockRecipe src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject root = new JsonObject();
        return root;
    }
}
