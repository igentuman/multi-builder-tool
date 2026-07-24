package igentuman.mbtool;

import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.BlockEquivalencyManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(Mbtool.MODID)
public class Mbtool
{
    public static final String MODID = "mbtool";
    public static final Logger logger = LogManager.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredItem<MultibuilderItem> MBTOOL = ITEMS.registerItem("mbtool", props -> new MultibuilderItem(props.stacksTo(1)));
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

        MbtoolConfig.register(modContainer);

        NeoForge.EVENT_BUS.register(this);
    }

    public void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            BlockEquivalencyManager.reinitialize();
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // TODO: Migrate to NeoForge 26.1 Transfer API
        // Capabilities.Energy.ITEM expects EnergyHandler (net.neoforged.neoforge.transfer.energy)
        // Capabilities.Item.ITEM expects ResourceHandler<ItemResource> (net.neoforged.neoforge.transfer)
        // CustomEnergyStorage and ItemInventoryHandler need to implement the new interfaces.
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MBTOOL.get());
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(rl("multiblocks_provider"), MultiblocksProvider.getInstance());
    }

    public static Identifier rlFromString(String name) {
        return Identifier.tryParse(name);
    }

    public static Identifier rl(String name) {
        return Identifier.fromNamespaceAndPath(MODID, name);
    }
}
