package me.cg360.spudengine.core.render.image;

import me.cg360.spudengine.core.render.pipeline.pass.LayeredRenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;

import java.nio.LongBuffer;
import java.util.List;

public class LayeredFrameBuffer {

    private final LayeredRenderPass renderPass;

    private final int renderTargetCount;

    private FrameBuffer frameBuffer;
    private RenderTargetAttachmentSet attachments;

    public LayeredFrameBuffer(SwapChain swapChain, int renderTargetCount) {
        this.renderTargetCount = renderTargetCount;
        VkExtent2D extent = swapChain.getSwapChainExtent();
        int width = extent.width();
        int height = extent.height();

        this.attachments = new RenderTargetAttachmentSet(swapChain.getDevice(), width, height, renderTargetCount);
        this.renderPass = new LayeredRenderPass(swapChain.getDevice(), this.attachments);

        this.buildFramebuffer(swapChain, width, height);

    }

    private void buildFramebuffer(SwapChain swapChain, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            List<Attachment> attachments = this.attachments.getAttachments();
            LongBuffer attachmentHandles = stack.mallocLong(attachments.size());

            for(Attachment attachment : attachments)
                attachmentHandles.put(attachment.getImageView().getHandle());

            attachmentHandles.flip();

            // All render targets should be the same format, so renderpass shouldn't matter.
            this.frameBuffer = new FrameBuffer(swapChain.getDevice(), width, height, attachmentHandles, this.getRenderPass().getHandle());
        }
    }

    public void onResize(SwapChain swapChain) {
        this.frameBuffer.cleanup();
        this.attachments.cleanup();

        VkExtent2D extent2D = swapChain.getSwapChainExtent();
        int width = extent2D.width();
        int height = extent2D.height();

        this.attachments = new RenderTargetAttachmentSet(swapChain.getDevice(), width, height, this.renderTargetCount);
        this.buildFramebuffer(swapChain, width, height);
    }

    public void cleanup() {
        this.renderPass.cleanup();
        this.attachments.cleanup();
        this.frameBuffer.cleanup();
    }

    public RenderTargetAttachmentSet getAttachments() {
        return this.attachments;
    }

    public FrameBuffer getFrameBuffer() {
        return this.frameBuffer;
    }

    public LayeredRenderPass getRenderPass() {
        return this.renderPass;
    }

    public int getRenderTargetCount() {
        return this.renderTargetCount;
    }
}
