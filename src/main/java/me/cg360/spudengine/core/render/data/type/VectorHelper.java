package me.cg360.spudengine.core.render.data.type;

import me.cg360.spudengine.core.render.data.BufferAllocator;
import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.ByteBuffer;

public class VectorHelper extends TypeHelper {

    private final int elementSizeInBytes;
    private final int elementCount;
    private final int count;

    // TODO: Uniform lists in some GPUs enforce minimum limits on per-uniform size.

    private VectorHelper(int elementSize, int elementCount, int vecCount, BufferAllocator allocator) {
        super(elementSize*elementCount*vecCount, allocator);
        this.elementSizeInBytes = elementSize;
        this.elementCount = elementCount;
        this.count = vecCount;
    }

    public VectorHelper(int elementSize, int elementCount, BufferAllocator allocator) {
        this(elementSize, elementCount, 1, allocator);
    }

    @Override
    public VectorHelper asList(int count) {
        return new VectorHelper(
                this.elementSizeInBytes,
                this.elementCount,
                this.count*count,
                this.allocator
        );
    }

    public void copy4fToBuffer(GeneralBuffer buffer, Vector4f... vectors) {
        this.copy4fToBuffer(buffer, 0, vectors);
    }

    public void copy4fToBuffer(GeneralBuffer buffer, int offset, Vector4f... vectors) {
        long mappedMemory = buffer.map();
        ByteBuffer vecBuffer = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());

        vecBuffer.position(offset);

        for(int i = 0; i < vectors.length; i++) {
            Vector4f vec = vectors[i];

            vecBuffer.putFloat(vec.x());
            vecBuffer.putFloat(vec.y());
            vecBuffer.putFloat(vec.z());
            vecBuffer.putFloat(vec.w());
        }

        buffer.unmap();
    }

}
