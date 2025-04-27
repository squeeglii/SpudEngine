package me.cg360.spudengine.core.render.image.texture.generated;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.texture.Texture;
import org.joml.Vector2f;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.ByteBuffer;

public class CircleTexture extends Texture {

    public CircleTexture(LogicalDevice device, String resourceName, int width, int height, int mipLevels, int radius, int outerBorder, int squishFactor, Color baseColour, Color borderColour, Color innerColour) {
        super(resourceName, true);

        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;
        this.format = VK11.VK_FORMAT_R8G8B8A8_SRGB;

        int pixelCount = width * height;
        int componentCount = 4 * pixelCount;

        Logger.debug("Creating image of size {}x{} - {} byte(s)", width, height, componentCount);
        ByteBuffer imageData = ByteBuffer.allocate(componentCount);

        int radiusSquared = radius * radius;

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                int centeredX = x - (width / 2);
                int centeredY = y - (height / 2);

                Vector2f offset = new Vector2f(centeredX, centeredY);
                Vector2f norm = offset.normalize(new Vector2f());

                float fracHeight = Math.abs(centeredY) / (float) height;
                double squishFrac = 1 + (squishFactor - 1f) * Math.sin(Math.PI * fracHeight);

                int borderRadiusSquared = (int) ((radius + (outerBorder * squishFrac)) * (radius + (outerBorder * squishFrac)));

                Color colour;
                float lengthSq = offset.lengthSquared();

                if(lengthSq < radiusSquared) {
                    colour = innerColour;
                } else if(lengthSq < borderRadiusSquared) {
                    colour = borderColour;
                } else {
                    colour = baseColour;
                }

                imageData.put((byte) colour.getRed());
                imageData.put((byte) colour.getGreen());
                imageData.put((byte) colour.getBlue());
                imageData.put((byte) colour.getAlpha());
            }
        }
        imageData.flip();

        this.createStagingBuffer(device, imageData);
        this.createImageWrappers(device);
    }

}