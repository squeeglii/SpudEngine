package me.cg360.spudengine.core.render.pipeline.descriptor.active;

import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import org.lwjgl.vulkan.VK11;

public class UniformDescriptorSet extends SimpleDescriptorSet {

    protected GeneralBuffer buffer;

    public UniformDescriptorSet(DescriptorPool pool, DescriptorSetLayout template, GeneralBuffer buffer, int binding) {
        super(pool, template, buffer, binding, VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, buffer.getRequestedSize());
        this.buffer = buffer;
    }

    public GeneralBuffer getBuffer() {
        return this.buffer;
    }

    public static UniformDescriptorSet[] create(DescriptorPool pool, DescriptorSetLayout template, TypeHelper type, int binding) {
        return UniformDescriptorSet.create(pool, template, type, binding, VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
    }

    public static UniformDescriptorSet[] create(DescriptorPool pool, DescriptorSetLayout template, TypeHelper type, int binding, int reqMask) {
        int count = template.getCount();
        if(count < 1)
            throw new IllegalStateException("UniformDescriptorSetLayout must specify more than 1 set");

        UniformDescriptorSet[] sets = new UniformDescriptorSet[count];

        for(int i = 0; i < count; i++) {
            GeneralBuffer setBuffer = type.allocateOne(pool.getDevice(), VK11.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, reqMask);
            sets[i] = new UniformDescriptorSet(pool, template, setBuffer, binding);
        }

        return sets;
    }
}
