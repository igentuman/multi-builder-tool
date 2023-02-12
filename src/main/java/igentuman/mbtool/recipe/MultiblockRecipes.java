package igentuman.mbtool.recipe;

import com.google.gson.stream.JsonReader;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.JarExtract;
import igentuman.mbtool.util.ResourceLoader;
import igentuman.mbtool.util.SerializationHelper;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.JarExtract;
import igentuman.mbtool.util.ResourceLoader;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultiblockRecipes {
    private static List<MultiblockRecipe> recipes = new ArrayList<>();

    private static  List<MultiblockRecipe> avaliableRecipes = new ArrayList<>();

    public static List<MultiblockRecipe> getRecipes() {
        return recipes;
    }

    public static List<MultiblockRecipe> getAvaliableRecipes() {
        if(avaliableRecipes.size() < 1) {
            for (MultiblockRecipe recipe: recipes) {
                if(recipe.isValid()) {
                    avaliableRecipes.add(recipe);
                }
            }
        }
        return avaliableRecipes;
    }

    public static void init() {
        loadRecipes();
        File mbtool = new File("config", "mbtool");
        File recipeDirectory = new File("config/mbtool", "recipes");
        if(!recipeDirectory.exists()) {
            if(!mbtool.exists()) {
                mbtool.mkdir();
            }
            recipeDirectory.mkdir();
            JarExtract.copy("assets/mbtool/config/recipes", recipeDirectory);
        }

    }



    public static MultiblockRecipe getRecipeByName(String name) {
        for (MultiblockRecipe recipe : recipes) {
            if (recipe.getName().equals(name)) {
                return recipe;
            }
        }

        return null;
    }

    private static void loadRecipes() {
        File recipeDirectory = new File("config/mbtool", "recipes");
        if (!recipeDirectory.exists()) {
            recipeDirectory.mkdir();
        }
        MultiblockRecipe recipe;
        ResourceLoader loader = new ResourceLoader(Mbtool.class, recipeDirectory, "assets/mbtool/config/recipes/");
        for(Map.Entry<String, InputStream> entry : loader.getResources().entrySet()) {
            String filename = entry.getKey();
            InputStream is = entry.getValue();

            if (!filename.endsWith(".json")) {
                continue;
            }
            JsonReader reader = new JsonReader(new InputStreamReader(is));

            try {
                recipe = SerializationHelper.GSON.fromJson(reader, MultiblockRecipe.class);
            } catch (NullPointerException e) {
                recipe = null;
            }

            if (recipe == null) {
                continue;
            }

            recipes.add(recipe);
        }
    }
}