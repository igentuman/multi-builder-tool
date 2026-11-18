package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.screen.MultibuilderSelectStructureScreen;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static igentuman.mbtool.Mbtool.MODID;
import static igentuman.mbtool.Mbtool.MULTIBUILDER_CONTAINER;
import static igentuman.mbtool.Mbtool.MULTIBUILDER_STRUCTURE_CONTAINER;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModHandler {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        // The tool GUI opens on the structure browser; its Inventory button switches to the slot view.
        event.register(MULTIBUILDER_CONTAINER.get(),
                (MultibuilderContainer container, Inventory inventory, Component title) ->
                        new MultibuilderSelectStructureScreen<>(container, inventory, title));
        event.register(MULTIBUILDER_STRUCTURE_CONTAINER.get(),
                (MultibuilderSelectStructureContainer container, Inventory inventory, Component title) ->
                        new MultibuilderSelectStructureScreen<>(container, inventory, title));
    }
}
