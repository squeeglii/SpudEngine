package me.cg360.spudengine.core.render.image;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class Image {

    private final LogicalDevice device;
    private final long image;
    private final long memory;

    private final int format;
    private final int mipLevels;

    private final int width;
    private final int height;

    private Image(LogicalDevice graphicsDevice, Builder builder, int width, int height, int usage) {
        this.device = graphicsDevice;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.format = builder.format;
            this.mipLevels = builder.mipLevels;

            this.width = width;
            this.height = height;

            VkImageCreateInfo createInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK11.VK_IMAGE_TYPE_2D)
                    .format(this.format)
                    .extent(it -> it
                            .width(this.width)
                            .height(this.height)
                            .depth(1)
                    )
                    .mipLevels(this.mipLevels)
                    .arrayLayers(builder.layerCount)
                    .samples(builder.sampleCount)
                    .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                    .sharingMode(VK11.VK_SHARING_MODE_EXCLUSIVE)
                    .tiling(VK11.VK_IMAGE_TILING_OPTIMAL)
                    .usage(usage);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateImage(this.device.asVk(), createInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create image");

            this.image = lp.get(0);

            // Image needs memory allocated for its data.
            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            VK11.vkGetImageMemoryRequirements(this.device.asVk(), this.image, memReqs);

            int memType = VulkanUtil.memoryTypeFromProperties(this.device.getPhysicalDevice(), memReqs.memoryTypeBits(), 0);
            VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(memType);

            int errAllocate = VK11.vkAllocateMemory(this.device.asVk(), memAlloc, null, lp);
            VulkanUtil.checkErrorCode(errAllocate, "Failed to allocate memory");
            this.memory = lp.get(0);

            // Bind memory
            int errBind = VK11.vkBindImageMemory(this.device.asVk(), this.image, this.memory, 0);
            VulkanUtil.checkErrorCode(errBind, "Failed to bind image memory");
        }
    }

    //TODO: Add a way to download the image to a buffer for
    // obtaining the depth at a point.
    //public void download(VkCommandBuffer cmd) {
    //
    //}

    public void cleanup() {
        VK11.vkDestroyImage(this.device.asVk(), this.image, null);
        VK11.vkFreeMemory(this.device.asVk(), this.memory, null);
    }

    public int getFormat() {
        return this.format;
    }

    public int getMipLevels() {
        return this.mipLevels;
    }

    public long getHandle() {
        return this.image;
    }

    public long getMemoryHandle() {
        return this.memory;
    }

    public static Builder builder(int width, int height, int usage) {
        return new Builder(width, height, usage);
    }


    public static class Builder {
        private int width;
        private int height;
        private int usage;

        private int format;
        private int mipLevels;
        private int sampleCount;
        private int layerCount;

        public Builder(int width, int height, int usage) {
            this.width = width;
            this.height = height;
            this.usage = usage;

            this.format = VK11.VK_FORMAT_R8G8B8A8_SRGB;
            this.mipLevels = 1;
            this.sampleCount = 1;
            this.layerCount = 1;
        }

        public Image build(LogicalDevice graphicsDevice) {
            return new Image(graphicsDevice, this, this.width, this.height, this.usage);
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder usage(int usage) {
            this.usage = usage;
            return this;
        }

        public Builder format(int format) {
            this.format = format;
            return this;
        }

        public Builder mipLevels(int mipLevels) {
            this.mipLevels = mipLevels;
            return this;
        }

        public Builder sampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }

        public Builder layerCount(int layerCount) {
            this.layerCount = layerCount;
            return this;
        }
    }

}
