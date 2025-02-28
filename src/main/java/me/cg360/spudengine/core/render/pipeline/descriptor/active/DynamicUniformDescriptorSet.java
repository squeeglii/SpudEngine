package me.cg360.spudengine.core.render.pipeline.descriptor.active;

import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import org.lwjgl.vulkan.VK11;

public class DynamicUniformDescriptorSet extends SimpleDescriptorSet {

    protected GeneralBuffer buffer;

    public DynamicUniformDescriptorSet(DescriptorPool pool, DescriptorSetLayout template, GeneralBuffer buffer, int binding) {
        super(pool, template, buffer, binding, VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, buffer.getRequestedSize());
        this.buffer = buffer;
    }

    public GeneralBuffer getBuffer() {
        return this.buffer;
    }


}
