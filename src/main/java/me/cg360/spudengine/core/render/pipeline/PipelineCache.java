package me.cg360.spudengine.core.render.pipeline;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import org.tinylog.Logger;

import java.nio.LongBuffer;

public class PipelineCache implements VkHandleWrapper {

    private final LogicalDevice device;
    private final long pipelineCache;

    public PipelineCache(LogicalDevice device) {
        Logger.debug("Creating pipeline cache");
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreatePipelineCache(device.asVk(), createInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create pipeline cache");

            this.pipelineCache = lp.get(0);
        }
    }

    public void cleanup() {
        Logger.debug("Destroying pipeline cache");
        VK11.vkDestroyPipelineCache(this.device.asVk(), this.pipelineCache, null);
    }

    public LogicalDevice getDevice() {
        return this.device;
    }

    public long getHandle() {
        return this.pipelineCache;
    }

}
