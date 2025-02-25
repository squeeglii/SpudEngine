package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.ByteBuffer;

public class SolidColourTexture extends Texture {

    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public SolidColourTexture(LogicalDevice device, String resourceName, int width, int height, int mipLevels, Color colour) {
        super(resourceName, true);

        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;
        this.format = VK11.VK_FORMAT_R8G8B8A8_SRGB;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int pixelCount = width * height;
            int componentCount = 4 * pixelCount;
            ByteBuffer imageData = stack.calloc(componentCount);

            Logger.debug("Creating image of size {}x{} - {} byte(s)", width, height, componentCount);

            // transparent == all zeros.
            if(colour != null && !colour.equals(TRANSPARENT)) {
                byte[] colourBytes = new byte[] {
                        (byte) (colour.getRed()   & 0xFF),
                        (byte) (colour.getGreen() & 0xFF),
                        (byte) (colour.getBlue()  & 0xFF),
                        (byte) (colour.getAlpha() & 0xFF)
                };

                for(int p = 0; p < pixelCount; p++) {
                    imageData.put(colourBytes);
                }
            }
            imageData.flip();

            this.createStagingBuffer(device, imageData);
            this.createImageWrappers(device);
        }
    }

}
