package me.cg360.spudengine.core.render.image.texture.generated;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.texture.Texture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.ByteBuffer;

public class BorderTexture extends Texture {

    public BorderTexture(LogicalDevice device, String resourceName, int width, int height, int mipLevels, int borderSize, Color baseColour, Color borderColour) {
        super(resourceName, true);

        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;
        this.format = VK11.VK_FORMAT_R8G8B8A8_SRGB;

        int pixelCount = width * height;
        int componentCount = 4 * pixelCount;

        Logger.debug("Creating image of size {}x{} - {} byte(s)", width, height, componentCount);
        ByteBuffer imageData = ByteBuffer.allocate(componentCount);

        for(int x = 0; x < width; x++) {
            boolean isInXBorder = x < borderSize || x >= width - borderSize;

            for(int y = 0; y < height; y++) {
                boolean isInYBorder = y < borderSize || y >= height - borderSize;

                boolean useBorder = isInXBorder || isInYBorder;
                Color tileColour = useBorder ? borderColour : baseColour;

                imageData.put((byte) tileColour.getRed());
                imageData.put((byte) tileColour.getGreen());
                imageData.put((byte) tileColour.getBlue());
                imageData.put((byte) tileColour.getAlpha());
            }
        }
        imageData.flip();

        this.createStagingBuffer(device, imageData);
        this.createImageWrappers(device);
    }

}