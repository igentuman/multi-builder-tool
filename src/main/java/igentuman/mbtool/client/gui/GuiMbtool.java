package igentuman.mbtool.client.gui;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.common.container.ContainerMbtool;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;


public class GuiMbtool extends GuiContainer {
    private static final ResourceLocation background = new ResourceLocation(
            Mbtool.MODID, "textures/gui/container/mbtool.png"
    );

    private final ContainerMbtool container;

    public GuiMbtool(ContainerMbtool inventorySlotsIn) {
        super(inventorySlotsIn);
        this.container = inventorySlotsIn;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)this.guiLeft, (float)this.guiTop, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();

        GlStateManager.popMatrix();
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("container.mbtool"), 8, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.mbtool.energy", container.getEnergyStored()), 8, 16, 4210752);
        this.fontRenderer.drawString(I18n.format("container.mbtool.recipe", container.getCurrentRecipeName()), 8, 26, 4210752);
    }
}
