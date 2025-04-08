package me.cg360.spudengine.wormholes.render.pass;

import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import org.lwjgl.vulkan.*;

public class PortalLayerColourRenderPass extends SwapChainRenderPass {

    public static final int MAX_PORTAL_DEPTH = 5;

    public PortalLayerColourRenderPass(SwapChain swapChain, int depthImageFormat) {
        super(swapChain, depthImageFormat);
    }

    @Override
    protected void configureColourAttachment(VkAttachmentDescription colourAttachment) {
        colourAttachment
                .format(this.swapChain.getSurfaceFormat().format())
                .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_LOAD)
                .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
    }

    @Override
    protected void configureDepthAttachment(VkAttachmentDescription depthAttachment, int depthImageFormat) {
        depthAttachment
                .format(depthImageFormat)
                .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK11.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .stencilLoadOp(VK11.VK_ATTACHMENT_LOAD_OP_LOAD)
                .stencilStoreOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                .finalLayout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
    }
}
