package me.cg360.spudengine.core.render.pipeline.shader;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.util.Arrays;

public class ShaderProgram {

    private final LogicalDevice device;
    private final Shader[] shaderModules;

    public ShaderProgram(LogicalDevice device, BinaryShaderFile... sources) {
        try {
            this.device = device;
            int numModules = sources != null ? sources.length : 0;
            this.shaderModules = new Shader[numModules];

            for (int i = 0; i < numModules; i++) {
                byte[] moduleContents = Files.readAllBytes(sources[i].getCompiledPath());
                long moduleHandle = this.parse(moduleContents);
                this.shaderModules[i] = new Shader(sources[i].type().vulkan(), moduleHandle);
            }

        } catch (IOException err) {
            Logger.error("Error reading shader files", err);
            throw new RuntimeException(err);
        }
    }

    private long parse(byte[] code) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pCode = stack.malloc(code.length).put(0, code);

            VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(pCode);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = VK11.vkCreateShaderModule(this.device.asVk(), moduleCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create shader module");

            return lp.get(0);
        }
    }

    public void cleanup() {
        for (Shader shaderModule : this.shaderModules)
            VK11.vkDestroyShaderModule(this.device.asVk(), shaderModule.handle(), null);
    }

    public Shader[] getShaderModules() {
        return this.shaderModules;
    }

    public static ShaderProgram attemptCompile(LogicalDevice graphicsDevice, BinaryShaderFile... shaders) {
        if (EngineProperties.SHOULD_RECOMPILE_SHADERS) {
            long recompiles = Arrays.stream(shaders).filter(ShaderCompiler::compileShaderIfChanged).count();
            Logger.info("Recompiled {} shader(s).", recompiles);
        }

        return new ShaderProgram(graphicsDevice, shaders);
    }

}
