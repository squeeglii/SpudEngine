package me.cg360.spudengine.core.input;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class MouseInput {

    private final long windowHandle;

    private Vector2f currentPos;
    private Vector2f previousPos;
    private Vector2f delta;
    private boolean isInWindow = false;

    private boolean leftButtonDown = false;
    private boolean rightButtonDown = false;
    private boolean middleButtonDown = false;
    private boolean button4Down = false;
    private boolean button5Down = false;

    public MouseInput(long windowHandle) {
        this.windowHandle = windowHandle;

        this.currentPos = new Vector2f();
        this.previousPos = new Vector2f(-1, -1);
        this.delta = new Vector2f();

        GLFW.glfwSetCursorPosCallback(this.windowHandle, (window, x, y) -> {
            this.currentPos.set(x, y);
        });

        GLFW.glfwSetCursorEnterCallback(this.windowHandle, (handle, hasEntered) -> {
            this.isInWindow = hasEntered;
        });

        GLFW.glfwSetMouseButtonCallback(this.windowHandle, (handle, button, action, mods) -> {
            this.leftButtonDown = button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS;
            this.rightButtonDown = button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS;
            this.middleButtonDown = button == GLFW.GLFW_MOUSE_BUTTON_3 && action == GLFW.GLFW_PRESS;
            this.button4Down = button == GLFW.GLFW_MOUSE_BUTTON_4 && action == GLFW.GLFW_PRESS;
            this.button5Down = button == GLFW.GLFW_MOUSE_BUTTON_5 && action == GLFW.GLFW_PRESS;
        });
    }

    public void tickInput() {
        this.delta.set(0, 0);

        if(this.previousPos.x >= 0 && this.previousPos.y >= 0 && this.isInWindow) {
            this.currentPos.sub(this.previousPos, this.delta);
        }

        this.previousPos.set(this.currentPos);
    }


    public Vector2f getCurrentPos() {
        return this.currentPos;
    }

    public Vector2f getPreviousPos() {
        return this.previousPos;
    }

    public Vector2f getDelta() {
        return this.delta;
    }

    // Mouse buttons
    public boolean isLeftButtonDown() {
        return this.leftButtonDown;
    }

    public boolean isRightButtonDown() {
        return this.rightButtonDown;
    }

    public boolean isMiddleButtonDown() {
        return this.middleButtonDown;
    }

    public boolean isButton4Down() {
        return this.button4Down;
    }

    public boolean isButton5Down() {
        return this.button5Down;
    }
}
