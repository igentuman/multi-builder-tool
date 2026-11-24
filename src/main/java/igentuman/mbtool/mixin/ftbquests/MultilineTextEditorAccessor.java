package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.client.gui.widget.MultilineTextBox;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MultilineTextEditorScreen.class, remap = false)
public interface MultilineTextEditorAccessor {
    @Accessor(value = "textBox", remap = false)
    MultilineTextBox mbtool$getTextBox();

    @Invoker(value = "insertAtEndOfLine", remap = false)
    void mbtool$insertAtEndOfLine(String text);
}
