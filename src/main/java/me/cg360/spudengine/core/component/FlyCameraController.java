package me.cg360.spudengine.core.component;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.input.MouseInput;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class FlyCameraController extends GameComponent {

    public FlyCameraController(GameComponent parent, SpudEngine engineInstance) {
        super(GameComponent.sub(parent, "camera_fly"), engineInstance);
    }

    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        window.getMouseInput().setCursorCaptured(true);
        window.getMouseInput().setForceCentered(true);
    }

    @Override
    protected void onInputTick(Window window, Scene scene, long delta) {
        MouseInput mouseInput = window.getMouseInput();
        Vector2f mDelta = mouseInput.getDelta().mul(0.01f);

        if(!window.isWindowFocused()) {
            mouseInput.setForceCentered(false);
            mouseInput.setCursorCaptured(false);
        }

        // Camera Rotation
        if(mouseInput.isCaptured() && mouseInput.isInWindow()) {
            float pitch = mDelta.y;
            float yaw = mDelta.x;
            if(scene.getMainCamera().isUpsideDown()) yaw = -yaw;

            scene.getMainCamera().addRotation(pitch, yaw);
        }

        // Camera movement
        int forward = 0, up = 0, left = 0;

        if(window.isKeyPressed(GLFW.GLFW_KEY_W)) forward += 1;
        if(window.isKeyPressed(GLFW.GLFW_KEY_S)) forward -= 1;

        if(window.isKeyPressed(GLFW.GLFW_KEY_A)) left += 1;
        if(window.isKeyPressed(GLFW.GLFW_KEY_D)) left -= 1;

        if(window.isKeyPressed(GLFW.GLFW_KEY_SPACE)) up += 1;
        if(window.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) up -= 1;

        if(mouseInput.isLeftButtonDown()) {
            mouseInput.setCursorCaptured(true);
            window.getMouseInput().setForceCentered(true);
        }

        if(forward != 0 || up != 0 || left != 0)
            scene.getMainCamera().move(forward, up, left, 0.01f*delta);
    }

    @Override
    protected void onInputEvent(Window window, int key, int action, int modifiers) {
        if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
            window.getMouseInput().setCursorCaptured(false);
            window.getMouseInput().setForceCentered(false);
        }
    }
}
