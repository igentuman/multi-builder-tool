package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.BrowserScrollPanel;
import igentuman.mbtool.client.gui.StructureBrowserModel;
import igentuman.mbtool.client.gui.StructureBrowserModel.Entry;
import igentuman.mbtool.client.gui.StructureBrowserModel.Folder;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.SelectStructurePacket;
import igentuman.mbtool.network.ToggleMeAccessPacket;
import igentuman.mbtool.network.ToggleMeAutocraftPacket;
import igentuman.mbtool.registration.MbtoolDataComponents;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static igentuman.mbtool.util.TextUtils.__;

/**
 * Structure browser. Opened first when the tool GUI opens; it shares the live server menu with the
 * inventory view, so switching views never creates a client-only inventory.
 */
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
    private MeToggleButton autocraftButton;
    private BrowserScrollPanel folderPanel;
    private BrowserScrollPanel grid;
    private MultibuilderScreen inventoryScreen;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int selectedIndex = -1;

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

    private static boolean meAvailable() {
        return ModUtil.isAe2Loaded() && MbtoolConfig.isAe2IntegrationEnabled();
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

        Button inventoryButton = addRenderableWidget(Button.builder(__("gui.mbtool.inventory"), button -> showInventory())
                .bounds(panelX + panelWidth - 82, panelY + 5, 76, 20).build());
        inventoryButton.active = menu instanceof MultibuilderContainer;
        autocraftButton = null;
        if (meAvailable()) {
            addRenderableWidget(new MeToggleButton(panelX + panelWidth - 123, panelY + 6, false));
            autocraftButton = addRenderableWidget(new MeToggleButton(panelX + panelWidth - 104, panelY + 6, true));
            autocraftButton.visible = meEnabled(false);
        }
        search = new EditBox(font, panelX + 7, panelY + 28, panelWidth - 14, 16,
                __("gui.mbtool.search_structures"));
        search.setMaxLength(128);
        search.setHint(__("gui.mbtool.search_structures").withStyle(ChatFormatting.DARK_GRAY));
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
                bevel(graphics, x, y, w, h, selected ? 0xFF8B8B8B : hovered ? 0xFFAAAAAA : 0xFF999999, selected);
                drawFitted(graphics, folderLabel(index).getString(), x + 4, y + (h - font.lineHeight) / 2 + 1, w - 8,
                        selected ? 0xFFFFFFFF : 0xFFE0E0E0);
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
                bevel(graphics, x, y, w, h, hovered ? 0xFFA0A0A0 : 0xFF8B8B8B, selected);
                int previewHeight = h - 29;
                graphics.fill(x + 3, y + 3, x + w - 3, y + previewHeight, 0xFF686868);
                graphics.enableScissor(x + 3, Math.max(getY(), y + 3), x + w - 3,
                        Math.min(getY() + getHeight(), y + previewHeight));
                try {
                    MultiblockRenderer.render(entry.value(), graphics, x + 4, y + 4, w - 8, previewHeight - 6);
                } finally {
                    graphics.disableScissor();
                }
                drawFitted(graphics, entry.name(), x + 4, y + h - 24, w - 8, 0xFFFFFFFF);
                drawFitted(graphics, entry.folderName(), x + 4, y + h - 12, w - 8, 0xFFD0D0D0);
            }

            @Override
            protected void activateEntry(int index) {
                selectStructure(filtered.get(index));
            }
        });
        addRenderableWidget(Button.builder(__("gui.done"), button -> onClose())
                .bounds(panelX + panelWidth - 66, panelY + panelHeight - 23, 60, 20).build());
        loadStructures();
        folderPanel.setScroll(folderScroll);
        grid.setScroll(gridScroll);
        setInitialFocus(search);
    }

    @Override
    protected void setInitialFocus() {
        // Keyboard-driven re-initialisation would otherwise tab focus away from the search field.
        if (search != null) setInitialFocus(search);
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
        List<MultiblockStructure> sourceStructures = List.copyOf(MultiblocksProvider.getStructures());
        requiredItems.clear();
        List<Entry<MultiblockStructure>> entries = new ArrayList<>();
        for (int i = 0; i < sourceStructures.size(); i++) {
            MultiblockStructure structure = sourceStructures.get(i);
            String group = structure.getGroup();
            if (group == null || group.isBlank()) group = MultiblockStructure.DEFAULT_GROUP;
            String groupName = MultiblockStructure.DEFAULT_GROUP.equals(group)
                    ? __("gui.mbtool.ungrouped").getString() : displayName(group);
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
        if (sourceRevision != MultiblocksProvider.getRevision() || entry.sourceIndex() >= current.size()
                || current.get(entry.sourceIndex()) != entry.value()) {
            loadStructures();
            return;
        }
        SelectStructurePacket selection = new SelectStructurePacket(menu.containerId, playerSlot, entry.sourceIndex());
        selection.applyTo(stack);
        PacketDistributor.sendToServer(selection);
        selectedIndex = entry.sourceIndex();
        onClose();
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
        if (!(stack.getItem() instanceof MultibuilderItem)) return false;
        return autocraft ? MultibuilderItem.isMeAutocraftingEnabled(stack)
                : stack.getOrDefault(MbtoolDataComponents.ME_ACCESS.get(), false);
    }

    private void toggleMe(boolean autocraft) {
        ItemStack stack = toolStack();
        if (!meAvailable() || !(stack.getItem() instanceof MultibuilderItem) || (autocraft && !meEnabled(false))) return;
        boolean enabled = !meEnabled(autocraft);
        InteractionHand hand = playerSlot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (autocraft) {
            MultibuilderItem.setMeAutocraftingEnabled(stack, enabled);
            PacketDistributor.sendToServer(new ToggleMeAutocraftPacket(enabled, hand));
        } else {
            stack.set(MbtoolDataComponents.ME_ACCESS.get(), enabled);
            PacketDistributor.sendToServer(new ToggleMeAccessPacket(enabled, hand));
            if (autocraftButton != null) autocraftButton.visible = enabled;
        }
    }

    private final class MeToggleButton extends Button {
        private final boolean autocraft;
        private final ItemStack icon;
        private Boolean lastEnabled;

        private MeToggleButton(int x, int y, boolean autocraft) {
            super(x, y, 18, 18, __(autocraft ? "gui.mbtool.me_autocraft" : "gui.mbtool.me_access"),
                    button -> toggleMe(autocraft), DEFAULT_NARRATION);
            this.autocraft = autocraft;
            icon = BuiltInRegistries.ITEM.getOptional(autocraft
                            ? ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table")
                            : ResourceLocation.fromNamespaceAndPath("ae2", "wireless_terminal"))
                    .map(ItemStack::new).orElse(ItemStack.EMPTY);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean enabled = meEnabled(autocraft);
            if (lastEnabled == null || lastEnabled != enabled) {
                String key = autocraft ? "gui.mbtool.me_autocraft" : "gui.mbtool.me_access";
                MutableComponent tooltip = Component.empty().append(getMessage()).append("\n")
                        .append(__(key + ".descr").withStyle(ChatFormatting.GOLD));
                if (autocraft) tooltip.append("\n")
                        .append(__(key + ".notice").withStyle(ChatFormatting.RED));
                tooltip.append("\n").append(__(key + (enabled ? ".enabled" : ".disabled"))
                        .withStyle(enabled ? (autocraft ? ChatFormatting.AQUA : ChatFormatting.GREEN) : ChatFormatting.RED));
                setTooltip(Tooltip.create(tooltip));
                lastEnabled = enabled;
            }
            BrowserScrollPanel.bevel(graphics, getX(), getY(), width, height,
                    isHoveredOrFocused() ? 0xFFAAAAAA : enabled ? 0xFF8B8B8B : 0xFFC6C6C6, enabled);
            if (!icon.isEmpty()) graphics.renderItem(icon, getX() + 1, getY() + 1);
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        BrowserScrollPanel.bevel(graphics, panelX, panelY, panelWidth, panelHeight, 0xFFC6C6C6, false);
        drawFitted(graphics, title.getString(), panelX + 7, panelY + 11,
                panelWidth - (meAvailable() ? 139 : 98), 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ItemStack stack = toolStack();
        selectedIndex = stack.has(MbtoolDataComponents.RUNTIME_STRUCTURE.get())
                ? -1 : stack.getOrDefault(MbtoolDataComponents.STRUCTURE_RECIPE.get(), -1);
        if (autocraftButton != null) autocraftButton.visible = meEnabled(false);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawFitted(graphics, __("gui.mbtool.structure_count", filtered.size(), totalStructures).getString(),
                panelX + 7, panelY + panelHeight - 16, panelWidth - 80, 0xFF404040, false);
        if (filtered.isEmpty()) graphics.drawCenteredString(font, __("gui.mbtool.no_matching_structures"),
                grid.getX() + grid.getWidth() / 2, grid.getY() + grid.getHeight() / 2, 0xFFFFFFFF);
        renderBrowserTooltip(graphics, mouseX, mouseY);
    }

    private void renderBrowserTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = grid.hoveredIndex(mouseX, mouseY);
        if (index >= 0) {
            Entry<MultiblockStructure> entry = filtered.get(index);
            MultiblockStructure structure = entry.value();
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(entry.name()));
            tooltip.add(Component.literal(entry.folderName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(__("gui.mbtool.multiblock_button.dimensions",
                    structure.getWidth(), structure.getHeight(), structure.getDepth()).withStyle(ChatFormatting.GRAY));
            ItemStack tool = toolStack();
            if (tool.getItem() instanceof MultibuilderItem item) {
                IItemHandler inventory = item.getInventory(tool);
                for (ItemStack needed : requiredItems.computeIfAbsent(structure, MultiblockStructure::getNeededItems)) {
                    int available = 0;
                    for (int slot = 0; inventory != null && slot < inventory.getSlots(); slot++) {
                        ItemStack stored = inventory.getStackInSlot(slot);
                        if (ItemStack.isSameItemSameComponents(stored, needed)) available += stored.getCount();
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
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        if (minecraft.player != null) minecraft.player.closeContainer();
        else minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
