package me.cg360.spudengine.core.render.hardware;

import me.cg360.spudengine.core.util.OSType;
import me.cg360.spudengine.core.util.Util;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public class LogicalDevice {

    private final PhysicalDevice physicalDevice;
    private final VkDevice logicalDevice;


    public LogicalDevice(PhysicalDevice physicalDevice) {
        Logger.debug("Creating logical device");
        this.physicalDevice = physicalDevice;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Define required extensions
            Set<String> deviceExtensions = this.getDeviceExtensions();
            boolean usePortability = deviceExtensions.contains(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME) &&
                                     Util.getOS() == OSType.MACOS;
            int numExtensions = usePortability ? 2 : 1;

            PointerBuffer requiredExtensions = stack.mallocPointer(numExtensions);
            requiredExtensions.put(stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));

            if (usePortability)
                requiredExtensions.put(stack.ASCII(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));

            requiredExtensions.flip();

            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);

            // Enable all the queue families
            VkQueueFamilyProperties.Buffer queuePropsBuff = physicalDevice.getQueueFamilyProperties();
            int numQueuesFamilies = queuePropsBuff.capacity();
            VkDeviceQueueCreateInfo.Buffer queueCreationInfoBuf = VkDeviceQueueCreateInfo.calloc(numQueuesFamilies, stack);

            for (int i = 0; i < numQueuesFamilies; i++) {
                FloatBuffer priorities = stack.callocFloat(queuePropsBuff.get(i).queueCount()); // All queue priorities are zero'd
                queueCreationInfoBuf.get(i)
                        .sType(VK11.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(i)
                        .pQueuePriorities(priorities);
            }

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .ppEnabledExtensionNames(requiredExtensions)
                    .pEnabledFeatures(features)
                    .pQueueCreateInfos(queueCreationInfoBuf);

            PointerBuffer pp = stack.mallocPointer(1);
            int errCreateDevice = VK11.vkCreateDevice(physicalDevice.asVk(), deviceCreateInfo, null, pp);
            VulkanUtil.checkErrorCode(errCreateDevice, "Failed to create device");

            this.logicalDevice = new VkDevice(pp.get(0), physicalDevice.asVk(), deviceCreateInfo);
        }
    }


    public void cleanup() {
        Logger.debug("Destroying Vulkan device");
        VK11.vkDestroyDevice(this.logicalDevice, null);
    }

    public void waitIdle() {
        VK11.vkDeviceWaitIdle(this.logicalDevice);
    }


    private Set<String> getDeviceExtensions() {
        Set<String> deviceExtensions = new HashSet<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numExtensionsBuf = stack.callocInt(1);
            VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice.asVk(), (String) null, numExtensionsBuf, null);
            int numExtensions = numExtensionsBuf.get(0);
            Logger.debug("Device supports [{}] extensions", numExtensions);

            VkExtensionProperties.Buffer propsBuff = VkExtensionProperties.calloc(numExtensions, stack);
            VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice.asVk(), (String) null, numExtensionsBuf, propsBuff);
            for (int i = 0; i < numExtensions; i++) {
                VkExtensionProperties props = propsBuff.get(i);
                String extensionName = props.extensionNameString();
                deviceExtensions.add(extensionName);
                Logger.debug("Supported device extension [{}]", extensionName);
            }
        }
        return deviceExtensions;
    }

    public PhysicalDevice getPhysicalDevice() {
        return this.physicalDevice;
    }

    public VkDevice asVk() {
        return this.logicalDevice;
    }
}
