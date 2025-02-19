package me.cg360.spudengine.core.world.entity;

import java.util.UUID;

public abstract class StaticModelEntity extends RenderedEntity {

    private String modelId;

    public StaticModelEntity(UUID entityId, String modelId) {
        super(entityId);
        this.modelId = modelId == null ? "" : modelId.toLowerCase();
    }

    @Override
    public final String getModelId() {
        return this.modelId;
    }

}
