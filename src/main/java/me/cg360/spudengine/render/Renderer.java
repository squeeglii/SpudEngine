package me.cg360.spudengine.render;

import me.cg360.spudengine.EngineProperties;
import me.cg360.spudengine.world.Scene;

public class Renderer {

    private final VulkanInstance vulkanInstance;

    public Renderer(Window window, Scene scene) {
        this.vulkanInstance = new VulkanInstance(EngineProperties.USE_DEBUGGING);
    }

    public void render(Window window, Scene scene) {

    }

    public void cleanup() {
        this.vulkanInstance.cleanup();
    }

}
