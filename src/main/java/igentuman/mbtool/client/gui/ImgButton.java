package igentuman.mbtool.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import static igentuman.mbtool.Mbtool.MODID;

public class ImgButton extends GuiButton {
    private ItemStack stack;
    protected static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation(MODID,"textures/gui/container/widgets.png");

    public ImgButton(int buttonId, int x, int y, String buttonText, ItemStack stack) {
        super(buttonId, x, y, buttonText);
        this.stack = stack;
        this.width = 39;
        this.height = 39;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        RenderHelper.enableGUIStandardItemLighting();
        if (this.visible)
        {
            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            int i = this.getHoverState(this.hovered);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            this.drawTexturedModalRect(this.x, this.y, 0, 0 + i * 39, this.width, this.height);
            this.mouseDragged(mc, mouseX, mouseY);

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 0);
            GlStateManager.scale(1.5f, 1.5f, 1.5f);
            mc.getRenderItem().renderItemIntoGUI(stack, (int) (this.x/1.5f)+5 , (int) (this.y/1.5f)+5);
            GlStateManager.popMatrix();
        }
    }

}
