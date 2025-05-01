package me.cg360.spudengine.core.render.context;

import me.cg360.spudengine.core.render.pipeline.Pipeline;

public class RenderContext {

    protected RenderGoal renderGoal;
    protected Pipeline currentPipeline;
    protected int pass;
    protected int frameIndex;


    public RenderGoal renderGoal() {
        return this.renderGoal;
    }

    public Pipeline currentPipeline() {
        return this.currentPipeline;
    }

    public int pass() {
        return this.pass;
    }

    public int frameIndex() {
        return this.frameIndex;
    }
}
