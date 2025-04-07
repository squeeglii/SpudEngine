package me.cg360.spudengine.core.render.context;

import me.cg360.spudengine.core.render.pipeline.Pipeline;

public class RenderContext {

    protected RenderGoal renderGoal;
    protected Pipeline currentPipeline;
    protected int pass;


    public RenderGoal getRenderGoal() {
        return this.renderGoal;
    }

    public Pipeline getCurrentPipeline() {
        return this.currentPipeline;
    }

    public int getPass() {
        return this.pass;
    }
}
