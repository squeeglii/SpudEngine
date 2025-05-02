package me.cg360.spudengine.core.render.image;

import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkExtent2D;

import java.nio.LongBuffer;
import java.util.Arrays;

public class ComposeFrameBuffer {

    private final SwapChainRenderPass renderPass;
    private FrameBuffer[] frameBuffers;
    private Attachment[] depthAttachments;

    public ComposeFrameBuffer(SwapChain swapChain) {
        this.renderPass = new SwapChainRenderPass(swapChain, AbstractRenderer.DEPTH_ATTACHMENT_FORMAT);

        this.createDepthImages(swapChain);
        this.createFrameBuffers(swapChain);
    }

    protected void createDepthImages(SwapChain swapChain) {
        int numImages = swapChain.getImageViews().length;
        VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();

        this.depthAttachments = new Attachment[numImages];
        for (int i = 0; i < numImages; i++) {
            this.depthAttachments[i] = new Attachment(swapChain.getDevice(), swapChainExtent.width(), swapChainExtent.height(),
                    AbstractRenderer.DEPTH_ATTACHMENT_FORMAT, VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }
    }

    private void createFrameBuffers(SwapChain swapChain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D extent2D = swapChain.getSwapChainExtent();
            int width = extent2D.width();
            int height = extent2D.height();

            int numImages = swapChain.getImageViews().length;
            this.frameBuffers = new FrameBuffer[numImages];

            LongBuffer attachments = stack.mallocLong(2);

            for (int i = 0; i < numImages; i++) {
                ImageView colourView = swapChain.getImageViews()[i];
                ImageView depthView = this.depthAttachments[i].getImageView();

                attachments.put(0, colourView.getHandle());
                attachments.put(1, depthView.getHandle());
                this.frameBuffers[i] = new FrameBuffer(swapChain.getDevice(), width, height, attachments, this.renderPass.getHandle());
            }
        }
    }


    public void onResize(SwapChain swapChain) {
        VulkanUtil.cleanupAll(this.frameBuffers);
        Arrays.stream(this.depthAttachments).forEach(Attachment::cleanup);

        this.createDepthImages(swapChain);
        this.createFrameBuffers(swapChain);
    }

    public void cleanup() {
        VulkanUtil.cleanupAll(this.frameBuffers);
        Arrays.stream(this.depthAttachments).forEach(Attachment::cleanup);

        this.renderPass.cleanup();
    }

    public FrameBuffer[] getBuffers() {
        return this.frameBuffers;
    }

    public Attachment[] getDepthAttachments() {
        return this.depthAttachments;
    }

    public SwapChainRenderPass getRenderPass() {
        return this.renderPass;
    }
}
