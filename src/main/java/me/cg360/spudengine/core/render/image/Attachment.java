package me.cg360.spudengine.core.render.image;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.vulkan.VK11;

/** Render pass attachment, describing the structure of an associate image.*/
public class Attachment {

    private final Image image;
    private final ImageView imageView;

    private boolean isDepthAttachment;


    public Attachment(LogicalDevice device, int width, int height, int format, int usage){
        this.image = Image.builder(width, height, usage | VK11.VK_IMAGE_USAGE_SAMPLED_BIT)
                .setFormat(format)
                .build(device);

        int aspectMask = 0;

        if ((usage & VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0) {
            aspectMask = VK11.VK_IMAGE_ASPECT_COLOR_BIT;
            this.isDepthAttachment = false;
        }

        if ((usage & VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) {
            aspectMask = VK11.VK_IMAGE_ASPECT_DEPTH_BIT;
            this.isDepthAttachment = true;
        }

        this.imageView = ImageView.builder()
                  .format(this.image.getFormat())
                  .aspectMask(aspectMask)
                  .build(device, this.image.getHandle());
    }


    public void cleanup () {
        this.imageView.cleanup();
        this.image.cleanup();
    }

    public Image getImage () {
        return this.image;
    }

    public ImageView getImageView () {
        return this.imageView;
    }

    public boolean isDepthAttachment () {
        return this.isDepthAttachment;
    }
}
