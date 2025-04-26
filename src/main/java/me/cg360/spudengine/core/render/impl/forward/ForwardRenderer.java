package me.cg360.spudengine.core.render.impl.forward;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.context.RenderGoal;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.render.pipeline.shader.*;
import me.cg360.spudengine.core.render.pipeline.util.stencil.CompareOperation;
import me.cg360.spudengine.core.render.pipeline.util.stencil.StencilConfig;
import me.cg360.spudengine.core.render.pipeline.util.stencil.StencilOperation;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import me.cg360.spudengine.wormholes.render.pass.PortalLayerColourRenderPass;
import me.cg360.spudengine.wormholes.render.pass.PortalLayerColourStartRenderPass;
import me.cg360.spudengine.wormholes.render.pass.PortalLayerStencilRenderPass;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ForwardRenderer extends RenderProcess {

    private static final int DEPTH_ATTACHMENT_FORMAT = VK11.VK_FORMAT_D32_SFLOAT_S8_UINT;

    private final LogicalDevice device;

    private CommandBuffer[] commandBuffers;
    private Fence[] fences;
    private SwapChainRenderPass[] renderPasses;

    private SwapChain swapChain;
    private FrameBuffer[] frameBuffers;

    private PipelineCache pipelineCache;
    private Pipeline[] standardPipeline;
    private Pipeline[] wireframePipeline;
    private Pipeline[] stencilEnvironmentPipeline;
    private Pipeline[] stencilPortalPipeline;

    private ShaderProgram standardShaderProgram;
    private ShaderProgram stencilShaderProgram;

    private final Scene scene;

    private Attachment[] depthAttachments;

    private DescriptorPool descriptorPool;

    private DescriptorSetLayout lProjectionMatrix;
    private UniformDescriptorSet dProjectionMatrix;
    private GeneralBuffer uProjectionMatrix;

    private DescriptorSetLayout lViewMatrix;
    private UniformDescriptorSet[] dViewMatrix;
    private GeneralBuffer[] uViewMatrix;

    private StandardSamplers standardSamplers;

    private final InternalRenderContext renderContext;
    private final ShaderIO shaderIO; // #reset(...) whenever new draw call


    public ForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);

        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.device = swapChain.getDevice();

        this.scene = scene;

        int numImages = swapChain.getImageViews().length;
        this.createDepthImages();
        this.createRenderPasses(swapChain, this.depthAttachments[0].getImage().getFormat());
        this.createFrameBuffers();

        Logger.info("Loading Shader Program...");
        this.standardShaderProgram = this.buildStandardShaderProgram();
        //TODO: PORTAL REFERENCE REMOVAL.
        this.stencilShaderProgram = ShaderProgram.attemptCompile(this.device,
                new BinaryShaderFile(ShaderType.VERTEX, "shaders/vertex"),
                new BinaryShaderFile(ShaderType.GEOMETRY, "shaders/portal/colour/geometry"),
                new BinaryShaderFile(ShaderType.FRAGMENT, "shaders/portal/stencil_cutout/fragment")
        );
        Logger.info("Successfully loaded shader program!");

        this.shaderIO = new ShaderIO();
        this.renderContext = new InternalRenderContext();

        DescriptorSetLayout[] descriptorSetLayouts = this.initDescriptorSets();

        this.buildPipelines(descriptorSetLayouts);
        this.createCommandBuffers(commandPool, numImages);

        // Initialize uniforms which don't change frame-to-frame.
        this.setConstantUniforms();
    }

    @NotNull
    private ShaderProgram buildStandardShaderProgram() {
        return ShaderProgram.attemptCompile(
                this.device,
                EngineProperties.shaders
        );
    }

    protected void createRenderPasses(SwapChain swapChain, int depthImageFormat) {
        //TODO: Remove portal references
        int passLimit = PortalLayerColourRenderPass.MAX_PORTAL_DEPTH * 2;
        int colourPassesStart = PortalLayerColourRenderPass.MAX_PORTAL_DEPTH;
        this.renderPasses = new SwapChainRenderPass[passLimit];

        // Initial pass, clears all values
        this.renderPasses[0] = new SwapChainRenderPass(swapChain, depthImageFormat);

        // stenciling render passes.
        for(int pass = 1; pass < colourPassesStart; pass++) {
            this.renderPasses[pass] = new PortalLayerStencilRenderPass(swapChain, depthImageFormat);
        }

        // colour render passes
        this.renderPasses[colourPassesStart] = new PortalLayerColourStartRenderPass(swapChain, depthImageFormat);
        for(int pass = colourPassesStart + 1; pass < passLimit; pass++) {
            this.renderPasses[pass] = new PortalLayerColourRenderPass(swapChain, depthImageFormat);
        }
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

                // pass random render pass. They're all compatible.
                this.frameBuffers[i] = new FrameBuffer(this.device, swapChainExtent.width(), swapChainExtent.height(),
                                        pAttachments, this.renderPasses[0].getHandle());
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

    protected DescriptorSetLayoutBundle buildUniformLayout(DescriptorSetLayoutBundle bundle) {
        this.lProjectionMatrix = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT);
        this.lViewMatrix = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT)
                .enablePerFrameWrites(this.swapChain);

        bundle.addVertexUniforms(this.lProjectionMatrix, this.lViewMatrix);

        this.standardSamplers = new StandardSamplers(this.shaderIO, bundle);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.buildUniformLayout(bundle);

        return bundle;
    }

    protected void buildDescriptorSets() {
        this.dProjectionMatrix = UniformDescriptorSet.create(this.descriptorPool, this.lProjectionMatrix, DataTypes.MAT4X4F, 0)[0];
        this.uProjectionMatrix = this.dProjectionMatrix.getBuffer();

        this.dViewMatrix = UniformDescriptorSet.create(this.descriptorPool, this.lViewMatrix, DataTypes.MAT4X4F, 0);
        this.uViewMatrix = ShaderIO.collectUniformBuffers(this.dViewMatrix);

        this.standardSamplers.buildSets(this.descriptorPool);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.createDescriptorSets(this.descriptorPool);
    }

    protected void buildPipelines(DescriptorSetLayout[] descriptorSetLayouts) {
        Logger.debug("Building Pipelines...");

        Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION_UV.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantLayout(  // @see ForwardRenderer#applyPushConstants(...) for how this is filled.
                        DataTypes.MAT4X4F // transform
                )
                .setCullMode(VK11.VK_CULL_MODE_FRONT_BIT); // uhhhh, right. Sure. This works but

        this.standardPipeline = new Pipeline[this.renderPasses.length];
        this.wireframePipeline = new Pipeline[this.renderPasses.length];
        this.stencilEnvironmentPipeline = new Pipeline[this.renderPasses.length];
        this.stencilPortalPipeline = new Pipeline[this.renderPasses.length];

        //builder.setUsingStencilTest(true);
        for(int pass = 0; pass < this.renderPasses.length; pass++) {
            int val = pass % PortalLayerColourRenderPass.MAX_PORTAL_DEPTH;
            Logger.debug("Standard #{}: read {}", pass, val);

            this.standardPipeline[pass] = builder
                    .setUsingStencilTest(false)
                    .setUsingDepthTest(true)
                    .setUsingDepthWrite(true)
                    //.setStencilBack(maskComp)
                    .build(this.pipelineCache, "standard", this.renderPasses[pass].getHandle(), this.standardShaderProgram, 1);
        }

        builder.setUsingWireframe(true)
                .setUsingStencilTest(false);
        for(int pass = 0; pass < this.renderPasses.length; pass++)
            this.wireframePipeline[pass] = builder
                    .build(this.pipelineCache, "wireframe", this.renderPasses[pass].getHandle(), this.standardShaderProgram, 1);



        builder.setUsingWireframe(false)
                .setUsingStencilTest(true);
        for(int pass = 0; pass < this.renderPasses.length; pass++) {
            StencilConfig sSimpleWrite = new StencilConfig(pass, CompareOperation.ALWAYS)
                    .setAllWriteOps(StencilOperation.REPLACE)
                    .setDepthFailOp(StencilOperation.KEEP);

            Logger.info("Stencil-env #{}: write {}", pass, pass);

            this.stencilEnvironmentPipeline[pass] = builder
                    .setStencilBack(sSimpleWrite)
                    .build(this.pipelineCache, "stencil-environment", this.renderPasses[pass].getHandle(), this.stencilShaderProgram, 1);
        }


        builder.setUsingWireframe(false)
                .setUsingStencilTest(true);
        for(int pass = 0; pass < this.renderPasses.length; pass++) {
            StencilConfig sSimpleWrite = new StencilConfig(pass+1, CompareOperation.ALWAYS)
                    .setAllWriteOps(StencilOperation.REPLACE)
                    .setDepthFailOp(StencilOperation.KEEP);

            Logger.info("Stencil-portal #{}: write {}", pass, pass+1);

            this.stencilPortalPipeline[pass] = builder
                    .setStencilBack(sSimpleWrite)
                    .build(this.pipelineCache, "stencil-portal", this.renderPasses[pass].getHandle(), this.stencilShaderProgram, 1);
        }

        builder.cleanup();
        Logger.debug("Built pipelines!");
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
        this.renderContext.reset();

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

            VkCommandBuffer cmd = commandBuffer.beginRecording();
            VkClearValue.Buffer clearValues = generateClearValues(stack); // initial

            // todo: remove portal references
            for(int pass = 0; pass < PortalLayerColourRenderPass.MAX_PORTAL_DEPTH; pass++) {
                this.renderContext.setRenderGoal(RenderGoal.STENCIL_WRITING);

                VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                        .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                        .renderPass(this.renderPasses[pass].getHandle())
                        .pClearValues(clearValues)
                        .renderArea(a -> a.extent().set(width, height))
                        .framebuffer(frameBuffer.getHandle());

                Pipeline selectedPipeline = this.stencilEnvironmentPipeline[pass];

                // stencil pass.
                this.doRenderPass(cmd, renderPassBeginInfo, selectedPipeline, stack, pass, context -> {
                    this.setupView(cmd, stack, width, height);
                    this.shaderIO.reset(stack, selectedPipeline, this.descriptorPool);

                    Matrix4f view = this.scene.getMainCamera().getViewMatrix();
                    DataTypes.MAT4X4F.copyToBuffer(this.uViewMatrix[idx], view);

                    this.shaderIO.setUniform(this.lProjectionMatrix, this.dProjectionMatrix);
                    this.shaderIO.setUniform(this.lViewMatrix, this.dViewMatrix, idx);

                    for (SubRenderProcess process : this.subRenderProcesses)
                        process.renderPreMesh(this.shaderIO, this.standardSamplers, idx, context.getPass());

                    this.drawAllSceneModels(cmd, renderer, selectedPipeline, idx);

                    // redraw portal models.
                    //this.renderContext.setRenderGoal(RenderGoal.STENCIL_ADJUSTMENT);
                    //this.stencilPortalPipeline[context.getPass()].bind(cmd);
                    //this.drawAllSceneModels(cmd, renderer, selectedPipeline, idx);

                    this.shaderIO.free(); // free buffers from stack.
                });
            }

            // standard pass.
            this.renderContext.setRenderGoal(RenderGoal.STANDARD_DRAWING);
            for(int pass = PortalLayerColourRenderPass.MAX_PORTAL_DEPTH; pass < this.renderPasses.length; pass++) {
                int virtualPass = pass - PortalLayerColourRenderPass.MAX_PORTAL_DEPTH;

                VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                        .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                        .renderPass(this.renderPasses[pass].getHandle())
                        .pClearValues(clearValues)
                        .renderArea(a -> a.extent().set(width, height))
                        .framebuffer(frameBuffer.getHandle());

                Pipeline selectedPipeline = renderer.useWireframe
                        ? this.wireframePipeline[pass]
                        : this.standardPipeline[pass];

                this.doRenderPass(cmd, renderPassBeginInfo, selectedPipeline, stack, pass, context -> {
                    this.setupView(cmd, stack, width, height);
                    this.shaderIO.reset(stack, selectedPipeline, this.descriptorPool);

                    Matrix4f view = this.scene.getMainCamera().getViewMatrix();
                    DataTypes.MAT4X4F.copyToBuffer(this.uViewMatrix[idx], view);

                    this.shaderIO.setUniform(this.lProjectionMatrix, this.dProjectionMatrix);
                    this.shaderIO.setUniform(this.lViewMatrix, this.dViewMatrix, idx);

                    for (SubRenderProcess process : this.subRenderProcesses)
                        process.renderPreMesh(this.shaderIO, this.standardSamplers, idx, virtualPass);

                    this.drawAllSceneModels(cmd, renderer, selectedPipeline, idx);

                    this.shaderIO.free(); // free buffers from stack.
                });
            }

            commandBuffer.endRecording();
        }
    }

    private void setupView(VkCommandBuffer cmd, MemoryStack stack, int width, int height) {
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

    protected void drawAllSceneModels(VkCommandBuffer cmd, Renderer renderer, Pipeline selectedPipeline, int frameIndex) {
        for (String modelId: this.scene.getUsedModelIds()) {
            this.drawModel(cmd, renderer, selectedPipeline, modelId, frameIndex);
        }
    }

    protected void drawModel(VkCommandBuffer cmd, Renderer renderer, Pipeline selectedPipeline, String modelId, int frameIndex) {
        BufferedModel model = renderer.getModelManager().getModel(modelId);
        List<StaticModelEntity> entities = this.scene.getEntitiesWithModel(modelId);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.renderModel(this.shaderIO, this.standardSamplers, model, frameIndex);

        for(BundledMaterial material: model.getMaterials()) {
            if (material.meshes().isEmpty())
                continue;

            this.standardSamplers.setMaterial(material);

            for(BufferedMesh mesh: material.meshes()) {
                this.shaderIO.bindMesh(cmd, mesh);

                for(RenderedEntity entity: entities) {
                    if(!entity.shouldDraw(this.renderContext)) continue;

                    long layoutHandle = selectedPipeline.getPipelineLayoutHandle();
                    this.shaderIO.bindDescriptorSets(cmd, layoutHandle);
                    this.shaderIO.applyVertexPushConstants(cmd, layoutHandle, entity.getTransform());

                    VK11.vkCmdDrawIndexed(cmd, mesh.numIndices(), 1, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void submit(CommandQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = this.swapChain.getCurrentFrame();

            CommandBuffer commandBuffer = this.commandBuffers[idx];
            Fence currentFence = this.fences[idx];
            currentFence.reset();

            ForwardSemaphores forwardSemaphores = this.swapChain.getSyncSemaphores()[idx];

            queue.submit(stack.pointers(commandBuffer.asVk()),
                         stack.longs(forwardSemaphores.imgAcquisitionSemaphore().getHandle()),
                         stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                         stack.longs(forwardSemaphores.renderCompleteSemaphore().getHandle()),
                         currentFence);
        }
    }

    protected void startRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, MemoryStack stack) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
    }

    protected void doRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, MemoryStack stack, int pass, Consumer<RenderContext> passActions) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
        this.renderContext.setPass(pass);
        this.renderContext.setCurrentPipeline(pipeline);
        passActions.accept(this.renderContext);
        VK11.vkCmdEndRenderPass(cmd);
    }

    protected void nextSubPass(VkCommandBuffer cmd, Pipeline pipeline, MemoryStack stack) {
        VK11.vkCmdNextSubpass(cmd, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
    }

    @Override
    public void processModelBatch(List<BufferedModel> models) {
        this.device.waitIdle();
        Logger.debug("Processing {} models", models.size());

        for (BufferedModel vulkanModel : models) {
            for (BundledMaterial vulkanMaterial : vulkanModel.getMaterials()) {
                if (vulkanMaterial.meshes().isEmpty())
                    continue;

                this.standardSamplers.registerTexture(vulkanMaterial.texture());
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
            this.standardSamplers.registerOverlay(texture);
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
        this.standardSamplers.cleanup();

        this.descriptorPool.cleanup(); // descriptor sets cleaned up here.

        VulkanUtil.cleanupAll(this.standardPipeline);
        VulkanUtil.cleanupAll(this.wireframePipeline);
        VulkanUtil.cleanupAll(this.stencilEnvironmentPipeline);
        VulkanUtil.cleanupAll(this.stencilPortalPipeline);

        this.standardShaderProgram.cleanup();
        this.stencilShaderProgram.cleanup();

        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        VulkanUtil.cleanupAll(this.renderPasses);

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

    @Override
    public RenderContext getCurrentContext() {
        return this.renderContext;
    }

    public static class InternalRenderContext extends RenderContext {

        protected void reset() {
            this.pass = -1;
            this.renderGoal = RenderGoal.NONE;
            this.currentPipeline = null;
        }

        protected void setPass(int pass) {
            this.pass = pass;
        }

        protected void setRenderGoal(RenderGoal renderGoal) {
            this.renderGoal = renderGoal;
        }

        protected void setCurrentPipeline(Pipeline currentPipeline) {
            this.currentPipeline = currentPipeline;
        }
    }

}
