package me.cg360.spudengine.render;

import me.cg360.spudengine.render.hardware.LogicalDevice;
import me.cg360.spudengine.render.hardware.PhysicalDevice;
import me.cg360.spudengine.render.hardware.Surface;
import me.cg360.spudengine.render.hardware.SurfaceFormat;
import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class SwapChain {

    private final LogicalDevice logicalDevice;
    private final ImageView[] imageViews;
    private final SurfaceFormat surfaceFormat;
    private final VkExtent2D swapChainExtent;

    private final long swapchainHandle;


    public SwapChain(LogicalDevice device, Surface surface, Window window, int requestedImages, boolean vsync) {
        Logger.debug("Creating Vulkan SwapChain");
        this.logicalDevice = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {

            PhysicalDevice physicalDevice = device.getPhysicalDevice();

            // Get surface capabilities
            VkSurfaceCapabilitiesKHR surfCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            int errCode = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                            device.getPhysicalDevice().asVk(),
                            surface.asVk(),
                            surfCapabilities);
            VulkanUtil.checkErrorCode(errCode, "Failed to get surface capabilities");

            int numImages = SwapChain.calcNumImages(surfCapabilities, requestedImages);

            this.surfaceFormat = SwapChain.calcSurfaceFormat(physicalDevice, surface);
            this.swapChainExtent = SwapChain.calcSwapChainExtent(window, surfCapabilities);

            // Build SwapChain
            VkSwapchainCreateInfoKHR vkSwapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface.asVk())
                    .minImageCount(numImages)
                    .imageFormat(surfaceFormat.format())
                    .imageColorSpace(surfaceFormat.colourSpace())
                    .imageExtent(swapChainExtent)
                    .imageArrayLayers(1)
                    .imageUsage(VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK11.VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(surfCapabilities.currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .clipped(true);

            if (vsync) vkSwapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
            else       vkSwapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);

            LongBuffer lp = stack.mallocLong(1);
            int errCreate = KHRSwapchain.vkCreateSwapchainKHR(device.asVk(), vkSwapchainCreateInfo, null, lp);
            VulkanUtil.checkErrorCode(errCreate, "Failed to create swapchain");

            this.swapchainHandle = lp.get(0);

            this.imageViews = createImageViews(stack, device, this.swapchainHandle, this.surfaceFormat.format());
        }
    }

    public void cleanup() {
        Logger.debug("Destroying Vulkan SwapChain");
        this.swapChainExtent.free();

        Arrays.asList(this.imageViews)
              .forEach(ImageView::cleanup);

        KHRSwapchain.vkDestroySwapchainKHR(this.logicalDevice.asVk(), this.swapchainHandle, null);
    }


    private static int calcNumImages(VkSurfaceCapabilitiesKHR surfCapabilities, int requestedImages) {
        int maxImages = surfCapabilities.maxImageCount();
        int minImages = surfCapabilities.minImageCount();
        int result = Math.clamp(requestedImages, minImages, maxImages);

        Logger.debug("Requested [{}] images, got [{}] images. Surface capabilities, maxImages: [{}], minImages [{}]",
                     requestedImages, result, maxImages, minImages);

        return result;
    }

    private static SurfaceFormat calcSurfaceFormat(PhysicalDevice physicalDevice, Surface surface) {
        int imageFormat;
        int colorSpace;
        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer ip = stack.mallocInt(1);
            int errCountFormats = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.asVk(), surface.asVk(), ip, null);
            VulkanUtil.checkErrorCode(errCountFormats, "Failed to get the number of available surface formats");

            int numFormats = ip.get(0);
            if (numFormats <= 0) throw new RuntimeException("No surface formats retrieved");

            VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.calloc(numFormats, stack);
            int errGetFormats = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.asVk(), surface.asVk(), ip, surfaceFormats);
            VulkanUtil.checkErrorCode(errGetFormats, "Failed to get available surface formats");

            imageFormat = VK11.VK_FORMAT_B8G8R8A8_SRGB;
            colorSpace = surfaceFormats.get(0).colorSpace();
            for (int i = 0; i < numFormats; i++) {
                VkSurfaceFormatKHR surfaceFormatKHR = surfaceFormats.get(i);
                boolean isSRGB = surfaceFormatKHR.format() == VK11.VK_FORMAT_B8G8R8A8_SRGB &&
                                 surfaceFormatKHR.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
                if (isSRGB) {
                    imageFormat = surfaceFormatKHR.format();
                    colorSpace = surfaceFormatKHR.colorSpace();
                    break;
                }
            }
        }

        return new SurfaceFormat(imageFormat, colorSpace);
    }

    private static VkExtent2D calcSwapChainExtent(Window window, VkSurfaceCapabilitiesKHR surfCapabilities) {
        VkExtent2D result = VkExtent2D.calloc();
        if (surfCapabilities.currentExtent().width() == 0xFFFFFFFF) {
            // Surface size undefined. Set to the window size if within bounds
            VkExtent2D minExtent = surfCapabilities.minImageExtent();
            VkExtent2D maxExtent = surfCapabilities.minImageExtent();

            int width = Math.clamp(window.getWidth(), minExtent.width(), maxExtent.width());
            int height = Math.clamp(window.getHeight(), minExtent.height(), maxExtent.height());

            result.width(width);
            result.height(height);
        } else {
            // Surface already defined, just use that for the swap chain
            result.set(surfCapabilities.currentExtent());
        }
        return result;
    }

    private static ImageView[] createImageViews(MemoryStack stack, LogicalDevice device, long swapChain, int format) {
        ImageView[] result;

        IntBuffer ip = stack.mallocInt(1);
        int errCountSwapImages = KHRSwapchain.vkGetSwapchainImagesKHR(device.asVk(), swapChain, ip, null);
        VulkanUtil.checkErrorCode(errCountSwapImages, "Failed to get number of surface images");
        int numImages = ip.get(0);

        LongBuffer swapChainImages = stack.mallocLong(numImages);
        int errGetSwapImages = KHRSwapchain.vkGetSwapchainImagesKHR(device.asVk(), swapChain, ip, swapChainImages);
        VulkanUtil.checkErrorCode(errGetSwapImages, "Failed to get surface images");

        result = new ImageView[numImages];
        ImageView.Builder builder = ImageView.builder()
                                             .format(format)
                                             .aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT);
        for (int i = 0; i < numImages; i++)
            result[i] = builder.build(device, swapChainImages.get(i));

        return result;
    }
}
