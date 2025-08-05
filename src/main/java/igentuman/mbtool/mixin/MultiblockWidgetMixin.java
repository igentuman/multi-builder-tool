package igentuman.mbtool.mixin;

import giselle.jei_mekanism_multiblocks.client.JEI_MekanismMultiblocks_Client;
import giselle.jei_mekanism_multiblocks.client.gui.IntSliderWithButtons;
import giselle.jei_mekanism_multiblocks.client.jei.CostList;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import giselle.jei_mekanism_multiblocks.client.jei.category.FissionReactorCategory;
import igentuman.mbtool.client.gui.PassToMbtoolBtn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.regex.Pattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiblockWidget.class)
public abstract class MultiblockWidgetMixin {

    private static final Pattern MULTIBLOCK_TYPE_PATTERN = Pattern.compile("Fission|Turbine");

    @Shadow @Final protected CostList costsList;
    @Shadow protected IntSliderWithButtons heightWidget;
    @Shadow protected IntSliderWithButtons widthWidget;
    @Shadow protected IntSliderWithButtons lengthWidget;
    @Shadow private boolean initialzed;
    public PassToMbtoolBtn passToMbtoolBtn = initButton();

    private PassToMbtoolBtn initButton() {
        return new PassToMbtoolBtn(0, 100, 16, 16, 0, 0, pButton -> {
            costsList.isActive();
        });
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"), remap = false)
    private void onRenderReturn(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if(!MULTIBLOCK_TYPE_PATTERN.matcher(this.getClass().toString()).find()) {
            return;
        }
        passToMbtoolBtn.render(guiGraphics, mouseX, mouseY, partialTick);
        passToMbtoolBtn.setTooltip(Tooltip.create(Component.translatable("mbtool.tooltip.load_into_mbtool")));
        passToMbtoolBtn.setContext(
                this.costsList,
                this.heightWidget.getSlider().getValue(),
                this.widthWidget.getSlider().getValue(),
                this.lengthWidget.getSlider().getValue()
        );
    }

    @Inject(method = "updateInput", at = @At("HEAD"), remap = false)
    private void onUpdateInput(double pMouseX, double pMouseY, CallbackInfo ci) {
        if(!MULTIBLOCK_TYPE_PATTERN.matcher(this.getClass().toString()).find()) {
            return;
        }
        if (JEI_MekanismMultiblocks_Client.PRESSED) {
            passToMbtoolBtn.mouseClicked(pMouseX, pMouseY, 0);
        }
    }

}
