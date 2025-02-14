package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.shader.BinaryShaderFile;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderCompiler;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderType;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.render.sync.SyncSemaphores;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ForwardRendererActivity {

    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private final FrameBuffer[] frameBuffers;
    private final SwapChainRenderPass renderPass;
    private final SwapChain swapChain;

    private final Pipeline pipeline;
    private final Pipeline wireframePipeline;
    private final ShaderProgram shaderProgram;

    public ForwardRendererActivity(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache) {
        this.swapChain = swapChain;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LogicalDevice device = swapChain.getDevice();
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            ImageView[] imageViews = swapChain.getImageViews();
            int numImages = imageViews.length;

            this.renderPass = new SwapChainRenderPass(swapChain);

            LongBuffer pAttachments = stack.mallocLong(1);
            this.frameBuffers = new FrameBuffer[numImages];

            for (int i = 0; i < numImages; i++) {
                pAttachments.put(0, imageViews[i].getHandle());
                this.frameBuffers[i] = new FrameBuffer(device, swapChainExtent.width(), swapChainExtent.height(),
                        pAttachments, this.renderPass.getHandle());
            }

            List<BinaryShaderFile> shaders = List.of(
                    new BinaryShaderFile(ShaderType.VERTEX, "shaders/vertex"),
                    new BinaryShaderFile(ShaderType.FRAGMENT, "shaders/fragment")
            );

            if (EngineProperties.SHOULD_RECOMPILE_SHADERS) {
                long recompiles = shaders.stream().filter(ShaderCompiler::compileShaderIfChanged).count();
                Logger.info("Recompiled {} shader(s).", recompiles);
            }

            this.shaderProgram = new ShaderProgram(device, shaders);

            Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION.get());
            this.pipeline = builder.build(pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);

            builder.setWireFrameEnabled(true);
            this.wireframePipeline = builder.build(pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);

            builder.cleanup();

            this.commandBuffers = new CommandBuffer[numImages];
            this.fences = new Fence[numImages];
            for (int i = 0; i < numImages; i++) {
                this.commandBuffers[i] = new CommandBuffer(commandPool, true, false);
                this.fences[i] = new Fence(device, true);
            }
        }
    }

    public void waitForFence() {
        int idx = this.swapChain.getCurrentFrame();
        Fence currentFence = this.fences[idx];
        currentFence.fenceWait();
    }

    public void record(Renderer renderer, Collection<BufferedModel> models, float time) {
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

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            Color c = EngineProperties.CLEAR_COLOUR;
            float[] components = new float[4];
            c.getComponents(components);
            clearValues.apply(0, v -> v.color()
                    .float32(0, components[0])
                    .float32(1, components[1])
                    .float32(2, components[2])
                    .float32(3, components[3])
            );

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.renderPass.getHandle())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getHandle());

            commandBuffer.beginRecording();
            VkCommandBuffer cmd = commandBuffer.asVk();
            VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE); // ----

            long selectedPipeline = renderer.useWireframe ? this.wireframePipeline.getHandle() : this.pipeline.getHandle();
            VK11.vkCmdBindPipeline(cmd, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, selectedPipeline);

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

            // Render Models
            LongBuffer offsets = stack.mallocLong(1);
            offsets.put(0, 0L);
            LongBuffer vertexBufferHandle = stack.mallocLong(1);

            // Push time as a "push constant"
            FloatBuffer timeBuf = stack.mallocFloat(1);
            timeBuf.put(0, time);
            VK11.vkCmdPushConstants(cmd, this.pipeline.getPipelineLayoutHandle(), VK11.VK_SHADER_STAGE_FRAGMENT_BIT, 0, timeBuf);

            IntBuffer tickRateBuf = stack.mallocInt(1);
            tickRateBuf.put(0, EngineProperties.UPDATES_PER_SECOND);
            VK11.vkCmdPushConstants(cmd, this.pipeline.getPipelineLayoutHandle(), VK11.VK_SHADER_STAGE_FRAGMENT_BIT, VulkanUtil.INT_BYTES, tickRateBuf);

            for (BufferedModel vulkanModel : models) {
                for (BufferedMesh mesh : vulkanModel.getSubMeshes()) {
                    vertexBufferHandle.put(0, mesh.vertices().getHandle());
                    VK11.vkCmdBindVertexBuffers(cmd, 0, vertexBufferHandle, offsets);
                    VK11.vkCmdBindIndexBuffer(cmd, mesh.indices().getHandle(), 0, VK11.VK_INDEX_TYPE_UINT32);
                    VK11.vkCmdDrawIndexed(cmd, mesh.numIndices(), 1, 0, 0, 0);
                }
            }

            VK11.vkCmdEndRenderPass(cmd); // ------------------------------------------------------
            commandBuffer.endRecording();
        }
    }

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

    public void cleanup() {
        this.pipeline.cleanup();
        this.wireframePipeline.cleanup();

        this.shaderProgram.cleanup();

        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        this.renderPass.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

}
