package me.cg360.spudengine.wormholes.render.pass;

import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class PortalLayeredRenderPass extends SwapChainRenderPass {

    public static final int MAX_PORTAL_DEPTH = 10;

    public PortalLayeredRenderPass(SwapChain swapChain, int depthImageFormat) {
        super(swapChain, depthImageFormat);
    }

    @Override
    protected void configureSubpasses(VkRenderPassCreateInfo builder, MemoryStack stack, VkAttachmentReference.Buffer colourRef, VkAttachmentReference depthRef) {
        VkSubpassDescription.Buffer subPass = VkSubpassDescription.calloc(MAX_PORTAL_DEPTH, stack);
        for(int i = 0; i < MAX_PORTAL_DEPTH; i++) {
            subPass.get(i)
                    .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(colourRef.remaining())
                    .pDepthStencilAttachment(depthRef)
                    .pColorAttachments(colourRef);
        }

        VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(MAX_PORTAL_DEPTH, stack);

        //int sharedStageMask = VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
        int src = VK11.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
        int dst = VK11.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;

        int sharedStageMask = VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT |
                              VK11.VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;

        //TODO: for tomorrow.
        // subpasses are inappropriate for what's needed here. stageMask and accessMask are for synchronisation.
        // use full render passes

        // srcSubpass    |  subpass we're dependent on.   EXTERNAL == previous render pass.
        // dstSubpass    |  index of current subpass, the one this dep exists for ????
        // srcStageMask  |  what must finish executing _before_ starting transition from src -> dest    ] these two seem quite tightly linked?
        // dstStageMask  |  what can we not execute until the stages of srcStageMask have completed.    ]
        // srcAccessMask |  memory access used by src access
        // dstAccessMask |  memory access used by dst access

        int colourReadOnly = VK11.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT ;
        int colourWriteOnly = VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
        int depthAndColour = VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK11.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;

        int finalPassIndex = MAX_PORTAL_DEPTH - 1;

        // Entry Subpass
        subpassDependencies.get(0)
                .srcSubpass(VK11.VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(src)
                .dstStageMask(dst)
                .srcAccessMask(0)   // ignore depth, but write over colour?
                .dstAccessMask(depthAndColour);
                ;//.dependencyFlags(VK11.VK_DEPENDENCY_BY_REGION_BIT);

        // Middle subpasses
        for(int i = 1; i < finalPassIndex; i++) {
            subpassDependencies.get(i)
                    .srcSubpass(i-1)
                    .dstSubpass(i)
                    .srcStageMask(src)
                    .dstStageMask(dst)
                    .srcAccessMask(0)   // ignore depth, but write over colour?
                    .dstAccessMask(depthAndColour);
                    ;//.dependencyFlags(VK11.VK_DEPENDENCY_BY_REGION_BIT);
        }

        // Ending subpass.
        subpassDependencies.get(finalPassIndex)
                .srcSubpass(finalPassIndex - 1)
                .dstSubpass(finalPassIndex)
                .srcStageMask(src)
                .dstStageMask(dst)
                .srcAccessMask(0)   // ignore depth, but write over colour?
                .dstAccessMask(depthAndColour);

        // Add to pass creator.
        builder.pSubpasses(subPass)
               .pDependencies(subpassDependencies);
    }
}
