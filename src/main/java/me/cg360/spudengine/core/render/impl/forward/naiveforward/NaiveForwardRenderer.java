package me.cg360.spudengine.core.render.impl.forward.naiveforward;

import me.cg360.spudengine.core.render.RenderSystem;
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
import me.cg360.spudengine.core.render.impl.forward.AbstractForwardRenderer;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.render.pipeline.util.ShaderStage;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.util.VulkanUtil;
import me.cg360.spudengine.core.world.Scene;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

public class NaiveForwardRenderer extends AbstractForwardRenderer {

    private SwapChainRenderPass renderPass;

    private Pipeline standardPipeline;
    private Pipeline wireframePipeline;

    private InternalRenderContext renderContext;

    public NaiveForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(swapChain, commandPool, pipelineCache, scene, 1, subRenderProcesses);

        this.renderContext = new InternalRenderContext();
    }

    @Override
    protected void buildPipelines(DescriptorSetLayout[] descriptorSetLayouts) {
        Logger.debug("Building Pipelines...");

        Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION_UV.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantStage(ShaderStage.GEOMETRY)
                .setPushConstantLayout(  // @see ShaderIO#applyVertex/GeometryPushConstants(...) for how this is filled.
                        DataTypes.MAT4X4F // transform
                )
                .setCullMode(VK11.VK_CULL_MODE_FRONT_BIT); // uhhhh, right. Sure. This works but

        this.standardPipeline = builder.build(this.pipelineCache, "standard", this.renderPass.getHandle(), this.shaderProgram, 1);
        this.wireframePipeline = builder
                .setUsingWireframe(true)
                .build(this.pipelineCache, "wireframe", this.renderPass.getHandle(), this.shaderProgram, 1);

        builder.cleanup();
        Logger.debug("Built pipelines!");
    }

    @Override
    protected void createRenderPasses(SwapChain swapChain, int depthImageFormat, int requestedPassCount) {
        this.renderPass = new SwapChainRenderPass(swapChain, this.depthAttachments[0].getImage().getFormat());
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

                this.frameBuffers[i] = new FrameBuffer(this.device, swapChainExtent.width(), swapChainExtent.height(),
                        pAttachments, this.renderPass.getHandle());
            }
        }
    }

    protected void doRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, MemoryStack stack, Consumer<RenderContext> passActions) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
        this.renderContext.setPass(0);
        this.renderContext.setCurrentPipeline(pipeline);
        passActions.accept(this.renderContext);
        VK11.vkCmdEndRenderPass(cmd);
    }

    @Override
    public void recordDraw(RenderSystem renderSystem) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = this.swapChain.getCurrentFrame();
            this.renderContext.setFrameIndex(idx);

            //Fence fence = this.fences[idx];
            CommandBuffer commandBuffer = this.commandBuffers[idx];
            FrameBuffer frameBuffer = this.frameBuffers[idx];

            //fence.fenceWait();
            //fence.reset();
            commandBuffer.reset();

            VkClearValue.Buffer clearValues = VulkanUtil.generateClearValues(stack);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.renderPass.getHandle())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getHandle());

            VkCommandBuffer cmd = commandBuffer.beginRecording();
            Pipeline selectedPipeline = renderSystem.useWireframe
                    ? this.wireframePipeline
                    : this.standardPipeline;

            this.doRenderPass(cmd, renderPassBeginInfo, selectedPipeline, stack, context -> {
                // Setup view
                this.setupView(cmd, stack, width, height);

                // Populate Uniforms
                this.shaderIO.reset(stack, selectedPipeline, this.descriptorPool);

                Matrix4f view = this.scene.getMainCamera().getViewMatrix();
                DataTypes.MAT4X4F.copyToBuffer(this.uViewMatrix[idx], view);

                this.shaderIO.setUniform(this.lProjectionMatrix, this.dProjectionMatrix);
                this.shaderIO.setUniform(this.lViewMatrix, this.dViewMatrix, idx);

                for(SubRenderProcess process:  this.subRenderProcesses)
                    process.renderPreMesh(this.renderContext, this.shaderIO, this.standardSamplers);

                this.drawAllSceneModels(cmd, renderSystem, selectedPipeline, idx);

                this.shaderIO.free(); // free buffers from stack.
            });

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

        this.standardPipeline.cleanup();
        this.wireframePipeline.cleanup();

        this.shaderProgram.cleanup();

        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        this.renderPass.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

    @Override
    public RenderContext getCurrentContext() {
        return this.renderContext;
    }


    private static class InternalRenderContext extends RenderContext {

        protected void reset() {
            this.frameIndex = -1;
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

        protected void setFrameIndex(int frameIndex) {
            this.frameIndex = frameIndex;
        }
    }

}
