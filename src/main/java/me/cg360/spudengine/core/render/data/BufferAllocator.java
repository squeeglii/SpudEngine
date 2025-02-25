package me.cg360.spudengine.core.render.data;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;

@FunctionalInterface
public interface BufferAllocator {

    GeneralBuffer allocateList(LogicalDevice device, TypeHelper type, int count, int bufferType, int requirementMask);

}
