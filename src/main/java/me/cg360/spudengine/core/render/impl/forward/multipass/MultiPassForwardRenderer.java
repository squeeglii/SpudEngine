package me.cg360.spudengine.core.render.impl.forward.multipass;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.context.RenderGoal;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.impl.forward.CommonForwardRenderer;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.wormholes.render.pass.PortalLayerColourRenderPass;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

public class MultiPassForwardRenderer extends CommonForwardRenderer {

    private SwapChainRenderPass[] renderPasses;

    private Pipeline[] standardPipeline;
    private Pipeline[] wireframePipeline;

    private final InternalRenderContext renderContext;


    public MultiPassForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(swapChain, commandPool, pipelineCache, scene, subRenderProcesses);

        this.renderContext = new InternalRenderContext();
    }

    @Override
    protected void createRenderPasses(SwapChain swapChain, int depthImageFormat) {
        //TODO: Remove portal references
        this.renderPasses = new SwapChainRenderPass[PortalLayerColourRenderPass.MAX_PORTAL_DEPTH];

        // Initial pass, clears all values
        this.renderPasses[0] = new SwapChainRenderPass(swapChain, depthImageFormat);

        // colour render passes
        for(int pass =  1; pass < PortalLayerColourRenderPass.MAX_PORTAL_DEPTH; pass++) {
            this.renderPasses[pass] = new PortalLayerColourRenderPass(swapChain, depthImageFormat);
        }
    }

    @Override
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

    @Override
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

        //builder.setUsingStencilTest(true);
        for(int pass = 0; pass < this.renderPasses.length; pass++) {
            int val = pass % PortalLayerColourRenderPass.MAX_PORTAL_DEPTH;
            Logger.debug("Standard #{}: read {}", pass, val);

            this.standardPipeline[pass] = builder
                    .setUsingStencilTest(false)
                    .setUsingDepthTest(true)
                    .setUsingDepthWrite(true)
                    .build(this.pipelineCache, "standard", this.renderPasses[pass].getHandle(), this.shaderProgram, 1);
        }

        builder.setUsingWireframe(true)
                .setUsingStencilTest(false);
        for(int pass = 0; pass < this.renderPasses.length; pass++)
            this.wireframePipeline[pass] = builder
                    .build(this.pipelineCache, "wireframe", this.renderPasses[pass].getHandle(), this.shaderProgram, 1);

        builder.cleanup();
        Logger.debug("Built pipelines!");
    }

    protected void doRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, MemoryStack stack, int pass, Consumer<RenderContext> passActions) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
        this.renderContext.setPass(pass);
        this.renderContext.setCurrentPipeline(pipeline);
        passActions.accept(this.renderContext);
        VK11.vkCmdEndRenderPass(cmd);
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
            VkClearValue.Buffer clearValues = CommonForwardRenderer.generateClearValues(stack); // initial

            // standard pass.
            this.renderContext.setRenderGoal(RenderGoal.STANDARD_DRAWING);
            for(int pass = 0; pass < PortalLayerColourRenderPass.MAX_PORTAL_DEPTH; pass++) {
                VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                        .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                        .renderPass(this.renderPasses[pass].getHandle())
                        .pClearValues(clearValues)
                        .renderArea(a -> a.extent().set(width, height))
                        .framebuffer(frameBuffer.getHandle());

                Pipeline selectedPipeline = renderer.useWireframe
                        ? this.wireframePipeline[pass]
                        : this.standardPipeline[pass];

                // Run a pass for each layer of portal depth.
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

                    this.shaderIO.free(); // free buffers from stack.
                });
            }

            commandBuffer.endRecording();
        }
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

        this.shaderProgram.cleanup();

        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        VulkanUtil.cleanupAll(this.renderPasses);

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
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
