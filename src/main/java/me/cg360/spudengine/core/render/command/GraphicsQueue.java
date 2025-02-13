package me.cg360.spudengine.core.render.command;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.hardware.PhysicalDevice;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

public class GraphicsQueue extends CommandQueue {

    public GraphicsQueue(LogicalDevice device, int queueIndex) {
        super(device, GraphicsQueue.getGraphicsQueueFamilyIndex(device), queueIndex);
    }

    private static int getGraphicsQueueFamilyIndex(LogicalDevice device) {
        int index = -1;
        PhysicalDevice physicalDevice = device.getPhysicalDevice();
        VkQueueFamilyProperties.Buffer queuePropsBuff = physicalDevice.getQueueFamilyProperties();
        int numQueuesFamilies = queuePropsBuff.capacity();

        for (int i = 0; i < numQueuesFamilies; i++) {
            VkQueueFamilyProperties props = queuePropsBuff.get(i);
            boolean graphicsQueue = (props.queueFlags() & VK11.VK_QUEUE_GRAPHICS_BIT) != 0;

            if (graphicsQueue) {
                index = i;
                break;
            }
        }

        if (index < 0)
            throw new RuntimeException("Failed to get graphics Queue family index");

        return index;
    }

}
