package me.cg360.spudengine.core.render.pipeline.util;

import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

// src - from fragment
// dst - currently in buffer
public record BlendFunc(BlendFactor srcColour, BlendFactor srcAlpha, BlendFactor dstColour, BlendFactor dstAlpha) {

    public static final BlendFunc DEFAULT = new BlendFunc(
            BlendFactor.SRC_ALPHA,
            BlendFactor.ONE_MINUS_SRC_ALPHA,
            BlendFactor.ONE,
            BlendFactor.ZERO
    );

    public static final BlendFunc REPLACE = new BlendFunc(
            BlendFactor.ONE,
            BlendFactor.ONE,
            BlendFactor.ZERO,
            BlendFactor.ZERO
    );

    public static final BlendFunc USE_DESTINATION_ALPHA = new BlendFunc(
            BlendFactor.DST_ALPHA,
            BlendFactor.ONE_MINUS_DST_ALPHA,
            BlendFactor.ONE,
            BlendFactor.ZERO
    );

    /*
    BlendFactor.DST_ALPHA,
            BlendFactor.ZERO,
            BlendFactor.ONE_MINUS_DST_ALPHA,
            BlendFactor.ONE
     */

    public void configure(VkPipelineColorBlendAttachmentState attachment) {
        attachment.srcColorBlendFactor(this.srcColour.ordinal());
        attachment.srcAlphaBlendFactor(this.srcAlpha.ordinal());
        attachment.dstColorBlendFactor(this.dstColour.ordinal());
        attachment.dstAlphaBlendFactor(this.dstAlpha.ordinal());
    }

}
