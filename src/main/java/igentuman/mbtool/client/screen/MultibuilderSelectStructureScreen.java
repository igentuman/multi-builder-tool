package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.BrowserScrollPanel;
import igentuman.mbtool.client.gui.GuiTheme;
import igentuman.mbtool.client.gui.StructureBrowserModel;
import igentuman.mbtool.client.gui.ThemedButton;
import igentuman.mbtool.client.gui.StructureBrowserModel.Entry;
import igentuman.mbtool.client.gui.StructureBrowserModel.Folder;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.config.MbtoolClientConfig;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.NetworkHandler;
import igentuman.mbtool.network.RequestStructureCraftPacket;
import igentuman.mbtool.network.CancelCraftingJobsPacket;
import igentuman.mbtool.client.CraftingProgress;
import igentuman.mbtool.network.SelectStructurePacket;
import igentuman.mbtool.network.ToggleIgnoreCollisionsPacket;
import igentuman.mbtool.network.ToggleMeAccessPacket;
import igentuman.mbtool.network.ToggleMeAutocraftPacket;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;

import static igentuman.mbtool.util.TextUtils.__;

/** Both views share the live server menu. Switching views must never create a client-only inventory. */
public class MultibuilderSelectStructureScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    private final T menu;
    private final Inventory playerInventory;
    private final int playerSlot;
    private StructureBrowserModel<MultiblockStructure> model;
    private final Map<MultiblockStructure, List<ItemStack>> requiredItems = new IdentityHashMap<>();
    private List<Entry<MultiblockStructure>> filtered = List.of();
    private List<Folder> folders = List.of();
    private String query = "";
    private String folder;
    private int allMatches;
    private int totalStructures;
    private int sourceRevision = -1;
    private EditBox search;
    private ToggleButton autocraftButton;
    private BrowserScrollPanel folderPanel;
    private BrowserScrollPanel grid;
    private MultibuilderScreen inventoryScreen;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int selectedIndex = -1;
    private List<MultiblockStructure> sourceStructures = List.of();
    private int mouseX;
    private int mouseY;

    public MultibuilderSelectStructureScreen(T menu, Inventory inventory, Component title) {
        super(__("gui.mbtool.select_structure"));
        this.menu = menu;
        this.playerInventory = inventory;
        this.playerSlot = menu instanceof MultibuilderContainer container ? container.getPlayerSlot()
                : menu instanceof MultibuilderSelectStructureContainer container ? container.getPlayerSlot() : -1;
    }

    @Override
    public T getMenu() {
        return menu;
    }

    @Override
    protected void init() {
        double folderScroll = folderPanel == null ? 0 : folderPanel.getScroll();
        double gridScroll = grid == null ? 0 : grid.getScroll();
        panelX = 8;
        panelY = 8;
        panelWidth = width - 16;
        panelHeight = height - 16;
        int sidebarWidth = Math.min(160, Math.max(80, panelWidth / 5));
        int contentY = panelY + 48;
        int contentHeight = Math.max(24, panelHeight - 74);
        int gridX = panelX + sidebarWidth + 12;
        int gridWidth = panelWidth - sidebarWidth - 18;
        int columns = Math.max(1, (gridWidth - 6) / 100);
        int cardWidth = (gridWidth - 10 - (columns - 1) * 3) / columns;
        int cardHeight = Math.min(132, Math.max(84, cardWidth));
        MultiblockRenderer.setVisiblePreviewCount(columns * (contentHeight / (cardHeight + 3) + 2));

        addRenderableWidget(new ThemedButton(panelX + panelWidth - 82, panelY + 5, 76, 20,
                __("gui.mbtool.inventory"), button -> showInventory()));
        autocraftButton = null;
        if (ModUtil.isAe2Loaded()) {
            addRenderableWidget(new ToggleButton(panelX + panelWidth - 123, panelY + 6, "gui.mbtool.me_access",
                    "ae2:wireless_terminal", () -> meEnabled(false), () -> toggleMe(false), ChatFormatting.GREEN, false));
            autocraftButton = addRenderableWidget(new ToggleButton(panelX + panelWidth - 104, panelY + 6, "gui.mbtool.me_autocraft",
                    "minecraft:crafting_table", () -> meEnabled(true), () -> toggleMe(true), ChatFormatting.AQUA, true));
            autocraftButton.visible = meEnabled(false);
        }
        addRenderableWidget(new ToggleButton(panelX + panelWidth - (ModUtil.isAe2Loaded() ? 142 : 104), panelY + 6,
                "gui.mbtool.ignore_collisions", "minecraft:barrier", this::ignoreCollisions, this::toggleIgnoreCollisions,
                ChatFormatting.GREEN, false));
        addRenderableWidget(new ToggleButton(panelX + panelWidth - (ModUtil.isAe2Loaded() ? 161 : 123), panelY + 6,
                "gui.mbtool.dark_mode", "D", MbtoolClientConfig::isDarkMode,
                MbtoolClientConfig::toggleDarkMode, ChatFormatting.GREEN, false));
        search = new EditBox(font, panelX + 7, panelY + 28, panelWidth - 14, 16,
                __("gui.mbtool.search_structures"));
        search.setMaxLength(128);
        search.setHint(__("gui.mbtool.search_structures"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            applyFilter();
        });
        addRenderableWidget(search);
        folderPanel = addRenderableWidget(new BrowserScrollPanel(panelX + 6, contentY, sidebarWidth,
                contentHeight, 1, 16, 1, __("gui.mbtool.folders")) {
            @Override
            protected Component entryNarration(int index) {
                return folderLabel(index);
            }

            @Override
            protected void renderEntry(GuiGraphics graphics, int index, int x, int y, int w, int h, boolean hovered) {
                boolean selected = index == 0 ? folder == null : folders.get(index - 1).id().equals(folder);
                bevel(graphics, x, y, w, h, GuiTheme.color(
                        selected ? 0xFF8B8B8B : hovered ? 0xFFAAAAAA : 0xFF999999,
                        selected ? 0xFF505050 : hovered ? 0xFF606060 : 0xFF3C3C3C), selected);
                drawFitted(graphics, folderLabel(index).getString(), x + 4, y + (h - font.lineHeight) / 2, w - 8, selected ? 0xFFFFFF : 0xE0E0E0);
            }

            @Override
            protected void activateEntry(int index) {
                folder = index == 0 ? null : folders.get(index - 1).id();
                double scroll = getScroll();
                applyFilter();
                setScroll(scroll);
            }
        });
        grid = addRenderableWidget(new BrowserScrollPanel(gridX, contentY, gridWidth, contentHeight,
                columns, cardHeight, 3, __("gui.mbtool.select_structure")) {
            @Override
            protected Component entryNarration(int index) {
                return Component.literal(filtered.get(index).name());
            }

            @Override
            protected void renderEntry(GuiGraphics graphics, int index, int x, int y, int w, int h, boolean hovered) {
                Entry<MultiblockStructure> entry = filtered.get(index);
                boolean selected = entry.sourceIndex() == selectedIndex;
                bevel(graphics, x, y, w, h, GuiTheme.color(
                        hovered ? 0xFFA0A0A0 : 0xFF8B8B8B, hovered ? 0xFF555555 : 0xFF3C3C3C), selected);
                int previewHeight = h - 29;
                graphics.fill(x + 3, y + 3, x + w - 3, y + previewHeight, GuiTheme.color(0xFF686868, 0xFF282828));
                graphics.enableScissor(x + 3, Math.max(getY(), y + 3), x + w - 3,
                        Math.min(getY() + getHeight(), y + previewHeight));
                try {
                    MultiblockRenderer.render(entry.value(), graphics, x + 4, y + 4, w - 8, previewHeight - 6);
                } finally {
                    graphics.disableScissor();
                }
                drawFitted(graphics, entry.name(), x + 4, y + h - 24, w - 8, 0xFFFFFF);
                drawFitted(graphics, entry.folderName(), x + 4, y + h - 12, w - 8, 0xD0D0D0);
                CraftingProgress.Summary progress = CraftingProgress.forStructure(entry.value().getName());
                if (progress != null) {
                    int top = y + previewHeight - 25;
                    graphics.fill(x + 3, top - 2, x + w - 3, y + previewHeight, 0xC0000000);
                    drawFitted(graphics, progress.status().getString(), x + 5, top, w - 10, 0xB5FFFF);
                    drawFitted(graphics, progress.timing().getString(), x + 5, top + 11, w - 10, 0xFFFFFF);
                }
                if (hovered && (craftAvailable() || progress != null && progress.active())) renderCraftButton(graphics, x, y, w, progress);
            }

            @Override
            protected boolean clickEntry(int index, int x, int y, int w, int h, double mouseX, double mouseY) {
                CraftingProgress.Summary progress = CraftingProgress.forStructure(filtered.get(index).value().getName());
                if ((!craftAvailable() && (progress == null || !progress.active()))
                        || !overCraftButton(x, y, w, mouseX, mouseY)) return false;
                requestCraft(filtered.get(index));
                return true;
            }

            @Override
            protected void activateEntry(int index) {
                selectStructure(filtered.get(index));
            }
        });
        addRenderableWidget(new ThemedButton(panelX + panelWidth - 66, panelY + panelHeight - 23,
                60, 20, __("gui.done"), button -> onClose()));
        loadStructures();
        folderPanel.setScroll(folderScroll);
        grid.setScroll(gridScroll);
        setInitialFocus(search);
    }

    private Component folderLabel(int index) {
        if (index == 0) return __("gui.mbtool.all_structures", allMatches);
        Folder value = folders.get(index - 1);
        return Component.literal(value.name() + " (" + value.count() + ")");
    }

    private static String displayName(String key) {
        if (key == null || key.isBlank()) return "";
        String translated = __(key).getString();
        return translated.equals(key) ? TextUtils.convertToName(key.replace("mbtool.structure.", "")) : translated;
    }

    private void loadStructures() {
        sourceStructures = List.copyOf(MultiblocksProvider.getStructures());
        requiredItems.clear();
        List<Entry<MultiblockStructure>> entries = new ArrayList<>();
        for (int i = 0; i < sourceStructures.size(); i++) {
            MultiblockStructure structure = sourceStructures.get(i);
            if (structure.isGroup()) continue;
            String group = structure.getGroup();
            if (group == null || group.isBlank()) group = "default";
            String groupName = "default".equals(group) ? __("gui.mbtool.ungrouped").getString() : displayName(group);
            entries.add(new Entry<>(structure, i, displayName(structure.getName()), group, groupName));
        }
        totalStructures = entries.size();
        model = new StructureBrowserModel<>(entries);
        sourceRevision = MultiblocksProvider.getRevision();
        if (folder != null && model.folders("").stream().noneMatch(value -> value.id().equals(folder))) folder = null;
        applyFilter();
    }

    private void applyFilter() {
        if (model == null) return;
        filtered = model.filter(query, folder);
        folders = model.folders(query);
        allMatches = model.filter(query, null).size();
        if (grid != null) grid.setItemCount(filtered.size());
        if (folderPanel != null) folderPanel.setItemCount(folders.size() + 1);
    }

    private ItemStack toolStack() {
        return playerSlot >= 0 && playerSlot < playerInventory.getContainerSize()
                ? playerInventory.getItem(playerSlot) : ItemStack.EMPTY;
    }

    private void selectStructure(Entry<MultiblockStructure> entry) {
        ItemStack stack = toolStack();
        if (!(stack.getItem() instanceof MultibuilderItem)) return;
        // Synchronization can replace the provider list while the browser is open.
        List<MultiblockStructure> current = MultiblocksProvider.getStructures();
        if (entry.sourceIndex() >= current.size() || current.get(entry.sourceIndex()) != entry.value()) {
            loadStructures();
            return;
        }
        SelectStructurePacket selection = new SelectStructurePacket(menu.containerId, playerSlot, entry.sourceIndex());
        selection.applyTo(stack.getOrCreateTag());
        NetworkHandler.INSTANCE.sendToServer(selection);
        selectedIndex = entry.sourceIndex();
        onClose();
    }

    private boolean craftAvailable() {
        return ModUtil.isAe2Loaded() && meEnabled(false);
    }

    private int craftButtonWidth() {
        return Math.max(font.width(__("gui.mbtool.craft")), font.width(__("gui.mbtool.crafting.cancel"))) + 8;
    }

    private boolean overCraftButton(int x, int y, int w, double mouseX, double mouseY) {
        int left = x + w - craftButtonWidth() - 5;
        int top = y + 5;
        return mouseX >= left && mouseX < left + craftButtonWidth() && mouseY >= top && mouseY < top + 13;
    }

    private void renderCraftButton(GuiGraphics graphics, int x, int y, int w, CraftingProgress.Summary progress) {
        int width = craftButtonWidth();
        int left = x + w - width - 5;
        int top = y + 5;
        boolean hover = overCraftButton(x, y, w, mouseX, mouseY);
        BrowserScrollPanel.bevel(graphics, left, top, width, 13, hover ? 0xFF5F9EA0 : 0xFF3C6E71, false);
        graphics.drawCenteredString(font, __(progress != null && progress.active() ? "gui.mbtool.crafting.cancel" : "gui.mbtool.craft"), left + width / 2, top + 3, hover ? 0xFFFFA0 : 0xFFFFFF);
    }

    private void requestCraft(Entry<MultiblockStructure> entry) {
        CraftingProgress.Summary progress = CraftingProgress.forStructure(entry.value().getName());
        if (progress != null && progress.active()) {
            NetworkHandler.INSTANCE.sendToServer(new CancelCraftingJobsPacket(entry.value().getName()));
            return;
        }
        if (minecraft == null || !(toolStack().getItem() instanceof MultibuilderItem)) return;
        List<MultiblockStructure> current = MultiblocksProvider.getStructures();
        if (entry.sourceIndex() >= current.size() || current.get(entry.sourceIndex()) != entry.value()) {
            loadStructures();
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        NetworkHandler.INSTANCE.sendToServer(new RequestStructureCraftPacket(menu.containerId, playerSlot, entry.sourceIndex()));
    }

    private void showInventory() {
        if (minecraft == null) return;
        if (menu instanceof MultibuilderContainer container) {
            if (inventoryScreen == null) inventoryScreen = new MultibuilderScreen(container, playerInventory,
                    __("gui.mbtool.multibuilder"), this);
            minecraft.setScreen(inventoryScreen);
        }
    }

    private boolean meEnabled(boolean autocraft) {
        ItemStack stack = toolStack();
        return stack.getItem() instanceof MultibuilderItem && stack.hasTag()
                && stack.getTag().getBoolean(autocraft ? "meAutocrafting" : "meAccessAllowed");
    }

    private void toggleMe(boolean autocraft) {
        ItemStack stack = toolStack();
        if (!(stack.getItem() instanceof MultibuilderItem) || (autocraft && !meEnabled(false))) return;
        boolean enabled = !meEnabled(autocraft);
        InteractionHand hand = playerSlot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (autocraft) {
            MultibuilderItem.setMeAutocraftingEnabled(stack, enabled);
            NetworkHandler.INSTANCE.sendToServer(new ToggleMeAutocraftPacket(enabled, hand));
        } else {
            stack.getOrCreateTag().putBoolean("meAccessAllowed", enabled);
            NetworkHandler.INSTANCE.sendToServer(new ToggleMeAccessPacket(enabled, hand));
            if (autocraftButton != null) autocraftButton.visible = enabled;
        }
    }

    private boolean ignoreCollisions() {
        ItemStack stack = toolStack();
        return stack.getItem() instanceof MultibuilderItem && MultibuilderItem.isIgnoreCollisions(stack);
    }

    private void toggleIgnoreCollisions() {
        ItemStack stack = toolStack();
        if (!(stack.getItem() instanceof MultibuilderItem)) return;
        boolean enabled = !MultibuilderItem.isIgnoreCollisions(stack);
        MultibuilderItem.setIgnoreCollisions(stack, enabled);
        InteractionHand hand = playerSlot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        NetworkHandler.INSTANCE.sendToServer(new ToggleIgnoreCollisionsPacket(enabled, hand));
    }

    private final class ToggleButton extends Button {
        private final String key;
        private final BooleanSupplier state;
        private final ChatFormatting enabledColor;
        private final boolean notice;
        private final ItemStack icon;
        private final String iconText;
        private Boolean lastEnabled;

        private ToggleButton(int x, int y, String key, String iconIdOrText, BooleanSupplier state, Runnable toggle,
                             ChatFormatting enabledColor, boolean notice) {
            super(x, y, 18, 18, __(key), button -> toggle.run(), DEFAULT_NARRATION);
            this.key = key;
            this.state = state;
            this.enabledColor = enabledColor;
            this.notice = notice;
            iconText = iconIdOrText.contains(":") ? null : iconIdOrText;
            ResourceLocation id = iconText == null ? ResourceLocation.tryParse(iconIdOrText) : null;
            var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            icon = item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean enabled = state.getAsBoolean();
            if (lastEnabled == null || lastEnabled != enabled) {
                var tooltip = Component.empty().append(getMessage()).append("\n")
                        .append(__(key + ".descr").withStyle(ChatFormatting.GOLD));
                if (notice) tooltip.append("\n")
                        .append(__(key + ".notice").withStyle(ChatFormatting.RED));
                tooltip.append("\n").append(__(key + (enabled ? ".enabled" : ".disabled"))
                        .withStyle(enabled ? enabledColor : ChatFormatting.RED));
                setTooltip(Tooltip.create(tooltip));
                lastEnabled = enabled;
            }
            BrowserScrollPanel.bevel(graphics, getX(), getY(), width, height,
                    GuiTheme.color(isHoveredOrFocused() ? 0xFFAAAAAA : enabled ? 0xFF8B8B8B : 0xFFC6C6C6,
                            isHoveredOrFocused() ? 0xFF606060 : enabled ? 0xFF505050 : 0xFF3C3C3C), enabled);
            if (!icon.isEmpty()) graphics.renderItem(icon, getX() + 1, getY() + 1);
            if (iconText != null) graphics.drawCenteredString(font, iconText,
                    getX() + width / 2, getY() + (height - font.lineHeight) / 2, 0xFFFFFF);
        }
    }

    private void drawFitted(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        drawFitted(graphics, text, x, y, maxWidth, color, true);
    }

    private void drawFitted(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        if (font.width(text) > maxWidth) text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
        graphics.drawString(font, text, x, y, color, shadow);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        renderBackground(graphics);
        BrowserScrollPanel.bevel(graphics, panelX, panelY, panelWidth, panelHeight,
                GuiTheme.color(0xFFC6C6C6, 0xFF383838), false);
        drawFitted(graphics, title.getString(), panelX + 7, panelY + 11,
                panelWidth - (ModUtil.isAe2Loaded() ? 177 : 136), GuiTheme.labelColor(), false);
        ItemStack stack = toolStack();
        selectedIndex = stack.hasTag() && stack.getTag().contains("recipe") && !stack.getTag().contains("runtimeStructure")
                ? stack.getTag().getInt("recipe") : -1;
        if (autocraftButton != null) autocraftButton.visible = meEnabled(false);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawFitted(graphics, __("gui.mbtool.structure_count", filtered.size(),
                totalStructures).getString(),
                panelX + 7, panelY + panelHeight - 16, panelWidth - 80, GuiTheme.labelColor(), false);
        if (filtered.isEmpty()) graphics.drawCenteredString(font, __("gui.mbtool.no_matching_structures"),
                grid.getX() + grid.getWidth() / 2, grid.getY() + grid.getHeight() / 2, 0xFFFFFF);
        renderBrowserTooltip(graphics, mouseX, mouseY);
    }

    private void renderBrowserTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = grid.hoveredIndex(mouseX, mouseY);
        CraftingProgress.Summary hoveredProgress = index < 0 ? null
                : CraftingProgress.forStructure(filtered.get(index).value().getName());
        if (index >= 0 && (craftAvailable() || hoveredProgress != null && hoveredProgress.active())
                && overCraftButton(grid.entryX(index), grid.entryY(index), grid.getCellWidth(), mouseX, mouseY)) {
            CraftingProgress.Summary progress = CraftingProgress.forStructure(filtered.get(index).value().getName());
            if (progress != null && progress.active()) {
                graphics.renderComponentTooltip(font, List.of(progress.status(), progress.timing(),
                        __("gui.mbtool.crafting.cancel.descr").withStyle(ChatFormatting.GOLD)), mouseX, mouseY);
                return;
            }
            graphics.renderComponentTooltip(font, List.of(__("gui.mbtool.craft"),
                    __("gui.mbtool.craft.descr").withStyle(ChatFormatting.GOLD),
                    __("gui.mbtool.craft.notice").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (index >= 0) {
            Entry<MultiblockStructure> entry = filtered.get(index);
            MultiblockStructure structure = entry.value();
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(entry.name()));
            tooltip.add(Component.literal(entry.folderName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(__("gui.mbtool.multiblock_button.dimensions",
                    structure.getWidth(), structure.getHeight(), structure.getDepth()).withStyle(ChatFormatting.GRAY));
            ItemStack tool = toolStack();
            if (tool.getItem() instanceof MultibuilderItem item) {
                var inventory = item.getInventory(tool);
                for (ItemStack needed : requiredItems.computeIfAbsent(structure, MultiblockStructure::getNeededItems)) {
                    int available = 0;
                    for (int slot = 0; inventory != null && slot < inventory.getSlots(); slot++) {
                        ItemStack stored = inventory.getStackInSlot(slot);
                        if (ItemStack.isSameItemSameTags(stored, needed)) available += stored.getCount();
                    }
                    boolean enough = playerInventory.player.isCreative() || available >= needed.getCount();
                    tooltip.add(Component.literal(needed.getCount() + "x ").append(needed.getHoverName())
                            .withStyle(enough ? ChatFormatting.GREEN : ChatFormatting.RED));
                }
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } else {
            int folderIndex = folderPanel.hoveredIndex(mouseX, mouseY);
            if (folderIndex >= 0) graphics.renderTooltip(font, folderLabel(folderIndex), mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) return;
        if (!minecraft.player.isAlive() || minecraft.player.isRemoved() || !(toolStack().getItem() instanceof MultibuilderItem)) {
            onClose();
            return;
        }
        search.tick();
        if (sourceRevision != MultiblocksProvider.getRevision()) loadStructures();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (search.isFocused() && key != GLFW.GLFW_KEY_TAB) {
            search.keyPressed(key, scanCode, modifiers);
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scanCode) && !search.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
