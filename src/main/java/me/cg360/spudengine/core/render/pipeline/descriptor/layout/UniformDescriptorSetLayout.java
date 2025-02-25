package me.cg360.spudengine.core.render.pipeline.descriptor.layout;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.vulkan.VK11;

public class UniformDescriptorSetLayout extends SimpleDescriptorSetLayout {

    public UniformDescriptorSetLayout(LogicalDevice device, int binding, int stage) {
        super(device, VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, binding, stage);
    }

}
