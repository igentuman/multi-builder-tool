package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.MultiblockButton;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.registration.MbtoolDataComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    private static final Identifier TEXTURE = rl("textures/gui/container/mbtool.png");
    private final Screen previousScreen;
    private int currentPage = 0;
    private int pages = 1;

    private Button prevButton;
    private Button nextButton;
    private EditBox searchField;

    private static final int GRID_COLUMNS = 3;
    private static final int GRID_ROWS = 2;
    private static final int BUTTON_SIZE = 64;
    private static final int BUTTON_SPACING = 2;
    private static final int STRUCTURES_PER_PAGE = GRID_COLUMNS * GRID_ROWS;

    private List<MultiblockStructure> allStructures = new ArrayList<>();
    private List<MultiblockStructure> filteredStructures = new ArrayList<>();
    private List<MultiblockButton> multiblockButtons = new ArrayList<>();
    private String currentFilter = "";

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        this(pMenu, pPlayerInventory, pTitle, null);
    }

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle, Screen previousScreen) {
        super(pMenu, pPlayerInventory, pTitle,
            Math.max(226, GRID_COLUMNS * BUTTON_SIZE + (GRID_COLUMNS - 1) * BUTTON_SPACING + 10),
            Math.max(186, GRID_ROWS * BUTTON_SIZE + (GRID_ROWS - 1) * BUTTON_SPACING + 20));
        this.previousScreen = previousScreen;
        loadStructures();
    }

    @Override
    protected void init() {
        super.init();

        int screenX = (this.width - this.imageWidth) / 2;
        int screenY = (this.height - this.imageHeight) / 2;

        multiblockButtons.clear();

        int searchFieldWidth = Math.min(200, this.imageWidth - 80);
        int searchFieldX = screenX + (this.imageWidth - searchFieldWidth) / 2;
        int searchFieldY = screenY + this.imageHeight - 28;

        this.searchField = new EditBox(this.font, searchFieldX, searchFieldY, searchFieldWidth, 16, Component.literal("Search structures..."));
        this.searchField.setHint(Component.literal("Search structures..."));
        this.searchField.setResponder(this::onSearchChanged);
        this.searchField.setFocused(true);
        this.addRenderableWidget(this.searchField);

        int gridWidth = GRID_COLUMNS * BUTTON_SIZE + (GRID_COLUMNS - 1) * BUTTON_SPACING;
        int gridStartX = screenX + (this.imageWidth - gridWidth) / 2;
        int gridStartY = screenY + 20;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int buttonX = gridStartX + col * (BUTTON_SIZE + BUTTON_SPACING);
                int buttonY = gridStartY + row * (BUTTON_SIZE + BUTTON_SPACING);

                MultiblockButton button = MultiblockButton.builder()
                        .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                        .structure(null)
                        .container(getMenu())
                        .onPress(this::onMultiblockButtonPressed)
                        .build();

                multiblockButtons.add(button);
                this.addRenderableWidget(button);
            }
        }

        this.prevButton = Button.builder(Component.literal("<"), button -> previousPage())
                .bounds(screenX + 10, screenY + this.imageHeight - 30, 20, 20)
                .build();

        this.nextButton = Button.builder(Component.literal(">"), button -> nextPage())
                .bounds(screenX + this.imageWidth - 30, screenY + this.imageHeight - 30, 20, 20)
                .build();

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);

        updateButtonStates();
        updateMultiblockButtons();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);
        // Renders widgets (buttons), labels, slots
        super.extractContents(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        List<Tooltip> savedTooltips = new ArrayList<>();
        for (MultiblockButton button : multiblockButtons) {
            savedTooltips.add(button.getTooltip());
            button.setTooltip(null);
        }

        super.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        for (int i = 0; i < multiblockButtons.size(); i++) {
            multiblockButtons.get(i).setTooltip(savedTooltips.get(i));
        }

        renderCustomTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderCustomTooltip(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY) {
        MultiblockButton hoveredButton = null;
        for (MultiblockButton button : multiblockButtons) {
            if (button.isMouseOver(pMouseX, pMouseY) && button.visible && button.getTooltip() != null) {
                hoveredButton = button;
                break;
            }
        }

        if (hoveredButton != null) {
            var tooltip = hoveredButton.getTooltip();
            if (tooltip != null) {
                int tooltipX = pMouseX;
                int tooltipY = pMouseY;
                pGuiGraphics.setTooltipForNextFrame(this.font, tooltip.toCharSequence(this.minecraft), tooltipX, tooltipY);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int pKeyCode = event.key();
        if (this.searchField != null && this.searchField.isFocused()) {
            if (this.searchField.keyPressed(event)) {
                return true;
            }
            if (pKeyCode != 256) {
                return true;
            }
        }

        if (this.searchField != null && this.searchField.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchField != null && this.searchField.isFocused()) {
            if (this.searchField.charTyped(event)) {
                return true;
            }
            return true;
        }

        if (this.searchField != null && this.searchField.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean b) {
        if (this.searchField.mouseClicked(event, b)) {
            return true;
        }
        return super.mouseClicked(event, b);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        FormattedCharSequence formattedcharsequence = title.getVisualOrderText();
        graphics.text(this.font, formattedcharsequence, 110 - this.font.width(formattedcharsequence) / 2, 6, 4210752, false);
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
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < pages - 1;
    }

    protected void onPageChanged() {
        updateMultiblockButtons();
    }

    public int getCurrentPage() { return currentPage; }

    public void setCurrentPage(int page) {
        if (page >= 0 && page < pages) {
            this.currentPage = page;
            updateButtonStates();
            onPageChanged();
        }
    }

    public void setPages(int pages) {
        this.pages = Math.max(1, pages);
        if (currentPage >= this.pages) currentPage = this.pages - 1;
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

    private void loadStructures() {
        allStructures = MultiblocksProvider.getStructures();
        filteredStructures = new ArrayList<>(allStructures);
        updatePagination();
    }

    private void onSearchChanged(String searchText) {
        currentFilter = searchText.toLowerCase().trim();
        applyFilter();
    }

    private void applyFilter() {
        if (currentFilter.isEmpty()) {
            filteredStructures = new ArrayList<>(allStructures);
        } else {
            filteredStructures = allStructures.stream()
                    .filter(structure -> structure.getName() != null &&
                            structure.getName().toLowerCase().contains(currentFilter))
                    .collect(Collectors.toList());
        }

        currentPage = 0;
        updatePagination();
        updateButtonStates();
        updateMultiblockButtons();
    }

    private void updatePagination() {
        if (filteredStructures.isEmpty()) {
            pages = 1;
        } else {
            pages = (int) Math.ceil((double) filteredStructures.size() / STRUCTURES_PER_PAGE);
        }

        if (currentPage >= pages) {
            currentPage = 0;
        }
    }

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

    private void onMultiblockButtonPressed(MultiblockButton button) {
        MultiblockStructure structure = button.getStructure();
        if (structure != null && minecraft != null && minecraft.player != null) {
            int originalIndex = allStructures.indexOf(structure);

            int slot = menu.getPlayerSlot();
            if (slot >= 0) {
                ItemStack multibuilderStack;
                if (slot == 40) {
                    multibuilderStack = minecraft.player.getOffhandItem();
                } else {
                    multibuilderStack = minecraft.player.getInventory().getItem(slot);
                }

                if (multibuilderStack.is(MBTOOL.get())) {
                    multibuilderStack.set(MbtoolDataComponents.STRUCTURE_RECIPE.get(), originalIndex);

                    MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
                    multibuilderItem.setRuntimeStructure(multibuilderStack, null);
                }
            }

            if (previousScreen instanceof MultibuilderScreen) {
                ((MultibuilderScreen)previousScreen).selectedStructure = originalIndex;
            }

            onClose();
        }
    }
}
