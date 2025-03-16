package me.cg360.spudengine.core.render.pipeline.shader;

import java.nio.file.Path;

/** Points to a SPIR-V file. */
public record BinaryShaderFile(ShaderType type, String path) {

    public String getSourcePath() {
        String preSlash = this.path.startsWith("/") || this.path.startsWith("\\")
                ? ""
                : "/";

        return preSlash + this.path + ".glsl";
    }

    public Path getCompiledPath() {
        return Path.of("compiled", this.path + ".spv");
    }

}
