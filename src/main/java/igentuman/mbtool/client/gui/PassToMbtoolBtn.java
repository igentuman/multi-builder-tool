package igentuman.mbtool.client.gui;

import giselle.jei_mekanism_multiblocks.client.jei.CostList;
import igentuman.mbtool.integration.jei.MultiblockStructure;
import igentuman.mbtool.item.MultibuilderItem;
import igentuman.mbtool.util.MekanismStructureGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static igentuman.mbtool.Mbtool.MBTOOL;

public class PassToMbtoolBtn extends ImageButton {
    protected CostList costsList;
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mbtool", "textures/item/mbtool.png");
    private int structureHeight = 0;
    private int structureWidth = 0;
    private int structureLength = 0;

    public PassToMbtoolBtn(int pX, int pY, int pWidth, int pHeight, int pXTexStart, int pYTexStart, Button.OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pXTexStart, pYTexStart, TEXTURE, pOnPress);
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.renderItem(new ItemStack(MBTOOL.get()), this.getX(), this.getY());
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(super.mouseClicked(pMouseX, pMouseY, pButton)) {
            if(Minecraft.getInstance().player == null) {
                return true;
            }
            ItemStack mbtool = getLocalMbtool();
            if(mbtool.isEmpty()) return true;
            List<ItemStack> blocks = this.costsList.getCosts();
            MultiblockStructure structure = MekanismStructureGenerator.generate(blocks, structureHeight, structureWidth, structureLength);
            if(structure == null) return true;
            MultibuilderItem item = (MultibuilderItem) mbtool.getItem();
            item.setRuntimeStructure(mbtool, structure);
        }
        return false;
    }

    private ItemStack getLocalMbtool() {
        ItemStack result = ItemStack.EMPTY;
        Player player = Minecraft.getInstance().player;
        if(player == null) return result;
        for (ItemStack item: player.getInventory().items) {
            if(item.is(MBTOOL.get())) {
                return item;
            }
        }
        return  result;
    }

    public void setContext(CostList list, int height, int width, int length) {
        this.costsList = list;
        this.structureHeight = height;
        this.structureWidth = width;
        this.structureLength = length;
    }
}
