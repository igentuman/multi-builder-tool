package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static igentuman.mbtool.Mbtool.rl;
import static igentuman.mbtool.util.TextUtils.__;

/** Slot-based tool inventory. Reached from the structure browser; "Choose" returns to it. */
public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private Button chooseButton;
    public int selectedStructure = -1;
    private Screen structureBrowser;
    private boolean switchingView;

    public MultibuilderScreen(MultibuilderContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 216;
        this.imageHeight = 206;
    }

    public MultibuilderScreen(MultibuilderContainer menu, Inventory inventory, Component title, Screen structureBrowser) {
        this(menu, inventory, title);
        this.structureBrowser = structureBrowser;
    }

    @Override
    public void removed() {
        // A view change keeps the menu and its carried stack alive.
        if (!switchingView) super.removed();
        switchingView = false;
    }

    @Override
    protected void init() {
        super.init();
        loadSelectedStructure();

        int x = this.leftPos + 150;
        int y = this.topPos + 67;
        this.chooseButton = Button.builder(__("gui.mbtool.choose"), this::onChooseButtonClick)
                .bounds(x, y, 57, 17)
                .tooltip(Tooltip.create(__("gui.mbtool.select_structure")))
                .build();
        this.addRenderableWidget(this.chooseButton);
        chooseButton.active = menu.getCarried().isEmpty();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        chooseButton.active = menu.getCarried().isEmpty();
    }

    private ItemStack toolStack() {
        int slot = menu.getPlayerSlot();
        if (minecraft == null || minecraft.player == null || slot < 0) return ItemStack.EMPTY;
        return minecraft.player.getInventory().getItem(slot);
    }

    private void loadSelectedStructure() {
        ItemStack stack = toolStack();
        selectedStructure = stack.getItem() instanceof MultibuilderItem item ? item.getSelectedStructureId(stack) : -1;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (List.of(49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59).contains(pKeyCode)) {
            return false;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private void onChooseButtonClick(Button button) {
        if (minecraft == null || minecraft.player == null) return;
        // Keep a carried stack visible until the player places it in a slot.
        if (!menu.getCarried().isEmpty()) return;
        if (structureBrowser == null) {
            structureBrowser = new MultibuilderSelectStructureScreen<>(menu, minecraft.player.getInventory(),
                    __("gui.mbtool.select_structure"));
        }
        switchingView = true;
        minecraft.setScreen(structureBrowser);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        ItemStack multibuilderStack = toolStack();
        if (!(multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem)) return;
        MultiblockStructure structure = multibuilderItem.getCurrentStructure(multibuilderStack);
        if (structure != null) MultiblockRenderer.render(structure, pGuiGraphics, x + 148, y + 9, 60, 60);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.inventoryLabelX, 3, 4210752, false);
    }
}
