package me.cg360.spudengine.core.render.data;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.data.type.MatrixHelper;

public class DataTypes {

    private static final BufferAllocator SIMPLE_BUFFER = (device, type, count, bufferType, requirementMask) ->
            new GeneralBuffer(device, (long) type.size() * count, bufferType, requirementMask);

    public static final TypeHelper FLOAT = new TypeHelper(4, SIMPLE_BUFFER);
    public static final TypeHelper INT = new TypeHelper(4, SIMPLE_BUFFER);

    public static final TypeHelper VEC2F = new TypeHelper(2 * FLOAT.size(), SIMPLE_BUFFER);
    public static final TypeHelper VEC3F = new TypeHelper(3 * FLOAT.size(), SIMPLE_BUFFER);

    public static final MatrixHelper MAT4X4F = new MatrixHelper(16 * FLOAT.size(), SIMPLE_BUFFER);


}
