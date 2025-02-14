package me.cg360.spudengine.core.render.pipeline.shader;

import java.net.URL;
import java.nio.file.Path;

/** Points to a SPIR-V file. */
public record BinaryShaderFile(ShaderType type, String path) {

    public URL getSourcePath() {
        return BinaryShaderFile.class.getClassLoader().getResource(this.path + ".glsl");
    }

    public Path getCompiledPath() {
        return Path.of("compiled", this.path + ".spv");
    }

}
