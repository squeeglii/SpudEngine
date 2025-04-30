package me.cg360.spudengine.core.render.pipeline;

import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.geometry.VertexFormatDefinition;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.Shader;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.util.BlendFunc;
import me.cg360.spudengine.core.render.pipeline.util.ShaderStage;
import me.cg360.spudengine.core.render.pipeline.util.stencil.StencilConfig;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

public class Pipeline implements VkHandleWrapper {

    public static final int ALL_COLOUR_CHANNELS = VK11.VK_COLOR_COMPONENT_R_BIT | VK11.VK_COLOR_COMPONENT_G_BIT | VK11.VK_COLOR_COMPONENT_B_BIT | VK11.VK_COLOR_COMPONENT_A_BIT;

    private final LogicalDevice device;

    private final long pipelineLayout;
    private final long pipeline;

    private final ShaderStage pushConstantStage;
    private final int pushConstantSize;

    private final String nickname;

    public Pipeline(PipelineCache pipelineCache, Builder builder) {
        this.nickname = builder.debugNickname == null ? "anonymous" : builder.debugNickname;
        this.device = pipelineCache.getDevice();

        Logger.debug("Constructing pipeline [{}] ...", nickname);

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
            for (int i = 0; i < builder.numColourAttachments; i++) {
                VkPipelineColorBlendAttachmentState state = blendAttachmentState.get(i)
                        .colorWriteMask(builder.colourWriteMask)
                        .colorBlendOp(builder.colourBlendOp)
                        .blendEnable(builder.useBlend);

                if(builder.blendFunc != null)
                    builder.blendFunc.configure(state);
            }

            VkPipelineColorBlendStateCreateInfo vkColorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                            .pAttachments(blendAttachmentState);

            VkPipelineDepthStencilStateCreateInfo vkDepthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthBoundsTestEnable(false)
                    .depthTestEnable(builder.isUsingDepthTest())
                    .depthWriteEnable(builder.isWritingDepth())
                    .depthCompareOp(VK11.VK_COMPARE_OP_LESS_OR_EQUAL);

            if (builder.isStencilConfigSpecified()){
                vkDepthStencilState.stencilTestEnable(builder.isUsingStencilTest());

                builder.getStencilFrontConfig().ifPresent(frontConfig -> {
                    VkStencilOpState front = frontConfig.configure(stack);
                    vkDepthStencilState.front(front);
                });

                builder.getStencilBackConfig().ifPresent(backConfig -> {
                    VkStencilOpState back = backConfig.configure(stack);
                    vkDepthStencilState.back(back);
                });

            } else {
                vkDepthStencilState.stencilTestEnable(false);
            }

            VkPipelineDynamicStateCreateInfo vkDynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                            .pDynamicStates(stack.ints(
                                    VK11.VK_DYNAMIC_STATE_VIEWPORT,   // support window resizing.
                                    VK11.VK_DYNAMIC_STATE_SCISSOR
                            ));

            VkPushConstantRange.Buffer pushConstantRange = null;
            if (builder.getPushConstantSize() > 0) {
                this.pushConstantStage = builder.getPushConstantStage();
                this.pushConstantSize = builder.getPushConstantSize();

                pushConstantRange = VkPushConstantRange.calloc(1, stack)
                        .stageFlags(this.pushConstantStage.bitMask())
                        .offset(0)
                        .size(this.pushConstantSize);

                Logger.trace("Push Constants set to {} Mode! (size: {})", this.pushConstantStage, this.pushConstantSize);

            } else {
                Logger.trace("Push Constants disabled for Pipeline Layout (size = 0)!");
                this.pushConstantStage = ShaderStage.NONE;
                this.pushConstantSize = 0;
            }

            DescriptorSetLayout[] descriptorSetLayouts = builder.getDescriptorLayouts();
            int numLayouts = descriptorSetLayouts != null ? descriptorSetLayouts.length : 0;
            LongBuffer ppLayout = stack.mallocLong(numLayouts);
            List<Long> ppLayoutDebug = new ArrayList<>(numLayouts);
            for (int i = 0; i < numLayouts; i++) {
                ppLayout.put(i, descriptorSetLayouts[i].getHandle());
                ppLayoutDebug.add(ppLayout.get(i));
            }

            Logger.trace("Pipeline created with descriptor layout: {} - ({} elements)", ppLayoutDebug, numLayouts);
            Logger.trace("Pipeline layout push constants: {}", pushConstantRange);

            VkPipelineLayoutCreateInfo pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                            .pSetLayouts(ppLayout)
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
                    .renderPass(builder.renderPass)
                    .subpass(builder.subpass)
                    .pDepthStencilState(vkDepthStencilState);

            int errCreate = VK11.vkCreateGraphicsPipelines(this.device.asVk(), pipelineCache.getHandle(), pipeline, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Error creating graphics pipeline");
            this.pipeline = lp.get(0);
            Logger.debug("Created Pipeline [{}] (id: 0x{}, layout: 0x{})!",
                    this.nickname,
                    HexFormat.of().toHexDigits(this.pipeline),
                    HexFormat.of().toHexDigits(this.pipelineLayout)
            );
        }
    }

