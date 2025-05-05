package me.cg360.spudengine.core.render.impl.layered.stage;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.context.RenderGoal;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.*;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.impl.layered.LayeredSemaphores;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.*;
import me.cg360.spudengine.core.render.pipeline.util.BlendFunc;
import me.cg360.spudengine.core.render.pipeline.util.ShaderStage;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ComposeRenderer extends RenderProcess {

    protected LogicalDevice device;

    protected SwapChain swapChain;
    protected ComposeFrameBuffer frameBuffer;
    protected RenderTargetAttachmentSet renderTargetAttachments;

    protected CommandBuffer[] commandBuffers;
    protected Fence[] fences;

    protected ShaderProgram shaderProgram;

    protected DescriptorPool descriptorPool;
    protected RenderTargetSamplers renderTargetSamplers;

    protected PipelineCache pipelineCache;
    protected Pipeline standardPipeline;

    protected final ShaderIO shaderIO; // #reset(...) whenever new draw call
    private final InternalRenderContext renderContext;


    public ComposeRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, RenderTargetAttachmentSet renderTargets, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);

        this.device = swapChain.getDevice();
        this.swapChain = swapChain;

        this.renderTargetAttachments = renderTargets;

        this.pipelineCache = pipelineCache;

        // todo: create frame buffer here.
        this.frameBuffer = new ComposeFrameBuffer(this.swapChain);

        Logger.info("Loading Shader Program...");
        this.shaderProgram = ShaderProgram.attemptCompile(this.device,
                new BinaryShaderFile(ShaderType.VERTEX, "shaders/layered/vertex_compose"),
                new BinaryShaderFile(ShaderType.FRAGMENT, "shaders/layered/fragment_compose")
        );
        Logger.info("Successfully loaded shader program!");

        this.shaderIO = new ShaderIO();

        DescriptorSetLayout[] descriptorSetLayouts = this.initDescriptorSets();

        this.buildPipeline(descriptorSetLayouts);

        int numImages = swapChain.getImageViews().length;
        this.createCommandBuffers(commandPool, numImages);

        this.renderContext = new InternalRenderContext();

        for (int i = 0; i < numImages; i++)
            this.preRecordDraw(i);
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
        this.renderTargetSamplers = new RenderTargetSamplers(this.shaderIO, bundle, this.renderTargetAttachments.getRenderTargetCount());

        for(SubRenderProcess process: this.subRenderProcesses)
            process.buildUniformLayout(bundle);

        return bundle;
    }

    protected void buildDescriptorSets() {
        this.renderTargetSamplers.buildSets(this.descriptorPool);
        this.reprocessRenderTargets(this.renderTargetAttachments);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.createDescriptorSets(this.descriptorPool);
    }

    protected void buildPipeline(DescriptorSetLayout[] descriptorSetLayouts) {
        Pipeline.Builder builder = Pipeline.builder(VertexFormats.EMPTY.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantStage(ShaderStage.NONE)
                .setCullMode(VK11.VK_CULL_MODE_NONE) // todo: if issues, check this.
                .setUsingDepthWrite(true)
                .setUsingDepthTest(false)
                .setUsingBlend(true)
                .setBlendFunc(BlendFunc.DEFAULT)
                .setColourBlendOp(VK11.VK_BLEND_OP_ADD);
        // ^^^ always overwrite depth. Lets you reference the depth buffer later, but ignores ordering issues.

        long renderPassHandle = this.frameBuffer.getRenderPass().getHandle();
        this.standardPipeline = builder.build(this.pipelineCache, "standard-compose", renderPassHandle, 0, this.shaderProgram, 1);
    }

    protected void createCommandBuffers(CommandPool commandPool, int numImages) {
        this.commandBuffers = new CommandBuffer[numImages];
        this.fences = new Fence[numImages];

        for (int i = 0; i < numImages; i++) {
            this.commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            this.fences[i] = new Fence(device, true);
        }
    }

    public void preRecordDraw(int imageIdx) {
        Logger.info("Pre-recording draw for compositor");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();

            CommandBuffer commandBuffer = this.commandBuffers[imageIdx];
            FrameBuffer frameBuffer = this.frameBuffer.getBuffers()[imageIdx];

            commandBuffer.reset();

            VkClearValue.Buffer clearValues = VulkanUtil.generateClearValues(stack);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.frameBuffer.getRenderPass().getHandle())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getHandle());

            VkCommandBuffer cmd = commandBuffer.beginRecording();
            Pipeline selectedPipeline = this.standardPipeline;

            this.doRenderPass(cmd, renderPassBeginInfo, selectedPipeline, imageIdx, stack, context -> {
                // Setup view
                VulkanUtil.setupStandardViewport(cmd, stack, width, height);

                // Populate Uniforms
                this.shaderIO.reset(stack, selectedPipeline, this.descriptorPool);

                for(SubRenderProcess process:  this.subRenderProcesses)
                    process.renderPreMesh(this.renderContext, this.shaderIO, null, cmd);

                for(SubRenderProcess process: this.subRenderProcesses)
                    process.renderModel(this.renderContext, this.shaderIO, null, null);

                // For each render target, draw a rectangle.
                int renderTargets = this.renderTargetAttachments.getRenderTargetCount();
                for(int i = 0; i < renderTargets; i++) {
                    this.renderTargetSamplers.selectRenderTarget(i);

                    long layoutHandle = selectedPipeline.getPipelineLayoutHandle();
                    this.shaderIO.bindDescriptorSets(cmd, layoutHandle);
                    VK11.vkCmdDraw(cmd, 3, 1, 0, 0);
                }

                this.shaderIO.free(); // free buffers from stack.
            });

            commandBuffer.endRecording();
        }

        Logger.info("Finished pre-recording draw for compositor");
    }

    @Override
    public void draw(RenderSystem renderSystem) {
        int idx = this.swapChain.getCurrentFrame();
        Fence fence = this.fences[idx];
        fence.fenceWait();
        fence.reset();
    }

    @Override
    public void submit(CommandQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = this.swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = this.commandBuffers[idx];
            Fence currentFence = this.fences[idx];
            LayeredSemaphores syncSemaphores = this.swapChain.getSyncSemaphores()[idx];

            queue.submit(stack.pointers(commandBuffer.asVk()),
                    stack.longs(syncSemaphores.layersCompleteSemaphore().getHandle()),
                    stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.composeCompleteSemaphore().getHandle()),
                    currentFence);
        }
    }

    @Override
    public void waitTillFree() {
        int idx = this.swapChain.getCurrentFrame();
        Fence currentFence = this.fences[idx];
        currentFence.fenceWait();
    }

    @Override
    public void onResize(SwapChain newSwapChain) {
        this.swapChain = newSwapChain;
        this.frameBuffer.onResize(this.swapChain);

        int numImages = this.swapChain.getImageViews().length;
        for (int i = 0; i < numImages; i++)
            this.preRecordDraw(i);
    }

    public void onResize(SwapChain newSwapChain, RenderTargetAttachmentSet newRenderTargets) {
        //TODO: Make something cleaner that forces a reset of render targets.
        // Currently, this onResize is "optional", but that can lead to dirty
        this.renderTargetAttachments = newRenderTargets;
        this.renderTargetSamplers.freeRenderTargets();
        this.reprocessRenderTargets(newRenderTargets);
        this.onResize(newSwapChain);
    }

    public void reprocessRenderTargets(RenderTargetAttachmentSet attachmentSet) {
        Logger.info("Reprocessing render targets");
        for(int i = 0; i < attachmentSet.getRenderTargetCount(); i++) {
            Attachment colour = attachmentSet.getColourAttachment(i);
            Attachment depth = attachmentSet.getDepthAttachment(i);

            this.renderTargetSamplers.registerRenderTarget(this.device, colour, depth, i);
        }
        Logger.info("Reprocessed render targets");
    }

    protected void doRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, int frameIndex, MemoryStack stack, Consumer<RenderContext> passActions) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
        this.renderContext.setRenderGoal(RenderGoal.COMPOSING);
        this.renderContext.setPass(0);
        this.renderContext.setCurrentPipeline(pipeline);
        this.renderContext.setFrameIndex(frameIndex);
        passActions.accept(this.renderContext);
        VK11.vkCmdEndRenderPass(cmd);
    }

    @Override
    public void processModelBatch(List<BufferedModel> models) {
        Logger.warn("Model batch sent to Compose Renderer. Ignored.");
    }

    @Override
    public void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures) {
        Logger.warn("Overlay batch sent to Compose Renderer. Ignored.");
    }

    @Override
    public void cleanup() {
        for(SubRenderProcess process: this.subRenderProcesses)
            process.cleanup();

        this.renderTargetSamplers.cleanup();

        this.descriptorPool.cleanup(); // descriptor sets cleaned up here.

        this.standardPipeline.cleanup();

        this.shaderProgram.cleanup();

        this.frameBuffer.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

    @Override
    public Attachment getDepthAttachment(int index) {
        return this.frameBuffer.getDepthAttachments()[index];
    }

    @Override
    public int getDepthFormat() {
        return AbstractRenderer.DEPTH_ATTACHMENT_FORMAT;
    }

    @Override
    public RenderContext getCurrentContext() {
        return this.renderContext;
    }

    private static class InternalRenderContext extends RenderContext {

        public void reset() {
            super.reset();
        }

        public void setPass(int pass) {
            this.pass = pass;
        }

        public void setFrameIndex(int frameIndex) {
            this.frameIndex = frameIndex;
        }

        public void setRenderGoal(RenderGoal renderGoal) {
            this.renderGoal = renderGoal;
        }

        public void setCurrentPipeline(Pipeline currentPipeline) {
            this.currentPipeline = currentPipeline;
        }

    }
}
