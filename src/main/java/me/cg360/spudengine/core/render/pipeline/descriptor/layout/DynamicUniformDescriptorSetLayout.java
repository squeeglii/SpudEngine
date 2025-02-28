package me.cg360.spudengine.core.render.pipeline.descriptor.layout;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.vulkan.VK11;

public class DynamicUniformDescriptorSetLayout extends SimpleDescriptorSetLayout {

    public DynamicUniformDescriptorSetLayout(LogicalDevice device, int binding, int stage) {
        super(device, VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, binding, stage);
    }

}
