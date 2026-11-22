package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.BlockReplacementState;
import igentuman.mbtool.client.gui.BrowserScrollPanel;
import igentuman.mbtool.client.gui.GuiTheme;
import igentuman.mbtool.client.gui.ThemedButton;
import igentuman.mbtool.config.MbtoolClientConfig;
import igentuman.mbtool.util.BlockReplacementPreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class BlockReplacementsScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    private static final int SLOT = 24;
    private final MultibuilderSelectStructureScreen<T> parent;
    private List<BlockReplacementPreferences.Group> groups = List.of();
    private BlockReplacementPreferences choices = BlockReplacementPreferences.EMPTY;
    private BrowserScrollPanel panel;
    private int optionColumns;

    public BlockReplacementsScreen(MultibuilderSelectStructureScreen<T> parent) {
        super(Component.translatable("gui.mbtool.block_replacements"));
        this.parent = parent;
    }

    @Override
    public T getMenu() { return parent.getMenu(); }

    @Override
    protected void init() {
        double scroll = panel == null ? 0 : panel.getScroll();
        groups = BlockReplacementState.groups();
        choices = BlockReplacementState.snapshot();
        optionColumns = Math.max(1, (width - 48) / SLOT);
        int largest = groups.stream().mapToInt(group -> group.blocks().size() + 1).max().orElse(1);
        int rowHeight = 31 + ((largest + optionColumns - 1) / optionColumns) * SLOT;
        panel = addRenderableWidget(new BrowserScrollPanel(16, 53, width - 32, Math.max(24, height - 90),
                1, rowHeight, 6, title) {
            @Override
            protected Component entryNarration(int index) {
                return groupLabel(index).copy().append(". ").append(selectionLabel(groups.get(index)));
            }

            @Override
            protected void renderEntry(GuiGraphics graphics, int index, int x, int y, int w, int h, boolean hovered) {
                var group = groups.get(index);
                bevel(graphics, x, y, w, h, GuiTheme.color(0xFF999999, 0xFF303030), false);
                graphics.drawString(font, font.plainSubstrByWidth(groupLabel(index).getString(), w - 10), x + 5, y + 4, 0xFFFFFF);
                graphics.drawString(font, font.plainSubstrByWidth(selectionLabel(group).getString(), w - 10), x + 5, y + 16, 0xFFFFFF);
                for (int option = 0; option <= group.blocks().size(); option++) {
                    Block block = option == 0 ? null : group.blocks().get(option - 1);
                    int left = x + 5 + option % optionColumns * SLOT;
                    int top = y + 29 + option / optionColumns * SLOT;
                    boolean selected = block == null ? !choices.selections().containsKey(group.id())
                            : ForgeRegistries.BLOCKS.getKey(block).equals(choices.selections().get(group.id()));
                    bevel(graphics, left, top, SLOT - 2, SLOT - 2,
                            selected ? 0xFF3B969A : GuiTheme.color(0xFF777777, 0xFF484848), selected);
                    ItemStack stack = new ItemStack(block == null ? Items.BARRIER : block.asItem());
                    graphics.renderItem(stack, left + 3, top + 3);
                    if (selected) graphics.fill(left + 2, top + SLOT - 5, left + SLOT - 4, top + SLOT - 3, 0xFFFFFF80);
                }
            }

            @Override
            protected boolean clickEntry(int index, int x, int y, int w, int h, double mouseX, double mouseY) {
                int option = optionAt(index, mouseX, mouseY);
                if (option >= 0) select(index, option);
                return true;
            }

            @Override
            protected void activateEntry(int index) {
                var group = groups.get(index);
                int selected = -1;
                for (int i = 0; i < group.blocks().size(); i++) {
                    if (ForgeRegistries.BLOCKS.getKey(group.blocks().get(i)).equals(choices.selections().get(group.id()))) selected = i;
                }
                select(index, (selected + 2) % (group.blocks().size() + 1));
            }
        });
        panel.setItemCount(groups.size());
        panel.setScroll(scroll);
        addRenderableWidget(new ThemedButton(16, height - 29, 126, 20,
                Component.translatable("gui.mbtool.replacements.reset"), button -> {
            MbtoolClientConfig.clearReplacements(groups);
            choices = BlockReplacementState.snapshot();
        }));
        addRenderableWidget(new ThemedButton(width - 86, height - 29, 70, 20,
                Component.translatable("gui.done"), button -> onClose()));
    }

    private Component groupLabel(int index) {
        return Component.translatable("gui.mbtool.replacements.set", index + 1,
                groups.get(index).blocks().get(0).getName());
    }

    private Component selectionLabel(BlockReplacementPreferences.Group group) {
        var selected = choices.selections().get(group.id());
        return selected == null ? Component.translatable("gui.mbtool.replacements.none")
                : Component.translatable("gui.mbtool.replacements.preferred", ForgeRegistries.BLOCKS.getValue(selected).getName());
    }

    private int optionAt(int index, double mouseX, double mouseY) {
        int x = (int) mouseX - panel.entryX(index) - 5;
        int y = (int) mouseY - panel.entryY(index) - 29;
        if (x < 0 || y < 0 || x / SLOT >= optionColumns || x % SLOT >= SLOT - 2 || y % SLOT >= SLOT - 2) return -1;
        int option = y / SLOT * optionColumns + x / SLOT;
        return option <= groups.get(index).blocks().size() ? option : -1;
    }

    private void select(int index, int option) {
        var group = groups.get(index);
        var id = option == 0 ? null : ForgeRegistries.BLOCKS.getKey(group.blocks().get(option - 1));
        if (id != null && id.equals(choices.selections().get(group.id()))) id = null;
        if (option > 0 && group.blocks().get(option - 1).asItem() == Items.AIR) return;
        MbtoolClientConfig.selectReplacement(group.id(), id);
        choices = BlockReplacementState.snapshot();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        BrowserScrollPanel.bevel(graphics, 8, 8, width - 16, height - 16, GuiTheme.color(0xFFC6C6C6, 0xFF383838), false);
        graphics.drawCenteredString(font, title, width / 2, 15, GuiTheme.labelColor());
        graphics.drawCenteredString(font, font.plainSubstrByWidth(Component.translatable("gui.mbtool.replacements.help").getString(), width - 32),
                width / 2, 33, GuiTheme.labelColor());
        super.render(graphics, mouseX, mouseY, partialTick);
        if (groups.isEmpty()) graphics.drawCenteredString(font, Component.translatable("gui.mbtool.replacements.empty"),
                width / 2, height / 2, GuiTheme.labelColor());
        int group = panel.hoveredIndex(mouseX, mouseY);
        if (group >= 0) {
            int option = optionAt(group, mouseX, mouseY);
            if (option == 0) graphics.renderTooltip(font, Component.translatable("gui.mbtool.replacements.none"), mouseX, mouseY);
            else if (option > 0) {
                Block block = groups.get(group).blocks().get(option - 1);
                graphics.renderComponentTooltip(font, List.of(block.getName(), Component.literal(ForgeRegistries.BLOCKS.getKey(block).toString())), mouseX, mouseY);
            }
        }
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) return;
        if (minecraft.player.containerMenu != getMenu()) {
            minecraft.setScreen(null);
            return;
        }
        if (!minecraft.player.isAlive() || minecraft.player.isRemoved() || !getMenu().stillValid(minecraft.player)) {
            parent.onClose();
            return;
        }
        if (groups != BlockReplacementState.groups()) rebuildWidgets();
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
