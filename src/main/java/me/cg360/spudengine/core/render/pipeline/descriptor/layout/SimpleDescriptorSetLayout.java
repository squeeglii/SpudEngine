package me.cg360.spudengine.core.render.pipeline.descriptor.layout;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.tinylog.Logger;

import java.nio.LongBuffer;

public class SimpleDescriptorSetLayout extends DescriptorSetLayout {

    public SimpleDescriptorSetLayout(LogicalDevice device, int type, int binding, int stage) {
        super(device, type, binding, stage);
    }

    @Override
    public long buildDescriptorSetLayout(int type, int binding, int stage) {
        long layout;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            layoutBindings.get(0)
                    .binding(binding)
                    .descriptorType(type)
                    .descriptorCount(1)
                    .stageFlags(stage);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(layoutBindings);

            LongBuffer pSetLayout = stack.mallocLong(1);
            VK11.vkCreateDescriptorSetLayout(this.device.asVk(), layoutInfo, null, pSetLayout);
            layout = pSetLayout.get(0);
        }

        Logger.trace("Created layout of id: {}", layout);
        return layout;
    }
}
