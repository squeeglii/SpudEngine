package me.cg360.spudengine.core.render.pipeline.util;

import org.lwjgl.vulkan.VK11;

public enum ShaderStage {

    NONE(0),
    VERTEX(VK11.VK_SHADER_STAGE_VERTEX_BIT),
    GEOMETRY(VK11.VK_SHADER_STAGE_GEOMETRY_BIT),
    FRAGMENT(VK11.VK_SHADER_STAGE_FRAGMENT_BIT);

    private final int bitMask;

    ShaderStage(int bitMask) {
        this.bitMask = bitMask;
    }

    public int bitMask() {
        return this.bitMask;
    }

}
