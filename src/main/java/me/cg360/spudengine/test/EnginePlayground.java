package me.cg360.spudengine.test;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.component.FlyCameraController;
import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.geometry.model.*;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.world.entity.impl.SimpleMoveableEntity;
import me.cg360.spudengine.core.world.Scene;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class EnginePlayground extends GameComponent {

    // Assumes clockwise winding order.
    private static final Mesh MESH_TRIANGLE_2D = Mesh.withoutProvidedUVs(
            new float[]{
                -0.5f, -0.5f, 0.0f,
                 0.0f,  0.5f, 0.0f,
                 0.5f, -0.5f, 0.0f   },
            new int[]{ 0, 1, 2 }
        );

    private static final Mesh MESH_CUBE = new Mesh(
            new float[]{
                    -0.5f, 0.5f, 0.5f,
                    -0.5f, -0.5f, 0.5f,
                    0.5f, -0.5f, 0.5f,
                    0.5f, 0.5f, 0.5f,
                    -0.5f, 0.5f, -0.5f,
                    0.5f, 0.5f, -0.5f,
                    -0.5f, -0.5f, -0.5f,
                    0.5f, -0.5f, -0.5f,
            },
            new float[]{
                    0.0f, 0.0f,
                    1.0f, 0.0f,
                    2.0f, 0.0f,
                    2.0f, 1.0f,
                    2.0f, 2.0f,
                    1.0f, 2.0f,
                    0.0f, 2.0f,
                    0.0f, 1.0f,
            },
            new int[]{
                    // Front face
                    0, 1, 3, 3, 1, 2,
                    // Top Face
                    4, 0, 3, 5, 4, 3,
                    // Right face
                    3, 2, 7, 5, 3, 7,
                    // Left face
                    6, 1, 0, 6, 0, 4,
                    // Bottom face
                    2, 1, 6, 2, 6, 7,
                    // Back face
                    7, 6, 4, 7, 4, 5,
            },
            0 // Default material.
    );

    private static final Vector3f ROTATION_AXIS = new Vector3f(0, 1, 0);

    private SimpleMoveableEntity chamberEntity;
    private SimpleMoveableEntity cubeEntity;
    private float angle;

    public EnginePlayground(SpudEngine engineInstance) {
        super("Engine Playground", engineInstance);

        this.addSubListener(new FlyCameraController(this, engineInstance));
    }

    @Override
    protected void onInit(Window window, Scene scene, RenderSystem renderSystem) {
        //Logger.info("\u001B[31mANSI Test.");
        Texture colTex = renderSystem.getTextureManager().newCheckerboardTexture(
                "magenta", 256, 256, 3, 32, Color.MAGENTA, Color.BLACK
        );

        renderSystem.getModelManager().processModels(
                ModelLoader.loadEnvironmentModel("chamber01", "env/chamber01.obj"),
                new Model("cube", new Material("missing", Material.WHITE), MESH_CUBE)
        );

        this.chamberEntity = new SimpleMoveableEntity("chamber01");
        this.cubeEntity = new SimpleMoveableEntity("cube");

        scene.addEntity(this.chamberEntity);
        scene.addEntity(this.cubeEntity);

        this.chamberEntity.getRotation().identity().rotateAxis((float) Math.toRadians(145f), ROTATION_AXIS);
        this.chamberEntity.updateTransform();

        this.cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(65f), ROTATION_AXIS);
        this.cubeEntity.updateTransform();

        this.chamberEntity.setPosition(0, -3, -5);
        this.cubeEntity.setPosition(0, 1, -14);

        scene.getMainCamera().setPosition(-1, 1.5f, -4);
        scene.getMainCamera().setRotation((float) Math.toRadians(40f), (float) Math.toRadians(this.angle));
    }


    private static final int mode = 2;

    @Override
    protected void onLogicTick(Window window, Scene scene, long delta) {
        this.angle += 1.0f; //* delta;

        if (this.angle >= 360)
            this.angle = this.angle - 360;

        switch (mode) {
            case 0 -> {  // rotate room
                this.chamberEntity.getRotation().identity().rotateAxis((float) Math.toRadians(this.angle), ROTATION_AXIS);
                this.chamberEntity.updateTransform();

                this.cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(this.angle), ROTATION_AXIS);
                this.cubeEntity.updateTransform();

                this.chamberEntity.setPosition(0, -3, -5);
                this.cubeEntity.setPosition(0, 1, -14);
            }
            case 1 -> { // static room
                this.chamberEntity.getRotation().identity().rotateAxis((float) Math.toRadians(145f), ROTATION_AXIS);
                this.chamberEntity.updateTransform();

                this.cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(65f), ROTATION_AXIS);
                this.cubeEntity.updateTransform();

                this.chamberEntity.setPosition(0, -3, -18);
                this.cubeEntity.setPosition(-2, -2.5f, -18);
                this.cubeEntity.setScale(0.8f);
            }
            case 2 -> { // rotate camera

            }
        }

    }

    @Override
    protected void onInputEvent(Window window, int key, int action, int modifiers) {
        if(key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_RELEASE)
            this.getEngine().getRenderer().useWireframe = !this.getEngine().getRenderer().useWireframe;
    }
}
