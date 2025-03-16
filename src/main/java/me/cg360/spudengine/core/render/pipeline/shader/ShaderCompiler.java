package me.cg360.spudengine.core.render.pipeline.shader;

import org.lwjgl.util.shaderc.Shaderc;
import org.tinylog.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ShaderCompiler {

    public static boolean compileShaderIfChanged(BinaryShaderFile shaderFile) {
        byte[] compiledShader;
        String shaderSrc = shaderFile.getSourcePath();

        // todo: reimplement file timestamp checking.
        // move shaders out of resources?

        try(InputStream shaderIn = ShaderCompiler.class.getResourceAsStream(shaderSrc)) {
            File spvFile = new File(shaderFile.getCompiledPath().toUri());

            if(shaderIn == null)
                throw new FileNotFoundException("Cannot find internal shader source '%s'".formatted(shaderSrc));


            Logger.info("Compiling shader [{}] to [{}]", shaderSrc, spvFile.getPath());

            BufferedInputStream read = new BufferedInputStream(shaderIn);
            byte[] srcBytes = read.readAllBytes();
            String srcString = new String(srcBytes); // encoding? um

            compiledShader = ShaderCompiler.compileShader(shaderFile, srcString, shaderFile.type().shaderc());
            Path target = spvFile.toPath();
            target.getParent().toFile().mkdirs();
            Files.write(target, compiledShader, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;

            /*
            if (!spvFile.exists() || glslFile.lastModified() > spvFile.lastModified()) {


            } else {
                Logger.debug("Shader [{}] already compiled. Loading compiled version: [{}]", glslFile.getPath(), spvFile.getPath());
                return false;
            }
             */

        } catch (IOException err) {
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
