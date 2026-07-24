package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.DismantleHandler;
import igentuman.mbtool.client.render.DismantleOverlayRenderer;
import igentuman.mbtool.client.render.PreviewRenderer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.PlacedStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import static igentuman.mbtool.Mbtool.MODID;
import static igentuman.mbtool.Mbtool.MBTOOL;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRenderAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        float partialTick = mc.getDeltaTracker().getRealtimeDeltaTicks();

        if (DismantleHandler.isDismantling()) {
            PlacedStructure dismantlingStructure = DismantleHandler.getDismantlingStructure();
            if (dismantlingStructure != null) {
                DismantleOverlayRenderer.renderDismantleOverlay(event.getPoseStack(), dismantlingStructure, partialTick);
            }
            return;
        }

        ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        boolean main = !mainItem.isEmpty() && mainItem.is(MBTOOL.get()) && ClientHandler.hasStructure(mainItem);

        if (!main || !ClientHandler.canShowPreview(player, mainItem)) {
            return;
        }

        PreviewRenderer.renderPreview(event.getPoseStack(), partialTick);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;
        
        // Check if shift is pressed
        if (!mc.options.keyShift.isDown()) return;
        
        // Check if player is holding a MultibuilderItem
        ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        
        ItemStack multibuilderStack = null;
        if (!mainItem.isEmpty() && mainItem.is(MBTOOL.get())) {
            multibuilderStack = mainItem;
        }
        
        if (multibuilderStack == null) return;
        
        // Get the MultibuilderItem instance
        if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
            double scrollDelta = event.getScrollDeltaY();
            
            if (scrollDelta > 0) {
                // Scroll up - rotate clockwise
                multibuilderItem.rotate(multibuilderStack, 1);
            } else if (scrollDelta < 0) {
                // Scroll down - rotate counter-clockwise
                multibuilderItem.rotate(multibuilderStack, -1);
            }
            
            // Cancel the event to prevent inventory scrolling
            event.setCanceled(true);
        }
    }
}