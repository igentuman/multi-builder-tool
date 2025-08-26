package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.container.MultibuilderSelectStructureContainer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.Mbtool.rl;
import static igentuman.mbtool.integration.nc.ReactorDesignParser.parseNuclearCraftReactorDesign;

public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private static final ResourceLocation PASTE_ICON = rl("textures/gui/paste.png");
    private Button chooseButton;
    private ImageButton pasteButton;
    public int selectedStructure = -1;
    
    public MultibuilderScreen(MultibuilderContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 216;
        this.imageHeight = 206;
    }

    @Override
    protected void init() {
        super.init();
        
        // Load selected structure from item NBT
        loadSelectedStructure();
        
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
        
        // Position the paste button under the choose button
        int pasteX = this.leftPos + 170 + (buttonWidth - 18) / 2; // Center the 18x18 button under the choose button
        int pasteY = this.topPos + 67 + buttonHeight + 2; // 2 pixels gap below choose button
        
        this.pasteButton = new ImageButton(pasteX, pasteY, 18, 18, 0, 0, 18, PASTE_ICON, 18, 36, this::onPasteButtonClick);
        this.pasteButton.setTooltip(Tooltip.create(Component.translatable("gui.mbtool.paste_reactor_tooltip")));
        if(!ModUtil.isNCNLoaded()) {
            this.pasteButton.visible = false;
        }
        this.addRenderableWidget(this.pasteButton);
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
    
    private void onPasteButtonClick(Button button) {
        String jsonText = Minecraft.getInstance().keyboardHandler.getClipboard();
        
        if (jsonText == null || jsonText.trim().isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.mbtool.clipboard_empty"));
            }
            return;
        }
        
        try {
            String input = jsonText.trim();
            
            // Determine input type for user feedback
            String inputType = "JSON";
            if (input.startsWith("http://") || input.startsWith("https://")) {
                inputType = "URL";
            } else if (input.startsWith("file://") || 
                (!input.startsWith("{") && (input.contains("/") || input.contains("\\") || input.endsWith(".json")))) {
                inputType = "file";
            }
            
            // Parse the input (URL, file path, or JSON) and create MultiblockStructure
            MultiblockStructure structure = parseNuclearCraftReactorDesign(input);
            if (structure != null) {
                // Set the runtime structure in the multibuilder item
                if (this.minecraft != null && this.minecraft.player != null) {
                    Player player = this.minecraft.player;
                    ItemStack multibuilderStack = player.getInventory().getItem(player.getInventory().selected);
                    
                    if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
                        multibuilderItem.setRuntimeStructure(multibuilderStack, structure);
                        String messageKey = switch (inputType) {
                            case "URL" -> "message.mbtool.reactor_loaded_url";
                            case "file" -> "message.mbtool.reactor_loaded_file";
                            default -> "message.mbtool.reactor_loaded_json";
                        };
                        player.sendSystemMessage(Component.translatable(messageKey));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.mbtool.no_multibuilder_item"));
                    }
                }
            } else {
                if (this.minecraft != null && this.minecraft.player != null) {
                    String messageKey = switch (inputType) {
                        case "URL" -> "message.mbtool.reactor_parse_failed_url";
                        case "file" -> "message.mbtool.reactor_parse_failed_file";
                        default -> "message.mbtool.reactor_parse_failed_json";
                    };
                    this.minecraft.player.sendSystemMessage(Component.translatable(messageKey));
                }
            }
        } catch (Exception e) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.mbtool.clipboard_parse_error", e.getMessage()));
            }
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
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.inventoryLabelX, 3, 4210752, false);
    }
}
