package me.cg360.spudengine.core.util;

import org.joml.Vector3f;

import java.util.Arrays;

public class Vectors {

    public static Vector3f formatRadians(Vector3f vec) {
        vec.x = (float) Math.toRadians(vec.x);
        vec.y = (float) Math.toRadians(vec.y);
        vec.z = (float) Math.toRadians(vec.z);
        return vec;
    }

    public static Vector3f toRadians(Vector3f vec) {
        return new Vector3f(
                (float) Math.toRadians(vec.x),
                (float) Math.toRadians(vec.y),
                (float) Math.toRadians(vec.z)
        );
    }

    public static Vector3f toRadians(float x, float y, float z) {
        return new Vector3f(
                (float) Math.toRadians(x),
                (float) Math.toRadians(y),
                (float) Math.toRadians(z)
        );
    }

    public static float[] scaleAll(float[] floats, float scale) {
        float[] result = new float[floats.length];

        for (int i = 0; i < floats.length; i++)
            result[i] = floats[i] * scale;

        return result;
    }

}
