package me.cg360.spudengine.core.component;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.input.MouseInput;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import org.lwjgl.glfw.GLFW;

public class CursorCapture extends GameComponent {

    public CursorCapture(GameComponent parent, SpudEngine engineInstance) {
        super(GameComponent.sub(parent, "cursor_capture"), engineInstance);
    }

    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        window.getMouseInput().setCursorCaptured(true);
        window.getMouseInput().setForceCentered(true);
    }

    @Override
    protected void onInputTick(Window window, Scene scene, long delta) {
        MouseInput mouseInput = window.getMouseInput();

        if(!window.isWindowFocused()) {
            mouseInput.setForceCentered(false);
            mouseInput.setCursorCaptured(false);
        }

        if(mouseInput.isLeftButtonDown()) {
            mouseInput.setCursorCaptured(true);
            window.getMouseInput().setForceCentered(true);
        }
    }

    @Override
    protected void onInputEvent(Window window, int key, int action, int modifiers) {
        if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
            window.getMouseInput().setCursorCaptured(false);
            window.getMouseInput().setForceCentered(false);
        }
    }
}
