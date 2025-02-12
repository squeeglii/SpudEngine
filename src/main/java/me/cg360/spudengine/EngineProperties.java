package me.cg360.spudengine;

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
    public static final Color CLEAR_COLOUR = new Color(1.0f, 0.0f, 1.0f, 1.0f);

    // Logic
    public static final int UPDATES_PER_SECOND = 30;

    // Graphics
    public static final String PREFERRED_DEVICE_NAME = null;
    public static final int STARTING_WIDTH = 1600;
    public static final int STARTING_HEIGHT = 900;
    public static final int SWAP_CHAIN_IMAGES = 3;
    public static final boolean VSYNC = true;


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
