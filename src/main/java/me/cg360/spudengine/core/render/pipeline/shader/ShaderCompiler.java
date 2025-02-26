package me.cg360.spudengine.core.render.pipeline.shader;

import org.lwjgl.util.shaderc.Shaderc;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ShaderCompiler {

    public static boolean compileShaderIfChanged(BinaryShaderFile shaderFile) {
        byte[] compiledShader;
        try {

            File glslFile = new File(shaderFile.getSourcePath().toURI());
            File spvFile = new File(shaderFile.getCompiledPath().toUri());

            if (!spvFile.exists() || glslFile.lastModified() > spvFile.lastModified()) {
                Logger.info("Compiling shader [{}] to [{}]", glslFile.getPath(), spvFile.getPath());
                String shaderCode = new String(Files.readAllBytes(glslFile.toPath()));

                compiledShader = ShaderCompiler.compileShader(shaderFile, shaderCode, shaderFile.type().shaderc());
                Path target = spvFile.toPath();
                target.getParent().toFile().mkdirs();
                Files.write(target, compiledShader, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return true;

            } else {
                Logger.debug("Shader [{}] already compiled. Loading compiled version: [{}]", glslFile.getPath(), spvFile.getPath());
                return false;
            }

        } catch (IOException | URISyntaxException err) {
            throw new RuntimeException(err);
        }
    }

    public static byte[] compileShader(BinaryShaderFile shaderSource, String shaderCode, int shaderType) {
        long compiler = 0;
        long options = 0;
        byte[] compiledShader;

        try {
            compiler = Shaderc.shaderc_compiler_initialize();
            options = Shaderc.shaderc_compile_options_initialize();

            long result = Shaderc.shaderc_compile_into_spv(
                    compiler, shaderCode, shaderType,
                    "shader.glsl", "main", options
            );

            int compileResult = Shaderc.shaderc_result_get_compilation_status(result);

            if (compileResult != Shaderc.shaderc_compilation_status_success) {
                String message = Shaderc.shaderc_result_get_error_message(result);
                throw new ShaderCompilationException(shaderSource, compileResult, message);
            }

            ByteBuffer buffer = Shaderc.shaderc_result_get_bytes(result);
            if(buffer == null)
                throw new IllegalStateException("Failed to buffer during shader compilation.");

            compiledShader = new byte[buffer.remaining()];
            buffer.get(compiledShader);

        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }

        return compiledShader;
    }

}
