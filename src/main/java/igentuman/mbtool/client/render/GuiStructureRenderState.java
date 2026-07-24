package igentuman.mbtool.client.render;

import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import javax.annotation.Nullable;

/**
 * Picture-in-picture render state carrying a {@link MultiblockStructure} to be drawn as a real
 * 3D block mesh inside a GUI element (see {@link GuiStructureRenderer}), instead of flat item icons.
 */
public record GuiStructureRenderState(
    MultiblockStructure structure,
    float angleY,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public GuiStructureRenderState(MultiblockStructure structure, float angleY, int x0, int y0, int x1, int y1,
                                    float scale, @Nullable ScreenRectangle scissorArea) {
        this(structure, angleY, x0, y0, x1, y1, scale, scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
