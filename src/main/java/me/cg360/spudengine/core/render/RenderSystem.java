package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.PresentQueue;
import me.cg360.spudengine.core.render.geometry.model.ModelManager;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.hardware.PhysicalDevice;
import me.cg360.spudengine.core.render.hardware.Surface;
import me.cg360.spudengine.core.render.command.GraphicsQueue;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureManager;
import me.cg360.spudengine.core.render.impl.RenderProcessInitialiser;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.world.Scene;
import org.joml.Vector3f;
import org.tinylog.Logger;

import java.util.List;

public class RenderSystem {

    private final VulkanInstance vulkanInstance;

    private final LogicalDevice graphicsDevice;
    private final Surface surface;

    private SwapChain swapChain;

    private final RenderProcess renderProcess;
    private final CommandPool commandPool;
    private final GraphicsQueue graphicsQueue;
    private final PresentQueue presentQueue;

    private final PipelineCache pipelineCache;

    private final TextureManager textureManager;
    private final ModelManager modelManager;

    public boolean useWireframe;

    public RenderSystem(Window window, Scene scene, RenderProcessInitialiser processInit, SubRenderProcess[] subRenderProcesses) {
        this.vulkanInstance = new VulkanInstance(EngineProperties.USE_DEBUGGING);

        PhysicalDevice physicalDevice = PhysicalDevice.createPhysicalDevice(this.vulkanInstance, EngineProperties.PREFERRED_DEVICE_NAME);
        this.graphicsDevice = new LogicalDevice(physicalDevice);
        this.surface = new Surface(physicalDevice, window.getHandle());
        this.graphicsQueue = new GraphicsQueue(this.graphicsDevice, 0);
        this.presentQueue = new PresentQueue(this.graphicsDevice, this.surface, 0);

        this.swapChain = new SwapChain(this.graphicsDevice, this.surface, window,
                                       EngineProperties.SWAP_CHAIN_IMAGES, EngineProperties.VSYNC,
                                       this.presentQueue, this.graphicsQueue);

        this.commandPool = new CommandPool(this.graphicsDevice, this.graphicsQueue.getQueueFamilyIndex());
        this.pipelineCache = new PipelineCache(this.graphicsDevice);

        Logger.debug("Creating Render Process...");
        this.renderProcess = processInit.create(this.swapChain, this.commandPool, this.pipelineCache, scene, subRenderProcesses);

        this.textureManager = new TextureManager(this.graphicsDevice);
        this.modelManager = new ModelManager(this, this.graphicsDevice, this.textureManager);
    }

    public void render(Window window, Scene scene, float time) {
        if (window.getWidth() <= 0 && window.getHeight() <= 0)
            return;

        this.renderProcess.waitTillFree();

        int imageIndex;
        if (window.requiresSizeUpdate() || (imageIndex = this.swapChain.acquireNextImage()) < 0 ) {
            window.setRequiresSizeUpdate(false);
            this.onResize(window);
            scene.getProjection().resize(window.getWidth(), window.getHeight());

            imageIndex = this.swapChain.acquireNextImage();
        }

        if(this.textureManager.hasPendingOverlays()) {
            List<Texture> overlaysToUpload = this.textureManager.getPendingOverlays();
            this.renderProcess.processOverlays(this.commandPool, this.graphicsQueue, overlaysToUpload);
        }

        this.renderProcess.recordDraw(this);
        this.renderProcess.submit(this.graphicsQueue);

        if (this.swapChain.presentImage(this.presentQueue, imageIndex))
            window.setRequiresSizeUpdate(true);
    }

    public void onResize(Window window) {
        this.graphicsDevice.waitIdle();
        this.graphicsQueue.waitIdle();
        this.swapChain.cleanup();

        this.swapChain = new SwapChain(this.graphicsDevice, this.surface, window,
                EngineProperties.SWAP_CHAIN_IMAGES, EngineProperties.VSYNC,
                this.presentQueue, this.graphicsQueue);

        this.renderProcess.onResize(this.swapChain);
    }

    public Vector3f getWorldPosFrom(int screenX, int screenY) {
        int currentFrame = this.swapChain.getCurrentFrame();
        Attachment depth = this.renderProcess.getDepthAttachment(currentFrame);

        throw new IllegalStateException("Unimplemented."); //TODO
    }


    public void cleanup() {
        this.presentQueue.waitIdle();
        this.graphicsQueue.waitIdle();
        this.graphicsDevice.waitIdle();

        this.textureManager.cleanup();
        this.modelManager.cleanup();

        this.swapChain.cleanup();

        this.pipelineCache.cleanup();
        this.renderProcess.cleanup();
        this.commandPool.cleanup();

        this.surface.cleanup();
        this.graphicsDevice.cleanup();
        this.graphicsDevice.getPhysicalDevice().cleanup();
        this.vulkanInstance.cleanup();
    }

    public CommandPool getCommandPool() {
        return this.commandPool;
    }

    public GraphicsQueue getGraphicsQueue() {
        return this.graphicsQueue;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public TextureManager getTextureManager() {
        return this.textureManager;
    }

    public RenderProcess getRenderProcess() {
        return this.renderProcess;
    }
}
