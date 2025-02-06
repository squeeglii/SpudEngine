package me.cg360.spudengine.render.hardware;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkInstance;
import org.tinylog.Logger;

import java.nio.LongBuffer;

public class Surface {

    private final PhysicalDevice physicalDevice;
    private final long vkSurface;

    public Surface(PhysicalDevice physicalDevice, long windowHandle) {
        Logger.debug("Creating Vulkan surface");
        this.physicalDevice = physicalDevice;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            VkInstance vkInstance = this.physicalDevice.asVk().getInstance();
            GLFWVulkan.glfwCreateWindowSurface(vkInstance, windowHandle, null, pSurface);

            this.vkSurface = pSurface.get(0);
        }
    }

    public void cleanup() {
        Logger.debug("Destroying Vulkan surface");
        VkInstance vkInstance = this.physicalDevice.asVk().getInstance();
        KHRSurface.vkDestroySurfaceKHR(vkInstance, this.vkSurface, null);
    }

    public long asVk() {
        return this.vkSurface;
    }
}
