package me.cg360.spudengine.core.render.pipeline.descriptor;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.tinylog.Logger;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DescriptorPool implements VkHandleWrapper {

    private final LogicalDevice device;
    private final long descriptorPool;

    private DescriptorSetLayout[] descriptorSetLayouts;

    public DescriptorPool(LogicalDevice device, DescriptorSetLayout... layout) {
        this(device, DescriptorPool.tallyTypes(layout));
        this.descriptorSetLayouts = layout;
    }

    public DescriptorPool(LogicalDevice device, Map<Integer, Integer> descriptorTypeTally) {
        Logger.debug("Creating descriptor pool");
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int maxSets = 0;
            int numTypes = descriptorTypeTally.size();
            VkDescriptorPoolSize.Buffer typeCounts = VkDescriptorPoolSize.calloc(numTypes, stack);

            int i = 0;
            for (Map.Entry<Integer, Integer> entry: descriptorTypeTally.entrySet()) {
                maxSets += entry.getValue();
                typeCounts.get(i++)
                          .type(entry.getKey())
                          .descriptorCount(entry.getValue());
            }

            Logger.debug("DescriptorPool Type Tally: {}", descriptorTypeTally);
            Logger.debug("Max Sets: {}  Types: {}", maxSets, numTypes);

            VkDescriptorPoolCreateInfo descriptorPoolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .flags(VK11.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .pPoolSizes(typeCounts)
                    .maxSets(maxSets);

            LongBuffer pDescriptorPool = stack.mallocLong(1);
            int errCode = VK11.vkCreateDescriptorPool(device.asVk(), descriptorPoolInfo, null, pDescriptorPool);
            VulkanUtil.checkErrorCode(errCode, "Failed to create descriptor pool");
            this.descriptorPool = pDescriptorPool.get(0);
        }
    }

    private static Map<Integer, Integer> tallyTypes(DescriptorSetLayout... layoutElements) {
        Map<Integer, Integer> tally = new HashMap<>();

        for(DescriptorSetLayout layout: layoutElements) {
            int t = layout.getDescriptorSetType();
            int lastVal = tally.getOrDefault(t, 0);
            tally.put(t, lastVal + layout.getCount());
        }

        return tally;
    }

    public void freeDescriptorSet(long vkDescriptorSet) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer longBuffer = stack.mallocLong(1);
            longBuffer.put(0, vkDescriptorSet);

            int errFree = VK11.vkFreeDescriptorSets(device.asVk(), this.descriptorPool, longBuffer);
            VulkanUtil.checkErrorCode(errFree, "Failed to free descriptor set");

            // TODO: Do the sets need clearing up here.
        }
    }

    @Override
    public void cleanup() {
        Logger.debug("Cleaning up descriptor pool");
        VK11.vkDestroyDescriptorPool(this.device.asVk(), this.descriptorPool, null);

        Arrays.stream(this.descriptorSetLayouts).forEach(DescriptorSetLayout::cleanup);
    }

    @Override
    public final long getHandle() {
        return this.descriptorPool;
    }

    public final LogicalDevice getDevice() {
        return this.device;
    }

    public DescriptorSetLayout[] getDescriptorSetLayouts() {
        return this.descriptorSetLayouts;
    }
}
