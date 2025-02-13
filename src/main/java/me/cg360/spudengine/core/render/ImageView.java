package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

public class ImageView implements VkHandleWrapper {

    private final LogicalDevice device;
    private final long imageViewHandle;

    private final int aspectMask;
    private final int mipLevels;

    private ImageView(LogicalDevice graphicsDevice, long imageHandle, Builder builder) {
        this.device = graphicsDevice;
        this.aspectMask = builder.aspectMask;
        this.mipLevels = builder.mipLevels;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            VkImageViewCreateInfo viewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(imageHandle)
                    .viewType(builder.viewType)
                    .format(builder.format)
                    .subresourceRange(it -> it
                            .aspectMask(this.aspectMask)
                            .baseMipLevel(0)
                            .levelCount(this.mipLevels)
                            .baseArrayLayer(builder.baseArrayLayer)
                            .layerCount(builder.layerCount));

            int errCreate = VK11.vkCreateImageView(this.device.asVk(), viewCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create image view");

            this.imageViewHandle = lp.get(0);
        }
    }


    public void cleanup() {
        VK11.vkDestroyImageView(device.asVk(), this.imageViewHandle, null);
    }

    public long getHandle() {
        return this.imageViewHandle;
    }


    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {

        private int aspectMask;
        private int baseArrayLayer;
        private int format;
        private int layerCount;
        private int mipLevels;
        private int viewType;

        public Builder() {
            this.aspectMask = 0;
            this.format = 0;

            this.baseArrayLayer = 0;
            this.layerCount = 1;
            this.mipLevels = 1;
            this.viewType = VK11.VK_IMAGE_VIEW_TYPE_2D;
        }

        public ImageView build(LogicalDevice graphicsDevice, long imageHandle) {
            return new ImageView(graphicsDevice, imageHandle, this);
        }

        public Builder aspectMask(int aspectMask) {
            this.aspectMask = aspectMask;
            return this;
        }

        public Builder baseArrayLayer(int baseArrayLayer) {
            this.baseArrayLayer = baseArrayLayer;
            return this;
        }

        public Builder format(int format) {
            this.format = format;
            return this;
        }

        public Builder layerCount(int layerCount) {
            this.layerCount = layerCount;
            return this;
        }

        public Builder mipLevels(int mipLevels) {
            this.mipLevels = mipLevels;
            return this;
        }

        public Builder viewType(int viewType) {
            this.viewType = viewType;
            return this;
        }

    }

}
