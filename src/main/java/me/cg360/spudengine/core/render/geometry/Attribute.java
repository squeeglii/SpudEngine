package me.cg360.spudengine.core.render.geometry;

import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.vulkan.VK11;

public record Attribute(int binding, int format, int size) {

    public static final AttributeFormat VEC3F = binding -> new Attribute(binding, VK11.VK_FORMAT_R32G32B32_SFLOAT, VulkanUtil.FLOAT_BYTES*3);
    public static final AttributeFormat VEC2F = binding -> new Attribute(binding, VK11.VK_FORMAT_R32G32_SFLOAT, VulkanUtil.FLOAT_BYTES*2);

    public static final AttributeFormat INT = binding -> new Attribute(binding, VK11.VK_FORMAT_R32_SINT, VulkanUtil.INT_BYTES);
    public static final AttributeFormat FLOAT = binding -> new Attribute(binding, VK11.VK_FORMAT_R32_SFLOAT, VulkanUtil.FLOAT_BYTES);
}
