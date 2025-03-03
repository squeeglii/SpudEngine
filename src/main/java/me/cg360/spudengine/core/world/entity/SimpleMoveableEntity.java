package me.cg360.spudengine.core.world.entity;

import me.cg360.spudengine.core.world.Scene;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class SimpleMoveableEntity extends SimpleEntity {

    public SimpleMoveableEntity(String modelId) {
        super(UUID.randomUUID(), modelId);
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
}
