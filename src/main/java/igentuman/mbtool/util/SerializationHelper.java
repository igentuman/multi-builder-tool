package igentuman.mbtool.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipeSerializer;

public class SerializationHelper {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(BlockInformation.class, new BlockInformationSerializer())
            .registerTypeAdapter(MultiblockRecipe.class, new MultiblockRecipeSerializer())
            .create();
}