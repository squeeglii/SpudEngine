package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.ByteBuffer;

public class CheckerboardTexture extends Texture {

    private static final Color TILE_A = new Color(0, 0, 0, 255);
    private static final Color TILE_B = new Color(255, 0, 255, 255);

    public CheckerboardTexture(LogicalDevice device, String resourceName, int width, int height, int mipLevels, int checkerboardSize) {
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

            for(int x = 0; x < width; x++) {
                int xTile = Math.floorDiv(x, checkerboardSize);

                for(int y = 0; y < height; y++) {
                    int yTile = Math.floorDiv(y, checkerboardSize);

                    boolean useFirst = (xTile + yTile) % 2 == 0;
                    Color tileColour = useFirst ? TILE_A : TILE_B;

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

}