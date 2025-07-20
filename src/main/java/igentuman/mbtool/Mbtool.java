package igentuman.mbtool;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
       igentuman.mbtool.Mbtool.MODID
)
@Mod.EventBusSubscriber
public class Mbtool
{
    public static final String MODID = "mbtool";
    public static final Logger logger = LogManager.getLogger();

    public Mbtool() {
        this(FMLJavaModLoadingContext.get());
    }

    public Mbtool(FMLJavaModLoadingContext context) {

    }

    public static ResourceLocation rlFromString(String name) {
        return ResourceLocation.tryParse(name);
    }
}
