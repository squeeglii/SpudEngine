package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.util.OSType;
import me.cg360.spudengine.core.util.Util;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VulkanInstance {

    private static final String PORTABILITY_EXTENSION = "VK_KHR_portability_enumeration";

    private final VkInstance instance;

    private long debugHandle;
    private VkDebugUtilsMessengerCreateInfoEXT debugUtils;

    public VulkanInstance(boolean debuggingEnabled) {
        Logger.info("Initializing Vulkan Instance (should enable debugging? {})", debuggingEnabled);

        try(MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer engineName = stack.UTF8(EngineProperties.ENGINE_NAME);
            ByteBuffer appName = stack.UTF8(EngineProperties.APP_NAME);

            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .apiVersion(VK11.VK_API_VERSION_1_1)
                    .pEngineName(engineName)
                    .engineVersion(EngineProperties.ENGINE_VERSION)
                    .pApplicationName(appName)
                    .applicationVersion(EngineProperties.APP_VERSION);

            // Collect validation layers to use
            List<String> validationLayers = this.getSupportedValidationLayers();
            int numValidationLayers = validationLayers.size();
            boolean usingDebugging = debuggingEnabled;

            if (debuggingEnabled && numValidationLayers == 0) {
                usingDebugging = false;
                Logger.warn("Debugging requested but no supported validation layers found. Disabling debugging.");
            }

            Logger.info("Using VK validation? {}", usingDebugging);

            // Set required layers
            PointerBuffer requiredLayers = null;
            if (usingDebugging) {
                requiredLayers = stack.mallocPointer(numValidationLayers);
                for (int i = 0; i < numValidationLayers; i++) {
                    Logger.debug("Using validation layer [{}]", validationLayers.get(i));
                    requiredLayers.put(i, stack.ASCII(validationLayers.get(i)));
                }
            }

            // Handle extensions.
            Set<String> instanceExtensions = getInstanceExtensions();

            // GLFW Extension
            PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null)
                throw new RuntimeException("Failed to find the GLFW platform surface extensions");

            // Debugging requires specific extensions on macos
            // Apply required extensions.
            PointerBuffer requiredExtensions;
            boolean usePortability = instanceExtensions.contains(PORTABILITY_EXTENSION) && Util.getOS() == OSType.MACOS;

            if (usingDebugging) {
                ByteBuffer vkDebugUtilsExtension = stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                int numExtensions = usePortability
                        ? glfwExtensions.remaining() + 2
                        : glfwExtensions.remaining() + 1;

                requiredExtensions = stack.mallocPointer(numExtensions);
                requiredExtensions.put(glfwExtensions).put(vkDebugUtilsExtension);

                if (usePortability) {
                    ByteBuffer ext = stack.UTF8(PORTABILITY_EXTENSION);
                    requiredExtensions.put(ext);
                }

            } else {
                int numExtensions = usePortability
                        ? glfwExtensions.remaining() + 1
                        : glfwExtensions.remaining();

                requiredExtensions = stack.mallocPointer(numExtensions);
                requiredExtensions.put(glfwExtensions);

                if (usePortability) {
                    ByteBuffer ext = stack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME);
                    requiredExtensions.put(ext);
                }
            }
            requiredExtensions.flip();

            long debugExtId = MemoryUtil.NULL;
            if(usingDebugging) {
                this.debugUtils = VulkanUtil.createDebugCallback();
                debugExtId = this.debugUtils.address();
            }

            // Create instance info
            VkInstanceCreateInfo instanceInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pNext(debugExtId)
                    .pApplicationInfo(appInfo)
                    .ppEnabledLayerNames(requiredLayers)
                    .ppEnabledExtensionNames(requiredExtensions);

            if (usePortability) {
                instanceInfo.flags(0x00000001); // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
            }

            PointerBuffer pInstance = stack.mallocPointer(1);
            int errCode = VK11.vkCreateInstance(instanceInfo, null, pInstance);
            VulkanUtil.checkErrorCode(errCode, "Error creating instance");
            this.instance = new VkInstance(pInstance.get(0), instanceInfo);

            this.debugHandle = VK11.VK_NULL_HANDLE;
            if (usingDebugging) {
                LongBuffer longBuff = stack.mallocLong(1);
                int errCodeDebug = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(this.instance, debugUtils, null, longBuff);
                VulkanUtil.checkErrorCode(errCodeDebug, "Error creating debug utils");
                this.debugHandle = longBuff.get(0);
            }
        }
    }

    public void cleanup() {
        Logger.debug("Destroying Vulkan instance");

        if (this.debugHandle != VK11.VK_NULL_HANDLE) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(this.instance, this.debugHandle, null);
        }

        if (this.debugUtils != null) {
            this.debugUtils.pfnUserCallback().free();
            this.debugUtils.free();
        }

        VK11.vkDestroyInstance(this.instance, null);
    }

    /**
     * Validation is used for debugging. See if the vulkan install present
     * supports validation, and provide the layers.
     */
    private List<String> getSupportedValidationLayers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            // -- Get amount of layers
            IntBuffer numLayersArr = stack.callocInt(1);
            VK11.vkEnumerateInstanceLayerProperties(numLayersArr, null);
            int numLayers = numLayersArr.get(0);
            Logger.debug("Instance supports [{}] layers", numLayers);

            // -- Get layer names.
            VkLayerProperties.Buffer propsBuf = VkLayerProperties.calloc(numLayers, stack);
            VK11.vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf);
            List<String> supportedLayers = new ArrayList<>(numLayers);

            for (int i = 0; i < numLayers; i++) {
                VkLayerProperties props = propsBuf.get(i);
                String layerName = props.layerNameString();
                supportedLayers.add(layerName);
            }
            Logger.debug("Supported layers: {}", supportedLayers);

            // -- Select validation layer to use.
            List<String> layersToUse = new ArrayList<>();

            // Main validation layer
            if (supportedLayers.contains("VK_LAYER_KHRONOS_validation")) {
                layersToUse.add("VK_LAYER_KHRONOS_validation");
                return layersToUse;
            }

            // Fallback if main fails.
            if (supportedLayers.contains("VK_LAYER_LUNARG_standard_validation")) {
                layersToUse.add("VK_LAYER_LUNARG_standard_validation");
                return layersToUse;
            }

            // Fallback if fallback fails.
            // Try to include as many of these as possible, where supported.
            List<String> requestedLayers = new ArrayList<>();
            requestedLayers.add("VK_LAYER_GOOGLE_threading");
            requestedLayers.add("VK_LAYER_LUNARG_parameter_validation");
            requestedLayers.add("VK_LAYER_LUNARG_object_tracker");
            requestedLayers.add("VK_LAYER_LUNARG_core_validation");
            requestedLayers.add("VK_LAYER_GOOGLE_unique_objects");
            return requestedLayers.stream().filter(supportedLayers::contains).toList();
        }
    }

    private Set<String> getInstanceExtensions() {
        Set<String> instanceExtensions = new HashSet<>();
        try (MemoryStack stack = MemoryStack.stackPush()) {

            // Count available extensions.
            IntBuffer numExtensionsBuf = stack.callocInt(1);
            VK11.vkEnumerateInstanceExtensionProperties((String) null, numExtensionsBuf, null);
            int numExtensions = numExtensionsBuf.get(0);
            Logger.debug("Vulkan Instance supports [{}] extensions", numExtensions);

            // Collect and list extensions.
            VkExtensionProperties.Buffer instanceExtensionsProps = VkExtensionProperties.calloc(numExtensions, stack);
            VK11.vkEnumerateInstanceExtensionProperties((String) null, numExtensionsBuf, instanceExtensionsProps);
            for (int i = 0; i < numExtensions; i++) {
                VkExtensionProperties props = instanceExtensionsProps.get(i);
                String extensionName = props.extensionNameString();
                instanceExtensions.add(extensionName);
            }

            Logger.debug("Supported vulkan instance extensions: {}", instanceExtensions);
        }

        return instanceExtensions;
    }

    public VkInstance asVk() {
        return this.instance;
    }
}
