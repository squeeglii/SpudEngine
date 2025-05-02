package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.pipeline.shader.BinaryShaderFile;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderType;
import me.cg360.spudengine.test.EnginePlayground;
import me.cg360.spudengine.wormholes.WormholeDemo;
import org.tinylog.Logger;

import java.awt.*;

/**
 * A mix of constants and values assigned at startup.
 * During runtime, you can assume these are constants.
 */
public class EngineProperties {

    // Engine
    public static final String ENGINE_NAME = "SpudEngine";
    public static final int ENGINE_VERSION = 1;

    // Publishing
    public static final String APP_NAME = "Working Project";
    public static final int APP_VERSION = 1;
    public static final String WINDOW_TITLE = "Working Project";

    // Debugging
    public static final boolean USE_DEBUGGING = true;
    public static final Color CLEAR_COLOUR = new Color(0.05f, 0.05f, 0.05f, 1.00f);
    public static final Color FULL_ALPHA = new Color(0.00f, 0.00f, 0.00f, 0.00f);

    // Logic
    public static final Entrypoint GAME = WormholeDemo::new;
    public static final int UPDATES_PER_SECOND = 30; // logic tick interval

    // Graphics
    public static final boolean SHOULD_RECOMPILE_SHADERS = true; // Disable if init hangs after pipeline cache
    public static final String PREFERRED_DEVICE_NAME = null;
    public static final int STARTING_WIDTH = 1600;
    public static final int STARTING_HEIGHT = 900;
    public static final int SWAP_CHAIN_IMAGES = 3;
    public static final boolean VSYNC = false;
    public static final float FOV = 70.4f;
    public static final float NEAR_PLANE = 0.1f;
    public static final float FAR_PLANE = 100.0f;
    public static final int MAX_TEXTURES = 1024;
    public static final int MAX_OVERLAY_TEXTURES = 1024;
    public static final BinaryShaderFile[] FORWARD_SHADERS = new BinaryShaderFile[] {
            new BinaryShaderFile(ShaderType.VERTEX, "shaders/vertex"),
            new BinaryShaderFile(ShaderType.GEOMETRY, "shaders/portal/colour/geometry"),
            new BinaryShaderFile(ShaderType.FRAGMENT, "shaders/portal/colour/fragment"),
    };

    public static final BinaryShaderFile[] LAYERED_SHADERS = new BinaryShaderFile[] {
            new BinaryShaderFile(ShaderType.VERTEX, "shaders/vertex"),
            new BinaryShaderFile(ShaderType.GEOMETRY, "shaders/portal/colour/geometry"),
            new BinaryShaderFile(ShaderType.FRAGMENT, "shaders/portal/colour/fragment_layered"),
    };

    // -- End of properties --
    private static boolean isInitialized = false;

    public static void initialize(String[] launchArgs) {
        if (isInitialized) {
            Logger.warn("Engine Properties have already been initialized! Skipping re-init.");
            return;
        }

        isInitialized = true;
    }

}
