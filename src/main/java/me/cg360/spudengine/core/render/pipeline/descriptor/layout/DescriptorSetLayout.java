package me.cg360.spudengine.core.render.pipeline.descriptor.layout;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

public abstract class DescriptorSetLayout implements VkHandleWrapper {

    protected final long descriptorSet;
    protected final LogicalDevice device;

    protected final int type;

    protected int count;

    protected DescriptorSetLayout(LogicalDevice device, int type, int binding, int stage) {
        this.type = type;
        this.device = device;
        this.count = 1;
        this.descriptorSet = this.buildDescriptorSetLayout(type, binding, stage);
    }


    public abstract long buildDescriptorSetLayout(int type, int binding, int stage);

    @Override
    public void cleanup() {
        Logger.debug("Destroying descriptor set layout");
        VK11.vkDestroyDescriptorSetLayout(this.device.asVk(), this.descriptorSet, null);
    }

    public DescriptorSetLayout setCount(int count) {
        this.count = count;
        return this;
    }

    public DescriptorSetLayout enablePerFrameWrites(SwapChain swapChain) {
        this.count = swapChain.getImageViews().length;
        return this;
    }

    @Override
    public final long getHandle() {
        return this.descriptorSet;
    }

    public int getCount() {
        return this.count;
    }

    public final int getDescriptorSetType() {
        return this.type;
    }

    public final LogicalDevice getDevice() {
        return this.device;
    }

    @Override
    public String toString() {
        return "[ DescriptorLayout :: type=%s, count=%s ]".formatted(this.type, this.count);
    }
}
