package igentuman.mbtool;

import igentuman.mbtool.client.screen.MultibuilderScreen;
import igentuman.mbtool.client.screen.MultibuilderSelectStructureScreen;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.NetworkHandler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
    public static final RegistryObject<Item> MBTOOL = ITEMS.register("mbtool", () -> new MultibuilderItem(ONE_ITEM_PROPERTIES));
    public static final RegistryObject<MenuType<MultibuilderContainer>> MULTIBUILDER_CONTAINER = CONTAINERS.register("mbtool_container",
            () -> IForgeMenuType.create((windowId, inv, data) -> new MultibuilderContainer(windowId, data.readBlockPos(), inv, data.readInt())));
    public static final RegistryObject<MenuType<MultibuilderSelectStructureContainer>> MULTIBUILDER_STRUCTURE_CONTAINER = CONTAINERS.register("mbtool_structure_container",
            () -> IForgeMenuType.create((windowId, inv, data) -> new MultibuilderSelectStructureContainer(windowId, data.readBlockPos(), inv, data.readInt())));


    public Mbtool() {
        this(FMLJavaModLoadingContext.get());
    }

    public Mbtool(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModEventBus());
        CONTAINERS.register(context.getModEventBus());
        context.getModEventBus().addListener(this::commonSetup);
        context.getModEventBus().addListener(Mbtool::init);
        context.getModEventBus().addListener(this::addCreative);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.registerPackets();
        });
    }

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MULTIBUILDER_CONTAINER.get(), MultibuilderScreen::new);
            MenuScreens.register(MULTIBUILDER_STRUCTURE_CONTAINER.get(), 
                (MultibuilderSelectStructureContainer container, Inventory inventory, Component title) -> 
                    new MultibuilderSelectStructureScreen(container, inventory, title));
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MBTOOL);
        }
    }

    public static ResourceLocation rlFromString(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }
}
