package me.cg360.spudengine.core.render.image.texture;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.util.IndexedLinkedHashMap;
import org.tinylog.Logger;

import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TextureManager {

    public static final String TEX_MISSING = "missing";
    public static final String TEX_BLANK = "blank";

    public static final String BASE_PATH = "assets/textures/";

    private final IndexedLinkedHashMap<String, Texture> textures;

    private Texture missingTexture; // id0 // these get cleaned up automatically but make sure to null them in #cleanup()
    private Texture blankTexture;   // id1

    public TextureManager(LogicalDevice device) {
        this.textures = new IndexedLinkedHashMap<>();
        this.missingTexture = new CheckerboardTexture(device, TEX_MISSING, 64, 64, 1, 64);
        this.blankTexture = new SolidColourTexture(device, TEX_BLANK, 64, 64, 1, Color.WHITE);
        this.addToMap(this.missingTexture);
        this.addToMap(this.blankTexture);

        Logger.info("Initialized Texture Manager");
    }

    public Texture loadTexture(LogicalDevice device, String resourcePath, int format) {
        if(resourcePath == null || resourcePath.isBlank())
            return this.missingTexture;

        String formattedPath = resourcePath.trim().toLowerCase();

        Texture texture = this.textures.get(formattedPath);
        if (texture != null) {
            Logger.warn("Tried to re-register texture at '{}'. Using existing texture.", formattedPath);
            return texture;
        }


        //URL resourceDir = ClassLoader.getSystemResource();

        try {
            String p = this.getClass().getResource(BASE_PATH + formattedPath).getPath();
            File file = new File(p);
            if(!file.exists()) {
                Logger.error("Missing texture at '{}'. Using fallback.", file.getPath());
                return this.missingTexture;
            }

            Texture fileTexture = new FilesystemTexture(device, formattedPath, file.getPath(), format);
            return this.addToMap(fileTexture);

        //} catch (URISyntaxException err) {
        //    Logger.error("Failed to parse texture path '{}': {}", resourceDir, err.getMessage());
        //    return this.missingTexture;
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

}
