package me.cg360.spudengine.render;

import me.cg360.spudengine.render.hardware.LogicalDevice;
import me.cg360.spudengine.util.VkHandleWrapper;
import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

public class Framebuffer implements VkHandleWrapper {

    private final LogicalDevice device;
    private final long framebufferHandle;

    public Framebuffer(LogicalDevice device, int width, int height, LongBuffer pAttachments, long renderPass) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .pAttachments(pAttachments)
                    .width(width)
                    .height(height)
                    .layers(1)
                    .renderPass(renderPass);

            LongBuffer lp = stack.mallocLong(1);
            int errCreateFB = VK11.vkCreateFramebuffer(device.asVk(), fci, null, lp);
            VulkanUtil.checkErrorCode(errCreateFB, "Failed to create framebuffer");

            this.framebufferHandle = lp.get(0);
        }
    }

    public void cleanup() {
        VK11.vkDestroyFramebuffer(this.device.asVk(), this.framebufferHandle, null);
    }

    public long getHandle() {
        return this.framebufferHandle;
    }

}
