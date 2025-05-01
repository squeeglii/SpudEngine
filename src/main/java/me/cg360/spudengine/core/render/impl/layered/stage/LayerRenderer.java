package me.cg360.spudengine.core.render.impl.layered.stage;

import me.cg360.spudengine.core.exception.UnimplementedException;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.context.RenderGoal;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.*;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.shader.StandardSamplers;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.world.Scene;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.util.List;

/** Render each layer to  */
public class LayerRenderer extends AbstractRenderer {

    protected CommandBuffer[] commandBuffers;
    protected Fence[] fences;

    protected LayeredFrameBuffer frameBuffer;

    protected ShaderProgram shaderProgram;

    protected PipelineCache pipelineCache;

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

    }

    @Override
    public void recordDraw(Renderer renderer) {
        throw new UnimplementedException("Layered Renderer is not implemented yet.");
    }

    @Override
    public void submit(CommandQueue queue) {

    }

    @Override
    public void waitTillFree() {

    }

    @Override
    public void onResize(SwapChain newSwapChain) {
        DataTypes.MAT4X4F.copyToBuffer(this.uProjectionMatrix, this.scene.getProjection().asMatrix());
        this.swapChain = newSwapChain;
        this.frameBuffer.resize(this.swapChain);
    }

    @Override
    public void cleanup() {

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
