package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.GuiTheme;
import igentuman.mbtool.client.gui.ThemedButton;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.config.MbtoolClientConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static igentuman.mbtool.Mbtool.rl;
import static igentuman.mbtool.util.TextUtils.__;

/** Slot-based inventory view of the tool. Shares the live menu with the structure browser. */
public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final Identifier TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private Button chooseButton;
    private Screen structureBrowser;
    private boolean switchingView;

    public MultibuilderScreen(MultibuilderContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 216, 206);
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
        chooseButton = new ThemedButton(leftPos + 150, topPos + 67, 57, 17, __("gui.mbtool.choose"), this::onChooseButtonClick);
        chooseButton.setTooltip(Tooltip.create(__("gui.mbtool.select_structure")));
        addRenderableWidget(chooseButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // The player must place a carried stack before leaving the slot view.
        chooseButton.active = menu.getCarried().isEmpty();
    }

    private ItemStack toolStack() {
        int slot = menu.getPlayerSlot();
        return slot >= 0 && slot < playerInventoryRef().getContainerSize() ? playerInventoryRef().getItem(slot) : ItemStack.EMPTY;
    }

    private Inventory playerInventoryRef() {
        return minecraft.player.getInventory();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key >= 49 && key <= 59) {
            return false;
        }
        return super.keyPressed(event);
    }

    private void onChooseButtonClick(Button button) {
        if (minecraft == null || minecraft.player == null || !menu.getCarried().isEmpty()) return;
        if (structureBrowser == null) {
            structureBrowser = new MultibuilderSelectStructureScreen<>(menu, minecraft.player.getInventory(),
                    __("gui.mbtool.select_structure"));
        }
        switchingView = true;
        minecraft.setScreen(structureBrowser);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int tint = MbtoolClientConfig.isDarkMode() ? 0xFF525252 : 0xFFFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256, tint);
        ItemStack stack = toolStack();
        if (stack.getItem() instanceof MultibuilderItem item) {
            MultiblockStructure structure = item.getCurrentStructure(stack);
            if (structure != null) MultiblockRenderer.render(structure, graphics, leftPos + 148, topPos + 9, 60, 60);
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, inventoryLabelX, 3, GuiTheme.labelColor(), false);
    }
}
