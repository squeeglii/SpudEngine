package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class FilesystemTexture extends Texture {

    public FilesystemTexture(LogicalDevice device, String resourceName, String path, int format) {
        super(resourceName, false);

        ByteBuffer imageBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            imageBuffer = STBImage.stbi_load(path, w, h, channels, 4);

            if (imageBuffer == null)
                throw new ImageParseFailException("Texture file [%s] at '%s' not loaded: %s".formatted(
                        resourceName, path,
                        STBImage.stbi_failure_reason()
                ));

            this.width = w.get();
            this.height = h.get();
            this.mipLevels = 1;
            this.format = format;

            this.createStagingBuffer(device, imageBuffer);
            this.createImageWrappers(device);
        }

        STBImage.stbi_image_free(imageBuffer);
    }
}
