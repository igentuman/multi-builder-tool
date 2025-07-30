package igentuman.mbtool.client.gui;

import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A comprehensive example screen showing various uses of MultiblockButton
 * This demonstrates a gallery of multiblock structures with different button styles
 */
public class MultiblockGalleryScreen extends Screen {
    private MultiblockSelectionGrid structureGrid;
    private MultiblockButton previewButton;
    private MultiblockButton selectedStructureButton;
    private Button closeButton;
    private Button styleToggleButton;
    
    private final List<MultiblockStructure> exampleStructures = new ArrayList<>();
    private final List<String> structureNames = new ArrayList<>();
    private int currentStyleIndex = 0;
    private final String[] styleNames = {"Standard", "Dark", "Light", "Blue", "Green", "Transparent"};
    
    public MultiblockGalleryScreen() {
        super(Component.translatable("gui.mbtool.multiblock_gallery"));
        initializeExampleStructures();
    }
    
    private void initializeExampleStructures() {
        // In a real implementation, you would load these from saved NBT data
        // For demonstration, we'll create empty structures with different names
        String[] names = {
            "Simple House", "Castle Tower", "Windmill", "Bridge", 
            "Factory", "Lighthouse", "Temple", "Observatory"
        };
        
        for (String name : names) {
            // Create empty structure (in practice, load from NBT)
            MultiblockStructure structure = new MultiblockStructure(new CompoundTag());
            exampleStructures.add(structure);
            structureNames.add(name);
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Structure selection grid
        int gridX = 20;
        int gridY = 40;
        int gridWidth = 280;
        int gridHeight = 200;
        
        structureGrid = new MultiblockSelectionGrid(
            gridX, gridY, gridWidth, gridHeight,
            this::onStructureSelected
        );
        
        // Add example structures to the grid
        for (int i = 0; i < exampleStructures.size(); i++) {
            structureGrid.addStructure(
                exampleStructures.get(i),
                structureNames.get(i),
                Component.translatable("gui.mbtool.structure_description", structureNames.get(i))
            );
        }
        
        this.addRenderableWidget(structureGrid);
        
        // Large preview button showing selected structure
        int previewX = gridX + gridWidth + 20;
        int previewY = gridY;
        int previewSize = 120;
        
        previewButton = MultiblockButtonStyles.createLargeSquare(
            previewX, previewY,
            exampleStructures.isEmpty() ? null : exampleStructures.get(0),
            this::onPreviewButtonPressed
        );
        previewButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.mbtool.preview_tooltip")
        ));
        
        this.addRenderableWidget(previewButton);
        
        // Style demonstration buttons
        int styleY = previewY + previewSize + 20;
        selectedStructureButton = createStyledButton(previewX, styleY, currentStyleIndex);
        this.addRenderableWidget(selectedStructureButton);
        
        // Style toggle button
        styleToggleButton = Button.builder(
            Component.translatable("gui.mbtool.style", styleNames[currentStyleIndex]),
            this::onStyleTogglePressed
        ).bounds(previewX, styleY + 70, 120, 20).build();
        
        this.addRenderableWidget(styleToggleButton);
        
        // Close button
        closeButton = Button.builder(
            Component.translatable("gui.done"),
            (btn) -> this.onClose()
        ).bounds(this.width - 80, this.height - 30, 60, 20).build();
        
        this.addRenderableWidget(closeButton);
        
