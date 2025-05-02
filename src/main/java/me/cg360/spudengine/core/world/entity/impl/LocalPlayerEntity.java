package me.cg360.spudengine.core.world.entity.impl;

import me.cg360.spudengine.core.input.MouseInput;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.world.Camera;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import me.cg360.spudengine.core.world.entity.trait.InputTicked;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class LocalPlayerEntity extends StaticModelEntity implements InputTicked {

    private final Camera sceneCamera;

    public LocalPlayerEntity(Model modelId) {
        this(modelId.getId());
    }

    public LocalPlayerEntity(String modelId) {
        super(UUID.randomUUID(), modelId);
        this.sceneCamera = new Camera(true);
    }

    /** */
    @Override
    public void consumeInputTick(Window window, Scene scene, long delta) {
        MouseInput mouseInput = window.getMouseInput();

        // Camera Rotation
        if(mouseInput.isCaptured() && mouseInput.isInWindow()) {
            Vector2f mDelta = mouseInput.getDelta().mul(0.01f);

            float pitch = mDelta.y;
            float yaw = mDelta.x;

            if(scene.getMainCamera().isUpsideDown())
                yaw = -yaw;

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

        if(forward != 0 || up != 0 || left != 0)
            scene.getMainCamera().move(forward, up, left, 0.01f*delta);
    }

    @Override
    protected void onAdd(Scene scene) {
        scene.setMainCamera(this.sceneCamera);
    }

    @Override
    protected void onRemove(Scene scene) {
        if(scene.getMainCamera() == this.sceneCamera) {
            scene.setMainCamera(null); // unless another camera took over, ditch it.
        }
    }

    @Override
    public Matrix4f getTransform() {
        return this.sceneCamera.getViewMatrix().invert(new Matrix4f());
    }

    @Override
    public boolean shouldDraw(RenderContext renderContext) {
        return renderContext.subpass() == 1;
    }
}
