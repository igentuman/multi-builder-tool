package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.client.gui.widget.ContextMenuItem;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.CreateObjectMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewEditor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(value = QuestPanel.class, remap = false)
public abstract class ChapterPreviewMenuMixin {
    @Shadow(remap = false) @Final private QuestScreen questScreen;
    @Shadow(remap = false) protected double questX;
    @Shadow(remap = false) protected double questY;

    @ModifyArg(method = "mousePressed(Ldev/ftb/mods/ftblibrary/client/gui/input/MouseButton;)Z",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbquests/client/gui/quests/QuestScreen;openContextMenu(Ljava/util/List;)Ldev/ftb/mods/ftblibrary/client/gui/widget/ContextMenu;",
                    remap = false),
            index = 0, remap = false)
    private List<ContextMenuItem> mbtool$addPreview(List<ContextMenuItem> menu) {
        Chapter chapter = questScreen.getSelectedChapter().orElse(null);
        if (chapter == null) return menu;
        QuestScreen screen = questScreen;
        double x = questX, y = questY;
        menu.add(new ContextMenuItem(Component.translatable("gui.mbtool.ftbquests"),
                ItemIcon.ofItem(Mbtool.MBTOOL.get()), button ->
                MultiblockPreviewEditor.openCanvas(MultiblockPreviewEditor.defaultPreview(), screen::openGui, marker -> {
                    MultiblockPreviewTag tag = MultiblockPreviewTag.parse(marker).orElse(null);
                    if (tag == null) return;
                    // Chapter images are sized in quest grid units; the preview size is in GUI pixels.
                    double gridSize = Math.max(1.0, screen.getQuestButtonSize());
                    ChapterImage image = new ChapterImage(0L, chapter)
                            .setImage(new MultiblockPreviewIcon(tag))
                            .setPosition(x, y);
                    image.setDimensions(tag.width() / gridSize, tag.height() / gridSize);
                    // Same request FTB's own "Image" entry sends; the server assigns the ID and syncs the chapter.
                    Play2ServerNetworking.send(CreateObjectMessage.requestCreation(image));
                })));
        return menu;
    }
}
