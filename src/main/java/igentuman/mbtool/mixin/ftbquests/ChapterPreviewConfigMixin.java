package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
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
    @Inject(method = "fillConfigGroup", at = @At("TAIL"))
    private void mbtool$previewSettings(ConfigGroup group, CallbackInfo ci) {
        ChapterImage image = (ChapterImage) (Object) this;
        if (!(image.getImage() instanceof MultiblockPreviewIcon icon)) return;
        // Keep FTB's position, size, visibility, and other image controls; replace its texture selector.
        group.getValues().removeIf(value -> value.id.equals("image"));
        MultiblockPreviewComponent preview = new MultiblockPreviewComponent(icon.tag());
        MultiblockPreviewEditor.addModelSettings(group, preview,
                () -> image.setImage(new MultiblockPreviewIcon(preview.toTag())));
    }
}
