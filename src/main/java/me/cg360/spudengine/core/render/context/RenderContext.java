package me.cg360.spudengine.core.render.context;

import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import org.lwjgl.vulkan.VkExtent2D;

public class RenderContext {

    protected int screenWidth;
    protected int screenHeight;

    protected RenderGoal renderGoal;
    protected Pipeline currentPipeline;
    protected int pass;
    protected int subpass;
    protected int frameIndex;

    protected int redrawIteration;
    protected boolean requestRedraw = false;

    public RenderContext() {
        this.reset();
    }

    protected void reset() {
        this.frameIndex = -1;
        this.pass = -1;
        this.subpass = 0;
        this.redrawIteration = 0;
        this.renderGoal = RenderGoal.NONE;
        this.currentPipeline = null;

        this.requestRedraw = false;
    }

    public void refreshResolution(SwapChain swapChain) {
        if(swapChain != null) {
            VkExtent2D extent2D = swapChain.getSwapChainExtent();
            this.screenWidth = extent2D.width();
            this.screenHeight = extent2D.height();
        } else {
            this.screenWidth = -1;
            this.screenHeight = -1;
        }
    }


    public RenderGoal renderGoal() {
        return this.renderGoal;
    }

    public Pipeline currentPipeline() {
        return this.currentPipeline;
    }

    public int pass() {
        return this.pass;
    }

    public int subpass() {
        return this.subpass;
    }

    public int frameIndex() {
        return this.frameIndex;
    }

    public int screenWidth() {
        return this.screenWidth;
    }

    public int screenHeight() {
        return this.screenHeight;
    }

    public int redrawIteration() {
        return this.redrawIteration;
    }

    public void requestRedraw() {
        this.requestRedraw = true;
    }

    public void cancelRedraw() {
        this.requestRedraw = false;
    }

    public boolean hasRequestedRedraw() {
        if(this.requestRedraw) {
            this.redrawIteration++;
            this.requestRedraw = false;
            return true;
        }

        return false;
    }
}
