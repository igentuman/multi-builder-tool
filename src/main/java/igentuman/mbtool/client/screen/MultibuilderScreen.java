package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.render.MultiblockRenderer;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.rl;

public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private static final ResourceLocation PASTE_ICON = rl("textures/gui/paste.png");
    private Button chooseButton;
    private boolean meAccessEnabled = false;
    private boolean meAutocraftEnabled = false;
    // ME access toggle button area (16x16), relative to GUI top-left
    private static final int ME_BTN_REL_X = 168;
    private static final int ME_BTN_REL_Y = 107;
    private static final int ME_BTN_SIZE = 16;
    private static final int AUTOCRAFT_BTN_REL_X = ME_BTN_REL_X;
    private static final int AUTOCRAFT_BTN_REL_Y = ME_BTN_REL_Y + ME_BTN_SIZE + 2;
    private ItemStack meIconStack = ItemStack.EMPTY;
    private ItemStack autocraftIconStack = ItemStack.EMPTY;
    public int selectedStructure = -1;
    
    public MultibuilderScreen(MultibuilderContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 216;
        this.imageHeight = 206;
    }

    @Override
    protected void init() {
        super.init();
        
        // Load selected structure and ME access state from item NBT
        loadSelectedStructure();
        loadMeAccessState();
        loadMeAutocraftState();

        // Create icon stacks for the ME button and autocraft button
        try {
            BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("ae2", "wireless_terminal")
            ).ifPresent(item -> meIconStack = new ItemStack(item));
            BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table")
            ).ifPresent(item -> autocraftIconStack = new ItemStack(item));
        } catch (Exception ignored) {
            // AE2 not loaded
        }
        
        // Position the button in the GUI
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
        
        this.addRenderableWidget(this.chooseButton);
    }
    
    private void loadMeAccessState() {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().selected);
            if (multibuilderStack.getItem() instanceof MultibuilderItem) {
                meAccessEnabled = multibuilderStack.getOrDefault(MbtoolDataComponents.ME_ACCESS.get(), false);
            }
        }
    }

    private void loadMeAutocraftState() {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().selected);
            if (multibuilderStack.getItem() instanceof MultibuilderItem) {
                meAutocraftEnabled = MultibuilderItem.isMeAutocraftingEnabled(multibuilderStack);
            }
        }
    }

    private void loadSelectedStructure() {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().selected);
            selectedStructure = -1;
            if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
                selectedStructure = multibuilderItem.getSelectedStructureId(multibuilderStack);
            }
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (List.of(49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59).contains(pKeyCode)) {
            return false;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if ME access toggle button was clicked
        int btnX = this.leftPos + ME_BTN_REL_X;
        int btnY = this.topPos + ME_BTN_REL_Y;
        if (button == 0 && mouseX >= btnX && mouseX < btnX + ME_BTN_SIZE && mouseY >= btnY && mouseY < btnY + ME_BTN_SIZE) {
            if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return super.mouseClicked(mouseX, mouseY, button);
            meAccessEnabled = !meAccessEnabled;
            // Send packet to server
            if (this.minecraft != null && this.minecraft.player != null) {
                Player player = this.minecraft.player;
                InteractionHand hand = player.getMainHandItem().is(MBTOOL.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                PacketDistributor.sendToServer(new ToggleMeAccessPacket(meAccessEnabled, hand));
                // Also update client-side ItemStack for immediate visual feedback
                ItemStack stack = player.getItemInHand(hand);
                stack.set(MbtoolDataComponents.ME_ACCESS.get(), meAccessEnabled);
            }
            return true;
        }

        // Check if Autocraft toggle button was clicked (only when ME access is enabled)
        if (meAccessEnabled) {
            int acBtnX = this.leftPos + AUTOCRAFT_BTN_REL_X;
            int acBtnY = this.topPos + AUTOCRAFT_BTN_REL_Y;
            if (button == 0 && mouseX >= acBtnX && mouseX < acBtnX + ME_BTN_SIZE && mouseY >= acBtnY && mouseY < acBtnY + ME_BTN_SIZE) {
                if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return super.mouseClicked(mouseX, mouseY, button);
                meAutocraftEnabled = !meAutocraftEnabled;
                if (this.minecraft != null && this.minecraft.player != null) {
                    Player player = this.minecraft.player;
                    InteractionHand hand = player.getMainHandItem().is(MBTOOL.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    PacketDistributor.sendToServer(new ToggleMeAutocraftPacket(meAutocraftEnabled, hand));
                    // Update client-side for immediate visual feedback
                    ItemStack stack = player.getItemInHand(hand);
                    MultibuilderItem.setMeAutocraftingEnabled(stack, meAutocraftEnabled);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onChooseButtonClick(Button button) {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            int slot = player.getInventory().selected;
            
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
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        ItemStack multibuilderStack = Minecraft.getInstance().player.getInventory().getItem(Minecraft.getInstance().player.getInventory().selected);
        if(!multibuilderStack.is(MBTOOL.get())) return;
        MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
        MultiblockStructure structure = multibuilderItem.getCurrentStructure(multibuilderStack);
        if(structure == null) return;
        MultiblockRenderer.render(
                structure,
                pGuiGraphics.pose(),
                x + 148, y + 9, 60, 60
        );

        // Render ME access toggle button
        renderMeAccessButton(pGuiGraphics, x, y, pMouseX, pMouseY);

        // Render autocraft toggle button (only when ME access is enabled)
        if (meAccessEnabled) {
            renderAutocraftButton(pGuiGraphics, x, y, pMouseX, pMouseY);
        }
    }

    private void renderMeAccessButton(GuiGraphics guiGraphics, int guiX, int guiY, int mouseX, int mouseY) {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        int btnX = guiX + ME_BTN_REL_X;
        int btnY = guiY + ME_BTN_REL_Y;

        // Draw button background with border
        int bgColor = meAccessEnabled ? 0xFF00AA00 : 0xFF555555;
        int innerColor = meAccessEnabled ? 0xFF003300 : 0xFF222222;
        guiGraphics.fill(btnX - 1, btnY - 1, btnX + ME_BTN_SIZE + 1, btnY + ME_BTN_SIZE + 1, bgColor);
        guiGraphics.fill(btnX, btnY, btnX + ME_BTN_SIZE, btnY + ME_BTN_SIZE, innerColor);

        // Render the wireless terminal item icon
        if (!meIconStack.isEmpty()) {
            guiGraphics.renderItem(meIconStack, btnX, btnY);
        }

        // Render tooltip on hover
        if (mouseX >= btnX && mouseX < btnX + ME_BTN_SIZE && mouseY >= btnY && mouseY < btnY + ME_BTN_SIZE) {
            String stateKey = meAccessEnabled ? "gui.mbtool.me_access.enabled" : "gui.mbtool.me_access.disabled";
            ChatFormatting color = meAccessEnabled ? ChatFormatting.GREEN : ChatFormatting.RED;
            guiGraphics.renderTooltip(this.font,
                List.of(Component.translatable("gui.mbtool.me_access"),
                        Component.translatable("gui.mbtool.me_access.descr").withStyle(ChatFormatting.GOLD),
                        Component.translatable(stateKey).withStyle(color)),
                Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderAutocraftButton(GuiGraphics guiGraphics, int guiX, int guiY, int mouseX, int mouseY) {
        if (!ModUtil.isAe2Loaded() || !MbtoolConfig.isAe2IntegrationEnabled()) return;
        int btnX = guiX + AUTOCRAFT_BTN_REL_X;
        int btnY = guiY + AUTOCRAFT_BTN_REL_Y;

        // Draw button background with border
        int bgColor = meAutocraftEnabled ? 0xFF0066CC : 0xFF555555;
        int innerColor = meAutocraftEnabled ? 0xFF002244 : 0xFF222222;
        guiGraphics.fill(btnX - 1, btnY - 1, btnX + ME_BTN_SIZE + 1, btnY + ME_BTN_SIZE + 1, bgColor);
        guiGraphics.fill(btnX, btnY, btnX + ME_BTN_SIZE, btnY + ME_BTN_SIZE, innerColor);

        // Render the crafting table item icon
        if (!autocraftIconStack.isEmpty()) {
            guiGraphics.renderItem(autocraftIconStack, btnX, btnY);
        }

        // Render tooltip on hover
        if (mouseX >= btnX && mouseX < btnX + ME_BTN_SIZE && mouseY >= btnY && mouseY < btnY + ME_BTN_SIZE) {
            String stateKey = meAutocraftEnabled ? "gui.mbtool.me_autocraft.enabled" : "gui.mbtool.me_autocraft.disabled";
            ChatFormatting color = meAutocraftEnabled ? ChatFormatting.AQUA : ChatFormatting.RED;
            guiGraphics.renderTooltip(this.font,
                    List.of(Component.translatable("gui.mbtool.me_autocraft"),
                            Component.translatable("gui.mbtool.me_autocraft.descr").withStyle(ChatFormatting.GOLD),
                            Component.translatable("gui.mbtool.me_autocraft.notice").withStyle(ChatFormatting.RED),
                            Component.translatable(stateKey).withStyle(color)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.inventoryLabelX, 3, 4210752, false);
    }
}
