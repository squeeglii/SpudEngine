package me.cg360.spudengine.wormholes;

public class GameProperties {

    public static final SelectedRenderer RENDER_PROCESS = SelectedRenderer.MULTI_PASS_FORWARD;

    // Must be +1 what's in the shader for multi-pass support.
    // -- easier than updating all the maths.
    public static final int MAX_PORTAL_DEPTH = 7;


    public static final boolean FORCE_RENDER_SOLID_PORTALS = false;

    // does not work on naive renderer:
    public static final boolean SHOW_ORIGIN_ROOM = true;
    public static final int ONLY_SHOW_RECURSION_LEVEL = -1;  // -1 = disabled.

}
