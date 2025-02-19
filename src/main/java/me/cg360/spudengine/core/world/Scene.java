package me.cg360.spudengine.core.world;

import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;

import java.util.*;

public class Scene {

    private HashMap<UUID, StaticModelEntity> entities;
    private HashMap<String, List<StaticModelEntity>> modelSets;

    private Projection projection;

    public Scene(Window window) {
        this.entities = new HashMap<>();
        this.modelSets = new HashMap<>();
        this.projection = new Projection();

        this.projection.resize(window.getWidth(), window.getHeight());
    }

    // todo: distinguish between rendered entities and some improved version of DummyEntities.
    public void addEntity(StaticModelEntity entity) {
        UUID entityId = entity.getEntityId();

        if(this.entities.containsKey(entityId)) {
            this.removeEntity(entityId);
        }

        this.entities.put(entityId, entity);
        String modelId = entity.getModelId();

        List<StaticModelEntity> groupedEntities;
        if(!this.modelSets.containsKey(modelId)) {
            groupedEntities = new LinkedList<>();
            this.modelSets.put(modelId, groupedEntities);

        } else groupedEntities = this.modelSets.get(modelId);

        groupedEntities.add(entity);

        entity.onAdd(this);
    }

    public void removeEntity(RenderedEntity entity) {
        this.removeEntity(entity.getEntityId());
    }

    public void removeEntity(UUID entityId) {
        RenderedEntity removedEntity = this.entities.remove(entityId);

        if(removedEntity == null)
            return;

        removedEntity.onRemove(this);

        List<StaticModelEntity> modelList = this.getEntitiesWithModel(removedEntity.getModelId());
        modelList.remove(removedEntity);

        // Save processing models when they're not used.
        if(modelList.isEmpty())
            this.modelSets.remove(removedEntity.getModelId());
    }

    public Projection getProjection() {
        return this.projection;
    }

    public Set<String> getUsedModelIds() {
        return this.modelSets.keySet();
    }

    public List<StaticModelEntity> getEntitiesWithModel(String modelId) {
        List<StaticModelEntity> models = this.modelSets.get(modelId);
        return models == null ? new LinkedList<>() : models;
    }
}
