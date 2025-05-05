package me.cg360.spudengine.wormholes.logic;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.render.geometry.model.ModelLoader;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureManager;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.wormholes.GeneratedAssets;
import org.lwjgl.vulkan.VK11;

import java.util.LinkedList;
import java.util.List;

public class AssetInitialisationStage extends GameComponent {

    public AssetInitialisationStage(GameComponent parent, SpudEngine engineInstance) {
        super(GameComponent.sub(parent, "asset_init"), engineInstance);
    }

    @Override
    protected void onInit(Window window, Scene scene, RenderSystem renderSystem) {
        GeneratedAssets.registerTextures(renderSystem.getTextureManager());
        this.registerFilesystemTextures(renderSystem);
        this.registerModels();
    }

    public List<Model> loadEnvironmentModels() {
        return List.of(
                ModelLoader.loadEnvironmentModel("env/chamber01"),
                ModelLoader.loadEnvironmentModel("env/chamber_01_texupdate")
        );
    }

    public void registerFilesystemTextures(RenderSystem renderSystem) {
        TextureManager texManager = renderSystem.getTextureManager();
        LogicalDevice device = renderSystem.getCommandPool().getDevice();
        int format = VK11.VK_FORMAT_R8G8B8A8_SRGB;

        Texture uvTexture = texManager.loadTexture(device, "debug/uv_square.png", format);
        texManager.markAsOverlays(uvTexture);
    }

    public void registerModels() {
        List<Model> modelBatch = new LinkedList<>();

        modelBatch.addAll(GeneratedAssets.getAllModels());
        modelBatch.addAll(this.loadEnvironmentModels());

        this.renderer().getModelManager().processModels(modelBatch);
    }

}
