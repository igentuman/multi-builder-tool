package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewComponent;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChapterImage.class, remap = false)
public abstract class ChapterPreviewConfigMixin {
    @Inject(method = "fillConfigGroup(Ldev/ftb/mods/ftblibrary/client/config/EditableConfigGroup;)V",
            at = @At("TAIL"), remap = false)
    private void mbtool$previewSettings(EditableConfigGroup group, CallbackInfo ci) {
        ChapterImage image = (ChapterImage) (Object) this;
        if (!(image.getImage() instanceof MultiblockPreviewIcon icon)) return;
        // Keep FTB's position, size, visibility, and other image controls; replace only its texture selector.
        group.getValues().removeIf(value -> "image".equals(value.id));
        MultiblockPreviewComponent preview = new MultiblockPreviewComponent(icon.tag());
        MultiblockPreviewEditor.addModelSettings(group, preview,
                () -> image.setImage(new MultiblockPreviewIcon(preview.toTag())));
    }
}
