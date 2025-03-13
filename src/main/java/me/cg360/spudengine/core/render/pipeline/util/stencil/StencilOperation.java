package me.cg360.spudengine.core.render.pipeline.util.stencil;

// VK11.VK_STENCIL_OP_...
public enum StencilOperation {
    KEEP,
    ZERO,
    REPLACE,
    INCREMENT_AND_CLAMP,
    DECREMENT_AND_CLAMP,
    INVERT,
    INCREMENT_AND_WRAP,
    DECREMENT_AND_WRAP
}
