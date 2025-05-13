package me.cg360.spudengine.wormholes.render;

import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.wormholes.world.entity.PortalEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class PortalTransformHelper extends TypeHelper {

    //TODO: Fix alignment of vec3s in shaders / use better wording than just dropping vec4's
    //      Shader handled uniforms in minimum of 16 byte chunks on this device.
    //      Get the val from the device, and align based off of that.

    private static final int SIZE =
            DataTypes.VEC4F.size() +      // pos        vec3 but it needs a padding 0 because alignment.
            DataTypes.VEC4F.size() +      // up         vec3 but it needs a padding 0 because alignment.
            DataTypes.VEC4F.size() + // rotation
            DataTypes.MAT4X4F.size();     // room transform.

    public PortalTransformHelper() {
        super(SIZE, TypeHelper.SIMPLE_BUFFER);
    }

    public static void copyToBuffer(GeneralBuffer buffer, PortalEntity entity, Matrix4f connectionTransform) {
        Vector3f pos = entity.getPosition();
        Vector3f up = entity.getUp();
        Vector3f normal = entity.getNormal();

        long mappedMemory = buffer.map();
        ByteBuffer out = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());

        PortalTransformHelper.writeAlignedVec3f(out, pos, 0);
        PortalTransformHelper.writeAlignedVec3f(out, up, 0);
        PortalTransformHelper.writeAlignedVec3f(out, normal, 0);
        connectionTransform.get(out); // this actually writes to the buffer. confusing naming

        buffer.unmap();
    }

    public static void copyIncompleteToBuffer(GeneralBuffer buffer, PortalEntity entity) {
        Vector3f pos = entity.getPosition();
        Vector3f up = entity.getUp();
        Vector3f normal = entity.getNormal();

        long mappedMemory = buffer.map();
        ByteBuffer out = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());

        PortalTransformHelper.writeAlignedVec3f(out, pos, 0);
        PortalTransformHelper.writeAlignedVec3f(out, up, 0);
        PortalTransformHelper.writeAlignedVec3f(out, normal, 0);
        new Matrix4f().get(out);  // identity as transform.

        buffer.unmap();
    }

    public static void setAsMissingToBuffer(GeneralBuffer buffer) {
        long mappedMemory = buffer.map();
        ByteBuffer out = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());

        Vector3f zero = new Vector3f();

        PortalTransformHelper.writeAlignedVec3f(out, zero, 0);
        PortalTransformHelper.writeAlignedVec3f(out, zero, 0);
        PortalTransformHelper.writeAlignedVec3f(out, zero, 0);
        new Matrix4f().zero().get(out);  // zero as transform.

        buffer.unmap();
    }

    private static void writeAlignedVec3f(ByteBuffer buffer, Vector3f vec, int padding) {
        buffer.putFloat(vec.x).putFloat(vec.y).putFloat(vec.z);
        buffer.putFloat(padding); // alignment.
    }

    private static void writeQuaternion(ByteBuffer buffer, Quaternionf vec) {
        buffer.putFloat(vec.x).putFloat(vec.y).putFloat(vec.z).putFloat(vec.w);
    }

}
