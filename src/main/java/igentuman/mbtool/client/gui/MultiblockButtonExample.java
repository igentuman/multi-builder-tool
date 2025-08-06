package igentuman.mbtool.client.gui;

import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Example screen demonstrating the usage of MultiblockButton
 */
public class MultiblockButtonExample extends Screen {
    private MultiblockButton exampleButton;
    private MultiblockStructure exampleStructure;
    
    public MultiblockButtonExample() {
        super(Component.translatable("gui.mbtool.multiblock_button_example"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Create an example multiblock structure (you would load this from NBT data)
        // For demonstration, we'll create an empty structure - in practice you'd load from saved data
        this.exampleStructure = createExampleStructure();
        
        // Create the multiblock button
        int buttonWidth = 100;
        int buttonHeight = 80;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = (this.height - buttonHeight) / 2;
        
        this.exampleButton = MultiblockButton.builder()
            .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
            .structure(exampleStructure)
            .onPress(this::onMultiblockButtonPressed)
            .message(Component.translatable("gui.mbtool.example_structure"))
            .build();
        
        // Add tooltip
        this.exampleButton.setTooltip(Tooltip.create(
            Component.translatable("gui.mbtool.multiblock_button.tooltip")
        ));
        
        this.addRenderableWidget(this.exampleButton);
        
        // Example of a button without background
        MultiblockButton transparentButton = MultiblockButton.builder()
            .bounds(buttonX + 120, buttonY, buttonWidth, buttonHeight)
            .structure(exampleStructure)
            .onPress(this::onTransparentButtonPressed)
            .renderBackground(false)
            .message(Component.translatable("gui.mbtool.transparent_example"))
            .build();
        
        this.addRenderableWidget(transparentButton);
    }
    
    private MultiblockStructure createExampleStructure() {
        // In a real implementation, you would load this from NBT data
        // For now, we'll create an empty structure that can be populated later
        CompoundTag emptyNbt = new CompoundTag();
        return new MultiblockStructure(emptyNbt);
    }
    
    private void onMultiblockButtonPressed(MultiblockButton button) {
        // Handle button press - for example, open a structure selection screen
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(
                Component.translatable("gui.mbtool.multiblock_button.pressed")
            );
        }
    }
    
    private void onTransparentButtonPressed(MultiblockButton button) {
        // Handle transparent button press
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(
                Component.translatable("gui.mbtool.transparent_button.pressed")
            );
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        this.renderBackground(guiGraphics);
        
        // Render title
        guiGraphics.drawCenteredString(
            this.font, 
            this.title, 
            this.width / 2, 
            20, 
            0xFFFFFF
        );
        
        // Render instructions
        Component instructions = Component.translatable("gui.mbtool.multiblock_button.instructions");
        guiGraphics.drawCenteredString(
            this.font,
            instructions,
            this.width / 2,
            this.height - 30,
            0xAAAAAA
        );
        
        // Call super to render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Method to update the structure displayed in the button
     * This can be called from outside to change what structure is shown
     */
    public void updateStructure(MultiblockStructure newStructure) {
        this.exampleStructure = newStructure;
        if (this.exampleButton != null) {
            this.exampleButton.setStructure(newStructure);
        }
    }
}