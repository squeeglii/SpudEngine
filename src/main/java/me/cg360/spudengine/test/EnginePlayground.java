package me.cg360.spudengine.test;

import me.cg360.spudengine.core.GameHooks;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.geometry.model.Mesh;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.world.Scene;
import org.tinylog.Logger;

import java.util.List;

public class EnginePlayground extends GameHooks {

    // Assumes clockwise winding order.
    private static final Mesh MESH_TRIANGLE_2D = new Mesh(
            new float[]{
                -0.5f, -0.5f, 0.0f,
                 0.0f,  0.5f, 0.0f,
                 0.5f, -0.5f, 0.0f   },
            new int[]{ 0, 1, 2 }
        );

    public EnginePlayground(SpudEngine engineInstance) {
        super("Engine Playground", engineInstance);
    }

    @Override
    protected void init(Window window, Scene scene, Renderer renderer) {
        Logger.info("\u001B[31mANSI Test.");
        List<Mesh> meshDataList = List.of(MESH_TRIANGLE_2D);
        List<Model> modelDataList = List.of(
                new Model("triangle", meshDataList)
        );

        renderer.getModelManager().transformModels(renderer, modelDataList);
    }

    @Override
    protected void logicTick(Window window, Scene scene, long delta) {
        //Logger.info("Time: {}+{}", this.getEngine().getTicksAlive(), delta);
    }

    @Override
    protected void input(Window window, Scene scene, long delta) {

    }
}
