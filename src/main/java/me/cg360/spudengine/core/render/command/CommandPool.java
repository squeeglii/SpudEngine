package me.cg360.spudengine.core.render.command;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.tinylog.Logger;

import java.nio.LongBuffer;

public class CommandPool implements VkHandleWrapper {

    private final LogicalDevice device;
    private final long commandPoolHandle;

    public CommandPool(LogicalDevice device, int queueFamilyIndex) {
        Logger.debug("Creating Vulkan CommandPool");

        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK11.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT) // simpler. Disable for performance if pre-recording.
                    .queueFamilyIndex(queueFamilyIndex);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateCommandPool(this.device.asVk(), cmdPoolInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create command pool");

            this.commandPoolHandle = lp.get(0);
        }
    }

    public void cleanup() {
        VK11.vkDestroyCommandPool(this.device.asVk(), this.commandPoolHandle, null);
    }

    public LogicalDevice getDevice() {
        return this.device;
    }

    public long getHandle() {
        return this.commandPoolHandle;
    }

}
