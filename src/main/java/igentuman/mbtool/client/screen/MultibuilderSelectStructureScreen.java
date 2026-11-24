package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.BlockReplacementState;
import igentuman.mbtool.client.CraftingProgress;
import igentuman.mbtool.client.gui.BrowserScrollPanel;
import igentuman.mbtool.client.gui.GuiTheme;
import igentuman.mbtool.client.gui.StructureBrowserModel;
import igentuman.mbtool.client.gui.StructureBrowserModel.Entry;
import igentuman.mbtool.client.gui.StructureBrowserModel.Folder;
import igentuman.mbtool.client.gui.ThemedButton;
import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.config.MbtoolClientConfig;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.CancelCraftingJobsPacket;
import igentuman.mbtool.network.RequestStructureCraftPacket;
import igentuman.mbtool.network.SelectStructurePacket;
import igentuman.mbtool.network.ToggleIgnoreCollisionsPacket;
import igentuman.mbtool.network.ToggleMeAccessPacket;
import igentuman.mbtool.network.ToggleMeAutocraftPacket;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static igentuman.mbtool.util.TextUtils.__;


@SuppressWarnings("removal")
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

    private static boolean ae2Available() {
        return ModUtil.isAe2Loaded() && MbtoolConfig.isAe2IntegrationEnabled();
    }

    @Override
    protected void init() {
        double folderScroll = folderPanel == null ? 0 : folderPanel.getScroll();
        double gridScroll = grid == null ? 0 : grid.getScroll();
        boolean ae2 = ae2Available();
        panelX = 8;
        panelY = 8;
        panelWidth = width - 16;
        panelHeight = height - 16;
        int sidebarWidth = Math.min(160, Math.max(80, panelWidth / 5));
        int contentY = panelY + 48;
        int contentHeight = Math.max(24, panelHeight - 74);
        int gridX = panelX + sidebarWidth + 22;
        int gridWidth = panelWidth - sidebarWidth - 28;
        int columns = Math.max(1, (gridWidth - 6) / 100);
        int cardWidth = (gridWidth - 10 - (columns - 1) * 3) / columns;
        int cardHeight = Math.min(132, Math.max(84, cardWidth));

        // Header: title left; dark mode, ignore collisions, ME controls, Inventory right.
        int right = panelX + panelWidth - 6;
        if (menu instanceof MultibuilderContainer) {
            addRenderableWidget(new ThemedButton(right - 76, panelY + 5, 76, 20,
                    __("gui.mbtool.inventory"), button -> showInventory()));
            right -= 80;
        }
        autocraftButton = null;
        if (ae2) {
            autocraftButton = addRenderableWidget(new ToggleButton(right - 18, panelY + 6, "gui.mbtool.me_autocraft",
                    "minecraft:crafting_table", () -> meEnabled(true), () -> toggleMe(true), ChatFormatting.AQUA, true));
            autocraftButton.visible = meEnabled(false);
            right -= 19;
            addRenderableWidget(new ToggleButton(right - 18, panelY + 6, "gui.mbtool.me_access",
                    "ae2:wireless_terminal", () -> meEnabled(false), () -> toggleMe(false), ChatFormatting.GREEN, false));
            right -= 19;
        }
        addRenderableWidget(new ToggleButton(right - 18, panelY + 6,
                "gui.mbtool.ignore_collisions", "minecraft:barrier", this::ignoreCollisions, this::toggleIgnoreCollisions,
                ChatFormatting.GREEN, false));
        right -= 19;
        addRenderableWidget(new ToggleButton(right - 18, panelY + 6,
                "gui.mbtool.dark_mode", "D", MbtoolClientConfig::isDarkMode,
                MbtoolClientConfig::toggleDarkMode, ChatFormatting.GREEN, false));

        search = new EditBox(font, panelX + 7, panelY + 28, panelWidth - 14, 16, __("gui.mbtool.search_structures"));
        search.setMaxLength(128);
        search.setHint(__("gui.mbtool.search_structures"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            applyFilter();
        });
        addRenderableWidget(search);

        folderPanel = addRenderableWidget(new BrowserScrollPanel(panelX + 6, contentY, sidebarWidth + 12,
                contentHeight, 1, 14, 1, __("gui.mbtool.folders")) {
            @Override
            protected Component entryNarration(int index) {
                return folderLabel(index);
            }

            @Override
            protected void renderEntry(GuiGraphicsExtractor graphics, int index, int x, int y, int w, int h, boolean hovered) {
                boolean selected = index == 0 ? folder == null : folders.get(index - 1).id().equals(folder);
                bevel(graphics, x, y, w, h, GuiTheme.color(
                        selected ? 0xFF8B8B8B : hovered ? 0xFFAAAAAA : 0xFF999999,
                        selected ? 0xFF505050 : hovered ? 0xFF606060 : 0xFF3C3C3C), selected);
                drawFitted(graphics, folderLabel(index).getString(), x + 4, y + (h - font.lineHeight) / 2 + 1, w - 6,
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
            protected void renderEntry(GuiGraphicsExtractor graphics, int index, int x, int y, int w, int h, boolean hovered) {
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
                drawFitted(graphics, entry.name(), x + 4, y + h - 24, w - 8, 0xFFFFFFFF);
                drawFitted(graphics, entry.folderName(), x + 4, y + h - 12, w - 8, 0xFFD0D0D0);
                CraftingProgress.Summary progress = CraftingProgress.forStructure(entry.value().getName());
                if (progress != null) {
                    int top = y + previewHeight - 25;
                    graphics.fill(x + 3, top - 2, x + w - 3, y + previewHeight, 0xC0000000);
                    drawFitted(graphics, progress.status().getString(), x + 5, top, w - 10, 0xFFB5FFFF);
                    drawFitted(graphics, progress.timing().getString(), x + 5, top + 11, w - 10, 0xFFFFFFFF);
                }
                if (hovered && craftActionVisible(progress)) renderCraftButton(graphics, x, y, w, progress);
            }

            @Override
            protected boolean clickEntry(int index, int x, int y, int w, int h, double mouseX, double mouseY) {
                CraftingProgress.Summary progress = CraftingProgress.forStructure(filtered.get(index).value().getName());
                if (!craftActionVisible(progress) || !overCraftButton(x, y, w, mouseX, mouseY)) return false;
                requestCraft(filtered.get(index));
                return true;
            }

            @Override
            protected void activateEntry(int index) {
                selectStructure(filtered.get(index));
            }
        });
        MultiblockRenderer.setVisiblePreviewCount(grid.visibleCapacity());

        addRenderableWidget(new ThemedButton(panelX + panelWidth - 202, panelY + panelHeight - 23,
                132, 20, __("gui.mbtool.block_replacements"),
                button -> minecraft.setScreen(new BlockReplacementsScreen<>(this))));
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

    /** Prefer translations; turn untranslated identifiers into readable names. */
    private static String displayName(String key) {
        if (key == null || key.isBlank()) return "";
        String translated = __(key).getString();
        return translated.equals(key) ? TextUtils.convertToName(key.replace("mbtool.structure.", "")) : translated;
    }

    private static String folderDisplayName(String group) {
        String key = "gui.mbtool.folder." + group;
        String translated = __(key).getString();
        return translated.equals(key) ? displayName(group) : translated;
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
                    ? __("gui.mbtool.ungrouped").getString() : folderDisplayName(group);
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
        if (folderPanel != null) {
            double scroll = folderPanel.getScroll();
            folderPanel.setItemCount(folders.size() + 1);
            folderPanel.setScroll(scroll);
        }
    }

    private ItemStack toolStack() {
        return playerSlot >= 0 && playerSlot < playerInventory.getContainerSize()
                ? playerInventory.getItem(playerSlot) : ItemStack.EMPTY;
    }

    private InteractionHand toolHand() {
        return playerSlot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Identity check against the live provider list; a stale result list is reloaded instead. */
    private boolean isCurrent(Entry<MultiblockStructure> entry) {
        List<MultiblockStructure> current = MultiblocksProvider.getStructures();
        return entry.sourceIndex() < current.size() && current.get(entry.sourceIndex()) == entry.value();
    }

    private void selectStructure(Entry<MultiblockStructure> entry) {
        ItemStack stack = toolStack();
        if (!(stack.getItem() instanceof MultibuilderItem)) return;
        if (!isCurrent(entry)) {
            loadStructures();
            return;
        }
        SelectStructurePacket selection = new SelectStructurePacket(menu.containerId, playerSlot, entry.sourceIndex());
        selection.applyTo(stack);
        ClientPacketDistributor.sendToServer(selection);
        selectedIndex = entry.sourceIndex();
        onClose();
    }

    private boolean craftAvailable() {
        return ae2Available() && meEnabled(false);
    }

    /** Craft while ME access is on; Cancel stays available for an active job even if ME access was disabled. */
    private boolean craftActionVisible(CraftingProgress.Summary progress) {
        return ModUtil.isAe2Loaded() && (craftAvailable() || progress != null && progress.active());
    }

    private int craftButtonWidth() {
        return Math.max(font.width(__("gui.mbtool.craft")), font.width(__("gui.mbtool.crafting.cancel"))) + 8;
    }

    private boolean overCraftButton(int x, int y, int w, double mouseX, double mouseY) {
        int left = x + w - craftButtonWidth() - 5;
        int top = y + 5;
        return mouseX >= left && mouseX < left + craftButtonWidth() && mouseY >= top && mouseY < top + 13;
    }

    private void renderCraftButton(GuiGraphicsExtractor graphics, int x, int y, int w, CraftingProgress.Summary progress) {
        int width = craftButtonWidth();
        int left = x + w - width - 5;
        int top = y + 5;
        boolean hover = overCraftButton(x, y, w, mouseX, mouseY);
        BrowserScrollPanel.bevel(graphics, left, top, width, 13, hover ? 0xFF5F9EA0 : 0xFF3C6E71, false);
        graphics.centeredText(font, __(progress != null && progress.active() ? "gui.mbtool.crafting.cancel" : "gui.mbtool.craft"),
                left + width / 2, top + 3, hover ? 0xFFFFFFA0 : 0xFFFFFFFF);
    }

    private void requestCraft(Entry<MultiblockStructure> entry) {
        CraftingProgress.Summary progress = CraftingProgress.forStructure(entry.value().getName());
        if (progress != null && progress.active()) {
            ClientPacketDistributor.sendToServer(new CancelCraftingJobsPacket(entry.value().getName()));
            return;
        }
        if (minecraft == null || !(toolStack().getItem() instanceof MultibuilderItem)) return;
        if (!isCurrent(entry)) {
            loadStructures();
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        ClientPacketDistributor.sendToServer(new RequestStructureCraftPacket(menu.containerId, playerSlot,
                entry.sourceIndex(), BlockReplacementState.snapshot()));
    }

    private void showInventory() {
        if (minecraft == null) return;
        if (menu instanceof MultibuilderContainer container) {
            if (inventoryScreen == null) {
                inventoryScreen = new MultibuilderScreen(container, playerInventory, __("gui.mbtool.multibuilder"), this);
            }
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
        if (!(stack.getItem() instanceof MultibuilderItem) || (autocraft && !meEnabled(false))) return;
        boolean enabled = !meEnabled(autocraft);
        if (autocraft) {
            MultibuilderItem.setMeAutocraftingEnabled(stack, enabled);
            ClientPacketDistributor.sendToServer(new ToggleMeAutocraftPacket(enabled, toolHand()));
        } else {
            stack.set(MbtoolDataComponents.ME_ACCESS.get(), enabled);
            ClientPacketDistributor.sendToServer(new ToggleMeAccessPacket(enabled, toolHand()));
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
        ClientPacketDistributor.sendToServer(new ToggleIgnoreCollisionsPacket(enabled, toolHand()));
    }

    /** Small header toggle: item icon or letter, bevel showing state, tooltip with description and state. */
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
            Identifier id = iconText == null ? Identifier.tryParse(iconIdOrText) : null;
            icon = id == null ? ItemStack.EMPTY
                    : BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean enabled = state.getAsBoolean();
            if (lastEnabled == null || lastEnabled != enabled) {
                MutableComponent tooltip = Component.empty().append(getMessage()).append("\n")
                        .append(__(key + ".descr").withStyle(ChatFormatting.GOLD));
                if (notice) tooltip.append("\n").append(__(key + ".notice").withStyle(ChatFormatting.RED));
                tooltip.append("\n").append(__(key + (enabled ? ".enabled" : ".disabled"))
                        .withStyle(enabled ? enabledColor : ChatFormatting.RED));
                setTooltip(Tooltip.create(tooltip));
                lastEnabled = enabled;
            }
            BrowserScrollPanel.bevel(graphics, getX(), getY(), width, height,
                    GuiTheme.color(isHoveredOrFocused() ? 0xFFAAAAAA : enabled ? 0xFF8B8B8B : 0xFFC6C6C6,
                            isHoveredOrFocused() ? 0xFF606060 : enabled ? 0xFF505050 : 0xFF3C3C3C), enabled);
            if (!icon.isEmpty()) graphics.item(icon, getX() + 1, getY() + 1);
            if (iconText != null) graphics.centeredText(font, iconText,
                    getX() + width / 2, getY() + (height - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
        }
    }

    private void drawFitted(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int color) {
        drawFitted(graphics, text, x, y, maxWidth, color, true);
    }

    private void drawFitted(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        if (maxWidth <= 0) return;
        if (font.width(text) > maxWidth) text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
        graphics.text(font, text, x, y, color, shadow);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        BrowserScrollPanel.bevel(graphics, panelX, panelY, panelWidth, panelHeight,
                GuiTheme.color(0xFFC6C6C6, 0xFF383838), false);
        int headerControls = (menu instanceof MultibuilderContainer ? 80 : 0) + (ae2Available() ? 38 : 0) + 38 + 14;
        drawFitted(graphics, title.getString(), panelX + 7, panelY + 11, panelWidth - headerControls,
                GuiTheme.labelColor(), false);
        ItemStack stack = toolStack();
        selectedIndex = stack.getItem() instanceof MultibuilderItem && !stack.has(MbtoolDataComponents.RUNTIME_STRUCTURE.get())
                ? stack.getOrDefault(MbtoolDataComponents.STRUCTURE_RECIPE.get(), -1) : -1;
        if (autocraftButton != null) autocraftButton.visible = meEnabled(false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawFitted(graphics, __("gui.mbtool.structure_count", filtered.size(), totalStructures).getString(),
                panelX + 7, panelY + panelHeight - 16, panelWidth - 214, GuiTheme.labelColor(), false);
        if (filtered.isEmpty()) graphics.centeredText(font, __("gui.mbtool.no_matching_structures"),
                grid.getX() + grid.getWidth() / 2, grid.getY() + grid.getHeight() / 2, 0xFFFFFFFF);
        renderBrowserTooltip(graphics, mouseX, mouseY);
    }

    private void renderBrowserTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int index = grid.hoveredIndex(mouseX, mouseY);
        if (index >= 0) {
            Entry<MultiblockStructure> entry = filtered.get(index);
            CraftingProgress.Summary progress = CraftingProgress.forStructure(entry.value().getName());
            if (craftActionVisible(progress)
                    && overCraftButton(grid.entryX(index), grid.entryY(index), grid.getCellWidth(), mouseX, mouseY)) {
                if (progress != null && progress.active()) {
                    graphics.setComponentTooltipForNextFrame(font, List.of(progress.status(), progress.timing(),
                            __("gui.mbtool.crafting.cancel.descr").withStyle(ChatFormatting.GOLD)), mouseX, mouseY);
                } else {
                    graphics.setComponentTooltipForNextFrame(font, List.of(__("gui.mbtool.craft"),
                            __("gui.mbtool.craft.descr").withStyle(ChatFormatting.GOLD),
                            __("gui.mbtool.craft.notice").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                }
                return;
            }
            graphics.setComponentTooltipForNextFrame(font, structureTooltip(entry), mouseX, mouseY);
            return;
        }
        int folderIndex = folderPanel.hoveredIndex(mouseX, mouseY);
        if (folderIndex >= 0) graphics.setTooltipForNextFrame(font, folderLabel(folderIndex), mouseX, mouseY);
    }

    /** Full name, folder, dimensions, and required materials with tool-inventory availability. */
    private List<Component> structureTooltip(Entry<MultiblockStructure> entry) {
        MultiblockStructure structure = entry.value();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(entry.name()));
        tooltip.add(Component.literal(entry.folderName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(__("gui.mbtool.multiblock_button.dimensions",
                structure.getWidth(), structure.getHeight(), structure.getDepth()).withStyle(ChatFormatting.GRAY));
        ItemStack tool = toolStack();
        if (tool.getItem() instanceof MultibuilderItem item) {
            IItemHandler inventory = item.getInventory(tool);
            boolean creative = playerInventory.player.isCreative();
            for (ItemStack needed : requiredItems.computeIfAbsent(structure, MultiblockStructure::getNeededItems)) {
                int available = 0;
                for (int slot = 0; inventory != null && slot < inventory.getSlots(); slot++) {
                    ItemStack stored = inventory.getStackInSlot(slot);
                    if (ItemStack.isSameItemSameComponents(stored, needed)) available += stored.getCount();
                }
                boolean enough = creative || available >= needed.getCount();
                tooltip.add(Component.literal(needed.getCount() + "x ").append(needed.getHoverName())
                        .withStyle(enough ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
        return tooltip;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) return;
        if (!minecraft.player.isAlive() || minecraft.player.isRemoved()
                || !(toolStack().getItem() instanceof MultibuilderItem)) {
            onClose();
            return;
        }
        if (sourceRevision != MultiblocksProvider.getRevision()) loadStructures();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        if (search.isFocused() && event.key() != org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            // Typing in search must not trigger inventory or gameplay shortcuts.
            search.keyPressed(event);
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        else super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
