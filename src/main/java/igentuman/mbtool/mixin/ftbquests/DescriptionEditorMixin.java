package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MultilineTextBox;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewEditor;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultilineTextEditorScreen.class, remap = false)
public abstract class DescriptionEditorMixin {
    @Shadow @Final private Panel toolbarPanel;
    @Shadow @Final private MultilineTextBox textBox;
    @Shadow private void insertAtEndOfLine(String text) { throw new AssertionError(); }
    @Unique private SimpleButton mbtool$previewButton;

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void mbtool$addPreviewButton(CallbackInfo ci) {
        if (mbtool$previewButton == null) {
            mbtool$previewButton = new SimpleButton(toolbarPanel,
                    Component.translatable("gui.mbtool.ftbquests"), ItemIcon.getItemIcon(Mbtool.MBTOOL.get()), (button, mouse) -> {
                if (!mouse.isLeft()) return;
                int cursor = textBox.cursorPos();
                MultiblockPreviewEditor.open(MultiblockPreviewEditor.defaultPreview(),
                        () -> ((BaseScreen) (Object) this).openGui(), marker -> {
                            textBox.seekCursor(Whence.ABSOLUTE, cursor);
                            // Keep the preview on its own line even when the cursor was in the middle of text.
                            insertAtEndOfLine("\n" + marker + "\n");
                        });
            });
        }
        if (!toolbarPanel.getWidgets().contains(mbtool$previewButton)) toolbarPanel.add(mbtool$previewButton);
        mbtool$previewButton.setPosAndSize(225, 1, 16, 16);
    }
}
