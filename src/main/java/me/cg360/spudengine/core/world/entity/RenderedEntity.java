package me.cg360.spudengine.core.world.entity;

import me.cg360.spudengine.core.world.Scene;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class RenderedEntity {

    private final UUID entityId;

    public RenderedEntity(UUID entityId) {
        this.entityId = entityId;
    }

    public abstract void onAdd(Scene scene);
    public abstract void onRemove(Scene scene);

    public abstract String getModelId();
    public abstract Matrix4f getTransform();

    public abstract Vector3f getPosition();
    public abstract Quaternionf getRotation();
    public abstract float getScale();

    public abstract void updateTransform();

    public final UUID getEntityId() {
        return this.entityId;
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
