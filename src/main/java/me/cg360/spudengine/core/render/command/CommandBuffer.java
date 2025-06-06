package me.cg360.spudengine.core.render.command;

import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

public class CommandBuffer {

    private final VkCommandBuffer commandBuffer;
    private final CommandPool commandPool;
    private boolean primary;
    private final boolean oneTimeSubmit;

    public CommandBuffer(CommandPool commandPool, boolean primary, boolean oneTimeSubmit) {
        Logger.trace("Creating command buffer");
        this.commandPool = commandPool;
        this.primary = primary;
        this.oneTimeSubmit = oneTimeSubmit;
        VkDevice vkDevice = commandPool.getDevice().asVk();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.getHandle())
                    .level(primary ? VK11.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK11.VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                    .commandBufferCount(1);

            PointerBuffer pb = stack.mallocPointer(1);
            int errAllocate = VK11.vkAllocateCommandBuffers(vkDevice, cmdBufAllocateInfo, pb);
            VulkanUtil.checkErrorCode(errAllocate, "Failed to allocate command buffer");

            this.commandBuffer = new VkCommandBuffer(pb.get(0), vkDevice);
        }

    }

    public CommandBuffer record(Runnable runnable) {
        this.beginRecording();
        runnable.run();
        this.endRecording();
        return this;
    }

    public CommandBuffer record(SecondaryInheritance inheritanceInfo, Runnable runnable) {
        this.beginRecording(inheritanceInfo);
        runnable.run();
        this.endRecording();
        return this;
    }

    public VkCommandBuffer beginRecording() {
        this.beginRecording(null);
        return this.asVk();
    }

    public VkCommandBuffer beginRecording(SecondaryInheritance inheritanceInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            // Short-lived commands.
            if (this.oneTimeSubmit)
                cmdBufInfo.flags(VK11.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            // If this is only needed for primary, should Primary & Secondary command buffers be different classes?
            if (!this.primary) {
                if (inheritanceInfo == null)
                    throw new RuntimeException("Secondary buffers must declare inheritance info");

                VkCommandBufferInheritanceInfo vkInheritanceInfo = VkCommandBufferInheritanceInfo.calloc(stack)
                        .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                        .renderPass(inheritanceInfo.renderPass())
                        .subpass(inheritanceInfo.subPass())
                        .framebuffer(inheritanceInfo.framebuffer());
                cmdBufInfo.pInheritanceInfo(vkInheritanceInfo);
                cmdBufInfo.flags(VK11.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
            }

            int errCreate = VK11.vkBeginCommandBuffer(this.commandBuffer, cmdBufInfo);
            VulkanUtil.checkErrorCode(errCreate, "Failed to begin command buffer recording");
        }

        return this.asVk();
    }

    public CommandBuffer endRecording() {
        int errClear = VK11.vkEndCommandBuffer(this.commandBuffer);
        VulkanUtil.checkErrorCode(errClear, "Failed to end command buffer recording");
        return this;
    }

    public void reset() {
        VK11.vkResetCommandBuffer(this.commandBuffer, VK11.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
    }

    public void submitAndWait(CommandQueue queue) {
        Fence fence = new Fence(this.commandPool.getDevice(), true).reset();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            queue.submit(stack.pointers(this.asVk()), null, null, null, fence);
        }

        fence.fenceWait();
        fence.cleanup();
        this.cleanup();
    }

    public void cleanup() {
        Logger.trace("Destroying command buffer");
        VK11.vkFreeCommandBuffers(this.commandPool.getDevice().asVk(), this.commandPool.getHandle(), this.commandBuffer);
    }

    public VkCommandBuffer asVk() {
        return this.commandBuffer;
    }

}