        // Select first structure by default
        if (!exampleStructures.isEmpty()) {
            structureGrid.setSelectedIndex(0);
            onStructureSelected(exampleStructures.get(0));
        }
    }
    
    private MultiblockButton createStyledButton(int x, int y, int styleIndex) {
        MultiblockStructure structure = structureGrid.getSelectedStructure();
        
        return switch (styleIndex) {
            case 0 -> MultiblockButtonStyles.createStandard(x, y, 120, 60, structure, this::onStyledButtonPressed);
            case 1 -> MultiblockButtonStyles.createDark(x, y, 120, 60, structure, this::onStyledButtonPressed);
            case 2 -> MultiblockButtonStyles.createLight(x, y, 120, 60, structure, this::onStyledButtonPressed);
            case 3 -> MultiblockButtonStyles.createBlueAccent(x, y, 120, 60, structure, this::onStyledButtonPressed);
            case 4 -> MultiblockButtonStyles.createGreenAccent(x, y, 120, 60, structure, this::onStyledButtonPressed);
            case 5 -> MultiblockButtonStyles.createTransparent(x, y, 120, 60, structure, this::onStyledButtonPressed);
            default -> MultiblockButtonStyles.createStandard(x, y, 120, 60, structure, this::onStyledButtonPressed);
        };
    }
    
    private void onStructureSelected(MultiblockStructure structure) {
        // Update preview button
        if (previewButton != null) {
            previewButton.setStructure(structure);
        }
        
        // Update styled button
        if (selectedStructureButton != null) {
            selectedStructureButton.setStructure(structure);
        }
    }
    
    private void onPreviewButtonPressed(MultiblockButton button) {
        // Handle preview button press - could open detailed view
        if (this.minecraft != null && this.minecraft.player != null) {
            String structureName = "Unknown";
            int selectedIndex = structureGrid.selectedIndex;
            if (selectedIndex >= 0 && selectedIndex < structureNames.size()) {
                structureName = structureNames.get(selectedIndex);
            }
            
            this.minecraft.player.sendSystemMessage(
                Component.translatable("gui.mbtool.preview_pressed", structureName)
            );
        }
    }
    
    private void onStyledButtonPressed(MultiblockButton button) {
        // Handle styled button press
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(
                Component.translatable("gui.mbtool.styled_button_pressed", styleNames[currentStyleIndex])
            );
        }
    }
    
    private void onStyleTogglePressed(Button button) {
        // Cycle through different styles
        currentStyleIndex = (currentStyleIndex + 1) % styleNames.length;
        
        // Update button text
        styleToggleButton.setMessage(Component.translatable("gui.mbtool.style", styleNames[currentStyleIndex]));
        
        // Recreate the styled button with new style
        this.removeWidget(selectedStructureButton);
        selectedStructureButton = createStyledButton(
            selectedStructureButton.getX(), 
            selectedStructureButton.getY(), 
            currentStyleIndex
        );
        this.addRenderableWidget(selectedStructureButton);
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
            15,
            0xFFFFFF
        );
        
        // Render section labels
        guiGraphics.drawString(
            this.font,
            Component.translatable("gui.mbtool.structure_gallery"),
            20,
            25,
            0xFFFFFF
        );
        
        guiGraphics.drawString(
            this.font,
            Component.translatable("gui.mbtool.preview"),
            320,
            25,
            0xFFFFFF
        );
        
        guiGraphics.drawString(
            this.font,
            Component.translatable("gui.mbtool.style_demo"),
            320,
            175,
            0xFFFFFF
        );
        
        // Render selected structure info
        int selectedIndex = structureGrid.selectedIndex;
        if (selectedIndex >= 0 && selectedIndex < structureNames.size()) {
            String structureName = structureNames.get(selectedIndex);
            MultiblockStructure structure = exampleStructures.get(selectedIndex);
            
            Component info = Component.translatable("gui.mbtool.selected_info", 
                structureName,
                structure.getWidth(),
                structure.getHeight(),
                structure.getDepth()
            );
            
            guiGraphics.drawString(
                this.font,
                info,
                320,
                140,
                0xCCCCCC
            );
        }
        
        // Call super to render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Method to add a new structure to the gallery
     * This could be called from external code to populate the gallery
     */
    public void addStructure(MultiblockStructure structure, String name) {
        exampleStructures.add(structure);
        structureNames.add(name);
        
        if (structureGrid != null) {
            structureGrid.addStructure(
                structure,
                name,
                Component.translatable("gui.mbtool.structure_description", name)
            );
        }
    }
    
    /**
     * Method to clear all structures from the gallery
     */
    public void clearStructures() {
        exampleStructures.clear();
        structureNames.clear();
        
        if (structureGrid != null) {
            structureGrid.clearStructures();
        }
    }
}