package igentuman.mbtool.client.gui;

import igentuman.mbtool.client.render.GuiStructureRenderState;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MultiblockButton extends AbstractButton {
    private MultiblockStructure structure;
    private final OnPress onPress;
    private boolean renderBackground = true;
    private int backgroundColor = 0xFF8B8B8B;
    private int hoveredBackgroundColor = 0xFFA0A0A0;
    private int pressedBackgroundColor = 0xFF606060;
    private MultibuilderSelectStructureContainer container;
    private boolean hasAllIngredients = false;
    private Tooltip currentTooltip;

    @Override
    public void setTooltip(@Nullable Tooltip tooltip) {
        super.setTooltip(tooltip);
        this.currentTooltip = tooltip;
    }

    public Tooltip getTooltip() {
        return currentTooltip;
    }

    public MultiblockButton(int x, int y, int width, int height, MultiblockStructure structure, OnPress onPress) {
        this(x, y, width, height, structure, onPress, Component.empty());
    }

    public MultiblockButton(int x, int y, int width, int height, MultiblockStructure structure, OnPress onPress, Component message) {
        super(x, y, width, height, message);
        this.structure = structure;
        this.onPress = onPress;
        updateTooltip();
    }

    public void setStructure(MultiblockStructure structure) {
        this.structure = structure;
        updateTooltip();
    }

    private void setContainer(MultibuilderSelectStructureContainer container) {
        this.container = container;
    }

    public MultiblockStructure getStructure() {
        return this.structure;
    }

    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
    }

    public void setBackgroundColors(int normal, int hovered, int pressed) {
        this.backgroundColor = normal;
        this.hoveredBackgroundColor = hovered;
        this.pressedBackgroundColor = pressed;
    }

    void updateTooltip() {
        hasAllIngredients = true;
        if (structure != null && structure.getName() != null && !structure.getName().isEmpty()) {
            Component tooltip = Component.translatable(structure.getName())
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.mbtool.multiblock_button.dimensions",
                    structure.getWidth(), structure.getHeight(), structure.getDepth())
                    .withStyle(style -> style.withColor(0x808080)));
            List<Component> itemsTooltip = new ArrayList<>();
            if(container != null) {
                for(ItemStack stack: structure.getNeededItems()) {
                    boolean hasEnough = container.hasEnough(stack);
                    if(!hasEnough) {
                        hasAllIngredients = false;
                    }
                    ChatFormatting style = hasEnough ? ChatFormatting.GREEN : ChatFormatting.RED;
                    itemsTooltip.add(Component.literal(stack.getCount() + "x " + stack.getHoverName().getString()).withStyle(style));
                }
            }
            if(!itemsTooltip.isEmpty()) {
                tooltip = tooltip.copy().append(Component.literal("\n"));
                for(Component itemComponent : itemsTooltip) {
                    tooltip = tooltip.copy().append(Component.literal("\n")).append(itemComponent);
                }
            }
            setTooltip(Tooltip.create(tooltip));
        } else {
            setTooltip(null);
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.onPress(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        if (renderBackground) {
            int bgColor = backgroundColor;
            if (!this.active) {
                bgColor = 0xFF606060;
            } else if (this.isHoveredOrFocused()) {
                bgColor = hoveredBackgroundColor;
            }

            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            int borderColor;
            if (container != null && structure != null && !structure.getNeededItems().isEmpty()) {
                borderColor = hasAllIngredients ? 0xFF00FF00 : 0xFFFF0000;
            } else {
                borderColor = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF000000;
            }
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);
        }

        if (structure != null && !structure.getBlocks().isEmpty()) {
            int padding = renderBackground ? 3 : 1;
            int renderX = this.getX() + padding;
            int renderY = this.getY() + padding;
            int renderWidth = Math.max(1, this.width - (padding * 2));
            int renderHeight = Math.max(1, this.height - (padding * 2));
            renderStructureItems(guiGraphics, structure, renderX, renderY, renderWidth, renderHeight, 0f);
        } else {
            if (renderBackground) {
                int centerX = this.getX() + this.width / 2;
                int centerY = this.getY() + this.height / 2;
                guiGraphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.mbtool.no_structure").getString(),
                    centerX,
                    centerY - 4,
                    0x808080
                );
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (this.structure != null) {
            narrationElementOutput.add(NarratedElementType.TITLE,
                Component.translatable("gui.mbtool.multiblock_button.narration",
                    this.structure.getWidth(), this.structure.getHeight(), this.structure.getDepth()));
        } else {
            narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        }
    }

    /**
     * Renders a multiblock structure as a real 3D block mesh (via {@link GuiStructureRenderState})
     * inside the given bounds, isometric-tilted. angleY rotates the view around the Y axis (radians).
     */
    public static void renderStructureItems(GuiGraphicsExtractor guiGraphics, MultiblockStructure structure,
                                            int x, int y, int w, int h, float angleY) {
        if (structure.getBlocks().isEmpty() || w <= 0 || h <= 0) return;

        float scale = computeFitScale(structure.getWidth(), structure.getHeight(), structure.getDepth(), angleY, w, h);
        if (scale <= 0f) return;

        guiGraphics.submitPictureInPictureRenderState(
            new GuiStructureRenderState(structure, angleY, x, y, x + w, y + h, scale, guiGraphics.peekScissorStack())
        );
    }

    /**
     * Fits the structure's isometric-projected bounding box (same rotateX(30)+rotateY(225+angleY)
     * transform {@code GuiStructureRenderer} applies to the mesh) into a w x h pixel box, by
     * projecting the 8 bounding-box corners rather than approximating.
     */
    private static float computeFitScale(int width, int height, int depth, float angleY, int w, int h) {
        Matrix4f rotation = new Matrix4f()
            .rotateX((float) Math.toRadians(30))
            .rotateY((float) Math.toRadians(225) + angleY);

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector3f corner = new Vector3f();
        for (int cx = 0; cx <= 1; cx++) {
            for (int cy = 0; cy <= 1; cy++) {
                for (int cz = 0; cz <= 1; cz++) {
                    corner.set(cx * width - width / 2f, cy * height - height / 2f, cz * depth - depth / 2f);
                    rotation.transformPosition(corner);
                    minX = Math.min(minX, corner.x);
                    maxX = Math.max(maxX, corner.x);
                    minY = Math.min(minY, corner.y);
                    maxY = Math.max(maxY, corner.y);
                }
            }
        }

        float projW = maxX - minX;
        float projH = maxY - minY;
        float scale = Math.min((w - 4f) / Math.max(projW, 0.01f), (h - 4f) / Math.max(projH, 0.01f));
        return Math.min(scale, 24f);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(MultiblockButton button);
    }

    public static class Builder {
        private int x, y, width, height;
        private MultiblockStructure structure;
        private OnPress onPress;
        private Component message = Component.empty();
        private boolean renderBackground = true;
        private int backgroundColor = 0xFF8B8B8B;
        private int hoveredBackgroundColor = 0xFFA0A0A0;
        private int pressedBackgroundColor = 0xFF606060;
        private MultibuilderSelectStructureContainer container;

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            return this;
        }
        public Builder structure(MultiblockStructure structure) { this.structure = structure; return this; }
        public Builder onPress(OnPress onPress) { this.onPress = onPress; return this; }
        public Builder message(Component message) { this.message = message; return this; }
        public Builder renderBackground(boolean renderBackground) { this.renderBackground = renderBackground; return this; }
        public Builder backgroundColors(int normal, int hovered, int pressed) {
            this.backgroundColor = normal; this.hoveredBackgroundColor = hovered; this.pressedBackgroundColor = pressed;
            return this;
        }
        public Builder container(MultibuilderSelectStructureContainer menu) { this.container = menu; return this; }

        public MultiblockButton build() {
            MultiblockButton button = new MultiblockButton(x, y, width, height, structure, onPress, message);
            button.setRenderBackground(renderBackground);
            button.setBackgroundColors(backgroundColor, hoveredBackgroundColor, pressedBackgroundColor);
            button.setContainer(container);
            button.updateTooltip();
            return button;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
