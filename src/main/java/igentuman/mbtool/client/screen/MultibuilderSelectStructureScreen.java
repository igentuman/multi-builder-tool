package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.MultiblockButton;
import igentuman.mbtool.client.render.PreviewRenderer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static igentuman.mbtool.Mbtool.rl;
import static igentuman.mbtool.Mbtool.MBTOOL;

public class MultibuilderSelectStructureScreen extends AbstractContainerScreen<MultibuilderSelectStructureContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool.png");
    private final Screen previousScreen;
    private int currentPage = 0;
    private int pages = 1;
    
    private Button prevButton;
    private Button nextButton;
    private EditBox searchField;
    
    // Grid configuration
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_ROWS = 2;
    private static final int BUTTON_SIZE = 64;
    private static final int BUTTON_SPACING = 2;
    private static final int STRUCTURES_PER_PAGE = GRID_COLUMNS * GRID_ROWS;
    
    // Multiblock data
    private List<MultiblockStructure> allStructures = new ArrayList<>();
    private List<MultiblockStructure> filteredStructures = new ArrayList<>();
    private List<MultiblockButton> multiblockButtons = new ArrayList<>();
    private String currentFilter = "";

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        this(pMenu, pPlayerInventory, pTitle, null);
    }

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle, Screen previousScreen) {
        super(pMenu, pPlayerInventory, pTitle);
        
        // Calculate screen size based on grid
        int gridWidth = GRID_COLUMNS * BUTTON_SIZE + (GRID_COLUMNS - 1) * BUTTON_SPACING;
        int gridHeight = GRID_ROWS * BUTTON_SIZE + (GRID_ROWS - 1) * BUTTON_SPACING;
        
        this.imageWidth = Math.max(226, gridWidth + 10); // Add padding
        this.imageHeight = Math.max(186, gridHeight + 20); // Add padding for title, search field and pagination
        this.previousScreen = previousScreen;
        
        // Load structures from provider
        loadStructures();
    }

    @Override
    protected void init() {
        super.init();
        
        int screenX = (this.width - this.imageWidth) / 2;
        int screenY = (this.height - this.imageHeight) / 2;
        
        // Clear existing multiblock buttons
        multiblockButtons.clear();
        
        // Create search field
        int searchFieldWidth = Math.min(200, this.imageWidth - 80);
        int searchFieldX = screenX + (this.imageWidth - searchFieldWidth) / 2;
        int searchFieldY = screenY + this.imageHeight - 28;
        
        this.searchField = new EditBox(this.font, searchFieldX, searchFieldY, searchFieldWidth, 16, Component.literal("Search structures..."));
        this.searchField.setHint(Component.literal("Search structures..."));
        this.searchField.setResponder(this::onSearchChanged);
        this.searchField.setFocused(true); // Focus by default
        this.addRenderableWidget(this.searchField);
        
        // Calculate grid starting position (centered in the screen, below search field)
        int gridWidth = GRID_COLUMNS * BUTTON_SIZE + (GRID_COLUMNS - 1) * BUTTON_SPACING;
        int gridHeight = GRID_ROWS * BUTTON_SIZE + (GRID_ROWS - 1) * BUTTON_SPACING;
        int gridStartX = screenX + (this.imageWidth - gridWidth) / 2;
        int gridStartY = screenY + 20; // Leave space for title and search field
        
        // Create multiblock buttons in a 3x3 grid
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int buttonX = gridStartX + col * (BUTTON_SIZE + BUTTON_SPACING);
                int buttonY = gridStartY + row * (BUTTON_SIZE + BUTTON_SPACING);
                
                MultiblockButton button = MultiblockButton.builder()
                        .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                        .structure(null) // Will be set when updating page
                        .container(getMenu())
                        .onPress(this::onMultiblockButtonPressed)
                        .build();
                
                multiblockButtons.add(button);
                this.addRenderableWidget(button);
            }
        }
        
        // Previous button (left side)
        this.prevButton = Button.builder(Component.literal("<"), button -> previousPage())
                .bounds(screenX + 10, screenY + this.imageHeight - 30, 20, 20)
                .build();
        
        // Next button (right side)
        this.nextButton = Button.builder(Component.literal(">"), button -> nextPage())
                .bounds(screenX + this.imageWidth - 30, screenY + this.imageHeight - 30, 20, 20)
                .build();
        
        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);
        
        updateButtonStates();
        updateMultiblockButtons();
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        
        // Temporarily disable tooltips for multiblock buttons to prevent double tooltips
        List<Tooltip> savedTooltips = new ArrayList<>();
        for (MultiblockButton button : multiblockButtons) {
            savedTooltips.add(button.getTooltip());
            button.setTooltip(null);
        }
        
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        
        // Restore tooltips
        for (int i = 0; i < multiblockButtons.size(); i++) {
            multiblockButtons.get(i).setTooltip(savedTooltips.get(i));
        }
        
        this.renderCustomTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
    
    /**
     * Custom tooltip rendering that adjusts position based on button location
     */
    private void renderCustomTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        // Check if we're hovering over a multiblock button
        MultiblockButton hoveredButton = null;
        for (MultiblockButton button : multiblockButtons) {
            if (button.isMouseOver(pMouseX, pMouseY) && button.visible && button.getTooltip() != null) {
                hoveredButton = button;
                break;
            }
        }
        
        if (hoveredButton != null) {
            // Get the tooltip from the button
            var tooltip = hoveredButton.getTooltip();
            if (tooltip != null) {
                // Calculate screen center
                int screenCenterY = this.height / 2;
                
                // Check if button is in the top half of the screen
                boolean buttonInTopHalf = hoveredButton.getY() < screenCenterY;
                
                if (buttonInTopHalf) {
                    // For buttons in top half, render tooltip below the button
                    int tooltipX = pMouseX;
                    int tooltipY = hoveredButton.getY() + hoveredButton.getHeight() + 5;
                    
                    // Ensure tooltip doesn't go off screen
                    tooltipY = Math.min(tooltipY, this.height - 50); // Leave some margin at bottom
                    
                    pGuiGraphics.renderTooltip(this.font, tooltip.toCharSequence(this.minecraft), tooltipX, tooltipY);
                } else {
                    // For buttons in bottom half, render tooltip above the button
                    int tooltipX = pMouseX;
                    int tooltipY = hoveredButton.getY() - 5;
                    
                    // Ensure tooltip doesn't go off screen
                    tooltipY = Math.max(tooltipY, 20); // Leave some margin at top
                    
                    pGuiGraphics.renderTooltip(this.font, tooltip.toCharSequence(this.minecraft), tooltipX, tooltipY);
                }
                return;
            }
        }
        
        // Default tooltip rendering for other elements
        //this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
    
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // If search field is focused, handle all key presses to prevent default Minecraft key bindings
        if (this.searchField != null && this.searchField.isFocused()) {
            if (this.searchField.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                return true;
            }
            // Return true for any key press when search field is focused to prevent default handling
            // except for ESC key which should still close the GUI
            if (pKeyCode != 256) { // 256 is ESC key
                return true;
            }
        }
        
        if (this.searchField != null && this.searchField.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
    
    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        // If search field is focused, prioritize it for character input
        if (this.searchField != null && this.searchField.isFocused()) {
            if (this.searchField.charTyped(pCodePoint, pModifiers)) {
                return true;
            }
            // Return true to prevent other character handling when search field is focused
            return true;
        }
        
        if (this.searchField != null && this.searchField.charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.searchField != null) {
            this.searchField.tick();
        }
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.searchField.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        FormattedCharSequence formattedcharsequence = title.getVisualOrderText();
        graphics.drawString(this.font, formattedcharsequence, 110 - this.font.width(formattedcharsequence) / 2, 6, 4210752, false);

    }
    
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateButtonStates();
            onPageChanged();
        }
    }
    
    private void nextPage() {
        if (currentPage < pages - 1) {
            currentPage++;
            updateButtonStates();
            onPageChanged();
        }
    }
    
    private void updateButtonStates() {
        if (prevButton != null) {
            prevButton.active = currentPage > 0;
        }
        if (nextButton != null) {
            nextButton.active = currentPage < pages - 1;
        }
    }
    
    protected void onPageChanged() {
        updateMultiblockButtons();
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int page) {
        if (page >= 0 && page < pages) {
            this.currentPage = page;
            updateButtonStates();
            onPageChanged();
        }
    }
    
    public void setPages(int pages) {
        this.pages = Math.max(1, pages);
        if (currentPage >= this.pages) {
            currentPage = this.pages - 1;
        }
        updateButtonStates();
    }

    @Override
    public void onClose() {
        if (this.previousScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        } else {
            super.onClose();
        }
    }
    
    /**
     * Load structures from MultiblocksProvider and calculate pagination
     */
    private void loadStructures() {
        allStructures = MultiblocksProvider.getStructures();
        filteredStructures = new ArrayList<>(allStructures);
        
        // Calculate number of pages needed
        updatePagination();
    }
    
    /**
     * Handle search field changes
     */
    private void onSearchChanged(String searchText) {
        currentFilter = searchText.toLowerCase().trim();
        applyFilter();
    }
    
    /**
     * Apply current filter to structures
     */
    private void applyFilter() {
        if (currentFilter.isEmpty()) {
            filteredStructures = new ArrayList<>(allStructures);
        } else {
            filteredStructures = allStructures.stream()
                    .filter(structure -> structure.getName() != null && 
                            structure.getName().toLowerCase().contains(currentFilter))
                    .collect(Collectors.toList());
        }
        
        // Reset to first page and update pagination
        currentPage = 0;
        updatePagination();
        updateButtonStates();
        updateMultiblockButtons();
    }
    
    /**
     * Update pagination based on filtered structures
     */
    private void updatePagination() {
        if (filteredStructures.isEmpty()) {
            pages = 1;
        } else {
            pages = (int) Math.ceil((double) filteredStructures.size() / STRUCTURES_PER_PAGE);
        }
        
        // Reset to first page if current page is out of bounds
        if (currentPage >= pages) {
            currentPage = 0;
        }
    }
    
    /**
     * Update multiblock buttons with structures for current page
     */
    private void updateMultiblockButtons() {
        int startIndex = currentPage * STRUCTURES_PER_PAGE;
        
        for (int i = 0; i < multiblockButtons.size(); i++) {
            MultiblockButton button = multiblockButtons.get(i);
            int structureIndex = startIndex + i;
            
            if (structureIndex < filteredStructures.size()) {
                button.setStructure(filteredStructures.get(structureIndex));
                button.visible = true;
                button.active = true;
            } else {
                button.setStructure(null);
                button.visible = false;
                button.active = false;
            }
        }
    }
    
    /**
     * Handle multiblock button press
     */
    private void onMultiblockButtonPressed(MultiblockButton button) {
        MultiblockStructure structure = button.getStructure();
        if (structure != null && minecraft != null && minecraft.player != null) {
            // Find the index in the original allStructures list
            int originalIndex = allStructures.indexOf(structure);
            
            // Save to the item's NBT
            int slot = menu.getPlayerSlot();
            if (slot >= 0) {
                ItemStack multibuilderStack;
                if (slot == 40) { // Offhand
                    multibuilderStack = minecraft.player.getOffhandItem();
                } else {
                    multibuilderStack = minecraft.player.getInventory().getItem(slot);
                }
                
                if (multibuilderStack.is(MBTOOL.get())) {
                    multibuilderStack.getOrCreateTag().putInt("recipe", originalIndex);
                    
                    // Nullify runtimeStructure when player chooses a structure
                    MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
                    multibuilderItem.setRuntimeStructure(multibuilderStack, null);
                }
            }
            
            // Update the previous screen if it exists
            if (previousScreen instanceof MultibuilderScreen) {
                ((MultibuilderScreen)previousScreen).selectedStructure = originalIndex;
            }
            
            onClose();
        }
    }
}
