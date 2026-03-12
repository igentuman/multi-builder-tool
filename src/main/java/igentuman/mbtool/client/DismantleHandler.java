package igentuman.mbtool.client;

import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.DismantleStructurePacket;
import igentuman.mbtool.util.PlacedStructuresManager;
import igentuman.mbtool.util.PlacedStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = igentuman.mbtool.Mbtool.MODID, value = Dist.CLIENT)
public class DismantleHandler {
    private static boolean isDismantling = false;
    private static long dismantleStartTime = 0;
    private static final long DISMANTLE_DURATION = 555;
    private static BlockPos targetPos = null;
    private static InteractionHand targetHand = null;
    
    // Ray tracing state for tooltip display
    private static PlacedStructure currentLookedAtStructure = null;
    private static ItemStack currentMultibuilderStack = null;
    private static InteractionHand currentHand = null;
    
    /**
     * Public method to check if dismantling is currently active
     */
    public static boolean isDismantling() {
        return isDismantling;
    }
    
    /**
     * Get the structure currently being dismantled (for rendering overlay)
     */
    public static PlacedStructure getDismantlingStructure() {
        if (!isDismantling || targetPos == null) return null;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        
        // Get the multibuilder stack from the target hand
        ItemStack multibuilderStack = mc.player.getItemInHand(targetHand);
        if (!(multibuilderStack.getItem() instanceof MultibuilderItem)) return null;
        
        // Find the structure at the target position
        return PlacedStructuresManager.findStructureAt(multibuilderStack, targetPos);
    }
    
    /**
     * Utility method to check if the player is holding a multibuilder tool
     */
    private static boolean isHoldingMultibuilderTool(Player player) {
        if (player == null) return false;
        
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        return (mainHand.getItem() instanceof MultibuilderItem) || 
               (offHand.getItem() instanceof MultibuilderItem);
    }
    
