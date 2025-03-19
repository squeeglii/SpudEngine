package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.geometry.model.Material;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureSampler;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.SamplerDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.SamplerDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.util.BlendFunc;
import me.cg360.spudengine.core.render.pipeline.util.stencil.CompareOperation;
import me.cg360.spudengine.core.render.pipeline.util.stencil.StencilConfig;
import me.cg360.spudengine.core.render.pipeline.util.stencil.StencilOperation;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.render.sync.SyncSemaphores;
import me.cg360.spudengine.core.exception.EngineLimitExceededException;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import me.cg360.spudengine.wormholes.GeneratedAssets;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;

public class ForwardRenderer extends RenderProcess {

    private static final int DEPTH_ATTACHMENT_FORMAT = VK11.VK_FORMAT_D32_SFLOAT_S8_UINT;

    private static final int PORTAL_DEPTH = 4;

    private final LogicalDevice device;

    private CommandBuffer[] commandBuffers;
    private Fence[] fences;
    private SwapChainRenderPass renderPass;

    private SwapChain swapChain;
    private FrameBuffer[] frameBuffers;

    private PipelineCache pipelineCache;
    private Pipeline standardPipeline;
    private Pipeline wireframePipeline;
    private Pipeline portalPipeline;
    private Pipeline roomGeometryPipeline;

    private ShaderProgram shaderProgram;

    private final Scene scene;

    private Attachment[] depthAttachments;

    private DescriptorPool descriptorPool;

    private DescriptorSetLayout lProjectionMatrix;
    private UniformDescriptorSet dProjectionMatrix;
    private GeneralBuffer uProjectionMatrix;

    private DescriptorSetLayout lViewMatrix;
    private UniformDescriptorSet[] dViewMatrix;
    private GeneralBuffer[] uViewMatrix;

    private DescriptorSetLayout lSampler;
    private Map<String, SamplerDescriptorSet> dSampler;
    private TextureSampler uSampler;

    private DescriptorSetLayout lOverlaySampler;
    private Map<String, SamplerDescriptorSet> dOverlaySampler;
    private TextureSampler uOverlaySampler;

    private final ShaderIO shaderIO; // #reset(...) whenever new draw call


