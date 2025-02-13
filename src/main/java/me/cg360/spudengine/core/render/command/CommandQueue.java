package me.cg360.spudengine.core.render.command;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class CommandQueue {

    private final int queueFamilyIndex;
    private final VkQueue queue;

    public CommandQueue(LogicalDevice device, int queueFamilyIndex, int queueIndex) {
        Logger.debug("Creating Command Queue");
        this.queueFamilyIndex = queueFamilyIndex;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            VK11.vkGetDeviceQueue(device.asVk(), queueFamilyIndex, queueIndex, pQueue);
            long queueHandle = pQueue.get(0);

            this.queue = new VkQueue(queueHandle, device.asVk());
        }
    }

    public void submit(PointerBuffer commandBuffers, LongBuffer waitSemaphores, IntBuffer dstStageMasks, LongBuffer signalSemaphores, Fence fence) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(commandBuffers)
                    .pSignalSemaphores(signalSemaphores);

            if (waitSemaphores != null) {
                submitInfo.waitSemaphoreCount(waitSemaphores.capacity())
                          .pWaitSemaphores(waitSemaphores)
                          .pWaitDstStageMask(dstStageMasks);
            } else {
                submitInfo.waitSemaphoreCount(0);
            }

            long fenceHandle = fence != null
                    ? fence.getHandle()
                    : VK11.VK_NULL_HANDLE;

            int errSubmit = VK11.vkQueueSubmit(this.queue, submitInfo, fenceHandle);
            VulkanUtil.checkErrorCode(errSubmit, "Failed to submit command to queue");
        }
    }

    public void waitIdle() {
        VK11.vkQueueWaitIdle(this.queue);
    }

    public VkQueue asVk() {
        return this.queue;
    }

    public int getQueueFamilyIndex() {
        return this.queueFamilyIndex;
    }
}
