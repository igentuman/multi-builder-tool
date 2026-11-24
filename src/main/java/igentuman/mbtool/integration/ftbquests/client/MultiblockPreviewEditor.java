package igentuman.mbtool.integration.ftbquests.client;

import dev.ftb.mods.ftblibrary.client.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.config.gui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.client.util.ImageComponent.ImageAlign;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewIcon;
import igentuman.mbtool.integration.ftbquests.MultiblockPreviewTag;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class MultiblockPreviewEditor {
    private MultiblockPreviewEditor() {}

    public static void open(MultiblockPreviewComponent component, Runnable returnToEditor, Consumer<String> accepted) {
        open(component, returnToEditor, accepted, true);
    }

    public static void openCanvas(MultiblockPreviewComponent component, Runnable returnToEditor, Consumer<String> accepted) {
        open(component, returnToEditor, accepted, false);
    }

    private static void open(MultiblockPreviewComponent component, Runnable returnToEditor,
                             Consumer<String> accepted, boolean descriptionLayout) {
        EditableConfigGroup group = new EditableConfigGroup("gui.mbtool.ftbquests", save -> {
            returnToEditor.run();
            if (save) accepted.accept(component.toString());
        });
        addModelSettings(group, component, () -> {});
        group.addInt("width", component.getWidth(), component::setWidth, MultiblockPreviewTag.DEFAULT_SIZE, 1, 1000);
        group.addInt("height", component.getHeight(), component::setHeight, MultiblockPreviewTag.DEFAULT_SIZE, 1, 1000);
        if (descriptionLayout) {
            group.addEnum("align", component.getAlign(), component::setAlign, ImageAlign.NAME_MAP, ImageAlign.CENTER);
            group.addBool("fit", component.isFit(), component::setFit, false);
        }
        new EditConfigScreen(group).openGui();
    }

    public static void addModelSettings(EditableConfigGroup group, MultiblockPreviewComponent component, Runnable changed) {
        group.add("structure", new StructureConfig(), component.structureId, value -> {
            component.structureId = value;
            changed.run();
        }, component.structureId).setNameKey("gui.mbtool.ftbquests.structure");
        group.addDouble("rotation_speed", component.rotationSpeed, value -> {
            component.rotationSpeed = value;
            changed.run();
        }, MultiblockPreviewTag.DEFAULT_ROTATION_SPEED,
                -MultiblockPreviewTag.MAX_ROTATION_SPEED, MultiblockPreviewTag.MAX_ROTATION_SPEED)
                .setNameKey("gui.mbtool.ftbquests.rotation_speed");
    }

    public static MultiblockPreviewComponent defaultPreview() {
        String id = MultiblocksProvider.getStructures().stream()
                .filter(s -> s.getId() != null)
                .map(s -> s.getId().toString())
                .findFirst().orElse("mbtool:missing");
        return new MultiblockPreviewComponent(new MultiblockPreviewTag(id,
                MultiblockPreviewTag.DEFAULT_SIZE, MultiblockPreviewTag.DEFAULT_SIZE, "center", false));
    }

    private static final class StructureConfig extends EditableString {
        @Override
        public Component getStringForGUI(String value) {
            var structure = MultiblockPreviewIcon.findStructure(value);
            return structure == null ? Component.literal(String.valueOf(value)) : Component.translatable(structure.getName());
        }

        @Override
        public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
            BaseScreen parent = clicked.getGui();
            Minecraft.getInstance().setScreen(new QuestStructurePickerScreen(getValue(), selection -> {
                if (selection != null) setValue(selection);
                callback.save(selection != null);
                parent.openGui();
            }));
        }
    }
}
