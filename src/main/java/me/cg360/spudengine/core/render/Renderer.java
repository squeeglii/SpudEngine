package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.command.PresentQueue;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.hardware.PhysicalDevice;
import me.cg360.spudengine.core.render.hardware.Surface;
import me.cg360.spudengine.core.render.command.GraphicsQueue;
import me.cg360.spudengine.core.world.Scene;

public class Renderer {

    private final VulkanInstance vulkanInstance;

    private final LogicalDevice graphicsDevice;
    private final Surface surface;
    private final GraphicsQueue graphicsQueue;

    private final SwapChain swapChain;


    private final CommandPool commandPool;
    private final PresentQueue presentQueue;
    private final ForwardRendererActivity forwardRenderActivity;

    public Renderer(Window window, Scene scene) {
        this.vulkanInstance = new VulkanInstance(EngineProperties.USE_DEBUGGING);

        PhysicalDevice physicalDevice = PhysicalDevice.createPhysicalDevice(this.vulkanInstance, EngineProperties.PREFERRED_DEVICE_NAME);
        this.graphicsDevice = new LogicalDevice(physicalDevice);
        this.surface = new Surface(physicalDevice, window.getHandle());
        this.graphicsQueue = new GraphicsQueue(this.graphicsDevice, 0);
        this.presentQueue = new PresentQueue(this.graphicsDevice, this.surface, 0);

        this.swapChain = new SwapChain(this.graphicsDevice, this.surface, window,
                                       EngineProperties.SWAP_CHAIN_IMAGES, EngineProperties.VSYNC,
                                       this.presentQueue, new CommandQueue[]{ this.graphicsQueue });

        this.commandPool = new CommandPool(this.graphicsDevice, this.graphicsQueue.getQueueFamilyIndex());
        this.forwardRenderActivity = new ForwardRendererActivity(this.swapChain, this.commandPool);
    }

    public void render(Window window, Scene scene) {
        this.forwardRenderActivity.waitForFence();

        int imageIndex = this.swapChain.acquireNextImage();
        if (imageIndex < 0 ) return;

        this.forwardRenderActivity.submit(this.graphicsQueue);

        this.swapChain.presentImage(this.presentQueue, imageIndex);
    }


    public void cleanup() {
        this.presentQueue.waitIdle();
        this.graphicsQueue.waitIdle();
        this.graphicsDevice.waitIdle();

        this.swapChain.cleanup();

        this.forwardRenderActivity.cleanup();
        this.commandPool.cleanup();

        this.surface.cleanup();
        this.graphicsDevice.cleanup();
        this.graphicsDevice.getPhysicalDevice().cleanup();
        this.vulkanInstance.cleanup();
    }

}
