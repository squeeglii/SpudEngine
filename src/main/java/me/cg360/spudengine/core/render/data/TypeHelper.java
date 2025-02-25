package me.cg360.spudengine.core.render.data;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;

public class TypeHelper {

    protected final int size;
    protected final BufferAllocator allocator;

    public TypeHelper(int sizeInBytes, BufferAllocator allocator) {
        this.size = sizeInBytes;
        this.allocator = allocator;
    }

    public GeneralBuffer allocateList(LogicalDevice device, int count, int bufferType, int requirementMask) {
        return this.allocator.allocateList(device, this, bufferType, count, requirementMask);
    }

    public GeneralBuffer allocateOne(LogicalDevice device, int bufferType, int requirementMask) {
        return this.allocator.allocateList(device, this, 1, bufferType, requirementMask);
    }

    public int size() {
        return this.size;
    }
}