    public Pipeline bind(VkCommandBuffer cmd) {
        return this.bind(cmd, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    public Pipeline bind(VkCommandBuffer cmd, int bindPoint) {
        //Logger.trace("Binding pipeline [{}]", this.nickname);
        VK11.vkCmdBindPipeline(cmd, bindPoint, this.getHandle());
        return this;
    }

    public void cleanup() {
        Logger.debug("Destroying pipeline [{}]", this.nickname);
        VK11.vkDestroyPipelineLayout(this.device.asVk(), this.pipelineLayout, null);
        VK11.vkDestroyPipeline(this.device.asVk(), this.pipeline, null);
    }

    public long getHandle() {
        return this.pipeline;
    }

    public long getPipelineLayoutHandle() {
        return this.pipelineLayout;
    }

    public int getPushConstantSize() {
        return this.pushConstantSize;
    }

    public ShaderStage getPushConstantStage() {
        return this.pushConstantStage;
    }

    public String getNickname() {
        return this.nickname;
    }

    public static Builder builder(VertexFormatDefinition vertexFormatDefinition) {
        return new Builder(vertexFormatDefinition);
    }


    public static class Builder {

        // Essential
        private String debugNickname;
        private long renderPass;
        private int subpass;
        private ShaderProgram shaderProgram;
        private int numColourAttachments;
        private VertexFormatDefinition vertexFormatDefinition;

        // Optional
        private int cullMode = VK11.VK_CULL_MODE_NONE;
        private boolean useWireframe = false;

        private ShaderStage pushConstantStage = ShaderStage.VERTEX;
        private TypeHelper[] pushConstantLayout = null;

        private DescriptorSetLayout[] descriptorLayouts;

        private boolean useDepthTest;
        private boolean useDepthWrite;

        private boolean enableStencilTest;
        private StencilConfig stencilFront;
        private StencilConfig stencilBack;

        private int colourWriteMask;

        private boolean useBlend;
        private int colourBlendOp;
        private BlendFunc blendFunc;

        public Builder(VertexFormatDefinition format) {
            this.vertexFormatDefinition = format;
            this.descriptorLayouts = null;

            this.colourWriteMask = ALL_COLOUR_CHANNELS;
            this.colourBlendOp = VK11.VK_BLEND_OP_ADD;
            this.blendFunc = BlendFunc.DEFAULT;

            this.resetDepth();
            this.resetStencil();
        }

        public Pipeline build(PipelineCache pipelineCache, String nickname, long renderPass, ShaderProgram shaderProgram, int numColorAttachments) {
            return this.build(pipelineCache, nickname, renderPass, 0, shaderProgram, numColorAttachments);
        }

        public Pipeline build(PipelineCache pipelineCache, String nickname, long renderPass, int subpass, ShaderProgram shaderProgram, int numColorAttachments) {
            this.debugNickname = nickname;
            this.renderPass = renderPass;
            this.subpass = subpass;
            this.shaderProgram = shaderProgram;
            this.numColourAttachments = numColorAttachments;

            return new Pipeline(pipelineCache, this);
        }

        public void cleanup() {
            this.vertexFormatDefinition.cleanup();
        }

        // Setters that reset things
        public Builder resetStencil() {
            this.stencilFront = null;
            this.stencilBack = null;
            this.enableStencilTest = false;
            return this;
        }

        public Builder resetDepth() {
            this.useDepthWrite = true;
            this.useDepthTest = true;
            return this;
        }


        // Setters
        public Builder setUsingDepthTest(boolean useDepth) {
            this.useDepthTest = useDepth;
            return this;
        }

        public Builder setUsingDepthWrite(boolean useDepthWrite) {
            this.useDepthWrite = useDepthWrite;
            return this;
        }

        public Builder setUsingStencilTest(boolean enableStencilTest) {
            this.enableStencilTest = enableStencilTest;
            return this;
        }

        public Builder setColourWriteMask(int colourWriteMask) {
            this.colourWriteMask = colourWriteMask;
            return this;
        }

        public Builder disableColourWrite() {
            this.colourWriteMask = 0;
            return this;
        }

        public Builder enableColourWrite() {
            this.colourWriteMask = ALL_COLOUR_CHANNELS;
            return this;
        }

        public Builder enableAlphaWrite() {
            this.colourWriteMask |= VK11.VK_COLOR_COMPONENT_A_BIT;
            return this;
        }

        public Builder disableAlphaWrite() {
            this.colourWriteMask &= ~VK11.VK_COLOR_COMPONENT_A_BIT;
            return this;
        }

        public Builder setUsingBlend(boolean useBlend) {
            this.useBlend = useBlend;
            return this;
        }

        public Builder setColourBlendOp(int colourBlendOp) {
            this.colourBlendOp = colourBlendOp;
            return this;
        }

        public Builder setBlendFunc(BlendFunc blendFunc) {
            this.blendFunc = blendFunc == null
                    ? BlendFunc.DEFAULT
                    : blendFunc;
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

        public Builder setPushConstantStage(ShaderStage pushConstantStage) {
            this.pushConstantStage = pushConstantStage;
            return this;
        }

        public Builder setPushConstantLayout(TypeHelper... pushConstantElements) {
            this.pushConstantLayout = pushConstantElements;
            return this;
        }

        public Builder setDescriptorLayouts(DescriptorSetLayout... descriptorLayout) {
            this.descriptorLayouts = descriptorLayout;
            return this;
        }

        public Builder setStencilFront(StencilConfig stencilFront) {
            this.stencilFront = stencilFront;
            return this;
        }

        public Builder setStencilBack(StencilConfig stencilBack) {
            this.stencilBack = stencilBack;
            return this;
        }

        // Getters
        public boolean isUsingDepthTest() {
            return this.useDepthTest;
        }

        public boolean isWritingDepth() {
            return this.useDepthWrite;
        }

        public boolean isUsingWireframe() {
            return this.useWireframe;
        }

        public int getCullMode() {
            return this.cullMode;
        }

        public ShaderStage getPushConstantStage() {
            return this.pushConstantStage;
        }

        public int getPushConstantSize() {
            if(this.pushConstantLayout == null)
                return 0;

            return Arrays.stream(this.pushConstantLayout)
                         .mapToInt(TypeHelper::size)
                         .sum();
        }

        public DescriptorSetLayout[] getDescriptorLayouts() {
            return this.descriptorLayouts;
        }

        public boolean isStencilConfigSpecified() {
            return this.stencilFront != null ||  this.stencilBack != null;
        }

        public boolean isUsingStencilTest() {
            return this.enableStencilTest;
        }

        public Optional<StencilConfig> getStencilFrontConfig() {
            return Optional.ofNullable(this.stencilFront);
        }

        public Optional<StencilConfig> getStencilBackConfig() {
            return Optional.ofNullable(this.stencilBack);
        }
    }

}
