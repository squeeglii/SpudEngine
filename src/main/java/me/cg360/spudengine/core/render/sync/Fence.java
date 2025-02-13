package me.cg360.spudengine.core.render.sync;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

public class Fence {

    private final LogicalDevice device;
    private final long fence;

    public Fence(LogicalDevice device, boolean signaled) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(signaled ? VK11.VK_FENCE_CREATE_SIGNALED_BIT : 0);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateFence(device.asVk(), fenceCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create fence");
            this.fence = lp.get(0);
        }
    }

    public void cleanup() {
        VK11.vkDestroyFence(this.device.asVk(), this.fence, null);
    }

    public void fenceWait() {
        VK11.vkWaitForFences(this.device.asVk(), this.fence, true, Long.MAX_VALUE);
    }

    /** @return this fence, chaining. */
    public Fence reset() {
        VK11.vkResetFences(this.device.asVk(), this.fence);
        return this;
    }

    public long getHandle() {
        return this.fence;
    }

}
