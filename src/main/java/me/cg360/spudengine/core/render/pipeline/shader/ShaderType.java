package me.cg360.spudengine.core.render.pipeline.shader;

import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK11;

public enum ShaderType {

    VERTEX(Shaderc.shaderc_glsl_vertex_shader, VK11.VK_SHADER_STAGE_VERTEX_BIT),
    FRAGMENT(Shaderc.shaderc_glsl_fragment_shader, VK11.VK_SHADER_STAGE_FRAGMENT_BIT),;

    private int shaderc;
    private int vulkan;

    ShaderType(int shaderc, int vulkan) {
        this.shaderc = shaderc;
        this.vulkan = vulkan;
    }

    public int shaderc() {
        return this.shaderc;
    }

    public int vulkan() {
        return this.vulkan;
    }
}
