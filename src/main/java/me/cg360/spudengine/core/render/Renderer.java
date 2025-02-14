package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.PresentQueue;
import me.cg360.spudengine.core.render.geometry.model.ModelManager;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.hardware.PhysicalDevice;
import me.cg360.spudengine.core.render.hardware.Surface;
import me.cg360.spudengine.core.render.command.GraphicsQueue;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.world.Scene;

public class Renderer {

    private final VulkanInstance vulkanInstance;

    private final LogicalDevice graphicsDevice;
    private final Surface surface;
    private final SwapChain swapChain;

    private final ForwardRendererActivity forwardRenderActivity;
    private final CommandPool commandPool;
    private final GraphicsQueue graphicsQueue;
    private final PresentQueue presentQueue;

    private final PipelineCache pipelineCache;

    private final ModelManager modelManager;

    public Renderer(Window window, Scene scene) {
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
        this.forwardRenderActivity = new ForwardRendererActivity(this.swapChain, this.commandPool, this.pipelineCache);

        this.modelManager = new ModelManager();
    }

    public void render(Window window, Scene scene, float time) {
        this.forwardRenderActivity.waitForFence();

        int imageIndex = this.swapChain.acquireNextImage();
        if (imageIndex < 0 ) return;

        this.forwardRenderActivity.record(this.modelManager.getAllModels(), time);
        this.forwardRenderActivity.submit(this.graphicsQueue);

        this.swapChain.presentImage(this.presentQueue, imageIndex);
    }


    public void cleanup() {
        this.presentQueue.waitIdle();
        this.graphicsQueue.waitIdle();
        this.graphicsDevice.waitIdle();

        this.modelManager.cleanup();

        this.swapChain.cleanup();

        this.pipelineCache.cleanup();
        this.forwardRenderActivity.cleanup();
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
}
