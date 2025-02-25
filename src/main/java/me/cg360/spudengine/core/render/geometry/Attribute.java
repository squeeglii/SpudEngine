package me.cg360.spudengine.core.render.geometry;

import me.cg360.spudengine.core.render.data.TypeHelper;
import me.cg360.spudengine.core.render.data.DataTypes;
import org.lwjgl.vulkan.VK11;

public record Attribute(int binding, int format, int size) {

    public Attribute(int binding, int format, TypeHelper type) {
        this(binding, format, type.size());
    }

    public static final AttributeFormat VEC3F = binding -> new Attribute(binding, VK11.VK_FORMAT_R32G32B32_SFLOAT, DataTypes.VEC3F);
    public static final AttributeFormat VEC2F = binding -> new Attribute(binding, VK11.VK_FORMAT_R32G32_SFLOAT, DataTypes.VEC2F);

    public static final AttributeFormat INT = binding -> new Attribute(binding, VK11.VK_FORMAT_R32_SINT, DataTypes.INT);
    public static final AttributeFormat FLOAT = binding -> new Attribute(binding, VK11.VK_FORMAT_R32_SFLOAT, DataTypes.FLOAT);


}
