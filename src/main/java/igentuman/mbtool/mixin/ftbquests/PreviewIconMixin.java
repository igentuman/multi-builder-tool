package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.icon.Icon;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Icon.class, remap = false)
public abstract class PreviewIconMixin {
    @Inject(method = "getIcon(Ljava/lang/String;)Ldev/ftb/mods/ftblibrary/icon/Icon;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mbtool$readPreview(String id, CallbackInfoReturnable<Icon<?>> cir) {
        MultiblockPreviewTag.parse(id).ifPresent(tag -> cir.setReturnValue(new MultiblockPreviewIcon(tag)));
    }
}
