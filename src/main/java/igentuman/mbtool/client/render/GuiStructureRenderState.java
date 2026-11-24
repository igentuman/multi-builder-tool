package igentuman.mbtool.client.render;

import igentuman.mbtool.util.MultiblockStructure;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;

import javax.annotation.Nullable;

/**
 * Picture-in-picture render state carrying a {@link MultiblockStructure} to be drawn as a real
 * 3D block mesh inside a GUI element (see {@link GuiStructureRenderer}), instead of flat item icons.
 *
 * <p>{@code cached} states are static thumbnails: they are baked once into an LRU-cached texture
 * and blitted on later frames. Non-cached states (JEI drag-rotation, FTB rotating widgets) are
 * re-rendered every frame.</p>
 */
public record GuiStructureRenderState(
    MultiblockStructure structure,
    float angleY,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    boolean cached,
    int tint,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public GuiStructureRenderState(MultiblockStructure structure, float angleY, int x0, int y0, int x1, int y1,
                                    float scale, @Nullable ScreenRectangle scissorArea) {
        this(structure, angleY, x0, y0, x1, y1, scale, false, -1, IDENTITY_POSE, scissorArea);
    }

    public GuiStructureRenderState(MultiblockStructure structure, float angleY, int x0, int y0, int x1, int y1,
                                    float scale, boolean cached, int tint, Matrix3x2f pose,
                                    @Nullable ScreenRectangle scissorArea) {
        this(structure, angleY, x0, y0, x1, y1, scale, cached, tint, pose, scissorArea,
            getBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    private static @Nullable ScreenRectangle getBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose,
                                                       @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea == null ? bounds : scissorArea.intersection(bounds);
    }
}
