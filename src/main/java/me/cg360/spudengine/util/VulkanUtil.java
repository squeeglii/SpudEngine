package me.cg360.spudengine.util;

import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.tinylog.Logger;

public class VulkanUtil {

    public static final int MESSAGE_SEVERITY_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;

    public static final int MESSAGE_TYPE_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;


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

    public static void checkErrorCode(int errorCode, String errMsg) {
        if (errorCode != VK11.VK_SUCCESS)
            throw new RuntimeException("VkError [%s]: %s".formatted(errorCode, errMsg));
    }

}
