package igentuman.mbtool;

import igentuman.mbtool.client.screen.MultibuilderScreen;
import igentuman.mbtool.client.screen.MultibuilderSelectStructureScreen;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.BlockEquivalencyManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(Mbtool.MODID)
public class Mbtool
{
    public static final String MODID = "mbtool";
    public static final Logger logger = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final Item.Properties ONE_ITEM_PROPERTIES = new Item.Properties().stacksTo(1);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<Item, MultibuilderItem> MBTOOL = ITEMS.register("mbtool", () -> new MultibuilderItem(ONE_ITEM_PROPERTIES));
    public static final DeferredHolder<MenuType<?>, MenuType<MultibuilderContainer>> MULTIBUILDER_CONTAINER = CONTAINERS.register("mbtool_container",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new MultibuilderContainer(windowId, data.readBlockPos(), inv, data.readInt())));
    public static final DeferredHolder<MenuType<?>, MenuType<MultibuilderSelectStructureContainer>> MULTIBUILDER_STRUCTURE_CONTAINER = CONTAINERS.register("mbtool_structure_container",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new MultibuilderSelectStructureContainer(windowId, data.readBlockPos(), inv, data.readInt())));


    public Mbtool(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
        MbtoolDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::onModConfigEvent);
        
        // Register configuration
        MbtoolConfig.register(modContainer);

        NeoForge.EVENT_BUS.register(this);
    }
    
    public void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            // Reinitialize block equivalency manager when config changes
            BlockEquivalencyManager.reinitialize();
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) -> {
            if (stack.getItem() instanceof MultibuilderItem item) {
                return item.getEnergy(stack);
            }
            return null;
        }, MBTOOL.get());

        event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> {
            if (stack.getItem() instanceof MultibuilderItem item) {
                return item.getInventory(stack);
            }
            return null;
        }, MBTOOL.get());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MBTOOL.get());
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MultiblocksProvider.getInstance());
    }

    public static ResourceLocation rlFromString(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(MULTIBUILDER_CONTAINER.get(), MultibuilderScreen::new);
            event.register(MULTIBUILDER_STRUCTURE_CONTAINER.get(),
                    (MultibuilderSelectStructureContainer container, Inventory inventory, Component title) ->
                            new MultibuilderSelectStructureScreen(container, inventory, title));
        }
    }
}
