package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.util.client.ImageComponent;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewComponent;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewEditor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class QuestPreviewMixin {
    @Shadow private Quest quest;

    @Inject(method = "findImageComponent", at = @At("HEAD"), cancellable = true)
    private void mbtool$findPreview(Component text, CallbackInfoReturnable<ImageComponent> cir) {
        MultiblockPreviewTag.parse(text.getString()).ifPresent(tag -> cir.setReturnValue(new MultiblockPreviewComponent(tag)));
    }

    @Inject(method = "editImage", at = @At("HEAD"), cancellable = true)
    private void mbtool$editPreview(int line, ImageComponent image, CallbackInfo ci) {
        if (!(image instanceof MultiblockPreviewComponent preview)) return;
        ViewQuestPanel panel = (ViewQuestPanel) (Object) this;
        MultiblockPreviewEditor.open(preview, panel::openGui, marker -> {
            quest.getRawDescription().set(line, marker);
            new EditObjectMessage(quest).sendToServer();
            panel.refreshWidgets();
        });
        ci.cancel();
    }
}
