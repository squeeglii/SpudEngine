package me.cg360.spudengine.render.pipeline;

import me.cg360.spudengine.render.geometry.VertexFormatDefinition;
import me.cg360.spudengine.render.hardware.LogicalDevice;
import me.cg360.spudengine.render.pipeline.shader.Shader;
import me.cg360.spudengine.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.util.VkHandleWrapper;
import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class Pipeline implements VkHandleWrapper {

    public static final int ALL_COLOUR_CHANNELS = VK11.VK_COLOR_COMPONENT_R_BIT | VK11.VK_COLOR_COMPONENT_G_BIT | VK11.VK_COLOR_COMPONENT_B_BIT | VK11.VK_COLOR_COMPONENT_A_BIT;

    private final LogicalDevice device;

    private final long pipelineLayout;
    private final long pipeline;

    public Pipeline(PipelineCache pipelineCache, Builder builder) {
        Logger.debug("Creating pipeline");
        this.device = pipelineCache.getDevice();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            ByteBuffer entrypoint = stack.UTF8("main");

            Shader[] shaderModules = builder.shaderProgram.getShaderModules();
            int numShaders = shaderModules.length;
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(numShaders, stack);

            for (int i = 0; i < numShaders; i++) {
                Shader shader = shaderModules[i];
                shaderStages.get(i)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                            .stage(shader.stage())
                            .module(shader.handle())
                            .pName(entrypoint);
            }

            VkPipelineInputAssemblyStateCreateInfo vkInpAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                            .topology(VK11.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);  // render type (i.e, lines, points, tris)

            VkPipelineViewportStateCreateInfo vkViewport = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1); // portals may need multiple?

            VkPipelineRasterizationStateCreateInfo vkRasterisation = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                            .polygonMode(builder.isUsingWireframe() ? VK11.VK_POLYGON_MODE_LINE : VK11.VK_POLYGON_MODE_FILL)
                            .cullMode(builder.getCullMode())
                            .frontFace(VK11.VK_FRONT_FACE_CLOCKWISE)
                            .lineWidth(1.0f);

            VkPipelineMultisampleStateCreateInfo vkMultisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                            .rasterizationSamples(VK11.VK_SAMPLE_COUNT_1_BIT); // No multisampling.

            VkPipelineColorBlendAttachmentState.Buffer blendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(builder.numColourAttachments, stack);
            for (int i = 0; i < builder.numColourAttachments; i++)
                blendAttachmentState.get(i).colorWriteMask(ALL_COLOUR_CHANNELS);

            VkPipelineColorBlendStateCreateInfo vkColorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                            .pAttachments(blendAttachmentState);

            VkPipelineDynamicStateCreateInfo vkDynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                            .pDynamicStates(stack.ints(
                                    VK11.VK_DYNAMIC_STATE_VIEWPORT,   // support window resizing.
                                    VK11.VK_DYNAMIC_STATE_SCISSOR
                            ));

            VkPipelineLayoutCreateInfo pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

            int errCreateLayout = VK11.vkCreatePipelineLayout(this.device.asVk(), pPipelineLayoutCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreateLayout, "Failed to create pipeline layout");
            this.pipelineLayout = lp.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(shaderStages)
                    .pVertexInputState(builder.vertexFormatDefinition.asVk())
                    .pInputAssemblyState(vkInpAssembly)
                    .pViewportState(vkViewport)
                    .pRasterizationState(vkRasterisation)
                    .pMultisampleState(vkMultisample)
                    .pColorBlendState(vkColorBlendState)
                    .pDynamicState(vkDynamicStates)
                    .layout(this.pipelineLayout)
                    .renderPass(builder.renderPass);

            int errCreate = VK11.vkCreateGraphicsPipelines(this.device.asVk(), pipelineCache.getHandle(), pipeline, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Error creating graphics pipeline");
            this.pipeline = lp.get(0);
        }
    }


    public void cleanup() {
        Logger.debug("Destroying pipeline");
        VK11.vkDestroyPipelineLayout(this.device.asVk(), this.pipelineLayout, null);
        VK11.vkDestroyPipeline(this.device.asVk(), this.pipeline, null);
    }

    public long getHandle() {
        return this.pipeline;
    }

    public long getPipelineLayoutHandle() {
        return this.pipelineLayout;
    }


    public static Builder builder(VertexFormatDefinition vertexFormatDefinition) {
        return new Builder(vertexFormatDefinition);
    }


    public static class Builder {

        // Essential
        private long renderPass;
        private ShaderProgram shaderProgram;
        private int numColourAttachments;
        private VertexFormatDefinition vertexFormatDefinition;

        // Optional
        private int cullMode = VK11.VK_CULL_MODE_NONE;
        private boolean useWireframe = false;

        public Builder(VertexFormatDefinition format) {
            this.vertexFormatDefinition = format;
        }

        public Pipeline build(PipelineCache pipelineCache, long renderPass, ShaderProgram shaderProgram, int numColorAttachments) {
            this.renderPass = renderPass;
            this.shaderProgram = shaderProgram;
            this.numColourAttachments = numColorAttachments;

            return new Pipeline(pipelineCache, this);
        }

        public void cleanup() {
            this.vertexFormatDefinition.cleanup();
        }

        // Setters
        public void setWireFrameEnabled(boolean useWireframe) {
            this.useWireframe = useWireframe;
        }

        public void setCullMode(int cullMode) {
            this.cullMode = cullMode;
        }


        public boolean isUsingWireframe() {
            return this.useWireframe;
        }

        public int getCullMode() {
            return this.cullMode;
        }
    }

}
