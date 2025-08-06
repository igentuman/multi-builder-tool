package igentuman.mbtool.client.gui;

import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.network.chat.Component;

/**
 * Utility class providing common styles and configurations for MultiblockButton
 */
public class MultiblockButtonStyles {
    
    // Standard button colors
    public static final int STANDARD_NORMAL = 0xFF8B8B8B;
    public static final int STANDARD_HOVERED = 0xFFA0A0A0;
    public static final int STANDARD_PRESSED = 0xFF606060;
    
    // Dark theme colors
    public static final int DARK_NORMAL = 0xFF404040;
    public static final int DARK_HOVERED = 0xFF505050;
    public static final int DARK_PRESSED = 0xFF303030;
    
    // Light theme colors
    public static final int LIGHT_NORMAL = 0xFFE0E0E0;
    public static final int LIGHT_HOVERED = 0xFFF0F0F0;
    public static final int LIGHT_PRESSED = 0xFFD0D0D0;
    
    // Accent colors
    public static final int BLUE_NORMAL = 0xFF4A90E2;
    public static final int BLUE_HOVERED = 0xFF5BA0F2;
    public static final int BLUE_PRESSED = 0xFF3A80D2;
    
    public static final int GREEN_NORMAL = 0xFF5CB85C;
    public static final int GREEN_HOVERED = 0xFF6CC86C;
    public static final int GREEN_PRESSED = 0xFF4CA84C;
    
    public static final int RED_NORMAL = 0xFFD9534F;
    public static final int RED_HOVERED = 0xFFE9635F;
    public static final int RED_PRESSED = 0xFFC9433F;
    
    /**
     * Creates a standard multiblock button with default styling
     */
    public static MultiblockButton createStandard(int x, int y, int width, int height, 
                                                 MultiblockStructure structure, 
                                                 MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .backgroundColors(STANDARD_NORMAL, STANDARD_HOVERED, STANDARD_PRESSED)
            .build();
    }
    
    /**
     * Creates a dark-themed multiblock button
     */
    public static MultiblockButton createDark(int x, int y, int width, int height, 
                                             MultiblockStructure structure, 
                                             MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .backgroundColors(DARK_NORMAL, DARK_HOVERED, DARK_PRESSED)
            .build();
    }
    
    /**
     * Creates a light-themed multiblock button
     */
    public static MultiblockButton createLight(int x, int y, int width, int height, 
                                              MultiblockStructure structure, 
                                              MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .backgroundColors(LIGHT_NORMAL, LIGHT_HOVERED, LIGHT_PRESSED)
            .build();
    }
    
    /**
     * Creates a transparent multiblock button (no background)
     */
    public static MultiblockButton createTransparent(int x, int y, int width, int height, 
                                                    MultiblockStructure structure, 
                                                    MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .renderBackground(false)
            .build();
    }
    
    /**
     * Creates a blue accent multiblock button
     */
    public static MultiblockButton createBlueAccent(int x, int y, int width, int height, 
                                                   MultiblockStructure structure, 
                                                   MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .backgroundColors(BLUE_NORMAL, BLUE_HOVERED, BLUE_PRESSED)
            .build();
    }
    
    /**
     * Creates a green accent multiblock button (good for "confirm" actions)
     */
    public static MultiblockButton createGreenAccent(int x, int y, int width, int height, 
                                                    MultiblockStructure structure, 
                                                    MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .backgroundColors(GREEN_NORMAL, GREEN_HOVERED, GREEN_PRESSED)
            .build();
    }
    
    /**
     * Creates a red accent multiblock button (good for "delete" or "cancel" actions)
     */
    public static MultiblockButton createRedAccent(int x, int y, int width, int height, 
                                                  MultiblockStructure structure, 
                                                  MultiblockButton.OnPress onPress) {
        return MultiblockButton.builder()
            .bounds(x, y, width, height)
            .structure(structure)
            .onPress(onPress)
            .backgroundColors(RED_NORMAL, RED_HOVERED, RED_PRESSED)
            .build();
    }
    
    /**
     * Creates a small square multiblock button (good for toolbars or grids)
     */
    public static MultiblockButton createSmallSquare(int x, int y, MultiblockStructure structure, 
                                                    MultiblockButton.OnPress onPress) {
        return createStandard(x, y, 32, 32, structure, onPress);
    }
    
    /**
     * Creates a medium square multiblock button
     */
    public static MultiblockButton createMediumSquare(int x, int y, MultiblockStructure structure, 
                                                     MultiblockButton.OnPress onPress) {
        return createStandard(x, y, 64, 64, structure, onPress);
    }
    
    /**
     * Creates a large square multiblock button
     */
    public static MultiblockButton createLargeSquare(int x, int y, MultiblockStructure structure, 
                                                    MultiblockButton.OnPress onPress) {
        return createStandard(x, y, 96, 96, structure, onPress);
    }
    
    /**
     * Creates a rectangular multiblock button (good for lists)
     */
    public static MultiblockButton createRectangular(int x, int y, MultiblockStructure structure, 
                                                    MultiblockButton.OnPress onPress) {
        return createStandard(x, y, 120, 60, structure, onPress);
    }
    
    /**
     * Creates a multiblock button with a tooltip describing the structure
     */
    public static MultiblockButton createWithStructureTooltip(int x, int y, int width, int height, 
                                                             MultiblockStructure structure, 
                                                             MultiblockButton.OnPress onPress,
                                                             String structureName) {
        MultiblockButton button = createStandard(x, y, width, height, structure, onPress);
        
        if (structure != null && !structure.getBlocks().isEmpty()) {
            Component tooltip = Component.translatable("gui.mbtool.multiblock_button.structure_info",
                structureName,
                structure.getWidth(),
                structure.getHeight(), 
                structure.getDepth(),
                structure.getBlocks().size());
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
        }
        
        return button;
    }
}