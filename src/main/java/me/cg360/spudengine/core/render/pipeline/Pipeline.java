package me.cg360.spudengine.core.render.pipeline;

import me.cg360.spudengine.core.render.geometry.Attribute;
import me.cg360.spudengine.core.render.geometry.VertexFormatDefinition;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.pipeline.shader.Shader;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class Pipeline implements VkHandleWrapper {

    public static final int ALL_COLOUR_CHANNELS = VK11.VK_COLOR_COMPONENT_R_BIT | VK11.VK_COLOR_COMPONENT_G_BIT | VK11.VK_COLOR_COMPONENT_B_BIT | VK11.VK_COLOR_COMPONENT_A_BIT;

    private final LogicalDevice device;

    private final long pipelineLayout;
    private final long pipeline;

    private final int pushConstantStage;

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

            VkPipelineDepthStencilStateCreateInfo vkDepthStencilState = null;
            if (builder.isUsingDepth()){
                vkDepthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                        .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                        .depthTestEnable(true)
                        .depthWriteEnable(true)
                        .depthCompareOp(VK11.VK_COMPARE_OP_LESS_OR_EQUAL)
                        .depthBoundsTestEnable(false)
                        .stencilTestEnable(false);
            }

            VkPipelineDynamicStateCreateInfo vkDynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                            .pDynamicStates(stack.ints(
                                    VK11.VK_DYNAMIC_STATE_VIEWPORT,   // support window resizing.
                                    VK11.VK_DYNAMIC_STATE_SCISSOR
                            ));


            VkPushConstantRange.Buffer pushConstantRange = null;
            if (builder.getPushConstantSize() > 0) {
                pushConstantRange = VkPushConstantRange.calloc(1, stack)
                        .stageFlags(builder.getPushConstantStage())
                        .offset(0)
                        .size(builder.getPushConstantSize());
                this.pushConstantStage = builder.getPushConstantStage();
            } else this.pushConstantStage = 0;

            VkPipelineLayoutCreateInfo pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                            .pPushConstantRanges(pushConstantRange);

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

            if(vkDepthStencilState != null)
                pipeline.pDepthStencilState(vkDepthStencilState);

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

    public int getPushConstantStage() {
        return this.pushConstantStage;
    }

    public boolean isUsingPerVertexPushConstants() {
        return (this.pushConstantStage & VK11.VK_SHADER_STAGE_VERTEX_BIT) > 0;
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
        private boolean useDepth = true;
        private int cullMode = VK11.VK_CULL_MODE_NONE;
        private boolean useWireframe = false;

        private int pushConstantStage = VK11.VK_SHADER_STAGE_VERTEX_BIT;
        private int[] pushConstantLayout = null;

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
        public Builder setUsingDepth(boolean useDepth) {
            this.useDepth = useDepth;
            return this;
        }

        public Builder setUsingWireframe(boolean useWireframe) {
            this.useWireframe = useWireframe;
            return this;
        }

        public Builder setCullMode(int cullMode) {
            this.cullMode = cullMode;
            return this;
        }

        public Builder setPushConstantStage(int pushConstantStage) {
            this.pushConstantStage = pushConstantStage;
            return this;
        }

        public Builder setPushConstantLayout(int... pushConstantElementSizes) {
            this.pushConstantLayout = pushConstantElementSizes;
            return this;
        }

        public boolean isUsingDepth() {
            return this.useDepth;
        }

        public boolean isUsingWireframe() {
            return this.useWireframe;
        }

        public int getCullMode() {
            return this.cullMode;
        }

        public int getPushConstantStage() {
            return this.pushConstantStage;
        }

        public int getPushConstantSize() {
            if(this.pushConstantLayout == null)
                return 0;

            return Arrays.stream(this.pushConstantLayout).sum();
        }
    }

}
