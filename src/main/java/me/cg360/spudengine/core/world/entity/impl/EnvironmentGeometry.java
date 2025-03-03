package me.cg360.spudengine.core.world.entity.impl;

import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.SimpleEntity;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class EnvironmentGeometry extends StaticModelEntity {

    private final Matrix4f transform;

    public EnvironmentGeometry(String modelId) {
        super(UUID.randomUUID(), modelId);

        this.transform = new Matrix4f();
    }

    @Override
    protected void onAdd(Scene scene) {

    }

    @Override
    protected void onRemove(Scene scene) {

    }

    @Override
    public final Matrix4f getTransform() {
        return this.transform;
    }

}
