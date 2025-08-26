package igentuman.mbtool;

import igentuman.mbtool.client.screen.MultibuilderScreen;
import igentuman.mbtool.client.screen.MultibuilderSelectStructureScreen;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.registry.MbtoolDataComponents;
import igentuman.mbtool.util.ItemCapabilityProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(
       igentuman.mbtool.Mbtool.MODID
)
public class Mbtool
{
    public static final String MODID = "mbtool";
    public static final Logger logger = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final Item.Properties ONE_ITEM_PROPERTIES = new Item.Properties().stacksTo(1).setNoCombineRepair().enchantable(0);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    public static final DeferredHolder<Item, MultibuilderItem> MBTOOL = ITEMS.register("mbtool", () -> new MultibuilderItem(ONE_ITEM_PROPERTIES));
    public static final DeferredHolder<MenuType<?>, MenuType<MultibuilderContainer>> MULTIBUILDER_CONTAINER = CONTAINERS.register("mbtool_container",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new MultibuilderContainer(windowId, data.readBlockPos(), inv, data.readInt())));
    public static final DeferredHolder<MenuType<?>, MenuType<MultibuilderSelectStructureContainer>> MULTIBUILDER_STRUCTURE_CONTAINER = CONTAINERS.register("mbtool_structure_container",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new MultibuilderSelectStructureContainer(windowId, data.readBlockPos(), inv, data.readInt())));


    public Mbtool(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
        MbtoolDataComponents.DATA_COMPONENTS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(NetworkHandler::registerPackets);
        
        // Register configuration
        MbtoolConfig.register();
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Network registration is now handled via RegisterPayloadHandlersEvent
        });
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MULTIBUILDER_CONTAINER.get(), MultibuilderScreen::new);
        event.register(MULTIBUILDER_STRUCTURE_CONTAINER.get(), 
            (MultibuilderSelectStructureContainer container, Inventory inventory, Component title) -> 
                new MultibuilderSelectStructureScreen(container, inventory, title));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MBTOOL.get());
        }
    }
    
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register energy storage capability for multibuilder items
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, context) -> {
                if (stack.getItem() instanceof MultibuilderItem) {
                    return ItemCapabilityProvider.getEnergyStorage(stack);
                }
                return null;
            },
            MBTOOL.get()
        );
        
        // Register item handler capability for multibuilder items
        event.registerItem(
            Capabilities.ItemHandler.ITEM,
            (stack, context) -> {
                if (stack.getItem() instanceof MultibuilderItem) {
                    return ItemCapabilityProvider.getItemHandler(stack);
                }
                return null;
            },
            MBTOOL.get()
        );
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Resource manager registration is now handled differently in NeoForge 1.21
        // MultiblocksProvider should be registered through AddReloadListenerEvent instead
    }

    public static ResourceLocation rlFromString(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }
}
