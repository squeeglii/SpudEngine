package me.cg360.spudengine.core.render.pipeline.descriptor.active;

import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureSampler;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class SamplerDescriptorSet extends DescriptorSet {

    public SamplerDescriptorSet(DescriptorPool pool, DescriptorSetLayout template, int binding, Texture texture, TextureSampler sampler) {
        super(pool, template, binding); // calls buildDescriptorSet()

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(texture.getImageView().getHandle())
                    .sampler(sampler.getHandle());

            VkWriteDescriptorSet.Buffer descrBuffer = VkWriteDescriptorSet.calloc(1, stack);
            descrBuffer.get(0)
                    .sType(VK11.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(this.descriptorSet)
                    .dstBinding(binding)
                    .descriptorType(VK11.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            VK11.vkUpdateDescriptorSets(pool.getDevice().asVk(), descrBuffer, null);
        }
    }
}
