package igentuman.mbtool.mixin.ftbquests;

import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import igentuman.mbtool.Mbtool;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;
import igentuman.mbtool.integration.ftbquests.client.MultiblockPreviewEditor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(value = QuestPanel.class, remap = false)
public abstract class ChapterPreviewMenuMixin {
    @Shadow @Final private QuestScreen questScreen;
    @Shadow protected double questX;
    @Shadow protected double questY;

    @ModifyArg(method = "mousePressed", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/client/gui/quests/QuestScreen;openContextMenu(Ljava/util/List;)Ldev/ftb/mods/ftblibrary/ui/ContextMenu;"), index = 0)
    private List<ContextMenuItem> mbtool$addPreview(List<ContextMenuItem> menu) {
        Chapter chapter = ((QuestScreenAccessor) questScreen).mbtool$getSelectedChapter();
        double x = questX, y = questY;
        double gridSize = questScreen.getQuestButtonSize();
        menu.add(new ContextMenuItem(Component.translatable("gui.mbtool.ftbquests"),
                ItemIcon.getItemIcon(Mbtool.MBTOOL.get()), button ->
                MultiblockPreviewEditor.openCanvas(MultiblockPreviewEditor.defaultPreview(), questScreen::openGui, marker -> {
                    MultiblockPreviewTag tag = MultiblockPreviewTag.parse(marker).orElseThrow();
                    ChapterImage image = new ChapterImage(chapter);
                    // Chapter images are sized in quest grid units (the usual image defaults to 1x1).
                    CompoundTag data = image.writeData(new CompoundTag());
                    data.putDouble("width", tag.width() / gridSize);
                    data.putDouble("height", tag.height() / gridSize);
                    image.readData(data);
                    image.setImage(new MultiblockPreviewIcon(tag)).setPosition(x, y);
                    chapter.addImage(image);
                    new EditObjectMessage(chapter).sendToServer();
                    questScreen.refreshWidgets();
                })));
        return menu;
    }
}
