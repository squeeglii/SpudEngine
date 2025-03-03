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

    public void updateTransform() {
        this.modelMatrix.translationRotateScale(this.position, this.rotation, this.scale);
    }

    @Override
    public Matrix4f getTransform() {
        return this.modelMatrix;
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Quaternionf getRotation() {
        return this.rotation;
    }

    public float getScale() {
        return this.scale;
    }

    public Vector3f getEulerAngles() {
        Vector3f eular = new Vector3f();
        this.getRotation().getEulerAnglesXYZ(eular);
        return eular;
    }

    public float getPitch() {
        return this.getRotation().y;
    }

    public float getYaw() {
        return this.getEulerAngles().z;
    }

    public float getRoll() {
        return this.getEulerAngles().x;
    }


}
