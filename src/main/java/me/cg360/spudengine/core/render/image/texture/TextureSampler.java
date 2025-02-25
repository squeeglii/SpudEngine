package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

public class TextureSampler implements VkHandleWrapper {

    public static final int MAX_ANISOTROPY = 16;

    private final LogicalDevice device;
    private final long textureSampler;

    public TextureSampler(LogicalDevice device, int mipLevels, boolean enableAnisotrophy) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK11.VK_FILTER_LINEAR)
                    .minFilter(VK11.VK_FILTER_LINEAR)
                    .addressModeU(VK11.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK11.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK11.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .borderColor(VK11.VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK11.VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK11.VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .minLod(0.0f)
                    .maxLod(mipLevels)
                    .mipLodBias(0.0f);
            if (enableAnisotrophy && device.isSamplerAnisotrophyEnabled()) {
                samplerInfo.anisotropyEnable(true)
                           .maxAnisotropy(MAX_ANISOTROPY);
            }

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateSampler(device.asVk(), samplerInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create sampler");
            this.textureSampler = lp.get(0);
        }
    }

    @Override
    public void cleanup() {
        VK11.vkDestroySampler(this.device.asVk(), this.textureSampler, null);
    }

    @Override
    public long getHandle() {
        return this.textureSampler;
    }
}
