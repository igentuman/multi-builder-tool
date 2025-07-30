package igentuman.mbtool.client.screen;

import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import static igentuman.mbtool.Mbtool.rl;

public class MultibuilderSelectStructureScreen extends AbstractContainerScreen<MultibuilderSelectStructureContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool_inventory.png");

    public MultibuilderSelectStructureScreen(MultibuilderSelectStructureContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 186;
        this.imageHeight = 186;
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
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, 85, 4210752, false);
    }
}