    public ForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);

        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.device = swapChain.getDevice();

        this.scene = scene;

        int numImages = swapChain.getImageViews().length;
        this.createDepthImages();
        this.renderPass = new SwapChainRenderPass(swapChain, this.depthAttachments[0].getImage().getFormat());
        this.createFrameBuffers();

        Logger.info("Loading Shader Program...");
        this.shaderProgram = this.buildShaderProgram();
        this.shaderIO = new ShaderIO();
        Logger.info("Successfully loaded shader program!");

        DescriptorSetLayout[] descriptorSetLayouts = this.initDescriptorSets();

        this.buildPipelines(descriptorSetLayouts);
        this.createCommandBuffers(commandPool, numImages);

        // Initialize uniforms which don't change frame-to-frame.
        this.setConstantUniforms();
    }

    @NotNull
    private ShaderProgram buildShaderProgram() {
        return ShaderProgram.attemptCompile(
                this.device,
                EngineProperties.shaders
        );
    }

    protected void createCommandBuffers(CommandPool commandPool, int numImages) {
        this.commandBuffers = new CommandBuffer[numImages];
        this.fences = new Fence[numImages];
        for (int i = 0; i < numImages; i++) {
            this.commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            this.fences[i] = new Fence(device, true);
        }
    }

    protected void createDepthImages() {
        int numImages = this.swapChain.getImageViews().length;
        VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();

        this.depthAttachments = new Attachment[numImages];
        for (int i = 0; i < numImages; i++) {
            this.depthAttachments[i] = new Attachment(this.device, swapChainExtent.width(), swapChainExtent.height(),
                    DEPTH_ATTACHMENT_FORMAT, VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }
    }

    protected void createFrameBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();
            ImageView[] imageViews = this.swapChain.getImageViews();
            int numImages = imageViews.length;

            LongBuffer pAttachments = stack.mallocLong(2);
            this.frameBuffers = new FrameBuffer[numImages];

            for (int i = 0; i < numImages; i++) {
                ImageView depthView = this.depthAttachments[i].getImageView();
                pAttachments.put(0, imageViews[i].getHandle());
                pAttachments.put(1, depthView.getHandle());

                this.frameBuffers[i] = new FrameBuffer(this.device, swapChainExtent.width(), swapChainExtent.height(),
                                        pAttachments, this.renderPass.getHandle());
            }
        }
    }

    protected final DescriptorSetLayout[] initDescriptorSets() {
        Logger.info("Initializing Descriptor Sets...");

        DescriptorSetLayoutBundle bundle = new DescriptorSetLayoutBundle(this.device, this.swapChain);
        DescriptorSetLayout[] layout = this.buildUniformLayout(bundle).merge();
        Logger.info("Collected {} uniform(s) from {}", layout.length, bundle);

        this.descriptorPool = new DescriptorPool(this.device, layout);
        this.buildDescriptorSets();

        Logger.info("Initialized Descriptor Sets!");
        return layout;
    }

    // Extend definitions here.
    protected void buildDescriptorSets() {
        this.dProjectionMatrix = UniformDescriptorSet.create(this.descriptorPool, this.lProjectionMatrix, DataTypes.MAT4X4F, 0)[0];
        this.uProjectionMatrix = this.dProjectionMatrix.getBuffer();

        this.dViewMatrix = UniformDescriptorSet.create(this.descriptorPool, this.lViewMatrix, DataTypes.MAT4X4F, 0);
        this.uViewMatrix = ShaderIO.collectUniformBuffers(this.dViewMatrix);

        this.dSampler = new HashMap<>();
        this.uSampler = new TextureSampler(this.device, 1, true);

        this.dOverlaySampler = new HashMap<>();
        this.uOverlaySampler = new TextureSampler(this.device, 1, true, VK11.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.createDescriptorSets(this.descriptorPool);
    }

    protected DescriptorSetLayoutBundle buildUniformLayout(DescriptorSetLayoutBundle bundle) {
        this.lProjectionMatrix = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT);
        this.lViewMatrix = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT)
                .enablePerFrameWrites(this.swapChain);
        this.lSampler = new SamplerDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(EngineProperties.MAX_TEXTURES);
        this.lOverlaySampler = new SamplerDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(EngineProperties.MAX_OVERLAY_TEXTURES);

        bundle.addVertexUniforms(this.lProjectionMatrix, this.lViewMatrix);
        bundle.addFragmentUniforms(this.lSampler);
        bundle.addFragmentUniforms(this.lOverlaySampler);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.buildUniformLayout(bundle);

        return bundle;
    }

    protected void buildPipelines(DescriptorSetLayout[] descriptorSetLayouts) {
        Logger.debug("Building Pipelines...");

        Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION_UV.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantLayout(  // @see ForwardRenderer#applyPushConstants(...) for how this is filled.
                        //DataTypes.MAT4X4F, // proj
                        DataTypes.MAT4X4F // transform
                )
                .setCullMode(VK11.VK_CULL_MODE_FRONT_BIT); // uhhhh, right. Sure. This works but

        this.standardPipeline = builder.build(this.pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);
        this.wireframePipeline = builder.setUsingWireframe(true)
                                        .build(this.pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);

        // --------------------------
        //TODO: These pipelines below are very much for portal. Move these to a seperate renderer.
        builder.setUsingWireframe(false);

        StencilConfig stencilWrite = new StencilConfig(1, CompareOperation.ALWAYS);
        stencilWrite.setAllWriteOps(StencilOperation.REPLACE);

        StencilConfig stencilRead = new StencilConfig(0, CompareOperation.EQUAL);
        stencilRead.setPassOp(StencilOperation.KEEP);
        stencilRead.setDepthFailOp(StencilOperation.KEEP);
        stencilRead.setStencilFailOp(StencilOperation.KEEP);
        stencilRead.setWriteMask(0x00);

        this.portalPipeline = builder.resetDepth().resetStencil()
                .disableColourWrite().enableAlphaWrite() // could we do some magic blending
                .setUsingDepthTest(false)
                .setUsingDepthWrite(false)
                .setUsingBlend(false)
                //.setColourBlendOp(VK11.VK_BLEND_OP_ADD)
                //.setBlendFunc(BlendFunc.REPLACE)
                //.setUsingStencilTest(true)
                //.setStencilBack(stencilWrite)
                .build(this.pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);

        this.roomGeometryPipeline = builder.resetDepth().resetStencil()
                .enableColourWrite()
                .setUsingDepthWrite(true)
                .setUsingDepthTest(true)
                .setUsingBlend(false)
                .setColourBlendOp(VK11.VK_BLEND_OP_ADD)
                .setBlendFunc(BlendFunc.DEFAULT)
                //.setUsingStencilTest(true)
                //.setStencilBack(stencilRead)
                .build(this.pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);

        builder.cleanup();
        Logger.debug("Built pipelines!");
    }

    public void addTextureToShader(Texture texture) {
        String textureFileName = texture.getResourceName();
        SamplerDescriptorSet samplerDescriptorSet = this.dSampler.get(textureFileName);

        if (samplerDescriptorSet == null) {
            if(this.dSampler.size() >= EngineProperties.MAX_TEXTURES)
                throw new EngineLimitExceededException("MAX_TEXTURES", EngineProperties.MAX_TEXTURES, this.dSampler.size()+1);

            samplerDescriptorSet = new SamplerDescriptorSet(this.descriptorPool, this.lSampler, 0, texture, this.uSampler);
            this.dSampler.put(textureFileName, samplerDescriptorSet);
        }
    }

    public void addOverlayToShader(Texture texture) {
        String textureFileName = texture.getResourceName();
        SamplerDescriptorSet overlaySamplerDescriptorSet = this.dOverlaySampler.get(textureFileName);

        if (overlaySamplerDescriptorSet == null) {
            if(this.dOverlaySampler.size() >= EngineProperties.MAX_OVERLAY_TEXTURES)
                throw new EngineLimitExceededException("MAX_OVERLAY_TEXTURES", EngineProperties.MAX_OVERLAY_TEXTURES, this.dOverlaySampler.size()+1);

            overlaySamplerDescriptorSet = new SamplerDescriptorSet(this.descriptorPool, this.lOverlaySampler, 0, texture, this.uOverlaySampler);
            this.dOverlaySampler.put(textureFileName, overlaySamplerDescriptorSet);
        }
    }

    protected void setConstantUniforms() {
        DataTypes.MAT4X4F.copyToBuffer(this.uProjectionMatrix, this.scene.getProjection().asMatrix());
    }

    @Override
    public void waitTillFree() {
        int idx = this.swapChain.getCurrentFrame();
        Fence currentFence = this.fences[idx];
        currentFence.fenceWait();
    }

    @Override
    public void recordDraw(Renderer renderer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = this.swapChain.getCurrentFrame();

            //Fence fence = this.fences[idx];
            CommandBuffer commandBuffer = this.commandBuffers[idx];
            FrameBuffer frameBuffer = this.frameBuffers[idx];

            //fence.fenceWait();
            //fence.reset();
            commandBuffer.reset();

            VkClearValue.Buffer clearValues = generateClearValues(stack);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.renderPass.getHandle())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getHandle());

            VkCommandBuffer cmd = commandBuffer.beginRecording();
            VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE); // ----

            Pipeline selectedPipeline = renderer.useWireframe
                    ? this.wireframePipeline.bind(cmd)
                    : this.standardPipeline.bind(cmd);

            // Setup view
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0)
                    .y(height)
                    .height(-height)         // flip viewport - opengl's coordinate system is nicer.
                    .width(width)
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            VK11.vkCmdSetViewport(cmd, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .extent(it -> it
                            .width(width)
                            .height(height))
                    .offset(it -> it
                            .x(0)
                            .y(0));
            VK11.vkCmdSetScissor(cmd, 0, scissor);

            // Populate Uniforms
            this.shaderIO.reset(stack, selectedPipeline, this.descriptorPool);

            Matrix4f view = this.scene.getMainCamera().getViewMatrix();
            DataTypes.MAT4X4F.copyToBuffer(this.uViewMatrix[idx], view);

            this.shaderIO.setUniform(this.lProjectionMatrix, this.dProjectionMatrix);
            this.shaderIO.setUniform(this.lViewMatrix, this.dViewMatrix, idx);

            for(SubRenderProcess process:  this.subRenderProcesses)
                process.renderPreMesh(this.shaderIO, idx);

            // Render the models!
            // TODO: Remove these portal-code references.

            this.setOverlayMaterial(GeneratedAssets.PORTAL_CUTOUT);

            //selectedPipeline = this.portalPipeline.bind(cmd);
            //this.drawModel(cmd, renderer, selectedPipeline, GeneratedAssets.BLUE_PORTAL_MODEL.getId(), idx);
            //this.drawModel(cmd, renderer, selectedPipeline, GeneratedAssets.ORANGE_PORTAL_MODEL.getId(), idx);

            //selectedPipeline = this.roomGeometryPipeline.bind(cmd);
            this.drawNonPortalModels(cmd, renderer, selectedPipeline, idx);

            //this.drawMeshes(cmd, renderer, selectedPipeline, idx);

            this.shaderIO.free(); // free buffers from stack.
            VK11.vkCmdEndRenderPass(cmd); // ------------------------------------------------------
            commandBuffer.endRecording();
        }
    }

    private static VkClearValue.Buffer generateClearValues(MemoryStack stack) {
        VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
        Color c = EngineProperties.CLEAR_COLOUR;
        float[] components = new float[4];
        c.getComponents(components);
        clearValues.apply(0, v -> v.color()
                .float32(0, components[0])
                .float32(1, components[1])
                .float32(2, components[2])
                .float32(3, components[3])
        );
        clearValues.apply(1, v -> v.depthStencil()
                .depth(1.0f)
                .stencil(0)
        );
        return clearValues;
    }

    protected void drawMeshes(VkCommandBuffer cmd, Renderer renderer, Pipeline selectedPipeline, int frameIndex) {
        for (String modelId: this.scene.getUsedModelIds()) {
            this.drawModel(cmd, renderer, selectedPipeline, modelId, frameIndex);
        }
    }

    protected void drawNonPortalModels(VkCommandBuffer cmd, Renderer renderer, Pipeline selectedPipeline, int frameIndex) {
        for (String modelId: this.scene.getUsedModelIds()) {
            if(modelId.equalsIgnoreCase(GeneratedAssets.BLUE_PORTAL_MODEL.getId())) continue;
            if(modelId.equalsIgnoreCase(GeneratedAssets.ORANGE_PORTAL_MODEL.getId())) continue;

            this.drawModel(cmd, renderer, selectedPipeline, modelId, frameIndex);
        }
    }

    protected void drawModel(VkCommandBuffer cmd, Renderer renderer, Pipeline selectedPipeline, String modelId, int frameIndex) {
        BufferedModel model = renderer.getModelManager().getModel(modelId);
        List<StaticModelEntity> entities = this.scene.getEntitiesWithModel(modelId);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.renderModel(this.shaderIO, model, frameIndex);

        for(BundledMaterial material: model.getMaterials()) {
            if (material.meshes().isEmpty())
                continue;

            this.setMaterial(material);

            for(BufferedMesh mesh: material.meshes()) {
                this.shaderIO.bindMesh(cmd, mesh);

                for(RenderedEntity entity: entities) {
                    long layoutHandle = selectedPipeline.getPipelineLayoutHandle();
                    this.shaderIO.bindDescriptorSets(cmd, layoutHandle);
                    this.shaderIO.applyVertexPushConstants(cmd, layoutHandle, entity.getTransform());

                    VK11.vkCmdDrawIndexed(cmd, mesh.numIndices(), 1, 0, 0, 0);
                }
            }
        }
    }

    protected void setMaterial(BundledMaterial material) {
        SamplerDescriptorSet samplerDescriptorSet = this.dSampler.get(material.texture().getResourceName());
        this.shaderIO.setUniform(this.lSampler, samplerDescriptorSet);
    }

    // todo: get some checks on this - make sure there's always a texture available.
    protected void setOverlayMaterial(Material material) {
        SamplerDescriptorSet samplerDescriptorSet = this.dOverlaySampler.get(material.texture());
        this.shaderIO.setUniform(this.lOverlaySampler, samplerDescriptorSet);
    }

    @Override
    public void submit(CommandQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = this.swapChain.getCurrentFrame();

            CommandBuffer commandBuffer = this.commandBuffers[idx];
            Fence currentFence = this.fences[idx];
            currentFence.reset();

            SyncSemaphores syncSemaphores = this.swapChain.getSyncSemaphores()[idx];

            queue.submit(stack.pointers(commandBuffer.asVk()),
                         stack.longs(syncSemaphores.imgAcquisitionSemaphore().getHandle()),
                         stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                         stack.longs(syncSemaphores.renderCompleteSemaphore().getHandle()),
                         currentFence);
        }
    }

    @Override
    public void processModelBatch(List<BufferedModel> models) {
        this.device.waitIdle();
        Logger.debug("Processing {} models", models.size());

        for (BufferedModel vulkanModel : models) {
            for (BundledMaterial vulkanMaterial : vulkanModel.getMaterials()) {
                if (vulkanMaterial.meshes().isEmpty())
                    continue;

                this.addTextureToShader(vulkanMaterial.texture());
            }
        }
    }

    @Override
    public void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures) {
        this.device.waitIdle();
        Logger.debug("Processing {} overlay textures", overlayTextures.size());

        CommandBuffer cmd = new CommandBuffer(uploadPool, true, true);

        cmd.record(() -> {
            for(Texture texture: overlayTextures) {
                texture.upload(cmd);
            }
        }).submitAndWait(queue);

        // all textures transformed, use!
        for(Texture texture : overlayTextures) {
            this.addOverlayToShader(texture);
        }
    }


    @Override
    public void onResize(SwapChain newSwapChain) {
        DataTypes.MAT4X4F.copyToBuffer(this.uProjectionMatrix, this.scene.getProjection().asMatrix());

        this.swapChain = newSwapChain;
        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        this.createDepthImages();
        this.createFrameBuffers();
    }

    @Override
    public void cleanup() {
        for(SubRenderProcess process: this.subRenderProcesses)
            process.cleanup();

        this.uProjectionMatrix.cleanup();
        Arrays.stream(this.uViewMatrix).forEach(GeneralBuffer::cleanup);
        this.uSampler.cleanup();
        this.uOverlaySampler.cleanup();

        this.descriptorPool.cleanup(); // descriptor sets cleaned up here.

        this.standardPipeline.cleanup();
        this.wireframePipeline.cleanup();
        this.roomGeometryPipeline.cleanup();
        this.portalPipeline.cleanup();

        this.shaderProgram.cleanup();

        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        this.renderPass.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

    @Override
    public Attachment getDepthAttachment(int index) {
        return this.depthAttachments[index];
    }

    @Override
    public int getDepthFormat() {
        return DEPTH_ATTACHMENT_FORMAT;
    }

}
