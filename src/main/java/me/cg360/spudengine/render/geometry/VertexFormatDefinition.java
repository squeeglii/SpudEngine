package me.cg360.spudengine.render.geometry;

import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public class VertexFormatDefinition {

    protected VkPipelineVertexInputStateCreateInfo definitionHandle;

    private final VkVertexInputAttributeDescription.Buffer attributesBuffer;
    private final VkVertexInputBindingDescription.Buffer bindingsBuffer;

    public VertexFormatDefinition(Attribute... attributes) {
        this.attributesBuffer = VkVertexInputAttributeDescription.calloc(attributes.length);
        this.bindingsBuffer = VkVertexInputBindingDescription.calloc(1);
        this.definitionHandle = VkPipelineVertexInputStateCreateInfo.calloc();

        int currentIndex = 0;

        for(int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];

            // TODO: remove if everything is working well.
            //       this is just a sanity check.
            if(attribute.binding() != VertexFormats.DEFAULT_BINDING)
                throw new IllegalStateException("Vertex Format uses a non-zero binding! This isn't supported yet!");

            this.attributesBuffer.get(i)
                    .binding(attribute.binding())
                    .location(i)
                    .format(attribute.format())
                    .offset(currentIndex);

            currentIndex += attribute.size();
        }

        // currentIndex is now the size.

        this.bindingsBuffer.get(0)
                .binding(VertexFormats.DEFAULT_BINDING)
                .stride(currentIndex)
                .inputRate(VK11.VK_VERTEX_INPUT_RATE_VERTEX);

        this.definitionHandle.sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                         .pVertexBindingDescriptions(this.bindingsBuffer)
                         .pVertexAttributeDescriptions(this.attributesBuffer);
    }

    public void cleanup() {
        this.definitionHandle.free();
        this.bindingsBuffer.free();
        this.attributesBuffer.free();
    }

    public VkPipelineVertexInputStateCreateInfo asVk() {
        return this.definitionHandle;
    }
}
