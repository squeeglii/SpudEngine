package me.cg360.spudengine.core.render.impl.layered;

import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.impl.layered.stage.ComposeRenderer;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.world.Scene;

public abstract class LayeredRenderer extends RenderProcess {

    private LayeredRenderer layerStage;
    private ComposeRenderer composeStage;

    public LayeredRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);
    }

    @Override
    public void cleanup() {
        this.layerStage.cleanup();
        this.composeStage.cleanup();
    }
}
