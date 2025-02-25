package me.cg360.spudengine.core.world;

import me.cg360.spudengine.core.EngineProperties;
import org.joml.Matrix4f;

public class Projection {

    private final Matrix4f projectionMatrix;

    public Projection() {
        this.projectionMatrix = new Matrix4f();
    }

    public void resize(int width, int height) {
        this.projectionMatrix.identity();

        float aspectRatio = (float) width / (float) height;
        this.projectionMatrix.perspective(EngineProperties.FOV, aspectRatio, EngineProperties.NEAR_PLANE, EngineProperties.FAR_PLANE, true);
    }

    public Matrix4f asMatrix() {
        return this.projectionMatrix;
    }

}
