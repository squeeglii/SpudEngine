package me.cg360.spudengine.core.render.impl.layered.stage;

import me.cg360.spudengine.core.exception.UnimplementedException;
import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.context.RenderGoal;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.image.*;
import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.impl.layered.LayeredSemaphores;
import me.cg360.spudengine.core.render.image.LayeredFrameBuffer;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.util.ShaderStage;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import me.cg360.spudengine.core.world.Scene;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.util.Arrays;

/** Render each layer to  */
public class LayerRenderer extends AbstractRenderer {

    protected CommandBuffer[] commandBuffers;
    protected Fence[] fences;

    protected LayeredFrameBuffer frameBuffer;

    protected ShaderProgram shaderProgram;

    protected PipelineCache pipelineCache;
    protected Pipeline[] standardPipeline; // split by subpass.

    private final InternalRenderContext renderContext;

    public LayerRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, int renderTargetCount, SubRenderProcess[] subRenderProcesses) {
        super(swapChain, scene, subRenderProcesses);

        this.pipelineCache = pipelineCache;

        int numImages = swapChain.getImageViews().length;
        this.createFrameBuffer(renderTargetCount);

        Logger.info("Loading Shader Program...");
        this.shaderProgram = this.buildShaderProgram();
        Logger.info("Successfully loaded shader program!");

        DescriptorSetLayout[] descriptorSetLayouts = this.initDescriptorSets();

        this.buildPipelines(descriptorSetLayouts);
        this.createCommandBuffers(commandPool, numImages);

        // Initialize uniforms which don't change frame-to-frame.
        this.setConstantUniforms();

        this.renderContext = new InternalRenderContext();
    }

    protected void createFrameBuffer(int renderTargetCount) {
        this.frameBuffer = new LayeredFrameBuffer(this.swapChain, renderTargetCount);
    }

    protected void createCommandBuffers(CommandPool commandPool, int numImages) {
        this.commandBuffers = new CommandBuffer[numImages];
        this.fences = new Fence[numImages];

        for (int i = 0; i < numImages; i++) {
            this.commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            this.fences[i] = new Fence(device, true);
        }
    }

    @Override
    protected void buildPipelines(DescriptorSetLayout[] descriptorSetLayouts) {
        Logger.debug("Building Pipelines...");

        Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION_UV.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantStage(ShaderStage.GEOMETRY)
                .setPushConstantLayout(
                        DataTypes.MAT4X4F // transform
                )
                .setCullMode(VK11.VK_CULL_MODE_FRONT_BIT); // uhhhh, right. Sure. This works but

        long renderPass = this.frameBuffer.getRenderPass().getHandle();
        int renderTargetCount = this.frameBuffer.getRenderTargetCount();
        this.standardPipeline = new Pipeline[renderTargetCount];

        for(int i = 0; i < renderTargetCount; i++)
            this.standardPipeline[i] = builder.build(pipelineCache, "standard", renderPass, i, this.shaderProgram, 1);
    }

    @Override
    public void draw(RenderSystem renderSystem) {


        //TODO: Make sure clear colour is clear.
        throw new UnimplementedException("Layer Renderer (LayeredRenderer sub-renderer is not implemented yet.");
    }

    @Override
    public void submit(CommandQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            currentFence.reset();
            LayeredSemaphores syncSemaphores = swapChain.getSyncSemaphores()[idx];

            queue.submit(stack.pointers(commandBuffer.asVk()),
                    stack.longs(syncSemaphores.imageAcquisitionSemaphore().getHandle()),
                    stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.layersCompleteSemaphore().getHandle()), currentFence);
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
        DataTypes.MAT4X4F.copyToBuffer(this.uProjectionMatrix, this.scene.getProjection().asMatrix());
        this.swapChain = newSwapChain;
        this.frameBuffer.onResize(this.swapChain);
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

        this.shaderProgram.cleanup();

        this.frameBuffer.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

    @Override
    public Attachment getDepthAttachment(int index) {
        throw  new IllegalStateException("Tried to get depth from layer renderer. Get depth from compose layer instead.");
    }

    @Override
    public int getDepthFormat() {
        return AbstractRenderer.DEPTH_ATTACHMENT_FORMAT;
    }

    @Override
    public RenderContext getCurrentContext() {
        return this.renderContext;
    }

    public RenderTargetAttachmentSet getRenderTargetAttachments() {
        return this.frameBuffer.getAttachments();
    }

    private static class InternalRenderContext extends RenderContext {

        public InternalRenderContext() {
            this.frameIndex = -1;
            this.pass = -1;
            this.renderGoal = RenderGoal.NONE;
            this.currentPipeline = null;
        }

        public void setFrameIndex(int frameIndex) {
            this.frameIndex = frameIndex;
        }

        public void setPass(int pass) {
            this.pass = pass;
        }

        public void setRenderGoal(RenderGoal renderGoal) {
            this.renderGoal = renderGoal;
        }

        public void setCurrentPipeline(Pipeline currentPipeline) {
            this.currentPipeline = currentPipeline;
        }

    }
}
