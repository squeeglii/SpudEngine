package me.cg360.spudengine.core.render.sync;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

public class Semaphore implements VkHandleWrapper {

    private final LogicalDevice device;
    private final long semaphore;

    public Semaphore(LogicalDevice device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateSemaphore(device.asVk(), semaphoreCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create semaphore");
            this.semaphore = lp.get(0);
        }
    }

    public void cleanup() {
        VK11.vkDestroySemaphore(this.device.asVk(), this.semaphore, null);
    }

    public long getHandle() {
        return this.semaphore;
    }

}
