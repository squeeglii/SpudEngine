package me.cg360.spudengine.core.render.pipeline.util.stencil;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkStencilOpState;

public class StencilConfig {

    private int reference;
    private CompareOperation compareOp;

    private int readMask = 0xFF;
    private int writeMask = 0xFF;

    private StencilOperation passOp;
    private StencilOperation stencilFailOp;
    private StencilOperation depthFailOp;

    public StencilConfig(int ref, CompareOperation compareOp) {
        this.reference = ref;
        this.compareOp = compareOp;

        this.passOp = StencilOperation.KEEP;
        this.stencilFailOp = StencilOperation.KEEP;
        this.depthFailOp = StencilOperation.KEEP;
    }

    public VkStencilOpState configure(MemoryStack stack) {
        VkStencilOpState stencil = VkStencilOpState.calloc(stack);

        stencil.reference(this.reference);

        stencil.compareMask(this.readMask);
        stencil.writeMask(this.writeMask);

        stencil.compareOp(this.compareOp.ordinal());

        stencil.passOp(this.passOp.ordinal());
        stencil.failOp(this.stencilFailOp.ordinal());
        stencil.depthFailOp(this.depthFailOp.ordinal());

        return stencil;
    }

    public StencilConfig setReference(int reference) {
        this.reference = reference;
        return this;
    }

    public StencilConfig setCompareOp(CompareOperation compareOp) {
        this.compareOp = compareOp;
        return this;
    }

    public StencilConfig setReadMask(int readMask) {
        this.readMask = readMask;
        return this;
    }

    public StencilConfig setWriteMask(int writeMask) {
        this.writeMask = writeMask;
        return this;
    }

    public StencilConfig setPassOp(StencilOperation passOp) {
        this.passOp = passOp;
        return this;
    }

    public StencilConfig setStencilFailOp(StencilOperation stencilFailOp) {
        this.stencilFailOp = stencilFailOp;
        return this;
    }

    public StencilConfig setDepthFailOp(StencilOperation depthFailOp) {
        this.depthFailOp = depthFailOp;
        return this;
    }

    public StencilConfig setAllWriteOps(StencilOperation op) {
        this.passOp = op;
        this.stencilFailOp = op;
        this.depthFailOp = op;
        return this;
    }
}
