package me.cg360.spudengine.wormholes;

public class GameProperties {

    public static boolean forceRenderSolidPortals = false;

    public static final SelectedRenderer RENDER_PROCESS = SelectedRenderer.LAYERED_COMPOSE;

    // Must be +1 what's in the shader for multi-pass support.
    // -- easier than updating all the maths.
    public static final int MAX_PORTAL_DEPTH = 7;

    // does not work on naive renderer:
    public static final boolean SHOW_ORIGIN_ROOM = false;
    public static final int ONLY_SHOW_RECURSION_LEVEL = -1;  // -1 = disabled.

}
