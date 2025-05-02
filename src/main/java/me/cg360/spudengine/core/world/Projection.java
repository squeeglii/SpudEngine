package me.cg360.spudengine.core.world;

import me.cg360.spudengine.core.EngineProperties;
import org.joml.Matrix4f;

public class Projection {

    private final Matrix4f projectionMatrix;
    private int width;
    private int height;
    private float near;
    private float far;

    public Projection() {
        this.projectionMatrix = new Matrix4f();
        this.near = EngineProperties.NEAR_PLANE;
        this.far = EngineProperties.FAR_PLANE;
    }

    public void resize(int width, int height) {
        this.projectionMatrix.identity();

        this.width = width;
        this.height = height;

        float aspectRatio = (float) width / (float) height;
        double fov = Math.PI * (EngineProperties.FOV / 180.0d);
        this.projectionMatrix.perspective((float) fov, aspectRatio, this.near, this.far, true);
    }

    public Matrix4f asMatrix() {
        return this.projectionMatrix;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public float getNearPlane() {
        return this.near;
    }

    public float getFarPlane() {
        return this.far;
    }
}
