package me.cg360.spudengine.render.geometry;

import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.vulkan.VK11;

public record Attribute(int binding, int format, int size) {

    public static final AttributeFormat VEC3F = binding -> new Attribute(binding, VK11.VK_FORMAT_R32G32B32_SFLOAT, VulkanUtil.FLOAT_BYTES*3);

}
