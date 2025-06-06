package me.cg360.spudengine.core.render.pipeline.pass;

import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

/**
 * Render Pass Template.
 * Renders colour to the existing swapchain images.
 */
public class SwapChainRenderPass implements VkHandleWrapper {

    protected final SwapChain swapChain;
    protected final long renderPassHandle;

    public SwapChainRenderPass(SwapChain swapChain, int depthImageFormat) {
        this.swapChain = swapChain;   //TODO: Does this need re-creating onResize?

        try(MemoryStack stack = MemoryStack.stackPush()) {
            // Attachment
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, stack);
            this.configureColourAttachment(attachments.get(0));
            this.configureDepthAttachment(attachments.get(1), depthImageFormat); // Depth

            // Subpass Attachments.
            VkAttachmentReference.Buffer colourReference = VkAttachmentReference.calloc(1, stack)
                    .attachment(0)
                    .layout(VK11.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkAttachmentReference depthReference = VkAttachmentReference.malloc(stack)
                    .attachment(1)
                    .layout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            // Assemble final pass
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments);

            this.configureSubpasses(renderPassInfo, stack, colourReference, depthReference);

            // Build it all!
            LongBuffer lp = stack.mallocLong(1);
            int errCreatePass = VK11.vkCreateRenderPass(swapChain.getDevice().asVk(), renderPassInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreatePass, "Failed to create render pass");
            this.renderPassHandle = lp.get(0);
        }
    }

    protected void configureColourAttachment(VkAttachmentDescription colourAttachment) {
        colourAttachment // Colour.
        .format(this.swapChain.getSurfaceFormat().format())
        .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
        .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
        .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
        .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
        .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
    }

    protected void configureDepthAttachment(VkAttachmentDescription depthAttachment, int depthImageFormat) {
        depthAttachment // Colour.
                .format(depthImageFormat)
                .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK11.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .stencilLoadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .stencilStoreOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
    }

    protected void configureSubpasses(VkRenderPassCreateInfo builder, MemoryStack stack, VkAttachmentReference.Buffer colourRef, VkAttachmentReference depthRef) {
        VkSubpassDescription.Buffer subPass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(colourRef.remaining())
                .pDepthStencilAttachment(depthRef)
                .pColorAttachments(colourRef);

        VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(1, stack);
        int sharedStageMask = VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT |
                              VK11.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

        subpassDependencies.get(0)
                .srcSubpass(VK11.VK_SUBPASS_EXTERNAL)
                .srcStageMask(sharedStageMask)
                .srcAccessMask(0)
                .dstSubpass(0)
                .dstStageMask(sharedStageMask)
                .dstAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK11.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

        // Add to pass creator.
        builder.pSubpasses(subPass)
               .pDependencies(subpassDependencies);
    }

    public void cleanup() {
        VK11.vkDestroyRenderPass(this.swapChain.getDevice().asVk(), this.renderPassHandle, null);
    }

    public long getHandle() {
        return this.renderPassHandle;
    }

}
