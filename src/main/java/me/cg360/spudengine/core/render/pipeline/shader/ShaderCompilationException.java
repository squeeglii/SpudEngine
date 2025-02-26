package me.cg360.spudengine.core.render.pipeline.shader;

public class ShaderCompilationException extends RuntimeException {

    public ShaderCompilationException(BinaryShaderFile shaderSource, int errCode, String message) {
        super("[%s] -> Err #%s: %s".formatted(
                ShaderCompilationException.getShaderName(shaderSource),
                errCode, message)
        );
    }


    private static String getShaderName(BinaryShaderFile shaderSource) {
        return shaderSource == null
                ? "<<built-in>>"
                : shaderSource.getSourcePath().getPath();
    }

}
