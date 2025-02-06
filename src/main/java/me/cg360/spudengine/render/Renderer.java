package me.cg360.spudengine.render;

import me.cg360.spudengine.EngineProperties;
import me.cg360.spudengine.render.hardware.LogicalDevice;
import me.cg360.spudengine.render.hardware.PhysicalDevice;
import me.cg360.spudengine.render.hardware.Surface;
import me.cg360.spudengine.render.queue.GraphicsQueue;
import me.cg360.spudengine.world.Scene;

public class Renderer {

    private final VulkanInstance vulkanInstance;

    private final LogicalDevice graphicsDevice;
    private final Surface surface;
    private final GraphicsQueue graphicsQueue;

    private final SwapChain swapChain;


    public Renderer(Window window, Scene scene) {
        this.vulkanInstance = new VulkanInstance(EngineProperties.USE_DEBUGGING);

        PhysicalDevice physicalDevice = PhysicalDevice.createPhysicalDevice(this.vulkanInstance, EngineProperties.PREFERRED_DEVICE_NAME);
        this.graphicsDevice = new LogicalDevice(physicalDevice);
        this.surface = new Surface(physicalDevice, window.getHandle());
        this.graphicsQueue = new GraphicsQueue(this.graphicsDevice, 0);

        this.swapChain = new SwapChain(this.graphicsDevice, this.surface, window,
                                       EngineProperties.SWAP_CHAIN_IMAGES, EngineProperties.VSYNC);

    }

    public void render(Window window, Scene scene) {

    }

    public void cleanup() {
        this.swapChain.cleanup();

        this.surface.cleanup();
        this.graphicsDevice.cleanup();
        this.graphicsDevice.getPhysicalDevice().cleanup();
        this.vulkanInstance.cleanup();

    }

}
