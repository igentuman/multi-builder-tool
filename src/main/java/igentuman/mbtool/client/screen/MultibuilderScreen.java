package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.gui.MultiblockButton;
import igentuman.mbtool.config.MbtoolConfig;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.network.ToggleMeAccessPacket;
import igentuman.mbtool.network.ToggleMeAutocraftPacket;
import igentuman.mbtool.registration.MbtoolDataComponents;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.rl;

public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final Identifier TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private Button chooseButton;
    private boolean meAccessEnabled = false;
    private boolean meAutocraftEnabled = false;
    private static final int ME_BTN_REL_X = 168;
    private static final int ME_BTN_REL_Y = 107;
    private static final int ME_BTN_SIZE = 16;
    private static final int AUTOCRAFT_BTN_REL_X = ME_BTN_REL_X;
    private static final int AUTOCRAFT_BTN_REL_Y = ME_BTN_REL_Y + ME_BTN_SIZE + 2;
    private ItemStack meIconStack = ItemStack.EMPTY;
    private ItemStack autocraftIconStack = ItemStack.EMPTY;
    public int selectedStructure = -1;

    public MultibuilderScreen(MultibuilderContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, 216, 206);
    }

    @Override
    protected void init() {
        super.init();

        loadSelectedStructure();
        loadMeAccessState();
        loadMeAutocraftState();

        try {
            BuiltInRegistries.ITEM.getOptional(
                Identifier.fromNamespaceAndPath("ae2", "wireless_terminal")
            ).ifPresent(item -> meIconStack = new ItemStack(item));
            BuiltInRegistries.ITEM.getOptional(
                Identifier.fromNamespaceAndPath("minecraft", "crafting_table")
            ).ifPresent(item -> autocraftIconStack = new ItemStack(item));
        } catch (Exception ignored) {}

        int x = this.leftPos + 150;
        int y = this.topPos + 67;
        int buttonWidth = 57;
        int buttonHeight = 17;

        this.chooseButton = Button.builder(
                Component.translatable("gui.mbtool.choose"),
            this::onChooseButtonClick
        ).bounds(x, y, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.mbtool.select_structure")))
                .build();
        chooseButton.visible = true;
        chooseButton.active = true;
        this.addRenderableWidget(this.chooseButton);
    }

    private void loadMeAccessState() {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().getSelectedSlot());
            if (multibuilderStack.getItem() instanceof MultibuilderItem) {
                meAccessEnabled = multibuilderStack.getOrDefault(MbtoolDataComponents.ME_ACCESS.get(), false);
            }
        }
    }

    private void loadMeAutocraftState() {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().getSelectedSlot());
            if (multibuilderStack.getItem() instanceof MultibuilderItem) {
                meAutocraftEnabled = MultibuilderItem.isMeAutocraftingEnabled(multibuilderStack);
            }
        }
    }

    private void loadSelectedStructure() {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().getSelectedSlot());
            selectedStructure = -1;
            if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
                selectedStructure = multibuilderItem.getSelectedStructureId(multibuilderStack);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int pKeyCode = event.key();
        if (List.of(49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59).contains(pKeyCode)) {
            return false;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean b) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int btnX = this.leftPos + ME_BTN_REL_X;
        int btnY = this.topPos + ME_BTN_REL_Y;
        if (button == 0 && mouseX >= btnX && mouseX < btnX + ME_BTN_SIZE && mouseY >= btnY && mouseY < btnY + ME_BTN_SIZE) {
            if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return super.mouseClicked(event, b);
            meAccessEnabled = !meAccessEnabled;
            if (this.minecraft != null && this.minecraft.player != null) {
                Player player = this.minecraft.player;
                InteractionHand hand = player.getMainHandItem().is(MBTOOL.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                ClientPacketDistributor.sendToServer(new ToggleMeAccessPacket(meAccessEnabled, hand));
                ItemStack stack = player.getItemInHand(hand);
                stack.set(MbtoolDataComponents.ME_ACCESS.get(), meAccessEnabled);
            }
            return true;
        }

        if (meAccessEnabled) {
            int acBtnX = this.leftPos + AUTOCRAFT_BTN_REL_X;
            int acBtnY = this.topPos + AUTOCRAFT_BTN_REL_Y;
            if (button == 0 && mouseX >= acBtnX && mouseX < acBtnX + ME_BTN_SIZE && mouseY >= acBtnY && mouseY < acBtnY + ME_BTN_SIZE) {
                if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return super.mouseClicked(event, b);
                meAutocraftEnabled = !meAutocraftEnabled;
                if (this.minecraft != null && this.minecraft.player != null) {
                    Player player = this.minecraft.player;
                    InteractionHand hand = player.getMainHandItem().is(MBTOOL.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    ClientPacketDistributor.sendToServer(new ToggleMeAutocraftPacket(meAutocraftEnabled, hand));
                    ItemStack stack = player.getItemInHand(hand);
                    MultibuilderItem.setMeAutocraftingEnabled(stack, meAutocraftEnabled);
                }
                return true;
            }
        }

        return super.mouseClicked(event, b);
    }

    private void onChooseButtonClick(Button button) {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            int slot = player.getInventory().getSelectedSlot();

            MultibuilderSelectStructureContainer container = new MultibuilderSelectStructureContainer(
                0,
                player.blockPosition(),
                player.getInventory(),
                slot
            );

            MultibuilderSelectStructureScreen newScreen = new MultibuilderSelectStructureScreen(
                container,
                player.getInventory(),
                Component.translatable("gui.mbtool.select_structure"),
                this
            );

            this.minecraft.setScreen(newScreen);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);

        ItemStack multibuilderStack = Minecraft.getInstance().player.getInventory().getItem(Minecraft.getInstance().player.getInventory().getSelectedSlot());
        if(multibuilderStack.is(MBTOOL.get())) {
            MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
            MultiblockStructure structure = multibuilderItem.getCurrentStructure(multibuilderStack);
            if(structure != null) {
                MultiblockButton.renderStructureItems(pGuiGraphics, structure, x + 148, y + 9, 60, 60, 0f);
            }
        }

        renderMeAccessButton(pGuiGraphics, x, y, pMouseX, pMouseY);

        if (meAccessEnabled) {
            renderAutocraftButton(pGuiGraphics, x, y, pMouseX, pMouseY);
        }

        // Renders widgets (buttons), labels, slots
        super.extractContents(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    private void renderMeAccessButton(GuiGraphicsExtractor guiGraphics, int guiX, int guiY, int mouseX, int mouseY) {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        int btnX = guiX + ME_BTN_REL_X;
        int btnY = guiY + ME_BTN_REL_Y;

        int bgColor = meAccessEnabled ? 0xFF00AA00 : 0xFF555555;
        int innerColor = meAccessEnabled ? 0xFF003300 : 0xFF222222;
        guiGraphics.fill(btnX - 1, btnY - 1, btnX + ME_BTN_SIZE + 1, btnY + ME_BTN_SIZE + 1, bgColor);
        guiGraphics.fill(btnX, btnY, btnX + ME_BTN_SIZE, btnY + ME_BTN_SIZE, innerColor);

        if (!meIconStack.isEmpty()) {
            guiGraphics.item(meIconStack, btnX, btnY);
        }

        if (mouseX >= btnX && mouseX < btnX + ME_BTN_SIZE && mouseY >= btnY && mouseY < btnY + ME_BTN_SIZE) {
            String stateKey = meAccessEnabled ? "gui.mbtool.me_access.enabled" : "gui.mbtool.me_access.disabled";
            ChatFormatting color = meAccessEnabled ? ChatFormatting.GREEN : ChatFormatting.RED;
            guiGraphics.setTooltipForNextFrame(this.font,
                List.of(Component.translatable("gui.mbtool.me_access"),
                        Component.translatable("gui.mbtool.me_access.descr").withStyle(ChatFormatting.GOLD),
                        Component.translatable(stateKey).withStyle(color)),
                Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderAutocraftButton(GuiGraphicsExtractor guiGraphics, int guiX, int guiY, int mouseX, int mouseY) {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        int btnX = guiX + AUTOCRAFT_BTN_REL_X;
        int btnY = guiY + AUTOCRAFT_BTN_REL_Y;

        int bgColor = meAutocraftEnabled ? 0xFF0066CC : 0xFF555555;
        int innerColor = meAutocraftEnabled ? 0xFF002244 : 0xFF222222;
        guiGraphics.fill(btnX - 1, btnY - 1, btnX + ME_BTN_SIZE + 1, btnY + ME_BTN_SIZE + 1, bgColor);
        guiGraphics.fill(btnX, btnY, btnX + ME_BTN_SIZE, btnY + ME_BTN_SIZE, innerColor);

        if (!autocraftIconStack.isEmpty()) {
            guiGraphics.item(autocraftIconStack, btnX, btnY);
        }

        if (mouseX >= btnX && mouseX < btnX + ME_BTN_SIZE && mouseY >= btnY && mouseY < btnY + ME_BTN_SIZE) {
            String stateKey = meAutocraftEnabled ? "gui.mbtool.me_autocraft.enabled" : "gui.mbtool.me_autocraft.disabled";
            ChatFormatting color = meAutocraftEnabled ? ChatFormatting.AQUA : ChatFormatting.RED;
            guiGraphics.setTooltipForNextFrame(this.font,
                    List.of(Component.translatable("gui.mbtool.me_autocraft"),
                            Component.translatable("gui.mbtool.me_autocraft.descr").withStyle(ChatFormatting.GOLD),
                            Component.translatable("gui.mbtool.me_autocraft.notice").withStyle(ChatFormatting.RED),
                            Component.translatable(stateKey).withStyle(color)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.inventoryLabelX, 3, 4210752, false);
    }
}
