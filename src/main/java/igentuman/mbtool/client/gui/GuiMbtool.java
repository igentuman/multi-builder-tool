package igentuman.mbtool.client.gui;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.common.container.ContainerMbtool;
import igentuman.mbtool.recipe.MultiblockRecipe;
import igentuman.mbtool.recipe.MultiblockRecipes;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;


public class GuiMbtool extends GuiContainer {
    private static final ResourceLocation background = new ResourceLocation(
            Mbtool.MODID, "textures/gui/container/mbtool.png"
    );

    private int curPage = 0;
    private int pageSize = 12;
    private final ContainerMbtool container;

    private GuiButton nextPageBtn;
    private GuiButton prevPageBtn;
    private List<GuiButton> recipeBtns = new ArrayList<>();

    public GuiMbtool(ContainerMbtool inventorySlotsIn) {
        super(inventorySlotsIn);
        this.container = inventorySlotsIn;
        xSize = 225;
        ySize = 186;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        initButtons();
        initRecipes();
    }

    public void initButtons()
    {
        this.buttonList.add(nextPageBtn = new GuiButtonImage(0, guiLeft+207, guiTop+70, 14, 36, 227, 0, 37, background));
        this.buttonList.add(prevPageBtn = new GuiButtonImage(1, guiLeft+5, guiTop+70, 14, 36, 242, 0, 37, background));
    }


    public void selectRecipe(int id)
    {
        container.setCurrentRecipe(id-2);
    }

    protected void actionPerformed(GuiButton button)
    {
        switch (button.id) {
            case 0:
                curPage = Math.min(curPage+1, MultiblockRecipes.getAvaliableRecipes().size()/pageSize);
                break;
            case 1:
                curPage = Math.max(curPage-1, 0);
                break;
            default:
                selectRecipe(button.id);
        }
    }

    public void initRecipes()
    {
        recipeBtns.clear();
        List<MultiblockRecipe> recipes = MultiblockRecipes.getAvaliableRecipes();
        int x = guiLeft+21;
        int y = guiTop+15;
        int counter = 0;
        int btnWidth = 90;
        for(int i = curPage*pageSize+2; i < curPage*pageSize+pageSize+2;i++) {
            try {
                GuiButton btn = new GuiButton(i, x, y, btnWidth, 20, fontRenderer.trimStringToWidth(recipes.get(i - 2).getLabel(), btnWidth - 3));
                counter++;
                if (container.getCurrentRecipe()+2 == i) {
                    btn.enabled = false;
                }
                x += btnWidth + 3;
                if (counter > 1) {
                    x = guiLeft + 21;
                    y += 23;
                    counter = 0;
                }
                recipeBtns.add(btn);
                this.buttonList.add(btn);
            } catch (IndexOutOfBoundsException ignored) {
               break;
            }
        }
    }



    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)this.guiLeft, (float)this.guiTop, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();

        GlStateManager.popMatrix();
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int w = this.fontRenderer.getStringWidth(I18n.format("container.mbtool"));
        this.fontRenderer.drawString(I18n.format("container.mbtool"), xSize/2-w/2, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.mbtool.energy", container.getEnergyStored()), 8, 154, 4210752);
        buttonList.clear();
        initButtons();
        initRecipes();
    }
}
