package me.cg360.spudengine.test;

import me.cg360.spudengine.core.GameHooks;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.geometry.model.Mesh;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.world.entity.DummyEntity;
import me.cg360.spudengine.core.world.Scene;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EnginePlayground extends GameHooks {

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
                    0.5f, 0.0f,
                    1.0f, 0.0f,
                    1.0f, 0.5f,
                    1.0f, 1.0f,
                    0.5f, 1.0f,
                    0.0f, 1.0f,
                    0.0f, 0.5f,
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
            }
    );

    private static final Vector3f ROTATION_AXIS = new Vector3f(1, 1, 1);

    private DummyEntity cubeEntity;
    private float angle;

    public EnginePlayground(SpudEngine engineInstance) {
        super("Engine Playground", engineInstance);
    }

    @Override
    protected void init(Window window, Scene scene, Renderer renderer) {
        //Logger.info("\u001B[31mANSI Test.");
        List<Model> modelDataList = List.of(
                new Model("triangle", MESH_TRIANGLE_2D),
                new Model("cube", MESH_CUBE)
        );

        renderer.getModelManager().transformModels(renderer, modelDataList);

        this.cubeEntity = new DummyEntity("cube");
        scene.addEntity(this.cubeEntity);
    }

    @Override
    protected void logicTick(Window window, Scene scene, long delta) {
        this.angle += 4.0f; //* delta;

        if (this.angle >= 360)
            this.angle = this.angle - 360;


        this.cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(this.angle), ROTATION_AXIS);
        this.cubeEntity.updateTransform();

        this.cubeEntity.setPosition(0, 0, -4);
    }

    @Override
    protected void input(Window window, Scene scene, long delta) {

    }

    @Override
    protected void inputEvent(Window window, int key, int action, int modifiers) {
        if(key == GLFW.GLFW_KEY_W && action == GLFW.GLFW_RELEASE)
            this.getEngine().getRenderer().useWireframe = !this.getEngine().getRenderer().useWireframe;
    }
}
