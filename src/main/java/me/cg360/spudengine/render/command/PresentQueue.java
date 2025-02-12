package me.cg360.spudengine.render.command;

import me.cg360.spudengine.render.hardware.LogicalDevice;
import me.cg360.spudengine.render.hardware.PhysicalDevice;
import me.cg360.spudengine.render.hardware.Surface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

public class PresentQueue extends CommandQueue {

    public PresentQueue(LogicalDevice device, Surface surface, int queueIndex) {
        super(device, PresentQueue.getPresentQueueFamilyIndex(device, surface), queueIndex);
    }

    private static int getPresentQueueFamilyIndex(LogicalDevice device, Surface surface) {
        int index = -1;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PhysicalDevice physicalDevice = device.getPhysicalDevice();
            VkQueueFamilyProperties.Buffer queuePropsBuff = physicalDevice.getQueueFamilyProperties();

            int numQueuesFamilies = queuePropsBuff.capacity();
            IntBuffer intBuff = stack.mallocInt(1);

            for (int i = 0; i < numQueuesFamilies; i++) {
                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice.asVk(), i, surface.getHandle(), intBuff);
                boolean supportsPresentation = intBuff.get(0) == VK11.VK_TRUE;

                if (supportsPresentation) {
                    index = i;
                    break;
                }
            }
        }

        if (index < 0)
            throw new RuntimeException("Failed to get Presentation Queue family index");

        return index;
    }

}
