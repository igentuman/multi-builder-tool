package igentuman.mbtool.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** A clipped, virtualized grid with an independent draggable vanilla-style scrollbar. */
public abstract class BrowserScrollPanel extends AbstractWidget {
    private static final int SCROLLBAR_WIDTH = 6;
    protected final int columns;
    protected final int cellWidth;
    protected final int cellHeight;
    protected final int gap;
    private int itemCount;
    private double scroll;
    private boolean draggingScrollbar;
    private double grabOffset;
    private int keyboardIndex = -1;

    protected BrowserScrollPanel(int x, int y, int width, int height, int columns, int cellHeight,
                                 int gap, Component narration) {
        super(x, y, width, height, narration);
        this.columns = columns;
        this.cellHeight = cellHeight;
        this.gap = gap;
        this.cellWidth = (width - SCROLLBAR_WIDTH - 4 - gap * (columns - 1)) / columns;
    }

    public void setItemCount(int count) {
        itemCount = count;
        keyboardIndex = count == 0 ? -1 : Mth.clamp(keyboardIndex, 0, count - 1);
        setScroll(0);
    }

    public double getScroll() {
        return scroll;
    }

    public void setScroll(double value) {
        scroll = Mth.clamp(value, 0, maxScroll());
    }

    private int contentHeight() {
        return Math.max(0, ((itemCount + columns - 1) / columns) * (cellHeight + gap) - gap);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private int thumbHeight() {
        return Math.min(height, Math.max(12, height * height / Math.max(height, contentHeight())));
    }

    private int thumbY() {
        return getY() + (maxScroll() == 0 ? 0 : (int) (scroll / maxScroll() * (height - thumbHeight())));
    }

    public int hoveredIndex(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY) || mouseX >= getX() + width - SCROLLBAR_WIDTH - 4) return -1;
        int localX = (int) (mouseX - getX());
        int localY = (int) (mouseY - getY() + scroll);
        int column = localX / (cellWidth + gap);
        int row = localY / (cellHeight + gap);
        if (column >= columns || localX % (cellWidth + gap) >= cellWidth
                || localY % (cellHeight + gap) >= cellHeight) return -1;
        int index = row * columns + column;
        return index < itemCount ? index : -1;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF737373);
        graphics.enableScissor(getX(), getY(), getX() + width - SCROLLBAR_WIDTH - 3, getY() + height);
        try {
            int firstRow = (int) scroll / (cellHeight + gap);
            int lastRow = Math.min((itemCount + columns - 1) / columns,
                    (int) (scroll + height) / (cellHeight + gap) + 1);
            int hovered = hoveredIndex(mouseX, mouseY);
            for (int row = firstRow; row < lastRow; row++) {
                for (int col = 0; col < columns; col++) {
                    int index = row * columns + col;
                    if (index >= itemCount) break;
                    renderEntry(graphics, index, getX() + col * (cellWidth + gap),
                            getY() + row * (cellHeight + gap) - (int) scroll,
                            cellWidth, cellHeight, hovered == index || (isFocused() && keyboardIndex == index));
                }
            }
        } finally {
            graphics.disableScissor();
        }
        int barX = getX() + width - SCROLLBAR_WIDTH;
        graphics.fill(barX, getY(), barX + SCROLLBAR_WIDTH, getY() + height, 0xFF373737);
        if (maxScroll() > 0) {
            int y = thumbY();
            bevel(graphics, barX, y, SCROLLBAR_WIDTH, thumbHeight(), 0xFFC6C6C6, false);
        }
    }

    public static void bevel(GuiGraphics graphics, int x, int y, int width, int height, int color, boolean selected) {
        graphics.fill(x, y, x + width, y + height, color);
        int light = selected ? 0xFF373737 : 0xFFFFFFFF;
        int dark = selected ? 0xFFFFFFFF : 0xFF373737;
        graphics.fill(x, y, x + width - 1, y + 1, light);
        graphics.fill(x, y, x + 1, y + height - 1, light);
        graphics.fill(x + width - 1, y, x + width, y + height, dark);
        graphics.fill(x, y + height - 1, x + width, y + height, dark);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (mouseX >= getX() + width - SCROLLBAR_WIDTH) {
            if (maxScroll() > 0) {
                draggingScrollbar = true;
                grabOffset = mouseY >= thumbY() && mouseY < thumbY() + thumbHeight()
                        ? mouseY - thumbY() : thumbHeight() / 2.0;
                dragScrollbar(mouseY);
            }
            return;
        }
        int index = hoveredIndex(mouseX, mouseY);
        if (index >= 0) {
            keyboardIndex = index;
            activateEntry(index);
        }
    }

    private void dragScrollbar(double mouseY) {
        int travel = height - thumbHeight();
        if (travel > 0) setScroll((mouseY - getY() - grabOffset) / travel * maxScroll());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar || button != 0) return false;
        dragScrollbar(mouseY);
        return true;
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        draggingScrollbar = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        setScroll(scroll - delta * Math.min(36, cellHeight + gap));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!isFocused() || itemCount == 0) return false;
        int next = Math.max(0, keyboardIndex);
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                activateEntry(next);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> next--;
            case GLFW.GLFW_KEY_RIGHT -> next++;
            case GLFW.GLFW_KEY_UP -> next -= columns;
            case GLFW.GLFW_KEY_DOWN -> next += columns;
            case GLFW.GLFW_KEY_PAGE_UP -> next -= Math.max(1, height / (cellHeight + gap)) * columns;
            case GLFW.GLFW_KEY_PAGE_DOWN -> next += Math.max(1, height / (cellHeight + gap)) * columns;
            case GLFW.GLFW_KEY_HOME -> next = 0;
            case GLFW.GLFW_KEY_END -> next = itemCount - 1;
            default -> { return false; }
        }
        keyboardIndex = Mth.clamp(next, 0, itemCount - 1);
        int entryY = keyboardIndex / columns * (cellHeight + gap);
        if (entryY < scroll) setScroll(entryY);
        else if (entryY + cellHeight > scroll + height) setScroll(entryY + cellHeight - height);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, keyboardIndex >= 0 ? entryNarration(keyboardIndex) : getMessage());
    }

    protected abstract Component entryNarration(int index);
    protected abstract void renderEntry(GuiGraphics graphics, int index, int x, int y, int width, int height, boolean hovered);
    protected abstract void activateEntry(int index);
}
