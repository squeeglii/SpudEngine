package me.cg360.spudengine.core.world.entity;

import me.cg360.spudengine.core.world.Scene;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class DummyEntity extends StaticModelEntity {

    private String modelId;

    private Matrix4f modelMatrix;
    private Vector3f position;
    private Quaternionf rotation;
    private float scale;

    public DummyEntity(String modelId) {
        super(UUID.randomUUID(), modelId);

        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = 1.0f;
    }

    //TODO: Constructor including UUID. Should ensure there's no clashes.

    @Override
    public void onAdd(Scene scene) {

    }

    @Override
    public void onRemove(Scene scene) {

    }

    public void resetRotation() {
        this.rotation.set(0, 0, 0, 1);
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
        this.updateTransform();
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.updateTransform();
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        this.updateTransform();
    }

    public void setScale(float scale) {
        if(scale <= 0.0f)
            throw new IllegalArgumentException("Scale must be greater than zero");

        this.scale = scale;
        this.updateTransform();
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
