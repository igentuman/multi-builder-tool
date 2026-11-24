package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.client.util.ImageComponent;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.translation.TranslationKey;
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
    @Shadow(remap = false)
    private Quest quest;

    @Inject(method = "findImageComponent(Lnet/minecraft/network/chat/Component;)Ldev/ftb/mods/ftblibrary/client/util/ImageComponent;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void mbtool$findPreview(Component text, CallbackInfoReturnable<ImageComponent> cir) {
        MultiblockPreviewTag.parse(text.getString()).ifPresent(tag -> cir.setReturnValue(new MultiblockPreviewComponent(tag)));
    }

    @Inject(method = "editImage(ILdev/ftb/mods/ftblibrary/client/util/ImageComponent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void mbtool$editPreview(int line, ImageComponent image, CallbackInfo ci) {
        if (!(image instanceof MultiblockPreviewComponent preview)) return;
        ci.cancel();
        ViewQuestPanel panel = (ViewQuestPanel) (Object) this;
        MultiblockPreviewEditor.open(preview, panel::openGui, marker -> {
            if (quest == null) return;
            quest.modifyTranslatableListValue(TranslationKey.QUEST_DESC, lines -> {
                if (line >= 0 && line < lines.size()) lines.set(line, marker);
                else lines.add(marker);
            });
            panel.refreshWidgets();
        });
    }
}
