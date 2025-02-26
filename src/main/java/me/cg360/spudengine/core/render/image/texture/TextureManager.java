package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.exception.DuplicateException;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.texture.generated.BorderTexture;
import me.cg360.spudengine.core.render.image.texture.generated.CheckerboardTexture;
import me.cg360.spudengine.core.render.image.texture.generated.FilesystemTexture;
import me.cg360.spudengine.core.render.image.texture.generated.SolidColourTexture;
import me.cg360.spudengine.core.util.IndexedLinkedHashMap;
import me.cg360.spudengine.core.util.Registry;
import org.tinylog.Logger;

import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TextureManager implements Registry {

    private static final Color TILE_A = new Color(0, 0, 0, 255);
    private static final Color TILE_B = new Color(255, 0, 255, 255);

    public static final String TEX_MISSING = "missing";
    public static final String TEX_BLANK = "blank";

    public static final String BASE_PATH = "assets/textures/";

    private final LogicalDevice device;
    private final IndexedLinkedHashMap<String, Texture> textures;

    private Texture missingTexture; // id0 // these get cleaned up automatically but make sure to null them in #cleanup()
    private Texture blankTexture;   // id1

    public TextureManager(LogicalDevice device) {
        this.device = device;
        this.textures = new IndexedLinkedHashMap<>();
        this.missingTexture = new CheckerboardTexture(device, TEX_MISSING, 512, 512, 1, 32, TILE_A, TILE_B);
        this.blankTexture = new SolidColourTexture(device, TEX_BLANK, 64, 64, 1, Color.WHITE);
        this.addToMap(this.missingTexture);
        this.addToMap(this.blankTexture);

        Logger.info("Initialized Texture Manager");
    }

    public Texture loadTexture(LogicalDevice device, String resourcePath, int format) {
        if(resourcePath == null || resourcePath.isBlank())
            return this.blankTexture;

        String formattedPath = resourcePath.trim().toLowerCase();

        Texture texture = this.textures.get(formattedPath);
        if (texture != null) {
            Logger.warn("Tried to re-register texture at '{}'. Using existing texture.", formattedPath);
            return texture;
        }

        try {
            File file = new File(BASE_PATH, resourcePath);
            if(!file.exists()) {
                Logger.error("Missing texture at '{}'. Using fallback.", file.getPath());
                Logger.debug("Base Path: {}", BASE_PATH);
                Logger.debug("Resource: {}", resourcePath);
                return this.missingTexture;
            }

            Texture fileTexture = new FilesystemTexture(device, formattedPath, file.getPath(), format);
            return this.addToMap(fileTexture);
        } catch (ImageParseFailException err) {
            Logger.error("Failed to parse texture '{}': {}", formattedPath, err.getMessage());
            return this.missingTexture;
        }
    }

    public void registerTexture(Texture texture) {
        String resourcePath = texture.getResourceName();
        if (this.textures.containsKey(resourcePath)) {
            Logger.warn("Tried to re-register texture at '{}'. Ignored.", resourcePath);
            return;
        }

        this.addToMap(texture);
    }

    /**
     * Does not check if texture is present.
     * @return the texture passed as a parameter.
     */
    private Texture addToMap(Texture texture) {
        this.textures.put(texture.getResourceName(), texture);
        return texture;
    }

    public void cleanup() {
        this.textures.values().forEach(Texture::cleanup);
        this.missingTexture = null; // missing texture = texture id 0.
        this.blankTexture = null; // missing texture = texture id 1.
        this.textures.clear();
    }

    public List<Texture> getAsList() {
        List<Texture> tex = new ArrayList<>(this.textures.values());
        tex.add(this.missingTexture);
        return tex;
    }

    public int getPosition(String texturePath) {
        return texturePath != null
                ? this.textures.getIndexOf(texturePath.trim().toLowerCase())
                : -1;
    }

    public Texture getTexture(String texturePath) {
        return this.textures.get(texturePath.trim().toLowerCase());
    }

    @Override
    public String getRegistryIdentifier() {
        return "TEXTURE_MANAGER";
    }

    /** Creates and registers a solid colour texture. */
    public SolidColourTexture newSolidColourTexture(String resourceName, int width, int height, int mipLevels, Color colour) {
        if(this.textures.containsKey(resourceName))
            throw new DuplicateException(this, resourceName);

        SolidColourTexture texture = new SolidColourTexture(this.device, resourceName, width, height, mipLevels, colour);
        this.registerTexture(texture);

        return texture;
    }

    /** Creates and registers a checkerboard texture. */
    public CheckerboardTexture newCheckerboardTexture(String resourceName, int width, int height, int mipLevels, int checkerboardSize, Color colourA, Color colourB) {
        if(this.textures.containsKey(resourceName))
            throw new DuplicateException(this, resourceName);

        CheckerboardTexture texture = new CheckerboardTexture(this.device, resourceName, width, height, mipLevels, checkerboardSize, colourA, colourB);
        this.registerTexture(texture);

        return texture;
    }

    /** Creates and registers a border texture. */
    public BorderTexture newBorderTexture(String resourceName, int width, int height, int mipLevels, int borderSize, Color baseColour, Color borderColour) {
        if(this.textures.containsKey(resourceName))
            throw new DuplicateException(this, resourceName);

        BorderTexture texture = new BorderTexture(this.device, resourceName, width, height, mipLevels, borderSize, baseColour, borderColour);
        this.registerTexture(texture);

        return texture;
    }

}
