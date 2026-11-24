package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.gui.widget.SimpleButton;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewEditor;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen$ToolbarPanel", remap = false)
public abstract class DescriptionToolbarMixin {
    @Unique
    private SimpleButton mbtool$previewButton;

    @Inject(method = "addWidgets()V", at = @At("TAIL"), remap = false)
    private void mbtool$addPreviewButton(CallbackInfo ci) {
        Panel panel = (Panel) (Object) this;
        if (mbtool$previewButton == null) {
            mbtool$previewButton = new SimpleButton(panel, Component.translatable("gui.mbtool.ftbquests"),
                    ItemIcon.ofItem(Mbtool.MBTOOL.get()), (button, mouse) -> {
                if (mouse.isLeft()) mbtool$openPreviewEditor(panel.getGui());
            });
        }
        if (!panel.getWidgets().contains(mbtool$previewButton)) panel.add(mbtool$previewButton);
    }

    @Inject(method = "alignWidgets()V", at = @At("TAIL"), remap = false)
    private void mbtool$alignPreviewButton(CallbackInfo ci) {
        // FTB 26.1 places its last left-aligned toolbar button (undo) at x=239.
        if (mbtool$previewButton != null) mbtool$previewButton.setPosAndSize(259, 1, 16, 16);
    }

    @Unique
    private static void mbtool$openPreviewEditor(BaseScreen gui) {
        if (!(gui instanceof MultilineTextEditorScreen screen)) return;
        MultilineTextEditorAccessor editor = (MultilineTextEditorAccessor) screen;
        int cursor = editor.mbtool$getTextBox().cursorPos();
        MultiblockPreviewEditor.open(MultiblockPreviewEditor.defaultPreview(), screen::openGui, marker -> {
            editor.mbtool$getTextBox().seekCursor(Whence.ABSOLUTE, cursor);
            // Keep the preview on its own line even when the cursor was in the middle of text.
            editor.mbtool$insertAtEndOfLine("\n" + marker + "\n");
        });
    }
}
