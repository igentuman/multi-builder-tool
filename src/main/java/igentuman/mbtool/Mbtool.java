package igentuman.mbtool;

import igentuman.mbtool.client.screen.MultibuilderScreen;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final Item.Properties ONE_ITEM_PROPERTIES = new Item.Properties().stacksTo(1);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final RegistryObject<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter", () -> new MultibuilderItem(ONE_ITEM_PROPERTIES));
    public static final RegistryObject<MenuType<MultibuilderContainer<?>>> MULTIBUILDER_CONTAINER = CONTAINERS.register("storage_container",
            () -> IForgeMenuType.create((windowId, inv, data) -> new MultibuilderContainer<>(windowId, data.readBlockPos(), inv)));
    public Mbtool() {
        this(FMLJavaModLoadingContext.get());
    }

    public Mbtool(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModEventBus());
        CONTAINERS.register(context.getModEventBus());
    }

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MULTIBUILDER_CONTAINER.get(), MultibuilderScreen::new);
        });
    }

    public static ResourceLocation rlFromString(String name) {
        return ResourceLocation.tryParse(name);
    }
}
