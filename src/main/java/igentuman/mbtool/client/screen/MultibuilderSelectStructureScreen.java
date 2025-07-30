package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.MultiblockButton;
import igentuman.mbtool.common.MultiblocksProvider;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static igentuman.mbtool.Mbtool.rl;

public class MultibuilderSelectStructureScreen extends AbstractContainerScreen<MultibuilderSelectStructureContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool.png");
    private final Screen previousScreen;
    private int currentPage = 0;
    private int pages = 1;
    
    private Button prevButton;
    private Button nextButton;
    
    // Grid configuration
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_ROWS = 3;
    private static final int BUTTON_SIZE = 64;
    private static final int BUTTON_SPACING = 8;
    private static final int STRUCTURES_PER_PAGE = GRID_COLUMNS * GRID_ROWS;
    
    // Multiblock data
    private List<MultiblockStructure> allStructures = new ArrayList<>();
    private List<MultiblockButton> multiblockButtons = new ArrayList<>();

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        this(pMenu, pPlayerInventory, pTitle, null);
    }

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle, Screen previousScreen) {
        super(pMenu, pPlayerInventory, pTitle);
        
        // Calculate screen size based on grid
        int gridWidth = GRID_COLUMNS * BUTTON_SIZE + (GRID_COLUMNS - 1) * BUTTON_SPACING;
        int gridHeight = GRID_ROWS * BUTTON_SIZE + (GRID_ROWS - 1) * BUTTON_SPACING;
        
        this.imageWidth = Math.max(186, gridWidth + 40); // Add padding
        this.imageHeight = Math.max(186, gridHeight + 80); // Add padding for title and pagination
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
        
        // Calculate grid starting position (centered in the screen)
        int gridWidth = GRID_COLUMNS * BUTTON_SIZE + (GRID_COLUMNS - 1) * BUTTON_SPACING;
        int gridHeight = GRID_ROWS * BUTTON_SIZE + (GRID_ROWS - 1) * BUTTON_SPACING;
        int gridStartX = screenX + (this.imageWidth - gridWidth) / 2;
        int gridStartY = screenY + 30; // Leave space for title
        
        // Create multiblock buttons in a 3x3 grid
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int buttonX = gridStartX + col * (BUTTON_SIZE + BUTTON_SPACING);
                int buttonY = gridStartY + row * (BUTTON_SIZE + BUTTON_SPACING);
                
                MultiblockButton button = MultiblockButton.builder()
                        .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                        .structure(null) // Will be set when updating page
                        .onPress(this::onMultiblockButtonPressed)
                        .build();
                
                multiblockButtons.add(button);
                this.addRenderableWidget(button);
            }
        }
        
        // Previous button (left side)
        this.prevButton = Button.builder(Component.literal("<"), button -> previousPage())
                .bounds(screenX + 10, screenY + this.imageHeight - 25, 20, 20)
                .build();
        
        // Next button (right side)
        this.nextButton = Button.builder(Component.literal(">"), button -> nextPage())
                .bounds(screenX + this.imageWidth - 30, screenY + this.imageHeight - 25, 20, 20)
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
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.inventoryLabelX, 3, 4210752, false);
        
        // Draw page indicator
        String pageText = "Page " + (currentPage + 1) + "/" + pages;
        int pageTextWidth = this.font.width(pageText);
        int pageX = (this.imageWidth - pageTextWidth) / 2;
        graphics.drawString(this.font, pageText, pageX, this.imageHeight - 15, 4210752, false);
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
        allStructures = MultiblocksProvider.loadMultiblockStructures();
        
        // Calculate number of pages needed
        if (allStructures.isEmpty()) {
            pages = 1;
        } else {
            pages = (int) Math.ceil((double) allStructures.size() / STRUCTURES_PER_PAGE);
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
            
            if (structureIndex < allStructures.size()) {
                button.setStructure(allStructures.get(structureIndex));
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
        if (structure != null) {
            // TODO: Handle structure selection
            // For now, just close the screen or return to previous screen
            onClose();
        }
    }
}
