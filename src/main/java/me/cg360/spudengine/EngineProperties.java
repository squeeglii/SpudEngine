package me.cg360.spudengine;

import org.tinylog.Logger;

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

    // Logic
    public static final int UPDATES_PER_SECOND = 30;

    private static boolean isInitialized = false;

    public static void initialize(String[] launchArgs) {
        if (isInitialized) {
            Logger.warn("Engine Properties have already been initialized! Skipping re-init.");
            return;
        }

        isInitialized = true;
    }

}
