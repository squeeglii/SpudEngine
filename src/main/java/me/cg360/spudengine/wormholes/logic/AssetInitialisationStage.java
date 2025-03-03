package me.cg360.spudengine.wormholes.logic;

import me.cg360.spudengine.core.GameInstance;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.wormholes.GeneratedAssets;

import java.util.LinkedList;
import java.util.List;

public class AssetInitialisationStage extends GameInstance {

    public AssetInitialisationStage(SpudEngine engineInstance) {
        super("Generated Asset Initialiser", engineInstance);
    }

    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        GeneratedAssets.registerTextures(renderer.getTextureManager());
        this.registerModels();
    }

    public void registerModels() {
        List<Model> modelBatch = new LinkedList<>();

        modelBatch.addAll(GeneratedAssets.getAllModels());

        this.renderer().getModelManager().processModels(modelBatch);
    }

}
