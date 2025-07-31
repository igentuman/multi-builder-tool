package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.common.MultiblocksProvider;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static igentuman.mbtool.Mbtool.rl;
import static igentuman.mbtool.Mbtool.MBTOOL;

public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private Button chooseButton;
    public int selectedStructure = -1;
    
    public MultibuilderScreen(MultibuilderContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 186;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        
        // Load selected structure from item NBT
        loadSelectedStructure();
        
        // Position the button in the GUI
        int x = this.leftPos + 119;
        int y = this.topPos + 67;
        int buttonWidth = 57;
        int buttonHeight = 17;
        
        this.chooseButton = Button.builder(
                Component.translatable("gui.mbtool.choose"),
            this::onChooseButtonClick
        ).bounds(x, y, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.mbtool.select_structure")))
                .build();
        
        this.addRenderableWidget(this.chooseButton);
    }
    
    private void loadSelectedStructure() {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().selected);
            
            if (multibuilderStack.is(MBTOOL.get()) && multibuilderStack.hasTag()) {
                int recipeIndex = multibuilderStack.getOrCreateTag().getInt("recipe");
                if (recipeIndex >= 0) {
                    // Ensure structures are loaded
                    if (MultiblocksProvider.structures.isEmpty()) {
                        MultiblocksProvider.loadMultiblockStructures();
                    }
                    
                    if (recipeIndex < MultiblocksProvider.structures.size()) {
                        selectedStructure = recipeIndex;
                    }
                }
            }
        }
    }
    
    private void onChooseButtonClick(Button button) {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            int slot = player.getInventory().selected;
            
            MultibuilderSelectStructureContainer container = new MultibuilderSelectStructureContainer(
                0,
                player.blockPosition(),
                player.getInventory(),
                slot
            );
            
            MultibuilderSelectStructureScreen newScreen = new MultibuilderSelectStructureScreen(
                container,
                player.getInventory(),
                Component.translatable("gui.mbtool.select_structure"),
                this
            );
            
            this.minecraft.setScreen(newScreen);
        }
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if(selectedStructure > -1) {
            List<MultiblockStructure> allStructures = MultiblocksProvider.loadMultiblockStructures();

            // Render the selected structure
            MultiblockRenderer.render(
                allStructures.get(selectedStructure),
                pGuiGraphics.pose(),
                x + 118, y + 9, 60, 60
            );
        }
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
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, 85, 4210752, false);
    }
}
