package me.cg360.spudengine.render;

import me.cg360.spudengine.util.VkHandleWrapper;
import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

/**
 * Render Pass Template.
 * Renders colour to the existing swapchain images.
 */
public class SwapChainRenderPass implements VkHandleWrapper {

    private final SwapChain swapChain;
    private final long renderPassHandle;

    public SwapChainRenderPass(SwapChain swapChain) {
        this.swapChain = swapChain;

        try(MemoryStack stack = MemoryStack.stackPush()) {

            // Attachment
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
            attachments.get(0)
                       .format(this.swapChain.getSurfaceFormat().format())
                       .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                       .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                       .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                       .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                       .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            // Subpass
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack)
                    .attachment(0)
                    .layout(VK11.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subPass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(colorReference.remaining())
                    .pColorAttachments(colorReference);

            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(1, stack);
            subpassDependencies.get(0)
                    .srcSubpass(VK11.VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstSubpass(0)
                    .dstStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            // Assemble final pass
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments)
                    .pSubpasses(subPass)
                    .pDependencies(subpassDependencies);

            LongBuffer lp = stack.mallocLong(1);
            int errCreatePass = VK11.vkCreateRenderPass(swapChain.getDevice().asVk(), renderPassInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreatePass, "Failed to create render pass");
            this.renderPassHandle = lp.get(0);
        }
    }

    public void cleanup() {
        VK11.vkDestroyRenderPass(this.swapChain.getDevice().asVk(), this.renderPassHandle, null);
    }

    public long getHandle() {
        return this.renderPassHandle;
    }

}
