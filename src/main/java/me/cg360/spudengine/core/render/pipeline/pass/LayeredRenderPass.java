package me.cg360.spudengine.core.render.pipeline.pass;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.RenderTargetAttachmentSet;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

public class LayeredRenderPass {

    protected final LogicalDevice device;
    protected final long renderPassHandle;

    public LayeredRenderPass(LogicalDevice device, RenderTargetAttachmentSet attachmentSet) {
        this.device = device;

        try(MemoryStack stack = MemoryStack.stackPush()) {

            // Attachment Config
            List<Attachment> attachments = attachmentSet.getAttachments();
            VkAttachmentDescription.Buffer allAttachments = VkAttachmentDescription.calloc(attachments.size(), stack);

            for (int i = 0; i < attachments.size(); i++) {
                Attachment attachment = attachments.get(i);
                int finalLayout  = attachment.isDepthAttachment()
                        ? VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL
                        : VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

                allAttachments.get(i)
                            .format(attachment.getImage().getFormat())
                            .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                            .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                            .stencilLoadOp(VK11.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                            .stencilStoreOp(VK11.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                            .samples(1)
                            .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                            .finalLayout(finalLayout);
            }


            // Subpasses
            int numRenderTargets = attachmentSet.getRenderTargetCount();
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(numRenderTargets, stack);
            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(2 * numRenderTargets, stack);

            int subpassDependency = 0;
            for(int subpassId = 0; subpassId < numRenderTargets; subpassId++) {
                int colourLocation = attachmentSet.getColourAttachmentLocation(subpassId);
                int depthLocation = attachmentSet.getDepthAttachmentLocation(subpassId);

                this.createSubpass(stack, subpass, subpassId, colourLocation, depthLocation);

                this.configureDrawDependency(subpassDependencies.get(subpassDependency++), subpassId);
                this.configureLayoutDependency(subpassDependencies.get(subpassDependency++), subpassId);
            }

            // Assemble final pass
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pSubpasses(subpass)
                    .pAttachments(allAttachments)
                    .pDependencies(subpassDependencies);

            // Build it all!
            LongBuffer lp = stack.mallocLong(1);
            int errCreatePass = VK11.vkCreateRenderPass(this.device.asVk(), renderPassInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreatePass, "Failed to create render pass");
            this.renderPassHandle = lp.get(0);
        }
    }

    private void createSubpass(MemoryStack stack, VkSubpassDescription.Buffer subpassBuffer, int subpassId, int colourPos, int depthPos) {
        VkAttachmentReference.Buffer colourAttachments = VkAttachmentReference.calloc(1, stack);
        VkAttachmentReference depthAttachment = VkAttachmentReference.calloc(stack);

        colourAttachments.get(0).attachment(colourPos).layout(VK11.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        depthAttachment.attachment(depthPos).layout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        subpassBuffer.get(subpassId)
                .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colourAttachments)
                .pDepthStencilAttachment(depthAttachment);
    }

    private void configureDrawDependency(VkSubpassDependency dependency, int subpassId) {
        dependency.srcSubpass(VK11.VK_SUBPASS_EXTERNAL)
                  .dstSubpass(subpassId)
                  .srcStageMask(VK11.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                  .dstStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK11.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                  .srcAccessMask(VK11.VK_ACCESS_MEMORY_READ_BIT)
                  .dstAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                        | VK11.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK11.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                  .dependencyFlags(VK11.VK_DEPENDENCY_BY_REGION_BIT);
    }

    private void configureLayoutDependency(VkSubpassDependency dependency, int subpassId) {
        dependency.srcSubpass(subpassId)
                  .dstSubpass(VK11.VK_SUBPASS_EXTERNAL)
                  .srcStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK11.VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT)
                  .dstStageMask(VK11.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                  .srcAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                        | VK11.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK11.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                  .dstAccessMask(VK11.VK_ACCESS_MEMORY_READ_BIT)
                  .dependencyFlags(VK11.VK_DEPENDENCY_BY_REGION_BIT);
    }

    public void cleanup() {
        VK11.vkDestroyRenderPass(this.device.asVk(), this.renderPassHandle, null);
    }

    public long getHandle() {
        return this.renderPassHandle;
    }

}
