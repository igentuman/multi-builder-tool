package igentuman.mbtool.client.gui;

import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A grid widget that displays multiple multiblock structures as selectable buttons
 */
public class MultiblockSelectionGrid extends AbstractWidget {
    private final List<MultiblockStructureEntry> structures = new ArrayList<>();
    private final List<MultiblockButton> buttons = new ArrayList<>();
    private final Consumer<MultiblockStructure> onStructureSelected;
    private final int buttonSize;
    private final int spacing;
    private final int columns;
    public int selectedIndex = -1;
    
    public MultiblockSelectionGrid(int x, int y, int width, int height, 
                                  Consumer<MultiblockStructure> onStructureSelected) {
        this(x, y, width, height, onStructureSelected, 64, 4, 4);
    }
    
    public MultiblockSelectionGrid(int x, int y, int width, int height, 
                                  Consumer<MultiblockStructure> onStructureSelected,
                                  int buttonSize, int spacing, int columns) {
        super(x, y, width, height, Component.empty());
        this.onStructureSelected = onStructureSelected;
        this.buttonSize = buttonSize;
        this.spacing = spacing;
        this.columns = columns;
    }
    
    /**
     * Adds a multiblock structure to the grid
     */
    public void addStructure(MultiblockStructure structure, String name, Component description) {
        structures.add(new MultiblockStructureEntry(structure, name, description));
        rebuildButtons();
    }
    
    /**
     * Removes all structures from the grid
     */
    public void clearStructures() {
        structures.clear();
        buttons.clear();
        selectedIndex = -1;
    }
    
    /**
     * Gets the currently selected structure
     */
    public MultiblockStructure getSelectedStructure() {
        if (selectedIndex >= 0 && selectedIndex < structures.size()) {
            return structures.get(selectedIndex).structure;
        }
        return null;
    }
    
    /**
     * Sets the selected structure by index
     */
    public void setSelectedIndex(int index) {
        if (index >= -1 && index < structures.size()) {
            this.selectedIndex = index;
            updateButtonStates();
        }
    }
    
    private void rebuildButtons() {
        buttons.clear();
        
        for (int i = 0; i < structures.size(); i++) {
            MultiblockStructureEntry entry = structures.get(i);
            
            int row = i / columns;
            int col = i % columns;
            
            int buttonX = getX() + col * (buttonSize + spacing);
            int buttonY = getY() + row * (buttonSize + spacing);
            
            // Check if button fits within the widget bounds
            if (buttonX + buttonSize <= getX() + width && buttonY + buttonSize <= getY() + height) {
                int finalI = i;
                MultiblockButton button = MultiblockButtonStyles.createStandard(
                    buttonX, buttonY, buttonSize, buttonSize,
                    entry.structure,
                    (btn) -> onButtonPressed(finalI)
                );
                
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(entry.description));
                buttons.add(button);
            }
        }
        
        updateButtonStates();
    }
    
    private void onButtonPressed(int index) {
        setSelectedIndex(index);
        if (onStructureSelected != null && selectedIndex >= 0) {
            onStructureSelected.accept(structures.get(selectedIndex).structure);
        }
    }
    
    private void updateButtonStates() {
        for (int i = 0; i < buttons.size(); i++) {
            MultiblockButton button = buttons.get(i);
            if (i == selectedIndex) {
                // Highlight selected button
                button.setBackgroundColors(
                    MultiblockButtonStyles.BLUE_NORMAL,
                    MultiblockButtonStyles.BLUE_HOVERED,
                    MultiblockButtonStyles.BLUE_PRESSED
                );
            } else {
                // Reset to standard colors
                button.setBackgroundColors(
                    MultiblockButtonStyles.STANDARD_NORMAL,
                    MultiblockButtonStyles.STANDARD_HOVERED,
                    MultiblockButtonStyles.STANDARD_PRESSED
                );
            }
        }
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        
        // Render all buttons
        for (MultiblockButton button : buttons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // Render empty state message if no structures
        if (structures.isEmpty()) {
            Component emptyMessage = Component.translatable("gui.mbtool.no_structures_available");
            int centerX = getX() + width / 2;
            int centerY = getY() + height / 2;
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                emptyMessage,
                centerX,
                centerY,
                0x808080
            );
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) {
            return false;
        }
        
        // Check if any of our buttons were clicked
        for (MultiblockButton btn : buttons) {
            if (btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !active) {
            return false;
        }
        
        // Forward to buttons
        for (MultiblockButton btn : buttons) {
            btn.mouseReleased(mouseX, mouseY, button);
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible || !active) {
            return false;
        }
        
        // Forward to buttons
        for (MultiblockButton btn : buttons) {
            btn.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (selectedIndex >= 0 && selectedIndex < structures.size()) {
            MultiblockStructureEntry entry = structures.get(selectedIndex);
            narrationElementOutput.add(NarratedElementType.TITLE,
                Component.translatable("gui.mbtool.selected_structure", entry.name));
        } else {
            narrationElementOutput.add(NarratedElementType.TITLE,
                Component.translatable("gui.mbtool.structure_grid"));
        }
    }
    
    /**
     * Gets the number of structures in the grid
     */
    public int getStructureCount() {
        return structures.size();
    }
    
    /**
     * Gets the structure at the specified index
     */
    public MultiblockStructure getStructure(int index) {
        if (index >= 0 && index < structures.size()) {
            return structures.get(index).structure;
        }
        return null;
    }
    
    /**
     * Gets the name of the structure at the specified index
     */
    public String getStructureName(int index) {
        if (index >= 0 && index < structures.size()) {
            return structures.get(index).name;
        }
        return "";
    }
    
    /**
     * Internal class to hold structure data
     */
    private static class MultiblockStructureEntry {
        final MultiblockStructure structure;
        final String name;
        final Component description;
        
        MultiblockStructureEntry(MultiblockStructure structure, String name, Component description) {
            this.structure = structure;
            this.name = name;
            this.description = description;
        }
    }
}