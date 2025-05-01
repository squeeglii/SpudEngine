package me.cg360.spudengine.core.util;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.hardware.PhysicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.awt.*;
import java.util.Arrays;

public class VulkanUtil {

    public static final Runnable NO_ACTION = () -> {};

    public static final int MESSAGE_SEVERITY_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;

    public static final int MESSAGE_TYPE_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;


    public static final int FLOAT_BYTES = 4;
    public static final int INT_BYTES = 4;

    /** */
    public static VkDebugUtilsMessengerCreateInfoEXT createDebugCallback() {
        return VkDebugUtilsMessengerCreateInfoEXT
                .calloc()
                .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(MESSAGE_SEVERITY_BITMASK)
                .messageType(MESSAGE_TYPE_BITMASK)
                .pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                    VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

                    // convert messageSeverity to tinylogger severity.
                    if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                        Logger.tag("Vulkan").info(callbackData.pMessageString());
                    } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                        Logger.tag("Vulkan").warn(callbackData.pMessageString());
                    } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                        Logger.tag("Vulkan").error(callbackData.pMessageString());
                    } else {
                        Logger.tag("Vulkan").debug(callbackData.pMessageString());
                    }

                    return VK11.VK_FALSE;
                });
    }

    public static void checkErrorCode(int errorCode, String errMsg, Object... vars) {
        VulkanUtil.checkErrorCode(errorCode, NO_ACTION, errMsg, vars);
    }

    public static void checkErrorCode(int errorCode, Runnable debugDump, String errMsg, Object... vars) {
        if (errorCode == VK11.VK_SUCCESS) return;

        debugDump.run();

        String translation = switch (errorCode) {
            case -1000069000 -> "VK_ERROR_OUT_OF_POOL_MEMORY";
            default -> "UNKNOWN_CODE";
        };

        throw new RuntimeException("VkError [%s|%s]: %s".formatted(errorCode, translation, errMsg.formatted(vars)));
    }

    public static int memoryTypeFromProperties(PhysicalDevice physDevice, int typeBits, int reqsMask) {
        int result = -1;
        VkMemoryType.Buffer memoryTypes = physDevice.getMemoryProperties().memoryTypes();

        for (int i = 0; i < VK11.VK_MAX_MEMORY_TYPES; i++) {
            if ((typeBits & 1) == 1 && (memoryTypes.get(i).propertyFlags() & reqsMask) == reqsMask) {
                result = i;
                break;
            }

            typeBits >>= 1;
        }

        if (result < 0)
            throw new RuntimeException("Failed to find memoryType");

        return result;
    }

    public static void cleanupAll(VkHandleWrapper[] vkObjects) {
        Arrays.stream(vkObjects).forEach(VkHandleWrapper::cleanup);
    }

    public static VkClearValue.Buffer generateClearValues(MemoryStack stack) {
        VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
        Color c = EngineProperties.CLEAR_COLOUR;
        float[] components = new float[4];
        c.getComponents(components);
        clearValues.apply(0, v -> v.color()
                .float32(0, components[0])
                .float32(1, components[1])
                .float32(2, components[2])
                .float32(3, components[3])
        );
        clearValues.apply(1, v -> v.depthStencil()
                .depth(1.0f)
                .stencil(0)
        );
        return clearValues;
    }

    public static void setupStandardViewport(VkCommandBuffer cmd, MemoryStack stack, int width, int height) {
        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0)
                .y(height)
                .height(-height)         // flip viewport - opengl's coordinate system is nicer.
                .width(width)
                .minDepth(0.0f)
                .maxDepth(1.0f);
        VK11.vkCmdSetViewport(cmd, 0, viewport);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .extent(it -> it
                        .width(width)
                        .height(height))
                .offset(it -> it
                        .x(0)
                        .y(0));
        VK11.vkCmdSetScissor(cmd, 0, scissor);
    }

}
