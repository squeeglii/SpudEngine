package me.cg360.spudengine.core.render.image;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import org.lwjgl.vulkan.VK11;

import java.util.ArrayList;
import java.util.List;

public class RenderTargetAttachmentSet {

    private final int renderTargetCount;
    private final List<Attachment> attachments;

    public RenderTargetAttachmentSet(LogicalDevice device, int width, int height, int renderTargets) {
        this.renderTargetCount = renderTargets;
        this.attachments = new ArrayList<>(renderTargets * 2);

        for (int i = 0; i < renderTargets; i++) {
            this.attachments.add(new Attachment(  // Colour
                    device, width, height,
                    VK11.VK_FORMAT_R8G8B8A8_SRGB,
                    VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
            ));

            this.attachments.add(new Attachment(  // Depth
                    device, width, height,
                    AbstractRenderer.DEPTH_ATTACHMENT_FORMAT,
                    VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
            ));
        }
    }

    public void cleanup() {
        for(Attachment attachment : this.attachments)
            attachment.cleanup();
    }

    public Attachment getColourAttachment(int renderTargetId) {
        return this.attachments.get(renderTargetId * 2);
    }

    public Attachment getDepthAttachment(int renderTargetId) {
        return this.attachments.get(1 + (renderTargetId * 2));
    }

    public int getColourAttachmentLocation(int renderTargetId) {
        return renderTargetId * 2;
    }

    public int getDepthAttachmentLocation(int renderTargetId) {
        return 1 + (renderTargetId * 2);
    }

    public int getRenderTargetCount() {
        return this.renderTargetCount;
    }

    public List<Attachment> getAttachments() {
        return this.attachments;
    }
}
