package igentuman.mbtool.client.handler;

import igentuman.mbtool.client.render.PreviewRenderer;
import igentuman.mbtool.item.MultibuilderItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static igentuman.mbtool.Mbtool.MODID;
import static igentuman.mbtool.Mbtool.MBTOOL;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;

        ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offItem = player.getItemInHand(InteractionHand.OFF_HAND);

        boolean main = !mainItem.isEmpty() && mainItem.is(MBTOOL.get()) && ClientHandler.hasRecipe(mainItem);
        boolean off = !offItem.isEmpty() && offItem.is(MBTOOL.get()) && ClientHandler.hasRecipe(offItem);

        if (!main && !off) {
            return;
        }

        // Check if there's any delay (if needed for future implementation)
        // For now, we'll render the preview immediately
        
        PreviewRenderer.renderPreview(event.getPoseStack(), event.getPartialTick());
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
            double scrollDelta = event.getScrollDelta();
            
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