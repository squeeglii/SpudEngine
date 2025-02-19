package me.cg360.spudengine.core.util;

import me.cg360.spudengine.core.render.hardware.PhysicalDevice;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

public class VulkanUtil {

    public static final int MESSAGE_SEVERITY_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;

    public static final int MESSAGE_TYPE_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;


    public static final int FLOAT_BYTES = 4;
    public static final int INT_BYTES = 4;
    public static final int VEC2F_BYTES = 2 * FLOAT_BYTES;
    public static final int VEC3F_BYTES = 3 * FLOAT_BYTES;
    public static final int MAT4X4F = 16 * FLOAT_BYTES;

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
        if (errorCode != VK11.VK_SUCCESS)
            throw new RuntimeException("VkError [%s]: %s".formatted(errorCode, errMsg.formatted(vars)));
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

}
