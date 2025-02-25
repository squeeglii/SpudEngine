package me.cg360.spudengine.core.render.image;

import org.lwjgl.vulkan.VK11;

public enum UploadMode {

    UNDEFINED_TO_TRANSFER(
            VK11.VK_IMAGE_LAYOUT_UNDEFINED, VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            VK11.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, 0,
            VK11.VK_PIPELINE_STAGE_TRANSFER_BIT, VK11.VK_ACCESS_TRANSFER_WRITE_BIT
    ),
    TRANSFER_TO_SHADER_SAMPLER(
            VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            VK11.VK_PIPELINE_STAGE_TRANSFER_BIT, VK11.VK_ACCESS_TRANSFER_WRITE_BIT,
            VK11.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK11. VK_ACCESS_SHADER_READ_BIT
    );

    private int fromLayout, toLayout, srcStage, srcAccessMask, dstStage, dstAccessMask;

    UploadMode(int fromLayout, int toLayout, int srcStage, int srcAccessMask, int dstStage, int dstAccessMask) {
        this.fromLayout = fromLayout;
        this.toLayout = toLayout;
        this.srcStage = srcStage;
        this.srcAccessMask = srcAccessMask;
        this.dstStage = dstStage;
        this.dstAccessMask = dstAccessMask;
    }

    public int fromLayout() {
        return this.fromLayout;
    }

    public int toLayout() {
        return this.toLayout;
    }

    public int srcStage() {
        return this.srcStage;
    }

    public int srcAccessMask() {
        return this.srcAccessMask;
    }

    public int dstStage() {
        return this.dstStage;
    }

    public int dstAccessMask() {
        return this.dstAccessMask;
    }
}
