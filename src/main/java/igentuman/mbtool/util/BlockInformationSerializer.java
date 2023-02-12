package igentuman.mbtool.util;


import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

public class BlockInformationSerializer implements JsonSerializer<BlockInformation>, JsonDeserializer<BlockInformation> {
    public BlockInformationSerializer() {
    }

    public JsonElement serialize(BlockInformation src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject root = new JsonObject();
        root.addProperty("block", src.block.getRegistryName().toString());
        root.addProperty("x", src.position.getX());
        root.addProperty("y", src.position.getY());
        root.addProperty("z", src.position.getZ());
        if (src.meta != 0) {
            root.addProperty("meta", src.meta);
        }

        if (src.nbt != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream base64os = Base64.getEncoder().wrap(baos);

            try {
                CompressedStreamTools.writeCompressed(src.nbt, base64os);
            } catch (IOException var8) {
            }

            if (baos != null) {
                root.addProperty("nbt", baos.toString());
            }

            if (!src.writePositionData) {
                root.addProperty("skipPositionData", true);
            }
        }

        return root;
    }

    private static JsonElement NbtToJson(NBTTagCompound tag) {
        Set<String> keys = tag.getKeySet();
        JsonObject jsonRoot = new JsonObject();

        String key;
        Object element;
        for(Iterator var3 = keys.iterator(); var3.hasNext(); jsonRoot.add(key, (JsonElement)element)) {
            key = (String)var3.next();
            NBTBase nbt = tag.getTag(key);
            if (nbt instanceof NBTTagCompound) {
                element = NbtToJson((NBTTagCompound)nbt);
            } else if (nbt instanceof NBTPrimitive) {
                String NBTType = NBTBase.NBT_TYPES[nbt.getId()];
                if (NBTType.equals("BYTE")) {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getByte());
                } else if (NBTType.equals("SHORT")) {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getShort());
                } else if (NBTType.equals("INT")) {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getInt());
                } else if (NBTType.equals("LONG")) {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getLong());
                } else if (NBTType.equals("FLOAT")) {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getFloat());
                } else if (NBTType.equals("DOUBLE")) {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getDouble());
                } else {
                    element = new JsonPrimitive(((NBTPrimitive)nbt).getDouble());
                }
            } else if (nbt instanceof NBTTagString) {
                element = new JsonPrimitive(((NBTTagString)nbt).getString());
            } else {
                JsonArray array;
                if (nbt instanceof NBTTagList) {
                    NBTTagList tagList = (NBTTagList)nbt;
                    array = new JsonArray();

                    for(int i = 0; i < tagList.tagCount(); ++i) {
                        array.add(NbtToJson(tagList.getCompoundTagAt(i)));
                    }

                    element = array;
                } else {
                    int var10;
                    int var11;
                    if (nbt instanceof NBTTagIntArray) {
                        int[] intArray = ((NBTTagIntArray)nbt).getIntArray();
                        array = new JsonArray();
                        int[] var16 = intArray;
                        var10 = intArray.length;

                        for(var11 = 0; var11 < var10; ++var11) {
                            int value = var16[var11];
                            array.add(new JsonPrimitive(value));
                        }

                        element = array;
                    } else {
                        if (!(nbt instanceof NBTTagByteArray)) {
                            throw new IllegalArgumentException("NBTtoJSON doesn't support nbt base type=" + NBTBase.NBT_TYPES[nbt.getId()] + ", tag=" + nbt);
                        }

                        byte[] byteArray = ((NBTTagByteArray)nbt).getByteArray();
                        array = new JsonArray();
                        byte[] var9 = byteArray;
                        var10 = byteArray.length;

                        for(var11 = 0; var11 < var10; ++var11) {
                            byte value = var9[var11];
                            array.add(new JsonPrimitive(value));
                        }

                        element = array;
                    }
                }
            }
        }

        return jsonRoot;
    }

    public BlockInformation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            return null;
        } else {
            JsonObject jsonRoot = json.getAsJsonObject();
            BlockPos position = new BlockPos(jsonRoot.get("x").getAsInt(), jsonRoot.get("y").getAsInt(), jsonRoot.get("z").getAsInt());
            Block block = Block.getBlockFromName(jsonRoot.get("block").getAsString());
            int meta = 0;
            if (jsonRoot.has("meta")) {
                meta = jsonRoot.get("meta").getAsInt();
            }

            NBTTagCompound nbt = null;
            if (jsonRoot.has("nbt")) {
                InputStream is = new ByteArrayInputStream(jsonRoot.get("nbt").getAsString().getBytes(StandardCharsets.UTF_8));
                InputStream wrappedIs = Base64.getDecoder().wrap(is);

                try {
                    nbt = CompressedStreamTools.readCompressed(wrappedIs);
                } catch (IOException var12) {
                }
            }

            boolean writePositionData = !jsonRoot.has("skipPositionData");
            return new BlockInformation(position, block, meta, nbt, writePositionData);
        }
    }
}
