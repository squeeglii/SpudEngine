package me.cg360.spudengine.core.render.impl.forward;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.shader.StandardSamplers;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractForwardRenderer extends AbstractRenderer {

    protected CommandBuffer[] commandBuffers;
    protected Fence[] fences;

    protected FrameBuffer[] frameBuffers;

    protected PipelineCache pipelineCache;

    protected ShaderProgram shaderProgram;

    protected Attachment[] depthAttachments;

    public AbstractForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, int passCount, SubRenderProcess[] subRenderProcesses) {
        super(swapChain, scene, subRenderProcesses);

        this.pipelineCache = pipelineCache;

        int numImages = swapChain.getImageViews().length;
        this.createDepthImages();
        this.createRenderPasses(this.swapChain, AbstractRenderer.DEPTH_ATTACHMENT_FORMAT, passCount);
        this.createFrameBuffers();

        Logger.info("Loading Shader Program...");
        this.shaderProgram = this.buildShaderProgram();
        Logger.info("Successfully loaded shader program!");

        DescriptorSetLayout[] descriptorSetLayouts = this.initDescriptorSets();

        this.buildPipelines(descriptorSetLayouts);
        this.createCommandBuffers(commandPool, numImages);

        // Initialize uniforms which don't change frame-to-frame.
        this.setConstantUniforms();
    }

    protected abstract void createRenderPasses(SwapChain swapChain, int depthImageFormat, int requestedPassCount);
    protected abstract void createFrameBuffers();

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

    @Override
    public void waitTillFree() {
        int idx = this.swapChain.getCurrentFrame();
        Fence currentFence = this.fences[idx];
        currentFence.fenceWait();
    }

    @Override
    public void submit(CommandQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = this.swapChain.getCurrentFrame();

            CommandBuffer commandBuffer = this.commandBuffers[idx];
            Fence currentFence = this.fences[idx];
            currentFence.reset();

            ForwardSemaphores syncSemaphores = this.swapChain.getSyncSemaphores()[idx];

            queue.submit(stack.pointers(commandBuffer.asVk()),
                    stack.longs(syncSemaphores.imageAcquisitionSemaphore().getHandle()),
                    stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.renderCompleteSemaphore().getHandle()),
                    currentFence);
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

    protected void setupView(VkCommandBuffer cmd, MemoryStack stack, int width, int height) {
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

    @Override
    public Attachment getDepthAttachment(int index) {
        return this.depthAttachments[index];
    }

    @Override
    public int getDepthFormat() {
        return DEPTH_ATTACHMENT_FORMAT;
    }

}
