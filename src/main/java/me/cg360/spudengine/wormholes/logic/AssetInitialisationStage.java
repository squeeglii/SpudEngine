package me.cg360.spudengine.wormholes.logic;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.render.geometry.model.ModelLoader;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.wormholes.GeneratedAssets;

import java.util.LinkedList;
import java.util.List;

public class AssetInitialisationStage extends GameComponent {

    public AssetInitialisationStage(GameComponent parent, SpudEngine engineInstance) {
        super(GameComponent.sub(parent, "asset_init"), engineInstance);
    }

    @Override
    protected void onInit(Window window, Scene scene, RenderSystem renderSystem) {
        GeneratedAssets.registerTextures(renderSystem.getTextureManager());
        this.registerModels();
    }

    public List<Model> loadEnvironmentModels() {
        return List.of(
                ModelLoader.loadEnvironmentModel("env/chamber01"),
                ModelLoader.loadEnvironmentModel("env/chamber_01_texupdate")
        );
    }

    public void registerModels() {
        List<Model> modelBatch = new LinkedList<>();

        modelBatch.addAll(GeneratedAssets.getAllModels());
        modelBatch.addAll(this.loadEnvironmentModels());

        this.renderer().getModelManager().processModels(modelBatch);
    }

}
