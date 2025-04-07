package me.cg360.spudengine.core.world.entity;

import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.context.RenderGoal;
import me.cg360.spudengine.core.world.Scene;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class RenderedEntity {

    private final UUID entityId;
    private boolean isInScene;

    public RenderedEntity(UUID entityId) {
        this.entityId = entityId;
    }

    public final void markAddedToScene(Scene scene) {
        if(this.isInScene) return;
        if(!scene.hasEntity(this)) return;

        this.onAdd(scene);
        this.isInScene = true;
    }

    public final void markRemovedFromScene(Scene scene) {
        if(!this.isInScene) return;
        if(scene.hasEntity(this)) return;

        this.onRemove(scene);
        this.isInScene = false;
    }

    protected abstract void onAdd(Scene scene);
    protected abstract void onRemove(Scene scene);

    public abstract String getModelId();
    public abstract Matrix4f getTransform();

    public boolean shouldDraw(RenderContext renderContext) {
        return true;
    }

    public final UUID getEntityId() {
        return this.entityId;
    }
}
