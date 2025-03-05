package me.cg360.spudengine.core.render.data.type;

import me.cg360.spudengine.core.render.data.BufferAllocator;
import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class MatrixHelper extends TypeHelper {

    private final int perMatrixSize;

    public MatrixHelper(int sizeInBytes, BufferAllocator allocator) {
        super(sizeInBytes, allocator);
        this.perMatrixSize = sizeInBytes;
    }

    public MatrixHelper(int sizeInBytes, int perMatrixSize, BufferAllocator allocator) {
        super(sizeInBytes, allocator); // total size. count * per matrix
        this.perMatrixSize = perMatrixSize;
    }

    @Override
    public MatrixHelper asList(int count) {
        return new MatrixHelper(
                this.size * count,
                this.perMatrixSize,
                this.allocator
        );
    }

    public void copyToBuffer(GeneralBuffer buffer, Matrix4f... mArray) {
        this.copyToBuffer(buffer, 0, mArray);
    }

    public void copyToBuffer(GeneralBuffer buffer, int offset, Matrix4f... mArray) {
        long mappedMemory = buffer.map();
        ByteBuffer matrixBuffer = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());

        for(int i = 0; i < mArray.length; i++) {
            Matrix4f mat = mArray[i];
            int pos = offset + (i * this.perMatrixSize);
            mat.get(pos, matrixBuffer);
        }

        buffer.unmap();
    }

}
