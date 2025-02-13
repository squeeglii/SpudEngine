package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.render.sync.SyncSemaphores;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.LongBuffer;
import java.util.Arrays;

public class ForwardRendererActivity {

    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private final FrameBuffer[] frameBuffers;
    private final SwapChainRenderPass renderPass;
    private final SwapChain swapChain;

    public ForwardRendererActivity(SwapChain swapChain, CommandPool commandPool) {
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

            this.commandBuffers = new CommandBuffer[numImages];
            this.fences = new Fence[numImages];

            // buffer per swapchain image for convenience. only 2 / 3 are needed really.
            for (int i = 0; i < numImages; i++) {
                this.commandBuffers[i] = new CommandBuffer(commandPool, true, false);
                this.fences[i] = new Fence(device, true);
                this.recordCommandBuffer(this.commandBuffers[i], this.frameBuffers[i], swapChainExtent.width(), swapChainExtent.height());
            }
        }
    }

    public void waitForFence() {
        int idx = this.swapChain.getCurrentFrame();
        Fence currentFence = fences[idx];
        currentFence.fenceWait();
    }

    private void recordCommandBuffer(CommandBuffer commandBuffer, FrameBuffer frameBuffer, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);


            Color c = EngineProperties.CLEAR_COLOUR;
            Logger.debug("Set clear colour to: {}", c);
            clearValues.apply(0, v -> v.color()
                    .float32(0, c.getRed())
                    .float32(1, c.getGreen())
                    .float32(2, c.getBlue())
                    .float32(3, c.getAlpha())
            );

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.renderPass.getHandle())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getHandle());

            commandBuffer.beginRecording();
            VK11.vkCmdBeginRenderPass(commandBuffer.asVk(), renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
            VK11.vkCmdEndRenderPass(commandBuffer.asVk());
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
        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        this.renderPass.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

}
