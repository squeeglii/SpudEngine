package me.cg360.spudengine.render.queue;

import me.cg360.spudengine.render.hardware.LogicalDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkQueue;
import org.tinylog.Logger;

public class CommandQueue {

    private final VkQueue queue;

    public CommandQueue(LogicalDevice device, int queueFamilyIndex, int queueIndex) {
        Logger.debug("Creating Command Queue");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            VK11.vkGetDeviceQueue(device.asVk(), queueFamilyIndex, queueIndex, pQueue);
            long queueHandle = pQueue.get(0);

            this.queue = new VkQueue(queueHandle, device.asVk());
        }
    }


    public void waitIdle() {
        VK11.vkQueueWaitIdle(queue);
    }


    public VkQueue getQueue() {
        return this.queue;
    }

}
