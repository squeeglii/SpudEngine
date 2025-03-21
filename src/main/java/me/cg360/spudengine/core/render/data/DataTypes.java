package me.cg360.spudengine.core.render.data;

import me.cg360.spudengine.core.render.data.type.MatrixHelper;
import me.cg360.spudengine.core.render.data.type.VectorHelper;

import static me.cg360.spudengine.core.render.data.TypeHelper.SIMPLE_BUFFER;

public class DataTypes {

    public static final TypeHelper FLOAT = new TypeHelper(4, SIMPLE_BUFFER);
    public static final TypeHelper INT = new TypeHelper(4, SIMPLE_BUFFER);

    public static final VectorHelper VEC2F = new VectorHelper(FLOAT.size(), 2, SIMPLE_BUFFER);
    public static final VectorHelper VEC3F = new VectorHelper(FLOAT.size(), 3, SIMPLE_BUFFER);
    public static final VectorHelper VEC4F = new VectorHelper(FLOAT.size(), 4, SIMPLE_BUFFER);
    public static final VectorHelper QUATERNION = new VectorHelper(FLOAT.size(), 4, SIMPLE_BUFFER);

    public static final MatrixHelper MAT4X4F = new MatrixHelper(16 * FLOAT.size(), SIMPLE_BUFFER);


}
