package igentuman.mbtool.client.screen;

import igentuman.mbtool.client.render.MultiblockRenderer;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.ModUtil;
import igentuman.mbtool.util.MultiblocksProvider;
import igentuman.mbtool.container.MultibuilderContainer;
import igentuman.mbtool.util.MultiblockStructure;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraftforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static igentuman.mbtool.Mbtool.rl;
import static igentuman.mbtool.Mbtool.MBTOOL;
import static igentuman.mbtool.integration.nc.ReactorDesignParser.parseNuclearCraftReactorDesign;
import static igentuman.mbtool.util.TextUtils.__;

public class MultibuilderScreen extends AbstractContainerScreen<MultibuilderContainer> {
    private static final ResourceLocation TEXTURE = rl("textures/gui/container/mbtool_inventory.png");
    private static final ResourceLocation PASTE_ICON = rl("textures/gui/paste.png");
    private Button chooseButton;
    private ImageButton pasteButton;
    public int selectedStructure = -1;
    private Screen structureBrowser;
    private boolean switchingView;
    
    public MultibuilderScreen(MultibuilderContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 216;
        this.imageHeight = 206;
    }

    public MultibuilderScreen(MultibuilderContainer menu, Inventory inventory, Component title, Screen structureBrowser) {
        this(menu, inventory, title);
        this.structureBrowser = structureBrowser;
    }

    @Override
    public void removed() {
        // A view change keeps the menu and its carried stack alive.
        if (!switchingView) super.removed();
        switchingView = false;
    }

    @Override
    protected void init() {
        super.init();

        loadSelectedStructure();

        int x = this.leftPos + 150;
        int y = this.topPos + 67;
        int buttonWidth = 57;
        int buttonHeight = 17;
        
        this.chooseButton = Button.builder(
                __("gui.mbtool.choose"),
            this::onChooseButtonClick
        ).bounds(x, y, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(__("gui.mbtool.select_structure")))
                .build();
        
        this.addRenderableWidget(this.chooseButton);
        
        int pasteX = this.leftPos + 170 + (buttonWidth - 18) / 2;
        int pasteY = this.topPos + 67 + buttonHeight + 2;
        
        this.pasteButton = new ImageButton(pasteX, pasteY, 18, 18, 0, 0, 18, PASTE_ICON, 18, 36, this::onPasteButtonClick);
        this.pasteButton.setTooltip(Tooltip.create(__("gui.mbtool.paste_reactor_tooltip")));
        if(!ModUtil.isNCNLoaded()) {
            this.pasteButton.visible = false;
        }
        this.addRenderableWidget(this.pasteButton);
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        chooseButton.active = menu.getCarried().isEmpty();
    }

    private void loadSelectedStructure() {
        if (this.minecraft != null && this.minecraft.player != null) {
            Player player = this.minecraft.player;
            ItemStack multibuilderStack = player.getInventory().getItem(menu.getPlayerSlot());
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
    
    private void onChooseButtonClick(Button button) {
        if (minecraft == null || minecraft.player == null) return;
        if (structureBrowser == null) {
            structureBrowser = new MultibuilderSelectStructureScreen<>(menu, minecraft.player.getInventory(),
                    __("gui.mbtool.select_structure"));
        }
        // Keep a carried stack visible until the player places it in a slot.
        if (!menu.getCarried().isEmpty()) return;
        switchingView = true;
        minecraft.setScreen(structureBrowser);
    }

    private void onPasteButtonClick(Button button) {
        String jsonText = Minecraft.getInstance().keyboardHandler.getClipboard();
        
        if (jsonText == null || jsonText.trim().isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(__("message.mbtool.clipboard_empty"));
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
                    ItemStack multibuilderStack = player.getInventory().getItem(menu.getPlayerSlot());
                    
                    if (multibuilderStack.getItem() instanceof MultibuilderItem multibuilderItem) {
                        multibuilderItem.setRuntimeStructure(multibuilderStack, structure);
                        String messageKey = switch (inputType) {
                            case "URL" -> "message.mbtool.reactor_loaded_url";
                            case "file" -> "message.mbtool.reactor_loaded_file";
                            default -> "message.mbtool.reactor_loaded_json";
                        };
                        player.sendSystemMessage(__(messageKey));
                    } else {
                        player.sendSystemMessage(__("message.mbtool.no_multibuilder_item"));
                    }
                }
            } else {
                if (this.minecraft != null && this.minecraft.player != null) {
                    String messageKey = switch (inputType) {
                        case "URL" -> "message.mbtool.reactor_parse_failed_url";
                        case "file" -> "message.mbtool.reactor_parse_failed_file";
                        default -> "message.mbtool.reactor_parse_failed_json";
                    };
                    this.minecraft.player.sendSystemMessage(__(messageKey));
                }
            }
        } catch (Exception e) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(__("message.mbtool.clipboard_parse_error", e.getMessage()));
            }
        }
    }



    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        ItemStack multibuilderStack = Minecraft.getInstance().player.getInventory().getItem(menu.getPlayerSlot());
        if(!multibuilderStack.is(MBTOOL.get())) return;
        MultibuilderItem multibuilderItem = (MultibuilderItem) multibuilderStack.getItem();
        MultiblockStructure structure = multibuilderItem.getCurrentStructure(multibuilderStack);
        if (structure != null) MultiblockRenderer.render(
                structure,
                pGuiGraphics,
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
