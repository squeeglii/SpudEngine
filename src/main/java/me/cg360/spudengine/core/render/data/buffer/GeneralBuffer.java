package me.cg360.spudengine.core.render.data.buffer;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

public class GeneralBuffer implements VkHandleWrapper {

    private final LogicalDevice device;
    private final PointerBuffer pointerBuffer;
    private final long requestedSize;
    private final long allocationSize;

    private long mappedMemory;

    private final long buffer;
    private final long memory;

    public GeneralBuffer(LogicalDevice device, long size, int usage, int reqMask) {
        this.device = device;
        this.requestedSize = size;
        this.mappedMemory = MemoryUtil.NULL;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK11.VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateBuffer(device.asVk(), bufferCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create general buffer (usage: %s)", usage);

            this.buffer = lp.get(0);

            // Buffer handle is created, but memory needs to be allocated too:
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            VK11.vkGetBufferMemoryRequirements(device.asVk(), this.buffer, memReqs);
            int memType = VulkanUtil.memoryTypeFromProperties(device.getPhysicalDevice(), memReqs.memoryTypeBits(), reqMask);

            VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(memType);

            int errAllocate = VK11.vkAllocateMemory(device.asVk(), memAlloc, null, lp);
            VulkanUtil.checkErrorCode(errAllocate, "Failed to allocate memory");

            this.allocationSize = memAlloc.allocationSize();
            this.memory = lp.get(0);
            this.pointerBuffer = MemoryUtil.memAllocPointer(1);

            int errBind = VK11.vkBindBufferMemory(device.asVk(), this.buffer, this.memory, 0);
            VulkanUtil.checkErrorCode(errBind, "Failed to bind buffer memory");
        }
    }

    public long map() {
        if (this.mappedMemory == MemoryUtil.NULL) {
            int errMap = VK11.vkMapMemory(this.device.asVk(), this.memory, 0, this.allocationSize, 0, this.pointerBuffer);
            VulkanUtil.checkErrorCode(errMap, "Failed to map Buffer");
            this.mappedMemory = this.pointerBuffer.get(0);
        }

        return this.mappedMemory;
    }

    public void unmap() {
        if (this.mappedMemory != MemoryUtil.NULL) {
            VK11.vkUnmapMemory(this.device.asVk(), this.memory);
            this.mappedMemory = MemoryUtil.NULL;
        }
    }

    public void cleanup() {
        MemoryUtil.memFree(this.pointerBuffer);
        VK11.vkDestroyBuffer(this.device.asVk(), this.buffer, null);
        VK11.vkFreeMemory(this.device.asVk(), this.memory, null);
    }

    public long getHandle() {
        return this.buffer;
    }

    public long getRequestedSize() {
        return this.requestedSize;
    }

}
