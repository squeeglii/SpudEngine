package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Image;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.UploadMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.tinylog.Logger;

import java.nio.ByteBuffer;

public abstract class Texture {

    protected final String resourceName;
    protected final boolean isGenerated;

    protected int width;
    protected int height;
    protected int mipLevels;
    protected int format;

    protected Image image;
    protected ImageView view;

    protected GeneralBuffer stagingBuffer;
    protected boolean recordedTransition; // has been processed and uploaded.

    public Texture(String resourceName, boolean isGenerated) {
        Logger.debug("Creating texture: '{}' [generated={}]", resourceName, isGenerated);
        this.resourceName = resourceName.trim().toLowerCase();
        this.isGenerated = isGenerated;

        this.stagingBuffer = null;
        this.recordedTransition = false;
    }

    protected final void createStagingBuffer(LogicalDevice device, ByteBuffer imageBuffer) {
        int size = imageBuffer.remaining();
        int requirements = VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK11.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
        this.stagingBuffer = new GeneralBuffer(device, size, VK11.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, requirements);

        Logger.debug("Created Staging buffer of size {}", size);

        long mappedMemory = this.stagingBuffer.map();
        ByteBuffer buffer = MemoryUtil.memByteBuffer(mappedMemory, (int) this.stagingBuffer.getRequestedSize());
        buffer.put(imageBuffer);
        imageBuffer.flip();
        this.stagingBuffer.unmap();
    }

    protected final void createImageWrappers(LogicalDevice device) {
        this.image = Image.builder(this.width, this.height, VK11.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK11.VK_IMAGE_USAGE_SAMPLED_BIT)
                .format(this.format)
                .mipLevels(this.mipLevels)
                .build(device);
        this.view = ImageView.builder()
                .format(this.image.getFormat())
                .mipLevels(this.mipLevels)
                .aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT).build(device, this.image.getHandle());
    }


    public void upload(CommandBuffer cmd) {
        if(this.stagingBuffer == null || this.recordedTransition) {
            Logger.debug("Texture '{}' has already been uploaded", this.resourceName);
            return;
        }

        Logger.debug("Uploading texture '{}'", this.resourceName);
        this.recordedTransition = true;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.setTransitionMode(stack, cmd, UploadMode.UNDEFINED_TO_TRANSFER);
            this.copyBuffer(stack, cmd, this.stagingBuffer);
            this.setTransitionMode(stack, cmd, UploadMode.TRANSFER_TO_SHADER_SAMPLER);
        }
    }

    protected final void setTransitionMode(MemoryStack stack, CommandBuffer cmd, UploadMode uploadMode) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK11.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(uploadMode.fromLayout())
                .newLayout(uploadMode.toLayout())
                .srcQueueFamilyIndex(VK11.VK_QUEUE_FAMILY_IGNORED) // transferring ownership between queues.
                .dstQueueFamilyIndex(VK11.VK_QUEUE_FAMILY_IGNORED)
                .image(this.image.getHandle())
                .subresourceRange(it -> it       // what parts of image are affected.
                        .aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0)
                        .levelCount(this.mipLevels)
                        .baseArrayLayer(0)
                        .layerCount(1))
                .srcAccessMask(uploadMode.srcAccessMask())
                .dstAccessMask(uploadMode.dstStage());

        VK11.vkCmdPipelineBarrier(cmd.asVk(), uploadMode.srcStage(), uploadMode.dstStage(), 0, null, null, barrier);
    }

    protected final void copyBuffer(MemoryStack stack, CommandBuffer cmd, GeneralBuffer buffer) {
        VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                .bufferOffset(0)
                .bufferRowLength(0)
                .bufferImageHeight(0)
                .imageSubresource(it ->
                        it.aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT)
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1)
                )
                .imageOffset(it -> it.x(0).y(0).z(0))
                .imageExtent(it -> it.width(this.width).height(this.height).depth(1));

        VK11.vkCmdCopyBufferToImage(cmd.asVk(), buffer.getHandle(), this.image.getHandle(),
                VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
    }

    public final void cleanupStagingBuffer() {
        if(this.stagingBuffer == null) return;

        this.stagingBuffer.cleanup();
        this.stagingBuffer = null;
    }

    public void cleanup() {
        this.cleanupStagingBuffer();
        this.view.cleanup();
        this.image.cleanup();
    }


    public String getResourceName() {
        return this.resourceName;
    }

    public final boolean isGenerated() {
        return this.isGenerated;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public ImageView getImageView() {
        return this.view;
    }

}
