package igentuman.mbtool.integration.common;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class StructurePreviewState {
    private static final float AUTO_ROTATION_SPEED = (float) (Math.PI * 0.1);
    private static final float DRAG_SENSITIVITY = 0.02f;

    private final double centerX;
    private final double centerY;
    private final double radius;

    private boolean dragging = false;
    private double lastMouseX = 0;
    private float manualAngle = 0;
    private long releaseTime = 0;

    public StructurePreviewState(double centerX, double centerY, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public boolean isInRotationArea(double mouseX, double mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public float update(double mouseX, double mouseY) {
        long window = Minecraft.getInstance().getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (releaseTime == 0) {
            releaseTime = now;
        }

        float angle;
        if (dragging) {
            angle = manualAngle;
        } else {
            angle = manualAngle + ((now - releaseTime) / 1000.0f) * AUTO_ROTATION_SPEED;
            if (angle > Math.PI * 2) {
                angle %= (float) (Math.PI * 2);
                manualAngle = angle;
                releaseTime = now;
            }
        }

        if (leftDown && !dragging && isInRotationArea(mouseX, mouseY)) {
            dragging = true;
            lastMouseX = mouseX;
            manualAngle = angle;
        }
        if (dragging && !leftDown) {
            dragging = false;
            releaseTime = now;
        }
        if (dragging) {
            manualAngle += (float) (mouseX - lastMouseX) * DRAG_SENSITIVITY;
            lastMouseX = mouseX;
            angle = manualAngle;
        }
        return angle;
    }
}
