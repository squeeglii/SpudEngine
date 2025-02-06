package me.cg360.spudengine.render.hardware;

import me.cg360.spudengine.render.VulkanInstance;
import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/** Wrapper for VkPhysicalDevice that makes it easier to work with. */
public class PhysicalDevice {

    private final VkPhysicalDevice physicalDevice;
    private final VkPhysicalDeviceProperties properties;
    private final VkPhysicalDeviceFeatures features;
    private final VkExtensionProperties.Buffer extensions;
    private final VkPhysicalDeviceMemoryProperties memoryProperties;
    private final VkQueueFamilyProperties.Buffer queueFamilyProperties;

    public PhysicalDevice(VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.physicalDevice = vkPhysicalDevice;

            IntBuffer intBuffer = stack.mallocInt(1);

            // Get device properties
           this.properties = VkPhysicalDeviceProperties.calloc();
            VK11.vkGetPhysicalDeviceProperties(vkPhysicalDevice, this.properties);

            // Get device extensions
            int errCountExtensions = VK11.vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String) null, intBuffer, null);
            VulkanUtil.checkErrorCode(errCountExtensions, "Failed to get number of device extension properties");
            this.extensions = VkExtensionProperties.calloc(intBuffer.get(0));

            int errGetExtensions = VK11.vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String) null, intBuffer, this.extensions);
            VulkanUtil.checkErrorCode(errGetExtensions, "Failed to get extension properties");

            // Get Queue family properties
            VK11.vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, intBuffer, null);
            this.queueFamilyProperties = VkQueueFamilyProperties.calloc(intBuffer.get(0));
            VK11.vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, intBuffer, this.queueFamilyProperties);

            this.features = VkPhysicalDeviceFeatures.calloc();
            VK11.vkGetPhysicalDeviceFeatures(vkPhysicalDevice, this.features);

            // Get Memory information and properties
            this.memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
            VK11.vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, this.memoryProperties);
        }
    }


    public void cleanup() {
        if (Logger.isDebugEnabled()) {
            Logger.debug("Destroying physical device [{}]", this.getDeviceName());
        }

        this.memoryProperties.free();
        this.features.free();
        this.queueFamilyProperties.free();
        this.extensions.free();
        this.properties.free();
    }

    private boolean hasKHRSwapChainExtension() {
        boolean result = false;
        int numExtensions = this.extensions != null ? this.extensions.capacity() : 0;
        for (int i = 0; i < numExtensions; i++) {
            String extensionName = this.extensions.get(i).extensionNameString();
            if (KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(extensionName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean hasGraphicsQueueFamily() {
        boolean result = false;
        int numQueueFamilies = this.queueFamilyProperties != null
                               ? this.queueFamilyProperties.capacity()
                               : 0;

        // Loop through every queue, until one that supports graphics is found.
        for (int i = 0; i < numQueueFamilies; i++) {
            VkQueueFamilyProperties familyProps = this.queueFamilyProperties.get(i);
            if ((familyProps.queueFlags() & VK11.VK_QUEUE_GRAPHICS_BIT) != 0) {
                result = true;
                break;
            }
        }

        return result;
    }

    public String getDeviceName() {
        return this.properties.deviceNameString();
    }

    public VkPhysicalDevice asVk() {
        return this.physicalDevice;
    }

    public VkPhysicalDeviceProperties getProperties() {
        return this.properties;
    }

    public VkPhysicalDeviceFeatures getFeatures() {
        return this.features;
    }

    public VkPhysicalDeviceMemoryProperties getMemoryProperties() {
        return this.memoryProperties;
    }

    public VkQueueFamilyProperties.Buffer getQueueFamilyProperties() {
        return this.queueFamilyProperties;
    }


    public static PhysicalDevice createPhysicalDevice(VulkanInstance instance, String preferredDeviceName) {
        PhysicalDevice selected = null;

        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pPhysicalDevices = getPhysicalDevices(instance, stack);
            int numDevices = pPhysicalDevices != null ? pPhysicalDevices.capacity() : 0;
            if (numDevices <= 0)
                throw new RuntimeException("No graphics devices found");

            // Populate available devices
            List<PhysicalDevice> devices = new ArrayList<>(numDevices);
            for (int i = 0; i < numDevices; i++) {
                VkPhysicalDevice vkPhysicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), instance.getVulkanInstance());
                PhysicalDevice physicalDevice = new PhysicalDevice(vkPhysicalDevice);
                String deviceName = physicalDevice.getDeviceName();

                if (physicalDevice.hasGraphicsQueueFamily() && physicalDevice.hasKHRSwapChainExtension()) {
                    Logger.debug("Device [{}] supports required extensions", deviceName);
                    if (preferredDeviceName != null && preferredDeviceName.equals(deviceName)) {
                        selected = physicalDevice;
                        break;
                    }

                    devices.add(physicalDevice);
                } else {
                    Logger.debug("Device [{}] does not support required extensions", deviceName);
                    physicalDevice.cleanup();
                }
            }

            // No preferred device or it does not meet requirements, just pick the first one
            selected = selected == null && !devices.isEmpty()
                    ? devices.removeFirst()
                    : selected;

            // Clean up non-selected devices
            for (PhysicalDevice physicalDevice : devices)
                physicalDevice.cleanup();

            if (selected == null)
                throw new RuntimeException("No suitable physical devices found");

            Logger.debug("Selected device: [{}]", selected.getDeviceName());
        }

        return selected;
    }


    protected static PointerBuffer getPhysicalDevices(VulkanInstance instance, MemoryStack stack) {
        // Get number of physical devices
        IntBuffer intBuffer = stack.mallocInt(1);
        int errCodeCountDevices = VK11.vkEnumeratePhysicalDevices(instance.getVulkanInstance(), intBuffer, null);
        VulkanUtil.checkErrorCode(errCodeCountDevices, "Failed to get number of physical devices");

        int numDevices = intBuffer.get(0);
        Logger.debug("Detected {} physical device(s)", numDevices);

        // Populate physical devices list pointer
        PointerBuffer pPhysicalDevices = stack.mallocPointer(numDevices);
        int errCodeGetDevices = VK11.vkEnumeratePhysicalDevices(instance.getVulkanInstance(), intBuffer, pPhysicalDevices);
        VulkanUtil.checkErrorCode(errCodeGetDevices, "Failed to get physical devices");

        return pPhysicalDevices;
    }
}
