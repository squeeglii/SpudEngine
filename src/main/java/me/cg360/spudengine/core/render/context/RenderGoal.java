package me.cg360.spudengine.core.render.context;

public enum RenderGoal {

    NONE,
    STENCIL_WRITING,       // initial room writes.
    STENCIL_ADJUSTMENT,    // portals
    STANDARD_DRAWING

}