    /**
     * Performs extended ray tracing for dismantling (longer range than default)
     */
    private static BlockHitResult performExtendedRayTrace(Minecraft mc, double maxDistance) {
        Player player = mc.player;
        if (player == null || mc.level == null) return null;
        
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));
        
        ClipContext clipContext = new ClipContext(
            eyePos, 
            endPos, 
            ClipContext.Block.OUTLINE, 
            ClipContext.Fluid.NONE, 
            player
        );
        
        BlockHitResult result = mc.level.clip(clipContext);
        return result.getType() == HitResult.Type.BLOCK ? result : null;
    }
    
    /**
     * Validates that the clicked block is actually part of the structure
     */
    private static boolean isBlockPartOfStructure(PlacedStructure structure, BlockPos clickedPos) {
        if (structure == null || clickedPos == null) return false;
        
        // Use the structure's bounding box to check if the clicked position is within the structure
        return structure.contains(clickedPos);
    }
    
    /**
     * Checks if the multibuilder has enough inventory space to hold all items from dismantling the structure
     */
    private static boolean hasEnoughInventorySpace(ItemStack multibuilderStack, PlacedStructure structure) {
        if (multibuilderStack == null || structure == null) return false;
        
        // Get the multibuilder item
        if (!(multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem)) {
            return false;
        }
        
        // Get the inventory handler
        IItemHandler inventory = multibuilderItem.getInventory(multibuilderStack, Minecraft.getInstance().level.registryAccess());
        if (inventory == null) return false;
        
        // Find the structure definition by ID
        MultiblockStructure structureDefinition = null;
        String structureId = structure.getStructureId();
        
        // Ensure structures are loaded
        if (MultiblocksProvider.structures.isEmpty()) {
            MultiblocksProvider.getStructures();
        }
        
        // Find the structure by ID
        for (MultiblockStructure struct : MultiblocksProvider.structures) {
            if (struct.getId() != null && struct.getId().toString().equals(structureId)) {
                structureDefinition = struct;
                break;
            }
        }
        
        if (structureDefinition == null) {
            // If we can't find the structure definition, we can't check space
            // Allow the operation to proceed and let the server handle it
            return true;
        }
        
        // Get the items that would be returned from dismantling
        java.util.List<ItemStack> neededItems = structureDefinition.getNeededItems();
        
        // Simulate inserting all items to check if we have space
        for (ItemStack itemStack : neededItems) {
            ItemStack remaining = itemStack.copy();
            
            // Try to insert into any available slot (simulate)
            for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
                remaining = inventory.insertItem(i, remaining, true); // true = simulate
            }
            
            // If there's still items remaining, we don't have enough space
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Updates the ray tracing state to check what structure the player is looking at
     */
    private static void updateRayTracing(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) {
            currentLookedAtStructure = null;
            currentMultibuilderStack = null;
            currentHand = null;
            return;
        }
        
        // Check if player is holding a multibuilder tool
        if (!isHoldingMultibuilderTool(player)) {
            currentLookedAtStructure = null;
            currentMultibuilderStack = null;
            currentHand = null;
            return;
        }
        
        // Determine which hand has the multibuilder tool
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        ItemStack multibuilderStack = null;
        InteractionHand hand = null;
        
        if (mainHand.getItem() instanceof MultibuilderItem) {
            multibuilderStack = mainHand;
            hand = InteractionHand.MAIN_HAND;
        } else if (offHand.getItem() instanceof MultibuilderItem) {
            multibuilderStack = offHand;
            hand = InteractionHand.OFF_HAND;
        }
        
        if (multibuilderStack == null) {
            currentLookedAtStructure = null;
            currentMultibuilderStack = null;
            currentHand = null;
            return;
        }
        
        // Perform ray tracing to see what the player is looking at
        BlockHitResult blockHitResult = performExtendedRayTrace(mc, 64.0);
        if (blockHitResult == null) {
            currentLookedAtStructure = null;
            currentMultibuilderStack = null;
            currentHand = null;
            return;
        }
        
        BlockPos pos = blockHitResult.getBlockPos();
        if(blockHitResult.getDirection() == Direction.UP) {
            pos = pos.below();
        }
        if(blockHitResult.getDirection() == Direction.EAST) {
            pos = pos.west();
        }
        if(blockHitResult.getDirection() == Direction.SOUTH) {
            pos = pos.north();
        }
        // Check if there's a placed structure at this position
        PlacedStructure structure = PlacedStructuresManager.findStructureAt(multibuilderStack, pos);
        if (structure != null && isBlockPartOfStructure(structure, pos)) {
            currentLookedAtStructure = structure;
            currentMultibuilderStack = multibuilderStack;
            currentHand = hand;
        } else {
            currentLookedAtStructure = null;
            currentMultibuilderStack = null;
            currentHand = null;
        }
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Check if F key is pressed (GLFW_KEY_F = 70)
        if (event.getKey() == GLFW.GLFW_KEY_F && isHoldingMultibuilderTool(mc.player)) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                startDismantle(mc);
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                stopDismantle();
            }
        }
    }
    
    private static void startDismantle(Minecraft mc) {
        Player player = mc.player;
        if (player == null || !isHoldingMultibuilderTool(player)) return;
        
        // Determine which hand has the multibuilder tool
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        ItemStack multibuilderStack = null;
        InteractionHand hand = null;
        
        if (mainHand.getItem() instanceof MultibuilderItem) {
            multibuilderStack = mainHand;
            hand = InteractionHand.MAIN_HAND;
        } else if (offHand.getItem() instanceof MultibuilderItem) {
            multibuilderStack = offHand;
            hand = InteractionHand.OFF_HAND;
        }
        
        if (multibuilderStack == null) {
            return;
        }
        
        // Perform extended ray tracing (up to 48 blocks for large structures)
        BlockHitResult blockHitResult = performExtendedRayTrace(mc, 64.0);
        if (blockHitResult == null) {
            return;
        }
        
        BlockPos pos = blockHitResult.getBlockPos();
        if(blockHitResult.getDirection() == Direction.UP) {
            pos = pos.below();
        }
        if(blockHitResult.getDirection() == Direction.EAST) {
            pos = pos.west();
        }
        if(blockHitResult.getDirection() == Direction.SOUTH) {
            pos = pos.north();
        }
        // Check if there's a placed structure at this position
        PlacedStructure structure = PlacedStructuresManager.findStructureAt(multibuilderStack, pos);
        if (structure == null) {
            return;
        }
        
        // Validate that the clicked block is actually part of the structure
        if (!isBlockPartOfStructure(structure, pos)) {
            return;
        }
        
        // Check if there's enough inventory space before starting dismantle
        if (!hasEnoughInventorySpace(multibuilderStack, structure)) {
            player.sendSystemMessage(Component.translatable("message.mbtool.insufficient_inventory_space"));
            return;
        }
        
        // Start dismantling
        isDismantling = true;
        dismantleStartTime = System.currentTimeMillis();
        targetPos = pos;
        targetHand = hand;
    }
    
    private static void stopDismantle() {
        if (!isDismantling) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - dismantleStartTime;
        
        if (elapsedTime >= DISMANTLE_DURATION) {
            // Dismantle completed, send packet to server
            if (targetPos != null && targetHand != null) {
                PacketDistributor.sendToServer(new DismantleStructurePacket(targetPos, targetHand));
            }
        }
        
        // Reset state
        isDismantling = false;
        dismantleStartTime = 0;
        targetPos = null;
        targetHand = null;
    }
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Update ray tracing to check what structure the player is looking at
        updateRayTracing(mc);
        
        // Aggressively consume F key presses when holding multibuilder tool to prevent item swapping
        if (isHoldingMultibuilderTool(mc.player)) {
            // This runs at the START of each tick to catch the keybind before it processes
            while (mc.options.keySwapOffhand.consumeClick()) {
                // Consume all pending F key clicks to prevent vanilla behavior
                // The custom dismantle logic is handled separately in onKeyInput
            }
        }
    }
    
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Render dismantling progress bar if currently dismantling
        if (isDismantling) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - dismantleStartTime;
            float progress = Math.min(1.0f, (float) elapsedTime / DISMANTLE_DURATION);
            
            // Render progress bar
            renderDismantleProgressBar(event.getGuiGraphics(), progress);
            
            // Auto-complete if time is up
            if (elapsedTime >= DISMANTLE_DURATION) {
                stopDismantle();
            }
        }
        // Render "Hold F to dismantle" tooltip if looking at a dismantleable structure
        else if (currentLookedAtStructure != null && currentMultibuilderStack != null) {
            renderDismantleTooltip(event.getGuiGraphics());
        }
    }
    
    private static void renderDismantleProgressBar(GuiGraphics guiGraphics, float progress) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Progress bar dimensions
        int barWidth = 100;
        int barHeight = 7;
        int barX = (screenWidth - barWidth) / 2;
        int barY = screenHeight / 2 + 50;
        
        // Background
        guiGraphics.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFF000000);
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        
        // Progress fill
        int progressWidth = (int) (barWidth * progress);
        guiGraphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFFFF6600);
        
        // Text
        Component text = Component.translatable("message.mbtool.dismantling_progress", (int) (progress * 100));
        int textWidth = mc.font.width(text);
        int textX = (screenWidth - textWidth) / 2;
        int textY = barY - 15;
        
        guiGraphics.drawString(mc.font, text, textX, textY, 0xFFFFFFFF);
    }
    
    /**
     * Renders the "Hold F to dismantle" tooltip when looking at a dismantleable structure
     */
    private static void renderDismantleTooltip(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Create the tooltip text
        Component tooltipText = Component.translatable("message.mbtool.hold_f_to_dismantle");
        
        // Calculate text dimensions
        int textWidth = mc.font.width(tooltipText);
        int textHeight = mc.font.lineHeight;
        
        // Position the tooltip in the center of the screen, slightly below the crosshair
        int tooltipX = (screenWidth - textWidth) / 2;
        int tooltipY = (screenHeight / 2) + 60; // 60 pixels below center
        
        // Background padding
        int padding = 4;
        int bgX = tooltipX - padding;
        int bgY = tooltipY - padding;
        int bgWidth = textWidth + (padding * 2);
        int bgHeight = textHeight + (padding * 2);
        
        // Render background with slight transparency
        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xCC000000);
        
        // Render border
        guiGraphics.fill(bgX - 1, bgY - 1, bgX + bgWidth + 1, bgY, 0xFFFFFFFF); // Top
        guiGraphics.fill(bgX - 1, bgY + bgHeight, bgX + bgWidth + 1, bgY + bgHeight + 1, 0xFFFFFFFF); // Bottom
        guiGraphics.fill(bgX - 1, bgY, bgX, bgY + bgHeight, 0xFFFFFFFF); // Left
        guiGraphics.fill(bgX + bgWidth, bgY, bgX + bgWidth + 1, bgY + bgHeight, 0xFFFFFFFF); // Right
        
        // Render the text
        guiGraphics.drawString(mc.font, tooltipText, tooltipX, tooltipY, 0xFFFFFF00); // Yellow text
    }
}