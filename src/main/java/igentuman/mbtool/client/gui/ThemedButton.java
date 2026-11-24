package igentuman.mbtool.client.gui;

import igentuman.mbtool.config.MbtoolClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ThemedButton extends Button {
    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!MbtoolClientConfig.isDarkMode()) {
            extractDefaultSprite(graphics);
            extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
            return;
        }
        BrowserScrollPanel.bevel(graphics, getX(), getY(), width, height,
                !active ? 0xFF303030 : isHoveredOrFocused() ? 0xFF606060 : 0xFF484848, false);
        var font = Minecraft.getInstance().font;
        String text = getMessage().getString();
        int maxWidth = width - 4;
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
        }
        graphics.centeredText(font, text, getX() + width / 2, getY() + (height - font.lineHeight) / 2 + 1,
                active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }
}
