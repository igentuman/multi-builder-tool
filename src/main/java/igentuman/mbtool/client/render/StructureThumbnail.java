package igentuman.mbtool.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.function.Consumer;

/** A transparent GUI snapshot. Pixels stay on the GPU throughout baking and drawing. */
final class StructureThumbnail implements AutoCloseable {
    private static final int MAX_SIDE = 512;
    private final RenderTarget target;

    record Size(int width, int height) {
        static Size forGui(int width, int height, Matrix4f transform, double guiScale) {
            int displayWidth = Math.max(1, (int) Math.ceil(width * Math.hypot(transform.m00(), transform.m01())));
            int displayHeight = Math.max(1, (int) Math.ceil(height * Math.hypot(transform.m10(), transform.m11())));
            return forGui(displayWidth, displayHeight, guiScale);
        }

        static Size forGui(int width, int height, double guiScale) {
            double scale = Math.min(guiScale, (double) MAX_SIDE / Math.max(width, height));
            return new Size(Math.max(1, (int) Math.ceil(width * scale)),
                    Math.max(1, (int) Math.ceil(height * scale)));
        }
    }

    private StructureThumbnail(RenderTarget target) {
        this.target = target;
    }

    static StructureThumbnail bake(Size size, Consumer<RenderTarget> draw) {
        RenderSystem.assertOnRenderThread();
        SavedState state = new SavedState();
        RenderTarget target = null;
        try {
            // Card scissors use window coordinates and must not clip the offscreen image.
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            target = new TextureTarget(size.width(), size.height(), true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            draw.accept(target);
            return new StructureThumbnail(target);
        } catch (RuntimeException | Error ex) {
            if (target != null) target.destroyBuffers();
            throw ex;
        } finally {
            state.restore();
        }
    }

    long allocationBytes() {
        // RGBA8 color plus the 32-bit depth texture retained by TextureTarget.
        return (long) target.width * target.height * 8;
    }

    void redraw(Consumer<RenderTarget> draw) {
        SavedState state = new SavedState();
        try {
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            draw.accept(target);
        } finally {
            state.restore();
        }
    }

    void draw(PoseStack pose, int x, int y, int width, int height) {
        draw(pose, x, y, width, height, 0xFFFFFFFF);
    }

    void draw(PoseStack pose, int x, int y, int width, int height, int tint) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.enableBlend();
        // Offscreen alpha blending has already multiplied RGB by alpha.
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        float[] color = RenderSystem.getShaderColor();
        float r = color[0], g = color[1], b = color[2], a = color[3];
        float alpha = ((tint >>> 24) & 255) / 255F;
        // RGB is premultiplied in the offscreen target, so opacity must also scale its color.
        RenderSystem.setShaderColor(((tint >>> 16) & 255) / 255F * alpha,
                ((tint >>> 8) & 255) / 255F * alpha, (tint & 255) / 255F * alpha, alpha);
        try {
            Matrix4f matrix = pose.last().pose();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            // Framebuffer textures have their origin at the bottom left.
            buffer.vertex(matrix, x, y + height, 100).uv(0, 0).endVertex();
            buffer.vertex(matrix, x + width, y + height, 100).uv(1, 0).endVertex();
            buffer.vertex(matrix, x + width, y, 100).uv(1, 1).endVertex();
            buffer.vertex(matrix, x, y, 100).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShaderColor(r, g, b, a);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    @Override
    public void close() {
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            target.destroyBuffers();
        } finally {
            bindFramebuffers(drawFramebuffer, readFramebuffer);
        }
    }

    private static void bindFramebuffers(int draw, int read) {
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
    }

    /** Captured only on cache misses; regular thumbnail drawing requires no GL state queries. */
    private static final class SavedState {
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int[] viewport = new int[4];
        private final int[] scissor = new int[4];
        private final boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final float[] color = RenderSystem.getShaderColor().clone();
        private final int texture = RenderSystem.getShaderTexture(0);

        private SavedState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        }

        private void restore() {
            bindFramebuffers(drawFramebuffer, readFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (scissorEnabled) RenderSystem.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
            else RenderSystem.disableScissor();
            if (depthEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (blendEnabled) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (cullEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.depthMask(depthMask);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.setShaderTexture(0, texture);
        }
    }
}
