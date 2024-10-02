package igentuman.mbtool;

import igentuman.mbtool.client.handler.ClientHandler;
import igentuman.mbtool.network.GuiProxy;
import igentuman.mbtool.network.ModPacketHandler;
import igentuman.mbtool.util.MbtoolHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = igentuman.mbtool.Mbtool.MODID,
        name = igentuman.mbtool.Mbtool.NAME,
        version = igentuman.mbtool.Mbtool.VERSION,
        dependencies = ""
)
@Mod.EventBusSubscriber
public class Mbtool
{
    public static final String MODID = "mbtool";
    public static final String NAME = "Multi Builder Tool";
    public static final String VERSION = "1.0.4";

    @Mod.Instance(MODID)
    public static Mbtool instance;
        
    @SidedProxy(serverSide="igentuman.mbtool.common.CommonProxy", clientSide="igentuman.mbtool.client.ClientProxy")
    public static ISidedProxy proxy;

    public Logger logger;

    public static MbtoolHooks hooks = new MbtoolHooks();


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        hooks.hookPreInit();

        proxy.preInit(event);
        logger = event.getModLog();

        MinecraftForge.EVENT_BUS.register(new RegistryHandler());
        MinecraftForge.EVENT_BUS.register(new ClientHandler());

        MinecraftForge.EVENT_BUS.register(this);
        ModPacketHandler.registerMessages(MODID);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)  {
        hooks.hookInit();

        proxy.init(event);

        ConfigManager.sync(MODID, Config.Type.INSTANCE);
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiProxy());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        hooks.hookPostInit();

        proxy.postInit(event);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if(event.getModID().equals(MODID)) {
            ConfigManager.sync(MODID, Config.Type.INSTANCE);
        }
    }
}
