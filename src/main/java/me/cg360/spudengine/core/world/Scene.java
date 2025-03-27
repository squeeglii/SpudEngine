package me.cg360.spudengine.core.world;

import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import me.cg360.spudengine.core.world.entity.trait.InputTicked;

import java.util.*;

public class Scene {

    private HashMap<UUID, StaticModelEntity> entities;
    private HashMap<String, List<StaticModelEntity>> modelSets;

    private HashMap<UUID, InputTicked> inputTickedEntities;

    private Camera fallbackCamera;
    private Camera mainCamera;
    private Projection projection;

    public Scene(Window window) {
        this.entities = new HashMap<>();
        this.modelSets = new HashMap<>();
        this.inputTickedEntities = new HashMap<>();

        this.projection = new Projection();

        this.fallbackCamera = new Camera(true);
        this.mainCamera = this.fallbackCamera;

        this.projection.resize(window.getWidth(), window.getHeight());
    }

    public void passInputTick(Window window, Scene scene, long delta) {
        for(InputTicked input : this.inputTickedEntities.values()) {
            input.consumeInputTick(window, scene, delta);
        }
    }

    public void addEntities(StaticModelEntity... entities) {
        for(StaticModelEntity entity : entities)
            this.addEntity(entity);
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

        if(entity instanceof InputTicked inpTickAcceptor) {
            this.inputTickedEntities.put(entityId, inpTickAcceptor);
        }

        groupedEntities.add(entity);

        entity.markAddedToScene(this);
    }

    public void removeEntity(RenderedEntity entity) {
        if(entity == null)
            throw new IllegalArgumentException("Entity cannot be null");
        this.removeEntity(entity.getEntityId());
    }

    public void removeEntity(UUID entityId) {
        RenderedEntity removedEntity = this.entities.remove(entityId);

        if(removedEntity == null)
            return;

        removedEntity.markRemovedFromScene(this);

        this.inputTickedEntities.remove(removedEntity.getEntityId());

        List<StaticModelEntity> modelList = this.getEntitiesWithModel(removedEntity.getModelId());
        modelList.remove(removedEntity);

        // Save processing models when they're not used.
        if(modelList.isEmpty())
            this.modelSets.remove(removedEntity.getModelId());
    }

    public boolean hasEntity(RenderedEntity entityId) {
        return this.hasEntity(entityId.getEntityId());
    }

    public boolean hasEntity(UUID entityId) {
        return this.entities.containsKey(entityId);
    }

    public Scene setMainCamera(Camera mainCamera) {
        this.mainCamera = this.mainCamera == null
                ? this.fallbackCamera
                : mainCamera;
        return this;
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

    public Camera getMainCamera() {
        return this.mainCamera;
    }
}
