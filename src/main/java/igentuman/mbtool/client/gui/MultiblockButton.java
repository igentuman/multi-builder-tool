package igentuman.mbtool.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom button that renders a multiblock structure instead of text or image
 */
public class MultiblockButton extends AbstractButton {
    private MultiblockStructure structure;
    private final OnPress onPress;
    private boolean renderBackground = true;
    private int backgroundColor = 0xFF8B8B8B;
    private int hoveredBackgroundColor = 0xFFA0A0A0;
    private int pressedBackgroundColor = 0xFF606060;
    
    public MultiblockButton(int x, int y, int width, int height, MultiblockStructure structure, OnPress onPress) {
        this(x, y, width, height, structure, onPress, Component.empty());
    }
    
    public MultiblockButton(int x, int y, int width, int height, MultiblockStructure structure, OnPress onPress, Component message) {
        super(x, y, width, height, message);
        this.structure = structure;
        this.onPress = onPress;
        updateTooltip();
    }
    
    /**
     * Sets the multiblock structure to render
     */
    public void setStructure(MultiblockStructure structure) {
        this.structure = structure;
        updateTooltip();
    }
    
    /**
     * Gets the current multiblock structure
     */
    public MultiblockStructure getStructure() {
        return this.structure;
    }
    
    /**
     * Sets whether to render the button background
     */
    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
    }
    
    /**
     * Sets the background colors for different button states
     */
    public void setBackgroundColors(int normal, int hovered, int pressed) {
        this.backgroundColor = normal;
        this.hoveredBackgroundColor = hovered;
        this.pressedBackgroundColor = pressed;
    }
    
    /**
     * Updates the tooltip based on the current structure
     */
    void updateTooltip() {
        if (structure != null && structure.getName() != null && !structure.getName().isEmpty()) {
            // Create a single component with structure name and dimensions
            Component tooltip = Component.translatable(structure.getName())
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.mbtool.multiblock_button.dimensions", 
                    structure.getWidth(), structure.getHeight(), structure.getDepth())
                    .withStyle(style -> style.withColor(0x808080)));
            
            setTooltip(Tooltip.create(tooltip));
        } else {
            // Clear tooltip if no structure or no name
            setTooltip(null);
        }
    }
    
    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        // Render background if enabled
        if (renderBackground) {
            int bgColor = backgroundColor;
            if (!this.active) {
                bgColor = 0xFF606060; // Disabled color
            } else if (this.isHoveredOrFocused()) {
                bgColor = hoveredBackgroundColor;
            }
            
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            
            // Render border
            int borderColor = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF000000;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor); // Top
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor); // Bottom
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor); // Left
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor); // Right
        }
        
        // Render the multiblock structure if available
        if (structure != null && !structure.getBlocks().isEmpty()) {
            // Save the current state
            guiGraphics.pose().pushPose();
            
            try {
                // Enable scissor test to clip rendering to button bounds
                double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
                int scaledX = (int) (this.getX() * guiScale);
                int scaledY = (int) (this.getY() * guiScale);
                int scaledWidth = (int) (this.width * guiScale);
                int scaledHeight = (int) (this.height * guiScale);
                
                // Adjust for screen coordinates (Minecraft's scissor uses screen coordinates)
                int screenHeight = Minecraft.getInstance().getWindow().getHeight();
                scaledY = screenHeight - scaledY - scaledHeight;
                
                RenderSystem.enableScissor(scaledX, scaledY, scaledWidth, scaledHeight);
                
                // Add some padding to the rendering area
                int padding = renderBackground ? 3 : 1;
                int renderX = this.getX() + padding;
                int renderY = this.getY() + padding;
                int renderWidth = Math.max(1, this.width - (padding * 2));
                int renderHeight = Math.max(1, this.height - (padding * 2));
                
                // Render the multiblock structure
                MultiblockRenderer.render(structure, guiGraphics.pose(), renderX, renderY, renderWidth, renderHeight);
                
            } catch (Exception e) {
                // If rendering fails, just continue without the structure
                // This prevents crashes if there are issues with the multiblock data
                System.err.println("Failed to render multiblock structure: " + e.getMessage());
                e.printStackTrace();
            } finally {
                RenderSystem.disableScissor();
                guiGraphics.pose().popPose();
            }
        } else {
            // If no structure is available, render a placeholder
            if (renderBackground) {
                int centerX = this.getX() + this.width / 2;
                int centerY = this.getY() + this.height / 2;
                
                // Draw a simple "no structure" indicator
                guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.mbtool.no_structure"), 
                    centerX, 
                    centerY - 4, 
                    0x808080
                );
            }
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (this.structure != null) {
            narrationElementOutput.add(NarratedElementType.TITLE,
                Component.translatable("gui.mbtool.multiblock_button.narration", 
                    this.structure.getWidth(), this.structure.getHeight(), this.structure.getDepth()));
        } else {
            narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        }
    }
    
    /**
     * Functional interface for button press handling
     */
    @FunctionalInterface
    public interface OnPress {
        void onPress(MultiblockButton button);
    }
    
    /**
     * Builder class for easier MultiblockButton creation
     */
    public static class Builder {
        private int x, y, width, height;
        private MultiblockStructure structure;
        private OnPress onPress;
        private Component message = Component.empty();
        private boolean renderBackground = true;
        private int backgroundColor = 0xFF8B8B8B;
        private int hoveredBackgroundColor = 0xFFA0A0A0;
        private int pressedBackgroundColor = 0xFF606060;
        
        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder structure(MultiblockStructure structure) {
            this.structure = structure;
            return this;
        }
        
        public Builder onPress(OnPress onPress) {
            this.onPress = onPress;
            return this;
        }
        
        public Builder message(Component message) {
            this.message = message;
            return this;
        }
        
        public Builder renderBackground(boolean renderBackground) {
            this.renderBackground = renderBackground;
            return this;
        }
        
        public Builder backgroundColors(int normal, int hovered, int pressed) {
            this.backgroundColor = normal;
            this.hoveredBackgroundColor = hovered;
            this.pressedBackgroundColor = pressed;
            return this;
        }
        
        public MultiblockButton build() {
            MultiblockButton button = new MultiblockButton(x, y, width, height, structure, onPress, message);
            button.setRenderBackground(renderBackground);
            button.setBackgroundColors(backgroundColor, hoveredBackgroundColor, pressedBackgroundColor);
            button.updateTooltip(); // Ensure tooltip is updated after all properties are set
            return button;
        }
    }
    
    /**
     * Creates a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}