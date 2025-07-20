package igentuman.mbtool.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

import static igentuman.mbtool.Mbtool.MODID;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.tryBuild(MODID, "jei_plugin");
    }
}
