package me.cg360.spudengine.core.world.entity;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class SimpleEntity extends StaticModelEntity {

    protected Matrix4f modelMatrix;
    protected Vector3f position;
    protected Quaternionf rotation;
    protected float scale;

    public SimpleEntity(UUID entityId, String modelId) {
        super(entityId, modelId);

        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = 1.0f;
    }

    @Override
    public Matrix4f getTransform() {
        return this.modelMatrix;
    }

    @Override
    public Vector3f getPosition() {
        return this.position;
    }

    @Override
    public Quaternionf getRotation() {
        return this.rotation;
    }

    @Override
    public float getScale() {
        return this.scale;
    }

    @Override
    public void updateTransform() {
        this.modelMatrix.translationRotateScale(this.position, this.rotation, this.scale);
    }


}
