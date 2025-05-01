package me.cg360.spudengine.core.render.pipeline.descriptor.active;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.tinylog.Logger;

import java.nio.LongBuffer;

public abstract class DescriptorSet {

    protected final long descriptorSet;

    public DescriptorSet(DescriptorPool pool, DescriptorSetLayout template, int binding) {
        Logger.trace("Building DescriptorSet of type {}", template.getDescriptorSetType());
        this.descriptorSet = this.buildDescriptorSet(pool, template, binding);
    }

    protected long buildDescriptorSet(DescriptorPool pool, DescriptorSetLayout template, int binding) {
        long descriptorSet;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LogicalDevice device = pool.getDevice();
            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            pDescriptorSetLayout.put(0, template.getHandle());
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(pool.getHandle())
                    .pSetLayouts(pDescriptorSetLayout);

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            int errCode = VK11.vkAllocateDescriptorSets(device.asVk(), allocInfo, pDescriptorSet);
            VulkanUtil.checkErrorCode(errCode, () -> {
                Logger.debug(pool);
            }, "Failed to create descriptor set");

            descriptorSet = pDescriptorSet.get(0);
        }

        return descriptorSet;
    }

    public final long getHandle() {
        return this.descriptorSet;
    }
}
