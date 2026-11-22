package igentuman.mbtool.integration.ftbquests.client;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.client.ImageComponent.ImageAlign;
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
        ConfigGroup group = new ConfigGroup("gui.mbtool.ftbquests", save -> {
            returnToEditor.run();
            if (save) accepted.accept(component.toString());
        });
        addModelSettings(group, component, () -> {});
        group.addInt("width", component.width, v -> component.width = v, MultiblockPreviewTag.DEFAULT_SIZE, 1, 1000);
        group.addInt("height", component.height, v -> component.height = v, MultiblockPreviewTag.DEFAULT_SIZE, 1, 1000);
        if (descriptionLayout) {
            group.addEnum("align", component.align, v -> component.align = v, ImageAlign.NAME_MAP, ImageAlign.CENTER);
            group.addBool("fit", component.fit, v -> component.fit = v, false);
        }
        new EditConfigScreen(group).openGui();
    }

    public static void addModelSettings(ConfigGroup group, MultiblockPreviewComponent component, Runnable changed) {
        group.add("structure", new StructureConfig(), component.structureId, v -> {
            component.structureId = v;
            changed.run();
        }, component.structureId).setNameKey("gui.mbtool.ftbquests.structure");
        group.addDouble("rotation_speed", component.rotationSpeed, v -> {
            component.rotationSpeed = v;
            changed.run();
        }, MultiblockPreviewTag.DEFAULT_ROTATION_SPEED,
                -MultiblockPreviewTag.MAX_ROTATION_SPEED, MultiblockPreviewTag.MAX_ROTATION_SPEED)
                .setNameKey("gui.mbtool.ftbquests.rotation_speed");
    }

    public static MultiblockPreviewComponent defaultPreview() {
        String id = MultiblocksProvider.getStructures().stream()
                .filter(s -> !s.isGroup() && s.getId() != null)
                .map(s -> s.getId().toString()).findFirst().orElse("mbtool:missing");
        return new MultiblockPreviewComponent(new MultiblockPreviewTag(id,
                MultiblockPreviewTag.DEFAULT_SIZE, MultiblockPreviewTag.DEFAULT_SIZE, "center", false));
    }

    private static final class StructureConfig extends StringConfig {
        @Override
        public Component getStringForGUI(String value) {
            var structure = MultiblockPreviewComponent.findStructure(value);
            return structure == null ? Component.literal(value) : Component.translatable(structure.getName());
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
