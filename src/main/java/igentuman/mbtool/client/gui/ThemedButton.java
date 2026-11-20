package igentuman.mbtool.client.gui;

import igentuman.mbtool.config.MbtoolClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ThemedButton extends Button {
    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!MbtoolClientConfig.isDarkMode()) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            return;
        }
        BrowserScrollPanel.bevel(graphics, getX(), getY(), width, height,
                !active ? 0xFF303030 : isHoveredOrFocused() ? 0xFF606060 : 0xFF484848, false);
        renderString(graphics, Minecraft.getInstance().font, active ? 0xFFFFFF : 0xA0A0A0);
    }
}
